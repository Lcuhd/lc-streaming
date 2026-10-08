package com.example.domain.model

/**
 * Modelo de canal / stream ao vivo retornado pela API Xtream Codes (action=get_live_streams).
 */
data class LiveStreamChannel(
    val streamId: String,
    val name: String,
    val num: Int? = null,
    val streamIcon: String? = null,
    val categoryId: String,
    val epgChannelId: String? = null,
    val streamType: String? = "live"
)

/**
 * Resultado detalhado da consulta de canais no servidor Xtream para uma categoria.
 */
sealed class LiveStreamsLoadResult {
    data class Success(
        val channels: List<LiveStreamChannel>,
        val categoryId: String,
        val filterStrategyUsed: String
    ) : LiveStreamsLoadResult()

    data class Empty(
        val displayMessage: String,
        val categoryId: String,
        val filterStrategyUsed: String
    ) : LiveStreamsLoadResult()

    data class NetworkError(
        val displayMessage: String,
        val categoryId: String
    ) : LiveStreamsLoadResult()

    data class AuthError(
        val displayMessage: String,
        val categoryId: String
    ) : LiveStreamsLoadResult()

    data class Error(
        val displayMessage: String,
        val categoryId: String
    ) : LiveStreamsLoadResult()
}

/**
 * Estado em memória dos canais da categoria selecionada.
 */
data class LiveStreamsState(
    val categoryId: String,
    val categoryName: String,
    val channels: List<LiveStreamChannel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val strategyUsed: String = ""
)
