package com.example.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.DisneyBlue
import com.example.ui.theme.MaxPurple
import com.example.ui.theme.NetflixRed
import com.example.ui.theme.PrimeVideoBlue
import com.example.ui.theme.SkyShowtimeTeal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class StreamingPlatform(
    val id: String,
    val displayName: String,
    val brandColor: Color,
    val badgeShort: String
) {
    NETFLIX("netflix", "Netflix", NetflixRed, "N"),
    PRIME_VIDEO("prime", "Prime Video", PrimeVideoBlue, "PRIME"),
    MAX("max", "Max", MaxPurple, "MAX"),
    DISNEY_PLUS("disney", "Disney+", DisneyBlue, "D+"),
    SKYSHOWTIME("skyshowtime", "SkyShowtime", SkyShowtimeTeal, "SKY")
}

/**
 * Returns false for blank or placeholder metadata that must not be presented as real data.
 */
fun String.isDisplayableMetadata(): Boolean {
    val normalized = trim()
    return normalized.isNotEmpty() &&
        !normalized.equals("Brak danych", ignoreCase = true) &&
        !normalized.equals("Unknown", ignoreCase = true) &&
        !normalized.equals("N/A", ignoreCase = true) &&
        !normalized.equals("null", ignoreCase = true) &&
        normalized != "-"
}

/**
 * Checks a real ISO-8601 title release date against a rolling window.
 * This is the film/series release date, not a claimed streaming-service launch date.
 */
fun isRecentReleaseDate(
    releaseDate: String?,
    today: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date()),
    lookbackDays: Int = 180
): Boolean {
    val rawReleaseDate = releaseDate ?: return false
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val parsedRelease = runCatching { formatter.parse(rawReleaseDate) }.getOrNull() ?: return false
    val parsedToday = runCatching { formatter.parse(today) }.getOrNull() ?: return false
    if (formatter.format(parsedRelease) != rawReleaseDate || formatter.format(parsedToday) != today) return false
    val earliestAllowed = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).apply {
        time = parsedToday
        add(Calendar.DAY_OF_YEAR, -lookbackDays.coerceAtLeast(1))
    }.time
    return !parsedRelease.after(parsedToday) && !parsedRelease.before(earliestAllowed)
}

enum class MovieGenre(val displayName: String) {
    WSZYSTKIE("Wszystkie"),
    AKCJA("Akcja"),
    SCI_FI("Sci-Fi"),
    DRAMAT("Dramat"),
    KOMEDIA("Komedia"),
    THRILLER("Thriller"),
    HORROR("Horror"),
    ANIMACJA("Animacja"),
    KRYMINAL("Kryminał"),
    FANTASY("Fantasy"),
    PRZYGODOWY("Przygodowy")
}

enum class MovieRatingSource(val displayName: String) {
    FILMWEB("Filmweb"),
    TVMAZE("TVMaze"),
    NONE("Brak oceny")
}

enum class ContentTypeFilter(val label: String) {
    ALL("Wszystkie"),
    MOVIES("Filmy"),
    SERIES("Seriale")
}

@Immutable
data class Movie(
    val id: String,
    val title: String,
    val originalTitle: String,
    val year: Int,
    val genre: MovieGenre,
    val filmwebRating: Double,
    val filmwebVotesCount: Int,
    val platforms: List<StreamingPlatform>,
    val durationMinutes: Int,
    val director: String,
    val cast: List<String>,
    val description: String,
    val posterGradientColors: List<Long>,
    val posterEmoji: String,
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val isSeries: Boolean = false,
    val premiereDate: String? = null,
    val isPremiere: Boolean = false,
    val isUpcoming: Boolean = false,
    val isAutoSynced: Boolean = false,
    val hasPolishDubbing: Boolean = true,
    val ageRating: String = "16+",
    val polishTitle: String = title,
    val profileLogoUrl: String = posterUrl.ifBlank { backdropUrl },
    val ratingSource: MovieRatingSource = MovieRatingSource.FILMWEB,
    // False means streaming availability and Polish-language options have not been verified.
    val isAvailabilityVerified: Boolean = true
) {
    val hasRating: Boolean
        get() = filmwebRating > 0.0 && ratingSource != MovieRatingSource.NONE

    val isRecommendedOnFilmweb: Boolean
        get() = ratingSource == MovieRatingSource.FILMWEB && filmwebRating >= 6.5

    fun isWithinRatingRange(minRating: Double, maxRating: Double): Boolean =
        ratingSource == MovieRatingSource.FILMWEB && filmwebRating in minRating..maxRating

    fun displayTitle(usePolishTitles: Boolean = true): String =
        if (usePolishTitles) polishTitle.ifBlank { title } else originalTitle.ifBlank { title }

    fun secondaryTitle(usePolishTitles: Boolean = true): String =
        if (usePolishTitles) originalTitle.ifBlank { title } else polishTitle.ifBlank { title }

    fun withLanguage(usePolishTitles: Boolean = true): Movie =
        copy(title = displayTitle(usePolishTitles))
}

enum class MovieSortOption(val displayName: String) {
    FILMWEB_RATING_DESC("Ocena Filmweb (najwyższa)"),
    FILMWEB_RATING_ASC("Ocena Filmweb (najniższa)"),
    YEAR_DESC("Rok premiery (najnowsze)"),
    TITLE_ASC("Tytuł (A-Z)"),
    GENRE_ASC("Gatunek (A-Z)")
}
