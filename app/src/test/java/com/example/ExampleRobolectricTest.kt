package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.MovieCatalog
import com.example.data.MovieRepository
import com.example.model.StreamingPlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("KinoStream", appName)
    }

    @Test
    fun `verify movie catalog contains 5 platforms including SkyShowtime and all items have posters and backdrops`() {
        val movies = MovieCatalog.sampleMovies
        assertTrue("Movie catalog should contain many movies", movies.size >= 40)

        val platformsFound = movies.flatMap { it.platforms }.toSet()
        assertTrue(platformsFound.contains(StreamingPlatform.NETFLIX))
        assertTrue(platformsFound.contains(StreamingPlatform.PRIME_VIDEO))
        assertTrue(platformsFound.contains(StreamingPlatform.MAX))
        assertTrue(platformsFound.contains(StreamingPlatform.DISNEY_PLUS))
        assertTrue(platformsFound.contains(StreamingPlatform.SKYSHOWTIME))

        // Verify every single item in catalog has both posterUrl and backdropUrl pre-populated
        movies.forEach { movie ->
            assertTrue(
                "Movie ${movie.id} (${movie.title}) must have posterUrl",
                movie.posterUrl.startsWith("https://")
            )
            assertTrue(
                "Movie ${movie.id} (${movie.title}) must have backdropUrl",
                movie.backdropUrl.startsWith("https://")
            )
        }

        val highRatedMovies = movies.filter { it.filmwebRating >= 6.5 }
        val lowRatedMovies = movies.filter { it.filmwebRating < 6.5 }

        assertTrue("Should have movies with rating >= 6.5", highRatedMovies.isNotEmpty())
        assertTrue("Should have movies with rating < 6.5 (for red text requirement)", lowRatedMovies.isNotEmpty())

        assertTrue(highRatedMovies.first().isRecommendedOnFilmweb)
        assertFalse(lowRatedMovies.first().isRecommendedOnFilmweb)
    }

    @Test
    fun `verify Incepcja has posterUrl and backdropUrl and SkyShowtime is supported`() {
        val movies = MovieCatalog.sampleMovies
        val inception = movies.first { it.id == "inception" }
        assertEquals("Incepcja", inception.title)
        assertTrue(inception.posterUrl.startsWith("https://"))
        assertTrue(inception.backdropUrl.startsWith("https://"))
        assertFalse(inception.posterUrl == inception.backdropUrl)

        val skyShowtimeTitles = movies.filter { it.platforms.contains(StreamingPlatform.SKYSHOWTIME) }
        assertTrue("Should have multiple SkyShowtime movies and series", skyShowtimeTitles.size >= 6)
    }

    @Test
    fun `verify custom filmweb rating range and repository persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = MovieRepository(context)

        repository.setFilmwebRatingRange(7.5, 9.0)
        assertEquals(7.5, repository.minFilmwebRating.value, 0.01)
        assertEquals(9.0, repository.maxFilmwebRating.value, 0.01)

        val dune2 = MovieCatalog.sampleMovies.first { it.id == "dune-2" }
        assertTrue(dune2.isWithinRatingRange(7.5, 9.0))
        assertFalse(dune2.isWithinRatingRange(8.5, 9.5))
    }

    @Test
    fun `verify movie title search finds specific movies and series with real posters`() {
        val movies = MovieCatalog.sampleMovies

        // Search "Incepcja"
        val inceptionMatches = movies.filter { it.title.contains("Incepcja", ignoreCase = true) }
        assertEquals(1, inceptionMatches.size)
        assertTrue(inceptionMatches.first().posterUrl.startsWith("https://"))
        assertTrue(inceptionMatches.first().backdropUrl.startsWith("https://"))

        // Search "Yellowstone" on SkyShowtime
        val yellowstoneMatches = movies.filter { it.title.contains("Yellowstone", ignoreCase = true) }
        assertEquals(1, yellowstoneMatches.size)
        assertTrue(yellowstoneMatches.first().platforms.contains(StreamingPlatform.SKYSHOWTIME))
        assertTrue(yellowstoneMatches.first().posterUrl.startsWith("https://"))

        // Search "The Boys" on Prime Video
        val boysMatches = movies.filter { it.title.contains("The Boys", ignoreCase = true) }
        assertEquals(1, boysMatches.size)
        assertTrue(boysMatches.first().platforms.contains(StreamingPlatform.PRIME_VIDEO))
        assertTrue(boysMatches.first().posterUrl.startsWith("https://"))
    }
}
