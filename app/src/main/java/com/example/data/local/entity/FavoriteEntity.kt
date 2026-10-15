package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade Room para persistência local de referências de conteúdo favoritado pelo usuário.
 * Armazena exclusivamente referências estáveis (listId, contentType, contentId) sem duplicar objetos de mídia,
 * garantindo que atualizações de nome, capa e URL vindas do servidor Xtream sejam sempre refletidas.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val id: String, // Formula: "${listId}_${contentType}_${contentId}"
    val listId: String,
    val contentType: String, // "LIVE", "VOD", "SERIES"
    val contentId: String, // streamId ou seriesId
    val createdAt: Long = System.currentTimeMillis()
)
