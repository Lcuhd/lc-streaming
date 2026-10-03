package com.example.domain.model

/**
 * Representa as credenciais de identificação do dispositivo no LC Player.
 *
 * @property deviceId Identificador único no formato visual AA:BB:CC:DD:EE:FF
 * @property key Chave numérica/alfanumérico curta para identificação (ex: 728491)
 * @property status Status atual do dispositivo
 * @property createdAt Timestamp de geração/primeira inicialização
 * @property lastCheckedAt Timestamp da última verificação de status
 */
data class DeviceCredentials(
    val deviceId: String,
    val key: String,
    val status: ActivationStatus = ActivationStatus.WAITING_ACTIVATION,
    val createdAt: Long = System.currentTimeMillis(),
    val lastCheckedAt: Long? = null
)
