package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist_movies ORDER BY addedAt DESC")
    fun getAllWatchlistItems(): Flow<List<WatchlistEntity>>

    @Query("SELECT movieId FROM watchlist_movies ORDER BY addedAt DESC")
    fun getAllWatchlistIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist_movies WHERE movieId = :movieId)")
    suspend fun isInWatchlist(movieId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlistItem(item: WatchlistEntity)

    @Query("DELETE FROM watchlist_movies WHERE movieId = :movieId")
    suspend fun deleteByMovieId(movieId: String)

    @Query("DELETE FROM watchlist_movies")
    suspend fun clearWatchlist()
}
