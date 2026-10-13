package rocks.spotifast.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rocks.spotifast.SpotifastApp
import rocks.spotifast.data.models.Playlist
import rocks.spotifast.data.models.Track
import rocks.spotifast.ui.components.PlaylistCard
import rocks.spotifast.ui.components.TrackRow
import rocks.spotifast.ui.theme.*
import java.util.Calendar

@Composable
fun HomeScreen(
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = remember { SpotifastApp.instance.spotifyRepository }
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var recentTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        playlists = repository.getUserPlaylists().getOrDefault(emptyList())
        recentTracks = repository.getRecentlyPlayed().getOrDefault(emptyList())
        isLoading = false
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = SpotifyGreen)
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(bottom = 80.dp)
        ) {
            item {
                Text(
                    text = greeting,
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 16.dp)
                )
            }

            if (playlists.isNotEmpty()) {
                item {
                    Text(
                        text = "Your Playlists",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(playlists) { playlist ->
                            PlaylistCard(
                                playlist = playlist,
                                onClick = {
                                    recentTracks.firstOrNull()?.let { onTrackClick(it) }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            if (recentTracks.isNotEmpty()) {
                item {
                    Text(
                        text = "Recently Played",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(recentTracks) { track ->
                    TrackRow(
                        track = track,
                        onClick = { onTrackClick(track) }
                    )
                }
            }
        }
    }
}
