package com.retrohall.core

/** Turns ROM file names into readable titles and libretro thumbnail URLs. */
object GameTitles {
    private val bracketTag = Regex("""\s*[(\[][^)\]]*[)\]]""")
    private val roundTag = Regex("""\(([^)]*)\)""")
    private val packNumber = Regex("""^\d{3,5}\s*[-_.]\s*""")
    private val whitespace = Regex("""\s+""")
    private val thumbnailForbidden = Regex("""[&*/:`<>?\\|"]""")

    fun baseName(fileName: String): String {
        val dot = fileName.lastIndexOf('.')
        return if (dot > 0) fileName.substring(0, dot) else fileName
    }

    /**
     * "Super Mario Bros. 3 (USA) (Rev 1) [!].nes" becomes "Super Mario Bros. 3";
     * "0012 - 魂斗罗.nes" from numbered ROM packs becomes "魂斗罗".
     */
    fun displayTitle(fileName: String): String {
        val base = baseName(fileName)
        val cleaned = base
            .replace(bracketTag, "")
            .replace(packNumber, "")
            .replace('_', ' ')
            .replace(whitespace, " ")
            .trim()
        return cleaned.ifEmpty { base }
    }

    /** Tags in round brackets, e.g. ["USA", "Rev 1"]. */
    fun tags(fileName: String): List<String> = roundTag.findAll(baseName(fileName))
        .map { it.groupValues[1].trim() }
        .filter { it.isNotEmpty() }
        .toList()

    /** File name libretro-thumbnails uses for a game, with forbidden characters replaced by `_`. */
    fun thumbnailName(fileName: String): String = baseName(fileName).replace(thumbnailForbidden, "_")

    fun thumbnailUrl(platform: Platform, fileName: String): String {
        val system = platform.thumbnailSystemFor(RomIdentifier.extensionOf(fileName))
        return "https://thumbnails.libretro.com/" + encodePathSegment(system) +
            "/Named_Boxarts/" + encodePathSegment(thumbnailName(fileName)) + ".png"
    }

    /** Percent-encodes a URL path segment (spaces become %20, not +). */
    fun encodePathSegment(segment: String): String = buildString {
        for (byte in segment.encodeToByteArray()) {
            val c = byte.toInt() and 0xFF
            val ch = c.toChar()
            if (ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' || ch in "-._~") {
                append(ch)
            } else {
                append('%')
                append("0123456789ABCDEF"[c shr 4])
                append("0123456789ABCDEF"[c and 0x0F])
            }
        }
    }
}
