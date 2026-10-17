package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Movie
import com.example.ui.MyListFilter
import com.example.ui.UiStats
import com.example.ui.components.MovieCard
import com.example.ui.components.SupportFooterSection
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.FilmwebGreen
import com.example.ui.theme.FilmwebRed
import com.example.ui.theme.RoyalBlue
import java.util.Locale

@Composable
fun MyListScreen(
    movies: List<Movie>,
    favoriteIds: Set<String>,
    watchedIds: Set<String>,
    currentFilter: MyListFilter,
    stats: UiStats,
    minFilmwebRating: Double = 6.5,
    maxFilmwebRating: Double = 10.0,
    watchlistIds: Set<String> = emptySet(),
    onFilterChange: (MyListFilter) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    onToggleWatched: (Movie) -> Unit,
    onToggleWatchlist: (Movie) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .testTag("screen_my_list")
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Statistics
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
                        text = "Moja Filmoteka",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Lista Do obejrzenia (baza Room), ulubione i obejrzane",
                        style = MaterialTheme.typography.bodySmall,
                        color = RoyalBlue,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stats Cards Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "Do obejrzenia",
                            value = "${stats.watchlistCount}",
                            icon = "🔖",
                            accentColor = AccentCyan,
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            label = "Ulubione",
                            value = "${stats.favoriteCount}",
                            icon = "❤️",
                            accentColor = FilmwebRed,
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            label = "Obejrzane",
                            value = "${stats.watchedCount}",
                            icon = "✔️",
                            accentColor = FilmwebGreen,
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            label = "Śr. ocen",
                            value = if (stats.watchedCount > 0) String.format(Locale.US, "%.1f", stats.averageWatchedRating) else "-",
                            icon = "⭐",
                            accentColor = RoyalBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Filter Tabs
        item(contentType = "my_list_tabs") {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                TabRow(
                    selectedTabIndex = currentFilter.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = RoyalBlue,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[currentFilter.ordinal]),
                            color = RoyalBlue,
                            height = 3.dp
                        )
                    }
                ) {
                    MyListFilter.values().forEach { filter ->
                        Tab(
                            selected = currentFilter == filter,
                            onClick = { onFilterChange(filter) },
                            text = {
                                Text(
                                    text = filter.label,
                                    fontWeight = if (currentFilter == filter) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }
            }
        }

        // Movies List
        if (movies.isEmpty()) {
            item(contentType = "empty_state") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = when (currentFilter) {
                                MyListFilter.WATCHLIST -> "🔖"
                                MyListFilter.FAVORITES -> "💔"
                                MyListFilter.WATCHED -> "👀"
                                MyListFilter.ALL -> "📂"
                            },
                            fontSize = 48.sp
                        )
                        Text(
                            text = when (currentFilter) {
                                MyListFilter.WATCHLIST -> "Brak filmów na liście Do obejrzenia"
                                MyListFilter.FAVORITES -> "Brak ulubionych filmów"
                                MyListFilter.WATCHED -> "Nie oznaczono jeszcze żadnego filmu"
                                MyListFilter.ALL -> "Twoja lista jest pusta"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Przejdź do zakładki Katalog i kliknij zakładkę 🔖, serduszko ❤️ lub ptaszek ✔️ przy filmie.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(
                items = movies,
                key = { it.id },
                contentType = { "movie_card" }
            ) { movie ->
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    MovieCard(
                        movie = movie,
                        isFavorite = favoriteIds.contains(movie.id),
                        isWatched = watchedIds.contains(movie.id),
                        isInWatchlist = watchlistIds.contains(movie.id),
                        minFilmwebRating = minFilmwebRating,
                        maxFilmwebRating = maxFilmwebRating,
                        onMovieClick = { onMovieClick(movie) },
                        onToggleFavorite = { onToggleFavorite(movie) },
                        onToggleWatched = { onToggleWatched(movie) },
                        onToggleWatchlist = { onToggleWatchlist(movie) }
                    )
                }
            }
        }

        // Support Footer at bottom
        item {
            SupportFooterSection(
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(
                listOf(accentColor.copy(alpha = 0.4f), Color.Transparent)
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
