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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ContentTypeFilter
import com.example.model.Movie
import com.example.model.MovieGenre
import com.example.model.MovieSortOption
import com.example.model.StreamingPlatform
import com.example.ui.components.FilmwebRatingBadge
import com.example.ui.components.MovieCard
import com.example.ui.components.MoviePosterImage
import com.example.ui.components.SupportFooterSection
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.FilmwebGreen
import com.example.ui.theme.RoyalBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    movies: List<Movie>,
    favoriteIds: Set<String>,
    watchedIds: Set<String>,
    searchQuery: String,
    selectedPlatform: StreamingPlatform?,
    selectedGenre: MovieGenre,
    onlyHighRating: Boolean,
    sortOption: MovieSortOption,
    minFilmwebRating: Double = 6.5,
    maxFilmwebRating: Double = 10.0,
    autoSyncedCount: Int = 0,
    usePolishTitles: Boolean = true,
    contentTypeFilter: ContentTypeFilter = ContentTypeFilter.ALL,
    onSearchQueryChange: (String) -> Unit,
    onPlatformSelect: (StreamingPlatform?) -> Unit,
    onContentTypeSelect: (ContentTypeFilter) -> Unit = {},
    onGenreSelect: (MovieGenre) -> Unit,
    onToggleHighRating: () -> Unit,
    onSortOptionSelect: (MovieSortOption) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    onToggleWatched: (Movie) -> Unit,
    watchlistIds: Set<String> = emptySet(),
    onToggleWatchlist: (Movie) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .testTag("screen_discover")
            .fillMaxSize()
    ) {
        // Pinned Top Search Bar Section at the Top of the Main Screen
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("main_search_container"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // App Branding & Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_app_logo),
                            contentDescription = "Logo KinoStream",
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Text(
                            text = "KinoStream",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(RoyalBlue.copy(alpha = 0.15f))
                                .border(1.dp, RoyalBlue.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "5 PLATFORM • PL DUBBING",
                                color = RoyalBlue,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = "Wyniki: ${movies.size}",
                            fontSize = 12.sp,
                            color = RoyalBlue,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (autoSyncedCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(FilmwebGreen.copy(alpha = 0.14f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = FilmwebGreen,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "+$autoSyncedCount nowości VOD",
                                color = FilmwebGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dedicated Search Bar at the Top of Screen
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .testTag("search_input")
                        .fillMaxWidth(),
                    placeholder = {
                        Text(
                            if (usePolishTitles) {
                                "Szukaj polskiego tytułu (np. Incepcja, Diuna, Chłopaki, Król Tulsy)..."
                            } else {
                                "Szukaj filmu lub serialu (np. Inception, Dune, The Boys, Tulsa King)..."
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Szukaj filmu",
                            tint = RoyalBlue
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.testTag("clear_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Wyczyść wyszukiwanie",
                                    tint = RoyalBlue
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoyalBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    )
                )

                if (searchQuery.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Szukasz: „$searchQuery”",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Wyczyść filtr",
                            fontSize = 12.sp,
                            color = RoyalBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onSearchQueryChange("") }
                                .padding(2.dp)
                        )
                    }
                }
            }
        }

        // Scrollable catalog and platform/genre controls
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Content Type Filter Row: Wszystkie | 🎬 Filmy | 📺 Seriale
            item(contentType = "content_type_selector") {
                Column {
                    Text(
                        text = "Typ treści (Filmy / Seriale):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val types = listOf(
                            Triple(ContentTypeFilter.ALL, "✨ Wszystkie", RoyalBlue),
                            Triple(ContentTypeFilter.MOVIES, "🎬 Filmy", RoyalBlue),
                            Triple(ContentTypeFilter.SERIES, "📺 Seriale", AccentCyan)
                        )
                        types.forEach { (filter, label, activeColor) ->
                            val isSelected = contentTypeFilter == filter
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("filter_type_${filter.name.lowercase()}")
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) activeColor else MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.5.dp,
                                        if (isSelected) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onContentTypeSelect(filter) }
                                    .padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 5 Streaming Platforms Selector Row (Netflix, Prime Video, Max, Disney+, SkyShowtime)
            item(contentType = "platform_selector") {
                Column {
                    Text(
                        text = "Platforma VOD (tylko filmy i seriale z polskim dubbingiem):",
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

            // Featured Real Poster Showcase Carousel (when not actively filtering by text)
            if (searchQuery.isEmpty() && movies.isNotEmpty()) {
                item {
                    val featuredList = remember(movies, minFilmwebRating, maxFilmwebRating) {
                        movies.filter { it.filmwebRating in minFilmwebRating..maxFilmwebRating }.take(10)
                    }
                    if (featuredList.isNotEmpty()) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Hity: Filmy i Seriale",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Oficjalne plakaty",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RoyalBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = featuredList,
                                    key = { "featured_${it.id}" },
                                    contentType = { "featured_poster" }
                                ) { movie ->
                                    FeaturedPosterItem(
                                        movie = movie,
                                        isWatched = watchedIds.contains(movie.id),
                                        minFilmwebRating = minFilmwebRating,
                                        maxFilmwebRating = maxFilmwebRating,
                                        onClick = { onMovieClick(movie) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Movie Genres Filter Row (Horizontal Scrolling)
            item(contentType = "genre_selector") {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sortowanie po gatunku:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Sort By Menu Anchor
                        Box {
                            Row(
                                modifier = Modifier
                                    .testTag("btn_sort_menu")
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showSortMenu = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FilterList,
                                    contentDescription = "Sortowanie",
                                    tint = RoyalBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = sortOption.displayName.substringBefore(" ("),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBlue
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                MovieSortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.displayName,
                                                fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal,
                                                color = if (option == sortOption) RoyalBlue else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            onSortOptionSelect(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Genre Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MovieGenre.values().forEach { genre ->
                            val isSelected = selectedGenre == genre
                            FilterChip(
                                selected = isSelected,
                                onClick = { onGenreSelect(genre) },
                                label = {
                                    Text(
                                        text = genre.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoyalBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) RoyalBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }
            }

            // Results Counter
            item(contentType = "rating_filter_bar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Znaleziono: ${movies.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Movie List
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
                            Text(text = "🎬", fontSize = 48.sp)
                            Text(
                                text = "Brak filmów lub seriali spełniających kryteria",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Zmień platformę, dostosuj przedział oceny Filmweb w Ustawieniach lub wpisz inny tytuł.",
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

            // Support Footer at bottom of catalog ("Postaw mi kawę" + "Created with ❤️ by: Jan Czesko")
            item(contentType = "support_footer") {
                SupportFooterSection(
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun FeaturedPosterItem(
    movie: Movie,
    isWatched: Boolean,
    minFilmwebRating: Double,
    maxFilmwebRating: Double,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        Box {
            MoviePosterImage(
                movie = movie,
                isWatched = isWatched,
                showBadges = true,
                cornerRadius = 12.dp,
                modifier = Modifier
                    .width(120.dp)
                    .height(174.dp)
            )

            FilmwebRatingBadge(
                rating = movie.filmwebRating,
                minRating = minFilmwebRating,
                maxRating = maxFilmwebRating,
                compact = true,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = movie.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "${movie.platforms.first().displayName} • ${if (movie.isSeries) "Serial" else "Film"}",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun PlatformFilterPill(
    title: String,
    isSelected: Boolean,
    brandColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("filter_platform_${title.lowercase()}")
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) brandColor else MaterialTheme.colorScheme.surface)
            .border(
                1.5.dp,
                if (isSelected) brandColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
