package com.retrohall.core

/**
 * Multi-file disc formats. A `.cue` sheet names its `.bin` tracks and an `.m3u` playlist names the
 * discs of a multi-disc game; those files must sit next to the sheet when the core opens it.
 */
object DiscFiles {
    private val cueFile = Regex("""^\s*FILE\s+(?:"([^"]+)"|(\S+))""", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

    fun isSheet(fileName: String): Boolean = RomIdentifier.extensionOf(fileName) in setOf("cue", "m3u")

    /** File names (without directories) referenced by a `.cue` or `.m3u` file. */
    fun referencedFiles(fileName: String, content: String): List<String> {
        val paths = when (RomIdentifier.extensionOf(fileName)) {
            "cue" -> cueFile.findAll(content).map { it.groupValues[1].ifEmpty { it.groupValues[2] } }.toList()
            "m3u" -> content.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList()
            else -> emptyList()
        }
        return paths.map { it.replace('\\', '/').substringAfterLast('/') }.filter { it.isNotEmpty() }.distinct()
    }
}
