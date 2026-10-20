package com.retrohall.app.game

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.retrohall.app.data.AppSettings
import com.retrohall.app.data.SaveSlot
import com.retrohall.app.ui.components.ConfirmDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GameMenu(state: GameUiState, settings: AppSettings, controller: GameController) {
    val game = state.game ?: return
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            // Swallow touches so they do not reach the game underneath.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 640.dp),
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.menuPage != MenuPage.MAIN) {
                        IconButton(onClick = { state.menuPage = MenuPage.MAIN }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(state.menuPage.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "${game.platform.shortName} · ${game.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = controller::closeMenu) { Icon(Icons.Filled.Close, contentDescription = "关闭菜单") }
                }
                Spacer(Modifier.height(16.dp))
                when (state.menuPage) {
                    MenuPage.MAIN -> MainPage(state, settings, controller)
                    MenuPage.SAVE -> SlotsPage(state.slots.filterNot { it.isAuto }, saving = true, controller = controller)
                    MenuPage.LOAD -> SlotsPage(state.slots, saving = false, controller = controller)
                    MenuPage.CHEATS -> CheatsPage(state, controller)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainPage(state: GameUiState, settings: AppSettings, controller: GameController) {
    var confirmReset by remember { mutableStateOf(false) }
    val statesSupported = state.game?.platform?.core?.statesSupported ?: false
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        MenuTile(Icons.Filled.PlayArrow, "继续游戏", onClick = controller::closeMenu)
        MenuTile(Icons.Filled.Save, "保存进度", enabled = statesSupported) { state.menuPage = MenuPage.SAVE }
        MenuTile(Icons.Filled.History, "读取进度", enabled = statesSupported) { state.menuPage = MenuPage.LOAD }
        MenuTile(
            Icons.Filled.FastForward,
            if (state.fastForward) "快进中 ${settings.fastForwardSpeed}×" else "快进 ${settings.fastForwardSpeed}×",
            highlighted = state.fastForward,
            onClick = controller::toggleFastForward,
        )
        MenuTile(Icons.Filled.AutoFixHigh, "金手指") { state.menuPage = MenuPage.CHEATS }
        MenuTile(Icons.Filled.Tune, state.shader.label, onClick = controller::cycleShader)
        MenuTile(
            Icons.Filled.SportsEsports,
            if (state.showPad) "隐藏按键" else "显示按键",
            highlighted = state.showPad,
            onClick = controller::togglePad,
        )
        val (diskCount, currentDisk) = state.disks
        if (diskCount > 1) {
            MenuTile(Icons.Filled.Album, "光盘 ${currentDisk + 1}/$diskCount", onClick = controller::nextDisk)
        }
        MenuTile(Icons.Filled.RestartAlt, "重启游戏") { confirmReset = true }
        MenuTile(Icons.AutoMirrored.Filled.ExitToApp, "退出游戏", onClick = controller::exitGame)
    }
    if (confirmReset) {
        ConfirmDialog(
            title = "重启游戏？",
            message = "未保存的进度将会丢失。",
            confirmLabel = "重启",
            onConfirm = controller::resetGame,
            onDismiss = { confirmReset = false },
        )
    }
}

@Composable
private fun MenuTile(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(width = 108.dp, height = 84.dp),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(Modifier.height(6.dp))
            Text(label, color = tint, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlotsPage(slots: List<SaveSlot>, saving: Boolean, controller: GameController) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        slots.forEach { slot ->
            SlotCard(
                slot = slot,
                enabled = saving || slot.exists,
                onClick = { if (saving) controller.saveToSlot(slot.index) else controller.loadFromSlot(slot.index) },
            )
        }
    }
    if (!saving && slots.none { it.exists }) {
        Text(
            "还没有存档。在游戏中打开菜单选择「保存进度」即可存档。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun SlotCard(slot: SaveSlot, enabled: Boolean, onClick: () -> Unit) {
    val thumbnail = remember(slot.screenshot, slot.savedAt) {
        slot.screenshot?.let { file -> BitmapFactory.decodeFile(file.path)?.asImageBitmap() }
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.width(150.dp),
    ) {
        Column(Modifier.padding(8.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                if (thumbnail != null) {
                    Image(thumbnail, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                } else {
                    Text(if (slot.exists) "无截图" else "空", color = Color.Gray)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (slot.isAuto) "自动存档" else "存档 ${slot.index}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (slot.exists) formatTime(slot.savedAt) else "空栏位",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CheatsPage(state: GameUiState, controller: GameController) {
    var adding by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let(controller::importCheats)
    }
    Column {
        if (state.cheats.isEmpty()) {
            Text(
                "还没有金手指。可手动输入代码（支持 Game Genie、Pro Action Replay、GameShark 等格式，取决于核心），" +
                    "或导入 RetroArch 的 .cht 金手指文件。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.cheats.forEach { cheat ->
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(cheat.description, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        cheat.code,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Switch(checked = cheat.enabled, onCheckedChange = { controller.setCheatEnabled(cheat, it) })
                IconButton(onClick = { controller.deleteCheat(cheat) }) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "删除")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { adding = true }) { Text("添加金手指") }
            OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }) { Text("导入 .cht 文件") }
        }
    }
    if (adding) {
        AddCheatDialog(
            onAdd = { description, code -> controller.addCheat(description, code) },
            onDismiss = { adding = false },
        )
    }
}

@Composable
private fun AddCheatDialog(onAdd: (String, String) -> Unit, onDismiss: () -> Unit) {
    var description by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加金手指") },
        text = {
            Column {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("名称（如：无限生命）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("代码") },
                    placeholder = { Text("每行一段，多行会合并为一条") },
                    minLines = 3,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "添加后立即生效，可随时在列表中开关。",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = code.isNotBlank(),
                onClick = {
                    onAdd(description, code)
                    onDismiss()
                },
            ) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun formatTime(millis: Long): String = SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(millis))
