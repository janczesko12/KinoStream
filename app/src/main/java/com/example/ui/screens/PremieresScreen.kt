package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Movie
import com.example.model.StreamingPlatform
import com.example.ui.components.FilmwebRatingBadge
import com.example.ui.components.MoviePosterImage
import com.example.ui.components.PlatformBadgesRow
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.FilmwebGreen
import com.example.ui.theme.RoyalBlue

@Composable
fun PremieresScreen(
    premieres: List<Movie>,
    reminderIds: Set<String>,
    selectedPlatform: StreamingPlatform?,
    notificationsEnabled: Boolean,
    autoAddNewReleasesEnabled: Boolean = true,
    autoSyncedCount: Int = 0,
    isSyncing: Boolean = false,
    minFilmwebRating: Double = 6.5,
    maxFilmwebRating: Double = 10.0,
    onPlatformSelect: (StreamingPlatform?) -> Unit,
    onToggleReminder: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onNavigateToSettings: () -> Unit,
    onSendTestNotification: () -> Unit,
    onToggleAutoAddNewReleases: (Boolean) -> Unit = {},
    onSyncNewReleasesNow: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .testTag("screen_premieres")
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                RoyalBlue.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Column {
                    Text(
                        text = "Premiery i Auto-Nowości",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Netflix • Prime Video • Max • Disney+ • SkyShowtime",
                        style = MaterialTheme.typography.bodySmall,
                        color = RoyalBlue,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto-Adding New Releases Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(
                                listOf(RoyalBlue.copy(alpha = 0.5f), AccentCyan.copy(alpha = 0.4f))
                            )
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = RoyalBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Automatyczne dodawanie premier",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Gdy film lub serial wyjdzie na Netflix, Prime, Max, Disney+ lub SkyShowtime",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = autoAddNewReleasesEnabled,
                                    onCheckedChange = onToggleAutoAddNewReleases,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = RoyalBlue
                                    )
                                )
                            }

                            if (autoSyncedCount > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "✔️ Automatycznie dodano $autoSyncedCount nowych premier z platform VOD",
                                    color = FilmwebGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onSyncNewReleasesNow,
                                    enabled = !isSyncing,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_sync_premieres"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RoyalBlue
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Sync,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isSyncing) "Pobieranie nowości..." else "Pobierz nowe premiery",
                                        fontSize = 12.sp
                                    )
                                }

                                Button(
                                    onClick = onSendTestNotification,
                                    modifier = Modifier.testTag("btn_test_notification"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Notifications,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Alert", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5 Platforms Selector Filter (Netflix, Prime Video, Max, Disney+, SkyShowtime)
        item(contentType = "platform_selector") {
            Column {
                Text(
                    text = "Filtruj premiery wg platformy:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlatformFilterPill(
                        title = "Wszystkie",
                        isSelected = selectedPlatform == null,
                        brandColor = RoyalBlue,
                        onClick = { onPlatformSelect(null) },
                        modifier = Modifier.width(86.dp)
                    )
                    PlatformFilterPill(
                        title = "Netflix",
                        isSelected = selectedPlatform == StreamingPlatform.NETFLIX,
                        brandColor = StreamingPlatform.NETFLIX.brandColor,
                        onClick = { onPlatformSelect(StreamingPlatform.NETFLIX) },
                        modifier = Modifier.width(80.dp)
                    )
                    PlatformFilterPill(
                        title = "Prime Video",
                        isSelected = selectedPlatform == StreamingPlatform.PRIME_VIDEO,
                        brandColor = StreamingPlatform.PRIME_VIDEO.brandColor,
                        onClick = { onPlatformSelect(StreamingPlatform.PRIME_VIDEO) },
                        modifier = Modifier.width(96.dp)
                    )
                    PlatformFilterPill(
                        title = "Max",
                        isSelected = selectedPlatform == StreamingPlatform.MAX,
                        brandColor = StreamingPlatform.MAX.brandColor,
                        onClick = { onPlatformSelect(StreamingPlatform.MAX) },
                        modifier = Modifier.width(72.dp)
                    )
                    PlatformFilterPill(
                        title = "Disney+",
                        isSelected = selectedPlatform == StreamingPlatform.DISNEY_PLUS,
                        brandColor = StreamingPlatform.DISNEY_PLUS.brandColor,
                        onClick = { onPlatformSelect(StreamingPlatform.DISNEY_PLUS) },
                        modifier = Modifier.width(82.dp)
                    )
                    PlatformFilterPill(
                        title = "SkyShowtime",
                        isSelected = selectedPlatform == StreamingPlatform.SKYSHOWTIME,
                        brandColor = StreamingPlatform.SKYSHOWTIME.brandColor,
                        onClick = { onPlatformSelect(StreamingPlatform.SKYSHOWTIME) },
                        modifier = Modifier.width(106.dp)
                    )
                }
            }
        }

        // List of Premieres
        items(
            items = premieres,
            key = { it.id },
            contentType = { "premiere_card" }
        ) { movie ->
            val isReminded = reminderIds.contains(movie.id)

            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("premiere_card_${movie.id}")
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (isReminded) RoyalBlue.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onMovieClick(movie) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Premiere date pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (movie.isAutoSynced) FilmwebGreen.copy(alpha = 0.15f)
                                        else RoyalBlue.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "🗓️  ${movie.premiereDate ?: "Premiera wkrótce"}",
                                    color = if (movie.isAutoSynced) FilmwebGreen else RoyalBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Reminder bell icon
                            IconButton(
                                onClick = { onToggleReminder(movie) },
                                modifier = Modifier
                                    .testTag("btn_reminder_${movie.id}")
                                    .size(36.dp)
                                    .background(
                                        if (isReminded) RoyalBlue else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isReminded) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone,
                                    contentDescription = "Przypomnij o premierze",
                                    tint = if (isReminded) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Real Poster Image
                            MoviePosterImage(
                                movie = movie,
                                showBadges = true,
                                cornerRadius = 10.dp,
                                modifier = Modifier.size(86.dp, 124.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = movie.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "${movie.genre.displayName} • ${movie.durationMinutes} min",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = movie.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    FilmwebRatingBadge(
                                        rating = movie.filmwebRating,
                                        minRating = minFilmwebRating,
                                        maxRating = maxFilmwebRating,
                                        compact = true
                                    )

                                    PlatformBadgesRow(
                                        platforms = movie.platforms,
                                        compact = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
