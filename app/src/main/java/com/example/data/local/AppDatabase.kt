package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.DeviceInfoDao
import com.example.data.local.dao.FavoriteDao
import com.example.data.local.dao.ParentalProtectionDao
import com.example.data.local.dao.SeriesProgressDao
import com.example.data.local.entity.DeviceInfoEntity
import com.example.data.local.entity.FavoriteEntity
import com.example.data.local.entity.ParentalProtectionEntity
import com.example.data.local.entity.SeriesProgressEntity

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `series_progress_new` (
                `progressKey` TEXT NOT NULL,
                `listId` TEXT,
                `contentType` TEXT,
                `contentId` TEXT,
                `contentTitle` TEXT,
                `contentImage` TEXT,
                `episodeId` TEXT NOT NULL,
                `seriesId` TEXT NOT NULL,
                `seasonNum` INTEGER NOT NULL,
                `episodeNum` INTEGER NOT NULL,
                `positionMs` INTEGER NOT NULL,
                `durationMs` INTEGER NOT NULL,
                `completed` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`progressKey`)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `series_progress_new` (
                `progressKey`,
                `listId`,
                `contentType`,
                `contentId`,
                `contentTitle`,
                `contentImage`,
                `episodeId`,
                `seriesId`,
                `seasonNum`,
                `episodeNum`,
                `positionMs`,
                `durationMs`,
                `completed`,
                `updatedAt`
            )
            SELECT
                `episodeId` AS `progressKey`,
                CASE
                    WHEN SUBSTR(`seriesId`, 1, 4) = 'VOD_' THEN SUBSTR(`seriesId`, 5)
                    ELSE NULL
                END AS `listId`,
                CASE
                    WHEN SUBSTR(`seriesId`, 1, 4) = 'VOD_' THEN 'VOD'
                    ELSE 'SERIES'
                END AS `contentType`,
                CASE
                    WHEN SUBSTR(`seriesId`, 1, 4) = 'VOD_' THEN
                        CASE
                            WHEN INSTR(`episodeId`, '_VOD_') > 0 THEN SUBSTR(`episodeId`, INSTR(`episodeId`, '_VOD_') + 5)
                            ELSE `episodeId`
                        END
                    ELSE `seriesId`
                END AS `contentId`,
                NULL AS `contentTitle`,
                NULL AS `contentImage`,
                `episodeId`,
                `seriesId`,
                `seasonNum`,
                `episodeNum`,
                `positionMs`,
                `durationMs`,
                `completed`,
                `updatedAt`
            FROM `series_progress`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `series_progress`")
        db.execSQL("ALTER TABLE `series_progress_new` RENAME TO `series_progress`")
    }
}

@Database(
    entities = [DeviceInfoEntity::class, SeriesProgressEntity::class, FavoriteEntity::class, ParentalProtectionEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceInfoDao(): DeviceInfoDao
    abstract fun seriesProgressDao(): SeriesProgressDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun parentalProtectionDao(): ParentalProtectionDao

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
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
