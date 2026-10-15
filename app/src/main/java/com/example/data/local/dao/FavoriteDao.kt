package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interface DAO para persistência local de referências de favoritos.
 */
@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun removeFavoriteById(id: String)

    @Query("SELECT * FROM favorites WHERE listId = :listId AND contentType = :contentType ORDER BY createdAt DESC")
    suspend fun getFavorites(listId: String, contentType: String): List<FavoriteEntity>

    @Query("SELECT * FROM favorites WHERE listId = :listId AND contentType = :contentType ORDER BY createdAt DESC")
    fun getFavoritesFlow(listId: String, contentType: String): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id LIMIT 1)")
    suspend fun isFavorite(id: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id LIMIT 1)")
    fun isFavoriteFlow(id: String): Flow<Boolean>
}
