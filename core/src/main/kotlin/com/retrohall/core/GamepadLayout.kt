package com.retrohall.core

/** Buttons of the libretro RetroPad. Positions follow the SNES layout: B bottom, A right, Y left, X top. */
enum class RetroKey { A, B, X, Y, L1, L2, R1, R2, L3, R3, SELECT, START }

/** What a directional control sends to the core. */
enum class StickMode {
    /** Eight-way digital pad. */
    DPAD,

    /** Analog stick (N64). */
    ANALOG_LEFT,

    /** Right analog stick, used for the N64 C-buttons. */
    ANALOG_RIGHT,
}

/** A virtual button. [turbo] buttons repeatedly press and release [key] while held (连发). */
data class PadButton(val label: String, val key: RetroKey, val turbo: Boolean = false)

/** A face button placed at an offset, measured in button diameters, from the centre of its cluster. */
data class FaceButton(val button: PadButton, val x: Float, val y: Float)

/** A small digital cross that drives a stick, e.g. the N64 C-buttons. */
data class SecondaryStick(val label: String, val mode: StickMode)

data class GamepadLayout(
    val stick: StickMode = StickMode.DPAD,
    val face: List<FaceButton>,
    val leftShoulder: List<PadButton> = emptyList(),
    val rightShoulder: List<PadButton> = emptyList(),
    val select: PadButton? = PadButton("SELECT", RetroKey.SELECT),
    val start: PadButton? = PadButton("START", RetroKey.START),
    val secondaryStick: SecondaryStick? = null,
) {
    /** Every key this layout can press, used to release everything when the pad is hidden. */
    val allKeys: Set<RetroKey>
        get() = buildSet {
            face.forEach { add(it.button.key) }
            leftShoulder.forEach { add(it.key) }
            rightShoulder.forEach { add(it.key) }
            select?.let { add(it.key) }
            start?.let { add(it.key) }
        }
}

/**
 * Touch layouts for each platform. Button-to-RetroPad assignments follow each core's
 * documented default mapping, so physical controllers and the touch pad agree.
 */
object GamepadLayouts {
    private fun button(label: String, key: RetroKey, turbo: Boolean = false) = PadButton(label, key, turbo)

    private fun diamond(top: PadButton, left: PadButton, right: PadButton, bottom: PadButton) = listOf(
        FaceButton(top, 0f, -1.05f),
        FaceButton(left, -1.05f, 0f),
        FaceButton(right, 1.05f, 0f),
        FaceButton(bottom, 0f, 1.05f),
    )

    /** Two buttons on a diagonal, the left one lower, like a Game Boy. */
    private fun pair(left: PadButton, right: PadButton) = listOf(
        FaceButton(left, -0.65f, 0.3f),
        FaceButton(right, 0.65f, -0.3f),
    )

    private val L = button("L", RetroKey.L1)
    private val R = button("R", RetroKey.R1)

    val NES = GamepadLayout(
        face = listOf(
            FaceButton(button("B", RetroKey.B), -0.65f, 0.55f),
            FaceButton(button("A", RetroKey.A), 0.65f, 0.25f),
            FaceButton(button("TB", RetroKey.B, turbo = true), -0.65f, -0.7f),
            FaceButton(button("TA", RetroKey.A, turbo = true), 0.65f, -1.0f),
        ),
    )

    val SNES = GamepadLayout(
        face = diamond(
            top = button("X", RetroKey.X),
            left = button("Y", RetroKey.Y),
            right = button("A", RetroKey.A),
            bottom = button("B", RetroKey.B),
        ),
        leftShoulder = listOf(L),
        rightShoulder = listOf(R),
    )

    val GAME_BOY = GamepadLayout(
        face = pair(button("B", RetroKey.B), button("A", RetroKey.A)),
    )

    val GBA = GAME_BOY.copy(leftShoulder = listOf(L), rightShoulder = listOf(R))

    val NDS = SNES

    val N64 = GamepadLayout(
        stick = StickMode.ANALOG_LEFT,
        face = listOf(
            FaceButton(button("B", RetroKey.Y), -0.9f, -0.45f),
            FaceButton(button("A", RetroKey.B), 0.3f, 0.35f),
        ),
        leftShoulder = listOf(button("Z", RetroKey.L2), L),
        rightShoulder = listOf(R),
        select = null,
        secondaryStick = SecondaryStick("C", StickMode.ANALOG_RIGHT),
    )

    // Genesis Plus GX: RetroPad Y = A, B = B, A = C.
    val MEGA_DRIVE = GamepadLayout(
        face = listOf(
            FaceButton(button("A", RetroKey.Y), -1.2f, 0.35f),
            FaceButton(button("B", RetroKey.B), 0f, 0f),
            FaceButton(button("C", RetroKey.A), 1.2f, -0.35f),
        ),
        select = button("MODE", RetroKey.SELECT),
    )

    // Genesis Plus GX: RetroPad B = 1, A = 2. START is the pause button.
    val MASTER_SYSTEM = GamepadLayout(
        face = pair(button("1", RetroKey.B), button("2", RetroKey.A)),
        select = null,
    )

    val GAME_GEAR = MASTER_SYSTEM

    // Beetle PCE Fast: RetroPad A = I, B = II.
    val PC_ENGINE = GamepadLayout(
        face = pair(button("II", RetroKey.B), button("I", RetroKey.A)),
        start = button("RUN", RetroKey.START),
    )

    val PLAYSTATION = GamepadLayout(
        face = diamond(
            top = button("△", RetroKey.X),
            left = button("□", RetroKey.Y),
            right = button("○", RetroKey.A),
            bottom = button("✕", RetroKey.B),
        ),
        leftShoulder = listOf(button("L1", RetroKey.L1), button("L2", RetroKey.L2)),
        rightShoulder = listOf(button("R1", RetroKey.R1), button("R2", RetroKey.R2)),
    )

    // FinalBurn Neo: B, A, Y, X, L, R are buttons 1-6 (Neo Geo A-D are the first four).
    val ARCADE = GamepadLayout(
        face = listOf(
            FaceButton(button("A", RetroKey.B), -1.15f, 0.6f),
            FaceButton(button("B", RetroKey.A), 0f, 0.45f),
            FaceButton(button("F", RetroKey.R1), 1.15f, 0.3f),
            FaceButton(button("C", RetroKey.Y), -1.15f, -0.6f),
            FaceButton(button("D", RetroKey.X), 0f, -0.75f),
            FaceButton(button("E", RetroKey.L1), 1.15f, -0.9f),
        ),
        select = button("投币", RetroKey.SELECT),
        start = button("开始", RetroKey.START),
    )

    // Beetle NeoPop: RetroPad B = A, A = B.
    val NEO_GEO_POCKET = GamepadLayout(
        face = pair(button("A", RetroKey.B), button("B", RetroKey.A)),
        select = null,
        start = button("OPTION", RetroKey.START),
    )

    val WONDERSWAN = GamepadLayout(
        face = pair(button("B", RetroKey.B), button("A", RetroKey.A)),
        select = null,
    )

    val ATARI_2600 = GamepadLayout(
        face = listOf(FaceButton(button("FIRE", RetroKey.B), 0f, 0f)),
        start = button("RESET", RetroKey.START),
    )
}
