package com.retrohall.core

/** What the scanner knows about a file before deciding whether it is a game. */
class RomCandidate(
    val fileName: String,
    val sizeBytes: Long = 0,
    /** Names of the enclosing folders, nearest first. */
    val folders: List<String> = emptyList(),
    /** Entry names when the file is a zip archive that could be listed. */
    val zipEntries: List<String>? = null,
    /** The first [RomIdentifier.HEADER_SIZE] bytes, provided when [RomIdentifier.needsHeader] is true. */
    val header: ByteArray? = null,
)

/**
 * @param zipEntry entry inside a zip archive that holds the game and must be extracted before launch,
 *   or null when the file itself is passed to the core (including arcade sets, which stay zipped).
 */
data class Identified(val platform: Platform, val zipEntry: String? = null)

/** Decides which platform a file belongs to from its extension, its content and the folders around it. */
object RomIdentifier {
    /** Enough bytes to reach the ISO-9660 volume descriptor of a raw (2352-byte sector) disc image. */
    const val HEADER_SIZE = 0x9400

    private val byExtension: Map<String, Platform> = buildMap {
        Platform.entries.forEach { platform -> platform.extensions.forEach { put(it, platform) } }
    }

    private val ambiguousExtensions = setOf("bin", "iso", "img")

    /** Every extension the scanner should look at. */
    val supportedExtensions: Set<String> = byExtension.keys + ambiguousExtensions + "zip"

    /** BIOS archives that sit next to arcade games but are not games themselves. */
    private val arcadeBiosSets = setOf(
        "neogeo", "neocdz", "pgm", "skns", "decocass", "isgsm", "midssio", "nmk004", "ym2608", "cchip",
        "bubsys", "namcoc69", "namcoc70", "namcoc75", "qsound", "stvbios", "hng64", "naomi", "awbios",
        "konamigx", "coleco", "msx", "spectrum", "fdsbios",
    )

    fun extensionOf(fileName: String): String = fileName.substringAfterLast('.', "").lowercase()

    fun isSupported(fileName: String): Boolean = extensionOf(fileName) in supportedExtensions

    /** Whether the scanner should read the file header before calling [identify]. */
    fun needsHeader(fileName: String): Boolean = extensionOf(fileName) in ambiguousExtensions

    /** Whether the scanner should list the archive entries before calling [identify]. */
    fun needsZipListing(fileName: String): Boolean = extensionOf(fileName) == "zip"

    fun identify(candidate: RomCandidate): Identified? {
        val extension = extensionOf(candidate.fileName)
        byExtension[extension]?.let { return Identified(it) }

        val hinted = hintedPlatform(candidate.folders)
        return when (extension) {
            "zip" -> identifyZip(candidate, hinted)
            "bin" -> identifyBin(candidate.header, hinted)
            "iso", "img" -> identifyDiscImage(candidate.header, hinted)
            else -> null
        }
    }

    /** First platform whose folder hint matches one of [folders], nearest folder first. */
    fun hintedPlatform(folders: List<String>): Platform? {
        for (folder in folders) {
            val normalized = normalizeFolder(folder)
            if (normalized.isEmpty()) continue
            Platform.entries.firstOrNull { platform ->
                platform.folderHints.any { normalizeFolder(it) == normalized }
            }?.let { return it }
        }
        return null
    }

    private fun normalizeFolder(name: String): String =
        name.lowercase().filterNot { it == ' ' || it == '_' || it == '-' || it == '.' }

    private fun identifyZip(candidate: RomCandidate, hinted: Platform?): Identified? {
        val baseName = candidate.fileName.substringBeforeLast('.').lowercase()
        if (baseName in arcadeBiosSets) return null

        val entries = candidate.zipEntries ?: return Identified(hinted ?: Platform.ARCADE)
        val files = entries.filterNot { it.endsWith("/") }
        if (files.isEmpty()) return null

        // A zipped cartridge game: launch the single ROM inside. Disc images must not be zipped.
        files.firstNotNullOfOrNull { entry ->
            byExtension[extensionOf(entry)]?.takeIf { it != Platform.PS1 }?.let { entry to it }
        }?.let { (entry, platform) -> return Identified(platform, entry) }

        if (hinted != null && hinted != Platform.ARCADE) {
            val bin = files.firstOrNull { extensionOf(it) == "bin" }
            return if (bin != null && hinted in setOf(Platform.MD, Platform.PCE, Platform.A2600)) {
                Identified(hinted, bin)
            } else {
                null
            }
        }
        return Identified(Platform.ARCADE)
    }

    private fun identifyBin(header: ByteArray?, hinted: Platform?): Identified? {
        if (header != null) {
            if (header.hasAscii(0x100, "SEGA")) return Identified(Platform.MD)
            if (isPlayStationDisc(header)) return Identified(Platform.PS1)
        }
        return when (hinted) {
            Platform.MD, Platform.PCE, Platform.A2600, Platform.PS1 -> Identified(hinted)
            else -> null
        }
    }

    private fun identifyDiscImage(header: ByteArray?, hinted: Platform?): Identified? {
        if (header != null) {
            if (isPlayStationDisc(header)) return Identified(Platform.PS1)
            // PSP UMD images also use .iso; they are not supported yet.
            if (header.hasAscii(0x8008, "PSP GAME")) return null
        }
        return if (hinted == Platform.PS1) Identified(Platform.PS1) else null
    }

    /** Looks for "PLAYSTATION" in the system identifier of the primary volume descriptor. */
    private fun isPlayStationDisc(header: ByteArray): Boolean =
        header.hasAscii(0x8008, "PLAYSTATION") || header.hasAscii(0x9320, "PLAYSTATION")

    private fun ByteArray.hasAscii(offset: Int, text: String): Boolean {
        if (offset < 0 || offset + text.length > size) return false
        return text.indices.all { this[offset + it] == text[it].code.toByte() }
    }
}
