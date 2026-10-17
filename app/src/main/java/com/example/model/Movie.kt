package com.example.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.DisneyBlue
import com.example.ui.theme.MaxPurple
import com.example.ui.theme.NetflixRed
import com.example.ui.theme.PrimeVideoBlue
import com.example.ui.theme.SkyShowtimeTeal

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
    val ageRating: String = "16+"
) {
    val isRecommendedOnFilmweb: Boolean
        get() = filmwebRating >= 6.5

    fun isWithinRatingRange(minRating: Double, maxRating: Double): Boolean =
        filmwebRating in minRating..maxRating
}

enum class MovieSortOption(val displayName: String) {
    FILMWEB_RATING_DESC("Ocena Filmweb (najwyższa)"),
    FILMWEB_RATING_ASC("Ocena Filmweb (najniższa)"),
    YEAR_DESC("Rok premiery (najnowsze)"),
    TITLE_ASC("Tytuł (A-Z)"),
    GENRE_ASC("Gatunek (A-Z)")
}
