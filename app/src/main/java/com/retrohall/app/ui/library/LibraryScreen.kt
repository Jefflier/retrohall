package com.retrohall.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retrohall.app.data.Game
import com.retrohall.app.data.ScanProgress
import com.retrohall.app.ui.components.EmptyState
import com.retrohall.app.ui.components.GameCard
import com.retrohall.app.ui.components.PlatformCard
import com.retrohall.app.ui.components.SectionHeader
import com.retrohall.core.Platform
import com.retrohall.core.SortOrder
import java.text.Collator
import java.util.Locale

private val titleCollator: Collator = Collator.getInstance(Locale.CHINA)

/** Sorts games; titles are compared with the Chinese collator so mixed names sort by pinyin. */
fun sortGames(games: List<Game>, order: SortOrder): List<Game> = when (order) {
    SortOrder.TITLE -> games.sortedWith(compareBy(titleCollator) { it.title })
    SortOrder.RECENT -> games.sortedByDescending { it.lastPlayed }
    SortOrder.ADDED -> games.sortedByDescending { it.addedAt }
    SortOrder.PLAY_TIME -> games.sortedByDescending { it.playTimeMs }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    games: List<Game>,
    loaded: Boolean,
    scanProgress: ScanProgress?,
    sortOrder: SortOrder,
    onSortChange: (SortOrder) -> Unit,
    onOpenPlatform: (Platform) -> Unit,
    onSearch: () -> Unit,
    onGameClick: (Game) -> Unit,
    onAddFolder: () -> Unit,
    onImportFiles: () -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("复古游戏厅", fontWeight = FontWeight.Black) },
            actions = {
                IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, contentDescription = "搜索") }
                AddGamesMenu(onAddFolder, onImportFiles, onRescan)
            },
        )
        scanProgress?.let { ScanBanner(it) }
        when {
            !loaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            games.isEmpty() -> EmptyLibrary(onAddFolder, onImportFiles)
            else -> LibraryContent(games, sortOrder, onSortChange, onOpenPlatform, onGameClick)
        }
    }
}

@Composable
private fun AddGamesMenu(onAddFolder: () -> Unit, onImportFiles: () -> Unit, onRescan: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) { Icon(Icons.Filled.Add, contentDescription = "添加游戏") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("添加游戏文件夹") },
                leadingIcon = { Icon(Icons.Filled.CreateNewFolder, contentDescription = null) },
                onClick = {
                    expanded = false
                    onAddFolder()
                },
            )
            DropdownMenuItem(
                text = { Text("导入游戏文件") },
                leadingIcon = { Icon(Icons.Filled.FileOpen, contentDescription = null) },
                onClick = {
                    expanded = false
                    onImportFiles()
                },
            )
            DropdownMenuItem(
                text = { Text("重新扫描文件夹") },
                leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                onClick = {
                    expanded = false
                    onRescan()
                },
            )
        }
    }
}

@Composable
private fun ScanBanner(progress: ScanProgress) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                "正在扫描「${progress.folder}」… 已找到 ${progress.foundGames} 款游戏",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyLibrary(onAddFolder: () -> Unit, onImportFiles: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(
            icon = Icons.Filled.SportsEsports,
            title = "游戏库还是空的",
            message = "添加存放 ROM 的文件夹，应用会自动识别各平台的游戏并下载封面。\n" +
                "本应用不提供任何游戏，请使用你合法拥有的游戏备份。",
        ) {
            Button(onClick = onAddFolder, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CreateNewFolder, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("添加游戏文件夹")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onImportFiles, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.FileOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("导入游戏文件")
            }
        }
        Text("支持的平台", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.padding(16.dp),
        ) {
            Platform.entries.forEach { platform ->
                AssistChip(onClick = {}, label = { Text("${platform.shortName} ${platform.chineseName}") })
            }
        }
    }
}

@Composable
private fun LibraryContent(
    games: List<Game>,
    sortOrder: SortOrder,
    onSortChange: (SortOrder) -> Unit,
    onOpenPlatform: (Platform) -> Unit,
    onGameClick: (Game) -> Unit,
) {
    val byPlatform = remember(games) { games.groupBy { it.platform } }
    val recent = remember(games) { games.filter { it.lastPlayed > 0 }.sortedByDescending { it.lastPlayed }.take(12) }
    val favorites = remember(games) { games.filter { it.favorite } }
    val sorted = remember(games, sortOrder) { sortGames(games, sortOrder) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 108.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        fullWidth { SectionHeader("游戏平台") }
        fullWidth {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(Platform.entries.filter { it in byPlatform }) { platform ->
                    PlatformCard(platform, byPlatform.getValue(platform).size, onClick = { onOpenPlatform(platform) })
                }
            }
        }
        if (recent.isNotEmpty()) {
            fullWidth { SectionHeader("最近游玩") }
            fullWidth { GameRow(recent, onGameClick) }
        }
        if (favorites.isNotEmpty()) {
            fullWidth { SectionHeader("我的收藏") }
            fullWidth { GameRow(favorites, onGameClick) }
        }
        fullWidth {
            SectionHeader("全部游戏 · ${games.size}", action = { SortMenu(sortOrder, onSortChange) })
        }
        items(sorted, key = { it.id }) { game ->
            GameCard(game, onClick = { onGameClick(game) })
        }
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun GameRow(games: List<Game>, onGameClick: (Game) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(games, key = { it.id }) { game ->
            GameCard(game, onClick = { onGameClick(game) }, modifier = Modifier.width(104.dp))
        }
    }
}

@Composable
fun SortMenu(sortOrder: SortOrder, onSortChange: (SortOrder) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(sortOrder.label)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(order.label) },
                    onClick = {
                        expanded = false
                        onSortChange(order)
                    },
                )
            }
        }
    }
}

/** Grid of game cards used by the platform and search screens. */
@Composable
fun GameGrid(games: List<Game>, onGameClick: (Game) -> Unit, contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 108.dp),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        items(games, key = { it.id }) { game ->
            GameCard(game, onClick = { onGameClick(game) })
        }
    }
}
