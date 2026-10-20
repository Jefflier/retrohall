package com.retrohall.app

import android.app.Application
import com.retrohall.app.data.BiosManager
import com.retrohall.app.data.CoreManager
import com.retrohall.app.data.CoverLoader
import com.retrohall.app.data.GameDatabase
import com.retrohall.app.data.GameRepository
import com.retrohall.app.data.LibraryScanner
import com.retrohall.app.data.RomPreparer
import com.retrohall.app.data.SaveManager
import com.retrohall.app.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class RetroHallApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}

/**
 * Process-wide singletons. Everything is lazy because the game process only needs a few of them.
 */
object AppGraph {
    private lateinit var app: Application

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun init(application: Application) {
        app = application
    }

    val settings by lazy { SettingsStore(app) }
    val database by lazy { GameDatabase(app) }
    val games by lazy { GameRepository(database, scope) }
    val scanner by lazy { LibraryScanner(app, database) }
    val cores by lazy { CoreManager(app, settings, scope) }
    val bios by lazy { BiosManager(app) }
    val saves by lazy { SaveManager(app) }
    val covers by lazy { CoverLoader(app) }
    val roms by lazy { RomPreparer(app) }
}
