package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WatchlistEntity::class],
    version = 1,
    exportSchema = true
)
abstract class KinoStreamDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        @Volatile
        private var INSTANCE: KinoStreamDatabase? = null

        fun getInstance(context: Context): KinoStreamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KinoStreamDatabase::class.java,
                    "kinostream_watchlist.db"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
