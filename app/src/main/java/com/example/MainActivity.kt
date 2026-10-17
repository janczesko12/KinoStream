package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.components.MovieDetailSheet
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.MyListScreen
import com.example.ui.screens.PremieresScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.KinoStreamTheme
import com.example.ui.theme.RoyalBlue

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            KinoStreamTheme(themeMode = themeMode) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val filteredMovies by viewModel.filteredMovies.collectAsStateWithLifecycle()
    val premiereMovies by viewModel.premiereMovies.collectAsStateWithLifecycle()
    val savedMovies by viewModel.savedMovies.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val watchedIds by viewModel.watchedIds.collectAsStateWithLifecycle()
    val reminderIds by viewModel.reminderIds.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val selectedMovie by viewModel.selectedMovie.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedPlatform by viewModel.selectedPlatform.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
    val onlyHighRating by viewModel.onlyHighRating.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val myListFilter by viewModel.myListFilter.collectAsStateWithLifecycle()
    val currentThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val enabledNotificationPlatforms by viewModel.enabledNotificationPlatforms.collectAsStateWithLifecycle()
    val minFilmwebRating by viewModel.minFilmwebRating.collectAsStateWithLifecycle()
    val maxFilmwebRating by viewModel.maxFilmwebRating.collectAsStateWithLifecycle()
    val autoAddNewReleasesEnabled by viewModel.autoAddNewReleasesEnabled.collectAsStateWithLifecycle()
    val autoSyncedCount by viewModel.autoSyncedCount.collectAsStateWithLifecycle()
    val isSyncingNewReleases by viewModel.isSyncingNewReleases.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Handle back button when on subtabs or detail sheet
    BackHandler(enabled = selectedMovie != null || currentTab != MainTab.DISCOVER) {
        if (selectedMovie != null) {
            viewModel.selectMovie(null)
        } else if (currentTab != MainTab.DISCOVER) {
            viewModel.setTab(MainTab.DISCOVER)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .testTag("bottom_nav_bar")
                    .windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                // 1. Katalog / Odkrywaj
                NavigationBarItem(
                    selected = currentTab == MainTab.DISCOVER,
                    onClick = { viewModel.setTab(MainTab.DISCOVER) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.DISCOVER) Icons.Filled.Movie else Icons.Outlined.Movie,
                            contentDescription = "Katalog"
                        )
                    },
                    label = {
                        Text(
                            text = "Katalog",
                            fontSize = 11.sp,
                            fontWeight = if (currentTab == MainTab.DISCOVER) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = RoyalBlue,
                        indicatorColor = RoyalBlue,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // 2. Premiery
                NavigationBarItem(
                    selected = currentTab == MainTab.PREMIERES,
                    onClick = { viewModel.setTab(MainTab.PREMIERES) },
                    icon = {
                        BadgedBox(
                            badge = {
                                val badgeCount = reminderIds.size + autoSyncedCount
                                if (badgeCount > 0) {
                                    Badge(
                                        containerColor = RoyalBlue,
                                        contentColor = Color.White
                                    ) {
                                        Text("$badgeCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentTab == MainTab.PREMIERES) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = "Premiery"
                            )
                        }
                    },
                    label = {
                        Text(
                            text = "Premiery",
                            fontSize = 11.sp,
                            fontWeight = if (currentTab == MainTab.PREMIERES) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = RoyalBlue,
                        indicatorColor = RoyalBlue,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // 3. Moja Lista
                NavigationBarItem(
                    selected = currentTab == MainTab.MY_LIST,
                    onClick = { viewModel.setTab(MainTab.MY_LIST) },
                    icon = {
                        BadgedBox(
                            badge = {
                                val totalSaved = favoriteIds.size + watchedIds.size
                                if (totalSaved > 0) {
                                    Badge(
                                        containerColor = RoyalBlue,
                                        contentColor = Color.White
                                    ) {
                                        Text("$totalSaved")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentTab == MainTab.MY_LIST) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Moja Lista"
                            )
                        }
                    },
                    label = {
                        Text(
                            text = "Moja Lista",
                            fontSize = 11.sp,
                            fontWeight = if (currentTab == MainTab.MY_LIST) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = RoyalBlue,
                        indicatorColor = RoyalBlue,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // 4. Ustawienia
                NavigationBarItem(
                    selected = currentTab == MainTab.SETTINGS,
                    onClick = { viewModel.setTab(MainTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Ustawienia"
                        )
                    },
                    label = {
                        Text(
                            text = "Ustawienia",
                            fontSize = 11.sp,
                            fontWeight = if (currentTab == MainTab.SETTINGS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = RoyalBlue,
                        indicatorColor = RoyalBlue,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.DISCOVER -> {
                    DiscoverScreen(
                        movies = filteredMovies,
                        favoriteIds = favoriteIds,
                        watchedIds = watchedIds,
                        searchQuery = searchQuery,
                        selectedPlatform = selectedPlatform,
                        selectedGenre = selectedGenre,
                        onlyHighRating = onlyHighRating,
                        sortOption = sortOption,
                        minFilmwebRating = minFilmwebRating,
                        maxFilmwebRating = maxFilmwebRating,
                        autoSyncedCount = autoSyncedCount,
                        onSearchQueryChange = viewModel::setSearchQuery,
                        onPlatformSelect = viewModel::setPlatform,
                        onGenreSelect = viewModel::setGenre,
                        onToggleHighRating = viewModel::toggleHighRatingOnly,
                        onSortOptionSelect = viewModel::setSortOption,
                        onMovieClick = viewModel::selectMovie,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onToggleWatched = viewModel::toggleWatched
                    )
                }

                MainTab.PREMIERES -> {
                    PremieresScreen(
                        premieres = premiereMovies,
                        reminderIds = reminderIds,
                        selectedPlatform = selectedPlatform,
                        notificationsEnabled = notificationsEnabled,
                        autoAddNewReleasesEnabled = autoAddNewReleasesEnabled,
                        autoSyncedCount = autoSyncedCount,
                        isSyncing = isSyncingNewReleases,
                        minFilmwebRating = minFilmwebRating,
                        maxFilmwebRating = maxFilmwebRating,
                        onPlatformSelect = viewModel::setPlatform,
                        onToggleReminder = viewModel::toggleReminder,
                        onMovieClick = viewModel::selectMovie,
                        onNavigateToSettings = { viewModel.setTab(MainTab.SETTINGS) },
                        onSendTestNotification = viewModel::sendTestNotification,
                        onToggleAutoAddNewReleases = viewModel::setAutoAddNewReleasesEnabled,
                        onSyncNewReleasesNow = viewModel::syncNewReleasesNow
                    )
                }

                MainTab.MY_LIST -> {
                    MyListScreen(
                        movies = savedMovies,
                        favoriteIds = favoriteIds,
                        watchedIds = watchedIds,
                        currentFilter = myListFilter,
                        stats = stats,
                        minFilmwebRating = minFilmwebRating,
                        maxFilmwebRating = maxFilmwebRating,
                        onFilterChange = viewModel::setMyListFilter,
                        onMovieClick = viewModel::selectMovie,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onToggleWatched = viewModel::toggleWatched
                    )
                }

                MainTab.SETTINGS -> {
                    SettingsScreen(
                        currentThemeMode = currentThemeMode,
                        notificationsEnabled = notificationsEnabled,
                        enabledNotificationPlatforms = enabledNotificationPlatforms,
                        minFilmwebRating = minFilmwebRating,
                        maxFilmwebRating = maxFilmwebRating,
                        autoAddNewReleasesEnabled = autoAddNewReleasesEnabled,
                        autoSyncedCount = autoSyncedCount,
                        isSyncingNewReleases = isSyncingNewReleases,
                        onThemeModeChange = viewModel::setThemeMode,
                        onNotificationsEnabledChange = viewModel::setNotificationsEnabled,
                        onToggleNotificationPlatform = viewModel::toggleNotificationPlatform,
                        onSendTestNotification = viewModel::sendTestNotification,
                        onFilmwebRatingRangeChange = viewModel::setFilmwebRatingRange,
                        onAutoAddNewReleasesChange = viewModel::setAutoAddNewReleasesEnabled,
                        onSyncNewReleasesNow = viewModel::syncNewReleasesNow
                    )
                }
            }
        }
    }

    // Modal detail sheet
    selectedMovie?.let { movie ->
        MovieDetailSheet(
            movie = movie,
            isFavorite = favoriteIds.contains(movie.id),
            isWatched = watchedIds.contains(movie.id),
            isReminded = reminderIds.contains(movie.id),
            minFilmwebRating = minFilmwebRating,
            maxFilmwebRating = maxFilmwebRating,
            onDismiss = { viewModel.selectMovie(null) },
            onToggleFavorite = { viewModel.toggleFavorite(movie) },
            onToggleWatched = { viewModel.toggleWatched(movie) },
            onToggleReminder = { viewModel.toggleReminder(movie) }
        )
    }
}
