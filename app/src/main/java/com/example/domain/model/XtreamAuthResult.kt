package com.example.domain.model

/**
 * Representa o resultado da tentativa de autenticação direta com o servidor Xtream.
 */
sealed class XtreamAuthResult {

    data class Success(
        val displayMessage: String = "Servidor conectado",
        val accountStatus: String = "Ativa",
        val expiration: String? = null,
        val maxConnections: String? = null
    ) : XtreamAuthResult()

    data class InvalidCredentials(
        val displayMessage: String = "Usuário ou senha inválidos"
    ) : XtreamAuthResult()

    data class Unavailable(
        val displayMessage: String = "Não foi possível conectar ao servidor"
    ) : XtreamAuthResult()

    data class IncompatibleResponse(
        val displayMessage: String = "Resposta do servidor não reconhecida"
    ) : XtreamAuthResult()

    data class NotAllowed(
        val displayMessage: String
    ) : XtreamAuthResult()
}

/**
 * Modelo de apresentação para o diálogo de resultado do teste de conexão.
 */
data class ConnectionTestDialogState(
    val listTitle: String,
    val result: XtreamAuthResult
)
