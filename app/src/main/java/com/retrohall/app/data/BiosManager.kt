package com.retrohall.app.data

import android.content.Context
import android.net.Uri
import com.retrohall.core.BiosFile
import com.retrohall.core.Platform
import com.retrohall.core.RomIdentifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

data class BiosStatus(val platform: Platform, val file: BiosFile, val present: Boolean)

data class BiosImportResult(val recognized: List<String>, val other: List<String>)

/** BIOS files live in the libretro system directory, where cores look for them. */
class BiosManager(private val context: Context) {
    val systemDir: File = File(context.filesDir, "system").apply { mkdirs() }

    private val _statuses = MutableStateFlow(computeStatuses())
    val statuses: StateFlow<List<BiosStatus>> = _statuses.asStateFlow()

    fun fileFor(bios: BiosFile): File {
        val directory = if (bios.directory.isEmpty()) systemDir else File(systemDir, bios.directory)
        return File(directory, bios.fileName)
    }

    /** BIOS files this particular game cannot start without. */
    fun missingFor(game: Game): List<BiosFile> {
        val needed = when {
            game.platform == Platform.FC && RomIdentifier.extensionOf(game.fileName) == "fds" ->
                game.platform.bios.filter { it.fileName == "disksys.rom" }
            else -> game.platform.bios.filter { it.required }
        }
        return needed.filterNot { fileFor(it).isFile }
    }

    /**
     * Copies picked files into the system directory. Known BIOS names are stored with the exact
     * name and folder the core expects; anything else is copied as-is for cores we do not list.
     */
    suspend fun import(uris: List<Uri>): BiosImportResult = withContext(Dispatchers.IO) {
        val known = Platform.entries.flatMap { it.bios }
        val recognized = mutableListOf<String>()
        val other = mutableListOf<String>()
        for (uri in uris) {
            val name = FileUtils.displayName(context.contentResolver, uri) ?: continue
            val match = known.firstOrNull { it.fileName.equals(name, ignoreCase = true) }
            val target = if (match != null) fileFor(match) else File(systemDir, FileUtils.sanitize(name))
            val input = context.contentResolver.openInputStream(uri) ?: continue
            FileUtils.copyAtomically(input, target)
            if (match != null) recognized += match.fileName else other += name
        }
        refresh()
        BiosImportResult(recognized, other)
    }

    fun refresh() {
        _statuses.value = computeStatuses()
    }

    private fun computeStatuses(): List<BiosStatus> = Platform.entries.flatMap { platform ->
        platform.bios.map { BiosStatus(platform, it, fileFor(it).isFile) }
    }
}
