package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade Room para persistência das credenciais e status de identificação do dispositivo.
 * Permanece idêntico entre aberturas do aplicativo.
 */
@Entity(tableName = "device_info")
data class DeviceInfoEntity(
    @PrimaryKey val id: Int = 1,
    val deviceId: String,
    val activationKey: String,
    val status: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastCheckedAt: Long? = null,
    
    // Campos preparados para as futuras integrações com LC Admin:
    val serverUrl: String? = null,
    val dnsHost: String? = null,
    val username: String? = null,
    val password: String? = null,
    val expirationDateMillis: Long? = null
)
