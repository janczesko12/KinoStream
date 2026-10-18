package com.example

import com.example.data.MovieCatalog
import com.example.data.parseStreamingPlatform
import com.example.model.MovieRatingSource
import com.example.model.StreamingPlatform
import com.example.model.isDisplayableMetadata
import com.example.model.isRecentReleaseDate
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MovieMetadataIntegrityTest {

    @Test
    fun `unknown and third party ratings are never treated as Filmweb ratings`() {
        val template = MovieCatalog.sampleMovies.first { it.id == "inception" }

        val unrated = template.copy(
            filmwebRating = 0.0,
            filmwebVotesCount = 0,
            ratingSource = MovieRatingSource.NONE,
            isAvailabilityVerified = false
        )
        assertFalse(unrated.hasRating)
        assertFalse(unrated.isRecommendedOnFilmweb)
        assertFalse(unrated.isWithinRatingRange(6.5, 10.0))

        val tvMazeRated = template.copy(
            filmwebRating = 8.0,
            filmwebVotesCount = 0,
            ratingSource = MovieRatingSource.TVMAZE,
            isAvailabilityVerified = false
        )
        assertTrue(tvMazeRated.hasRating)
        assertFalse(tvMazeRated.isRecommendedOnFilmweb)
        assertFalse(tvMazeRated.isWithinRatingRange(6.5, 10.0))
    }

    @Test
    fun `placeholder metadata is treated as unavailable`() {
        assertFalse("Brak danych".isDisplayableMetadata())
        assertFalse("   ".isDisplayableMetadata())
        assertFalse("N/A".isDisplayableMetadata())
        assertTrue("Netflix".isDisplayableMetadata())
        assertTrue("Prime Video".isDisplayableMetadata())
    }

    @Test
    fun `cached provider IDs and enum names resolve to the same platforms`() {
        assertEquals(StreamingPlatform.NETFLIX, parseStreamingPlatform("NETFLIX"))
        assertEquals(StreamingPlatform.NETFLIX, parseStreamingPlatform("netflix"))
        assertEquals(StreamingPlatform.PRIME_VIDEO, parseStreamingPlatform("prime"))
        assertEquals(StreamingPlatform.PRIME_VIDEO, parseStreamingPlatform("Prime Video"))
        assertEquals(StreamingPlatform.DISNEY_PLUS, parseStreamingPlatform("disney"))
        assertEquals(StreamingPlatform.SKYSHOWTIME, parseStreamingPlatform("skyshowtime"))
        assertEquals(null, parseStreamingPlatform("unknown-platform"))
    }

    @Test
    fun `premiere date filter accepts recent past releases only`() {
        val today = LocalDate.of(2026, 10, 18)

        assertTrue(isRecentReleaseDate("2026-10-01", today))
        assertTrue(isRecentReleaseDate("2026-04-21", today))
        assertFalse(isRecentReleaseDate("2026-04-20", today))
        assertFalse(isRecentReleaseDate("2026-11-01", today))
        assertFalse(isRecentReleaseDate("not-a-date", today))
        assertFalse(isRecentReleaseDate(null, today))
    }
}
