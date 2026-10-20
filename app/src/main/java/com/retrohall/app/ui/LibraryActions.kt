package com.retrohall.app.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.retrohall.app.AppGraph
import com.retrohall.app.data.Game
import com.retrohall.app.game.GameActivity
import com.retrohall.core.BiosFile
import com.retrohall.core.EmulatorCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * User actions that outlive a single screen (scans, imports, downloads). They run in the
 * process-wide scope so rotating the screen does not cancel them.
 */
class LibraryActions(context: Context) {
    private val app = context.applicationContext
    private val scope = AppGraph.scope
    private val scanLock = Mutex()

    fun addFolder(uri: Uri) {
        runCatching { app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        AppGraph.settings.update { it.copy(romFolders = it.romFolders + uri.toString()) }
        scan(listOf(uri))
    }

    fun removeFolder(uri: String) {
        runCatching { app.contentResolver.releasePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        AppGraph.settings.update { it.copy(romFolders = it.romFolders - uri) }
        AppGraph.games.removeFolder(uri)
    }

    fun rescanAll() {
        val folders = AppGraph.settings.current.romFolders.map(Uri::parse)
        if (folders.isEmpty()) {
            toast("还没有添加游戏文件夹")
            return
        }
        scan(folders)
    }

    private fun scan(folders: List<Uri>) {
        scope.launch {
            scanLock.withLock {
                var found = 0
                for (folder in folders) {
                    found += runCatching { AppGraph.scanner.scanFolder(folder) }.getOrElse {
                        toast("无法读取文件夹：${it.message ?: AppGraph.scanner.describeFolder(folder)}")
                        0
                    }
                }
                AppGraph.games.reload()
                toast("扫描完成，找到 $found 款游戏")
            }
        }
    }

    fun importFiles(uris: List<Uri>) {
        scope.launch {
            toast("正在导入 ${uris.size} 个文件…")
            val result = runCatching { AppGraph.scanner.importFiles(uris) }.getOrElse {
                toast("导入失败：${it.message}")
                return@launch
            }
            AppGraph.games.reload()
            val skipped = if (result.unsupported.isEmpty()) "" else "，${result.unsupported.size} 个文件无法识别"
            toast("已导入 ${result.imported} 款游戏$skipped")
        }
    }

    fun importBios(uris: List<Uri>) {
        scope.launch {
            val result = runCatching { AppGraph.bios.import(uris) }.getOrElse {
                toast("导入失败：${it.message}")
                return@launch
            }
            val other = if (result.other.isEmpty()) "" else "，另有 ${result.other.size} 个文件已复制到系统目录"
            toast("已导入 ${result.recognized.size} 个 BIOS$other")
        }
    }

    fun importCore(core: EmulatorCore, uri: Uri) {
        scope.launch {
            if (AppGraph.cores.importCore(core, uri)) toast("已导入 ${core.displayName} 核心")
        }
    }

    fun downloadCore(core: EmulatorCore) {
        scope.launch {
            if (AppGraph.cores.download(core)) toast("${core.displayName} 核心已安装")
        }
    }

    fun setCover(game: Game, image: Uri) {
        AppGraph.games.setCover(app, game, image) { AppGraph.covers.invalidate(game) }
    }

    /**
     * Starts a game, downloading its core first if needed.
     * [onMissingBios] is called instead when the game needs BIOS files that are not imported.
     */
    fun play(
        activity: Activity,
        game: Game,
        skipBiosCheck: Boolean = false,
        onMissingBios: (List<BiosFile>) -> Unit = {},
        onLaunched: () -> Unit = {},
    ) {
        if (!skipBiosCheck) {
            val missing = AppGraph.bios.missingFor(game)
            if (missing.isNotEmpty()) {
                onMissingBios(missing)
                return
            }
        }
        scope.launch {
            val ready = AppGraph.cores.ensureInstalled(game.platform.core)
            withContext(Dispatchers.Main) {
                if (!ready) {
                    toast("${game.platform.core.displayName} 核心下载失败，请检查网络或在设置中更换下载源")
                    return@withContext
                }
                if (activity.isFinishing || activity.isDestroyed) return@withContext
                activity.startActivity(GameActivity.intent(activity, game.id))
                onLaunched()
            }
        }
    }

    private fun toast(message: String) {
        Handler(Looper.getMainLooper()).post { Toast.makeText(app, message, Toast.LENGTH_SHORT).show() }
    }
}
