package com.retrohall.app.data

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Process
import com.retrohall.core.CoreDownloads
import com.retrohall.core.CoreSource
import com.retrohall.core.EmulatorCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

sealed interface CoreState {
    data object Missing : CoreState

    /** [progress] is between 0 and 1, or negative when the size is unknown. */
    data class Downloading(val progress: Float) : CoreState

    data class Installed(val sizeBytes: Long) : CoreState

    data class Failed(val message: String) : CoreState
}

/** Downloads, imports and removes libretro cores. Cores live in the app's private files directory. */
class CoreManager(
    private val context: Context,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
) {
    private val coresDir = File(context.filesDir, "cores").apply { mkdirs() }
    private val downloads = mutableMapOf<EmulatorCore, Deferred<Boolean>>()

    private val _states = MutableStateFlow(EmulatorCore.entries.associateWith { installedState(it) })
    val states: StateFlow<Map<EmulatorCore, CoreState>> = _states.asStateFlow()

    fun coreFile(core: EmulatorCore): File = File(coresDir, core.libraryFileName)

    fun isInstalled(core: EmulatorCore): Boolean = coreFile(core).let { it.isFile && it.length() > 0 }

    /** Returns true when the core is ready, downloading it first if needed. */
    suspend fun ensureInstalled(core: EmulatorCore): Boolean = isInstalled(core) || download(core)

    /** Starts (or joins) a download. Concurrent callers share one download per core. */
    suspend fun download(core: EmulatorCore): Boolean {
        val job = synchronized(downloads) {
            downloads.getOrPut(core) {
                scope.async(Dispatchers.IO) {
                    try {
                        runDownload(core)
                    } finally {
                        synchronized(downloads) { downloads.remove(core) }
                    }
                }
            }
        }
        return job.await()
    }

    fun downloadInBackground(cores: Collection<EmulatorCore>) {
        cores.filterNot(::isInstalled).forEach { core -> scope.launch { download(core) } }
    }

    suspend fun importCore(core: EmulatorCore, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val temp = File(coresDir, core.libraryFileName + ".import")
        try {
            val input = context.contentResolver.openInputStream(uri) ?: throw IOException("无法读取文件")
            FileUtils.copyAtomically(input, temp)
            install(core, temp)
            true
        } catch (e: Exception) {
            temp.delete()
            setState(core, CoreState.Failed(e.message ?: "导入失败"))
            false
        }
    }

    fun delete(core: EmulatorCore) {
        coreFile(core).delete()
        setState(core, CoreState.Missing)
    }

    private fun runDownload(core: EmulatorCore): Boolean {
        // Cores are loaded into this process, so they must match its bitness, not just the device.
        val processAbis = if (Process.is64Bit()) Build.SUPPORTED_64_BIT_ABIS else Build.SUPPORTED_32_BIT_ABIS
        val abi = CoreDownloads.preferredAbi(processAbis.toList())
        if (abi == null) {
            setState(core, CoreState.Failed("不支持此设备的 CPU 架构"))
            return false
        }
        val current = settings.current
        val temp = File(coresDir, core.libraryFileName + ".download")
        setState(core, CoreState.Downloading(0f))
        return try {
            when (current.coreSource) {
                CoreSource.GITHUB -> httpDownload(CoreDownloads.githubUrl(core, abi, current.githubMirror), temp) {
                    setState(core, CoreState.Downloading(it))
                }

                CoreSource.BUILDBOT -> {
                    val zip = File(coresDir, core.libraryFileName + ".zip")
                    try {
                        httpDownload(CoreDownloads.buildbotUrl(core, abi), zip) { setState(core, CoreState.Downloading(it)) }
                        extractEntry(zip, CoreDownloads.buildbotEntryName(core), temp)
                    } finally {
                        zip.delete()
                    }
                }
            }
            install(core, temp)
            true
        } catch (e: Exception) {
            temp.delete()
            setState(core, CoreState.Failed(e.message ?: "下载失败"))
            false
        }
    }

    private fun install(core: EmulatorCore, file: File) {
        if (!FileUtils.isElf(file)) throw IOException("文件不是有效的核心（.so）")
        val target = coreFile(core)
        target.delete()
        if (!file.renameTo(target)) throw IOException("无法保存核心")
        setState(core, CoreState.Installed(target.length()))
    }

    private fun httpDownload(url: String, destination: File, onProgress: (Float) -> Unit) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "RetroHall")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("下载失败（HTTP $code）")
            val total = connection.contentLengthLong
            var lastReported = 0L
            FileUtils.copyAtomically(connection.inputStream, destination) { copied ->
                if (copied - lastReported >= 256 * 1024 || copied == total) {
                    lastReported = copied
                    onProgress(if (total > 0) copied.toFloat() / total else -1f)
                }
            }
        } catch (e: IOException) {
            throw IOException(e.message ?: "网络错误，请检查网络或在设置中更换下载源", e)
        } finally {
            connection.disconnect()
        }
    }

    private fun extractEntry(zip: File, entryName: String, destination: File) {
        ZipInputStream(zip.inputStream().buffered()).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                if (entry.name.substringAfterLast('/') == entryName) {
                    destination.outputStream().use { input.copyTo(it) }
                    return
                }
            }
        }
        throw IOException("压缩包中没有 $entryName")
    }

    private fun installedState(core: EmulatorCore): CoreState {
        val file = coreFile(core)
        return if (file.isFile && file.length() > 0) CoreState.Installed(file.length()) else CoreState.Missing
    }

    private fun setState(core: EmulatorCore, state: CoreState) {
        _states.update { it + (core to state) }
    }
}
