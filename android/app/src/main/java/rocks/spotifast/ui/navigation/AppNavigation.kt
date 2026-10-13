package rocks.spotifast.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import rocks.spotifast.SpotifastApp
import rocks.spotifast.data.models.Track
import rocks.spotifast.native.LibrespotBridge
import rocks.spotifast.ui.components.MiniPlayerBar
import rocks.spotifast.ui.screens.*
import rocks.spotifast.ui.theme.*

enum class NavScreen(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    SEARCH("Search", Icons.Default.Search),
    LIBRARY("Your Library", Icons.Default.LibraryMusic)
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val authManager = remember { SpotifastApp.instance.authManager }
    val isAuthenticated by authManager.authState.collectAsState()
    var isDemoMode by remember { mutableStateOf(false) }

    var currentScreen by remember { mutableStateOf(NavScreen.HOME) }
    var currentTrack by remember { mutableStateOf<Track?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var progressMs by remember { mutableLongStateOf(0L) }
    var showNowPlayingSheet by remember { mutableStateOf(false) }

    // Progress tick
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(1000)
            progressMs = LibrespotBridge.position()
        }
    }

    if (!isAuthenticated && !isDemoMode) {
        LoginScreen(
            onLoginSuccess = { isDemoMode = true }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = DarkBackground,
                    contentColor = TextPrimary
                ) {
                    NavScreen.entries.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SpotifyGreen,
                                selectedTextColor = SpotifyGreen,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = DarkCard
                            )
                        )
                    }
                }
            },
            modifier = modifier
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Main screen routing
                when (currentScreen) {
                    NavScreen.HOME -> HomeScreen(
                        onTrackClick = { track ->
                            currentTrack = track
                            isPlaying = true
                            progressMs = 0L
                            LibrespotBridge.loadTrack(track.uri, track.durationMs, track.previewUrl)
                        }
                    )
                    NavScreen.SEARCH -> SearchScreen(
                        onTrackClick = { track ->
                            currentTrack = track
                            isPlaying = true
                            progressMs = 0L
                            LibrespotBridge.loadTrack(track.uri, track.durationMs, track.previewUrl)
                        }
                    )
                    NavScreen.LIBRARY -> LibraryScreen(
                        onTrackClick = { track ->
                            currentTrack = track
                            isPlaying = true
                            progressMs = 0L
                            LibrespotBridge.loadTrack(track.uri, track.durationMs, track.previewUrl)
                        }
                    )
                }

                // Docked MiniPlayer
                currentTrack?.let { track ->
                    MiniPlayerBar(
                        track = track,
                        isPlaying = isPlaying,
                        progressMs = progressMs,
                        onPlayPauseClick = {
                            if (isPlaying) {
                                LibrespotBridge.pause()
                                isPlaying = false
                            } else {
                                LibrespotBridge.play()
                                isPlaying = true
                            }
                        },
                        onClick = { showNowPlayingSheet = true },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                // Full NowPlaying BottomSheet
                if (showNowPlayingSheet && currentTrack != null) {
                    NowPlayingBottomSheet(
                        track = currentTrack!!,
                        isPlaying = isPlaying,
                        progressMs = progressMs,
                        onPlayPauseClick = {
                            if (isPlaying) {
                                LibrespotBridge.pause()
                                isPlaying = false
                            } else {
                                LibrespotBridge.play()
                                isPlaying = true
                            }
                        },
                        onSeek = { targetMs ->
                            progressMs = targetMs
                            LibrespotBridge.seek(targetMs)
                        },
                        onNext = {
                            val tracks = rocks.spotifast.data.DemoCatalog.demoTracks
                            val currentIndex = tracks.indexOfFirst { it.uri == currentTrack?.uri }
                            val nextIndex = (currentIndex + 1) % tracks.size
                            val nextTrack = tracks[nextIndex]
                            currentTrack = nextTrack
                            isPlaying = true
                            progressMs = 0L
                            LibrespotBridge.loadTrack(nextTrack.uri, nextTrack.durationMs, nextTrack.previewUrl)
                        },
                        onPrevious = {
                            val tracks = rocks.spotifast.data.DemoCatalog.demoTracks
                            val currentIndex = tracks.indexOfFirst { it.uri == currentTrack?.uri }
                            val prevIndex = if (currentIndex <= 0) tracks.size - 1 else currentIndex - 1
                            val prevTrack = tracks[prevIndex]
                            currentTrack = prevTrack
                            isPlaying = true
                            progressMs = 0L
                            LibrespotBridge.loadTrack(prevTrack.uri, prevTrack.durationMs, prevTrack.previewUrl)
                        },
                        onDismiss = { showNowPlayingSheet = false }
                    )
                }
            }
        }
    }
}
