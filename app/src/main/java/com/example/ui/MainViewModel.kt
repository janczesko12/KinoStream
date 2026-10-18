package com.example.ui

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MovieRepository
import com.example.util.CrashReporting
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.example.model.ContentTypeFilter
import com.example.model.Movie
import com.example.model.MovieGenre
import com.example.model.MovieRatingSource
import com.example.model.MovieSortOption
import com.example.model.StreamingPlatform
import com.example.model.isRecentReleaseDate
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

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
    private val reportEmailHttpClient = okhttp3.OkHttpClient.Builder()
        .callTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

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

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()
    private val _initialLoadingMessage = MutableStateFlow("Przygotowujemy Twój katalog filmów…")
    val initialLoadingMessage: StateFlow<String> = _initialLoadingMessage.asStateFlow()

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
    private val _problemReportSuccessVersion = MutableStateFlow(0)
    val problemReportSuccessVersion: StateFlow<Int> = _problemReportSuccessVersion.asStateFlow()

    private var onlineSearchJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                // Show a real loading screen while the startup data stages are running.
                kotlinx.coroutines.withTimeoutOrNull(30_000) {
                    kotlinx.coroutines.coroutineScope {
                        _initialLoadingMessage.value = "Pobieranie filmów i seriali…"
                        val catalogJob = async { runCatching { repository.discoverMoreMovies() } }
                        val ratingsJob = async { runCatching { repository.syncFilmwebRatings() } }
                        catalogJob.await()
                        ratingsJob.await()

                        _initialLoadingMessage.value = "Aktualizowanie opisów i plakatów…"
                        val titlesJob = async { runCatching { repository.syncPolishStreamingTitles() } }
                        val postersJob = async { runCatching { repository.enrichCatalogPosters() } }
                        titlesJob.await()
                        postersJob.await()

                        if (repository.autoAddNewReleasesEnabled.value) {
                            _initialLoadingMessage.value = "Sprawdzanie najnowszych premier…"
                            _isSyncingNewReleases.value = true
                            try {
                                runCatching { repository.syncLatestPlatformReleases(sendNotification = false) }
                            } finally {
                                _isSyncingNewReleases.value = false
                            }
                        }

                        // Save all verified Firebase/provider metadata on-device after a successful refresh.
                        repository.persistFullCatalogSnapshot()
                        _initialLoadingMessage.value = "Gotowe! Otwieramy katalog…"
                    }
                }
            } finally {
                _isInitialLoading.value = false
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
            if (!movie.isAvailabilityVerified || movie.platforms.isEmpty()) return@filter false

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

            val matchesPlatform = params.platform == null ||
                (movie.isAvailabilityVerified && movie.platforms.contains(params.platform))
            val matchesGenre = params.genre == MovieGenre.WSZYSTKIE || movie.genre == params.genre
            val matchesRating = !params.highRatingOnly ||
                (movie.ratingSource == MovieRatingSource.FILMWEB &&
                    movie.filmwebRating in params.minRating..params.maxRating)

            matchesType && matchesQuery && matchesPlatform && matchesGenre && matchesRating
        }.map { it.withLanguage(usePolish) }
            .let { list ->
                when (params.sort) {
                    MovieSortOption.FILMWEB_RATING_DESC -> list.sortedWith(
                        compareBy<Movie> { it.ratingSource != MovieRatingSource.FILMWEB }
                            .thenByDescending { movie -> movie.filmwebRating.takeIf { movie.ratingSource == MovieRatingSource.FILMWEB } ?: 0.0 }
                    )
                    MovieSortOption.FILMWEB_RATING_ASC -> list.sortedWith(
                        compareBy<Movie> { it.ratingSource != MovieRatingSource.FILMWEB }
                            .thenBy { movie -> movie.filmwebRating.takeIf { movie.ratingSource == MovieRatingSource.FILMWEB } ?: 0.0 }
                    )
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
        movies.filter { movie ->
            movie.isAvailabilityVerified &&
                movie.platforms.isNotEmpty() &&
                isRecentReleaseDate(movie.premiereDate)
        }.filter { platform == null || it.platforms.contains(platform) }
            .map { it.withLanguage(usePolish) }
            .sortedByDescending { it.premiereDate.orEmpty() }
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
            movie.isAvailabilityVerified && movie.platforms.isNotEmpty() && when (filter) {
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
        val ratedWatchedMovies = watchedMovies.filter {
            it.ratingSource == MovieRatingSource.FILMWEB && it.filmwebRating > 0.0
        }
        val avgRating = if (ratedWatchedMovies.isNotEmpty()) {
            ratedWatchedMovies.map { it.filmwebRating }.average()
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
                "Dodano $count nowych tytułów dostępnych w polskim VOD. 🎬"
            } else {
                "Odświeżono dane premier. Lista pokazuje tytuły z ostatnich 180 dni, których dostępność na platformach w Polsce została potwierdzona."
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
        val result = repository.toggleReminder(movie.id)
        _snackbarMessage.value = when {
            !result.enabled -> "Wyłączono przypomnienie"
            result.scheduled -> "Zaplanowano powiadomienie na potwierdzoną datę premiery 🔔"
            else -> "Zapisano przypomnienie, ale brak zweryfikowanej przyszłej daty lub włączonej platformy. Nie wysłano fałszywego powiadomienia."
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

    fun submitProblemReport(description: String, contextInfo: String = "") {
        val trimmed = description.trim()
        if (trimmed.isEmpty()) {
            _snackbarMessage.value = "Opisz problem przed wysłaniem zgłoszenia."
            return
        }
        if (trimmed.length > 3000) {
            _snackbarMessage.value = "Opis jest za długi. Maksymalnie 3000 znaków."
            return
        }
        val reportContext = contextInfo.take(500)
        val device = (android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL).take(200)
        val androidVersion = android.os.Build.VERSION.RELEASE ?: "unknown"
        val appVersion = com.example.BuildConfig.VERSION_NAME
        val payload = hashMapOf(
            "description" to trimmed,
            "context" to reportContext,
            "createdAt" to FieldValue.serverTimestamp(),
            "status" to "new",
            "app" to "KinoStream",
            "appVersion" to appVersion,
            "device" to device,
            "androidVersion" to androidVersion
        )
        val firestore = runCatching { FirebaseFirestore.getInstance() }.getOrElse { error ->
            CrashReporting.recordNonFatal(getApplication(), error)
            _snackbarMessage.value = "Firebase nie jest skonfigurowany. Sprawdź google-services.json."
            return
        }
        _snackbarMessage.value = "Zapisywanie zgłoszenia…"
        firestore.collection("problem_reports")
            .add(payload)
            .addOnSuccessListener { document ->
                _problemReportSuccessVersion.value += 1
                _snackbarMessage.value = "Zgłoszenie zapisane. Wysyłanie powiadomienia e-mail…"
                sendReportEmail(
                    reportId = document.id,
                    description = trimmed,
                    contextInfo = reportContext,
                    appVersion = appVersion,
                    device = device,
                    androidVersion = androidVersion
                )
            }
            .addOnFailureListener { error ->
                CrashReporting.recordNonFatal(getApplication(), error)
                _snackbarMessage.value =
                    "Nie udało się zapisać zgłoszenia. Sprawdź konfigurację Firebase i reguły Firestore."
            }
    }

    private fun sendReportEmail(
        reportId: String,
        description: String,
        contextInfo: String,
        appVersion: String,
        device: String,
        androidVersion: String
    ) {
        val endpoint = com.example.BuildConfig.REPORT_EMAIL_WORKER_URL.trim()
        if (endpoint.isBlank()) {
            _snackbarMessage.value =
                "Zgłoszenie zapisano, ale powiadomienia e-mail nie są jeszcze podłączone do tej wersji."
            return
        }
        if (!endpoint.startsWith("https://")) {
            _snackbarMessage.value =
                "Zgłoszenie zapisano, ale adres usługi e-mail nie jest poprawnym adresem HTTPS."
            return
        }

        val json = org.json.JSONObject()
            .put("reportId", reportId)
            .put("description", description)
            .put("context", contextInfo)
            .put("appVersion", appVersion)
            .put("device", device)
            .put("androidVersion", androidVersion)

        val request = Request.Builder()
            .url(endpoint)
            .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Accept", "application/json")
            .build()

        reportEmailHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                _snackbarMessage.value =
                    "Zgłoszenie zapisano, ale nie udało się wysłać e-maila. Zgłoszenie pozostaje zapisane w bazie."
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    _snackbarMessage.value = when {
                        it.isSuccessful ->
                            "Zgłoszenie zapisano. Powiadomienie e-mail przyjęto do wysyłki."
                        it.code == 429 ->
                            "Zgłoszenie zapisano, ale limit wysyłki e-mail został chwilowo osiągnięty."
                        it.code == 503 ->
                            "Zgłoszenie zapisano, ale usługa e-mail nie jest jeszcze skonfigurowana."
                        else ->
                            "Zgłoszenie zapisano, ale e-mail nie został wysłany. Sprawdź konfigurację Cloudflare i Resend."
                    }
                }
            }
        })
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
