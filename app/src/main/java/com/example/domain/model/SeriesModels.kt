package com.example.domain.model

import com.example.data.local.entity.SeriesProgressEntity

/**
 * Modelo de série retornado pela API Xtream Codes (action=get_series).
 */
data class SeriesItem(
    val seriesId: String,
    val name: String,
    val cover: String? = null,
    val plot: String? = null,
    val rating: String? = null,
    val rating5based: Double? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val categoryId: String,
    val backdropPath: List<String> = emptyList(),
    val episodeRunTime: String? = null
)

/**
 * Resultado da consulta de séries no servidor Xtream para uma categoria.
 */
sealed class SeriesLoadResult {
    data class Success(
        val series: List<SeriesItem>,
        val categoryId: String,
        val filterStrategyUsed: String
    ) : SeriesLoadResult()

    data class Empty(
        val displayMessage: String,
        val categoryId: String,
        val filterStrategyUsed: String
    ) : SeriesLoadResult()

    data class NetworkError(
        val displayMessage: String,
        val categoryId: String
    ) : SeriesLoadResult()

    data class AuthError(
        val displayMessage: String,
        val categoryId: String
    ) : SeriesLoadResult()

    data class Error(
        val displayMessage: String,
        val categoryId: String
    ) : SeriesLoadResult()
}

/**
 * Estado em memória das séries da categoria selecionada.
 */
data class SeriesState(
    val categoryId: String,
    val categoryName: String,
    val seriesList: List<SeriesItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val strategyUsed: String = ""
)

/**
 * Modelo de temporada de uma série.
 */
data class SeasonItem(
    val seasonNumber: Int,
    val name: String,
    val episodeCount: Int = 0,
    val airDate: String? = null,
    val cover: String? = null
)

/**
 * Modelo de episódio de uma série (action=get_series_info).
 */
data class EpisodeItem(
    val id: String,
    val episodeNum: Int,
    val seasonNum: Int,
    val title: String,
    val containerExtension: String? = "mkv",
    val plot: String? = null,
    val image: String? = null,
    val duration: String? = null,
    val rating: String? = null,
    val releaseDate: String? = null
)

/**
 * Detalhes completos de uma série com temporadas e episódios (action=get_series_info).
 */
data class SeriesDetails(
    val seriesId: String,
    val title: String,
    val cover: String? = null,
    val plot: String? = null,
    val rating: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val seasons: List<SeasonItem> = emptyList(),
    val episodesBySeason: Map<Int, List<EpisodeItem>> = emptyMap()
)

/**
 * Resultado da consulta de detalhes de uma série.
 */
sealed class SeriesDetailsLoadResult {
    data class Success(val details: SeriesDetails) : SeriesDetailsLoadResult()
    data class Error(val displayMessage: String) : SeriesDetailsLoadResult()
}

/**
 * Estado do botão principal de ação da série ("Iniciar série" / "Continuar" / "Assistir novamente").
 */
sealed class SeriesButtonState {
    data class StartSeries(val firstEpisode: EpisodeItem) : SeriesButtonState()
    data class ContinueSeries(
        val targetEpisode: EpisodeItem,
        val targetSeasonNum: Int,
        val targetEpisodeNum: Int,
        val startPositionMs: Long
    ) : SeriesButtonState()
    data class RestartSeries(val firstEpisode: EpisodeItem) : SeriesButtonState()
    object NoEpisodesAvailable : SeriesButtonState()
}

/**
 * Estado em memória dos detalhes da série atualmente aberta.
 */
data class SeriesDetailsState(
    val seriesItem: SeriesItem,
    val details: SeriesDetails? = null,
    val selectedSeasonNum: Int? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val savedProgressList: List<SeriesProgressEntity> = emptyList()
) {
    val currentEpisodes: List<EpisodeItem>
        get() = details?.episodesBySeason?.get(selectedSeasonNum ?: details?.seasons?.firstOrNull()?.seasonNumber ?: 1) ?: emptyList()

    val primaryButtonState: SeriesButtonState
        get() = computePrimaryButtonState(details, savedProgressList)
}

/**
 * Calcula deterministicamente o estado do botão principal a partir dos detalhes e histórico.
 */
fun computePrimaryButtonState(
    details: SeriesDetails?,
    savedProgressList: List<SeriesProgressEntity>
): SeriesButtonState {
    if (details == null || details.seasons.isEmpty()) return SeriesButtonState.NoEpisodesAvailable

    // 1. Localiza o primeiro episódio disponível da primeira temporada válida
    val sortedSeasons = details.seasons.sortedBy { if (it.seasonNumber == 0) 999 else it.seasonNumber }
    var firstEpisode: EpisodeItem? = null
    for (s in sortedSeasons) {
        val eps = details.episodesBySeason[s.seasonNumber]?.sortedBy { it.episodeNum } ?: emptyList()
        if (eps.isNotEmpty()) {
            firstEpisode = eps.first()
            break
        }
    }

    if (firstEpisode == null) return SeriesButtonState.NoEpisodesAvailable

    // 2. Se nunca assistiu nenhuma parte desta série
    if (savedProgressList.isEmpty()) {
        return SeriesButtonState.StartSeries(firstEpisode)
    }

    // 3. Obtém o histórico de progresso mais recente
    val lastWatched = savedProgressList.maxByOrNull { it.updatedAt } ?: return SeriesButtonState.StartSeries(firstEpisode)

    // Se o episódio mais recente estiver INCOMPLETO (< 92% assistido)
    if (!lastWatched.completed) {
        val epIncomplete = findEpisodeById(details, lastWatched.episodeId)
        if (epIncomplete != null) {
            return SeriesButtonState.ContinueSeries(
                targetEpisode = epIncomplete,
                targetSeasonNum = lastWatched.seasonNum,
                targetEpisodeNum = lastWatched.episodeNum,
                startPositionMs = lastWatched.positionMs
            )
        }
    }

    // Se o episódio mais recente estiver CONCLUÍDO (>= 92% assistido ou fim da mídia)
    val nextEp = getNextEpisode(details, lastWatched.seasonNum, lastWatched.episodeNum)
    if (nextEp != null) {
        val nextProg = savedProgressList.firstOrNull { it.episodeId == nextEp.id }
        val startPos = if (nextProg != null && !nextProg.completed) nextProg.positionMs else 0L
        return SeriesButtonState.ContinueSeries(
            targetEpisode = nextEp,
            targetSeasonNum = nextEp.seasonNum,
            targetEpisodeNum = nextEp.episodeNum,
            startPositionMs = startPos
        )
    }

    // Se assistiu e concluiu o último episódio da última temporada
    return SeriesButtonState.RestartSeries(firstEpisode)
}

fun findEpisodeById(details: SeriesDetails, episodeId: String): EpisodeItem? {
    for ((_, epList) in details.episodesBySeason) {
        val found = epList.firstOrNull { it.id == episodeId }
        if (found != null) return found
    }
    return null
}

/**
 * Localiza o próximo episódio na mesma temporada ou na próxima temporada válida.
 */
fun getNextEpisode(details: SeriesDetails, currentSeasonNum: Int, currentEpisodeNum: Int): EpisodeItem? {
    // 1. Procura próximo episódio na mesma temporada
    val currentSeasonEpisodes = details.episodesBySeason[currentSeasonNum]?.sortedBy { it.episodeNum } ?: emptyList()
    val nextInSameSeason = currentSeasonEpisodes.firstOrNull { it.episodeNum > currentEpisodeNum }
    if (nextInSameSeason != null) {
        return nextInSameSeason
    }

    // 2. Se era o último episódio da temporada, procura a próxima temporada válida
    val sortedSeasons = details.seasons.sortedBy { if (it.seasonNumber == 0) 999 else it.seasonNumber }
    val currentSeasonIndex = sortedSeasons.indexOfFirst { it.seasonNumber == currentSeasonNum }

    if (currentSeasonIndex >= 0 && currentSeasonIndex < sortedSeasons.lastIndex) {
        for (i in (currentSeasonIndex + 1)..sortedSeasons.lastIndex) {
            val nextSeason = sortedSeasons[i]
            val nextSeasonEpisodes = details.episodesBySeason[nextSeason.seasonNumber]?.sortedBy { it.episodeNum } ?: emptyList()
            if (nextSeasonEpisodes.isNotEmpty()) {
                return nextSeasonEpisodes.first()
            }
        }
    }

    return null
}
