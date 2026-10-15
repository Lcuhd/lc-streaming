package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade Room para persistência local de categorias e canais bloqueados manualmente no Controle dos Pais.
 */
@Entity(tableName = "parental_protection")
data class ParentalProtectionEntity(
    @PrimaryKey
    val id: String, // Formula: "${listId}_${protectionType}_${contentType}_${targetId}"
    val listId: String,
    val protectionType: String, // "CATEGORY" ou "CHANNEL"
    val contentType: String, // "LIVE", "VOD", "SERIES"
    val targetId: String, // categoryId ou streamId
    val createdAt: Long = System.currentTimeMillis()
)
