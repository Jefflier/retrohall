package com.retrohall.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CheatParserTest {
    @Test
    fun `parses retroarch cht files`() {
        val text = """
            cheats = 3

            cheat0_desc = "Infinite Lives"
            cheat0_code = "SXIOPO"
            cheat0_enable = false

            cheat1_desc = "Max Money"
            cheat1_code = "82003884 0063+82003886 0063"
            cheat1_enable = true

            cheat2_desc = "Empty"
            cheat2_code = ""
        """.trimIndent()

        assertEquals(
            listOf(
                CheatCode("Infinite Lives", "SXIOPO", enabled = false),
                CheatCode("Max Money", "82003884 0063+82003886 0063", enabled = true),
            ),
            CheatParser.parse(text),
        )
    }

    @Test
    fun `parses plain description equals code lines`() {
        val text = """
            # 魂斗罗
            无限命 = SXIOPO
            30条命 = AAKXNEAA

            GZKXGNSE
        """.trimIndent()

        assertEquals(
            listOf(
                CheatCode("无限命", "SXIOPO"),
                CheatCode("30条命", "AAKXNEAA"),
                CheatCode("金手指 3", "GZKXGNSE"),
            ),
            CheatParser.parse(text),
        )
    }

    @Test
    fun `multi line codes are joined with plus`() {
        assertEquals("82003884 0063+82003886 0063", CheatParser.normalizeCode(" 82003884 0063\n\n82003886 0063 \n"))
    }
}
