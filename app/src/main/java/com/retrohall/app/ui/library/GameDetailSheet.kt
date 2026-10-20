package com.retrohall.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.retrohall.app.data.CoreState
import com.retrohall.app.data.Game
import com.retrohall.app.ui.components.ConfirmDialog
import com.retrohall.app.ui.components.GameCover
import com.retrohall.app.ui.components.PlatformTag
import com.retrohall.core.Formatters
import com.retrohall.core.GameTitles
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailSheet(
    game: Game,
    coreState: CoreState,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onChangeCover: () -> Unit,
    onRemove: () -> Unit,
) {
    var confirmRemove by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Row {
                GameCover(
                    game,
                    Modifier
                        .width(112.dp)
                        .aspectRatio(0.78f)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(game.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    PlatformTag(game.platform)
                    val tags = GameTitles.tags(game.fileName)
                    if (tags.isNotEmpty()) {
                        Text(
                            tags.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${game.fileName}\n${Formatters.size(game.sizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat("游玩次数", "${game.playCount}")
                Stat("游玩时长", if (game.playTimeMs > 0) Formatters.duration(game.playTimeMs) else "—")
                Stat("最近游玩", if (game.lastPlayed > 0) formatDate(game.lastPlayed) else "—")
            }

            Spacer(Modifier.height(20.dp))
            PlayButton(game, coreState, onPlay)

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onToggleFavorite, modifier = Modifier.weight(1f)) {
                    Icon(
                        if (game.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (game.favorite) Color(0xFFFF5370) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (game.favorite) "已收藏" else "收藏")
                }
                OutlinedButton(onClick = onChangeCover, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("封面")
                }
                OutlinedButton(onClick = { confirmRemove = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("移除")
                }
            }
        }
    }

    if (confirmRemove) {
        ConfirmDialog(
            title = "移除《${game.title}》？",
            message = if (game.isImported) {
                "将删除导入到应用内的 ROM 文件，存档会保留。"
            } else {
                "只从游戏库移除，不会删除原文件；重新扫描文件夹后它会再次出现。存档会保留。"
            },
            confirmLabel = "移除",
            onConfirm = onRemove,
            onDismiss = { confirmRemove = false },
        )
    }
}

@Composable
private fun PlayButton(game: Game, coreState: CoreState, onPlay: () -> Unit) {
    val core = game.platform.core
    Button(
        onClick = onPlay,
        enabled = coreState !is CoreState.Downloading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        when (coreState) {
            is CoreState.Installed -> {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("开始游戏", style = MaterialTheme.typography.titleMedium)
            }
            is CoreState.Downloading -> Text(
                if (coreState.progress >= 0) "正在下载 ${core.displayName} 核心 ${(coreState.progress * 100).toInt()}%" else "正在下载核心…",
            )
            is CoreState.Failed -> {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("重新下载核心并开始")
            }
            CoreState.Missing -> {
                Icon(Icons.Filled.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("下载 ${core.displayName} 核心并开始（约 ${core.approxSizeMb} MB）")
            }
        }
    }
    when (coreState) {
        is CoreState.Downloading -> LinearProgressIndicator(
            progress = { coreState.progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        is CoreState.Failed -> Text(
            "核心下载失败：${coreState.message}\n可在「设置 → 核心下载源」中更换下载源或填写 GitHub 镜像。",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        else -> Unit
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatDate(millis: Long): String = SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(millis))
