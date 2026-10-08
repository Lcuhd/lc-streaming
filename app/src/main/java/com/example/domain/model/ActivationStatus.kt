package com.example.domain.model

/**
 * Status de ativação do dispositivo no sistema LC Admin.
 *
 * Estados previstos na arquitetura conforme especificação LC Admin:
 * - WAITING_ACTIVATION ("Aguardando ativação")
 * - ACTIVE ("Dispositivo ativado")
 * - BLOCKED ("Dispositivo bloqueado")
 * - EXPIRED ("Acesso vencido")
 * - CONNECTION_ERROR ("Erro de conexão")
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
        title = "Dispositivo ativado",
        description = "Dispositivo ativado com sucesso no LC Admin."
    ),
    BLOCKED(
        title = "Dispositivo bloqueado",
        description = "Dispositivo bloqueado pelo administrador."
    ),
    EXPIRED(
        title = "Acesso vencido",
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
