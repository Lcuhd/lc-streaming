package com.example.domain.model

/**
 * Modelo de filme (VOD stream) retornado pela API Xtream Codes (action=get_vod_streams).
 */
data class VodMovie(
    val streamId: String,
    val name: String,
    val streamIcon: String? = null,
    val rating: String? = null,
    val rating5based: Double? = null,
    val containerExtension: String? = "mp4",
    val categoryId: String,
    val added: String? = null,
    val directSource: String? = null
)

/**
 * Resultado da consulta de filmes no servidor Xtream para uma categoria.
 */
sealed class VodStreamsLoadResult {
    data class Success(
        val movies: List<VodMovie>,
        val categoryId: String,
        val filterStrategyUsed: String
    ) : VodStreamsLoadResult()

    data class Empty(
        val displayMessage: String,
        val categoryId: String,
        val filterStrategyUsed: String
    ) : VodStreamsLoadResult()

    data class NetworkError(
        val displayMessage: String,
        val categoryId: String
    ) : VodStreamsLoadResult()

    data class AuthError(
        val displayMessage: String,
        val categoryId: String
    ) : VodStreamsLoadResult()

    data class Error(
        val displayMessage: String,
        val categoryId: String
    ) : VodStreamsLoadResult()
}

/**
 * Estado em memória dos filmes da categoria selecionada.
 */
data class VodStreamsState(
    val categoryId: String,
    val categoryName: String,
    val movies: List<VodMovie> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val strategyUsed: String = ""
)
