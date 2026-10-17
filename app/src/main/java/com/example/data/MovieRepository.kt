package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.KinoStreamDatabase
import com.example.data.local.WatchlistDao
import com.example.data.local.WatchlistEntity
import com.example.model.Movie
import com.example.model.StreamingPlatform
import com.example.ui.theme.ThemeMode
import com.example.util.NotificationHelper
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlin.math.round

class MovieRepository(
    private val context: Context,
    private val watchlistDao: WatchlistDao = KinoStreamDatabase.getInstance(context).watchlistDao()
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("kinostream_user_prefs", Context.MODE_PRIVATE)

    private val _movies = MutableStateFlow<List<Movie>>(loadCatalogWithCachedPosters())
    val movies: StateFlow<List<Movie>> = _movies.asStateFlow()

    val watchlistEntries: Flow<List<WatchlistEntity>> = watchlistDao.getAllWatchlistItems()
    val watchlistMovieIdsFlow: Flow<Set<String>> = watchlistDao.getAllWatchlistIds().map { it.toSet() }

    private val _favoriteMovieIds = MutableStateFlow<Set<String>>(loadStringSet("fav_movie_ids"))
    val favoriteMovieIds: StateFlow<Set<String>> = _favoriteMovieIds.asStateFlow()

    private val _watchedMovieIds = MutableStateFlow<Set<String>>(loadStringSet("watched_movie_ids"))
    val watchedMovieIds: StateFlow<Set<String>> = _watchedMovieIds.asStateFlow()

    private val _reminderMovieIds = MutableStateFlow<Set<String>>(loadStringSet("reminder_movie_ids"))
    val reminderMovieIds: StateFlow<Set<String>> = _reminderMovieIds.asStateFlow()

    private val _themeMode = MutableStateFlow<ThemeMode>(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow<Boolean>(
        prefs.getBoolean("notifications_enabled", true)
    )
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _enabledNotificationPlatforms = MutableStateFlow<Set<StreamingPlatform>>(
        loadNotificationPlatforms()
    )
    val enabledNotificationPlatforms: StateFlow<Set<StreamingPlatform>> =
        _enabledNotificationPlatforms.asStateFlow()

    // Automatic adding of new releases from Netflix, Prime Video, Max, Disney+
    private val _autoAddNewReleasesEnabled = MutableStateFlow<Boolean>(
        prefs.getBoolean("auto_add_new_releases", true)
    )
    val autoAddNewReleasesEnabled: StateFlow<Boolean> = _autoAddNewReleasesEnabled.asStateFlow()

    private val _autoSyncedCount = MutableStateFlow<Int>(0)
    val autoSyncedCount: StateFlow<Int> = _autoSyncedCount.asStateFlow()

    // Custom Filmweb rating range (default 6.5 - 10.0)
    private val _minFilmwebRating = MutableStateFlow<Double>(
        round(prefs.getFloat("min_filmweb_rating", 6.5f).toDouble() * 10.0) / 10.0
    )
    val minFilmwebRating: StateFlow<Double> = _minFilmwebRating.asStateFlow()

    private val _maxFilmwebRating = MutableStateFlow<Double>(
        round(prefs.getFloat("max_filmweb_rating", 10.0f).toDouble() * 10.0) / 10.0
    )
    val maxFilmwebRating: StateFlow<Double> = _maxFilmwebRating.asStateFlow()

    // Language of movie titles: true = Polish titles pulled from Polish streaming platforms (default),
    // false = Native/Original titles
    private val _usePolishTitles = MutableStateFlow<Boolean>(
        prefs.getBoolean("use_polish_streaming_titles", true)
    )
    val usePolishTitles: StateFlow<Boolean> = _usePolishTitles.asStateFlow()

    private fun loadCatalogWithCachedPosters(): List<Movie> {
        return MovieCatalog.sampleMovies
            .filter { it.hasPolishDubbing }
            .map { movie ->
                val cachedPlTitle = prefs.getString("pl_title_${movie.id}", null)
                val resolvedPlTitle = if (!cachedPlTitle.isNullOrBlank()) {
                    cachedPlTitle
                } else {
                    PosterNetworkService.localizeTitleToPolish(
                        rawTitle = movie.originalTitle,
                        isSeries = movie.isSeries,
                        catalogPolishFallback = movie.polishTitle
                    )
                }
                val cachedPoster = prefs.getString("poster_url_${movie.id}", null)
                movie.copy(
                    title = resolvedPlTitle,
                    polishTitle = resolvedPlTitle,
                    posterUrl = if (movie.posterUrl.isNotBlank()) movie.posterUrl else (cachedPoster ?: ""),
                    backdropUrl = if (movie.backdropUrl.isNotBlank()) movie.backdropUrl else (cachedPoster ?: "")
                )
            }
    }

    /**
     * Pulls official Polish streaming titles from Polish VOD metadata for catalog items.
     */
    suspend fun syncPolishStreamingTitles() = coroutineScope {
        val currentList = _movies.value.filter { it.hasPolishDubbing }
        var anyUpdated = false
        val updatedList = currentList.map { movie ->
            async {
                val resolvedPl = PosterNetworkService.fetchPolishStreamingTitle(
                    originalTitle = movie.originalTitle,
                    isSeries = movie.isSeries
                )
                val finalPl = PosterNetworkService.localizeTitleToPolish(
                    rawTitle = movie.originalTitle,
                    isSeries = movie.isSeries,
                    catalogPolishFallback = movie.polishTitle.ifBlank { resolvedPl }
                )
                if (finalPl.isNotBlank() && finalPl != movie.polishTitle) {
                    prefs.edit().putString("pl_title_${movie.id}", finalPl).apply()
                    anyUpdated = true
                    movie.copy(
                        title = finalPl,
                        polishTitle = finalPl
                    )
                } else {
                    movie
                }
            }
        }.awaitAll()

        if (anyUpdated) {
            _movies.value = updatedList.filter { it.hasPolishDubbing }
        }
    }

    /**
     * Resolves real official posters for any catalog items missing posterUrl
     * and caches them locally. Returns immediately if all catalog items already have posters.
     */
    suspend fun enrichCatalogPosters() = coroutineScope {
        val currentList = _movies.value.filter { it.hasPolishDubbing }
        if (currentList.all { it.posterUrl.isNotBlank() }) return@coroutineScope

        val updatedList = currentList.map { movie ->
            if (movie.posterUrl.isNotBlank()) {
                async { movie }
            } else {
                async {
                    val resolvedUrl = PosterNetworkService.fetchOfficialPosterUrl(
                        originalTitle = movie.originalTitle,
                        isSeries = movie.isSeries
                    )
                    if (!resolvedUrl.isNullOrBlank()) {
                        prefs.edit().putString("poster_url_${movie.id}", resolvedUrl).apply()
                        movie.copy(
                            posterUrl = resolvedUrl,
                            backdropUrl = movie.backdropUrl.ifBlank { resolvedUrl }
                        )
                    } else {
                        movie
                    }
                }
            }
        }.awaitAll()

        _movies.value = updatedList.filter { it.hasPolishDubbing }
    }

    /**
     * Automatically fetches new releases from Netflix, Prime Video, Max, Disney+, and SkyShowtime
     * that have Polish dubbing and adds them into the catalog.
     */
    suspend fun syncLatestPlatformReleases(sendNotification: Boolean = false): Int {
        val discovered = PosterNetworkService.fetchLatestPlatformReleases()
            .filter { it.hasPolishDubbing }
        if (discovered.isEmpty()) return 0

        val current = _movies.value.filter { it.hasPolishDubbing }.toMutableList()
        val existingIds = current.map { it.id }.toSet()
        val existingTitles = current.map { it.title.lowercase() }.toSet()

        var addedCount = 0
        var firstAdded: Movie? = null

        for (item in discovered) {
            if (!item.hasPolishDubbing) continue
            if (!existingIds.contains(item.id) && !existingTitles.contains(item.title.lowercase())) {
                current.add(0, item)
                addedCount++
                if (firstAdded == null) {
                    firstAdded = item
                }
            }
        }

        if (addedCount > 0) {
            _movies.value = current.filter { it.hasPolishDubbing }
            _autoSyncedCount.value = _autoSyncedCount.value + addedCount

            if (sendNotification && _notificationsEnabled.value && firstAdded != null) {
                NotificationHelper.sendPremiereNotification(
                    context = context,
                    title = firstAdded.displayTitle(_usePolishTitles.value),
                    message = "Automatycznie dodano $addedCount nowości z polskim dubbingiem do katalogu!",
                    platformName = firstAdded.platforms.joinToString(", ") { it.displayName },
                    filmwebRating = firstAdded.filmwebRating
                )
            }
        }
        return addedCount
    }

    /**
     * Searches online APIs (TVMaze & Apple iTunes) for additional real movies & series with Polish dubbing,
     * merging new results into the catalog so favorites/watched toggles also work on them.
     */
    suspend fun searchAndMergeOnline(query: String) {
        if (query.trim().length < 2) return
        val onlineResults = PosterNetworkService.searchLiveSeriesAndMovies(query)
            .filter { it.hasPolishDubbing }
        if (onlineResults.isEmpty()) return

        val existing = _movies.value.filter { it.hasPolishDubbing }.toMutableList()
        val existingIds = existing.map { it.id }.toSet()
        val existingTitles = existing.map { it.title.lowercase() }.toSet()

        var changed = false
        for (item in onlineResults) {
            if (!item.hasPolishDubbing) continue
            if (!existingIds.contains(item.id) && !existingTitles.contains(item.title.lowercase())) {
                existing.add(item)
                changed = true
            }
        }
        if (changed) {
            _movies.value = existing.filter { it.hasPolishDubbing }
        }
    }

    fun setFilmwebRatingRange(minRating: Double, maxRating: Double) {
        val safeMin = (round(minRating.coerceIn(0.0, 10.0) * 10.0) / 10.0)
        val safeMax = (round(maxRating.coerceIn(safeMin, 10.0) * 10.0) / 10.0)
        _minFilmwebRating.value = safeMin
        _maxFilmwebRating.value = safeMax
        prefs.edit()
            .putFloat("min_filmweb_rating", safeMin.toFloat())
            .putFloat("max_filmweb_rating", safeMax.toFloat())
            .apply()
    }

    fun setAutoAddNewReleasesEnabled(enabled: Boolean) {
        _autoAddNewReleasesEnabled.value = enabled
        prefs.edit().putBoolean("auto_add_new_releases", enabled).apply()
    }

    fun setUsePolishTitles(enabled: Boolean) {
        _usePolishTitles.value = enabled
        prefs.edit().putBoolean("use_polish_streaming_titles", enabled).apply()
    }

    fun toggleFavorite(movieId: String): Boolean {
        val current = _favoriteMovieIds.value.toMutableSet()
        val isNowFavorite = if (current.contains(movieId)) {
            current.remove(movieId)
            false
        } else {
            current.add(movieId)
            true
        }
        _favoriteMovieIds.value = current
        saveStringSet("fav_movie_ids", current)
        return isNowFavorite
    }

    fun toggleWatched(movieId: String): Boolean {
        val current = _watchedMovieIds.value.toMutableSet()
        val isNowWatched = if (current.contains(movieId)) {
            current.remove(movieId)
            false
        } else {
            current.add(movieId)
            true
        }
        _watchedMovieIds.value = current
        saveStringSet("watched_movie_ids", current)
        return isNowWatched
    }

    suspend fun toggleWatchlist(movie: Movie): Boolean {
        val exists = watchlistDao.isInWatchlist(movie.id)
        return if (exists) {
            watchlistDao.deleteByMovieId(movie.id)
            false
        } else {
            watchlistDao.insertWatchlistItem(
                WatchlistEntity(
                    movieId = movie.id,
                    title = movie.title,
                    originalTitle = movie.originalTitle,
                    polishTitle = movie.polishTitle,
                    year = movie.year,
                    isSeries = movie.isSeries,
                    genreName = movie.genre.name,
                    filmwebRating = movie.filmwebRating,
                    posterUrl = movie.posterUrl,
                    addedAt = System.currentTimeMillis()
                )
            )
            true
        }
    }

    suspend fun addToWatchlist(movie: Movie) {
        watchlistDao.insertWatchlistItem(
            WatchlistEntity(
                movieId = movie.id,
                title = movie.title,
                originalTitle = movie.originalTitle,
                polishTitle = movie.polishTitle,
                year = movie.year,
                isSeries = movie.isSeries,
                genreName = movie.genre.name,
                filmwebRating = movie.filmwebRating,
                posterUrl = movie.posterUrl,
                addedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeFromWatchlist(movieId: String) {
        watchlistDao.deleteByMovieId(movieId)
    }

    fun toggleReminder(movieId: String): Boolean {
        val current = _reminderMovieIds.value.toMutableSet()
        val isNowReminded = if (current.contains(movieId)) {
            current.remove(movieId)
            false
        } else {
            current.add(movieId)
            true
        }
        _reminderMovieIds.value = current
        saveStringSet("reminder_movie_ids", current)

        if (isNowReminded && _notificationsEnabled.value) {
            val movie = _movies.value.find { it.id == movieId }
            if (movie != null) {
                NotificationHelper.sendPremiereNotification(
                    context = context,
                    title = movie.title,
                    message = "Zapisano przypomnienie o premierze (${movie.premiereDate ?: "Wkrótce"})!",
                    platformName = movie.platforms.joinToString(", ") { it.displayName },
                    filmwebRating = movie.filmwebRating
                )
            }
        }
        return isNowReminded
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    fun togglePlatformNotification(platform: StreamingPlatform) {
        val current = _enabledNotificationPlatforms.value.toMutableSet()
        if (current.contains(platform)) {
            if (current.size > 1) {
                current.remove(platform)
            }
        } else {
            current.add(platform)
        }
        _enabledNotificationPlatforms.value = current
        prefs.edit().putStringSet("notif_platforms", current.map { it.name }.toSet()).apply()
    }

    fun sendTestPremiereNotification() {
        val sampleMovie = _movies.value.firstOrNull { it.isPremiere } ?: _movies.value.first()
        NotificationHelper.sendPremiereNotification(
            context = context,
            title = sampleMovie.title,
            message = "Hit filmowy debiutuje w streamingu! Sprawdź szczegóły w KinoStream.",
            platformName = sampleMovie.platforms.first().displayName,
            filmwebRating = sampleMovie.filmwebRating
        )
    }

    private fun loadStringSet(key: String): Set<String> {
        return prefs.getStringSet(key, emptySet()) ?: emptySet()
    }

    private fun saveStringSet(key: String, set: Set<String>) {
        prefs.edit().putStringSet(key, set).apply()
    }

    private fun loadThemeMode(): ThemeMode {
        val saved = prefs.getString("theme_mode", ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        return try {
            ThemeMode.valueOf(saved)
        } catch (_: Exception) {
            ThemeMode.DARK
        }
    }

    private fun loadNotificationPlatforms(): Set<StreamingPlatform> {
        val saved = prefs.getStringSet("notif_platforms", null)
        return if (saved == null) {
            StreamingPlatform.values().toSet()
        } else {
            saved.mapNotNull {
                try {
                    StreamingPlatform.valueOf(it)
                } catch (_: Exception) {
                    null
                }
            }.toSet().ifEmpty { StreamingPlatform.values().toSet() }
        }
    }
}
