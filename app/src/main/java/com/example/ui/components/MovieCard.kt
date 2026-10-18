package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Movie
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.FilmwebGreen
import com.example.ui.theme.FilmwebRed
import com.example.ui.theme.RoyalBlue

private val CardShape = RoundedCornerShape(16.dp)
private val TagShape = RoundedCornerShape(4.dp)

@Composable
fun MovieCard(
    movie: Movie,
    isFavorite: Boolean,
    isWatched: Boolean,
    onMovieClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleWatched: () -> Unit,
    modifier: Modifier = Modifier,
    isInWatchlist: Boolean = false,
    onToggleWatchlist: () -> Unit = {},
    minFilmwebRating: Double = 6.5,
    maxFilmwebRating: Double = 10.0
) {
    val cardBorderColor = when {
        isWatched -> FilmwebGreen.copy(alpha = 0.5f)
        isInWatchlist -> AccentCyan.copy(alpha = 0.6f)
        isFavorite -> RoyalBlue.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    }

    Card(
        modifier = modifier
            .testTag("movie_card_${movie.id}")
            .fillMaxWidth()
            .clip(CardShape)
            .border(1.dp, cardBorderColor, CardShape)
            .clickable(onClick = onMovieClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Real Movie / TV Series Poster Image
            MoviePosterImage(
                movie = movie,
                isWatched = isWatched,
                showBadges = true,
                cornerRadius = 12.dp,
                modifier = Modifier
                    .width(100.dp)
                    .height(144.dp)
            )

            // Movie Information Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(144.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Title
                    Text(
                        text = movie.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Type (Film/Serial), Genre & Duration Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(TagShape)
                                .background(RoyalBlue.copy(alpha = 0.14f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = movie.genre.displayName,
                                color = RoyalBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (movie.isSeries) {
                            Box(
                                modifier = Modifier
                                    .clip(TagShape)
                                    .background(AccentCyan.copy(alpha = 0.14f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Serial",
                                    color = AccentCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (movie.isAutoSynced) {
                            Box(
                                modifier = Modifier
                                    .clip(TagShape)
                                    .background(FilmwebGreen.copy(alpha = 0.14f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "AUTO",
                                    color = FilmwebGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(TagShape)
                                .background(
                                    if (movie.isAvailabilityVerified) FilmwebGreen.copy(alpha = 0.14f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (movie.isAvailabilityVerified) "PL Dubbing" else "Dostępność PL niepotwierdzona",
                                color = if (movie.isAvailabilityVerified) FilmwebGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = if (movie.isSeries) "• ${movie.durationMinutes}m" else "• ${movie.durationMinutes} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Filmweb Rating & Platforms Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FilmwebRatingBadge(
                        rating = movie.filmwebRating,
                        minRating = minFilmwebRating,
                        maxRating = maxFilmwebRating,
                        compact = true,
                        ratingSource = movie.ratingSource
                    )

                    PlatformBadgesRow(
                        platforms = if (movie.isAvailabilityVerified) movie.platforms else emptyList(),
                        compact = true
                    )
                }

                // Action Row: Heart (Ulubione) & Checkmark (Obejrzano)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val director = movie.director.trim()
                    if (
                        director.isNotBlank() &&
                        !director.equals("Brak danych", ignoreCase = true) &&
                        !director.equals("Unknown", ignoreCase = true)
                    ) {
                        Text(
                            text = "Reż. $director",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // Unavailable director metadata should not look like a missing platform.
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Przycisk "Do obejrzenia" (Watchlist - Room database)
                        IconButton(
                            onClick = onToggleWatchlist,
                            modifier = Modifier
                                .testTag("btn_watchlist_${movie.id}")
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isInWatchlist) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isInWatchlist) "Usuń z listy Do obejrzenia" else "Dodaj do listy Do obejrzenia",
                                tint = if (isInWatchlist) AccentCyan else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Mały ptaszek do odhaczenia gdy film już został obejrzany
                        IconButton(
                            onClick = onToggleWatched,
                            modifier = Modifier
                                .testTag("btn_watched_${movie.id}")
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isWatched) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                                contentDescription = if (isWatched) "Oznacz jako nieobejrzany" else "Oznacz jako obejrzany",
                                tint = if (isWatched) FilmwebGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Małe serce przy filmie i dodaje na listę ulubione
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .testTag("btn_favorite_${movie.id}")
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isFavorite) "Usuń z ulubionych" else "Dodaj do ulubionych",
                                tint = if (isFavorite) FilmwebRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
