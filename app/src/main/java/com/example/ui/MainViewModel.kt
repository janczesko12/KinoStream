package com.example.ui

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MovieRepository
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
    FAVORITES("Ulubione ❤️"),
    WATCHED("Obejrzane ✔️")
}

@Immutable
data class UiStats(
    val favoriteCount: Int = 0,
    val watchedCount: Int = 0,
    val averageWatchedRating: Double = 0.0,
    val totalWatchedMinutes: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MovieRepository(application.applicationContext)

    val allMovies: StateFlow<List<Movie>> = repository.movies
    val favoriteIds: StateFlow<Set<String>> = repository.favoriteMovieIds
    val watchedIds: StateFlow<Set<String>> = repository.watchedMovieIds
    val reminderIds: StateFlow<Set<String>> = repository.reminderMovieIds
    val themeMode: StateFlow<ThemeMode> = repository.themeMode
    val notificationsEnabled: StateFlow<Boolean> = repository.notificationsEnabled
    val enabledNotificationPlatforms: StateFlow<Set<StreamingPlatform>> = repository.enabledNotificationPlatforms

    // Custom Filmweb rating range (configurable in Settings)
    val minFilmwebRating: StateFlow<Double> = repository.minFilmwebRating
    val maxFilmwebRating: StateFlow<Double> = repository.maxFilmwebRating

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
        val maxRating: Double
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

    private val ratingRangePair = combine(
        minFilmwebRating,
        maxFilmwebRating
    ) { minR, maxR ->
        minR to maxR
    }

    private val filterParams = combine(
        baseFilterParams,
        ratingRangePair
    ) { base, (minR, maxR) ->
        base.copy(minRating = minR, maxRating = maxR)
    }

    val filteredMovies: StateFlow<List<Movie>> = combine(
        allMovies,
        filterParams
    ) { movies, params ->
        movies.filter { movie ->
            val matchesQuery = params.query.isBlank() ||
                movie.title.contains(params.query, ignoreCase = true) ||
                movie.originalTitle.contains(params.query, ignoreCase = true) ||
                movie.director.contains(params.query, ignoreCase = true) ||
                movie.cast.any { it.contains(params.query, ignoreCase = true) }

            val matchesPlatform = params.platform == null || movie.platforms.contains(params.platform)
            val matchesGenre = params.genre == MovieGenre.WSZYSTKIE || movie.genre == params.genre
            val matchesRating = !params.highRatingOnly ||
                (movie.filmwebRating in params.minRating..params.maxRating)

            matchesQuery && matchesPlatform && matchesGenre && matchesRating
        }.let { list ->
            when (params.sort) {
                MovieSortOption.FILMWEB_RATING_DESC -> list.sortedByDescending { it.filmwebRating }
                MovieSortOption.FILMWEB_RATING_ASC -> list.sortedBy { it.filmwebRating }
                MovieSortOption.YEAR_DESC -> list.sortedByDescending { it.year }
                MovieSortOption.TITLE_ASC -> list.sortedBy { it.title }
                MovieSortOption.GENRE_ASC -> list.sortedBy { it.genre.displayName }
            }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.movies.value)

    val premiereMovies: StateFlow<List<Movie>> = combine(
        allMovies,
        selectedPlatform
    ) { movies, platform ->
        movies.filter { it.isPremiere || it.isUpcoming || it.isAutoSynced }
            .filter { platform == null || it.platforms.contains(platform) }
            .sortedByDescending { it.year }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedMovies: StateFlow<List<Movie>> = combine(
        allMovies,
        favoriteIds,
        watchedIds,
        myListFilter
    ) { movies, favs, watched, filter ->
        movies.filter { movie ->
            when (filter) {
                MyListFilter.ALL -> favs.contains(movie.id) || watched.contains(movie.id)
                MyListFilter.FAVORITES -> favs.contains(movie.id)
                MyListFilter.WATCHED -> watched.contains(movie.id)
            }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<UiStats> = combine(
        allMovies,
        favoriteIds,
        watchedIds
    ) { movies, favs, watched ->
        val watchedMovies = movies.filter { watched.contains(it.id) }
        val avgRating = if (watchedMovies.isNotEmpty()) {
            watchedMovies.map { it.filmwebRating }.average()
        } else 0.0
        val totalMinutes = watchedMovies.sumOf { it.durationMinutes }

        UiStats(
            favoriteCount = favs.size,
            watchedCount = watched.size,
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
        _selectedMovie.value = movie
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
                "Katalog premier z Netflix, Prime Video, Max i Disney+ jest aktualny ✔️"
            }
        }
    }

    fun toggleFavorite(movie: Movie) {
        val isFav = repository.toggleFavorite(movie.id)
        _snackbarMessage.value = if (isFav) {
            "Dodano „${movie.title}” do ulubionych ❤️"
        } else {
            "Usunięto „${movie.title}” z ulubionych"
        }
    }

    fun toggleWatched(movie: Movie) {
        val isWatched = repository.toggleWatched(movie.id)
        _snackbarMessage.value = if (isWatched) {
            "Oznaczono „${movie.title}” jako obejrzany ✔️"
        } else {
            "Odznaczono film „${movie.title}”"
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
