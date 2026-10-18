package com.example.ui

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MovieRepository
import com.example.util.CrashReporting
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.example.model.ContentTypeFilter
import com.example.model.Movie
import com.example.model.MovieGenre
import com.example.model.MovieRatingSource
import com.example.model.MovieSortOption
import com.example.model.StreamingPlatform
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
import java.text.SimpleDateFormat

enum class MainTab(val title: String) {
    DISCOVER("Katalog"),
    PREMIERES("Premiery"),
    MY_LIST("Moja Lista"),
    SETTINGS("Ustawienia"),
    ADMIN("Admin")
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

@Immutable
data class ProblemReport(
    val id: String,
    val description: String,
    val context: String,
    val appVersion: String,
    val device: String,
    val androidVersion: String,
    val status: String,
    val createdAt: String
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

    private val adminAllowedEmail = "janczesko12@gmail.com"
    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated.asStateFlow()
    private val _adminUserEmail = MutableStateFlow<String?>(null)
    val adminUserEmail: StateFlow<String?> = _adminUserEmail.asStateFlow()
    private val _problemReports = MutableStateFlow<List<ProblemReport>>(emptyList())
    val problemReports: StateFlow<List<ProblemReport>> = _problemReports.asStateFlow()
    private val _adminMessage = MutableStateFlow<String?>(null)
    val adminMessage: StateFlow<String?> = _adminMessage.asStateFlow()
    private val _isAdminLoading = MutableStateFlow(false)
    val isAdminLoading: StateFlow<Boolean> = _isAdminLoading.asStateFlow()
    private var problemReportsRegistration: ListenerRegistration? = null

    private var onlineSearchJob: Job? = null

    init {
        restoreAdminSession()
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
        movies.filter {
            it.isAvailabilityVerified && it.platforms.isNotEmpty() &&
                (it.isPremiere || it.isUpcoming || it.isAutoSynced)
        }.filter { platform == null || it.platforms.contains(platform) }
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
                "Automatycznie dodano $count premier ze zweryfikowaną dostępnością VOD. 🎬"
            } else {
                "Nie znaleziono premier z potwierdzoną dostępnością w polskim VOD. Nie dodano niezweryfikowanych pozycji."
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
        val payload = hashMapOf(
            "description" to trimmed,
            "context" to contextInfo.take(500),
            "createdAt" to FieldValue.serverTimestamp(),
            "status" to "new",
            "app" to "KinoStream",
            "appVersion" to com.example.BuildConfig.VERSION_NAME,
            "device" to (android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL).take(200),
            "androidVersion" to (android.os.Build.VERSION.RELEASE ?: "unknown")
        )
        val firestore = runCatching { FirebaseFirestore.getInstance() }.getOrElse { error ->
            CrashReporting.recordNonFatal(getApplication(), error)
            _snackbarMessage.value = "Firebase nie jest skonfigurowany. Sprawdź google-services.json."
            return
        }
        _snackbarMessage.value = "Wysyłanie zgłoszenia…"
        firestore.collection("problem_reports")
            .add(payload)
            .addOnSuccessListener {
                _problemReportSuccessVersion.value += 1
                _snackbarMessage.value = "Zgłoszenie wysłane. Dziękujemy!"
            }
            .addOnFailureListener { error ->
                CrashReporting.recordNonFatal(getApplication(), error)
                _snackbarMessage.value =
                    "Nie udało się wysłać zgłoszenia. Sprawdź konfigurację Firebase i reguły Firestore."
            }
    }

    private fun restoreAdminSession() {
        val user = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
        if (user?.email?.equals(adminAllowedEmail, ignoreCase = true) == true && user.isEmailVerified) {
            _isAdminAuthenticated.value = true
            _adminUserEmail.value = user.email
            listenForProblemReports()
        }
    }

    fun signInAdmin(email: String, password: String) {
        val normalizedEmail = email.trim()
        if (!normalizedEmail.equals(adminAllowedEmail, ignoreCase = true)) {
            _adminMessage.value = "Ten adres nie ma dostępu do panelu administratora."
            return
        }
        if (password.isBlank()) {
            _adminMessage.value = "Wpisz hasło administratora."
            return
        }

        _isAdminLoading.value = true
        _adminMessage.value = null
        val auth = runCatching { FirebaseAuth.getInstance() }.getOrElse { error ->
            _isAdminLoading.value = false
            _adminMessage.value = "Brak konfiguracji Firebase. Dodaj google-services.json."
            CrashReporting.recordNonFatal(getApplication(), error)
            return
        }
        auth.signInWithEmailAndPassword(normalizedEmail, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user?.email?.equals(adminAllowedEmail, ignoreCase = true) != true) {
                    auth.signOut()
                    _isAdminAuthenticated.value = false
                    _adminMessage.value = "To konto nie jest kontem administratora."
                } else if (!user.isEmailVerified) {
                    _isAdminAuthenticated.value = false
                    _adminMessage.value = "Wysyłanie linku potwierdzającego adres e-mail…"
                    user.sendEmailVerification()
                        .addOnSuccessListener {
                            auth.signOut()
                            _isAdminLoading.value = false
                            _adminMessage.value = "Wysłaliśmy link potwierdzający. Otwórz wiadomość e-mail, potwierdź adres i zaloguj się ponownie."
                        }
                        .addOnFailureListener { error ->
                            auth.signOut()
                            _isAdminLoading.value = false
                            _adminMessage.value = "Nie udało się wysłać wiadomości weryfikacyjnej. Sprawdź ustawienia szablonów e-mail w Firebase."
                            CrashReporting.recordNonFatal(getApplication(), error)
                        }
                    return@addOnSuccessListener
                } else {
                    _isAdminAuthenticated.value = true
                    _adminUserEmail.value = user.email
                    _adminMessage.value = null
                    listenForProblemReports()
                    setTab(MainTab.ADMIN)
                }
                _isAdminLoading.value = false
            }
            .addOnFailureListener { error ->
                _isAdminLoading.value = false
                _adminMessage.value = "Logowanie nieudane. Sprawdź hasło i włączenie logowania e-mail/hasło w Firebase."
                CrashReporting.recordNonFatal(getApplication(), error)
            }
    }

    fun signOutAdmin() {
        problemReportsRegistration?.remove()
        problemReportsRegistration = null
        runCatching { FirebaseAuth.getInstance().signOut() }
        _isAdminAuthenticated.value = false
        _adminUserEmail.value = null
        _problemReports.value = emptyList()
        _adminMessage.value = null
        _isAdminLoading.value = false
        setTab(MainTab.SETTINGS)
    }

    fun refreshProblemReports() {
        if (!_isAdminAuthenticated.value) return
        _adminMessage.value = null
        listenForProblemReports()
    }

    private fun listenForProblemReports() {
        if (!_isAdminAuthenticated.value) return
        problemReportsRegistration?.remove()
        val firestore = runCatching { FirebaseFirestore.getInstance() }.getOrElse { error ->
            _adminMessage.value = "Nie można połączyć z Firestore. Sprawdź konfigurację Firebase."
            CrashReporting.recordNonFatal(getApplication(), error)
            return
        }
        problemReportsRegistration = firestore.collection("problem_reports")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _adminMessage.value = "Brak dostępu do zgłoszeń. Sprawdź reguły Firestore i konto administratora."
                    CrashReporting.recordNonFatal(getApplication(), error)
                    return@addSnapshotListener
                }
                _problemReports.value = snapshot?.documents.orEmpty().map { document ->
                    val createdAt = document.getTimestamp("createdAt")?.toDate()?.let { date ->
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date)
                    } ?: "Przed chwilą"
                    ProblemReport(
                        id = document.id,
                        description = document.getString("description").orEmpty(),
                        context = document.getString("context").orEmpty(),
                        appVersion = document.getString("appVersion").orEmpty(),
                        device = document.getString("device").orEmpty(),
                        androidVersion = document.getString("androidVersion").orEmpty(),
                        status = document.getString("status") ?: "new",
                        createdAt = createdAt
                    )
                }
            }
    }

    fun resolveProblemReport(reportId: String) {
        if (!_isAdminAuthenticated.value) {
            _adminMessage.value = "Zaloguj się jako administrator."
            return
        }
        FirebaseFirestore.getInstance()
            .collection("problem_reports")
            .document(reportId)
            .update(
                mapOf(
                    "status" to "resolved",
                    "resolvedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener { _adminMessage.value = "Zgłoszenie oznaczono jako rozwiązane." }
            .addOnFailureListener { error ->
                _adminMessage.value = "Nie udało się zmienić statusu zgłoszenia."
                CrashReporting.recordNonFatal(getApplication(), error)
            }
    }

    override fun onCleared() {
        problemReportsRegistration?.remove()
        problemReportsRegistration = null
        super.onCleared()
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
