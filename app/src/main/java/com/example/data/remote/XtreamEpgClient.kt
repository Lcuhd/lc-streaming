package com.example.data.remote

import android.os.SystemClock
import android.util.Base64
import android.util.Log
import com.example.domain.model.EpgProgram
import com.example.domain.model.ShortEpgFetchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP responsável por consultar o EPG curto (get_short_epg) de um canal de TV ao vivo
 * no servidor Xtream Codes.
 *
 * Endpoint:
 * <DNS>/player_api.php?username=<USER>&password=<PASS>&action=get_short_epg&stream_id=<STREAM_ID>&limit=2
 *
 * REQUISITOS DE SEGURANÇA E DIAGNÓSTICO:
 * - NUNCA exibe username, password ou URL completa em logs.
 * - Registra apenas host seguro, stream_id, status HTTP, tamanho e chaves de JSON.
 */
class XtreamEpgClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .build()
) {

    companion object {
        private const val TAG = "LCPlayer_EPG"

        /**
         * Constrói a URL para a requisição get_short_epg.
         */
        fun buildShortEpgUrl(
            dns: String,
            username: String,
            password: String,
            streamId: String,
            limit: Int = 2
        ): String {
            val cleanDns = dns.trim().trimEnd('/')
            val encodedUser = URLEncoder.encode(username.trim(), "UTF-8")
            val encodedPass = URLEncoder.encode(password.trim(), "UTF-8")
            val encodedStream = URLEncoder.encode(streamId.trim(), "UTF-8")
            return "$cleanDns/player_api.php?username=$encodedUser&password=$encodedPass&action=get_short_epg&stream_id=$encodedStream&limit=$limit"
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

        /**
         * Decodifica Base64 se a string for um texto codificado em Base64 válido.
         * Retorna o texto original caso não seja Base64 ou se a decodificação falhar.
         */
        fun decodeBase64IfNeeded(raw: String?): String {
            if (raw.isNullOrBlank()) return ""
            val trimmed = raw.trim()
            if (trimmed.length >= 4 && trimmed.length % 4 == 0 && !trimmed.contains(" ") && trimmed.matches(Regex("^[A-Za-z0-9+/=]+$"))) {
                try {
                    val bytes = Base64.decode(trimmed, Base64.DEFAULT)
                    val decoded = String(bytes, Charsets.UTF_8).trim()
                    if (decoded.isNotBlank() && decoded.all { it >= ' ' || it == '\n' || it == '\r' || it == '\t' }) {
                        return decoded
                    }
                } catch (_: Exception) {
                }
            }
            return trimmed
        }

        /**
         * Formata um Unix Timestamp (em segundos UTC) para o fuso horário local do aparelho (ex: "14:30").
         */
        fun formatTimestampToLocalTime(timestampSeconds: Long?): String? {
            if (timestampSeconds == null || timestampSeconds <= 0) return null
            return try {
                val date = Date(timestampSeconds * 1000L)
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                sdf.timeZone = TimeZone.getDefault()
                sdf.format(date)
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Converte strings de data no formato do servidor para o fuso/horário local "HH:mm".
         */
        fun parseServerDateToLocalTime(rawDate: String?): String? {
            if (rawDate.isNullOrBlank()) return null
            val trimmed = rawDate.trim()

            // Formatos comuns de data em servidores Xtream
            val formats = listOf(
                "yyyy-MM-dd HH:mm:ss Z",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss"
            )

            for (fmt in formats) {
                try {
                    val parser = SimpleDateFormat(fmt, Locale.US)
                    if (!fmt.contains("Z")) {
                        parser.timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val date = parser.parse(trimmed)
                    if (date != null) {
                        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                        formatter.timeZone = TimeZone.getDefault()
                        return formatter.format(date)
                    }
                } catch (_: Exception) {
                }
            }

            // Se a string já tiver formato "HH:mm" ou "HH:mm:ss"
            if (trimmed.contains(":") && trimmed.length in 5..8) {
                return trimmed.take(5)
            }

            return null
        }
    }

    /**
     * Busca o EPG curto para o stream_id informado.
     */
    suspend fun fetchShortEpg(
        dns: String,
        username: String,
        password: String,
        streamId: String
    ): ShortEpgFetchResult = withContext(Dispatchers.IO) {
        if (dns.isBlank() || username.isBlank() || password.isBlank() || streamId.isBlank()) {
            return@withContext ShortEpgFetchResult.Error(
                streamId = streamId,
                message = "Parâmetros inválidos para consulta de EPG"
            )
        }

        val safeHost = extractSafeHost(dns)
        val safeEndpoint = "http://$safeHost/player_api.php?action=get_short_epg&stream_id=$streamId&limit=2"
        val startTime = SystemClock.elapsedRealtime()

        Log.i(
            TAG,
            "[DIAGNOSTICO EPG] Iniciando chamada: stream_id=$streamId, endpoint_seguro=$safeEndpoint"
        )

        val url = try {
            buildShortEpgUrl(dns, username, password, streamId, limit = 2)
        } catch (e: Exception) {
            Log.e(TAG, "[DIAGNOSTICO EPG] Falha ao construir URL de EPG para stream_id=$streamId", e)
            return@withContext ShortEpgFetchResult.Error(streamId, "Erro ao criar requisição EPG")
        }

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "LCPlayer/1.0 (Android TV)")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val durationMs = SystemClock.elapsedRealtime() - startTime
                val code = response.code
                val rawBody = response.body?.string().orEmpty().trim()

                Log.i(
                    TAG,
                    "[DIAGNOSTICO EPG] Resposta recebida: stream_id=$streamId, status_http=$code, tamanho_bytes=${rawBody.length}, duracao=${durationMs}ms"
                )

                if (code != 200) {
                    Log.w(
                        TAG,
                        "[DIAGNOSTICO EPG] Servidor retornou código HTTP não-200 ($code) para stream_id=$streamId"
                    )
                    return@withContext ShortEpgFetchResult.Error(
                        streamId = streamId,
                        message = "Servidor retornou HTTP $code"
                    )
                }

                val parseResult = parseEpgJson(rawBody, streamId)

                // Se get_short_epg retornou sem programas, executa teste diagnóstico complementar com get_simple_data_table
                if (parseResult is ShortEpgFetchResult.Success && parseResult.currentProgram == null) {
                    Log.i(
                        TAG,
                        "[DIAGNOSTICO EPG] get_short_epg retornou 0 programas para stream_id=$streamId. Testando action=get_simple_data_table..."
                    )
                    testSimpleDataTable(dns, username, password, streamId)
                }

                parseResult
            }
        } catch (e: SocketTimeoutException) {
            Log.w(TAG, "[DIAGNOSTICO EPG] Timeout ao consultar EPG (host=$safeHost, stream_id=$streamId)")
            ShortEpgFetchResult.Error(streamId, "Timeout na consulta EPG")
        } catch (e: UnknownHostException) {
            Log.w(TAG, "[DIAGNOSTICO EPG] Host não encontrado ao consultar EPG (host=$safeHost, stream_id=$streamId)")
            ShortEpgFetchResult.Error(streamId, "Sem conexão para EPG")
        } catch (e: ConnectException) {
            Log.w(TAG, "[DIAGNOSTICO EPG] Conexão recusada para EPG (host=$safeHost, stream_id=$streamId)")
            ShortEpgFetchResult.Error(streamId, "Erro de conexão no EPG")
        } catch (e: IOException) {
            Log.w(TAG, "[DIAGNOSTICO EPG] Erro I/O ao consultar EPG (host=$safeHost, stream_id=$streamId)")
            ShortEpgFetchResult.Error(streamId, "Falha de rede EPG")
        } catch (e: Exception) {
            Log.e(TAG, "[DIAGNOSTICO EPG] Exceção ao consultar EPG para stream_id=$streamId", e)
            ShortEpgFetchResult.Error(streamId, "Erro inesperado ao consultar EPG")
        }
    }

    private suspend fun testSimpleDataTable(
        dns: String,
        username: String,
        password: String,
        streamId: String
    ) = withContext(Dispatchers.IO) {
        val testUrl = try {
            val cleanDns = dns.trim().trimEnd('/')
            val encodedUser = URLEncoder.encode(username.trim(), "UTF-8")
            val encodedPass = URLEncoder.encode(password.trim(), "UTF-8")
            val encodedStream = URLEncoder.encode(streamId.trim(), "UTF-8")
            "$cleanDns/player_api.php?username=$encodedUser&password=$encodedPass&action=get_simple_data_table&stream_id=$encodedStream"
        } catch (_: Exception) {
            return@withContext
        }

        val request = Request.Builder()
            .url(testUrl)
            .header("User-Agent", "LCPlayer/1.0 (Android TV)")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                val rawBody = response.body?.string().orEmpty().trim()
                Log.i(
                    TAG,
                    "[DIAGNOSTICO EPG COMPLEMENTAR] Resposta get_simple_data_table: stream_id=$streamId, status_http=$code, tamanho_bytes=${rawBody.length}"
                )
                if (code == 200 && rawBody.isNotBlank()) {
                    if (rawBody.startsWith("{")) {
                        val obj = JSONObject(rawBody)
                        val keys = obj.keys().asSequence().toList()
                        Log.i(TAG, "[DIAGNOSTICO EPG COMPLEMENTAR] get_simple_data_table e um Objeto JSON com chaves: $keys")
                        if (obj.has("epg_listings")) {
                            val array = obj.optJSONArray("epg_listings")
                            val count = array?.length() ?: 0
                            Log.i(TAG, "[DIAGNOSTICO EPG COMPLEMENTAR] get_simple_data_table 'epg_listings' possui $count itens")
                            if (array != null && count > 0) {
                                val first = array.optJSONObject(0)
                                Log.i(TAG, "[DIAGNOSTICO EPG COMPLEMENTAR] Item 0 chaves: ${first?.keys()?.asSequence()?.toList()}")
                                Log.i(TAG, "[DIAGNOSTICO EPG COMPLEMENTAR] Item 0 title: '${first?.optString("title")}'")
                            }
                        }
                    } else if (rawBody.startsWith("[")) {
                        val arr = JSONArray(rawBody)
                        Log.i(TAG, "[DIAGNOSTICO EPG COMPLEMENTAR] get_simple_data_table e um Array JSON com ${arr.length()} itens")
                        if (arr.length() > 0) {
                            val first = arr.optJSONObject(0)
                            Log.i(TAG, "[DIAGNOSTICO EPG COMPLEMENTAR] Item 0 chaves: ${first?.keys()?.asSequence()?.toList()}")
                            Log.i(TAG, "[DIAGNOSTICO EPG COMPLEMENTAR] Item 0 title: '${first?.optString("title")}'")
                        }
                    } else {
                        Log.i(TAG, "[DIAGNOSTICO EPG COMPLEMENTAR] Corpo bruto get_simple_data_table (100 chars): '${rawBody.take(100)}'")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "[DIAGNOSTICO EPG COMPLEMENTAR] Falha no teste get_simple_data_table para stream_id=$streamId", e)
        }
    }

    private fun parseEpgJson(rawBody: String, streamId: String): ShortEpgFetchResult {
        if (rawBody.isBlank()) {
            Log.i(TAG, "[DIAGNOSTICO EPG] Corpo da resposta está completamente vazio para stream_id=$streamId")
            return ShortEpgFetchResult.Success(streamId, currentProgram = null, nextProgram = null)
        }

        val programs = mutableListOf<EpgProgram>()

        try {
            var listingsArray: JSONArray? = null

            if (rawBody.startsWith("{")) {
                val rootObj = JSONObject(rawBody)
                val keys = rootObj.keys().asSequence().toList()
                Log.i(TAG, "[DIAGNOSTICO EPG] Objeto JSON raiz detectado. Chaves do objeto raiz: $keys")

                val hasEpgListings = rootObj.has("epg_listings")
                Log.i(TAG, "[DIAGNOSTICO EPG] Campo 'epg_listings' presente na raiz: $hasEpgListings")

                if (hasEpgListings) {
                    listingsArray = rootObj.optJSONArray("epg_listings")
                    val count = listingsArray?.length() ?: 0
                    Log.i(TAG, "[DIAGNOSTICO EPG] Quantidade de itens em 'epg_listings': $count")
                }
            } else if (rawBody.startsWith("[")) {
                listingsArray = JSONArray(rawBody)
                Log.i(TAG, "[DIAGNOSTICO EPG] Array JSON raiz detectado. Quantidade de elementos: ${listingsArray.length()}")
            } else {
                Log.w(TAG, "[DIAGNOSTICO EPG] Resposta bruta de EPG não é JSON válido (primeiros 150 chars): '${rawBody.take(150)}'")
                return ShortEpgFetchResult.Error(streamId, "Formato EPG inválido")
            }

            if (listingsArray != null && listingsArray.length() > 0) {
                val firstItem = listingsArray.optJSONObject(0)
                if (firstItem != null) {
                    val itemKeys = firstItem.keys().asSequence().toList()
                    Log.i(TAG, "[DIAGNOSTICO EPG] Nomes dos campos do Item 0 em epg_listings: $itemKeys")
                    val rawTitle = firstItem.optString("title")
                    val decodedTitle = decodeBase64IfNeeded(rawTitle)
                    val rawDesc = firstItem.optString("description")
                    val decodedDesc = decodeBase64IfNeeded(rawDesc)
                    Log.i(TAG, "[DIAGNOSTICO EPG] Item 0 title raw: '$rawTitle', decoded: '$decodedTitle'")
                    Log.i(TAG, "[DIAGNOSTICO EPG] Item 0 desc raw: '$rawDesc', decoded: '$decodedDesc'")
                    Log.i(TAG, "[DIAGNOSTICO EPG] Item 0 start: '${firstItem.optString("start")}', start_timestamp: '${firstItem.optString("start_timestamp")}'")
                    Log.i(TAG, "[DIAGNOSTICO EPG] Item 0 stop/end: '${firstItem.optString("stop")}/${firstItem.optString("end")}', stop_timestamp: '${firstItem.optString("stop_timestamp")}'")
                }

                for (i in 0 until listingsArray.length()) {
                    val itemObj = listingsArray.optJSONObject(i) ?: continue
                    val program = parseSingleEpgItem(itemObj)
                    if (program != null) {
                        programs.add(program)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[DIAGNOSTICO EPG] Erro ao interpretar JSON de EPG para stream_id=$streamId", e)
            return ShortEpgFetchResult.Error(streamId, "Formato EPG inválido")
        }

        if (programs.isEmpty()) {
            Log.i(TAG, "[DIAGNOSTICO EPG] Nenhum programa de EPG válido foi extraído da resposta para stream_id=$streamId")
            return ShortEpgFetchResult.Success(streamId, currentProgram = null, nextProgram = null)
        }

        // Separa programa atual e próximo programa
        val currentProgram = programs.firstOrNull { it.isNowPlaying } ?: programs.firstOrNull()
        val nextProgram = if (programs.size > 1) {
            if (currentProgram == programs[0]) programs[1] else programs[0]
        } else null

        Log.i(
            TAG,
            "[DIAGNOSTICO EPG] EPG processado com sucesso para stream_id=$streamId: Programa Atual='${currentProgram?.title}', Próximo='${nextProgram?.title}'"
        )

        return ShortEpgFetchResult.Success(
            streamId = streamId,
            currentProgram = currentProgram,
            nextProgram = nextProgram
        )
    }

    private fun parseSingleEpgItem(obj: JSONObject): EpgProgram? {
        val rawTitle = obj.optString("title").ifBlank { obj.optString("name") }
        val decodedTitle = decodeBase64IfNeeded(rawTitle)
        if (decodedTitle.isBlank()) return null

        val rawDesc = obj.optString("description").ifBlank { obj.optString("desc") }
        val decodedDesc = decodeBase64IfNeeded(rawDesc).ifBlank { null }

        val id = obj.optString("id").ifBlank { obj.optString("epg_id") }.ifBlank { null }

        // Extrai Timestamps Unix (em segundos)
        val startTs = parseLongOpt(obj, "start_timestamp")
        val stopTs = parseLongOpt(obj, "stop_timestamp") ?: parseLongOpt(obj, "end_timestamp")

        val nowTsSeconds = System.currentTimeMillis() / 1000L
        val isNowPlaying = when {
            obj.has("now_playing") -> obj.optInt("now_playing", 0) == 1 || obj.optBoolean("now_playing", false)
            startTs != null && stopTs != null -> nowTsSeconds in startTs..stopTs
            else -> false
        }

        // Formatação dos horários inicial e final para o fuso local
        val startTimeFormatted = formatTimestampToLocalTime(startTs)
            ?: parseServerDateToLocalTime(obj.optString("start"))

        val endTimeFormatted = formatTimestampToLocalTime(stopTs)
            ?: parseServerDateToLocalTime(obj.optString("end").ifBlank { obj.optString("stop") })

        return EpgProgram(
            id = id,
            title = decodedTitle,
            description = decodedDesc,
            startTimeFormatted = startTimeFormatted,
            endTimeFormatted = endTimeFormatted,
            startTimestamp = startTs,
            endTimestamp = stopTs,
            isNowPlaying = isNowPlaying
        )
    }

    private fun parseLongOpt(obj: JSONObject, key: String): Long? {
        if (!obj.has(key)) return null
        return try {
            val valStr = obj.optString(key).trim()
            if (valStr.isNotBlank() && valStr != "null") {
                valStr.toLongOrNull()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
