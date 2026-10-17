package com.example

import com.example.data.MovieCatalog
import com.example.data.PosterNetworkService
import com.example.model.MovieRatingSource
import kotlinx.coroutines.runBlocking
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
    fun `unverified metadata sources do not produce fabricated VOD premieres`() = runBlocking {
        assertTrue(PosterNetworkService.fetchLatestPlatformReleases().isEmpty())
    }
}
