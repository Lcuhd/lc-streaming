package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ParentalProtectionEntity

/**
 * Interface DAO para persistência local de proteções do Controle dos Pais.
 */
@Dao
interface ParentalProtectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addProtection(entity: ParentalProtectionEntity)

    @Query("DELETE FROM parental_protection WHERE id = :id")
    suspend fun removeProtectionById(id: String)

    @Query("SELECT * FROM parental_protection WHERE listId = :listId")
    suspend fun getProtectionsForList(listId: String): List<ParentalProtectionEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM parental_protection WHERE id = :id LIMIT 1)")
    suspend fun isProtected(id: String): Boolean
}
