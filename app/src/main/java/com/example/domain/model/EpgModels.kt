package com.example.domain.model

/**
 * Modelo de programa de TV retornado pela API EPG do Xtream Codes.
 */
data class EpgProgram(
    val id: String? = null,
    val title: String,
    val description: String? = null,
    val startTimeFormatted: String? = null,
    val endTimeFormatted: String? = null,
    val startTimestamp: Long? = null,
    val endTimestamp: Long? = null,
    val isNowPlaying: Boolean = false
) {
    val timeRangeFormatted: String
        get() = when {
            !startTimeFormatted.isNullOrBlank() && !endTimeFormatted.isNullOrBlank() ->
                "$startTimeFormatted - $endTimeFormatted"
            !startTimeFormatted.isNullOrBlank() ->
                startTimeFormatted
            else -> ""
        }
}

/**
 * Estado resumido de EPG para um canal de TV ao vivo (programa atual e próximo).
 */
data class ShortEpgState(
    val streamId: String,
    val currentProgram: EpgProgram? = null,
    val nextProgram: EpgProgram? = null,
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val errorMessage: String? = null
) {
    val hasEpg: Boolean
        get() = currentProgram != null || nextProgram != null
}

/**
 * Resultado da consulta de EPG para um canal especifico.
 */
sealed class ShortEpgFetchResult {
    data class Success(
        val streamId: String,
        val currentProgram: EpgProgram?,
        val nextProgram: EpgProgram?
    ) : ShortEpgFetchResult()

    data class Error(
        val streamId: String,
        val message: String
    ) : ShortEpgFetchResult()
}
