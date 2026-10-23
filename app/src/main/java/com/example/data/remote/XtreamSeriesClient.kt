package com.example.data.remote

import android.os.SystemClock
import android.util.Log
import com.example.domain.model.EpisodeItem
import com.example.domain.model.SeasonItem
import com.example.domain.model.SeriesDetails
import com.example.domain.model.SeriesDetailsLoadResult
import com.example.domain.model.SeriesItem
import com.example.domain.model.SeriesLoadResult
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Cliente responsável por consultar a lista de séries (action=get_series)
 * e os detalhes das temporadas/episódios (action=get_series_info) no servidor Xtream Codes.
 *
 * DIRETRIZES ESTRITAS DE SEGURANÇA:
 * - NUNCA expõe senhas, URLs completas ou credenciais em logs ou tela.
 * - Registra apenas host seguro, código HTTP, series_id, quantidade de episódios e extensões.
 */
class XtreamSeriesClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .build()
) {

    companion object {
        private const val TAG = "LCPlayer_Series"

        /**
         * Constrói a URL para consulta de séries de uma categoria (get_series).
         */
        fun buildSeriesUrl(
            dns: String,
            username: String,
            password: String,
            categoryId: String? = null
        ): String {
            val cleanDns = dns.trim().trimEnd('/')
            val encodedUser = URLEncoder.encode(username.trim(), "UTF-8")
            val encodedPass = URLEncoder.encode(password.trim(), "UTF-8")
            val base = "$cleanDns/player_api.php?username=$encodedUser&password=$encodedPass&action=get_series"
            return if (!categoryId.isNullOrBlank()) {
                val encodedCat = URLEncoder.encode(categoryId.trim(), "UTF-8")
                "$base&category_id=$encodedCat"
            } else {
                base
            }
        }

        /**
         * Constrói a URL para detalhes da série (get_series_info).
         */
        fun buildSeriesInfoUrl(
            dns: String,
            username: String,
            password: String,
            seriesId: String
        ): String {
            val cleanDns = dns.trim().trimEnd('/')
            val encodedUser = URLEncoder.encode(username.trim(), "UTF-8")
            val encodedPass = URLEncoder.encode(password.trim(), "UTF-8")
            val encodedSeries = URLEncoder.encode(seriesId.trim(), "UTF-8")
            return "$cleanDns/player_api.php?username=$encodedUser&password=$encodedPass&action=get_series_info&series_id=$encodedSeries"
        }

        private fun extractSafeHost(dns: String): String {
            return try {
                dns.trim()
                    .removePrefix("http://")
                    .removePrefix("https://")
                    .substringBefore("/")
            } catch (_: Exception) {
                "servidor"
            }
        }
    }

    /**
     * Consulta as séries da categoria informada.
     */
    fun fetchSeriesForCategory(
        dns: String,
        username: String,
        password: String,
        categoryId: String,
        categoryName: String
    ): SeriesLoadResult {
        if (dns.isBlank() || username.isBlank() || password.isBlank()) {
            return SeriesLoadResult.AuthError(
                displayMessage = "Credenciais não configuradas para esta lista",
                categoryId = categoryId
            )
        }

        val safeHost = extractSafeHost(dns)
        val startTime = SystemClock.elapsedRealtime()

        Log.i(
            TAG,
            "Iniciando busca de séries: categoria='$categoryName' (ID=$categoryId), host=$safeHost"
        )

        // ETAPA 1: Consulta preferencial direta com category_id
        val directUrl = try {
            buildSeriesUrl(dns, username, password, categoryId)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao construir URL para get_series category_id=$categoryId", e)
            return SeriesLoadResult.NetworkError(
                displayMessage = "Não foi possível construir requisição para o servidor",
                categoryId = categoryId
            )
        }

        val directResult = executeSeriesListRequest(directUrl, safeHost, isDirect = true)

        when (directResult) {
            is SeriesFetchResponse.Success -> {
                val durationMs = SystemClock.elapsedRealtime() - startTime
                val allParsed = directResult.series

                val filtered = allParsed.filter { seriesMatchesCategory(it, categoryId) }

                Log.i(
                    TAG,
                    "Resposta get_series obtida em ${durationMs}ms: total=${allParsed.size}, séries da categoria '$categoryName'=${filtered.size}"
                )

                if (filtered.isNotEmpty()) {
                    return SeriesLoadResult.Success(
                        series = filtered,
                        categoryId = categoryId,
                        filterStrategyUsed = if (allParsed.size == filtered.size) {
                            "API direta get_series por category_id"
                        } else {
                            "API direta get_series com filtragem local"
                        }
                    )
                }

                // Tenta fallback se consulta direta por query param retornar vazia
                Log.d(TAG, "Consulta direta get_series retornou 0 séries. Tentando fallback...")
                val fallbackResult = executeFallbackSeries(dns, username, password, safeHost, categoryId, categoryName)
                if (fallbackResult != null) {
                    return fallbackResult
                }

                return SeriesLoadResult.Empty(
                    displayMessage = "Nenhuma série encontrada nesta categoria",
                    categoryId = categoryId,
                    filterStrategyUsed = "API direta get_series (0 séries encontradas)"
                )
            }
            is SeriesFetchResponse.AuthError -> {
                return SeriesLoadResult.AuthError(
                    displayMessage = directResult.message,
                    categoryId = categoryId
                )
            }
            is SeriesFetchResponse.NetworkError -> {
                Log.w(TAG, "Consulta direta get_series falhou (${directResult.message}). Tentando fallback...")
                val fallbackResult = executeFallbackSeries(dns, username, password, safeHost, categoryId, categoryName)
                if (fallbackResult != null) {
                    return fallbackResult
                }
                return SeriesLoadResult.NetworkError(
                    displayMessage = directResult.message,
                    categoryId = categoryId
                )
            }
        }
    }

    private fun executeFallbackSeries(
        dns: String,
        username: String,
        password: String,
        safeHost: String,
        categoryId: String,
        categoryName: String
    ): SeriesLoadResult? {
        val fallbackUrl = try {
            buildSeriesUrl(dns, username, password, categoryId = null)
        } catch (_: Exception) {
            return null
        }

        val startTime = SystemClock.elapsedRealtime()
        val response = executeSeriesListRequest(fallbackUrl, safeHost, isDirect = false)

        return when (response) {
            is SeriesFetchResponse.Success -> {
                val durationMs = SystemClock.elapsedRealtime() - startTime
                val matching = response.series.filter { seriesMatchesCategory(it, categoryId) }
                Log.i(
                    TAG,
                    "Fallback get_series geral concluído em ${durationMs}ms: total=${response.series.size}, correspondentes para '$categoryName'=${matching.size}"
                )
                if (matching.isNotEmpty()) {
                    SeriesLoadResult.Success(
                        series = matching,
                        categoryId = categoryId,
                        filterStrategyUsed = "Fallback get_series geral + filtro local"
                    )
                } else {
                    SeriesLoadResult.Empty(
                        displayMessage = "Nenhuma série encontrada nesta categoria",
                        categoryId = categoryId,
                        filterStrategyUsed = "Fallback get_series geral (0 séries)"
                    )
                }
            }
            else -> null
        }
    }

    private fun seriesMatchesCategory(series: SeriesItem, targetCategoryId: String): Boolean {
        if (targetCategoryId.isBlank()) return true
        if (series.categoryId.isBlank()) return false
        return series.categoryId.trim() == targetCategoryId.trim()
    }

    private fun executeSeriesListRequest(
        url: String,
        safeHost: String,
        isDirect: Boolean
    ): SeriesFetchResponse {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "LCPlayer/1.0 (Android TV)")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                val rawBody = response.body?.string().orEmpty().trim()

                Log.d(
                    TAG,
                    "Resposta HTTP get_series [${if (isDirect) "Direta" else "Fallback"}]: code=$code, bytes=${rawBody.length}, host=$safeHost"
                )

                if (code == 401 || code == 403) {
                    return SeriesFetchResponse.AuthError("Não foi possível autenticar esta lista")
                }

                if (code in 500..599) {
                    return SeriesFetchResponse.NetworkError("O servidor de conteúdo retornou erro interno ($code)")
                }

                if (code != 200) {
                    return SeriesFetchResponse.NetworkError("Erro ao comunicar com o servidor ($code)")
                }

                parseSeriesListJson(rawBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exceção ao consultar lista de séries (host=$safeHost)", e)
            SeriesFetchResponse.NetworkError("Falha ao comunicar com o servidor de séries")
        }
    }

    private fun parseSeriesListJson(rawBody: String): SeriesFetchResponse {
        if (rawBody.isBlank()) {
            return SeriesFetchResponse.Success(emptyList())
        }

        val list = mutableListOf<SeriesItem>()

        try {
            if (rawBody.startsWith("[")) {
                val array = JSONArray(rawBody)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val item = parseSingleSeriesObject(obj)
                    if (item != null) list.add(item)
                }
            } else if (rawBody.startsWith("{")) {
                val rootObj = JSONObject(rawBody)
                val keys = rootObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val nestedObj = rootObj.optJSONObject(key)
                    if (nestedObj != null) {
                        val item = parseSingleSeriesObject(nestedObj)
                        if (item != null) list.add(item)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao interpretar JSON de séries", e)
            return SeriesFetchResponse.NetworkError("Erro ao processar dados de séries")
        }

        return SeriesFetchResponse.Success(list)
    }

    private fun parseSingleSeriesObject(obj: JSONObject): SeriesItem? {
        val seriesId = obj.optString("series_id").ifBlank {
            obj.optString("id")
        }.trim()

        if (seriesId.isBlank()) return null

        val name = obj.optString("name").ifBlank {
            obj.optString("title")
        }.trim().ifBlank {
            "Série $seriesId"
        }

        val coverRaw = obj.optString("cover").ifBlank {
            obj.optString("stream_icon")
        }.trim()
        val cover = if (coverRaw.isNotBlank() && coverRaw != "null") coverRaw else null

        val plot = obj.optString("plot").trim().ifBlank { null }
        val rating = obj.optString("rating").trim().ifBlank { null }
        val rating5based = if (obj.has("rating_5based")) {
            val r = obj.optDouble("rating_5based", -1.0)
            if (r >= 0) r else null
        } else null

        val genre = obj.optString("genre").trim().ifBlank { null }
        val releaseDate = obj.optString("releaseDate").ifBlank {
            obj.optString("release_date")
        }.trim().ifBlank { null }

        val categoryId = obj.optString("category_id").trim()
        val runTime = obj.optString("episode_run_time").trim().ifBlank { null }

        val added = obj.optString("added").trim().ifBlank { null }
        val lastModified = obj.optString("last_modified").trim().ifBlank { null }

        return SeriesItem(
            seriesId = seriesId,
            name = name,
            cover = cover,
            plot = plot,
            rating = rating,
            rating5based = rating5based,
            genre = genre,
            releaseDate = releaseDate,
            categoryId = categoryId,
            episodeRunTime = runTime,
            added = added,
            lastModified = lastModified
        )
    }

    /**
     * Consulta os detalhes completos de uma série (get_series_info),
     * incluindo temporadas e episódios agrupados.
     */
    fun fetchSeriesDetails(
        dns: String,
        username: String,
        password: String,
        seriesId: String
    ): SeriesDetailsLoadResult {
        if (dns.isBlank() || username.isBlank() || password.isBlank() || seriesId.isBlank()) {
            return SeriesDetailsLoadResult.Error("Credenciais ou ID da série inválidos")
        }

        val safeHost = extractSafeHost(dns)
        val startTime = SystemClock.elapsedRealtime()

        Log.i(TAG, "Buscando detalhes da série: series_id=$seriesId, host=$safeHost")

        val url = try {
            buildSeriesInfoUrl(dns, username, password, seriesId)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao construir URL de get_series_info para series_id=$seriesId", e)
            return SeriesDetailsLoadResult.Error("Não foi possível criar requisição para os detalhes da série")
        }

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "LCPlayer/1.0 (Android TV)")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val durationMs = SystemClock.elapsedRealtime() - startTime
                val code = response.code
                val rawBody = response.body?.string().orEmpty().trim()

                Log.d(
                    TAG,
                    "Resposta HTTP get_series_info: code=$code, bytes=${rawBody.length}, duration=${durationMs}ms, host=$safeHost"
                )

                if (code != 200 || rawBody.isBlank()) {
                    return SeriesDetailsLoadResult.Error("Servidor retornou erro $code ao buscar detalhes da série")
                }

                parseSeriesDetailsJson(rawBody, seriesId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exceção ao consultar detalhes da série series_id=$seriesId", e)
            SeriesDetailsLoadResult.Error("Falha de comunicação ao carregar episódios da série")
        }
    }

    private fun parseSeriesDetailsJson(rawBody: String, seriesId: String): SeriesDetailsLoadResult {
        try {
            if (!rawBody.startsWith("{")) {
                return SeriesDetailsLoadResult.Error("Resposta de detalhes em formato incompatível")
            }

            val rootObj = JSONObject(rawBody)

            // 1. Informações básicas da Série
            val infoObj = rootObj.optJSONObject("info")
            val title = infoObj?.optString("name")?.ifBlank { infoObj.optString("title") }?.trim()
                ?.ifBlank { "Série $seriesId" } ?: "Série $seriesId"

            val coverRaw = infoObj?.optString("cover")?.ifBlank { infoObj.optString("stream_icon") }?.trim().orEmpty()
            val cover = if (coverRaw.isNotBlank() && coverRaw != "null") coverRaw else null

            val plot = infoObj?.optString("plot")?.trim()?.ifBlank { null }
            val rating = infoObj?.optString("rating")?.trim()?.ifBlank { null }
            val genre = infoObj?.optString("genre")?.trim()?.ifBlank { null }
            val releaseDate = infoObj?.optString("releaseDate")?.ifBlank { infoObj.optString("release_date") }?.trim()?.ifBlank { null }

            // 2. Mapeamento de Episódios por Temporada
            val episodesBySeason = mutableMapOf<Int, MutableList<EpisodeItem>>()

            val episodesObj = rootObj.optJSONObject("episodes")
            if (episodesObj != null) {
                val keys = episodesObj.keys()
                while (keys.hasNext()) {
                    val seasonKeyStr = keys.next()
                    val seasonNum = seasonKeyStr.toIntOrNull() ?: continue
                    val episodeArray = episodesObj.optJSONArray(seasonKeyStr) ?: continue

                    val listForSeason = episodesBySeason.getOrPut(seasonNum) { mutableListOf() }
                    for (i in 0 until episodeArray.length()) {
                        val epObj = episodeArray.optJSONObject(i) ?: continue
                        val item = parseSingleEpisodeObject(epObj, seasonNum)
                        if (item != null) {
                            listForSeason.add(item)
                        }
                    }
                }
            } else {
                // Algumas respostas retornam episodes como JSONArray [ { "season": 1, ... } ]
                val episodesArray = rootObj.optJSONArray("episodes")
                if (episodesArray != null) {
                    for (i in 0 until episodesArray.length()) {
                        val epObj = episodesArray.optJSONObject(i) ?: continue
                        val sNum = epObj.optInt("season", epObj.optInt("season_num", 1))
                        val item = parseSingleEpisodeObject(epObj, sNum)
                        if (item != null) {
                            episodesBySeason.getOrPut(sNum) { mutableListOf() }.add(item)
                        }
                    }
                }
            }

            // 3. Temporadas (da chave "seasons" ou geradas dinamicamente das chaves de episódios)
            val seasonsList = mutableListOf<SeasonItem>()
            val seasonsArray = rootObj.optJSONArray("seasons")

            if (seasonsArray != null && seasonsArray.length() > 0) {
                for (i in 0 until seasonsArray.length()) {
                    val sObj = seasonsArray.optJSONObject(i) ?: continue
                    val sNum = sObj.optInt("season_number", sObj.optInt("season", i + 1))
                    val sName = sObj.optString("name").ifBlank {
                        if (sNum == 0) "Especial" else "Temporada $sNum"
                    }
                    val sCount = sObj.optInt("episode_count", episodesBySeason[sNum]?.size ?: 0)
                    val sCoverRaw = sObj.optString("cover").trim()
                    val sCover = if (sCoverRaw.isNotBlank() && sCoverRaw != "null") sCoverRaw else cover

                    seasonsList.add(
                        SeasonItem(
                            seasonNumber = sNum,
                            name = sName,
                            episodeCount = sCount,
                            airDate = sObj.optString("air_date").ifBlank { null },
                            cover = sCover
                        )
                    )
                }
            }

            // Se o objeto 'seasons' veio vazio ou ausente, constrói dinamicamente a partir das temporadas encontradas nos episódios
            if (seasonsList.isEmpty() && episodesBySeason.isNotEmpty()) {
                val sortedSeasonNums = episodesBySeason.keys.sorted()
                for (sNum in sortedSeasonNums) {
                    val epCount = episodesBySeason[sNum]?.size ?: 0
                    seasonsList.add(
                        SeasonItem(
                            seasonNumber = sNum,
                            name = if (sNum == 0) "Especial" else "Temporada $sNum",
                            episodeCount = epCount,
                            cover = cover
                        )
                    )
                }
            }

            // Ordena temporadas numericamente (1, 2, 3... e 0 ao final ou no início)
            seasonsList.sortBy { if (it.seasonNumber == 0) 999 else it.seasonNumber }

            val totalEpCount = episodesBySeason.values.sumOf { it.size }
            Log.i(
                TAG,
                "Detalhes da série 'title' carregados: ${seasonsList.size} temporadas, $totalEpCount episódios no total"
            )

            return SeriesDetailsLoadResult.Success(
                SeriesDetails(
                    seriesId = seriesId,
                    title = title,
                    cover = cover,
                    plot = plot,
                    rating = rating,
                    genre = genre,
                    releaseDate = releaseDate,
                    seasons = seasonsList,
                    episodesBySeason = episodesBySeason
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao interpretar JSON de get_series_info para series_id=$seriesId", e)
            return SeriesDetailsLoadResult.Error("Erro ao processar episódios da série")
        }
    }

    private fun parseSingleEpisodeObject(obj: JSONObject, seasonNum: Int): EpisodeItem? {
        val episodeId = obj.optString("id").ifBlank {
            obj.optString("episode_id")
        }.trim()

        if (episodeId.isBlank()) return null

        val episodeNum = obj.optInt("episode_num", obj.optInt("episode", 1))

        val titleRaw = obj.optString("title").ifBlank {
            obj.optString("name")
        }.trim()
        val title = if (titleRaw.isNotBlank()) titleRaw else "Episódio $episodeNum"

        val containerExt = obj.optString("container_extension").ifBlank {
            obj.optString("extension")
        }.trim().removePrefix(".").ifBlank { "mkv" }

        // Extrai metadados adicionais do episódio (que às vezes vêm aninhados em 'info')
        val infoObj = obj.optJSONObject("info")
        val plot = infoObj?.optString("plot")?.ifBlank { obj.optString("plot") }?.trim()?.ifBlank { null }
        val imageRaw = infoObj?.optString("movie_image")?.ifBlank {
            infoObj.optString("image")
        }?.ifBlank { obj.optString("movie_image") }?.trim().orEmpty()
        val image = if (imageRaw.isNotBlank() && imageRaw != "null") imageRaw else null

        val duration = infoObj?.optString("duration")?.ifBlank { obj.optString("duration") }?.trim()?.ifBlank { null }
        val rating = infoObj?.optString("rating")?.ifBlank { obj.optString("rating") }?.trim()?.ifBlank { null }
        val releaseDate = infoObj?.optString("releasedate")?.ifBlank {
            infoObj.optString("release_date")
        }?.ifBlank { obj.optString("releaseDate") }?.trim()?.ifBlank { null }

        return EpisodeItem(
            id = episodeId,
            episodeNum = episodeNum,
            seasonNum = seasonNum,
            title = title,
            containerExtension = containerExt,
            plot = plot,
            image = image,
            duration = duration,
            rating = rating,
            releaseDate = releaseDate
        )
    }

    private sealed class SeriesFetchResponse {
        data class Success(val series: List<SeriesItem>) : SeriesFetchResponse()
        data class AuthError(val message: String) : SeriesFetchResponse()
        data class NetworkError(val message: String) : SeriesFetchResponse()
    }
}
