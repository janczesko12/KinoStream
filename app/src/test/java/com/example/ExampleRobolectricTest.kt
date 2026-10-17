package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.MovieCatalog
import com.example.data.MovieRepository
import com.example.data.PosterNetworkService
import com.example.data.local.KinoStreamDatabase
import com.example.model.ContentTypeFilter
import com.example.model.StreamingPlatform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
    fun `verify movie catalog contains 110+ movies and series across 5 platforms and all have posters and profile logos`() {
        val movies = MovieCatalog.sampleMovies
        assertTrue("Movie catalog should contain at least 110 movies and series", movies.size >= 110)

        val onlyFilms = movies.filter { !it.isSeries }
        val onlySeries = movies.filter { it.isSeries }
        assertTrue("Should contain at least 45 movies", onlyFilms.size >= 45)
        assertTrue("Should contain at least 55 TV series", onlySeries.size >= 55)

        val platformsFound = movies.flatMap { it.platforms }.toSet()
        assertTrue(platformsFound.contains(StreamingPlatform.NETFLIX))
        assertTrue(platformsFound.contains(StreamingPlatform.PRIME_VIDEO))
        assertTrue(platformsFound.contains(StreamingPlatform.MAX))
        assertTrue(platformsFound.contains(StreamingPlatform.DISNEY_PLUS))
        assertTrue(platformsFound.contains(StreamingPlatform.SKYSHOWTIME))

        // Verify every single item in catalog has posterUrl, backdropUrl, and profileLogoUrl pre-populated
        movies.forEach { movie ->
            assertTrue(
                "Movie ${movie.id} (${movie.title}) must have posterUrl",
                movie.posterUrl.startsWith("https://")
            )
            assertTrue(
                "Movie ${movie.id} (${movie.title}) must have backdropUrl",
                movie.backdropUrl.startsWith("https://")
            )
            assertTrue(
                "Movie ${movie.id} (${movie.title}) must have profileLogoUrl",
                movie.profileLogoUrl.startsWith("https://")
            )
            assertTrue(
                "Movie ${movie.id} (${movie.title}) displayed in catalog must have Polish dubbing",
                movie.hasPolishDubbing
            )
        }

        // Verify all 11 reported titles have valid icons/posters/profile logos
        val reportedTitles = listOf(
            "1923",
            "Fallout",
            "Oppenheimer",
            "The Mandalorian",
            "The Bear",
            "Yellowstone",
            "Landman",
            "Mayor of Kingstown",
            "Tulsa King",
            "Halo",
            "The Lord of the Rings: The Rings of Power"
        )
        for (reqTitle in reportedTitles) {
            val found = movies.firstOrNull { it.originalTitle.equals(reqTitle, ignoreCase = true) }
            assertTrue("Catalog must contain $reqTitle", found != null)
            assertTrue("$reqTitle must have valid posterUrl", found!!.posterUrl.startsWith("https://"))
            assertTrue("$reqTitle must have valid profileLogoUrl", found.profileLogoUrl.startsWith("https://"))
        }
    }

    @Test
    fun `verify ContentTypeFilter separates movies and TV series`() {
        val all = MovieCatalog.sampleMovies
        val filteredMoviesOnly = all.filter {
            when (ContentTypeFilter.MOVIES) {
                ContentTypeFilter.ALL -> true
                ContentTypeFilter.MOVIES -> !it.isSeries
                ContentTypeFilter.SERIES -> it.isSeries
            }
        }
        val filteredSeriesOnly = all.filter {
            when (ContentTypeFilter.SERIES) {
                ContentTypeFilter.ALL -> true
                ContentTypeFilter.MOVIES -> !it.isSeries
                ContentTypeFilter.SERIES -> it.isSeries
            }
        }

        assertTrue(filteredMoviesOnly.isNotEmpty())
        assertTrue(filteredSeriesOnly.isNotEmpty())
        assertTrue(filteredMoviesOnly.all { !it.isSeries })
        assertTrue(filteredSeriesOnly.all { it.isSeries })
        assertEquals(all.size, filteredMoviesOnly.size + filteredSeriesOnly.size)
    }

    @Test
    fun `verify English titles are restored by default and switching to Polish titles works via Settings`() {
        val rawMovies = MovieCatalog.allRawMovies
        val displayedMovies = MovieCatalog.sampleMovies

        assertTrue("Raw dataset should contain movies without Polish dubbing", rawMovies.any { !it.hasPolishDubbing })
        assertTrue("Displayed movies must ONLY include titles with Polish dubbing", displayedMovies.all { it.hasPolishDubbing })
        assertFalse("The Wire (no PL dubbing) must not be displayed", displayedMovies.any { it.id == "no-dubbing-the-wire" })
        assertFalse("The Sopranos (no PL dubbing) must not be displayed", displayedMovies.any { it.id == "no-dubbing-sopranos" })

        // Verify titles that remain in English on Polish streaming platforms (e.g. Breaking Bad, The Boys, The Last of Us, Fallout, Stranger Things)
        // stay identical whether usePolishTitles = false (NAT) or usePolishTitles = true (PL)!
        val breakingBad = displayedMovies.first { it.id == "breaking-bad" }
        assertEquals("Breaking Bad", breakingBad.title)
        assertEquals("Breaking Bad", breakingBad.polishTitle)
        assertEquals("Breaking Bad", breakingBad.displayTitle(usePolishTitles = false))
        assertEquals("Breaking Bad", breakingBad.displayTitle(usePolishTitles = true))
        assertEquals("Breaking Bad", breakingBad.withLanguage(usePolishTitles = false).title)
        assertEquals("Breaking Bad", breakingBad.withLanguage(usePolishTitles = true).title)
        assertEquals("Breaking Bad", PosterNetworkService.localizeTitleToPolish("Breaking Bad", isSeries = true))

        val boys = displayedMovies.first { it.id == "the-boys-series" }
        assertEquals("The Boys", boys.title)
        assertEquals("The Boys", boys.polishTitle)
        assertEquals("The Boys", boys.displayTitle(usePolishTitles = false))
        assertEquals("The Boys", boys.displayTitle(usePolishTitles = true))

        val tlou = displayedMovies.first { it.id == "the-last-of-us-series" }
        assertEquals("The Last of Us", tlou.title)
        assertEquals("The Last of Us", tlou.polishTitle)
        assertEquals("The Last of Us", tlou.displayTitle(usePolishTitles = false))
        assertEquals("The Last of Us", tlou.displayTitle(usePolishTitles = true))

        val fallout = displayedMovies.first { it.id == "fallout-series" }
        assertEquals("Fallout", fallout.displayTitle(usePolishTitles = false))
        assertEquals("Fallout", fallout.displayTitle(usePolishTitles = true))

        // Verify titles that have official translated names on Polish streaming platforms are Polish by default
        val inception = displayedMovies.first { it.id == "inception" }
        assertEquals("Incepcja", inception.title)
        assertEquals("Incepcja", inception.polishTitle)
        assertEquals("Inception", inception.originalTitle)
        assertEquals("Inception", inception.displayTitle(usePolishTitles = false))
        assertEquals("Incepcja", inception.displayTitle(usePolishTitles = true))
        assertEquals("Incepcja", PosterNetworkService.localizeTitleToPolish("Inception", isSeries = false))

        val succession = displayedMovies.first { it.id == "succession-series" }
        assertEquals("Sukcesja", succession.title)
        assertEquals("Succession", succession.displayTitle(usePolishTitles = false))
        assertEquals("Sukcesja", succession.displayTitle(usePolishTitles = true))

        val witcher = displayedMovies.first { it.id == "the-witcher-series" }
        assertEquals("Wiedźmin", witcher.title)
        assertEquals("The Witcher", witcher.displayTitle(usePolishTitles = false))
        assertEquals("Wiedźmin", witcher.displayTitle(usePolishTitles = true))

        // Verify Settings preference in MovieRepository defaults to Polish streaming titles (true) and can be toggled
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = MovieRepository(context)
        assertTrue("Default title language should be Polish streaming titles (usePolishTitles = true)", repository.usePolishTitles.value)

        repository.setUsePolishTitles(false)
        assertFalse("After toggling to native in settings, usePolishTitles should be false", repository.usePolishTitles.value)

        repository.setUsePolishTitles(true)
        assertTrue("After restoring Polish streaming titles in settings, usePolishTitles should be true", repository.usePolishTitles.value)
    }

    @Test
    fun `verify Inception has posterUrl and backdropUrl and SkyShowtime is supported`() {
        val movies = MovieCatalog.sampleMovies
        val inception = movies.first { it.id == "inception" }
        assertEquals("Incepcja", inception.title)
        assertEquals("Incepcja", inception.polishTitle)
        assertEquals("Inception", inception.originalTitle)
        assertTrue(inception.posterUrl.startsWith("https://"))
        assertTrue(inception.backdropUrl.startsWith("https://"))
        assertFalse(inception.posterUrl == inception.backdropUrl)

        val skyShowtimeTitles = movies.filter { it.platforms.contains(StreamingPlatform.SKYSHOWTIME) }
        assertTrue("Should have many SkyShowtime movies and series", skyShowtimeTitles.size >= 15)
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
    fun `verify movie title search finds movies by both English and Polish streaming titles`() {
        val movies = MovieCatalog.sampleMovies

        // Search by English title "Inception" or Polish streaming title "Incepcja"
        val inceptionByEn = movies.filter {
            it.title.contains("Inception", ignoreCase = true) ||
                it.originalTitle.contains("Inception", ignoreCase = true) ||
                it.polishTitle.contains("Inception", ignoreCase = true)
        }
        assertEquals(1, inceptionByEn.size)

        val inceptionByPl = movies.filter {
            it.title.contains("Incepcja", ignoreCase = true) ||
                it.originalTitle.contains("Incepcja", ignoreCase = true) ||
                it.polishTitle.contains("Incepcja", ignoreCase = true)
        }
        assertEquals(1, inceptionByPl.size)

        // Search "Yellowstone" on SkyShowtime
        val yellowstoneMatches = movies.filter { it.title.contains("Yellowstone", ignoreCase = true) }
        assertEquals(1, yellowstoneMatches.size)
        assertTrue(yellowstoneMatches.first().platforms.contains(StreamingPlatform.SKYSHOWTIME))
        assertTrue(yellowstoneMatches.first().posterUrl.startsWith("https://"))

        // Search "Breaking Bad" on Netflix (remains "Breaking Bad" in both NAT and PL)
        val bbMatches = movies.filter { it.title.equals("Breaking Bad", ignoreCase = true) }
        assertEquals(1, bbMatches.size)
        assertEquals("Breaking Bad", bbMatches.first().polishTitle)
        assertTrue(bbMatches.first().platforms.contains(StreamingPlatform.NETFLIX))
    }

    @Test
    fun `verify Room Watchlist database saves and removes movies to watch later`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, KinoStreamDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = MovieRepository(context, db.watchlistDao())

        val inception = MovieCatalog.sampleMovies.first { it.id == "inception" }
        val fallout = MovieCatalog.sampleMovies.first { it.id == "fallout-series" }

        // Initially empty
        assertTrue(repository.watchlistMovieIdsFlow.first().isEmpty())

        // Add Inception to Watchlist
        val addedInception = repository.toggleWatchlist(inception)
        assertTrue("First toggle should add movie to Room Watchlist", addedInception)
        assertTrue(repository.watchlistMovieIdsFlow.first().contains("inception"))

        // Add Fallout to Watchlist
        val addedFallout = repository.toggleWatchlist(fallout)
        assertTrue(addedFallout)
        val watchlistEntries = repository.watchlistEntries.first()
        assertEquals(2, watchlistEntries.size)

        // Toggle Inception again to remove from Watchlist
        val removedInception = repository.toggleWatchlist(inception)
        assertFalse("Second toggle should remove movie from Room Watchlist", removedInception)
        assertFalse(repository.watchlistMovieIdsFlow.first().contains("inception"))
        assertTrue(repository.watchlistMovieIdsFlow.first().contains("fallout-series"))

        db.close()
    }
}
