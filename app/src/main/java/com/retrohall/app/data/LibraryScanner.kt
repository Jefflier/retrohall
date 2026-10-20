package com.retrohall.app.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.retrohall.core.GameTitles
import com.retrohall.core.Platform
import com.retrohall.core.RomCandidate
import com.retrohall.core.RomIdentifier
import com.retrohall.core.ZipDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipFile

data class ScanProgress(val folder: String, val foundGames: Int)

data class ImportResult(val imported: Int, val unsupported: List<String>)

/** Finds games in library folders (through the Storage Access Framework) and imports single files. */
class LibraryScanner(private val context: Context, private val database: GameDatabase) {
    private val resolver = context.contentResolver

    private val _progress = MutableStateFlow<ScanProgress?>(null)
    val progress: StateFlow<ScanProgress?> = _progress.asStateFlow()

    private class DocumentEntry(
        val id: String,
        val name: String,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long,
    )

    /** Scans a folder tree and replaces its games in the library. Returns the number of games found. */
    suspend fun scanFolder(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootName = queryName(DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId)) ?: rootId
        val found = mutableListOf<ScannedGame>()
        val queue = ArrayDeque<Pair<String, List<String>>>()
        queue.add(rootId to listOf(rootName))
        _progress.value = ScanProgress(rootName, 0)
        try {
            while (queue.isNotEmpty()) {
                ensureActive()
                val (folderId, folders) = queue.removeFirst()
                val children = listChildren(treeUri, folderId)
                // Tracks listed by a .cue sheet are launched through the sheet, not on their own.
                val hasCueSheet = children.any { RomIdentifier.extensionOf(it.name) == "cue" }
                for (child in children) {
                    if (child.isDirectory) {
                        if (!child.name.startsWith(".")) queue.add(child.id to listOf(child.name) + folders)
                        continue
                    }
                    if (!RomIdentifier.isSupported(child.name)) continue
                    val extension = RomIdentifier.extensionOf(child.name)
                    if (hasCueSheet && extension == "bin") continue

                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, child.id)
                    val candidate = RomCandidate(
                        fileName = child.name,
                        sizeBytes = child.size,
                        folders = folders,
                        zipEntries = if (RomIdentifier.needsZipListing(child.name) && !inArcadeFolder(folders)) {
                            listZip(documentUri)
                        } else {
                            null
                        },
                        header = if (RomIdentifier.needsHeader(child.name)) readHeader(documentUri) else null,
                    )
                    val identified = RomIdentifier.identify(candidate) ?: continue
                    found += ScannedGame(
                        uri = documentUri.toString(),
                        fileName = child.name,
                        title = GameTitles.displayTitle(child.name),
                        platform = identified.platform,
                        sizeBytes = child.size,
                        lastModified = child.lastModified,
                        folderUri = treeUri.toString(),
                        parentDocumentId = folderId,
                        zipEntry = identified.zipEntry,
                    )
                    _progress.value = ScanProgress(folders.first(), found.size)
                }
            }
            database.replaceFolder(treeUri.toString(), found)
            found.size
        } finally {
            _progress.value = null
        }
    }

    /** Copies picked files into app storage and adds the games among them to the library. */
    suspend fun importFiles(uris: List<Uri>): ImportResult = withContext(Dispatchers.IO) {
        val romsDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "roms").apply { mkdirs() }
        val copied = uris.mapNotNull { uri ->
            val name = FileUtils.displayName(resolver, uri) ?: return@mapNotNull null
            val target = File(romsDir, FileUtils.sanitize(name))
            val input = resolver.openInputStream(uri) ?: return@mapNotNull null
            FileUtils.copyAtomically(input, target)
            target
        }
        val hasCueSheet = copied.any { RomIdentifier.extensionOf(it.name) == "cue" }
        val unsupported = mutableListOf<String>()
        var imported = 0
        for (file in copied) {
            // Tracks next to a .cue sheet are kept on disk but not listed separately.
            if (hasCueSheet && RomIdentifier.extensionOf(file.name) == "bin") continue
            val identified = RomIdentifier.identify(
                RomCandidate(
                    fileName = file.name,
                    sizeBytes = file.length(),
                    zipEntries = if (RomIdentifier.needsZipListing(file.name)) {
                        runCatching { ZipFile(file).use { zip -> zip.entries().toList().map { it.name } } }.getOrNull()
                    } else {
                        null
                    },
                    header = if (RomIdentifier.needsHeader(file.name)) {
                        FileUtils.readHeader(file.inputStream(), RomIdentifier.HEADER_SIZE)
                    } else {
                        null
                    },
                ),
            )
            if (identified == null) {
                file.delete()
                unsupported += file.name
                continue
            }
            database.upsert(
                ScannedGame(
                    uri = Uri.fromFile(file).toString(),
                    fileName = file.name,
                    title = GameTitles.displayTitle(file.name),
                    platform = identified.platform,
                    sizeBytes = file.length(),
                    lastModified = file.lastModified(),
                    folderUri = null,
                    parentDocumentId = null,
                    zipEntry = identified.zipEntry,
                ),
            )
            imported++
        }
        ImportResult(imported, unsupported)
    }

    /** A readable path such as "内部存储/Games/GBA" for a library folder. */
    fun describeFolder(treeUri: Uri): String {
        val documentId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
            ?: return treeUri.toString()
        val volume = documentId.substringBefore(':')
        val path = documentId.substringAfter(':', "")
        val volumeName = if (volume == "primary") "内部存储" else volume
        return if (path.isEmpty()) volumeName else "$volumeName/$path"
    }

    private fun inArcadeFolder(folders: List<String>): Boolean =
        RomIdentifier.hintedPlatform(folders) == Platform.ARCADE

    private fun listChildren(treeUri: Uri, folderId: String): List<DocumentEntry> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, folderId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        return runCatching {
            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val id = cursor.getString(0) ?: continue
                        val name = cursor.getString(1) ?: continue
                        add(
                            DocumentEntry(
                                id = id,
                                name = name,
                                isDirectory = cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR,
                                size = if (cursor.isNull(3)) 0 else cursor.getLong(3),
                                lastModified = if (cursor.isNull(4)) 0 else cursor.getLong(4),
                            ),
                        )
                    }
                }
            }
        }.getOrNull().orEmpty()
    }

    private fun queryName(documentUri: Uri): String? = runCatching {
        resolver.query(documentUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
    }.getOrNull()

    private fun listZip(documentUri: Uri): List<String>? = runCatching {
        resolver.openFileDescriptor(documentUri, "r")?.use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).channel.use { ZipDirectory.listEntries(it) }
        }
    }.getOrNull()

    private fun readHeader(documentUri: Uri): ByteArray? = runCatching {
        resolver.openInputStream(documentUri)?.let { FileUtils.readHeader(it, RomIdentifier.HEADER_SIZE) }
    }.getOrNull()
}
