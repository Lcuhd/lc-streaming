package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.generator.DeviceIdentifierGenerator
import com.example.data.local.dao.DeviceInfoDao
import com.example.data.local.entity.DeviceInfoEntity
import com.example.domain.model.ActivationStatus
import com.example.domain.model.DeviceCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gerenciador centralizado de persistência de identidade do LC Player.
 *
 * Princípios inegociáveis:
 * 1. PERSISTÊNCIA DUAL:
 *    - SharedPreferences dedicado ("lc_player_identity"): imune a migrações e limpezas de banco Room.
 *    - Room Database ("device_info"): suporte a observação reativa de dados (Flow).
 * 2. REGRA DE OURO DA IDENTIDADE:
 *    - SE existir Device ID ou KEY em QUALQUER camada existente, SEMPRE reutilizar.
 *    - NUNCA sobrescrever valores existentes por cálculos determinísticos ou hashes novos.
 * 3. RECUPERAÇÃO / MIGRAÇÃO HISTÓRICA:
 *    - Varre todos os arquivos conhecidos de SharedPreferences e tabelas Room em busca de
 *      valores gravados em versões anteriores do LC Player.
 * 4. GERAÇÃO ESTREITAMENTE CONDICIONADA:
 *    - SOMENTE gera nova identidade se for a primeiríssima execução real em um aparelho totalmente limpo.
 *    - Após persistida, a identidade NUNCA mais é alterada por rotação, reabertura, troca de rede ou updates.
 */
class DeviceIdentityStorage(
    private val context: Context,
    private val deviceInfoDao: DeviceInfoDao
) {
    companion object {
        private const val TAG = "LCPlayer_Identity"

        // Arquivo canônico definitivo de preferências
        const val PREFS_FILE = "lc_player_identity"

        // Chaves canônicas definitivas
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_ACTIVATION_KEY = "activation_key"
        const val KEY_STATUS = "device_status"
        const val KEY_CREATED_AT = "created_at"
        const val KEY_LAST_CHECKED = "last_checked_at"

        // Nomes de arquivos históricos de preferências para busca e recuperação
        private val HISTORICAL_PREF_FILES = listOf(
            "lc_player_identity",
            "lc_player_prefs",
            "lc_device_preferences",
            "device_info",
            "device_credentials",
            "app_preferences"
        )

        // Chaves históricas para busca de Device ID
        private val HISTORICAL_DEVICE_ID_KEYS = listOf(
            "device_id",
            "deviceId",
            "lc_device_id",
            "DEVICE_ID",
            "mac_address",
            "device_mac"
        )

        // Chaves históricas para busca de KEY
        private val HISTORICAL_KEY_KEYS = listOf(
            "activation_key",
            "activationKey",
            "device_key",
            "lc_device_key",
            "key",
            "KEY"
        )
    }

    private val definitivePrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    }

    /**
     * Obtém ou inicializa a identidade única e definitiva do dispositivo.
     * NUNCA sobrescreve dados existentes.
     */
    suspend fun getOrInitializeCredentials(): DeviceCredentials = withContext(Dispatchers.IO) {
        // 1. Tenta carregar do SharedPreferences canônico
        var savedDeviceId = definitivePrefs.getString(KEY_DEVICE_ID, null)?.takeIf { it.isNotBlank() }
        var savedKey = definitivePrefs.getString(KEY_ACTIVATION_KEY, null)?.takeIf { it.isNotBlank() }
        var savedStatus = definitivePrefs.getString(KEY_STATUS, null)
        var savedCreatedAt = definitivePrefs.getLong(KEY_CREATED_AT, 0L).takeIf { it > 0 }
        var savedLastChecked = definitivePrefs.getLong(KEY_LAST_CHECKED, 0L).takeIf { it > 0 }

        // 2. Se faltar algum dado, consulta a base do Room
        val existingRoomEntity = deviceInfoDao.getDeviceInfoDirect()
        if (existingRoomEntity != null) {
            if (savedDeviceId.isNullOrBlank() && existingRoomEntity.deviceId.isNotBlank()) {
                savedDeviceId = existingRoomEntity.deviceId
                Log.i(TAG, "Device ID recuperado da base Room existente: $savedDeviceId")
            }
            if (savedKey.isNullOrBlank() && existingRoomEntity.activationKey.isNotBlank()) {
                savedKey = existingRoomEntity.activationKey
                Log.i(TAG, "KEY recuperada da base Room existente: $savedKey")
            }
            if (savedStatus.isNullOrBlank() && existingRoomEntity.status.isNotBlank()) {
                savedStatus = existingRoomEntity.status
            }
            if (savedCreatedAt == null && existingRoomEntity.createdAt > 0) {
                savedCreatedAt = existingRoomEntity.createdAt
            }
            if (savedLastChecked == null && existingRoomEntity.lastCheckedAt != null) {
                savedLastChecked = existingRoomEntity.lastCheckedAt
            }
        }

        // 3. Se ainda faltar Device ID ou KEY, faz varredura profunda em arquivos SharedPreferences históricos
        if (savedDeviceId.isNullOrBlank() || savedKey.isNullOrBlank()) {
            val allPrefFilesToScan = buildList {
                addAll(HISTORICAL_PREF_FILES)
                add("${context.packageName}_preferences")
                add(context.packageName)
            }.distinct()

            for (fileName in allPrefFilesToScan) {
                try {
                    val p = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
                    if (savedDeviceId.isNullOrBlank()) {
                        for (k in HISTORICAL_DEVICE_ID_KEYS) {
                            val v = p.getString(k, null)?.takeIf { it.isNotBlank() }
                            if (v != null) {
                                savedDeviceId = v
                                Log.i(TAG, "Device ID recuperado com sucesso do arquivo histórico '$fileName' chave '$k': $v")
                                break
                            }
                        }
                    }
                    if (savedKey.isNullOrBlank()) {
                        for (k in HISTORICAL_KEY_KEYS) {
                            val v = p.getString(k, null)?.takeIf { it.isNotBlank() }
                            if (v != null) {
                                savedKey = v
                                Log.i(TAG, "KEY recuperada com sucesso do arquivo histórico '$fileName' chave '$k': $v")
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Aviso ao varrer arquivo histórico '$fileName': ${e.message}")
                }

                if (!savedDeviceId.isNullOrBlank() && !savedKey.isNullOrBlank()) break
            }
        }

        // 4. Somente se NUNCA tiver existido em nenhuma camada (instalação absolutamente limpa), gera pela primeira vez
        val isFirstRunEver = savedDeviceId.isNullOrBlank() || savedKey.isNullOrBlank()
        val finalDeviceId = savedDeviceId ?: DeviceIdentifierGenerator.generateDeviceId(context)
        val finalKey = savedKey ?: DeviceIdentifierGenerator.generateKey(context)
        val finalStatus = savedStatus ?: ActivationStatus.WAITING_ACTIVATION.name
        val now = System.currentTimeMillis()
        val finalCreatedAt = savedCreatedAt ?: now
        val finalLastChecked = savedLastChecked ?: now

        if (isFirstRunEver) {
            Log.w(TAG, "Primeira execução detectada em aparelho limpo. Gerada nova identidade: Device ID='$finalDeviceId', KEY='$finalKey'")
        } else {
            Log.i(TAG, "Identidade pré-existente preservada com sucesso: Device ID='$finalDeviceId', KEY='$finalKey'")
        }

        // 5. Garante que AMBOS os armazenamentos (SharedPreferences e Room) estejam perfeitamente sincronizados
        saveToSharedPreferences(
            deviceId = finalDeviceId,
            key = finalKey,
            status = finalStatus,
            createdAt = finalCreatedAt,
            lastCheckedAt = finalLastChecked
        )

        val roomEntityToPersist = DeviceInfoEntity(
            id = 1,
            deviceId = finalDeviceId,
            activationKey = finalKey,
            status = finalStatus,
            createdAt = finalCreatedAt,
            lastCheckedAt = finalLastChecked
        )
        deviceInfoDao.insertOrUpdate(roomEntityToPersist)

        DeviceCredentials(
            deviceId = finalDeviceId,
            key = finalKey,
            status = ActivationStatus.fromStorage(finalStatus),
            createdAt = finalCreatedAt,
            lastCheckedAt = finalLastChecked
        )
    }

    /**
     * Atualiza o status de ativação em ambos os armazenamentos sincronizados.
     */
    suspend fun updateStatus(status: ActivationStatus, lastCheckedAt: Long) = withContext(Dispatchers.IO) {
        definitivePrefs.edit()
            .putString(KEY_STATUS, status.name)
            .putLong(KEY_LAST_CHECKED, lastCheckedAt)
            .apply()

        deviceInfoDao.updateStatus(status = status.name, lastCheckedAt = lastCheckedAt)
    }

    private fun saveToSharedPreferences(
        deviceId: String,
        key: String,
        status: String,
        createdAt: Long,
        lastCheckedAt: Long
    ) {
        definitivePrefs.edit()
            .putString(KEY_DEVICE_ID, deviceId)
            .putString(KEY_ACTIVATION_KEY, key)
            .putString(KEY_STATUS, status)
            .putLong(KEY_CREATED_AT, createdAt)
            .putLong(KEY_LAST_CHECKED, lastCheckedAt)
            .apply()
    }
}
