package com.retrohall.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {
    @Test
    fun `extensions are unique across platforms`() {
        val all = Platform.entries.flatMap { it.extensions }
        assertEquals(all.size, all.toSet().size)
    }

    @Test
    fun `every core serves at least one platform`() {
        EmulatorCore.entries.forEach { core ->
            assertTrue("${core.name} has no platform", core.platforms.isNotEmpty())
        }
    }

    @Test
    fun `layouts never assign two face buttons to the same spot`() {
        Platform.entries.forEach { platform ->
            val spots = platform.layout.face.map { it.x to it.y }
            assertEquals(platform.name, spots.size, spots.toSet().size)
        }
    }

    @Test
    fun `core urls`() {
        assertEquals(
            "https://raw.githubusercontent.com/Swordfish90/LemuroidCores/1.17.0/" +
                "lemuroid_core_mgba/src/main/jniLibs/arm64-v8a/libmgba_libretro_android.so",
            CoreDownloads.githubUrl(EmulatorCore.MGBA, "arm64-v8a"),
        )
        assertEquals(
            "https://ghfast.top/https://raw.githubusercontent.com/Swordfish90/LemuroidCores/1.17.0/" +
                "lemuroid_core_fceumm/src/main/jniLibs/armeabi-v7a/libfceumm_libretro_android.so",
            CoreDownloads.githubUrl(EmulatorCore.FCEUMM, "armeabi-v7a", "https://ghfast.top"),
        )
        assertEquals(
            "https://buildbot.libretro.com/nightly/android/latest/arm64-v8a/snes9x_libretro_android.so.zip",
            CoreDownloads.buildbotUrl(EmulatorCore.SNES9X, "arm64-v8a"),
        )
    }

    @Test
    fun `preferred abi skips unsupported ones`() {
        assertEquals("arm64-v8a", CoreDownloads.preferredAbi(listOf("riscv64", "arm64-v8a", "armeabi-v7a")))
    }

    @Test
    fun `disc sheets list their tracks`() {
        val cue = """
            FILE "Game (Track 1).bin" BINARY
              TRACK 01 MODE2/2352
            FILE "sub\Game (Track 2).bin" BINARY
              TRACK 02 AUDIO
        """.trimIndent()
        assertEquals(listOf("Game (Track 1).bin", "Game (Track 2).bin"), DiscFiles.referencedFiles("game.cue", cue))
        assertEquals(
            listOf("Disc 1.cue", "Disc 2.cue"),
            DiscFiles.referencedFiles("game.m3u", "#EXTM3U\nDisc 1.cue\n\nDisc 2.cue\n"),
        )
    }

    @Test
    fun `durations read naturally`() {
        assertEquals("不到 1 分钟", Formatters.duration(30_000))
        assertEquals("45 分钟", Formatters.duration(45 * 60_000L))
        assertEquals("2 小时", Formatters.duration(120 * 60_000L))
        assertEquals("1 小时 5 分钟", Formatters.duration(65 * 60_000L))
    }
}
