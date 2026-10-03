package com.example.data.repository

import android.content.Context
import com.example.data.generator.DeviceIdentifierGenerator
import com.example.data.local.dao.DeviceInfoDao
import com.example.data.local.entity.DeviceInfoEntity
import com.example.domain.model.ActivationStatus
import com.example.domain.model.DeviceCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repositório responsável por gerenciar a identidade e persistência do dispositivo.
 */
class DeviceRepository(
    private val context: Context,
    private val deviceInfoDao: DeviceInfoDao
) {

    /**
     * Flow observável das credenciais do dispositivo.
     */
    val deviceCredentialsFlow: Flow<DeviceCredentials?> = deviceInfoDao.getDeviceInfoFlow()
        .map { entity ->
            entity?.let {
                DeviceCredentials(
                    deviceId = it.deviceId,
                    key = it.activationKey,
                    status = ActivationStatus.fromStorage(it.status),
                    createdAt = it.createdAt,
                    lastCheckedAt = it.lastCheckedAt
                )
            }
        }

    /**
     * Inicializa as credenciais na primeira abertura se ainda não existirem.
     * Caso já existam no banco local, mantém exatamente os mesmos dados persistidos.
     */
    suspend fun getOrCreateDeviceCredentials(): DeviceCredentials = withContext(Dispatchers.IO) {
        val existing = deviceInfoDao.getDeviceInfoDirect()
        if (existing != null) {
            DeviceCredentials(
                deviceId = existing.deviceId,
                key = existing.activationKey,
                status = ActivationStatus.fromStorage(existing.status),
                createdAt = existing.createdAt,
                lastCheckedAt = existing.lastCheckedAt
            )
        } else {
            // Primeira inicialização: gera novos identificadores exclusivos
            val newDeviceId = DeviceIdentifierGenerator.generateDeviceId(context)
            val newKey = DeviceIdentifierGenerator.generateKey()
            val initialStatus = ActivationStatus.WAITING_ACTIVATION

            val newEntity = DeviceInfoEntity(
                id = 1,
                deviceId = newDeviceId,
                activationKey = newKey,
                status = initialStatus.name,
                createdAt = System.currentTimeMillis(),
                lastCheckedAt = System.currentTimeMillis()
            )

            deviceInfoDao.insertOrUpdate(newEntity)

            DeviceCredentials(
                deviceId = newDeviceId,
                key = newKey,
                status = initialStatus,
                createdAt = newEntity.createdAt,
                lastCheckedAt = newEntity.lastCheckedAt
            )
        }
    }

    /**
     * Atualiza o timestamp de verificação local.
     * Preparado para quando a API LC Admin for integrada na próxima etapa.
     */
    suspend fun refreshLocalStatus() = withContext(Dispatchers.IO) {
        val current = deviceInfoDao.getDeviceInfoDirect() ?: return@withContext
        val now = System.currentTimeMillis()
        deviceInfoDao.updateStatus(
            status = current.status,
            lastCheckedAt = now
        )
    }
}
