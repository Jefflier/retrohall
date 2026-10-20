package com.retrohall.app.game

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.retrohall.core.FaceButton
import com.retrohall.core.GamepadLayout
import com.retrohall.core.PadButton
import com.retrohall.core.RetroKey
import com.retrohall.core.StickMode
import kotlinx.coroutines.delay
import kotlin.math.atan2

data class PadAppearance(val opacity: Float, val scale: Float, val haptics: Boolean)

private val Idle = Color.White.copy(alpha = 0.16f)
private val Pressed = Color.White.copy(alpha = 0.55f)
private val Outline = Color.White.copy(alpha = 0.7f)
private val Label = Color.White.copy(alpha = 0.95f)

/** Eight directions clockwise from east, in screen coordinates (y grows downwards). */
private val Directions = listOf(
    IntOffset(1, 0), IntOffset(1, 1), IntOffset(0, 1), IntOffset(-1, 1),
    IntOffset(-1, 0), IntOffset(-1, -1), IntOffset(0, -1), IntOffset(1, -1),
)

private const val TURBO_HALF_PERIOD_MS = 40L
private const val STICK_UNITS = 2.7f

/**
 * Touch controls. In portrait they fill the area below the picture; in landscape they sit in the
 * corners on top of it. Areas between controls do not consume touches, so the Nintendo DS touch
 * screen underneath keeps working.
 */
@Composable
fun VirtualGamepad(
    layout: GamepadLayout,
    portrait: Boolean,
    appearance: PadAppearance,
    fastForward: Boolean,
    onKey: (RetroKey, Boolean) -> Unit,
    onStick: (StickMode, Float, Float) -> Unit,
    onMenu: () -> Unit,
    onFastForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.alpha(appearance.opacity.coerceIn(0.15f, 1f))) {
        val faceWidthUnits = layout.face.maxOf { it.x } - layout.face.minOf { it.x } + 1f
        val faceHeightUnits = layout.face.maxOf { it.y } - layout.face.minOf { it.y } + 1f
        val rightHeightUnits = faceHeightUnits + if (layout.secondaryStick != null) 1.9f else 0f
        val base = 58.dp * appearance.scale
        val unit = if (portrait) {
            val rowHeight = maxHeight - 24.dp - 44.dp - 44.dp
            minOf(base, rowHeight / maxOf(rightHeightUnits, STICK_UNITS), (maxWidth - 48.dp) / (STICK_UNITS + faceWidthUnits))
        } else {
            minOf(base, (maxHeight - 32.dp - 52.dp) / maxOf(rightHeightUnits, STICK_UNITS))
        }.coerceAtLeast(30.dp)

        val stick: @Composable () -> Unit = { Stick(layout.stick, unit * STICK_UNITS, appearance, onStick) }
        val right: @Composable () -> Unit = { RightCluster(layout, unit, appearance, onKey, onStick) }
        val center: @Composable () -> Unit = {
            CenterButtons(layout, unit, appearance, fastForward, onKey, onMenu, onFastForward)
        }

        if (portrait) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Shoulders(layout.leftShoulder, unit, appearance, onKey)
                    Shoulders(layout.rightShoulder, unit, appearance, onKey)
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    stick()
                    right()
                }
                Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) { center() }
            }
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Box(Modifier.align(Alignment.TopStart)) { Shoulders(layout.leftShoulder, unit, appearance, onKey) }
                Box(Modifier.align(Alignment.TopEnd)) { Shoulders(layout.rightShoulder, unit, appearance, onKey) }
                Box(Modifier.align(Alignment.BottomStart)) { stick() }
                Box(Modifier.align(Alignment.BottomEnd)) { right() }
                Box(Modifier.align(Alignment.BottomCenter)) { center() }
            }
        }
    }
}

@Composable
private fun Stick(mode: StickMode, size: Dp, appearance: PadAppearance, onStick: (StickMode, Float, Float) -> Unit) {
    if (mode == StickMode.DPAD) {
        DirectionPad(size, mode, appearance, onStick)
    } else {
        AnalogStick(size, mode, appearance, onStick)
    }
}

@Composable
private fun RightCluster(
    layout: GamepadLayout,
    unit: Dp,
    appearance: PadAppearance,
    onKey: (RetroKey, Boolean) -> Unit,
    onStick: (StickMode, Float, Float) -> Unit,
) {
    Column(horizontalAlignment = Alignment.End) {
        layout.secondaryStick?.let { secondary ->
            DirectionPad(unit * 1.8f, secondary.mode, appearance, onStick, label = secondary.label)
            Spacer(Modifier.height(unit * 0.1f))
        }
        FaceButtons(layout.face, unit, appearance, onKey)
    }
}

@Composable
private fun Shoulders(buttons: List<PadButton>, unit: Dp, appearance: PadAppearance, onKey: (RetroKey, Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        buttons.forEach { button ->
            HoldButton(
                label = button.label,
                width = unit * 1.3f,
                height = 40.dp,
                shape = RoundedCornerShape(12.dp),
                appearance = appearance,
                onPress = { pressed -> onKey(button.key, pressed) },
            )
        }
    }
}

@Composable
private fun CenterButtons(
    layout: GamepadLayout,
    unit: Dp,
    appearance: PadAppearance,
    fastForward: Boolean,
    onKey: (RetroKey, Boolean) -> Unit,
    onMenu: () -> Unit,
    onFastForward: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        layout.select?.let { button ->
            HoldButton(button.label, unit * 1.15f, 30.dp, RoundedCornerShape(50), appearance) { onKey(button.key, it) }
        }
        RoundIconButton(Icons.Filled.Menu, "菜单", active = false, onClick = onMenu)
        RoundIconButton(Icons.Filled.FastForward, "快进", active = fastForward, onClick = onFastForward)
        layout.start?.let { button ->
            HoldButton(button.label, unit * 1.15f, 30.dp, RoundedCornerShape(50), appearance) { onKey(button.key, it) }
        }
    }
}

@Composable
private fun RoundIconButton(icon: ImageVector, description: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (active) Color(0xCCFFB300) else Idle)
            .border(1.5.dp, Outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Label, modifier = Modifier.size(20.dp))
    }
}

/** A button that reports press and release, for shoulders and SELECT / START. */
@Composable
private fun HoldButton(
    label: String,
    width: Dp,
    height: Dp,
    shape: Shape,
    appearance: PadAppearance,
    onPress: (Boolean) -> Unit,
) {
    val view = LocalView.current
    val currentOnPress by rememberUpdatedState(onPress)
    var pressed by remember { mutableStateOf(false) }
    Box(
        Modifier
            .size(width, height)
            .clip(shape)
            .background(if (pressed) Pressed else Idle)
            .border(1.5.dp, Outline, shape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false).consume()
                    pressed = true
                    currentOnPress(true)
                    if (appearance.haptics) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        pressed = false
                        currentOnPress(false)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * Face buttons share one touch area so a finger can slide between them or press two at once
 * with its edge, as on a real pad.
 */
@Composable
private fun FaceButtons(buttons: List<FaceButton>, unit: Dp, appearance: PadAppearance, onKey: (RetroKey, Boolean) -> Unit) {
    val minX = buttons.minOf { it.x } - 0.5f
    val maxX = buttons.maxOf { it.x } + 0.5f
    val minY = buttons.minOf { it.y } - 0.5f
    val maxY = buttons.maxOf { it.y } + 0.5f
    val view = LocalView.current
    val currentOnKey by rememberUpdatedState(onKey)
    var pressed by remember(buttons) { mutableStateOf(emptySet<Int>()) }

    val turboKeys = pressed.mapNotNull { index -> buttons[index].button.takeIf { it.turbo }?.key }.toSet()
    LaunchedEffect(turboKeys) {
        if (turboKeys.isEmpty()) return@LaunchedEffect
        var down = false
        try {
            while (true) {
                down = !down
                turboKeys.forEach { currentOnKey(it, down) }
                delay(TURBO_HALF_PERIOD_MS)
            }
        } finally {
            turboKeys.forEach { currentOnKey(it, false) }
        }
    }

    Box(
        Modifier
            .size(unit * (maxX - minX), unit * (maxY - minY))
            .pointerInput(buttons, unit) {
                val unitPx = unit.toPx()
                fun hit(position: Offset): Int? {
                    var best: Int? = null
                    var bestDistance = Float.MAX_VALUE
                    buttons.forEachIndexed { index, button ->
                        val center = Offset((button.x - minX) * unitPx, (button.y - minY) * unitPx)
                        val distance = (position - center).getDistance()
                        if (distance < unitPx * 0.62f && distance < bestDistance) {
                            best = index
                            bestDistance = distance
                        }
                    }
                    return best
                }

                fun update(next: Set<Int>) {
                    val current = pressed
                    if (next == current) return
                    (current - next).forEach { index ->
                        val button = buttons[index].button
                        if (!button.turbo) currentOnKey(button.key, false)
                    }
                    val added = next - current
                    added.forEach { index ->
                        val button = buttons[index].button
                        if (!button.turbo) currentOnKey(button.key, true)
                    }
                    if (added.isNotEmpty() && appearance.haptics) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    pressed = next
                }

                awaitPointerEventScope {
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val next = HashSet<Int>()
                            event.changes.forEach { change ->
                                if (change.pressed) hit(change.position)?.let(next::add)
                                change.consume()
                            }
                            update(next)
                        }
                    } finally {
                        update(emptySet())
                    }
                }
            },
    ) {
        val density = LocalDensity.current
        buttons.forEachIndexed { index, button ->
            val label = button.button.label
            val fontScale = if (label.length > 2) 0.24f else 0.36f
            Box(
                Modifier
                    .offset(x = unit * (button.x - minX - 0.5f), y = unit * (button.y - minY - 0.5f))
                    .size(unit)
                    .clip(CircleShape)
                    .background(if (index in pressed) Pressed else Idle)
                    .border(2.dp, Outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = Label,
                    fontWeight = FontWeight.Bold,
                    fontSize = with(density) { (unit * fontScale).toSp() },
                )
            }
        }
    }
}

@Composable
private fun DirectionPad(
    size: Dp,
    mode: StickMode,
    appearance: PadAppearance,
    onStick: (StickMode, Float, Float) -> Unit,
    label: String? = null,
) {
    val view = LocalView.current
    val currentOnStick by rememberUpdatedState(onStick)
    var direction by remember { mutableStateOf(IntOffset.Zero) }

    Box(
        Modifier
            .size(size)
            .pointerInput(mode) {
                fun directionAt(position: Offset): IntOffset {
                    val center = Offset(this.size.width / 2f, this.size.height / 2f)
                    val delta = position - center
                    if (delta.getDistance() < this.size.width * 0.1f) return IntOffset.Zero
                    val degrees = (Math.toDegrees(atan2(delta.y, delta.x).toDouble()) + 360.0) % 360.0
                    return Directions[((degrees + 22.5) / 45.0).toInt() % 8]
                }

                fun update(next: IntOffset) {
                    if (next == direction) return
                    if (next != IntOffset.Zero && appearance.haptics) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    direction = next
                    currentOnStick(mode, next.x.toFloat(), next.y.toFloat())
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    update(directionAt(down.position))
                    try {
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            update(directionAt(change.position))
                        }
                    } finally {
                        update(IntOffset.Zero)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val arm = this.size.minDimension / 3f
            val width = this.size.width
            val height = this.size.height
            val cross = Path().apply {
                moveTo(arm, 0f)
                lineTo(arm * 2, 0f)
                lineTo(arm * 2, arm)
                lineTo(width, arm)
                lineTo(width, arm * 2)
                lineTo(arm * 2, arm * 2)
                lineTo(arm * 2, height)
                lineTo(arm, height)
                lineTo(arm, arm * 2)
                lineTo(0f, arm * 2)
                lineTo(0f, arm)
                lineTo(arm, arm)
                close()
            }
            drawPath(cross, Idle)
            val highlight = Size(arm, arm)
            if (direction.x < 0) drawRect(Pressed, Offset(0f, arm), highlight)
            if (direction.x > 0) drawRect(Pressed, Offset(arm * 2, arm), highlight)
            if (direction.y < 0) drawRect(Pressed, Offset(arm, 0f), highlight)
            if (direction.y > 0) drawRect(Pressed, Offset(arm, arm * 2), highlight)
            drawPath(cross, Outline, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
        }
        label?.let { Text(it, color = Label, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun AnalogStick(size: Dp, mode: StickMode, appearance: PadAppearance, onStick: (StickMode, Float, Float) -> Unit) {
    val view = LocalView.current
    val currentOnStick by rememberUpdatedState(onStick)
    var knob by remember { mutableStateOf(Offset.Zero) }

    Box(
        Modifier
            .size(size)
            .pointerInput(mode) {
                val radius = this.size.width / 2f
                val center = Offset(radius, this.size.height / 2f)
                fun update(position: Offset) {
                    var value = (position - center) / radius
                    val length = value.getDistance()
                    if (length > 1f) value /= length
                    knob = value
                    currentOnStick(mode, value.x, value.y)
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    if (appearance.haptics) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    update(down.position)
                    try {
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            update(change.position)
                        }
                    } finally {
                        knob = Offset.Zero
                        currentOnStick(mode, 0f, 0f)
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            drawCircle(Idle, radius)
            drawCircle(Outline, radius - 1.dp.toPx(), style = Stroke(width = 2.dp.toPx()))
            drawCircle(Pressed, radius * 0.42f, center = center + knob * (radius * 0.55f))
        }
    }
}
