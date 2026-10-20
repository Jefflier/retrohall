package com.retrohall.app.data

import android.content.Context
import android.net.Uri
import com.retrohall.core.CheatCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * In-memory view of the library. Libraries hold at most a few thousand games, so the whole table
 * is reloaded after every change instead of tracking fine-grained updates.
 */
class GameRepository(private val database: GameDatabase, private val scope: CoroutineScope) {
    private val _games = MutableStateFlow<List<Game>>(emptyList())
    val games: StateFlow<List<Game>> = _games.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    fun refresh() {
        scope.launch { reload() }
    }

    suspend fun reload() = withContext(Dispatchers.IO) {
        _games.value = database.allGames()
        _loaded.value = true
    }

    suspend fun game(id: Long): Game? = withContext(Dispatchers.IO) { database.game(id) }

    fun toggleFavorite(game: Game) = mutate { database.setFavorite(game.id, !game.favorite) }

    /** Removes a game from the library. Imported files are deleted because nothing else references them. */
    fun remove(game: Game) = mutate {
        database.delete(game.id)
        if (game.isImported) Uri.parse(game.uri).path?.let { File(it).delete() }
    }

    fun removeFolder(folderUri: String) = mutate { database.removeFolder(folderUri) }

    /** Copies a picked image into app storage and uses it as the game's cover. */
    fun setCover(context: Context, game: Game, image: Uri, onDone: () -> Unit) = mutate {
        val target = File(File(context.filesDir, "covers"), "${game.id}-${System.currentTimeMillis()}.img")
        val input = context.contentResolver.openInputStream(image) ?: return@mutate
        FileUtils.copyAtomically(input, target)
        game.coverPath?.let { File(it).delete() }
        database.setCover(game.id, target.path)
        onDone()
    }

    fun clearCover(game: Game) = mutate {
        game.coverPath?.let { File(it).delete() }
        database.setCover(game.id, null)
    }

    suspend fun cheats(gameId: Long): List<Cheat> = withContext(Dispatchers.IO) { database.cheats(gameId) }

    suspend fun addCheats(gameId: Long, cheats: List<CheatCode>) = withContext(Dispatchers.IO) {
        cheats.forEach { database.insertCheat(gameId, it.description, it.code, it.enabled) }
    }

    suspend fun setCheatEnabled(cheat: Cheat, enabled: Boolean) = withContext(Dispatchers.IO) {
        database.setCheatEnabled(cheat.id, enabled)
    }

    suspend fun deleteCheat(cheat: Cheat) = withContext(Dispatchers.IO) { database.deleteCheat(cheat.id) }

    private fun mutate(block: () -> Unit) {
        scope.launch(Dispatchers.IO) {
            block()
            reload()
        }
    }
}
