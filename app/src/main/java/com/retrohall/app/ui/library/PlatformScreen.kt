package com.retrohall.app.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.retrohall.app.data.Game
import com.retrohall.app.ui.components.EmptyState
import com.retrohall.core.Platform
import com.retrohall.core.SortOrder

@Composable
private fun gridPadding(): PaddingValues {
    val navigation = WindowInsets.navigationBars.asPaddingValues()
    return PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp + navigation.calculateBottomPadding())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformScreen(
    platform: Platform,
    games: List<Game>,
    sortOrder: SortOrder,
    onSortChange: (SortOrder) -> Unit,
    onBack: () -> Unit,
    onGameClick: (Game) -> Unit,
) {
    val sorted = remember(games, sortOrder) { sortGames(games, sortOrder) }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
            },
            title = {
                Column {
                    Text("${platform.shortName} · ${platform.chineseName}")
                    Text(
                        "${games.size} 款游戏 · ${platform.core.displayName} 核心",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            actions = { SortMenu(sortOrder, onSortChange) },
        )
        if (sorted.isEmpty()) {
            val formats = if (platform == Platform.ARCADE) {
                "FinalBurn Neo 格式的 .zip"
            } else {
                platform.extensions.sorted().joinToString(" ") { ".$it" }
            }
            EmptyState(
                icon = Icons.Filled.SportsEsports,
                title = "还没有${platform.chineseName}游戏",
                message = "支持的文件格式：$formats",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            GameGrid(sorted, onGameClick, gridPadding())
        }
    }
}

@Composable
fun SearchScreen(games: List<Game>, onBack: () -> Unit, onGameClick: (Game) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val trimmed = query.trim()
    val results = remember(games, trimmed) {
        if (trimmed.isEmpty()) {
            emptyList()
        } else {
            games.filter {
                it.title.contains(trimmed, ignoreCase = true) ||
                    it.fileName.contains(trimmed, ignoreCase = true) ||
                    it.platform.shortName.equals(trimmed, ignoreCase = true)
            }.let { sortGames(it, SortOrder.TITLE) }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("搜索游戏名称或平台，如 GBA") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = "清除") }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp)
                    .focusRequester(focusRequester),
            )
        }
        when {
            trimmed.isEmpty() -> Unit
            results.isEmpty() -> EmptyState(
                icon = Icons.Filled.SearchOff,
                title = "没有找到「$trimmed」",
                message = "试试游戏名的一部分，或输入平台名（如 FC、GBA）",
                modifier = Modifier.fillMaxSize(),
            )
            else -> GameGrid(results, onGameClick, gridPadding())
        }
    }
}
