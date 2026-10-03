package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.DeviceInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceInfoDao {

    @Query("SELECT * FROM device_info WHERE id = 1 LIMIT 1")
    fun getDeviceInfoFlow(): Flow<DeviceInfoEntity?>

    @Query("SELECT * FROM device_info WHERE id = 1 LIMIT 1")
    suspend fun getDeviceInfoDirect(): DeviceInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(deviceInfo: DeviceInfoEntity)

    @Query("UPDATE device_info SET status = :status, lastCheckedAt = :lastCheckedAt WHERE id = 1")
    suspend fun updateStatus(status: String, lastCheckedAt: Long)
}
