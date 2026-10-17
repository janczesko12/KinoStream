package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.KinoStreamDatabase
import com.example.data.local.WatchlistDao
import com.example.data.local.WatchlistEntity
import com.example.model.Movie
import com.example.model.MovieGenre
import com.example.model.MovieRatingSource
import com.example.model.StreamingPlatform
import org.json.JSONArray
import org.json.JSONObject
import com.example.ui.theme.ThemeMode
import com.example.util.NotificationHelper
import com.example.util.PremiereReminderScheduler
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
        // Never trust hard-coded platform labels. On a fresh install the verified online catalogue
        // is populated asynchronously by discoverMoreMovies(); keep the initial list empty rather
        // than showing incorrect VOD providers.
        val staticCatalog = MovieCatalog.sampleMovies
            .filter { it.hasPolishDubbing && it.platforms.isNotEmpty() }
            .map { movie ->
                val cachedPlTitle = prefs.getString("pl_title_${movie.id}", null)
                val resolvedPlTitle = cachedPlTitle?.takeIf { it.isNotBlank() }
                    ?: PosterNetworkService.localizeTitleToPolish(
                        rawTitle = movie.originalTitle,
                        isSeries = movie.isSeries,
                        catalogPolishFallback = movie.polishTitle
                    )
                val cachedPoster = prefs.getString("poster_url_${movie.id}", null)
                movie.copy(
                    title = resolvedPlTitle,
                    polishTitle = resolvedPlTitle,
                    posterUrl = if (movie.posterUrl.isNotBlank()) movie.posterUrl else (cachedPoster ?: ""),
                    backdropUrl = if (movie.backdropUrl.isNotBlank()) movie.backdropUrl else (cachedPoster ?: "")
                )
            }

        val cachedProviderCatalog = loadDiscoveredMovies()
            .filter { it.hasPolishDubbing && it.isAvailabilityVerified && it.platforms.isNotEmpty() &&
                (it.id.startsWith("tmdb-movie-") || it.id.startsWith("tmdb-tv-")) }

        // After a provider refresh, cached TMDB/JustWatch data is the source of truth.
        // Curated records are retained (including their Filmweb scores) only if a provider match exists.
        if (prefs.getLong("verified_polish_vod_v3_last_sync", 0L) > 0L && cachedProviderCatalog.isNotEmpty()) {
            val matchedProviderIds = mutableSetOf<String>()
            val verifiedStatic = staticCatalog.mapNotNull { local ->
                val providerMatch = cachedProviderCatalog.firstOrNull { sameProviderTitle(local, it) }
                    ?: return@mapNotNull null
                matchedProviderIds.add(providerMatch.id)
                local.copy(
                    platforms = providerMatch.platforms,
                    isAvailabilityVerified = true
                )
            }
            val newProviderItems = cachedProviderCatalog.filter { it.id !in matchedProviderIds }
            return (verifiedStatic + newProviderItems).distinctBy { it.id }
        }

        // Do not show speculative or outdated platform assignments while the provider API is
        // unavailable. Only previously verified TMDB/JustWatch entries are safe to display.
        return cachedProviderCatalog
    }

    private fun normalizedProviderTitle(movie: Movie): String =
        (movie.originalTitle.ifBlank { movie.title })
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]"), "")

    private fun sameProviderTitle(first: Movie, second: Movie): Boolean {
        if (first.isSeries != second.isSeries) return false
        val firstTitle = normalizedProviderTitle(first)
        val secondTitle = normalizedProviderTitle(second)
        if (firstTitle.isBlank() || firstTitle != secondTitle) return false
        return first.year == 0 || second.year == 0 || kotlin.math.abs(first.year - second.year) <= 1
    }

    /**
     * Pulls official Polish streaming titles from Polish VOD metadata for catalog items.
     */
    suspend fun syncPolishStreamingTitles() = coroutineScope {
        val currentList = _movies.value.filter { it.hasPolishDubbing && !isOnlineDiscovery(it) }
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
            val discoveries = _movies.value.filter(::isOnlineDiscovery)
            _movies.value = updatedList.filter { it.hasPolishDubbing } + discoveries
            persistDiscoveredMovies(discoveries)
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
            .filter { it.hasPolishDubbing && it.isAvailabilityVerified && it.platforms.isNotEmpty() }
        if (discovered.isEmpty()) return 0

        val current = _movies.value.filter { it.hasPolishDubbing }.toMutableList()
        val existingIds = current.mapTo(mutableSetOf()) { it.id }
        val existingTitles = current.mapTo(mutableSetOf()) { it.title.trim().lowercase() }

        var addedCount = 0
        var firstAdded: Movie? = null

        for (item in discovered) {
            if (!item.hasPolishDubbing) continue
            if (existingIds.add(item.id) && existingTitles.add(item.title.trim().lowercase())) {
                current.add(0, item)
                addedCount++
                if (firstAdded == null) {
                    firstAdded = item
                }
            }
        }

        if (addedCount > 0) {
            _movies.value = current.filter { it.hasPolishDubbing }
            persistDiscoveredMovies(_movies.value.filter(::isOnlineDiscovery))
            _autoSyncedCount.value = _autoSyncedCount.value + addedCount

            if (sendNotification && _notificationsEnabled.value && firstAdded != null &&
                firstAdded.isAvailabilityVerified &&
                firstAdded.platforms.any { it in _enabledNotificationPlatforms.value }
            ) {
                NotificationHelper.sendPremiereNotification(
                    context = context,
                    title = firstAdded.displayTitle(_usePolishTitles.value),
                    message = "Automatycznie dodano $addedCount nowości z polskim dubbingiem do katalogu!",
                    platformName = firstAdded.platforms.joinToString(", ") { it.displayName },
                    filmwebRating = firstAdded.filmwebRating.takeIf { firstAdded.hasRating } ?: 0.0
                )
            }
        }
        return addedCount
    }

    /**
     * Expands the initial catalogue from public TVMaze and Apple iTunes search indexes.
     * Runs at most once every 7 days, merges/deduplicates results and persists discoveries.
     * Search results are metadata only: platform availability and Filmweb ratings are not invented.
     */
    suspend fun discoverMoreMovies(force: Boolean = false): Int = coroutineScope {
        val now = System.currentTimeMillis()
        val lastSync = prefs.getLong("verified_polish_vod_v3_last_sync", 0L)
        val refreshInterval = 7L * 24L * 60L * 60L * 1000L
        if (!force && now - lastSync < refreshInterval) return@coroutineScope 0

        // Only this source returns provider-specific availability for Poland.
        val verifiedResults = runCatching {
            PosterNetworkService.fetchPolishStreamingCatalog(maxPagesPerProvider = 4)
        }.getOrDefault(emptyList())
            .filter { it.isAvailabilityVerified && it.platforms.isNotEmpty() }

        // An empty provider response must not leave a blank screen forever; keep cached verified
        // titles and do not advance the sync timestamp, so the next app launch can retry.
        if (verifiedResults.isEmpty()) {
            val cached = loadDiscoveredMovies()
                .filter { it.isAvailabilityVerified && it.platforms.isNotEmpty() &&
                    (it.id.startsWith("tmdb-movie-") || it.id.startsWith("tmdb-tv-")) }
            if (cached.isNotEmpty()) _movies.value = cached
            return@coroutineScope 0
        }

        // Start with the curated catalogue too, even on first launch when only cached
        // verified provider entries are allowed into the visible StateFlow.
        val existingStatic = MovieCatalog.sampleMovies
        val matchedProviderIds = mutableSetOf<String>()
        val verifiedStatic = existingStatic.mapNotNull { local ->
            val matching = verifiedResults.firstOrNull { sameProviderTitle(local, it) }
                ?: return@mapNotNull null
            matchedProviderIds.add(matching.id)
            val platformNames = matching.platforms.joinToString(", ") { it.displayName }
            local.copy(
                platforms = matching.platforms,
                isAvailabilityVerified = true,
                description = listOf(
                    local.description,
                    "Dostępność VOD w Polsce: $platformNames. Dane JustWatch udostępniane przez TMDB; wersja audio i napisy mogą się różnić."
                ).filter { it.isNotBlank() }.distinct().joinToString("\n\n")
            )
        }

        val newlyDiscovered = verifiedResults.filter { it.id !in matchedProviderIds }
        val merged = (verifiedStatic + newlyDiscovered)
            .filter { it.isAvailabilityVerified && it.platforms.isNotEmpty() }
            .distinctBy { it.id }

        if (merged.isNotEmpty()) {
            val beforeIds = _movies.value.mapTo(mutableSetOf()) { it.id }
            val addedCount = newlyDiscovered.count { it.id !in beforeIds }
            _movies.value = merged
            // Persist provider records (not speculative TVMaze/iTunes searches); the loader overlays
            // their current platform list onto curated records on subsequent launches.
            persistDiscoveredMovies(verifiedResults)
            prefs.edit().putLong("verified_polish_vod_v3_last_sync", now).apply()
            addedCount
        } else {
            0
        }
    }

    /**
     * Searches online APIs (TVMaze & Apple iTunes) for additional real movies & series with Polish dubbing,
     * merging new results into the catalog so favorites/watched toggles also work on them.
     */
    suspend fun searchAndMergeOnline(query: String) {
        if (query.trim().length < 2) return
        val onlineResults = PosterNetworkService.searchLiveSeriesAndMovies(query)
            .filter { it.hasPolishDubbing && it.isAvailabilityVerified && it.platforms.isNotEmpty() }
        if (onlineResults.isEmpty()) return

        val existing = _movies.value.filter { it.hasPolishDubbing }.toMutableList()
        val existingIds = existing.mapTo(mutableSetOf()) { it.id }
        val existingTitles = existing.mapTo(mutableSetOf()) { it.title.trim().lowercase() }

        var changed = false
        for (item in onlineResults) {
            if (!item.hasPolishDubbing) continue
            if (item.id !in existingIds && item.title.trim().lowercase() !in existingTitles) {
                existing.add(item)
                existingIds.add(item.id)
                existingTitles.add(item.title.trim().lowercase())
                changed = true
            }
        }
        if (changed) {
            _movies.value = existing.filter { it.hasPolishDubbing }
            persistDiscoveredMovies(_movies.value.filter(::isOnlineDiscovery))
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

    fun toggleReminder(movieId: String): ReminderToggleResult {
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

        val movie = _movies.value.find { it.id == movieId }
        if (!isNowReminded) {
            movie?.let { PremiereReminderScheduler.cancel(context, it.id) }
            return ReminderToggleResult(enabled = false, scheduled = false)
        }

        val scheduled = if (_notificationsEnabled.value && movie != null) {
            PremiereReminderScheduler.schedule(
                context = context,
                movie = movie,
                enabledPlatforms = _enabledNotificationPlatforms.value
            )
        } else false
        return ReminderToggleResult(enabled = true, scheduled = scheduled)
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        _reminderMovieIds.value.forEach { movieId ->
            val movie = _movies.value.firstOrNull { it.id == movieId } ?: return@forEach
            if (enabled) {
                val scheduled = PremiereReminderScheduler.schedule(context, movie, _enabledNotificationPlatforms.value)
                if (!scheduled) PremiereReminderScheduler.cancel(context, movie.id)
            } else {
                PremiereReminderScheduler.cancel(context, movie.id)
            }
        }
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
        _reminderMovieIds.value.forEach { movieId ->
            val movie = _movies.value.firstOrNull { it.id == movieId } ?: return@forEach
            if (_notificationsEnabled.value) {
                val scheduled = PremiereReminderScheduler.schedule(context, movie, current)
                if (!scheduled) PremiereReminderScheduler.cancel(context, movie.id)
            } else {
                PremiereReminderScheduler.cancel(context, movie.id)
            }
        }
    }

    fun sendTestPremiereNotification() {
        val sampleMovie = _movies.value.firstOrNull { it.isPremiere } ?: _movies.value.first()
        NotificationHelper.sendPremiereNotification(
            context = context,
            title = sampleMovie.title,
            message = "Hit filmowy debiutuje w streamingu! Sprawdź szczegóły w KinoStream.",
            platformName = sampleMovie.platforms.firstOrNull()?.displayName ?: "Niezweryfikowana platforma",
            filmwebRating = sampleMovie.filmwebRating.takeIf { sampleMovie.hasRating } ?: 0.0
        )
    }

    data class ReminderToggleResult(val enabled: Boolean, val scheduled: Boolean)

    private fun isOnlineDiscovery(movie: Movie): Boolean =
        movie.id.startsWith("tvmaze-") || movie.id.startsWith("itunes-") || movie.id.startsWith("auto-tvmaze-") || movie.id.startsWith("tmdb-movie-") || movie.id.startsWith("tmdb-tv-")

    private fun loadDiscoveredMovies(): List<Movie> {
        return try {
            val json = JSONArray(prefs.getString("discovered_movies_json", "[]"))
            (0 until json.length()).mapNotNull { index ->
                val item = json.optJSONObject(index) ?: return@mapNotNull null
                runCatching {
                    fun strings(key: String): List<String> {
                        val array = item.optJSONArray(key) ?: return emptyList()
                        return (0 until array.length()).map { array.optString(it) }
                    }
                    fun longs(key: String): List<Long> {
                        val array = item.optJSONArray(key) ?: return emptyList()
                        return (0 until array.length()).map { array.optLong(it) }
                    }
                    Movie(
                        id = item.getString("id"),
                        title = item.optString("title"),
                        originalTitle = item.optString("originalTitle"),
                        year = item.optInt("year", 0),
                        genre = runCatching { MovieGenre.valueOf(item.optString("genre", "DRAMAT")) }.getOrDefault(MovieGenre.DRAMAT),
                        filmwebRating = item.optDouble("rating", 0.0),
                        filmwebVotesCount = item.optInt("votes", 0),
                        platforms = strings("platforms").mapNotNull { value ->
                            runCatching { StreamingPlatform.valueOf(value) }.getOrNull()
                        },
                        durationMinutes = item.optInt("duration", 0),
                        director = item.optString("director", "Brak danych"),
                        cast = strings("cast"),
                        description = item.optString("description"),
                        posterGradientColors = longs("posterGradientColors"),
                        posterEmoji = item.optString("posterEmoji", "🎬"),
                        posterUrl = item.optString("posterUrl"),
                        backdropUrl = item.optString("backdropUrl"),
                        isSeries = item.optBoolean("isSeries", false),
                        premiereDate = item.opt("premiereDate")?.takeUnless { it == JSONObject.NULL }?.toString(),
                        isPremiere = item.optBoolean("isPremiere", false),
                        isUpcoming = item.optBoolean("isUpcoming", false),
                        isAutoSynced = item.optBoolean("isAutoSynced", false),
                        hasPolishDubbing = item.optBoolean("hasPolishDubbing", false),
                        ageRating = item.optString("ageRating", "16+"),
                        polishTitle = item.optString("polishTitle"),
                        profileLogoUrl = item.optString("profileLogoUrl"),
                        ratingSource = runCatching {
                            MovieRatingSource.valueOf(item.optString("ratingSource", "NONE"))
                        }.getOrDefault(MovieRatingSource.NONE),
                        isAvailabilityVerified = item.optBoolean("isAvailabilityVerified", false)
                    )
                }.getOrNull()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistDiscoveredMovies(movies: List<Movie>) {
        val output = JSONArray()
        movies.filter(::isOnlineDiscovery).distinctBy { it.id }.takeLast(2000).forEach { movie ->
            val platforms = JSONArray().apply { movie.platforms.forEach { put(it.name) } }
            val cast = JSONArray().apply { movie.cast.forEach { put(it) } }
            val colors = JSONArray().apply { movie.posterGradientColors.forEach { put(it) } }
            output.put(JSONObject().apply {
                put("id", movie.id)
                put("title", movie.title)
                put("originalTitle", movie.originalTitle)
                put("year", movie.year)
                put("genre", movie.genre.name)
                put("rating", movie.filmwebRating)
                put("ratingSource", movie.ratingSource.name)
                put("votes", movie.filmwebVotesCount)
                put("platforms", platforms)
                put("duration", movie.durationMinutes)
                put("director", movie.director)
                put("cast", cast)
                put("description", movie.description)
                put("posterGradientColors", colors)
                put("posterEmoji", movie.posterEmoji)
                put("posterUrl", movie.posterUrl)
                put("backdropUrl", movie.backdropUrl)
                put("isSeries", movie.isSeries)
                put("premiereDate", movie.premiereDate ?: JSONObject.NULL)
                put("isPremiere", movie.isPremiere)
                put("isUpcoming", movie.isUpcoming)
                put("isAutoSynced", movie.isAutoSynced)
                put("hasPolishDubbing", movie.hasPolishDubbing)
                put("isAvailabilityVerified", movie.isAvailabilityVerified)
                put("ageRating", movie.ageRating)
                put("polishTitle", movie.polishTitle)
                put("profileLogoUrl", movie.profileLogoUrl)
            })
        }
        prefs.edit().putString("discovered_movies_json", output.toString()).apply()
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
