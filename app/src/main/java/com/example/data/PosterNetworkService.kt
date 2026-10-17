package com.example.data

import com.example.model.Movie
import com.example.model.MovieGenre
import com.example.model.MovieRatingSource
import com.example.model.StreamingPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object PosterNetworkService {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // In-memory cache of resolved official posters & profile logos
    private val resolvedPosterCache = ConcurrentHashMap<String, String>()

    // Official Polish VOD Streaming Title Registry (Netflix PL, Prime Video PL, Max PL, Disney+ PL, SkyShowtime PL).
    // Titles that are NOT translated on Polish streaming stay in their original form (e.g., "Breaking Bad" -> "Breaking Bad")
    // regardless of whether Polish (PL) or Native (NAT) mode is active.
    private val polishTitleDictionary: Map<String, String> = mapOf(
        // Untranslated on Polish streaming (stay identical in PL and NAT!)
        "breaking bad" to "Breaking Bad",
        "the boys" to "The Boys",
        "fallout" to "Fallout",
        "the last of us" to "The Last of Us",
        "stranger things" to "Stranger Things",
        "peaky blinders" to "Peaky Blinders",
        "the bear" to "The Bear",
        "tulsa king" to "Tulsa King",
        "mayor of kingstown" to "Mayor of Kingstown",
        "landman" to "Landman",
        "the mandalorian" to "The Mandalorian",
        "1923" to "1923",
        "1883" to "1883",
        "yellowstone" to "Yellowstone",
        "oppenheimer" to "Oppenheimer",
        "halo" to "Halo",
        "interstellar" to "Interstellar",
        "wednesday" to "Wednesday",
        "squid game" to "Squid Game",
        "arcane" to "Arcane",
        "dark" to "Dark",
        "the crown" to "The Crown",
        "reacher" to "Reacher",
        "narcos" to "Narcos",
        "mindhunter" to "Mindhunter",
        "ozark" to "Ozark",
        "cyberpunk: edgerunners" to "Cyberpunk: Edgerunners",
        "one piece" to "One Piece",
        "cobra kai" to "Cobra Kai",
        "jack ryan" to "Jack Ryan",
        "fargo" to "Fargo",
        "westworld" to "Westworld",
        "loki" to "Loki",
        "wandavision" to "WandaVision",
        "ahsoka" to "Ahsoka",
        "obi-wan kenobi" to "Obi-Wan Kenobi",
        "daredevil" to "Daredevil",
        "x-men '97" to "X-Men '97",
        "lioness" to "Lioness",
        "dexter" to "Dexter",
        "billions" to "Billions",
        "andor" to "Andor",
        "top gun: maverick" to "Top Gun: Maverick",
        "deadpool & wolverine" to "Deadpool & Wolverine",
        "beetlejuice beetlejuice" to "Beetlejuice Beetlejuice",
        "road house" to "Road House",
        "madame web" to "Madame Web",
        "borderlands" to "Borderlands",
        "gladiator ii" to "Gladiator II",
        "gladiator" to "Gladiator",
        "joker" to "Joker",
        "joker: folie a deux" to "Joker: Folie à deux",
        "pulp fiction" to "Pulp Fiction",
        "forrest gump" to "Forrest Gump",
        "john wick: chapter 4" to "John Wick 4",
        "parasite" to "Parasite",
        "barbie" to "Barbie",
        "wonka" to "Wonka",
        "civil war" to "Civil War",
        "twisters" to "Twisters",
        "bad boys: ride or die" to "Bad Boys: Ride or Die",
        "kung fu panda 4" to "Kung Fu Panda 4",
        "mission: impossible - dead reckoning part one" to "Mission: Impossible - Dead Reckoning Part One",

        // Officially localized on Polish streaming platforms (Netflix PL, Max PL, Disney+ PL, Prime PL, SkyShowtime PL)
        "inception" to "Incepcja",
        "dune: part two" to "Diuna: Część druga",
        "dune" to "Diuna",
        "house of the dragon" to "Ród Smoka",
        "game of thrones" to "Gra o tron",
        "the witcher" to "Wiedźmin",
        "the day of the jackal" to "Dzień Szakala",
        "shogun" to "Szōgun",
        "money heist" to "Dom z papieru",
        "la casa de papel" to "Dom z papieru",
        "black mirror" to "Czarne lustro",
        "bridgerton" to "Bridgertonowie",
        "3 body problem" to "Problem trzech ciał",
        "the gentlemen" to "Dżentelmeni",
        "the night agent" to "Nocny agent",
        "invincible" to "Niezwyciężony",
        "the wheel of time" to "Koło Czasu",
        "good omens" to "Dobry omen",
        "mr. & mrs. smith" to "Pan i Pani Smith",
        "clarkson's farm" to "Farma Clarksona",
        "the rings of power" to "Władca Pierścieni: Pierścienie Władzy",
        "the lord of the rings: the rings of power" to "Władca Pierścieni: Pierścienie Władzy",
        "succession" to "Sukcesja",
        "thepenguin" to "Pingwin",
        "the penguin" to "Pingwin",
        "true detective" to "Detektyw",
        "the white lotus" to "Biały Lotos",
        "euphoria" to "Euforia",
        "silicon valley" to "Dolina Krzemowa",
        "chernobyl" to "Czarnobyl",
        "only murders in the building" to "Zbrodnie po sąsiedzku",
        "star trek: strange new worlds" to "Star Trek: Nieznane nowe światy",
        "twin peaks" to "Miasteczko Twin Peaks",
        "prison break" to "Skazany na śmierć",
        "lost" to "Zagubieni",
        "vikings" to "Wikingowie",
        "rick and morty" to "Rick i Morty",
        "the dark knight" to "Mroczny Rycerz",
        "poor things" to "Biedne istoty",
        "avatar: the way of water" to "Avatar: Istota wody",
        "spider-man: across the spider-verse" to "Spider-Man: Poprzez multiwersum",
        "inside out 2" to "W głowie się nie mieści 2",
        "the batman" to "Batman",
        "glass onion: a knives out mystery" to "Glass Onion: Film z serii „Na noże”",
        "glass onion" to "Glass Onion: Film z serii „Na noże”",
        "the substance" to "Substancja",
        "alien: romulus" to "Obcy: Romulus",
        "furiosa: a mad max saga" to "Furiosa: Saga Mad Max",
        "guardians of the galaxy vol. 3" to "Strażnicy Galaktyki: Volume 3",
        "venom: the last dance" to "Venom 3: Ostatni taniec",
        "rebel moon - part one: a child of fire" to "Rebel Moon – Część 1: Dziecko ognia",
        "ant-man and the wasp: quantumania" to "Ant-Man i Osa: Kwantomania",
        "red notice" to "Czerwona nota",
        "the matrix resurrections" to "Matrix Zmartwychwstania",
        "the matrix" to "Matrix",
        "kraven the hunter" to "Kraven Łowca",
        "meg 2: the trench" to "Meg 2: Głębia",
        "avengers: endgame" to "Avengers: Koniec gry",
        "avengers: infinity war" to "Avengers: Wojna bez granic",
        "spider-man: no way home" to "Spider-Man: Bez drogi do domu",
        "the shawshank redemption" to "Skazani na Shawshank",
        "the godfather" to "Ojciec chrzestny",
        "fight club" to "Podziemny krąg",
        "the lord of the rings: the fellowship of the ring" to "Władca Pierścieni: Drużyna Pierścienia",
        "the lord of the rings: the return of the king" to "Władca Pierścieni: Powrót Króla",
        "mad max: fury road" to "Mad Max: Na drodze gniewu",
        "everything everywhere all at once" to "Wszystko wszędzie naraz",
        "a quiet place: day one" to "Ciche miejsce: Dzień pierwszy",
        "the fall guy" to "Kaskader",
        "kingdom of the planet of the apes" to "Królestwo Planety Małp",
        "despicable me 4" to "Gru i Minionki: Pod przykrywką",
        "the super mario bros. movie" to "Super Mario Bros. Film",
        "puss in boots: the last wish" to "Kot w butach: Ostatnie życzenie"
    )

    // Titles known NOT to have Polish dubbing (filtered out and never displayed)
    private val noPolishDubbingTitles: Set<String> = setOf(
        "the wire",
        "prawo ulicy",
        "the sopranos",
        "rodzina soprano",
        "mad men",
        "deadwood",
        "six feet under",
        "oz"
    )

    fun hasPolishDubbingForTitle(title: String, originalTitle: String = title): Boolean {
        val lowerTitle = title.trim().lowercase()
        val lowerOrig = originalTitle.trim().lowercase()
        return !noPolishDubbingTitles.any {
            lowerTitle.contains(it) || lowerOrig.contains(it)
        }
    }

    private val resolvedPolishTitleCache = ConcurrentHashMap<String, String>()

    /**
     * Resolves the official title as displayed on Polish streaming platforms
     * (Netflix Polska, Prime Video Polska, Max Polska, Disney+ Polska, SkyShowtime Polska).
     * Never invents literal translations for untranslated shows (e.g., "Breaking Bad" stays "Breaking Bad").
     */
    fun localizeTitleToPolish(
        rawTitle: String,
        isSeries: Boolean,
        catalogPolishFallback: String = ""
    ): String {
        val cleaned = rawTitle.trim()
        if (cleaned.isBlank()) return catalogPolishFallback.ifBlank { cleaned }
        val lower = cleaned.lowercase()
        polishTitleDictionary[lower]?.let { return it }
        resolvedPolishTitleCache[lower]?.let { return it }
        if (catalogPolishFallback.isNotBlank()) return catalogPolishFallback.trim()
        return cleaned
    }

    /**
     * Synchronously queries Polish streaming catalog metadata (Apple iTunes Poland `country=PL&lang=pl_pl`
     * and the Polish VOD Streaming Title Registry) to resolve the official Polish streaming title.
     * If a show/movie is not translated on Polish VOD (e.g. "Breaking Bad"), it remains "Breaking Bad".
     */
    fun fetchPolishStreamingTitleSync(originalTitle: String, isSeries: Boolean): String {
        val cleaned = originalTitle.trim()
        if (cleaned.isBlank()) return cleaned
        val lower = cleaned.lowercase()
        polishTitleDictionary[lower]?.let { return it }
        resolvedPolishTitleCache[lower]?.let { return it }

        try {
            val media = if (isSeries) "tvShow" else "movie"
            val entity = if (isSeries) "tvSeason" else "movie"
            val encoded = URLEncoder.encode(cleaned, "UTF-8")
            val url = "https://itunes.apple.com/search?term=$encoded&country=PL&lang=pl_pl&media=$media&entity=$entity&limit=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; KinoStream/1.0)")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val results = json.optJSONArray("results")
                        if (results != null && results.length() > 0) {
                            val first = results.getJSONObject(0)
                            val plTrack = if (isSeries) {
                                first.optString("artistName", "")
                                    .ifBlank { first.optString("collectionName", "") }
                            } else {
                                first.optString("trackName", "")
                            }.replace(Regex("(?i),?\\s*(Season|Sezon|Series)\\s*\\d+.*$"), "").trim()

                            if (plTrack.isNotBlank()) {
                                resolvedPolishTitleCache[lower] = plTrack
                                return plTrack
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        resolvedPolishTitleCache[lower] = cleaned
        return cleaned
    }

    /**
     * Queries Polish streaming catalog metadata (Apple iTunes Poland `country=PL&lang=pl_pl`
     * and the Polish VOD Streaming Title Registry) to resolve the official Polish streaming title.
     * If a show/movie is not translated on Polish VOD (e.g. "Breaking Bad"), it remains "Breaking Bad".
     */
    suspend fun fetchPolishStreamingTitle(originalTitle: String, isSeries: Boolean): String =
        withContext(Dispatchers.IO) {
            fetchPolishStreamingTitleSync(originalTitle, isSeries)
        }

    fun getCachedPosterUrl(title: String): String? =
        resolvedPosterCache[title.trim().lowercase()]

    /**
     * Automatically resolves official movie or TV series poster & profile logo URL
     * from TVMaze, Apple iTunes (US & PL), and Wikipedia REST APIs.
     */
    suspend fun fetchOfficialPosterUrl(originalTitle: String, isSeries: Boolean): String? =
        withContext(Dispatchers.IO) {
            val cacheKey = originalTitle.trim().lowercase()
            if (cacheKey.isBlank()) return@withContext null
            resolvedPosterCache[cacheKey]?.let { return@withContext it }

            try {
                val resolved = if (isSeries) {
                    fetchFromTvMaze(originalTitle)
                        ?: fetchFromItunes(originalTitle, isSeries = true)
                        ?: fetchFromWikipedia(originalTitle)
                        ?: fetchFromItunes(originalTitle, isSeries = false)
                } else {
                    fetchFromItunes(originalTitle, isSeries = false)
                        ?: fetchFromWikipedia(originalTitle)
                        ?: fetchFromTvMaze(originalTitle)
                }
                if (!resolved.isNullOrBlank()) {
                    resolvedPosterCache[cacheKey] = resolved
                }
                resolved
            } catch (_: Exception) {
                null
            }
        }

    private fun fetchFromItunes(query: String, isSeries: Boolean = false): String? {
        val media = if (isSeries) "tvShow" else "movie"
        val entity = if (isSeries) "tvSeason" else "movie"
        for (country in listOf("US", "PL")) {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = "https://itunes.apple.com/search?term=$encoded&country=$country&media=$media&entity=$entity&limit=1"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Android; KinoStream/1.0)")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val results = json.optJSONArray("results")
                            if (results != null && results.length() > 0) {
                                val first = results.getJSONObject(0)
                                val artwork100 = first.optString("artworkUrl100", "")
                                if (artwork100.isNotBlank()) {
                                    return artwork100
                                        .replace("100x100bb.jpg", "400x600bb.jpg")
                                        .replace("100x100bb.png", "400x600bb.png")
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun fetchFromWikipedia(query: String): String? {
        for (lang in listOf("en", "pl")) {
            try {
                val pageTitle = URLEncoder.encode(query.trim().replace(" ", "_"), "UTF-8")
                val url = "https://$lang.wikipedia.org/api/rest_v1/page/summary/$pageTitle"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Android; KinoStream/1.0)")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val thumb = json.optJSONObject("thumbnail")?.optString("source", "")
                                ?: json.optJSONObject("originalimage")?.optString("source", "")
                                ?: ""
                            if (thumb.isNotBlank()) {
                                return thumb
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun fetchFromTvMaze(query: String): String? {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.tvmaze.com/singlesearch/shows?q=$encoded"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; KinoStream/1.0)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val imageObj = json.optJSONObject("image") ?: return null
                val medium = imageObj.optString("medium", "")
                val original = imageObj.optString("original", "")
                when {
                    medium.isNotBlank() -> medium.replace("http://", "https://")
                    original.isNotBlank() -> original.replace("http://", "https://")
                    else -> null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Automatically fetches new releases with Polish dubbing that came out on Netflix,
     * Prime Video, Max, Disney+, and SkyShowtime.
     */
    /**
     * TVMaze and Apple iTunes provide metadata, not current Polish VOD availability or dubbing.
     * Do not classify their results as premieres until a reliable availability source is integrated.
     */
    suspend fun fetchLatestPlatformReleases(): List<Movie> = withContext(Dispatchers.IO) {
        emptyList()
    }

    private fun fetchTvMazeWebSchedule(): List<Movie> {
        val url = "https://api.tvmaze.com/schedule/web"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android; KinoStream/1.0)")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val array = JSONArray(body)
            val results = mutableListOf<Movie>()

            for (i in 0 until array.length()) {
                if (results.size >= 12) break
                val ep = array.getJSONObject(i)
                val embedded = ep.optJSONObject("_embedded") ?: continue
                val show = embedded.optJSONObject("show") ?: continue
                val imageObj = show.optJSONObject("image") ?: continue
                val poster = imageObj.optString("medium").ifBlank { imageObj.optString("original") }
                    .replace("http://", "https://")
                if (poster.isBlank()) continue

                val rawName = show.optString("name", "").ifBlank { continue }
                val polishName = fetchPolishStreamingTitleSync(rawName, isSeries = true)
                val hasDubbing = hasPolishDubbingForTitle(polishName, rawName)
                if (!hasDubbing) continue

                val webChannel = show.optJSONObject("webChannel")?.optString("name", "")
                    ?: show.optJSONObject("network")?.optString("name", "") ?: ""
                val platforms = mapPlatforms(webChannel, rawName)
                val airdate = ep.optString("airdate", "Nowość")
                val year = airdate.take(4).toIntOrNull() ?: 2025

                val ratingObj = show.optJSONObject("rating")
                val rawAvg = ratingObj?.optDouble("average", 0.0) ?: 0.0
                val rating = if (rawAvg.isNaN() || rawAvg <= 0.0) 0.0
                else kotlin.math.round(rawAvg * 10.0) / 10.0
                val ratingSource = if (rating > 0.0) MovieRatingSource.TVMAZE else MovieRatingSource.NONE

                val genresArr = show.optJSONArray("genres")
                val firstGenre = if (genresArr != null && genresArr.length() > 0) genresArr.optString(0) else ""

                results.add(
                    Movie(
                        id = "auto-tvmaze-${show.optInt("id", i)}",
                        title = polishName,
                        originalTitle = rawName,
                        polishTitle = polishName,
                        year = year,
                        genre = mapGenre(firstGenre),
                        filmwebRating = rating,
                        filmwebVotesCount = 0,
                        ratingSource = ratingSource,
                        isAvailabilityVerified = false,
                        platforms = platforms,
                        durationMinutes = show.optInt("averageRuntime", 50).takeIf { it > 0 } ?: 50,
                        director = "Brak danych",
                        cast = emptyList(),
                        description = "Metadane serialu z TVMaze. Ocena nie pochodzi z Filmweb, a dostępność w Polsce i polska wersja językowa nie zostały zweryfikowane.",
                        posterGradientColors = listOf(0xFF0F4C81, 0xFF1B263B, 0xFF0D1B2A),
                        posterEmoji = "🆕",
                        posterUrl = poster,
                        backdropUrl = poster,
                        isSeries = true,
                        premiereDate = null,
                        isPremiere = false,
                        isUpcoming = false,
                        isAutoSynced = false,
                        hasPolishDubbing = true,
                        ageRating = "16+"
                    )
                )
            }
            return results
        }
    }

    /**
     * Live searches TVMaze (TV series) and Apple iTunes PL (Movies) for any user query,
     * returning only series & movies with Polish dubbing.
     */
    suspend fun searchLiveSeriesAndMovies(query: String): List<Movie> =
        withContext(Dispatchers.IO) {
            if (query.trim().length < 2) return@withContext emptyList()
            val results = mutableListOf<Movie>()
            try {
                results.addAll(searchTvMazeShows(query))
            } catch (_: Exception) {}
            try {
                results.addAll(searchItunesMovies(query, limit = 6))
            } catch (_: Exception) {}
            results.filter { it.hasPolishDubbing }
        }

    private fun searchTvMazeShows(query: String): List<Movie> {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "https://api.tvmaze.com/search/shows?q=$encoded"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android; KinoStream/1.0)")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val array = JSONArray(body)
            val list = mutableListOf<Movie>()

            for (i in 0 until minOf(array.length(), 6)) {
                val item = array.getJSONObject(i)
                val show = item.optJSONObject("show") ?: continue
                val imageObj = show.optJSONObject("image") ?: continue
                val poster = imageObj.optString("medium").ifBlank { imageObj.optString("original") }
                    .replace("http://", "https://")
                if (poster.isBlank()) continue

                val id = "tvmaze-${show.optInt("id", i)}"
                val rawName = show.optString("name", "").ifBlank { continue }
                val polishName = fetchPolishStreamingTitleSync(rawName, isSeries = true)
                val hasDubbing = hasPolishDubbingForTitle(polishName, rawName)
                if (!hasDubbing) continue

                val premiered = show.optString("premiered", "2023")
                val year = premiered.take(4).toIntOrNull() ?: 2023
                val ratingObj = show.optJSONObject("rating")
                val rawAvg = ratingObj?.optDouble("average", 0.0) ?: 0.0
                val rating = if (rawAvg.isNaN() || rawAvg <= 0.0) 0.0
                else kotlin.math.round(rawAvg * 10.0) / 10.0
                val ratingSource = if (rating > 0.0) MovieRatingSource.TVMAZE else MovieRatingSource.NONE

                val genresArr = show.optJSONArray("genres")
                val firstGenre = if (genresArr != null && genresArr.length() > 0) genresArr.optString(0) else ""
                val mappedGenre = mapGenre(firstGenre)

                val networkName = show.optJSONObject("webChannel")?.optString("name", "")
                    ?: show.optJSONObject("network")?.optString("name", "") ?: ""
                val platforms = mapPlatforms(networkName, rawName)

                list.add(
                    Movie(
                        id = id,
                        title = polishName,
                        originalTitle = rawName,
                        polishTitle = polishName,
                        year = year,
                        genre = mappedGenre,
                        filmwebRating = rating,
                        filmwebVotesCount = 0,
                        ratingSource = ratingSource,
                        isAvailabilityVerified = false,
                        platforms = platforms,
                        durationMinutes = show.optInt("averageRuntime", 50).takeIf { it > 0 } ?: 50,
                        director = "Brak danych",
                        cast = emptyList(),
                        description = "Metadane serialu z TVMaze. Ocena nie pochodzi z Filmweb, a dostępność w Polsce i polska wersja językowa nie zostały zweryfikowane.",
                        posterGradientColors = listOf(0xFF0F4C81, 0xFF1B263B, 0xFF0D1B2A),
                        posterEmoji = "📺",
                        posterUrl = poster,
                        backdropUrl = poster,
                        isSeries = true,
                        hasPolishDubbing = true,
                        ageRating = "16+"
                    )
                )
            }
            return list
        }
    }

    private fun searchItunesMovies(
        query: String,
        limit: Int = 6
    ): List<Movie> {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "https://itunes.apple.com/search?term=$encoded&country=PL&lang=pl_pl&media=movie&entity=movie&limit=$limit"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android; KinoStream/1.0)")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: return emptyList()
            val list = mutableListOf<Movie>()

            for (i in 0 until minOf(results.length(), limit)) {
                val track = results.getJSONObject(i)
                val rawTrackName = track.optString("trackName", "").ifBlank { continue }
                val polishTitle = localizeTitleToPolish(rawTrackName, isSeries = false)
                val hasDubbing = hasPolishDubbingForTitle(polishTitle, rawTrackName)
                if (!hasDubbing) continue

                val artwork100 = track.optString("artworkUrl100", "").ifBlank { continue }
                val highResPoster = artwork100
                    .replace("100x100bb.jpg", "400x600bb.jpg")
                    .replace("100x100bb.png", "400x600bb.png")

                val trackId = "itunes-${track.optLong("trackId", i.toLong())}"
                val releaseDate = track.optString("releaseDate", "2024")
                val year = releaseDate.take(4).toIntOrNull() ?: 2024
                val genreName = track.optString("primaryGenreName", "")
                val durationMillis = track.optLong("trackTimeMillis", 7200000L)
                val durationMillis = track.optLong("trackTimeMillis", 7200000L)
                val durationMinutes = (durationMillis / 60000L).toInt().coerceIn(75, 220)

                // iTunes search results do not contain Filmweb scores or verified VOD availability.
                val rating = 0.0
                val platforms = mapPlatforms("", rawTrackName)

                list.add(
                    Movie(
                        id = trackId,
                        title = polishTitle,
                        originalTitle = rawTrackName,
                        polishTitle = polishTitle,
                        year = year,
                        genre = mapGenre(genreName),
                        filmwebRating = rating,
                        filmwebVotesCount = 0,
                        ratingSource = MovieRatingSource.NONE,
                        isAvailabilityVerified = false,
                        platforms = platforms,
                        durationMinutes = durationMinutes,
                        director = "Brak danych",
                        cast = emptyList(),
                        description = "Metadane i plakat z Apple iTunes. Ocena Filmweb, dostępność VOD w Polsce i polska wersja językowa nie zostały zweryfikowane.",
                        posterGradientColors = listOf(0xFF1D3557, 0xFF457B9D, 0xFF0D1B2A),
                        posterEmoji = "🎬",
                        posterUrl = highResPoster,
                        backdropUrl = highResPoster,
                        isSeries = false,
                        premiereDate = null,
                        isPremiere = false,
                        isAutoSynced = false,
                        hasPolishDubbing = true,
                        ageRating = track.optString("contentAdvisoryRating", "12+")
                    )
                )
            }
            return list
        }
    }

    private fun mapGenre(raw: String): MovieGenre {
        val lower = raw.lowercase()
        return when {
            lower.contains("sci") || lower.contains("science") -> MovieGenre.SCI_FI
            lower.contains("action") || lower.contains("adventure") || lower.contains("akcja") -> MovieGenre.AKCJA
            lower.contains("comedy") || lower.contains("komedia") -> MovieGenre.KOMEDIA
            lower.contains("drama") || lower.contains("dramat") -> MovieGenre.DRAMAT
            lower.contains("thriller") || lower.contains("mystery") -> MovieGenre.THRILLER
            lower.contains("horror") -> MovieGenre.HORROR
            lower.contains("anim") || lower.contains("kids") || lower.contains("family") || lower.contains("rodzin") -> MovieGenre.ANIMACJA
            lower.contains("crime") || lower.contains("krymina") -> MovieGenre.KRYMINAL
            lower.contains("fantasy") -> MovieGenre.FANTASY
            else -> MovieGenre.DRAMAT
        }
    }

    private fun mapPlatforms(network: String, title: String): List<StreamingPlatform> {
        val lower = network.lowercase()
        return when {
            lower.contains("netflix") -> listOf(StreamingPlatform.NETFLIX)
            lower.contains("prime") || lower.contains("amazon") -> listOf(StreamingPlatform.PRIME_VIDEO)
            lower.contains("hbo") || lower.contains("max") -> listOf(StreamingPlatform.MAX)
            lower.contains("disney+") || lower == "disney" -> listOf(StreamingPlatform.DISNEY_PLUS)
            lower.contains("skyshowtime") -> listOf(StreamingPlatform.SKYSHOWTIME)
            else -> emptyList()
        }
    }
}
