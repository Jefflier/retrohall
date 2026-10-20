package com.retrohall.app.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.retrohall.core.DiscFiles
import com.retrohall.core.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FilterInputStream
import java.io.InputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/**
 * Turns a library entry into a file path a libretro core can open. Cores need real files, so
 * games behind `content://` URIs are copied into a cache (together with the tracks of a `.cue`
 * sheet) and zipped cartridges are extracted. The cache keeps the most recently played games.
 */
class RomPreparer(private val context: Context) {
    private val resolver = context.contentResolver
    private val cacheRoot = File(context.cacheDir, "games")

    suspend fun prepare(game: Game, onStatus: (String) -> Unit): File = withContext(Dispatchers.IO) {
        val gameDir = File(cacheRoot, game.id.toString()).apply { mkdirs() }
        gameDir.setLastModified(System.currentTimeMillis())
        val uri = Uri.parse(game.uri)
        val prepared = if (uri.scheme == "file") {
            val source = File(requireNotNull(uri.path))
            if (!source.isFile) throw FileNotFoundException("文件不存在：${source.name}")
            if (game.zipEntry != null) extractFromZipFile(source, game.zipEntry, gameDir) else source
        } else if (game.zipEntry != null) {
            onStatus("正在解压游戏…")
            extractFromZipStream(uri, game.zipEntry, gameDir)
        } else {
            onStatus("正在读取游戏文件（${Formatters.size(game.sizeBytes)}）…")
            copyDocument(uri, game.fileName, game.sizeBytes, gameDir)
                .also { copySheetReferences(game, it, gameDir) }
        }
        trimCache(keep = gameDir)
        prepared
    }

    private fun copyDocument(uri: Uri, name: String, expectedSize: Long, directory: File): File {
        val target = File(directory, FileUtils.sanitize(name))
        if (target.isFile && (expectedSize <= 0 || target.length() == expectedSize)) return target
        val input = resolver.openInputStream(uri) ?: throw FileNotFoundException("无法打开 $name")
        FileUtils.copyAtomically(input, target)
        return target
    }

    /** Copies the tracks a `.cue` sheet names (and the sheets an `.m3u` playlist names) next to it. */
    private fun copySheetReferences(game: Game, sheet: File, directory: File, depth: Int = 0) {
        if (!DiscFiles.isSheet(sheet.name) || depth > 1) return
        val treeUri = game.folderUri?.let(Uri::parse) ?: return
        val parentId = game.parentDocumentId ?: return
        val siblings = listSiblings(treeUri, parentId)
        DiscFiles.referencedFiles(sheet.name, sheet.readText()).forEach { referenced ->
            val match = siblings.entries.firstOrNull { it.key.equals(referenced, ignoreCase = true) } ?: return@forEach
            val copied = copyDocument(
                DocumentsContract.buildDocumentUriUsingTree(treeUri, match.value.first),
                match.key,
                match.value.second,
                directory,
            )
            copySheetReferences(game, copied, directory, depth + 1)
        }
    }

    /** Display name to (document id, size) for every file in a folder. */
    private fun listSiblings(treeUri: Uri, parentId: String): Map<String, Pair<String, Long>> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
        )
        return resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            buildMap {
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0) ?: continue
                    val name = cursor.getString(1) ?: continue
                    put(name, id to (if (cursor.isNull(2)) 0L else cursor.getLong(2)))
                }
            }
        }.orEmpty()
    }

    private fun extractFromZipFile(zip: File, entryName: String, directory: File): File {
        val target = File(directory, FileUtils.sanitize(entryName.substringAfterLast('/')))
        ZipFile(zip).use { archive ->
            val entry = archive.getEntry(entryName) ?: throw FileNotFoundException("压缩包中没有 $entryName")
            if (target.isFile && target.length() == entry.size) return target
            FileUtils.copyAtomically(archive.getInputStream(entry), target)
        }
        return target
    }

    private fun extractFromZipStream(uri: Uri, entryName: String, directory: File): File {
        val target = File(directory, FileUtils.sanitize(entryName.substringAfterLast('/')))
        if (target.isFile && target.length() > 0) return target
        val input = resolver.openInputStream(uri) ?: throw FileNotFoundException("无法打开压缩包")
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == entryName) {
                    // copyAtomically closes its input, so hand it a view that leaves the zip open.
                    FileUtils.copyAtomically(NonClosingInputStream(zip), target)
                    return target
                }
            }
        }
        throw FileNotFoundException("压缩包中没有 $entryName")
    }

    private fun trimCache(keep: File) {
        val directories = cacheRoot.listFiles()?.filter { it.isDirectory && it != keep } ?: return
        var total = FileUtils.directorySize(cacheRoot)
        for (directory in directories.sortedBy { it.lastModified() }) {
            if (total <= CACHE_LIMIT_BYTES) break
            total -= FileUtils.directorySize(directory)
            directory.deleteRecursively()
        }
    }

    private class NonClosingInputStream(input: InputStream) : FilterInputStream(input) {
        override fun close() = Unit
    }

    companion object {
        private const val CACHE_LIMIT_BYTES = 2L * 1024 * 1024 * 1024
    }
}
