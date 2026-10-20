package com.retrohall.app.ui

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.retrohall.app.AppGraph
import com.retrohall.app.data.CoreState
import com.retrohall.app.data.Game
import com.retrohall.app.ui.components.ConfirmDialog
import com.retrohall.app.ui.cores.CoresScreen
import com.retrohall.app.ui.library.GameDetailSheet
import com.retrohall.app.ui.library.LibraryScreen
import com.retrohall.app.ui.library.PlatformScreen
import com.retrohall.app.ui.library.SearchScreen
import com.retrohall.app.ui.settings.FolderItem
import com.retrohall.app.ui.settings.SettingsScreen
import com.retrohall.core.BiosFile
import com.retrohall.core.EmulatorCore
import com.retrohall.core.Platform
import com.retrohall.core.SortOrder

private sealed interface Screen {
    data class PlatformGames(val platform: Platform) : Screen

    data object Search : Screen
}

private enum class Tab(val label: String, val icon: ImageVector) {
    LIBRARY("游戏库", Icons.Filled.SportsEsports),
    CORES("核心", Icons.Filled.Memory),
    SETTINGS("设置", Icons.Filled.Settings),
}

@Composable
fun RetroHallRoot() {
    val activity = LocalContext.current as Activity
    val actions = remember { LibraryActions(activity) }

    val games by AppGraph.games.games.collectAsState()
    val loaded by AppGraph.games.loaded.collectAsState()
    val settings by AppGraph.settings.state.collectAsState()
    val scanProgress by AppGraph.scanner.progress.collectAsState()
    val coreStates by AppGraph.cores.states.collectAsState()
    val biosStatuses by AppGraph.bios.statuses.collectAsState()

    var tab by rememberSaveable { mutableStateOf(Tab.LIBRARY) }
    val stack = remember { mutableStateListOf<Screen>() }
    var selectedGameId by rememberSaveable { mutableStateOf<Long?>(null) }
    var coverGameId by rememberSaveable { mutableStateOf<Long?>(null) }
    var importCoreName by rememberSaveable { mutableStateOf<String?>(null) }
    var biosWarning by remember { mutableStateOf<Pair<Game, List<BiosFile>>?>(null) }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let(actions::addFolder)
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) actions.importFiles(uris)
    }
    val biosPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) actions.importBios(uris)
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val game = games.firstOrNull { it.id == coverGameId }
        if (uri != null && game != null) actions.setCover(game, uri)
        coverGameId = null
    }
    val corePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val core = EmulatorCore.entries.firstOrNull { it.name == importCoreName }
        if (uri != null && core != null) actions.importCore(core, uri)
        importCoreName = null
    }

    val pop: () -> Unit = { stack.removeAt(stack.lastIndex) }
    val selectGame: (Game) -> Unit = { selectedGameId = it.id }
    val setSort: (SortOrder) -> Unit = { order -> AppGraph.settings.update { it.copy(sortOrder = order) } }
    val play: (Game, Boolean) -> Unit = { game, skipBios ->
        actions.play(
            activity,
            game,
            skipBiosCheck = skipBios,
            onMissingBios = { missing -> biosWarning = game to missing },
            onLaunched = { selectedGameId = null },
        )
    }

    BackHandler(enabled = stack.isNotEmpty()) { pop() }
    BackHandler(enabled = stack.isEmpty() && tab != Tab.LIBRARY) { tab = Tab.LIBRARY }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        when (val top = stack.lastOrNull()) {
            is Screen.PlatformGames -> PlatformScreen(
                platform = top.platform,
                games = games.filter { it.platform == top.platform },
                sortOrder = settings.sortOrder,
                onSortChange = setSort,
                onBack = pop,
                onGameClick = selectGame,
            )

            Screen.Search -> SearchScreen(games = games, onBack = pop, onGameClick = selectGame)

            null -> Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    NavigationBar {
                        Tab.entries.forEach { item ->
                            NavigationBarItem(
                                selected = tab == item,
                                onClick = { tab = item },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(item.label) },
                            )
                        }
                    }
                },
            ) { padding ->
                val content = Modifier.padding(padding)
                when (tab) {
                    Tab.LIBRARY -> LibraryScreen(
                        games = games,
                        loaded = loaded,
                        scanProgress = scanProgress,
                        sortOrder = settings.sortOrder,
                        onSortChange = setSort,
                        onOpenPlatform = { stack.add(Screen.PlatformGames(it)) },
                        onSearch = { stack.add(Screen.Search) },
                        onGameClick = selectGame,
                        onAddFolder = { folderPicker.launch(null) },
                        onImportFiles = { filePicker.launch(arrayOf("*/*")) },
                        onRescan = actions::rescanAll,
                        modifier = content,
                    )

                    Tab.CORES -> CoresScreen(
                        coreStates = coreStates,
                        biosStatuses = biosStatuses,
                        coreSource = settings.coreSource,
                        onDownload = actions::downloadCore,
                        onDownloadEssentials = { AppGraph.cores.downloadInBackground(EmulatorCore.ESSENTIALS) },
                        onDelete = { AppGraph.cores.delete(it) },
                        onImportCore = { core ->
                            importCoreName = core.name
                            corePicker.launch(arrayOf("*/*"))
                        },
                        onImportBios = { biosPicker.launch(arrayOf("*/*")) },
                        modifier = content,
                    )

                    Tab.SETTINGS -> SettingsScreen(
                        settings = settings,
                        folders = settings.romFolders
                            .map { FolderItem(it, AppGraph.scanner.describeFolder(Uri.parse(it))) }
                            .sortedBy { it.path },
                        onUpdate = { transform -> AppGraph.settings.update(transform) },
                        onAddFolder = { folderPicker.launch(null) },
                        onRemoveFolder = actions::removeFolder,
                        modifier = content,
                    )
                }
            }
        }
    }

    games.firstOrNull { it.id == selectedGameId }?.let { game ->
        GameDetailSheet(
            game = game,
            coreState = coreStates[game.platform.core] ?: CoreState.Missing,
            onDismiss = { selectedGameId = null },
            onPlay = { play(game, false) },
            onToggleFavorite = { AppGraph.games.toggleFavorite(game) },
            onChangeCover = {
                coverGameId = game.id
                coverPicker.launch(arrayOf("image/*"))
            },
            onRemove = {
                AppGraph.games.remove(game)
                selectedGameId = null
            },
        )
    }

    biosWarning?.let { (game, missing) ->
        ConfirmDialog(
            title = "缺少 BIOS 文件",
            message = "《${game.title}》需要：\n" +
                missing.joinToString("\n") { "• ${it.fileName}（${it.description}）" } +
                "\n\n可在「核心」页导入 BIOS。仍要尝试启动吗？",
            confirmLabel = "仍然启动",
            onConfirm = { play(game, true) },
            onDismiss = { biosWarning = null },
        )
    }
}
