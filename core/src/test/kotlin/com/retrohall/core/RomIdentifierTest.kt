package com.retrohall.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RomIdentifierTest {
    private fun identify(
        name: String,
        folders: List<String> = emptyList(),
        zip: List<String>? = null,
        header: ByteArray? = null,
    ) = RomIdentifier.identify(RomCandidate(name, folders = folders, zipEntries = zip, header = header))

    @Test
    fun `unique extensions map straight to a platform`() {
        assertEquals(Identified(Platform.FC), identify("Contra (Japan).nes"))
        assertEquals(Identified(Platform.SFC), identify("Super Metroid.SFC"))
        assertEquals(Identified(Platform.GBA), identify("Pokemon - Emerald Version (USA).gba"))
        assertEquals(Identified(Platform.GBC), identify("zelda.gbc"))
        assertEquals(Identified(Platform.NDS), identify("game.nds"))
        assertEquals(Identified(Platform.N64), identify("Mario Kart 64.z64"))
        assertEquals(Identified(Platform.MD), identify("Sonic.md"))
        assertEquals(Identified(Platform.PS1), identify("Final Fantasy VII (Disc 1).cue"))
        assertEquals(Identified(Platform.PS1), identify("Crash.chd"))
        assertEquals(Identified(Platform.WS), identify("game.wsc"))
    }

    @Test
    fun `unknown extensions are ignored`() {
        assertNull(identify("readme.txt"))
        assertNull(identify("cover.png"))
        assertNull(identify("noextension"))
    }

    @Test
    fun `zipped cartridge launches the rom inside`() {
        assertEquals(
            Identified(Platform.GBA, "Metroid Fusion (USA).gba"),
            identify("Metroid Fusion (USA).zip", zip = listOf("readme.txt", "Metroid Fusion (USA).gba")),
        )
        assertEquals(
            Identified(Platform.FC, "smb.nes"),
            identify("smb.zip", zip = listOf("smb.nes")),
        )
    }

    @Test
    fun `zip without a known rom is an arcade set`() {
        assertEquals(Identified(Platform.ARCADE), identify("kof98.zip", zip = listOf("242-p1.p1", "242-p2.sp2")))
        assertEquals(Identified(Platform.ARCADE), identify("sf2.zip"))
    }

    @Test
    fun `arcade bios archives are not games`() {
        assertNull(identify("neogeo.zip", zip = listOf("sp-s2.sp1")))
        assertNull(identify("PGM.zip"))
    }

    @Test
    fun `zip in a platform folder without a rom is skipped instead of treated as arcade`() {
        assertNull(identify("disc.zip", folders = listOf("PS1"), zip = listOf("disc.cue", "disc.bin")))
    }

    @Test
    fun `zipped bin uses the folder hint`() {
        assertEquals(
            Identified(Platform.MD, "sonic.bin"),
            identify("sonic.zip", folders = listOf("Genesis"), zip = listOf("sonic.bin")),
        )
    }

    @Test
    fun `mega drive bin is recognised by its header`() {
        val header = ByteArray(0x200)
        "SEGA MEGA DRIVE".forEachIndexed { i, c -> header[0x100 + i] = c.code.toByte() }
        assertEquals(Identified(Platform.MD), identify("streets.bin", header = header))
    }

    @Test
    fun `playstation discs are recognised by the volume descriptor`() {
        val cooked = ByteArray(RomIdentifier.HEADER_SIZE)
        "PLAYSTATION".forEachIndexed { i, c -> cooked[0x8008 + i] = c.code.toByte() }
        assertEquals(Identified(Platform.PS1), identify("game.iso", header = cooked))

        val raw = ByteArray(RomIdentifier.HEADER_SIZE)
        "PLAYSTATION".forEachIndexed { i, c -> raw[0x9320 + i] = c.code.toByte() }
        assertEquals(Identified(Platform.PS1), identify("game.bin", header = raw))
    }

    @Test
    fun `psp iso is not claimed`() {
        val header = ByteArray(RomIdentifier.HEADER_SIZE)
        "PSP GAME".forEachIndexed { i, c -> header[0x8008 + i] = c.code.toByte() }
        assertNull(identify("game.iso", folders = listOf("ps1"), header = header))
    }

    @Test
    fun `ambiguous files fall back to folder hints`() {
        assertEquals(Identified(Platform.PS1), identify("game.iso", folders = listOf("PlayStation")))
        assertEquals(Identified(Platform.A2600), identify("pitfall.bin", folders = listOf("Atari 2600", "roms")))
        assertNull(identify("track 02.bin", folders = listOf("downloads")))
    }

    @Test
    fun `folder hints ignore case and separators`() {
        assertEquals(Platform.GBA, RomIdentifier.hintedPlatform(listOf("Game_Boy_Advance")))
        assertEquals(Platform.MD, RomIdentifier.hintedPlatform(listOf("Mega-Drive")))
        assertEquals(Platform.ARCADE, RomIdentifier.hintedPlatform(listOf("roms", "街机")))
        assertNull(RomIdentifier.hintedPlatform(listOf("Download")))
    }
}
