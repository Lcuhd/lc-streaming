package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.DeviceInfoDao
import com.example.data.local.dao.FavoriteDao
import com.example.data.local.dao.SeriesProgressDao
import com.example.data.local.entity.DeviceInfoEntity
import com.example.data.local.entity.FavoriteEntity
import com.example.data.local.entity.SeriesProgressEntity

@Database(entities = [DeviceInfoEntity::class, SeriesProgressEntity::class, FavoriteEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceInfoDao(): DeviceInfoDao
    abstract fun seriesProgressDao(): SeriesProgressDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lc_player_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
