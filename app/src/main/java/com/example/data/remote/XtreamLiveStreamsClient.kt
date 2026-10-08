package com.example.data.remote

import android.os.SystemClock
import android.util.Log
import com.example.domain.model.LiveStreamChannel
import com.example.domain.model.LiveStreamsLoadResult
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
 * Cliente responsável por consultar os canais reais (live streams) de uma categoria
 * no servidor compatível com Xtream Codes.
 *
 * Endpoint principal:
 * <DNS>/player_api.php?username=<USERNAME>&password=<PASSWORD>&action=get_live_streams&category_id=<CATEGORY_ID>
 *
 * Estratégia resiliente:
 * 1. Tenta consultar diretamente com '&category_id=<CATEGORY_ID>'.
 * 2. Valida os canais recebidos e filtra localmente por category_id (alguns servidores ignoram o parâmetro e retornam tudo).
 * 3. Se a consulta direta retornar vazia ou falhar na interpretação de categoria, executa fallback
 *    consultando 'action=get_live_streams' (geral) e filtra localmente pela categoria requisitada.
 *
 * DIRETRIZES ESTRITAS DE SEGURANÇA:
 * - NUNCA expõe senhas, URLs completas ou credenciais em logs ou tela.
 * - Registra apenas host seguro, código HTTP, content-type, duração e quantidade de canais.
 */
class XtreamLiveStreamsClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .build()
) {

    companion object {
        private const val TAG = "LCPlayer_LiveStreams"

        /**
         * Constrói a URL para consulta de canais ao vivo (com category_id opcional).
         */
        fun buildLiveStreamsUrl(
            dns: String,
            username: String,
            password: String,
            categoryId: String? = null
        ): String {
            val cleanDns = dns.trim().trimEnd('/')
            val encodedUser = URLEncoder.encode(username.trim(), "UTF-8")
            val encodedPass = URLEncoder.encode(password.trim(), "UTF-8")
            val base = "$cleanDns/player_api.php?username=$encodedUser&password=$encodedPass&action=get_live_streams"
            return if (!categoryId.isNullOrBlank()) {
                val encodedCat = URLEncoder.encode(categoryId.trim(), "UTF-8")
                "$base&category_id=$encodedCat"
            } else {
                base
            }
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
     * Consulta os canais da categoria informada.
     */
    fun fetchLiveStreamsForCategory(
        dns: String,
        username: String,
        password: String,
        categoryId: String,
        categoryName: String
    ): LiveStreamsLoadResult {
        if (dns.isBlank() || username.isBlank() || password.isBlank()) {
            return LiveStreamsLoadResult.AuthError(
                displayMessage = "Credenciais não configuradas para esta lista",
                categoryId = categoryId
            )
        }

        val safeHost = extractSafeHost(dns)
        val startTime = SystemClock.elapsedRealtime()

        Log.i(
            TAG,
            "Iniciando busca de canais: categoria='$categoryName' (ID=$categoryId), host=$safeHost"
        )

        // ETAPA 1: Consulta preferencial direta com category_id
        val directUrl = try {
            buildLiveStreamsUrl(dns, username, password, categoryId)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao construir URL direta para category_id=$categoryId", e)
            return LiveStreamsLoadResult.NetworkError(
                displayMessage = "Não foi possível construir requisição para o servidor",
                categoryId = categoryId
            )
        }

        val directResult = executeStreamsRequest(directUrl, safeHost, categoryId, categoryName, isDirect = true)

        when (directResult) {
            is StreamsFetchResponse.Success -> {
                val durationMs = SystemClock.elapsedRealtime() - startTime
                val allParsed = directResult.channels

                // Alguns servidores retornam apenas os canais da categoria; outros ignoram o parâmetro e retornam todos.
                // Filtramos localmente para garantir isolamento estrito da categoria.
                val filtered = allParsed.filter { streamMatchesCategory(it, categoryId) }

                Log.i(
                    TAG,
                    "Resposta direta obtida em ${durationMs}ms: total recebido=${allParsed.size}, canais correspondentes à categoria '$categoryName'=${filtered.size}"
                )

                if (filtered.isNotEmpty()) {
                    return LiveStreamsLoadResult.Success(
                        channels = filtered,
                        categoryId = categoryId,
                        filterStrategyUsed = if (allParsed.size == filtered.size) {
                            "API direta por category_id"
                        } else {
                            "API direta com filtragem local por category_id"
                        }
                    )
                }

                // Se a consulta direta retornou vazia (array vazio) mas o servidor pode não aceitar o filtro por query param,
                // tentamos o fallback consultando get_live_streams geral e filtrando localmente.
                Log.d(TAG, "Consulta direta retornou 0 canais. Tentando estratégia de fallback (get_live_streams geral)...")
                val fallbackResult = executeFallbackStreams(dns, username, password, safeHost, categoryId, categoryName)
                if (fallbackResult != null) {
                    return fallbackResult
                }

                return LiveStreamsLoadResult.Empty(
                    displayMessage = "Nenhum canal encontrado nesta categoria",
                    categoryId = categoryId,
                    filterStrategyUsed = "API direta (0 canais encontrados)"
                )
            }
            is StreamsFetchResponse.AuthError -> {
                return LiveStreamsLoadResult.AuthError(
                    displayMessage = directResult.message,
                    categoryId = categoryId
                )
            }
            is StreamsFetchResponse.NetworkError -> {
                // Tenta fallback caso o erro tenha sido específico do parâmetro de URL (ex: 400 ou 404 pelo query param)
                Log.w(TAG, "Consulta direta falhou (${directResult.message}). Tentando fallback geral...")
                val fallbackResult = executeFallbackStreams(dns, username, password, safeHost, categoryId, categoryName)
                if (fallbackResult != null) {
                    return fallbackResult
                }
                return LiveStreamsLoadResult.NetworkError(
                    displayMessage = directResult.message,
                    categoryId = categoryId
                )
            }
        }
    }

    /**
     * Fallback: consulta todos os canais ao vivo (sem category_id na URL) e filtra localmente.
     */
    private fun executeFallbackStreams(
        dns: String,
        username: String,
        password: String,
        safeHost: String,
        categoryId: String,
        categoryName: String
    ): LiveStreamsLoadResult? {
        val fallbackUrl = try {
            buildLiveStreamsUrl(dns, username, password, categoryId = null)
        } catch (_: Exception) {
            return null
        }

        val startTime = SystemClock.elapsedRealtime()
        val response = executeStreamsRequest(fallbackUrl, safeHost, categoryId, categoryName, isDirect = false)

        return when (response) {
            is StreamsFetchResponse.Success -> {
                val durationMs = SystemClock.elapsedRealtime() - startTime
                val matching = response.channels.filter { streamMatchesCategory(it, categoryId) }
                Log.i(
                    TAG,
                    "Fallback geral concluído em ${durationMs}ms: total=${response.channels.size}, correspondentes para '$categoryName' (ID=$categoryId)=${matching.size}"
                )
                if (matching.isNotEmpty()) {
                    LiveStreamsLoadResult.Success(
                        channels = matching,
                        categoryId = categoryId,
                        filterStrategyUsed = "Fallback get_live_streams geral + filtro local por category_id"
                    )
                } else {
                    LiveStreamsLoadResult.Empty(
                        displayMessage = "Nenhum canal encontrado nesta categoria",
                        categoryId = categoryId,
                        filterStrategyUsed = "Fallback get_live_streams geral (0 canais da categoria)"
                    )
                }
            }
            else -> null
        }
    }

    private fun streamMatchesCategory(channel: LiveStreamChannel, targetCategoryId: String): Boolean {
        if (channel.categoryId.isBlank()) return false
        return channel.categoryId.trim() == targetCategoryId.trim()
    }

    private fun executeStreamsRequest(
        url: String,
        safeHost: String,
        categoryId: String,
        categoryName: String,
        isDirect: Boolean
    ): StreamsFetchResponse {
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
                    "Resposta HTTP [${if (isDirect) "Direta" else "Fallback"}]: code=$code, tipo=$contentType, bytes=${rawBody.length}, host=$safeHost"
                )

                if (code == 401 || code == 403) {
                    return StreamsFetchResponse.AuthError("Não foi possível autenticar esta lista")
                }

                if (code in 500..599) {
                    return StreamsFetchResponse.NetworkError("O servidor de conteúdo retornou erro interno ($code)")
                }

                if (code != 200) {
                    return StreamsFetchResponse.NetworkError("Erro ao comunicar com o servidor ($code)")
                }

                parseStreamsJson(rawBody)
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Timeout ao consultar canais (host=$safeHost)", e)
            StreamsFetchResponse.NetworkError("O servidor demorou para responder")
        } catch (e: UnknownHostException) {
            Log.e(TAG, "DNS / sem conexão ao consultar canais (host=$safeHost)", e)
            StreamsFetchResponse.NetworkError("Sem conexão com a internet")
        } catch (e: ConnectException) {
            Log.e(TAG, "Conexão recusada ao consultar canais (host=$safeHost)", e)
            StreamsFetchResponse.NetworkError("Não foi possível conectar ao servidor de conteúdo")
        } catch (e: IOException) {
            Log.e(TAG, "Erro I/O ao consultar canais (host=$safeHost)", e)
            StreamsFetchResponse.NetworkError("Falha de rede ao carregar canais")
        } catch (e: Exception) {
            Log.e(TAG, "Exceção inesperada ao consultar canais", e)
            StreamsFetchResponse.NetworkError("Erro inesperado ao consultar canais")
        }
    }

    private fun parseStreamsJson(rawBody: String): StreamsFetchResponse {
        if (rawBody.isBlank()) {
            return StreamsFetchResponse.Success(emptyList())
        }

        val streams = mutableListOf<LiveStreamChannel>()

        try {
            if (rawBody.startsWith("[")) {
                val array = JSONArray(rawBody)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val stream = parseSingleStreamObject(obj)
                    if (stream != null) {
                        streams.add(stream)
                    }
                }
            } else if (rawBody.startsWith("{")) {
                val rootObj = JSONObject(rawBody)
                // Alguns servidores retornam mapa { "stream_id": { ... } }
                val keys = rootObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val nestedObj = rootObj.optJSONObject(key)
                    if (nestedObj != null) {
                        val stream = parseSingleStreamObject(nestedObj)
                        if (stream != null) {
                            streams.add(stream)
                        }
                    }
                }
            } else {
                Log.w(TAG, "Formato JSON de streams não reconhecido: ${rawBody.take(100)}")
                return StreamsFetchResponse.NetworkError("Resposta do servidor em formato incompatível")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao interpretar JSON de canais", e)
            return StreamsFetchResponse.NetworkError("Erro ao processar dados dos canais")
        }

        return StreamsFetchResponse.Success(streams)
    }

    private fun parseSingleStreamObject(obj: JSONObject): LiveStreamChannel? {
        val streamId = obj.optString("stream_id").ifBlank {
            obj.optString("id")
        }.trim()

        if (streamId.isBlank()) return null

        val name = obj.optString("name").ifBlank {
            obj.optString("title")
        }.trim().ifBlank {
            "Canal $streamId"
        }

        val num = if (obj.has("num")) {
            val numVal = obj.optInt("num", -1)
            if (numVal >= 0) numVal else null
        } else null

        val rawIcon = obj.optString("stream_icon").trim()
        val icon = if (rawIcon.isNotBlank() && rawIcon != "null") rawIcon else null

        val categoryId = obj.optString("category_id").trim()
        val rawEpg = obj.optString("epg_channel_id").trim()
        val epg = if (rawEpg.isNotBlank() && rawEpg != "null") rawEpg else null
        val streamType = obj.optString("stream_type", "live")

        return LiveStreamChannel(
            streamId = streamId,
            name = name,
            num = num,
            streamIcon = icon,
            categoryId = categoryId,
            epgChannelId = epg,
            streamType = streamType
        )
    }

    private sealed class StreamsFetchResponse {
        data class Success(val channels: List<LiveStreamChannel>) : StreamsFetchResponse()
        data class AuthError(val message: String) : StreamsFetchResponse()
        data class NetworkError(val message: String) : StreamsFetchResponse()
    }
}
