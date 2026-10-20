package com.retrohall.app.ui.cores

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retrohall.app.data.BiosStatus
import com.retrohall.app.data.CoreState
import com.retrohall.app.ui.components.SectionHeader
import com.retrohall.core.CoreSource
import com.retrohall.core.EmulatorCore
import com.retrohall.core.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoresScreen(
    coreStates: Map<EmulatorCore, CoreState>,
    biosStatuses: List<BiosStatus>,
    coreSource: CoreSource,
    onDownload: (EmulatorCore) -> Unit,
    onDownloadEssentials: () -> Unit,
    onDelete: (EmulatorCore) -> Unit,
    onImportCore: (EmulatorCore) -> Unit,
    onImportBios: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        TopAppBar(title = { Text("核心与 BIOS", fontWeight = FontWeight.Bold) })
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                IntroCard(
                    coreStates = coreStates,
                    coreSource = coreSource,
                    onDownloadEssentials = onDownloadEssentials,
                )
            }
            item { SectionHeader("模拟器核心", Modifier.padding(horizontal = 16.dp)) }
            items(EmulatorCore.entries) { core ->
                CoreRow(
                    core = core,
                    state = coreStates[core] ?: CoreState.Missing,
                    onDownload = { onDownload(core) },
                    onDelete = { onDelete(core) },
                    onImport = { onImportCore(core) },
                )
            }
            item {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SectionHeader(
                    "BIOS 文件",
                    Modifier.padding(horizontal = 16.dp),
                    action = { OutlinedButton(onClick = onImportBios) { Text("导入 BIOS") } },
                )
                Text(
                    "多数核心自带替代 BIOS，可直接游玩。使用原版 BIOS 能提高兼容性；" +
                        "FDS 游戏和 Neo Geo 街机游戏需要对应 BIOS。可一次选择多个文件，应用会按文件名自动归位。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            items(biosStatuses) { status -> BiosRow(status) }
        }
    }
}

@Composable
private fun IntroCard(
    coreStates: Map<EmulatorCore, CoreState>,
    coreSource: CoreSource,
    onDownloadEssentials: () -> Unit,
) {
    val installed = coreStates.values.count { it is CoreState.Installed }
    val essentialsMissing = EmulatorCore.ESSENTIALS.any { coreStates[it] !is CoreState.Installed }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Memory, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("已安装 $installed / ${EmulatorCore.entries.size} 个核心", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "核心是运行各平台游戏的模拟器程序，首次打开某个平台的游戏时会自动下载。" +
                    "当前下载源：${coreSource.label}（可在设置中更换）。",
                style = MaterialTheme.typography.bodySmall,
            )
            if (essentialsMissing) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onDownloadEssentials) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("一键下载常用核心（FC / SFC / GB / GBA / MD）")
                }
            }
        }
    }
}

@Composable
private fun CoreRow(
    core: EmulatorCore,
    state: CoreState,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onImport: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(core.displayName, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Column {
                Text(core.platforms.joinToString(" · ") { "${it.shortName} ${it.chineseName}" })
                when (state) {
                    is CoreState.Installed -> Text("已安装 · ${Formatters.size(state.sizeBytes)}", color = MaterialTheme.colorScheme.primary)
                    is CoreState.Downloading -> {
                        Spacer(Modifier.height(4.dp))
                        if (state.progress >= 0) {
                            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                    is CoreState.Failed -> Text("失败：${state.message}", color = MaterialTheme.colorScheme.error)
                    CoreState.Missing -> Text("未安装 · 约 ${core.approxSizeMb} MB")
                }
            }
        },
        leadingContent = {
            when (state) {
                is CoreState.Installed -> Icon(Icons.Filled.CheckCircle, contentDescription = "已安装", tint = MaterialTheme.colorScheme.primary)
                is CoreState.Downloading -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                is CoreState.Failed -> Icon(Icons.Filled.ErrorOutline, contentDescription = "失败", tint = MaterialTheme.colorScheme.error)
                CoreState.Missing -> Icon(Icons.Filled.RadioButtonUnchecked, contentDescription = "未安装")
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (state) {
                    CoreState.Missing -> IconButton(onClick = onDownload) { Icon(Icons.Filled.Download, contentDescription = "下载") }
                    is CoreState.Failed -> IconButton(onClick = onDownload) { Icon(Icons.Filled.Refresh, contentDescription = "重试") }
                    else -> Unit
                }
                CoreMenu(state, onDownload, onDelete, onImport)
            }
        },
    )
}

@Composable
private fun CoreMenu(state: CoreState, onDownload: () -> Unit, onDelete: () -> Unit, onImport: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, enabled = state !is CoreState.Downloading) {
            Icon(Icons.Filled.MoreVert, contentDescription = "更多")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (state is CoreState.Installed) {
                DropdownMenuItem(
                    text = { Text("重新下载") },
                    leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onDownload()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("从文件导入 .so") },
                leadingIcon = { Icon(Icons.Filled.FileOpen, contentDescription = null) },
                onClick = {
                    expanded = false
                    onImport()
                },
            )
            if (state is CoreState.Installed) {
                DropdownMenuItem(
                    text = { Text("删除") },
                    leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onDelete()
                    },
                )
            }
        }
    }
}

@Composable
private fun BiosRow(status: BiosStatus) {
    ListItem(
        headlineContent = {
            Text(
                if (status.file.directory.isEmpty()) status.file.fileName else "${status.file.directory}/${status.file.fileName}",
                fontWeight = FontWeight.Medium,
            )
        },
        supportingContent = { Text("${status.platform.shortName} · ${status.file.description}") },
        leadingContent = {
            if (status.present) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "已导入", tint = MaterialTheme.colorScheme.primary)
            } else {
                Icon(Icons.Filled.RadioButtonUnchecked, contentDescription = "未导入", tint = Color.Gray)
            }
        },
    )
}
