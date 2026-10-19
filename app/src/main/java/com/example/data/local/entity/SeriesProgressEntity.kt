package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade Room para persistência local do progresso de reprodução de episódios de séries.
 */
@Entity(tableName = "series_progress")
data class SeriesProgressEntity(
    val episodeId: String,
    val seriesId: String,
    val seasonNum: Int,
    val episodeNum: Int,
    val positionMs: Long,
    val durationMs: Long,
    val completed: Boolean,
    val updatedAt: Long = System.currentTimeMillis(),

    @PrimaryKey
    val progressKey: String = episodeId,
    val listId: String? = null,
    val contentType: String? = null,
    val contentId: String? = null,
    val contentTitle: String? = null,
    val contentImage: String? = null
)
