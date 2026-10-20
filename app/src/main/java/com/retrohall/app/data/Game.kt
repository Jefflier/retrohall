package com.retrohall.app.data

import com.retrohall.core.Platform

data class Game(
    val id: Long,
    /** `content://` document URI for games found in a folder, `file://` for imported games. */
    val uri: String,
    val fileName: String,
    val title: String,
    val platform: Platform,
    val sizeBytes: Long,
    val lastModified: Long,
    /** Tree URI of the library folder the game was found in; null for imported files. */
    val folderUri: String?,
    /** Document id of the folder holding the file, used to find `.cue` tracks next to it. */
    val parentDocumentId: String?,
    /** Entry to extract when the game is a zipped cartridge ROM. */
    val zipEntry: String?,
    val favorite: Boolean,
    val lastPlayed: Long,
    val playCount: Int,
    val playTimeMs: Long,
    val addedAt: Long,
    /** Custom cover image chosen by the user, stored in app storage. */
    val coverPath: String?,
) {
    val isImported: Boolean get() = uri.startsWith("file:")
}

/** A game found by the scanner or imported, before it gets a database id. */
data class ScannedGame(
    val uri: String,
    val fileName: String,
    val title: String,
    val platform: Platform,
    val sizeBytes: Long,
    val lastModified: Long,
    val folderUri: String?,
    val parentDocumentId: String?,
    val zipEntry: String?,
)

data class Cheat(
    val id: Long,
    val gameId: Long,
    val description: String,
    val code: String,
    val enabled: Boolean,
)
