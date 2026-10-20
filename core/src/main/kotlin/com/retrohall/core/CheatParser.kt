package com.retrohall.core

data class CheatCode(val description: String, val code: String, val enabled: Boolean = false)

/**
 * Reads cheat lists (金手指). Supports RetroArch `.cht` files and a simple text format
 * with one `描述 = 代码` per line. Multi-part codes are joined with `+`, which is how
 * libretro cores expect them.
 */
object CheatParser {
    private val retroArchKey = Regex("""^\s*cheat\d+_\w+\s*=""")

    fun parse(text: String): List<CheatCode> =
        if (text.lineSequence().any { retroArchKey.containsMatchIn(it) }) parseRetroArch(text) else parsePlain(text)

    /** Joins the non-blank lines of a code typed by the user into one libretro cheat string. */
    fun normalizeCode(code: String): String = code.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString("+")

    private fun parseRetroArch(text: String): List<CheatCode> {
        val values = mutableMapOf<String, String>()
        text.lineSequence().forEach { line ->
            val separator = line.indexOf('=')
            if (separator > 0) {
                val key = line.substring(0, separator).trim()
                val value = line.substring(separator + 1).trim().removeSurrounding("\"")
                values[key] = value
            }
        }
        val indices = values.keys
            .mapNotNull { Regex("""^cheat(\d+)_code$""").find(it)?.groupValues?.get(1)?.toIntOrNull() }
            .sorted()
        return indices.mapNotNull { index ->
            val code = values["cheat${index}_code"]?.trim().orEmpty()
            if (code.isEmpty()) return@mapNotNull null
            CheatCode(
                description = values["cheat${index}_desc"]?.takeIf { it.isNotBlank() } ?: "金手指 ${index + 1}",
                code = code,
                enabled = values["cheat${index}_enable"].equals("true", ignoreCase = true),
            )
        }
    }

    private fun parsePlain(text: String): List<CheatCode> = text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("//") }
        .mapIndexedNotNull { index, line ->
            val separator = line.indexOf('=')
            if (separator > 0) {
                val code = line.substring(separator + 1).trim()
                if (code.isEmpty()) null else CheatCode(line.substring(0, separator).trim(), code)
            } else {
                CheatCode("金手指 ${index + 1}", line)
            }
        }
        .toList()
}
