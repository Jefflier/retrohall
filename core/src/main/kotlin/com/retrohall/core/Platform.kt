package com.retrohall.core

import com.retrohall.core.EmulatorCore.FBNEO
import com.retrohall.core.EmulatorCore.FCEUMM
import com.retrohall.core.EmulatorCore.GAMBATTE
import com.retrohall.core.EmulatorCore.GENESIS_PLUS_GX
import com.retrohall.core.EmulatorCore.MEDNAFEN_NGP
import com.retrohall.core.EmulatorCore.MEDNAFEN_PCE_FAST
import com.retrohall.core.EmulatorCore.MEDNAFEN_WSWAN
import com.retrohall.core.EmulatorCore.MELONDS
import com.retrohall.core.EmulatorCore.MGBA
import com.retrohall.core.EmulatorCore.MUPEN64PLUS_NEXT
import com.retrohall.core.EmulatorCore.PCSX_REARMED
import com.retrohall.core.EmulatorCore.SNES9X
import com.retrohall.core.EmulatorCore.STELLA

/**
 * A BIOS / firmware file a core may look for in the system directory.
 *
 * @param required true when no game can start without it; false when the core has a built-in replacement
 *   or only some games need it.
 * @param directory subdirectory of the system directory the core searches.
 */
data class BiosFile(
    val fileName: String,
    val description: String,
    val required: Boolean = false,
    val directory: String = "",
)

/**
 * A game system. The enum name is stored in the database, so entries must never be renamed.
 */
enum class Platform(
    val shortName: String,
    val chineseName: String,
    val fullName: String,
    val core: EmulatorCore,
    /** File extensions that identify this platform on their own (lower case, without the dot). */
    val extensions: Set<String>,
    /** Folder names that hint at this platform when the extension is ambiguous (`.bin`, `.iso`, `.zip`). */
    val folderHints: Set<String>,
    /** Directory name in the libretro-thumbnails repository. */
    val thumbnailSystem: String,
    /** Brand colour used for cards and placeholders, as 0xAARRGGBB. */
    val color: Long,
    val layout: GamepadLayout,
    /** Width / height of the picture, used to size the game view in portrait. */
    val aspectRatio: Float,
    /** Core options applied when the game starts. */
    val coreVariables: Map<String, String> = emptyMap(),
    val bios: List<BiosFile> = emptyList(),
) {
    FC(
        shortName = "FC",
        chineseName = "红白机",
        fullName = "Nintendo Entertainment System / Famicom",
        core = FCEUMM,
        extensions = setOf("nes", "fds", "unf", "unif"),
        folderHints = setOf("fc", "nes", "famicom", "红白机"),
        thumbnailSystem = "Nintendo - Nintendo Entertainment System",
        color = 0xFFE53935,
        layout = GamepadLayouts.NES,
        aspectRatio = 4f / 3f,
        bios = listOf(BiosFile("disksys.rom", "FDS 磁碟机 BIOS（仅 .fds 游戏需要）")),
    ),
    SFC(
        shortName = "SFC",
        chineseName = "超级任天堂",
        fullName = "Super Nintendo / Super Famicom",
        core = SNES9X,
        extensions = setOf("sfc", "smc", "swc", "fig"),
        folderHints = setOf("sfc", "snes", "superfamicom", "super famicom", "超任"),
        thumbnailSystem = "Nintendo - Super Nintendo Entertainment System",
        color = 0xFF7E57C2,
        layout = GamepadLayouts.SNES,
        aspectRatio = 4f / 3f,
    ),
    GB(
        shortName = "GB",
        chineseName = "Game Boy",
        fullName = "Nintendo Game Boy",
        core = GAMBATTE,
        extensions = setOf("gb"),
        folderHints = setOf("gb", "gameboy", "game boy"),
        thumbnailSystem = "Nintendo - Game Boy",
        color = 0xFF7CB342,
        layout = GamepadLayouts.GAME_BOY,
        aspectRatio = 10f / 9f,
        coreVariables = mapOf(
            "gambatte_gb_colorization" to "internal",
            "gambatte_gb_internal_palette" to "GB - Pocket",
        ),
    ),
    GBC(
        shortName = "GBC",
        chineseName = "Game Boy Color",
        fullName = "Nintendo Game Boy Color",
        core = GAMBATTE,
        extensions = setOf("gbc"),
        folderHints = setOf("gbc", "gameboycolor", "game boy color"),
        thumbnailSystem = "Nintendo - Game Boy Color",
        color = 0xFF00ACC1,
        layout = GamepadLayouts.GAME_BOY,
        aspectRatio = 10f / 9f,
    ),
    GBA(
        shortName = "GBA",
        chineseName = "Game Boy Advance",
        fullName = "Nintendo Game Boy Advance",
        core = MGBA,
        extensions = setOf("gba"),
        folderHints = setOf("gba", "gameboyadvance", "game boy advance"),
        thumbnailSystem = "Nintendo - Game Boy Advance",
        color = 0xFF3949AB,
        layout = GamepadLayouts.GBA,
        aspectRatio = 3f / 2f,
        bios = listOf(BiosFile("gba_bios.bin", "GBA BIOS（可选，核心自带 HLE BIOS）")),
    ),
    NDS(
        shortName = "NDS",
        chineseName = "任天堂 DS",
        fullName = "Nintendo DS",
        core = MELONDS,
        extensions = setOf("nds"),
        folderHints = setOf("nds", "ds"),
        thumbnailSystem = "Nintendo - Nintendo DS",
        color = 0xFF546E7A,
        layout = GamepadLayouts.NDS,
        aspectRatio = 2f / 3f,
        coreVariables = mapOf(
            "melonds_number_of_screen_layouts" to "1",
            "melonds_touch_mode" to "Touch",
            "melonds_threaded_renderer" to "enabled",
        ),
        bios = listOf(
            BiosFile("bios7.bin", "ARM7 BIOS（可选，核心自带 FreeBIOS）"),
            BiosFile("bios9.bin", "ARM9 BIOS（可选）"),
            BiosFile("firmware.bin", "DS 固件（可选）"),
        ),
    ),
    N64(
        shortName = "N64",
        chineseName = "任天堂 64",
        fullName = "Nintendo 64",
        core = MUPEN64PLUS_NEXT,
        extensions = setOf("n64", "z64", "v64"),
        folderHints = setOf("n64", "nintendo64"),
        thumbnailSystem = "Nintendo - Nintendo 64",
        color = 0xFF43A047,
        layout = GamepadLayouts.N64,
        aspectRatio = 4f / 3f,
        coreVariables = mapOf(
            "mupen64plus-43screensize" to "320x240",
            "mupen64plus-FrameDuping" to "True",
        ),
    ),
    MD(
        shortName = "MD",
        chineseName = "世嘉五代",
        fullName = "Sega Mega Drive / Genesis",
        core = GENESIS_PLUS_GX,
        extensions = setOf("md", "gen", "smd"),
        folderHints = setOf("md", "genesis", "megadrive", "mega drive", "世嘉"),
        thumbnailSystem = "Sega - Mega Drive - Genesis",
        color = 0xFF1E88E5,
        layout = GamepadLayouts.MEGA_DRIVE,
        aspectRatio = 4f / 3f,
    ),
    SMS(
        shortName = "SMS",
        chineseName = "世嘉 Master System",
        fullName = "Sega Master System",
        core = GENESIS_PLUS_GX,
        extensions = setOf("sms"),
        folderHints = setOf("sms", "mastersystem", "master system"),
        thumbnailSystem = "Sega - Master System - Mark III",
        color = 0xFFD81B60,
        layout = GamepadLayouts.MASTER_SYSTEM,
        aspectRatio = 4f / 3f,
    ),
    GG(
        shortName = "GG",
        chineseName = "世嘉 Game Gear",
        fullName = "Sega Game Gear",
        core = GENESIS_PLUS_GX,
        extensions = setOf("gg"),
        folderHints = setOf("gg", "gamegear", "game gear"),
        thumbnailSystem = "Sega - Game Gear",
        color = 0xFF8E24AA,
        layout = GamepadLayouts.GAME_GEAR,
        aspectRatio = 10f / 9f,
    ),
    PS1(
        shortName = "PS",
        chineseName = "PlayStation",
        fullName = "Sony PlayStation",
        core = PCSX_REARMED,
        extensions = setOf("cue", "chd", "pbp", "m3u"),
        folderHints = setOf("ps", "ps1", "psx", "playstation"),
        thumbnailSystem = "Sony - PlayStation",
        color = 0xFF757575,
        layout = GamepadLayouts.PLAYSTATION,
        aspectRatio = 4f / 3f,
        coreVariables = mapOf("pcsx_rearmed_drc" to "disabled"),
        bios = listOf(
            BiosFile("scph5501.bin", "PS BIOS 美版（可选，核心自带 HLE BIOS）"),
            BiosFile("scph5500.bin", "PS BIOS 日版（可选）"),
            BiosFile("scph5502.bin", "PS BIOS 欧版（可选）"),
        ),
    ),
    PCE(
        shortName = "PCE",
        chineseName = "PC Engine",
        fullName = "NEC PC Engine / TurboGrafx-16",
        core = MEDNAFEN_PCE_FAST,
        extensions = setOf("pce"),
        folderHints = setOf("pce", "pcengine", "pc engine", "tg16", "turbografx"),
        thumbnailSystem = "NEC - PC Engine - TurboGrafx 16",
        color = 0xFFFB8C00,
        layout = GamepadLayouts.PC_ENGINE,
        aspectRatio = 4f / 3f,
    ),
    ARCADE(
        shortName = "ARC",
        chineseName = "街机",
        fullName = "Arcade (FinalBurn Neo)",
        core = FBNEO,
        // Arcade sets are plain .zip files; RomIdentifier recognises them by their content.
        extensions = emptySet(),
        folderHints = setOf("arcade", "fba", "fbneo", "fbalpha", "mame", "neogeo", "cps1", "cps2", "cps3", "街机"),
        thumbnailSystem = "FBNeo - Arcade Games",
        color = 0xFFF4511E,
        layout = GamepadLayouts.ARCADE,
        aspectRatio = 4f / 3f,
        bios = listOf(BiosFile("neogeo.zip", "Neo Geo BIOS（玩 Neo Geo 游戏需要）", directory = "fbneo")),
    ),
    NGP(
        shortName = "NGP",
        chineseName = "Neo Geo Pocket",
        fullName = "SNK Neo Geo Pocket / Color",
        core = MEDNAFEN_NGP,
        extensions = setOf("ngp", "ngc"),
        folderHints = setOf("ngp", "ngpc", "neogeopocket"),
        thumbnailSystem = "SNK - Neo Geo Pocket Color",
        color = 0xFF00897B,
        layout = GamepadLayouts.NEO_GEO_POCKET,
        aspectRatio = 20f / 19f,
    ),
    WS(
        shortName = "WS",
        chineseName = "WonderSwan",
        fullName = "Bandai WonderSwan / Color",
        core = MEDNAFEN_WSWAN,
        extensions = setOf("ws", "wsc"),
        folderHints = setOf("ws", "wsc", "wonderswan"),
        thumbnailSystem = "Bandai - WonderSwan Color",
        color = 0xFF6D4C41,
        layout = GamepadLayouts.WONDERSWAN,
        aspectRatio = 14f / 9f,
        coreVariables = mapOf("wswan_rotate_display" to "landscape"),
    ),
    A2600(
        shortName = "2600",
        chineseName = "雅达利 2600",
        fullName = "Atari 2600",
        core = STELLA,
        extensions = setOf("a26"),
        folderHints = setOf("atari", "atari2600", "a2600", "2600"),
        thumbnailSystem = "Atari - 2600",
        color = 0xFF8D6E63,
        layout = GamepadLayouts.ATARI_2600,
        aspectRatio = 4f / 3f,
    ),
    ;

    /** Thumbnail directory for a specific file; monochrome handhelds share an extension-based split. */
    fun thumbnailSystemFor(extension: String): String = when (extension.lowercase()) {
        "ngp" -> "SNK - Neo Geo Pocket"
        "ws" -> "Bandai - WonderSwan"
        else -> thumbnailSystem
    }

    companion object {
        fun fromId(id: String): Platform? = entries.firstOrNull { it.name == id }
    }
}
