package com.retrohall.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retrohall.app.BuildConfig
import com.retrohall.app.data.AppSettings
import com.retrohall.app.ui.components.ChoiceDialog
import com.retrohall.core.CoreSource
import com.retrohall.core.OrientationOption
import com.retrohall.core.ShaderOption
import kotlin.math.roundToInt

/** A library folder: its persisted tree URI and a readable path. */
data class FolderItem(val uri: String, val path: String)

private enum class SettingsDialog { SHADER, ORIENTATION, SPEED, SOURCE, MIRROR, ABOUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    folders: List<FolderItem>,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onAddFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }

    Column(modifier.fillMaxSize()) {
        TopAppBar(title = { Text("设置", fontWeight = FontWeight.Bold) })
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            group("游戏文件夹")
            items(folders, key = { it.uri }) { folder ->
                ListItem(
                    leadingContent = { Icon(Icons.Filled.Folder, contentDescription = null) },
                    headlineContent = { Text(folder.path) },
                    trailingContent = {
                        IconButton(onClick = { onRemoveFolder(folder.uri) }) {
                            Icon(Icons.Filled.Close, contentDescription = "移除文件夹")
                        }
                    },
                )
            }
            item {
                ListItem(
                    leadingContent = { Icon(Icons.Filled.CreateNewFolder, contentDescription = null) },
                    headlineContent = { Text("添加游戏文件夹") },
                    supportingContent = { Text("子文件夹会一并扫描，文件夹名（如 GBA、街机）可帮助识别游戏平台") },
                    modifier = Modifier.clickable(onClick = onAddFolder),
                )
            }

            group("画面")
            item {
                ValueRow("画面滤镜", settings.shader.label) { dialog = SettingsDialog.SHADER }
                ValueRow("屏幕方向", settings.orientation.label) { dialog = SettingsDialog.ORIENTATION }
            }

            group("虚拟按键")
            item {
                SliderRow(
                    title = "按键透明度",
                    value = settings.padOpacity,
                    range = 0.15f..1f,
                    format = { "${(it * 100).roundToInt()}%" },
                    onChange = { value -> onUpdate { it.copy(padOpacity = value) } },
                )
                SliderRow(
                    title = "按键大小",
                    value = settings.padScale,
                    range = 0.7f..1.4f,
                    format = { "${(it * 100).roundToInt()}%" },
                    onChange = { value -> onUpdate { it.copy(padScale = value) } },
                )
                SwitchRow("触感反馈", "按下虚拟按键时轻微震动", settings.haptics) { value ->
                    onUpdate { it.copy(haptics = value) }
                }
                SwitchRow("连接手柄时隐藏虚拟按键", "检测到实体手柄输入后自动隐藏，可在游戏菜单中重新显示", settings.hidePadWithController) { value ->
                    onUpdate { it.copy(hidePadWithController = value) }
                }
            }

            group("游戏")
            item {
                SwitchRow("退出时自动存档", "退出游戏时保存到「自动存档」栏位", settings.autoSaveOnExit) { value ->
                    onUpdate { it.copy(autoSaveOnExit = value) }
                }
                SwitchRow("启动时继续上次进度", "启动游戏时自动读取「自动存档」", settings.autoLoadOnStart) { value ->
                    onUpdate { it.copy(autoLoadOnStart = value) }
                }
                ValueRow("快进倍速", "${settings.fastForwardSpeed}×") { dialog = SettingsDialog.SPEED }
            }

            group("核心下载")
            item {
                ValueRow("下载源", settings.coreSource.label) { dialog = SettingsDialog.SOURCE }
                ValueRow(
                    "GitHub 加速镜像",
                    settings.githubMirror.ifBlank { "未设置（直连 GitHub）" },
                ) { dialog = SettingsDialog.MIRROR }
            }

            group("关于")
            item {
                ValueRow("版本", BuildConfig.VERSION_NAME) {}
                ValueRow("开源许可与免责声明", "GPLv3 · 基于 LibretroDroid 与 libretro 核心") { dialog = SettingsDialog.ABOUT }
            }
        }
    }

    when (dialog) {
        SettingsDialog.SHADER -> ChoiceDialog(
            title = "画面滤镜",
            options = ShaderOption.entries,
            selected = settings.shader,
            label = { it.label },
            onSelect = { option -> onUpdate { it.copy(shader = option) } },
            onDismiss = { dialog = null },
        )
        SettingsDialog.ORIENTATION -> ChoiceDialog(
            title = "屏幕方向",
            options = OrientationOption.entries,
            selected = settings.orientation,
            label = { it.label },
            onSelect = { option -> onUpdate { it.copy(orientation = option) } },
            onDismiss = { dialog = null },
        )
        SettingsDialog.SPEED -> ChoiceDialog(
            title = "快进倍速",
            options = listOf(2, 3, 4, 6),
            selected = settings.fastForwardSpeed,
            label = { "$it×" },
            onSelect = { speed -> onUpdate { it.copy(fastForwardSpeed = speed) } },
            onDismiss = { dialog = null },
        )
        SettingsDialog.SOURCE -> ChoiceDialog(
            title = "核心下载源",
            options = CoreSource.entries,
            selected = settings.coreSource,
            label = { it.label },
            description = { it.description },
            onSelect = { source -> onUpdate { it.copy(coreSource = source) } },
            onDismiss = { dialog = null },
        )
        SettingsDialog.MIRROR -> MirrorDialog(
            initial = settings.githubMirror,
            onSave = { mirror -> onUpdate { it.copy(githubMirror = mirror.trim()) } },
            onDismiss = { dialog = null },
        )
        SettingsDialog.ABOUT -> AboutDialog(onDismiss = { dialog = null })
        null -> Unit
    }
}

private fun LazyListScope.group(title: String) {
    item {
        HorizontalDivider()
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun ValueRow(title: String, value: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(value) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun SwitchRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        modifier = Modifier.clickable { onChange(!checked) },
    )
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    // Track the thumb locally and persist once the drag ends.
    var current by remember(value) { mutableFloatStateOf(value) }
    ListItem(
        headlineContent = { Text("$title：${format(current)}") },
        supportingContent = {
            Slider(
                value = current,
                onValueChange = { current = it },
                onValueChangeFinished = { onChange(current) },
                valueRange = range,
            )
        },
    )
}

@Composable
private fun MirrorDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GitHub 加速镜像") },
        text = {
            Column {
                Text(
                    "国内访问 GitHub 较慢时，可填写镜像前缀，下载地址会变为「前缀 + 原地址」。留空表示直连。",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    placeholder = { Text("例如 https://ghfast.top/") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(text)
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("开源许可与免责声明") },
        text = {
            Text(
                "复古游戏厅以 GNU GPLv3 协议开源。\n\n" +
                    "模拟功能由 LibretroDroid（GPLv3）与 libretro 生态的各模拟器核心提供：FCEUmm、Snes9x、Gambatte、" +
                    "mGBA、melonDS、Mupen64Plus-Next、Genesis Plus GX、PCSX ReARMed、Beetle、FinalBurn Neo、Stella 等，" +
                    "各核心遵循其自身的开源协议。封面图来自 libretro-thumbnails 项目。\n\n" +
                    "本应用不包含、不提供任何游戏 ROM 或 BIOS。请仅使用你合法拥有的游戏备份。",
                style = MaterialTheme.typography.bodySmall,
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } },
    )
}
