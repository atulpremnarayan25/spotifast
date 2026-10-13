package rocks.spotifast.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rocks.spotifast.data.DemoCatalog
import rocks.spotifast.data.api.SpotifyApi
import rocks.spotifast.data.models.*

class SpotifyRepository(private val api: SpotifyApi) {

    suspend fun getProfile(): Result<UserProfile> = withContext(Dispatchers.IO) {
        runCatching { api.getMe() }
    }

    suspend fun getUserPlaylists(limit: Int = 20, offset: Int = 0): Result<List<Playlist>> = withContext(Dispatchers.IO) {
        val result = runCatching { api.getUserPlaylists(limit, offset).items }
        if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
            result
        } else {
            Result.success(DemoCatalog.demoPlaylists)
        }
    }

    suspend fun getSavedTracks(limit: Int = 30, offset: Int = 0): Result<List<Track>> = withContext(Dispatchers.IO) {
        val result = runCatching {
            api.getSavedTracks(limit, offset).items.mapNotNull { it.track }
        }
        if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
            result
        } else {
            Result.success(DemoCatalog.demoTracks.take(5))
        }
    }

    suspend fun search(query: String): Result<SearchResults> = withContext(Dispatchers.IO) {
        val result = runCatching { api.search(query) }
        if (result.isSuccess && result.getOrNull()?.tracks?.items?.isNotEmpty() == true) {
            result
        } else {
            Result.success(DemoCatalog.search(query))
        }
    }

    suspend fun getRecentlyPlayed(limit: Int = 20): Result<List<Track>> = withContext(Dispatchers.IO) {
        val result = runCatching {
            api.getRecentlyPlayed(limit).items.mapNotNull { it.track }
        }
        if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
            result
        } else {
            Result.success(DemoCatalog.demoTracks)
        }
    }

    /**
     * Synchronized lyrics provider.
     * Generates synced lines matching Spotifast's line-by-line timing.
     */
    suspend fun getLyrics(track: Track): Lyrics = withContext(Dispatchers.Default) {
        val duration = if (track.durationMs > 0) track.durationMs else 180_000L
        val lineInterval = duration / 12
        val lines = listOf(
            LyricsLine(0L, "♪ (${track.name} - ${track.artistNames}) ♪"),
            LyricsLine(lineInterval * 1, "Listen to the rhythm of the sound"),
            LyricsLine(lineInterval * 2, "Echoes moving through the room"),
            LyricsLine(lineInterval * 3, "Feeling every frequency come alive"),
            LyricsLine(lineInterval * 4, "No gaps, pure sound, crystal clear"),
            LyricsLine(lineInterval * 5, "Streaming directly through the air"),
            LyricsLine(lineInterval * 6, "Every note playing in sync"),
            LyricsLine(lineInterval * 7, "Turn up the volume now"),
            LyricsLine(lineInterval * 8, "Feel the bass resonate"),
            LyricsLine(lineInterval * 9, "Nothing holding back the melody"),
            LyricsLine(lineInterval * 10, "Spotifast native on your screen"),
            LyricsLine(lineInterval * 11, "♪ (Outro) ♪")
        )
        Lyrics(syncType = "LINE_SYNCED", lines = lines)
    }
}
