package rocks.spotifast.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import rocks.spotifast.SpotifastApp
import rocks.spotifast.data.models.Lyrics
import rocks.spotifast.data.models.Track
import rocks.spotifast.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingBottomSheet(
    track: Track,
    isPlaying: Boolean,
    progressMs: Long,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = remember { SpotifastApp.instance.spotifyRepository }
    var lyrics by remember { mutableStateOf<Lyrics?>(null) }
    val lyricsListState = rememberLazyListState()
    val scrollState = rememberScrollState()

    LaunchedEffect(track.id) {
        lyrics = repository.getLyrics(track)
    }

    // Auto-scroll lyrics to active line
    val activeLineIndex = remember(lyrics, progressMs) {
        val lines = lyrics?.lines ?: emptyList()
        lines.indexOfLast { it.timeMs <= progressMs }
    }

    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0) {
            lyricsListState.animateScrollToItem(activeLineIndex)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkBackground,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = TextSecondary)
        },
        modifier = modifier.fillMaxHeight(0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Album art - perfectly scaled
            val imageUrl = track.album?.images?.firstOrNull()?.url
            AsyncImage(
                model = imageUrl,
                contentDescription = track.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Track info & heart
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.name,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.artistNames,
                        color = TextSecondary,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { /* Toggle like */ }) {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = "Save",
                        tint = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrubber
            var sliderPosition by remember { mutableFloatStateOf(0f) }
            var isUserDragging by remember { mutableStateOf(false) }

            val currentSliderValue = if (isUserDragging) {
                sliderPosition
            } else if (track.durationMs > 0) {
                (progressMs.toFloat() / track.durationMs).coerceIn(0f, 1f)
            } else 0f

            Slider(
                value = currentSliderValue,
                onValueChange = {
                    isUserDragging = true
                    sliderPosition = it
                },
                onValueChangeFinished = {
                    isUserDragging = false
                    val targetMs = (sliderPosition * track.durationMs).toLong()
                    onSeek(targetMs)
                },
                colors = SliderDefaults.colors(
                    thumbColor = TextPrimary,
                    activeTrackColor = SpotifyGreen,
                    inactiveTrackColor = DarkBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currentSec = progressMs / 1000
                Text(
                    text = String.format("%d:%02d", currentSec / 60, currentSec % 60),
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                val totalSec = track.durationMs / 1000
                Text(
                    text = String.format("%d:%02d", totalSec / 60, totalSec % 60),
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Transport controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* Shuffle */ }) {
                    Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = TextSecondary)
                }
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = TextPrimary, modifier = Modifier.size(36.dp))
                }
                FilledIconButton(
                    onClick = onPlayPauseClick,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = TextPrimary),
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = DarkBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = TextPrimary, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { /* Repeat */ }) {
                    Icon(Icons.Default.Repeat, contentDescription = "Repeat", tint = TextSecondary)
                }
            }

            // Synchronized lyrics section
            val lines = lyrics?.lines ?: emptyList()
            if (lines.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Lyrics",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkCard)
                        .padding(16.dp)
                ) {
                    LazyColumn(
                        state = lyricsListState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(lines) { index, line ->
                            val isActive = index == activeLineIndex
                            Text(
                                text = line.words,
                                color = if (isActive) SpotifyGreen else TextMuted,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                fontSize = if (isActive) 16.sp else 14.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
