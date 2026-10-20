package com.retrohall.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.retrohall.core.Platform

/**
 * The game library. Both the main process and the game process open it, which SQLite handles
 * with file locks; the game process only records play statistics and cheats.
 */
class GameDatabase(context: Context) : SQLiteOpenHelper(context, "library.db", null, VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE games (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uri TEXT NOT NULL UNIQUE,
                file_name TEXT NOT NULL,
                title TEXT NOT NULL,
                platform TEXT NOT NULL,
                size INTEGER NOT NULL DEFAULT 0,
                last_modified INTEGER NOT NULL DEFAULT 0,
                folder_uri TEXT,
                parent_doc_id TEXT,
                zip_entry TEXT,
                favorite INTEGER NOT NULL DEFAULT 0,
                last_played INTEGER NOT NULL DEFAULT 0,
                play_count INTEGER NOT NULL DEFAULT 0,
                play_time INTEGER NOT NULL DEFAULT 0,
                added_at INTEGER NOT NULL DEFAULT 0,
                cover_path TEXT
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX games_folder ON games(folder_uri)")
        db.execSQL(
            """
            CREATE TABLE cheats (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                game_id INTEGER NOT NULL REFERENCES games(id) ON DELETE CASCADE,
                description TEXT NOT NULL,
                code TEXT NOT NULL,
                enabled INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX cheats_game ON cheats(game_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 is the first schema; future migrations go here.
    }

    fun allGames(): List<Game> =
        readableDatabase.query("games", null, null, null, null, null, "title COLLATE NOCASE").use { it.toGames() }

    fun game(id: Long): Game? =
        readableDatabase.query("games", null, "id = ?", arrayOf(id.toString()), null, null, null)
            .use { it.toGames().firstOrNull() }

    /**
     * Inserts or updates the games found in one library folder and removes the ones that disappeared.
     * Favourites, play statistics and custom covers survive a rescan because rows are matched by URI.
     */
    fun replaceFolder(folderUri: String, found: List<ScannedGame>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            found.forEach { upsert(db, it) }
            val keep = found.mapTo(HashSet()) { it.uri }
            val stale = db.query("games", arrayOf("id", "uri"), "folder_uri = ?", arrayOf(folderUri), null, null, null)
                .use { c ->
                    buildList {
                        while (c.moveToNext()) {
                            if (c.getString(1) !in keep) add(c.getLong(0))
                        }
                    }
                }
            stale.forEach { db.delete("games", "id = ?", arrayOf(it.toString())) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun removeFolder(folderUri: String) {
        writableDatabase.delete("games", "folder_uri = ?", arrayOf(folderUri))
    }

    fun upsert(game: ScannedGame) {
        upsert(writableDatabase, game)
    }

    private fun upsert(db: SQLiteDatabase, game: ScannedGame) {
        val values = ContentValues().apply {
            put("file_name", game.fileName)
            put("title", game.title)
            put("platform", game.platform.name)
            put("size", game.sizeBytes)
            put("last_modified", game.lastModified)
            put("folder_uri", game.folderUri)
            put("parent_doc_id", game.parentDocumentId)
            put("zip_entry", game.zipEntry)
        }
        if (db.update("games", values, "uri = ?", arrayOf(game.uri)) == 0) {
            values.put("uri", game.uri)
            values.put("added_at", System.currentTimeMillis())
            db.insert("games", null, values)
        }
    }

    fun delete(id: Long) {
        writableDatabase.delete("games", "id = ?", arrayOf(id.toString()))
    }

    fun setFavorite(id: Long, favorite: Boolean) = updateGame(id, ContentValues().apply { put("favorite", if (favorite) 1 else 0) })

    fun setCover(id: Long, path: String?) = updateGame(id, ContentValues().apply { put("cover_path", path) })

    fun recordLaunch(id: Long) {
        writableDatabase.execSQL(
            "UPDATE games SET last_played = ?, play_count = play_count + 1 WHERE id = ?",
            arrayOf<Any>(System.currentTimeMillis(), id),
        )
    }

    fun addPlayTime(id: Long, millis: Long) {
        if (millis <= 0) return
        writableDatabase.execSQL("UPDATE games SET play_time = play_time + ? WHERE id = ?", arrayOf<Any>(millis, id))
    }

    fun cheats(gameId: Long): List<Cheat> =
        readableDatabase.query("cheats", null, "game_id = ?", arrayOf(gameId.toString()), null, null, "id").use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        Cheat(
                            id = c.getLong(c.getColumnIndexOrThrow("id")),
                            gameId = c.getLong(c.getColumnIndexOrThrow("game_id")),
                            description = c.getString(c.getColumnIndexOrThrow("description")),
                            code = c.getString(c.getColumnIndexOrThrow("code")),
                            enabled = c.getInt(c.getColumnIndexOrThrow("enabled")) != 0,
                        ),
                    )
                }
            }
        }

    fun insertCheat(gameId: Long, description: String, code: String, enabled: Boolean) {
        writableDatabase.insert(
            "cheats",
            null,
            ContentValues().apply {
                put("game_id", gameId)
                put("description", description)
                put("code", code)
                put("enabled", if (enabled) 1 else 0)
            },
        )
    }

    fun setCheatEnabled(id: Long, enabled: Boolean) {
        writableDatabase.update(
            "cheats",
            ContentValues().apply { put("enabled", if (enabled) 1 else 0) },
            "id = ?",
            arrayOf(id.toString()),
        )
    }

    fun deleteCheat(id: Long) {
        writableDatabase.delete("cheats", "id = ?", arrayOf(id.toString()))
    }

    private fun updateGame(id: Long, values: ContentValues) {
        writableDatabase.update("games", values, "id = ?", arrayOf(id.toString()))
    }

    private fun Cursor.toGames(): List<Game> {
        val id = getColumnIndexOrThrow("id")
        val uri = getColumnIndexOrThrow("uri")
        val fileName = getColumnIndexOrThrow("file_name")
        val title = getColumnIndexOrThrow("title")
        val platform = getColumnIndexOrThrow("platform")
        val size = getColumnIndexOrThrow("size")
        val lastModified = getColumnIndexOrThrow("last_modified")
        val folderUri = getColumnIndexOrThrow("folder_uri")
        val parentDocId = getColumnIndexOrThrow("parent_doc_id")
        val zipEntry = getColumnIndexOrThrow("zip_entry")
        val favorite = getColumnIndexOrThrow("favorite")
        val lastPlayed = getColumnIndexOrThrow("last_played")
        val playCount = getColumnIndexOrThrow("play_count")
        val playTime = getColumnIndexOrThrow("play_time")
        val addedAt = getColumnIndexOrThrow("added_at")
        val coverPath = getColumnIndexOrThrow("cover_path")
        return buildList {
            while (moveToNext()) {
                // Rows for platforms removed in a later version are skipped rather than crashing.
                val gamePlatform = Platform.fromId(getString(platform)) ?: continue
                add(
                    Game(
                        id = getLong(id),
                        uri = getString(uri),
                        fileName = getString(fileName),
                        title = getString(title),
                        platform = gamePlatform,
                        sizeBytes = getLong(size),
                        lastModified = getLong(lastModified),
                        folderUri = getStringOrNull(folderUri),
                        parentDocumentId = getStringOrNull(parentDocId),
                        zipEntry = getStringOrNull(zipEntry),
                        favorite = getInt(favorite) != 0,
                        lastPlayed = getLong(lastPlayed),
                        playCount = getInt(playCount),
                        playTimeMs = getLong(playTime),
                        addedAt = getLong(addedAt),
                        coverPath = getStringOrNull(coverPath),
                    ),
                )
            }
        }
    }

    private fun Cursor.getStringOrNull(index: Int): String? = if (isNull(index)) null else getString(index)

    companion object {
        private const val VERSION = 1
    }
}
