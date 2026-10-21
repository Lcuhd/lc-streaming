package com.example.data.remote

import android.os.SystemClock
import android.util.Log
import com.example.domain.model.VodDetails
import com.example.domain.model.VodDetailsLoadResult
import com.example.domain.model.VodMovie
import com.example.domain.model.VodStreamsLoadResult
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Cliente responsável por consultar os filmes reais (VOD streams) de uma categoria
 * no servidor compatível com Xtream Codes.
 *
 * Endpoint principal:
 * <DNS>/player_api.php?username=<USERNAME>&password=<PASSWORD>&action=get_vod_streams&category_id=<CATEGORY_ID>
 *
 * Estratégia resiliente:
 * 1. Tenta consultar diretamente com '&category_id=<CATEGORY_ID>'.
 * 2. Valida os filmes recebidos e filtra localmente por category_id.
 * 3. Se a consulta direta retornar vazia, executa fallback consultando 'action=get_vod_streams' (geral)
 *    e filtra localmente pela categoria requisitada.
 *
 * DIRETRIZES ESTRITAS DE SEGURANÇA:
 * - NUNCA expõe senhas, URLs completas ou credenciais em logs ou tela.
 * - Registra apenas host seguro, código HTTP, content-type, duração e quantidade de filmes.
 */
class XtreamVodStreamsClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .build()
) {

    companion object {
        private const val TAG = "LCPlayer_VodStreams"

        /**
         * Constrói a URL para consulta de filmes VOD (com category_id opcional).
         */
        fun buildVodStreamsUrl(
            dns: String,
            username: String,
            password: String,
            categoryId: String? = null
        ): String {
            val cleanDns = dns.trim().trimEnd('/')
            val encodedUser = URLEncoder.encode(username.trim(), "UTF-8")
            val encodedPass = URLEncoder.encode(password.trim(), "UTF-8")
            val base = "$cleanDns/player_api.php?username=$encodedUser&password=$encodedPass&action=get_vod_streams"
            return if (!categoryId.isNullOrBlank()) {
                val encodedCat = URLEncoder.encode(categoryId.trim(), "UTF-8")
                "$base&category_id=$encodedCat"
            } else {
                base
            }
        }

        /**
         * Constrói a URL para consulta dos detalhes completos de um filme (get_vod_info).
         */
        fun buildVodInfoUrl(
            dns: String,
            username: String,
            password: String,
            vodId: String
        ): String {
            val cleanDns = dns.trim().trimEnd('/')
            val encodedUser = URLEncoder.encode(username.trim(), "UTF-8")
            val encodedPass = URLEncoder.encode(password.trim(), "UTF-8")
            val encodedVod = URLEncoder.encode(vodId.trim(), "UTF-8")
            return "$cleanDns/player_api.php?username=$encodedUser&password=$encodedPass&action=get_vod_info&vod_id=$encodedVod"
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
     * Consulta os filmes da categoria informada.
     */
    fun fetchVodStreamsForCategory(
        dns: String,
        username: String,
        password: String,
        categoryId: String,
        categoryName: String
    ): VodStreamsLoadResult {
        if (dns.isBlank() || username.isBlank() || password.isBlank()) {
            return VodStreamsLoadResult.AuthError(
                displayMessage = "Credenciais não configuradas para esta lista",
                categoryId = categoryId
            )
        }

        val safeHost = extractSafeHost(dns)
        val startTime = SystemClock.elapsedRealtime()

        Log.i(
            TAG,
            "Iniciando busca de filmes: categoria='$categoryName' (ID=$categoryId), host=$safeHost"
        )

        // ETAPA 1: Consulta preferencial direta com category_id
        val directUrl = try {
            buildVodStreamsUrl(dns, username, password, categoryId)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao construir URL direta para VOD category_id=$categoryId", e)
            return VodStreamsLoadResult.NetworkError(
                displayMessage = "Não foi possível construir requisição para o servidor",
                categoryId = categoryId
            )
        }

        val directResult = executeVodRequest(directUrl, safeHost, isDirect = true)

        when (directResult) {
            is VodFetchResponse.Success -> {
                val durationMs = SystemClock.elapsedRealtime() - startTime
                val allParsed = directResult.movies

                // Filtra localmente para garantir isolamento estrito da categoria
                val filtered = allParsed.filter { movieMatchesCategory(it, categoryId) }

                Log.i(
                    TAG,
                    "Resposta VOD direta obtida em ${durationMs}ms: total recebido=${allParsed.size}, filmes correspondentes para '$categoryName'=${filtered.size}"
                )

                if (filtered.isNotEmpty()) {
                    return VodStreamsLoadResult.Success(
                        movies = filtered,
                        categoryId = categoryId,
                        filterStrategyUsed = if (allParsed.size == filtered.size) {
                            "API direta VOD por category_id"
                        } else {
                            "API direta VOD com filtragem local por category_id"
                        }
                    )
                }

                // Tenta fallback se consulta direta por query param retornar vazia
                Log.d(TAG, "Consulta direta VOD retornou 0 filmes. Tentando fallback (get_vod_streams geral)...")
                val fallbackResult = executeFallbackVod(dns, username, password, safeHost, categoryId, categoryName)
                if (fallbackResult != null) {
                    return fallbackResult
                }

                return VodStreamsLoadResult.Empty(
                    displayMessage = "Nenhum filme encontrado nesta categoria",
                    categoryId = categoryId,
                    filterStrategyUsed = "API direta VOD (0 filmes encontrados)"
                )
            }
            is VodFetchResponse.AuthError -> {
                return VodStreamsLoadResult.AuthError(
                    displayMessage = directResult.message,
                    categoryId = categoryId
                )
            }
            is VodFetchResponse.NetworkError -> {
                Log.w(TAG, "Consulta direta VOD falhou (${directResult.message}). Tentando fallback...")
                val fallbackResult = executeFallbackVod(dns, username, password, safeHost, categoryId, categoryName)
                if (fallbackResult != null) {
                    return fallbackResult
                }
                return VodStreamsLoadResult.NetworkError(
                    displayMessage = directResult.message,
                    categoryId = categoryId
                )
            }
        }
    }

    private fun executeFallbackVod(
        dns: String,
        username: String,
        password: String,
        safeHost: String,
        categoryId: String,
        categoryName: String
    ): VodStreamsLoadResult? {
        val fallbackUrl = try {
            buildVodStreamsUrl(dns, username, password, categoryId = null)
        } catch (_: Exception) {
            return null
        }

        val startTime = SystemClock.elapsedRealtime()
        val response = executeVodRequest(fallbackUrl, safeHost, isDirect = false)

        return when (response) {
            is VodFetchResponse.Success -> {
                val durationMs = SystemClock.elapsedRealtime() - startTime
                val matching = response.movies.filter { movieMatchesCategory(it, categoryId) }
                Log.i(
                    TAG,
                    "Fallback VOD geral concluído em ${durationMs}ms: total=${response.movies.size}, correspondentes para '$categoryName'=${matching.size}"
                )
                if (matching.isNotEmpty()) {
                    VodStreamsLoadResult.Success(
                        movies = matching,
                        categoryId = categoryId,
                        filterStrategyUsed = "Fallback get_vod_streams geral + filtro local"
                    )
                } else {
                    VodStreamsLoadResult.Empty(
                        displayMessage = "Nenhum filme encontrado nesta categoria",
                        categoryId = categoryId,
                        filterStrategyUsed = "Fallback get_vod_streams geral (0 filmes)"
                    )
                }
            }
            else -> null
        }
    }

    private fun movieMatchesCategory(movie: VodMovie, targetCategoryId: String): Boolean {
        if (targetCategoryId.isBlank()) return true
        if (movie.categoryId.isBlank()) return false
        return movie.categoryId.trim() == targetCategoryId.trim()
    }

    private fun executeVodRequest(
        url: String,
        safeHost: String,
        isDirect: Boolean
    ): VodFetchResponse {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "LCPlayer/1.0 (Android TV)")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                val contentType = response.header("Content-Type").orEmpty()
                val rawBody = response.body?.string().orEmpty().trim()

                Log.d(
                    TAG,
                    "Resposta HTTP VOD [${if (isDirect) "Direta" else "Fallback"}]: code=$code, tipo=$contentType, bytes=${rawBody.length}, host=$safeHost"
                )

                if (code == 401 || code == 403) {
                    return VodFetchResponse.AuthError("Não foi possível autenticar esta lista")
                }

                if (code in 500..599) {
                    return VodFetchResponse.NetworkError("O servidor de conteúdo retornou erro interno ($code)")
                }

                if (code != 200) {
                    return VodFetchResponse.NetworkError("Erro ao comunicar com o servidor ($code)")
                }

                parseVodJson(rawBody)
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Timeout ao consultar filmes VOD (host=$safeHost)", e)
            VodFetchResponse.NetworkError("O servidor demorou para responder")
        } catch (e: UnknownHostException) {
            Log.e(TAG, "DNS / sem conexão ao consultar filmes VOD (host=$safeHost)", e)
            VodFetchResponse.NetworkError("Sem conexão com a internet")
        } catch (e: ConnectException) {
            Log.e(TAG, "Conexão recusada ao consultar filmes VOD (host=$safeHost)", e)
            VodFetchResponse.NetworkError("Não foi possível conectar ao servidor de conteúdo")
        } catch (e: IOException) {
            Log.e(TAG, "Erro I/O ao consultar filmes VOD (host=$safeHost)", e)
            VodFetchResponse.NetworkError("Falha de rede ao carregar filmes")
        } catch (e: Exception) {
            Log.e(TAG, "Exceção inesperada ao consultar filmes VOD", e)
            VodFetchResponse.NetworkError("Erro inesperado ao consultar filmes")
        }
    }

    private fun parseVodJson(rawBody: String): VodFetchResponse {
        if (rawBody.isBlank()) {
            return VodFetchResponse.Success(emptyList())
        }

        val movies = mutableListOf<VodMovie>()

        try {
            if (rawBody.startsWith("[")) {
                val array = JSONArray(rawBody)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val movie = parseSingleMovieObject(obj)
                    if (movie != null) {
                        movies.add(movie)
                    }
                }
            } else if (rawBody.startsWith("{")) {
                val rootObj = JSONObject(rawBody)
                val keys = rootObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val nestedObj = rootObj.optJSONObject(key)
                    if (nestedObj != null) {
                        val movie = parseSingleMovieObject(nestedObj)
                        if (movie != null) {
                            movies.add(movie)
                        }
                    }
                }
            } else {
                Log.w(TAG, "Formato JSON de VOD não reconhecido: ${rawBody.take(100)}")
                return VodFetchResponse.NetworkError("Resposta do servidor em formato incompatível")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao interpretar JSON de filmes VOD", e)
            return VodFetchResponse.NetworkError("Erro ao processar dados dos filmes")
        }

        return VodFetchResponse.Success(movies)
    }

    private fun parseSingleMovieObject(obj: JSONObject): VodMovie? {
        val streamId = obj.optString("stream_id").ifBlank {
            obj.optString("id")
        }.trim()

        if (streamId.isBlank()) return null

        val name = obj.optString("name").ifBlank {
            obj.optString("title")
        }.trim().ifBlank {
            "Filme $streamId"
        }

        val rawIcon = obj.optString("stream_icon").ifBlank {
            obj.optString("cover")
        }.trim()
        val icon = if (rawIcon.isNotBlank() && rawIcon != "null") rawIcon else null

        val rating = obj.optString("rating").trim().ifBlank { null }
        val rating5based = if (obj.has("rating_5based")) {
            val r = obj.optDouble("rating_5based", -1.0)
            if (r >= 0) r else null
        } else null

        val containerExt = obj.optString("container_extension").ifBlank {
            obj.optString("extension")
        }.trim().removePrefix(".").ifBlank { "mp4" }

        val categoryId = obj.optString("category_id").trim()
        val added = obj.optString("added").trim().ifBlank { null }
        val directSource = obj.optString("direct_source").trim().ifBlank { null }

        return VodMovie(
            streamId = streamId,
            name = name,
            streamIcon = icon,
            rating = rating,
            rating5based = rating5based,
            containerExtension = containerExt,
            categoryId = categoryId,
            added = added,
            directSource = directSource
        )
    }

    private sealed class VodFetchResponse {
        data class Success(val movies: List<VodMovie>) : VodFetchResponse()
        data class AuthError(val message: String) : VodFetchResponse()
        data class NetworkError(val message: String) : VodFetchResponse()
    }

    /**
     * Consulta os detalhes completos de um filme VOD (action=get_vod_info).
     */
    fun fetchVodDetails(
        dns: String,
        username: String,
        password: String,
        vodId: String
    ): VodDetailsLoadResult {
        if (dns.isBlank() || username.isBlank() || password.isBlank() || vodId.isBlank()) {
            return VodDetailsLoadResult.Error("Credenciais ou identificador do filme inválidos")
        }

        val safeHost = extractSafeHost(dns)
        val startTime = SystemClock.elapsedRealtime()

        Log.i(TAG, "Iniciando consulta de detalhes do filme vod_id='$vodId', host=$safeHost")

        val url = try {
            buildVodInfoUrl(dns, username, password, vodId)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao construir URL para get_vod_info vod_id=$vodId", e)
            return VodDetailsLoadResult.Error("Não foi possível construir requisição para o servidor")
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

                Log.i(
                    TAG,
                    "Resposta get_vod_info obtida em ${durationMs}ms: HTTP $code, tamanho=${rawBody.length} bytes"
                )

                if (code == 401 || code == 403) {
                    return VodDetailsLoadResult.Error("Acesso não autorizado pelo servidor ($code)")
                }

                if (code in 500..599) {
                    return VodDetailsLoadResult.Error("O servidor de conteúdo retornou erro interno ($code)")
                }

                if (code != 200) {
                    return VodDetailsLoadResult.Error("Erro ao comunicar com o servidor ($code)")
                }

                if (rawBody.isBlank()) {
                    return VodDetailsLoadResult.Error("Servidor retornou resposta vazia")
                }

                parseVodDetailsJson(rawBody, vodId)
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Timeout ao consultar detalhes do filme (host=$safeHost)", e)
            VodDetailsLoadResult.Error("O servidor demorou para responder")
        } catch (e: UnknownHostException) {
            Log.e(TAG, "DNS / sem conexão ao consultar detalhes do filme (host=$safeHost)", e)
            VodDetailsLoadResult.Error("Sem conexão com a internet")
        } catch (e: ConnectException) {
            Log.e(TAG, "Conexão recusada ao consultar detalhes do filme (host=$safeHost)", e)
            VodDetailsLoadResult.Error("Não foi possível conectar ao servidor de conteúdo")
        } catch (e: IOException) {
            Log.e(TAG, "Erro I/O ao consultar detalhes do filme (host=$safeHost)", e)
            VodDetailsLoadResult.Error("Falha de rede ao carregar detalhes do filme")
        } catch (e: Exception) {
            Log.e(TAG, "Exceção inesperada ao consultar detalhes do filme", e)
            VodDetailsLoadResult.Error("Erro inesperado ao consultar detalhes do filme")
        }
    }

    private fun parseVodDetailsJson(
        rawBody: String,
        vodId: String
    ): VodDetailsLoadResult {
        return try {
            val rootObj = JSONObject(rawBody)
            val movieDataObj = rootObj.optJSONObject("movie_data")
            val infoObj = rootObj.optJSONObject("info") ?: movieDataObj ?: rootObj

            fun getSafeString(vararg keys: String): String? {
                val targets = listOfNotNull(infoObj, movieDataObj, rootObj)
                for (key in keys) {
                    for (target in targets) {
                        if (target.has(key) && !target.isNull(key)) {
                            val v = target.optString(key).trim()
                            if (v.isNotBlank() && v != "null") {
                                return v
                            }
                        }
                    }
                }
                return null
            }

            val title = getSafeString("name", "title") ?: "Filme $vodId"
            val movieImage = getSafeString("movie_image", "stream_icon", "cover")
            val plot = getSafeString("plot", "description")
            val cast = getSafeString("cast", "actors")
            val director = getSafeString("director")
            val genre = getSafeString("genre")
            val releaseDate = getSafeString("releaseDate", "release_date", "releasedate")
            val duration = getSafeString("duration", "duration_secs")
            val rating = getSafeString("rating")
            val ageRating = getSafeString("mpaa", "age_rating")
            val rawExt = getSafeString("container_extension", "containerExtension", "extension")?.removePrefix(".")

            fun parseBackdropFromObject(obj: JSONObject): String? {
                for (key in listOf("backdrop_path", "backdrop", "backdrops")) {
                    if (obj.has(key) && !obj.isNull(key)) {
                        val array = obj.optJSONArray(key)
                        if (array != null && array.length() > 0) {
                            val first = array.optString(0).trim()
                            if (first.isNotBlank() && first != "null") {
                                return first
                            }
                        }
                        val str = obj.optString(key).trim()
                        if (str.isNotBlank() && str != "null") {
                            return str
                        }
                    }
                }
                return null
            }

            val backdrop = parseBackdropFromObject(infoObj)
                ?: (movieDataObj?.let { parseBackdropFromObject(it) })
                ?: parseBackdropFromObject(rootObj)

            val details = VodDetails(
                streamId = vodId,
                title = title,
                movieImage = movieImage,
                backdrop = backdrop,
                plot = plot,
                cast = cast,
                director = director,
                genre = genre,
                releaseDate = releaseDate,
                duration = duration,
                rating = rating,
                ageRating = ageRating,
                containerExtension = rawExt
            )

            VodDetailsLoadResult.Success(details)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao interpretar JSON de get_vod_info para vod_id=$vodId", e)
            VodDetailsLoadResult.Error("Erro ao processar detalhes do filme")
        }
    }
}
