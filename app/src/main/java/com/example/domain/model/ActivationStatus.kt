package com.example.domain.model

/**
 * Status de ativação do dispositivo no sistema LC Admin.
 *
 * Estados previstos na arquitetura:
 * - WAITING_ACTIVATION (Aguardando ativação - status inicial padrão)
 * - ACTIVE (Ativo)
 * - BLOCKED (Bloqueado)
 * - EXPIRED (Vencido)
 * - CONNECTION_ERROR (Erro de conexão)
 */
enum class ActivationStatus(
    val title: String,
    val description: String
) {
    WAITING_ACTIVATION(
        title = "Aguardando ativação",
        description = "Cadastre este dispositivo no LC Admin para continuar."
    ),
    ACTIVE(
        title = "Ativo",
        description = "Dispositivo ativado com sucesso no LC Admin."
    ),
    BLOCKED(
        title = "Bloqueado",
        description = "Dispositivo bloqueado pelo administrador."
    ),
    EXPIRED(
        title = "Vencido",
        description = "Período de acesso expirado. Renove sua assinatura."
    ),
    CONNECTION_ERROR(
        title = "Erro de conexão",
        description = "Não foi possível conectar ao servidor LC Admin."
    );

    companion object {
        fun fromStorage(value: String?): ActivationStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: WAITING_ACTIVATION
        }
    }
}
