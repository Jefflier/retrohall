package com.retrohall.core

/**
 * Libretro cores used by the app. Every core is a native shared library that is
 * downloaded on demand and loaded by LibretroDroid.
 */
enum class EmulatorCore(
    /** Name used by the libretro buildbot and the LemuroidCores repository. */
    val coreName: String,
    val displayName: String,
    /** Rough download size, shown before the user downloads the core. */
    val approxSizeMb: Int,
    val statesSupported: Boolean = true,
) {
    FCEUMM("fceumm", "FCEUmm", 4),
    SNES9X("snes9x", "Snes9x", 3),
    GAMBATTE("gambatte", "Gambatte", 5),
    MGBA("mgba", "mGBA", 3),
    MELONDS("melonds", "melonDS", 6),
    MUPEN64PLUS_NEXT("mupen64plus_next_gles3", "Mupen64Plus-Next", 6),
    GENESIS_PLUS_GX("genesis_plus_gx", "Genesis Plus GX", 13),
    PCSX_REARMED("pcsx_rearmed", "PCSX ReARMed", 2),
    MEDNAFEN_PCE_FAST("mednafen_pce_fast", "Beetle PCE Fast", 4),
    FBNEO("fbneo", "FinalBurn Neo", 63),
    MEDNAFEN_NGP("mednafen_ngp", "Beetle NeoPop", 1),
    MEDNAFEN_WSWAN("mednafen_wswan", "Beetle Cygne", 2),
    STELLA("stella", "Stella", 6),
    ;

    /** File name LibretroDroid loads, e.g. `libmgba_libretro_android.so`. */
    val libraryFileName: String get() = "lib${coreName}_libretro_android.so"

    /** Platforms that run on this core. */
    val platforms: List<Platform> get() = Platform.entries.filter { it.core == this }

    companion object {
        /** Cores worth downloading first: they cover the most popular handheld and 8/16-bit systems. */
        val ESSENTIALS = listOf(FCEUMM, SNES9X, GAMBATTE, MGBA, GENESIS_PLUS_GX)
    }
}
