package com.example.ui

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MovieRepository
import com.example.model.ContentTypeFilter
import com.example.model.Movie
import com.example.model.MovieGenre
import com.example.model.MovieSortOption
import com.example.model.StreamingPlatform
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

enum class MainTab(val title: String) {
    DISCOVER("Katalog"),
    PREMIERES("Premiery"),
    MY_LIST("Moja Lista"),
    SETTINGS("Ustawienia")
}

enum class MyListFilter(val label: String) {
    ALL("Wszystkie"),
    WATCHLIST("Do obejrzenia 🔖"),
    FAVORITES("Ulubione ❤️"),
    WATCHED("Obejrzane ✔️")
}

@Immutable
data class UiStats(
    val favoriteCount: Int = 0,
    val watchedCount: Int = 0,
    val watchlistCount: Int = 0,
    val averageWatchedRating: Double = 0.0,
    val totalWatchedMinutes: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MovieRepository(application.applicationContext)

    val allMovies: StateFlow<List<Movie>> = repository.movies
    val favoriteIds: StateFlow<Set<String>> = repository.favoriteMovieIds
    val watchedIds: StateFlow<Set<String>> = repository.watchedMovieIds
    val watchlistIds: StateFlow<Set<String>> = repository.watchlistMovieIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val reminderIds: StateFlow<Set<String>> = repository.reminderMovieIds
    val themeMode: StateFlow<ThemeMode> = repository.themeMode
    val notificationsEnabled: StateFlow<Boolean> = repository.notificationsEnabled
    val enabledNotificationPlatforms: StateFlow<Set<StreamingPlatform>> = repository.enabledNotificationPlatforms

    // Custom Filmweb rating range (configurable in Settings)
    val minFilmwebRating: StateFlow<Double> = repository.minFilmwebRating
    val maxFilmwebRating: StateFlow<Double> = repository.maxFilmwebRating

    // Language of movie titles: false = English/Original (default), true = Polish
    val usePolishTitles: StateFlow<Boolean> = repository.usePolishTitles

    // Automatic adding of new releases from Netflix, Prime Video, Max, Disney+
    val autoAddNewReleasesEnabled: StateFlow<Boolean> = repository.autoAddNewReleasesEnabled
    val autoSyncedCount: StateFlow<Int> = repository.autoSyncedCount

    private val _isSyncingNewReleases = MutableStateFlow(false)
    val isSyncingNewReleases: StateFlow<Boolean> = _isSyncingNewReleases.asStateFlow()

    private val _currentTab = MutableStateFlow(MainTab.DISCOVER)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPlatform = MutableStateFlow<StreamingPlatform?>(null)
    val selectedPlatform: StateFlow<StreamingPlatform?> = _selectedPlatform.asStateFlow()

    private val _contentTypeFilter = MutableStateFlow(ContentTypeFilter.ALL)
    val contentTypeFilter: StateFlow<ContentTypeFilter> = _contentTypeFilter.asStateFlow()

    private val _selectedGenre = MutableStateFlow(MovieGenre.WSZYSTKIE)
    val selectedGenre: StateFlow<MovieGenre> = _selectedGenre.asStateFlow()

    private val _onlyHighRating = MutableStateFlow(false)
    val onlyHighRating: StateFlow<Boolean> = _onlyHighRating.asStateFlow()

    private val _sortOption = MutableStateFlow(MovieSortOption.FILMWEB_RATING_DESC)
    val sortOption: StateFlow<MovieSortOption> = _sortOption.asStateFlow()

    private val _myListFilter = MutableStateFlow(MyListFilter.ALL)
    val myListFilter: StateFlow<MyListFilter> = _myListFilter.asStateFlow()

    private val _selectedMovie = MutableStateFlow<Movie?>(null)
    val selectedMovie: StateFlow<Movie?> = _selectedMovie.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var onlineSearchJob: Job? = null

    init {
        viewModelScope.launch {
            repository.syncPolishStreamingTitles()
            repository.enrichCatalogPosters()
        }
        viewModelScope.launch {
            if (repository.autoAddNewReleasesEnabled.value) {
                _isSyncingNewReleases.value = true
                repository.syncLatestPlatformReleases(sendNotification = false)
                _isSyncingNewReleases.value = false
            }
        }
    }

    private data class FilterParams(
        val query: String,
        val platform: StreamingPlatform?,
        val genre: MovieGenre,
        val highRatingOnly: Boolean,
        val sort: MovieSortOption,
        val minRating: Double,
        val maxRating: Double,
        val contentType: ContentTypeFilter = ContentTypeFilter.ALL
    )

    private val baseFilterParams = combine(
        searchQuery,
        selectedPlatform,
        selectedGenre,
        onlyHighRating,
        sortOption
    ) { query, platform, genre, highRatingOnly, sort ->
        FilterParams(query, platform, genre, highRatingOnly, sort, 6.5, 10.0)
    }

    private val ratingAndTypePair = combine(
        minFilmwebRating,
        maxFilmwebRating,
        contentTypeFilter
    ) { minR, maxR, cType ->
        Triple(minR, maxR, cType)
    }

    private val filterParams = combine(
        baseFilterParams,
        ratingAndTypePair
    ) { base, (minR, maxR, cType) ->
        base.copy(minRating = minR, maxRating = maxR, contentType = cType)
    }

    val filteredMovies: StateFlow<List<Movie>> = combine(
        allMovies,
        filterParams,
        usePolishTitles
    ) { movies, params, usePolish ->
        movies.filter { movie ->
            if (!movie.hasPolishDubbing) return@filter false

            val matchesType = when (params.contentType) {
                ContentTypeFilter.ALL -> true
                ContentTypeFilter.MOVIES -> !movie.isSeries
                ContentTypeFilter.SERIES -> movie.isSeries
            }

            val matchesQuery = params.query.isBlank() ||
                movie.title.contains(params.query, ignoreCase = true) ||
                movie.originalTitle.contains(params.query, ignoreCase = true) ||
                movie.polishTitle.contains(params.query, ignoreCase = true) ||
                movie.director.contains(params.query, ignoreCase = true) ||
                movie.cast.any { it.contains(params.query, ignoreCase = true) }

            val matchesPlatform = params.platform == null || movie.platforms.contains(params.platform)
            val matchesGenre = params.genre == MovieGenre.WSZYSTKIE || movie.genre == params.genre
            val matchesRating = !params.highRatingOnly ||
                (movie.filmwebRating in params.minRating..params.maxRating)

            matchesType && matchesQuery && matchesPlatform && matchesGenre && matchesRating
        }.map { it.withLanguage(usePolish) }
            .let { list ->
                when (params.sort) {
                    MovieSortOption.FILMWEB_RATING_DESC -> list.sortedByDescending { it.filmwebRating }
                    MovieSortOption.FILMWEB_RATING_ASC -> list.sortedBy { it.filmwebRating }
                    MovieSortOption.YEAR_DESC -> list.sortedByDescending { it.year }
                    MovieSortOption.TITLE_ASC -> list.sortedBy { it.title }
                    MovieSortOption.GENRE_ASC -> list.sortedBy { it.genre.displayName }
                }
            }
    }.flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            repository.movies.value.map { it.withLanguage(repository.usePolishTitles.value) }
        )

    val premiereMovies: StateFlow<List<Movie>> = combine(
        allMovies,
        selectedPlatform,
        usePolishTitles
    ) { movies, platform, usePolish ->
        movies.filter { it.hasPolishDubbing && (it.isPremiere || it.isUpcoming || it.isAutoSynced) }
            .filter { platform == null || it.platforms.contains(platform) }
            .map { it.withLanguage(usePolish) }
            .sortedByDescending { it.year }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val userSavedIdSets = combine(
        favoriteIds,
        watchedIds,
        watchlistIds
    ) { favs, watched, watchlist ->
        Triple(favs, watched, watchlist)
    }

    val savedMovies: StateFlow<List<Movie>> = combine(
        allMovies,
        userSavedIdSets,
        myListFilter,
        usePolishTitles
    ) { movies, (favs, watched, watchlist), filter, usePolish ->
        movies.filter { movie ->
            movie.hasPolishDubbing && when (filter) {
                MyListFilter.ALL -> favs.contains(movie.id) || watched.contains(movie.id) || watchlist.contains(movie.id)
                MyListFilter.WATCHLIST -> watchlist.contains(movie.id)
                MyListFilter.FAVORITES -> favs.contains(movie.id)
                MyListFilter.WATCHED -> watched.contains(movie.id)
            }
        }.map { it.withLanguage(usePolish) }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<UiStats> = combine(
        allMovies,
        favoriteIds,
        watchedIds,
        watchlistIds
    ) { movies, favs, watched, watchlist ->
        val watchedMovies = movies.filter { watched.contains(it.id) }
        val avgRating = if (watchedMovies.isNotEmpty()) {
            watchedMovies.map { it.filmwebRating }.average()
        } else 0.0
        val totalMinutes = watchedMovies.sumOf { it.durationMinutes }

        UiStats(
            favoriteCount = favs.size,
            watchedCount = watched.size,
            watchlistCount = watchlist.size,
            averageWatchedRating = avgRating,
            totalWatchedMinutes = totalMinutes
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiStats())

    fun setTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        onlineSearchJob?.cancel()
        if (query.trim().length >= 2) {
            onlineSearchJob = viewModelScope.launch {
                delay(350)
                repository.searchAndMergeOnline(query)
            }
        }
    }

    fun setPlatform(platform: StreamingPlatform?) {
        _selectedPlatform.value = if (_selectedPlatform.value == platform) null else platform
    }

    fun setContentTypeFilter(filter: ContentTypeFilter) {
        _contentTypeFilter.value = filter
    }

    fun setGenre(genre: MovieGenre) {
        _selectedGenre.value = genre
    }

    fun toggleHighRatingOnly() {
        _onlyHighRating.value = !_onlyHighRating.value
    }

    fun setSortOption(option: MovieSortOption) {
        _sortOption.value = option
    }

    fun setMyListFilter(filter: MyListFilter) {
        _myListFilter.value = filter
    }

    fun selectMovie(movie: Movie?) {
        _selectedMovie.value = movie?.withLanguage(usePolishTitles.value)
    }

    fun setUsePolishTitles(enabled: Boolean) {
        repository.setUsePolishTitles(enabled)
        _selectedMovie.value = _selectedMovie.value?.withLanguage(enabled)
        _snackbarMessage.value = if (enabled) {
            "Włączono polskie nazwy filmów i seriali 🇵🇱"
        } else {
            "Przywrócono angielskie (oryginalne) nazwy filmów i seriali 🇬🇧"
        }
    }

    fun setFilmwebRatingRange(minRating: Double, maxRating: Double) {
        repository.setFilmwebRatingRange(minRating, maxRating)
        val formattedMin = String.format(Locale.US, "%.1f", repository.minFilmwebRating.value)
        val formattedMax = String.format(Locale.US, "%.1f", repository.maxFilmwebRating.value)
        _snackbarMessage.value = "Zapisano przedział ocen Filmweb: $formattedMin – $formattedMax ⭐"
    }

    fun setAutoAddNewReleasesEnabled(enabled: Boolean) {
        repository.setAutoAddNewReleasesEnabled(enabled)
        if (enabled) {
            syncNewReleasesNow()
        } else {
            _snackbarMessage.value = "Wyłączono automatyczne dodawanie premier"
        }
    }

    fun syncNewReleasesNow() {
        viewModelScope.launch {
            _isSyncingNewReleases.value = true
            val count = repository.syncLatestPlatformReleases(sendNotification = true)
            _isSyncingNewReleases.value = false
            _snackbarMessage.value = if (count > 0) {
                "Automatycznie pobrano i dodano $count nowych premier VOD! 🎬"
            } else {
                "Katalog premier z Netflix, Prime Video, Max, Disney+ i SkyShowtime jest aktualny ✔️"
            }
        }
    }

    fun toggleFavorite(movie: Movie) {
        val isFav = repository.toggleFavorite(movie.id)
        val displayTitle = movie.displayTitle(usePolishTitles.value)
        _snackbarMessage.value = if (isFav) {
            "Dodano „$displayTitle” do ulubionych ❤️"
        } else {
            "Usunięto „$displayTitle” z ulubionych"
        }
    }

    fun toggleWatched(movie: Movie) {
        val isWatched = repository.toggleWatched(movie.id)
        val displayTitle = movie.displayTitle(usePolishTitles.value)
        _snackbarMessage.value = if (isWatched) {
            "Oznaczono „$displayTitle” jako obejrzany ✔️"
        } else {
            "Odznaczono film „$displayTitle”"
        }
    }

    fun toggleWatchlist(movie: Movie) {
        viewModelScope.launch {
            val isInWatchlist = repository.toggleWatchlist(movie)
            val displayTitle = movie.displayTitle(usePolishTitles.value)
            _snackbarMessage.value = if (isInWatchlist) {
                "Zapisano „$displayTitle” na liście Do obejrzenia 🔖"
            } else {
                "Usunięto „$displayTitle” z listy Do obejrzenia"
            }
        }
    }

    fun toggleReminder(movie: Movie) {
        val isReminded = repository.toggleReminder(movie.id)
        _snackbarMessage.value = if (isReminded) {
            "Włączono przypomnienie o premierze 🔔"
        } else {
            "Wyłączono przypomnienie"
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        repository.setThemeMode(mode)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        repository.setNotificationsEnabled(enabled)
    }

    fun toggleNotificationPlatform(platform: StreamingPlatform) {
        repository.togglePlatformNotification(platform)
    }

    fun sendTestNotification() {
        repository.sendTestPremiereNotification()
        _snackbarMessage.value = "Wysłano testowe powiadomienie o premierze!"
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
