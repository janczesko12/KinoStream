package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist_movies")
data class WatchlistEntity(
    @PrimaryKey
    val movieId: String,
    val title: String,
    val originalTitle: String,
    val polishTitle: String,
    val year: Int,
    val isSeries: Boolean,
    val genreName: String,
    val filmwebRating: Double,
    val posterUrl: String,
    val addedAt: Long = System.currentTimeMillis()
)
