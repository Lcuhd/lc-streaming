package com.example.domain.model

/**
 * Modelos preparados para as próximas etapas do LC Player:
 * - Integração com API LC Admin
 * - Recebimento de DNS, Login, Senha e Validade
 * - Estruturas para Canais, Filmes e Séries
 */

data class LcAdminConnectionConfig(
    val serverUrl: String = "",
    val dnsHost: String = "",
    val username: String = "",
    val password: String = "",
    val expirationDateMillis: Long? = null,
    val maxConnections: Int = 1
)

enum class ContentType {
    LIVE_TV,
    MOVIE,
    SERIES
}

data class MediaCategoryStub(
    val id: String,
    val name: String,
    val type: ContentType
)

data class MediaStreamItemStub(
    val id: String,
    val title: String,
    val categoryId: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val type: ContentType
)
