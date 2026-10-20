package com.retrohall.core

import org.junit.Assert.assertEquals
import org.junit.Test

class GameTitlesTest {
    @Test
    fun `display title strips tags and extension`() {
        assertEquals("Super Mario Bros. 3", GameTitles.displayTitle("Super Mario Bros. 3 (USA) (Rev 1) [!].nes"))
        assertEquals("Pokemon - Emerald Version", GameTitles.displayTitle("Pokemon - Emerald Version (USA, Europe).gba"))
        assertEquals("Street Fighter II", GameTitles.displayTitle("Street_Fighter_II.zip"))
    }

    @Test
    fun `numbered pack prefixes are removed`() {
        assertEquals("魂斗罗", GameTitles.displayTitle("0012 - 魂斗罗.nes"))
        assertEquals("1942", GameTitles.displayTitle("1942.nes"))
    }

    @Test
    fun `title never becomes empty`() {
        assertEquals("(Beta)", GameTitles.displayTitle("(Beta).gba"))
    }

    @Test
    fun `tags are listed in order`() {
        assertEquals(listOf("USA", "Rev 1"), GameTitles.tags("Game (USA) (Rev 1) [!].sfc"))
    }

    @Test
    fun `thumbnail url follows libretro naming rules`() {
        assertEquals(
            "https://thumbnails.libretro.com/Nintendo%20-%20Game%20Boy%20Advance/Named_Boxarts/" +
                "Mario%20_%20Luigi%20-%20Superstar%20Saga%20%28USA%29.png",
            GameTitles.thumbnailUrl(Platform.GBA, "Mario & Luigi - Superstar Saga (USA).gba"),
        )
    }

    @Test
    fun `thumbnail system depends on extension for monochrome handhelds`() {
        assertEquals(
            "https://thumbnails.libretro.com/SNK%20-%20Neo%20Geo%20Pocket/Named_Boxarts/Game.png",
            GameTitles.thumbnailUrl(Platform.NGP, "Game.ngp"),
        )
    }

    @Test
    fun `non ascii names are utf8 encoded`() {
        assertEquals("%E9%AD%82", GameTitles.encodePathSegment("魂"))
    }
}
