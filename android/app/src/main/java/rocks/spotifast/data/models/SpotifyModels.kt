package rocks.spotifast.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Image(
    val url: String,
    val height: Int? = null,
    val width: Int? = null
)

@Serializable
data class ArtistRef(
    val id: String? = null,
    val name: String,
    val uri: String? = null
)

@Serializable
data class Album(
    val id: String,
    val name: String,
    val uri: String,
    @SerialName("album_type") val albumType: String? = null,
    val images: List<Image> = emptyList(),
    val artists: List<ArtistRef> = emptyList(),
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("total_tracks") val totalTracks: Int? = null
) {
    val year: String
        get() = releaseDate?.take(4) ?: ""
}

@Serializable
data class Track(
    val id: String? = null,
    val name: String,
    val uri: String,
    @SerialName("duration_ms") val durationMs: Long = 0L,
    val explicit: Boolean = false,
    val artists: List<ArtistRef> = emptyList(),
    val album: Album? = null,
    @SerialName("track_number") val trackNumber: Int? = null,
    val popularity: Int? = null,
    @SerialName("preview_url") val previewUrl: String? = null
) {
    val artistNames: String
        get() = artists.joinToString(", ") { it.name }
}

@Serializable
data class Artist(
    val id: String,
    val name: String,
    val uri: String,
    val images: List<Image> = emptyList(),
    val genres: List<String> = emptyList(),
    val popularity: Int? = null
)

@Serializable
data class Owner(
    val id: String,
    @SerialName("display_name") val displayName: String? = null
)

@Serializable
data class PlaylistTracksInfo(
    val total: Int = 0
)

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val uri: String,
    val description: String? = null,
    val images: List<Image> = emptyList(),
    val owner: Owner? = null,
    val tracks: PlaylistTracksInfo? = null,
    val public: Boolean? = null,
    val collaborative: Boolean = false
)

@Serializable
data class PlaylistItem(
    @SerialName("added_at") val addedAt: String? = null,
    val track: Track? = null
)

@Serializable
data class Page<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
    val limit: Int = 20,
    val offset: Int = 0,
    val next: String? = null
)

@Serializable
data class SearchResults(
    val tracks: Page<Track>? = null,
    val artists: Page<Artist>? = null,
    val albums: Page<Album>? = null,
    val playlists: Page<Playlist>? = null
)

@Serializable
data class PlaybackState(
    @SerialName("is_playing") val isPlaying: Boolean = false,
    @SerialName("progress_ms") val progressMs: Long? = 0L,
    val item: Track? = null,
    @SerialName("repeat_state") val repeatState: String = "off",
    @SerialName("shuffle_state") val shuffleState: Boolean = false
)

@Serializable
data class UserProfile(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    val images: List<Image> = emptyList(),
    val product: String? = null, // "premium" vs "free"
    val country: String? = null
)

@Serializable
data class LyricsLine(
    val timeMs: Long,
    val words: String
)

@Serializable
data class Lyrics(
    val syncType: String = "LINE_SYNCED",
    val lines: List<LyricsLine> = emptyList()
)
