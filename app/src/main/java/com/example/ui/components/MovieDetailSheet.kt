package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Movie
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.FilmwebGreen
import com.example.ui.theme.FilmwebRed
import com.example.ui.theme.FilmwebYellow
import com.example.ui.theme.RoyalBlue
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MovieDetailSheet(
    movie: Movie,
    isFavorite: Boolean,
    isWatched: Boolean,
    isReminded: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleWatched: () -> Unit,
    onToggleReminder: () -> Unit,
    minFilmwebRating: Double = 6.5,
    maxFilmwebRating: Double = 10.0
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isWithinRange = movie.filmwebRating in minFilmwebRating..maxFilmwebRating
    val rangeLabel = if (maxFilmwebRating >= 10.0) {
        String.format(Locale.US, "≥ %.1f", minFilmwebRating)
    } else {
        String.format(Locale.US, "%.1f – %.1f", minFilmwebRating, maxFilmwebRating)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surfaceColor,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Backdrop with Real Movie/Series Photo + Floating Poster
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = movie.posterGradientColors.map { Color(it) } + listOf(surfaceColor)
                        )
                    )
            ) {
                val heroUrl = movie.backdropUrl.ifBlank { movie.posterUrl }
                val backdropRequest = androidx.compose.runtime.remember(context, heroUrl) {
                    if (heroUrl.isBlank()) null
                    else ImageRequest.Builder(context)
                        .data(heroUrl)
                        .size(780, 440)
                        .memoryCacheKey(heroUrl)
                        .diskCacheKey(heroUrl)
                        .crossfade(true)
                        .setHeader("User-Agent", "Mozilla/5.0 (Android; KinoStream/1.0)")
                        .build()
                }
                if (backdropRequest != null) {
                    AsyncImage(
                        model = backdropRequest,
                        contentDescription = "Tło: ${movie.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        alpha = 0.65f
                    )
                }

                // Gradient overlay fading into surface
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.55f),
                                    surfaceColor
                                )
                            )
                        )
                )

                // Top Action: Close
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Zamknij",
                        tint = Color.White
                    )
                }

                // Bottom row inside hero: Floating Real Poster + Title & Platform Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    MoviePosterImage(
                        movie = movie,
                        isWatched = isWatched,
                        showBadges = false,
                        cornerRadius = 12.dp,
                        modifier = Modifier
                            .width(104.dp)
                            .height(152.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlatformBadgesRow(platforms = movie.platforms, compact = false)

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (movie.isSeries) AccentCyan.copy(alpha = 0.2f) else RoyalBlue.copy(alpha = 0.2f))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (movie.isSeries) "SERIAL" else "FILM",
                                    color = if (movie.isSeries) AccentCyan else RoyalBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        if (movie.isPremiere) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(RoyalBlue)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (movie.isAutoSynced) "AUTO-PREMIERA VOD" else "NOWA PREMIERA",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = movie.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "${movie.originalTitle} (${movie.year}) • ${movie.durationMinutes} min • ${movie.ageRating}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Body Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Primary Quick Actions: Heart (Ulubione) & Checkmark (Obejrzano)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("detail_fav_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFavorite) FilmwebRed else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isFavorite) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFavorite) "Ulubiony" else "Do ulubionych",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onToggleWatched,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("detail_watched_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isWatched) FilmwebGreen else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isWatched) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isWatched) Icons.Filled.Check else Icons.Outlined.CheckCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isWatched) "Obejrzano ✔️" else "Oznacz obejrzany",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (movie.isPremiere || movie.isUpcoming) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FilledTonalButton(
                        onClick = onToggleReminder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_reminder_button"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isReminded) RoyalBlue else RoyalBlue.copy(alpha = 0.15f),
                            contentColor = if (isReminded) Color.White else RoyalBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isReminded) Icons.Filled.Notifications else Icons.Filled.NotificationsNone,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isReminded) "Przypomnienie włączone (${movie.premiereDate})" else "Powiadom o premierze (${movie.premiereDate ?: "Wkrótce"})",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Filmweb Rating Card (highlighting the custom range & red text rule!)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isWithinRange) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        } else {
                            FilmwebRed.copy(alpha = 0.10f)
                        }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            if (isWithinRange) {
                                listOf(RoyalBlue.copy(alpha = 0.4f), AccentCyan.copy(alpha = 0.4f))
                            } else {
                                listOf(FilmwebRed.copy(alpha = 0.6f), FilmwebRed.copy(alpha = 0.3f))
                            }
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = if (isWithinRange) FilmwebYellow else FilmwebRed,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f", movie.filmwebRating),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isWithinRange) MaterialTheme.colorScheme.onSurface else FilmwebRed
                                )
                                Text(
                                    text = "/ 10",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "Ocena Filmweb (${NumberFormat.getNumberInstance(Locale.GERMANY).format(movie.filmwebVotesCount)} głosów)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isWithinRange) {
                                        FilmwebGreen.copy(alpha = 0.15f)
                                    } else {
                                        FilmwebRed.copy(alpha = 0.15f)
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isWithinRange) "W przedziale ($rangeLabel)" else "Poza zakresem ($rangeLabel)",
                                color = if (isWithinRange) FilmwebGreen else FilmwebRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Synopsis
                Text(
                    text = if (movie.isSeries) "Opis serialu" else "Opis filmu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = movie.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Director & Cast
                Text(
                    text = "Twórcy i obsada",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Reżyseria / Twórcy: ${movie.director}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = RoyalBlue
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    movie.cast.forEach { actor ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = actor,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
