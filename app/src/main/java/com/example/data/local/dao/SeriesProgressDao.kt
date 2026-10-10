package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SeriesProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interface DAO para persistência local do progresso de séries.
 */
@Dao
interface SeriesProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: SeriesProgressEntity)

    @Query("SELECT * FROM series_progress WHERE seriesId = :seriesId ORDER BY updatedAt DESC")
    suspend fun getProgressForSeries(seriesId: String): List<SeriesProgressEntity>

    @Query("SELECT * FROM series_progress WHERE seriesId = :seriesId ORDER BY updatedAt DESC")
    fun getProgressFlowForSeries(seriesId: String): Flow<List<SeriesProgressEntity>>

    @Query("SELECT * FROM series_progress WHERE episodeId = :episodeId LIMIT 1")
    suspend fun getProgressForEpisode(episodeId: String): SeriesProgressEntity?

    @Query("DELETE FROM series_progress WHERE seriesId = :seriesId")
    suspend fun clearProgressForSeries(seriesId: String)
}
