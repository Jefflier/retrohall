package com.retrohall.app.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.retrohall.app.data.AppSettings

@Composable
fun GameScreen(state: GameUiState, settings: AppSettings, controller: GameController) {
    BackHandler { controller.onBack() }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val portrait = maxHeight > maxWidth
        val game = state.game
        val aspectRatio = game?.platform?.aspectRatio ?: (4f / 3f)
        val cutoutTop = if (portrait) WindowInsets.displayCutout.asPaddingValues().calculateTopPadding() else 0.dp
        val pictureHeight = if (portrait) minOf(maxWidth / aspectRatio, maxHeight * 0.6f) else maxHeight

        state.retroView?.let { view ->
            // The same view instance must stay in the same place in the tree: moving it would
            // detach it from the window and stop the GL thread.
            AndroidView(
                factory = { view },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = cutoutTop)
                    .fillMaxWidth()
                    .height(pictureHeight),
            )
        }

        if (state.phase is GamePhase.Running && game != null && !state.menuOpen) {
            if (state.showPad) {
                VirtualGamepad(
                    layout = game.platform.layout,
                    portrait = portrait,
                    appearance = PadAppearance(settings.padOpacity, settings.padScale, settings.haptics),
                    fastForward = state.fastForward,
                    onKey = controller::onVirtualKey,
                    onStick = controller::onVirtualStick,
                    onMenu = controller::openMenu,
                    onFastForward = controller::toggleFastForward,
                    modifier = if (portrait) {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(maxHeight - pictureHeight - cutoutTop)
                    } else {
                        Modifier.fillMaxSize()
                    },
                )
            } else {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable(onClick = controller::openMenu),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Menu, contentDescription = "菜单", tint = Color.White)
                }
            }
        }

        when (val phase = state.phase) {
            is GamePhase.Loading -> Column(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(phase.message, color = Color.White)
            }

            is GamePhase.Failed -> Column(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text(phase.message, color = Color.White, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(24.dp))
                Button(onClick = controller::exitGame) { Text("返回游戏库") }
            }

            GamePhase.Running -> Unit
        }

        if (state.menuOpen && game != null) {
            GameMenu(state = state, settings = settings, controller = controller)
        }
    }
}
