package com.example.data.remote

import android.os.SystemClock
import android.util.Log
import com.example.domain.model.CategoryLoadResult
import com.example.domain.model.CategoryType
import com.example.domain.model.XtreamCategory
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
 * Cliente responsável por consultar categorias reais (Live TV, Filmes e Séries)
 * e validar o conteúdo real de Séries na API compatível com Xtream Codes.
 *
 * Endpoints utilizados:
 * - <DNS>/player_api.php?username=<USERNAME>&password=<PASSWORD>&action=get_live_categories
 * - <DNS>/player_api.php?username=<USERNAME>&password=<PASSWORD>&action=get_vod_categories
 * - <DNS>/player_api.php?username=<USERNAME>&password=<PASSWORD>&action=get_series_categories
 * - <DNS>/player_api.php?username=<USERNAME>&password=<PASSWORD>&action=get_series (fallback de séries / contagens)
 *
 * DIRETRIZES ESTRITAS DE SEGURANÇA:
 * - NUNCA exibe em tela ou logs: password, URL completa com senha ou credenciais.
 * - Registra apenas diagnóstico seguro: tipo, HTTP status, Content-Type, tamanho aproximado,
 *   tipo JSON da resposta, quantidade de elementos e nomes das propriedades do primeiro elemento.
 */
class XtreamCategoryClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .build()
) {

    companion object {
        private const val TAG = "LCPlayer_Categories"

        /**
         * Constrói a URL para a consulta de categorias no padrão Xtream Codes:
         * - Preserva protocolo original (http:// ou https://)
         * - Remove barras finais duplicadas do DNS
         * - Aplica URL encoding no username e password
         * - Adiciona o parâmetro de ação correspondente
         */
        fun buildCategoryUrl(dns: String, username: String, password: String, action: String): String {
            val cleanDns = dns.trim().trimEnd('/')
            val encodedUser = URLEncoder.encode(username.trim(), "UTF-8")
            val encodedPass = URLEncoder.encode(password.trim(), "UTF-8")
            return "$cleanDns/player_api.php?username=$encodedUser&password=$encodedPass&action=$action"
        }

        /**
         * Extrai de forma segura apenas o host para logs de diagnóstico,
         * sem registrar dados sensíveis ou parâmetros da query.
         */
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
     * Consulta as categorias para o tipo de conteúdo especificado (Live, VOD ou Séries).
     * Caso o tipo seja Séries e as categorias estejam vazias ou sem contagem de itens,
     * consulta complementarmente 'action=get_series' para relacionar category_id e
     * calcular as quantidades reais.
     */
    fun fetchCategories(
        dns: String,
        username: String,
        password: String,
        type: CategoryType
    ): CategoryLoadResult {
        if (dns.isBlank() || username.isBlank() || password.isBlank()) {
            return CategoryLoadResult.AuthError(
                displayMessage = "Não foi possível autenticar esta lista",
                type = type
            )
        }

        val requestUrl = try {
            buildCategoryUrl(dns, username, password, type.actionParam)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao construir URL segura para ação '${type.actionParam}'", e)
            return CategoryLoadResult.NetworkError(
                displayMessage = getNetworkErrorMessage(type),
                type = type
            )
        }

        val safeHost = extractSafeHost(dns)
        val startTime = SystemClock.elapsedRealtime()

        Log.d(TAG, "Iniciando consulta de categorias: tipo=${type.displayName}, action=${type.actionParam}, host=$safeHost")

        val request = Request.Builder()
            .url(requestUrl)
            .header("User-Agent", "LCPlayer/1.0 (Android TV)")
            .get()
            .build()

        val initialResult: CategoryLoadResult = try {
            client.newCall(request).execute().use { response ->
                val durationMs = SystemClock.elapsedRealtime() - startTime
                val httpCode = response.code
                val contentType = response.header("Content-Type").orEmpty()
                val rawBody = response.body?.string().orEmpty().trim()
                val bodyLengthBytes = rawBody.length

                // Diagnóstico seguro e detalhado conforme Requisito 2
                logSafeDiagnostics(
                    action = type.actionParam,
                    type = type,
                    host = safeHost,
                    httpCode = httpCode,
                    contentType = contentType,
                    bodyLengthBytes = bodyLengthBytes,
                    rawBody = rawBody,
                    durationMs = durationMs
                )

                // Servidor retornou código 5xx (Erro interno)
                if (httpCode in 500..599) {
                    Log.w(TAG, "Servidor retornou erro interno HTTP $httpCode para ${type.displayName}")
                    return CategoryLoadResult.NetworkError(
                        displayMessage = getNetworkErrorMessage(type),
                        type = type
                    )
                }

                // Erro de autenticação (401 Não Autorizado / 403 Proibido)
                if (httpCode == 401 || httpCode == 403) {
                    Log.w(TAG, "Servidor retornou recusa de autenticação HTTP $httpCode para ${type.displayName}")
                    return CategoryLoadResult.AuthError(
                        displayMessage = "Não foi possível autenticar esta lista",
                        type = type
                    )
                }

                if (httpCode != 200) {
                    Log.w(TAG, "Servidor retornou código inesperado: $httpCode para ${type.displayName}")
                    return CategoryLoadResult.NetworkError(
                        displayMessage = getNetworkErrorMessage(type),
                        type = type
                    )
                }

                parseCategoryResponse(rawBody, type)
            }
        } catch (e: SocketTimeoutException) {
            val durationMs = SystemClock.elapsedRealtime() - startTime
            Log.e(TAG, "Timeout ao consultar categorias de ${type.displayName} após ${durationMs}ms (host=$safeHost)", e)
            CategoryLoadResult.NetworkError(
                displayMessage = "O servidor demorou para responder",
                type = type
            )
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Falha de DNS ou sem conexão ao consultar ${type.displayName} (host=$safeHost)", e)
            CategoryLoadResult.NetworkError(
                displayMessage = "Sem conexão com a internet",
                type = type
            )
        } catch (e: ConnectException) {
            Log.e(TAG, "Conexão recusada ao consultar ${type.displayName} (host=$safeHost)", e)
            CategoryLoadResult.NetworkError(
                displayMessage = getNetworkErrorMessage(type),
                type = type
            )
        } catch (e: IOException) {
            Log.e(TAG, "Erro I/O de rede ao comunicar com ${type.displayName} (host=$safeHost)", e)
            CategoryLoadResult.NetworkError(
                displayMessage = getNetworkErrorMessage(type),
                type = type
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exceção inesperada durante leitura de categorias de ${type.displayName}", e)
            CategoryLoadResult.NetworkError(
                displayMessage = getNetworkErrorMessage(type),
                type = type
            )
        }

        // PARTE 2: Se for Séries, enriquecer ou recuperar categorias a partir de get_series caso necessário
        if (type == CategoryType.SERIES) {
            return resolveSeriesCategoriesAndCounts(dns, username, password, initialResult)
        }

        return initialResult
    }

    /**
     * Registra informações diagnósticas seguras sem expor credenciais.
     */
    private fun logSafeDiagnostics(
        action: String,
        type: CategoryType,
        host: String,
        httpCode: Int,
        contentType: String,
        bodyLengthBytes: Int,
        rawBody: String,
        durationMs: Long
    ) {
        val jsonType = when {
            rawBody.isEmpty() -> "Vazio (0 bytes)"
            rawBody.startsWith("[") -> "JSON Array"
            rawBody.startsWith("{") -> "JSON Object"
            else -> "Texto / Desconhecido"
        }

        val elementCount = when {
            rawBody.startsWith("[") -> try { JSONArray(rawBody).length() } catch (_: Exception) { -1 }
            rawBody.startsWith("{") -> try { JSONObject(rawBody).length() } catch (_: Exception) { -1 }
            else -> 0
        }

        val firstElementProperties: List<String> = try {
            if (rawBody.startsWith("[")) {
                val array = JSONArray(rawBody)
                if (array.length() > 0) {
                    val obj = array.optJSONObject(0)
                    obj?.keys()?.asSequence()?.toList().orEmpty()
                } else emptyList()
            } else if (rawBody.startsWith("{")) {
                val obj = JSONObject(rawBody)
                val firstKey = obj.keys().asSequence().firstOrNull()
                val nestedObj = firstKey?.let { obj.optJSONObject(it) }
                nestedObj?.keys()?.asSequence()?.toList() ?: obj.keys().asSequence().toList()
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        Log.i(
            TAG,
            "Diagnóstico de resposta [$action - ${type.displayName}]: " +
                    "HTTP=$httpCode, " +
                    "Content-Type='$contentType', " +
                    "Tamanho=$bodyLengthBytes bytes, " +
                    "Tipo=$jsonType, " +
                    "Elementos=$elementCount, " +
                    "PropriedadesPrimeiroElemento=$firstElementProperties, " +
                    "Duração=${durationMs}ms, " +
                    "Host=$host"
        )
    }

    /**
     * Decodifica a resposta JSON de categorias da API Xtream.
     * Suporta tanto JSON Array quanto JSON Object (arrays associativos do PHP),
     * além de variações flexíveis nos nomes dos atributos.
     */
    private fun parseCategoryResponse(rawBody: String, type: CategoryType): CategoryLoadResult {
        if (rawBody.isEmpty()) {
            Log.d(TAG, "Corpo vazio recebido para ${type.displayName}")
            return CategoryLoadResult.Empty(
                displayMessage = getEmptyMessage(type),
                type = type
            )
        }

        // Trata respostas que iniciam com '{' (JSON Object)
        if (rawBody.startsWith("{")) {
            try {
                val jsonObject = JSONObject(rawBody)

                // 1. Verifica se é objeto de erro de autenticação (ex.: {"user_info":{"auth":0}})
                val userInfo = jsonObject.optJSONObject("user_info")
                if (userInfo != null) {
                    val auth = userInfo.opt("auth")
                    val isAuthValid = when (auth) {
                        is Int -> auth == 1
                        is Long -> auth == 1L
                        is String -> auth == "1" || auth.equals("true", ignoreCase = true)
                        is Boolean -> auth
                        else -> false
                    }
                    if (!isAuthValid) {
                        Log.w(TAG, "Servidor retornou objeto indicando autenticação inválida (auth=0)")
                        return CategoryLoadResult.AuthError(
                            displayMessage = "Não foi possível autenticar esta lista",
                            type = type
                        )
                    }
                }

                // 2. Verifica se é objeto com mensagem de erro direta do backend
                if (jsonObject.has("error") || jsonObject.has("message")) {
                    val errorMsg = jsonObject.optString("error", jsonObject.optString("message", ""))
                    Log.w(TAG, "Servidor retornou objeto de erro para ${type.displayName}: $errorMsg")
                    return CategoryLoadResult.ParseError(
                        displayMessage = getParseErrorMessage(type),
                        type = type
                    )
                }

                // 3. Verifica se o JSON Object encapsula um array de categorias (ex: {"categories": [...]})
                val arrayCandidateKey = when {
                    jsonObject.has("categories") -> "categories"
                    jsonObject.has("series_categories") -> "series_categories"
                    jsonObject.has("data") -> "data"
                    jsonObject.has("items") -> "items"
                    else -> null
                }
                if (arrayCandidateKey != null) {
                    val candidateArray = jsonObject.optJSONArray(arrayCandidateKey)
                    if (candidateArray != null) {
                        return parseJsonArrayCategories(candidateArray, type)
                    }
                }

                // 4. Suporte a Arrays Associativos do PHP (ex: {"0": {...}, "1": {...}} ou {"10": {...}})
                val categories = ArrayList<XtreamCategory>()
                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val item = jsonObject.optJSONObject(key)
                    if (item != null) {
                        parseSingleCategoryItem(item, type, key)?.let { categories.add(it) }
                    } else {
                        // Caso especial: objeto com ID como chave e Nome como valor string: {"10": "Drama"}
                        val nameValue = jsonObject.optString(key, "")
                        if (nameValue.isNotBlank()) {
                            categories.add(
                                XtreamCategory(
                                    categoryId = key,
                                    categoryName = nameValue,
                                    parentId = null,
                                    type = type,
                                    itemCount = null
                                )
                            )
                        }
                    }
                }

                if (categories.isNotEmpty()) {
                    Log.i(TAG, "Categorias decodificadas com sucesso a partir de JSON Object para ${type.displayName}: ${categories.size} itens")
                    return CategoryLoadResult.Success(categories, type)
                }

                // Se o JSON Object estiver completamente vazio ({})
                if (jsonObject.length() == 0) {
                    return CategoryLoadResult.Empty(
                        displayMessage = getEmptyMessage(type),
                        type = type
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Falha ao decodificar JSON Object de categorias para ${type.displayName}", e)
                return CategoryLoadResult.ParseError(
                    displayMessage = getParseErrorMessage(type),
                    type = type
                )
            }

            return CategoryLoadResult.ParseError(
                displayMessage = getParseErrorMessage(type),
                type = type
            )
        }

        // Padrão JSON Array
        if (rawBody.startsWith("[")) {
            return try {
                val jsonArray = JSONArray(rawBody)
                parseJsonArrayCategories(jsonArray, type)
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao decodificar JSON Array de categorias para ${type.displayName}", e)
                CategoryLoadResult.ParseError(
                    displayMessage = getParseErrorMessage(type),
                    type = type
                )
            }
        }

        Log.w(TAG, "Resposta recebida não inicia com '[' nem '{'")
        return CategoryLoadResult.ParseError(
            displayMessage = getParseErrorMessage(type),
            type = type
        )
    }

    /**
     * Itera sobre um JSONArray extraindo as categorias de forma tolerante a variações de campos.
     */
    private fun parseJsonArrayCategories(jsonArray: JSONArray, type: CategoryType): CategoryLoadResult {
        if (jsonArray.length() == 0) {
            return CategoryLoadResult.Empty(
                displayMessage = getEmptyMessage(type),
                type = type
            )
        }

        val categories = ArrayList<XtreamCategory>(jsonArray.length())
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.optJSONObject(i) ?: continue
            parseSingleCategoryItem(item, type, fallbackId = (i + 1).toString())?.let { categories.add(it) }
        }

        if (categories.isEmpty()) {
            return CategoryLoadResult.Empty(
                displayMessage = getEmptyMessage(type),
                type = type
            )
        }

        Log.i(TAG, "Categorias carregadas com sucesso para ${type.displayName}: ${categories.size} itens")
        return CategoryLoadResult.Success(categories, type)
    }

    /**
     * Extrai um objeto de categoria individual aceitando diferentes nomes de atributos comuns na API Xtream.
     */
    private fun parseSingleCategoryItem(item: JSONObject, type: CategoryType, fallbackId: String): XtreamCategory? {
        val categoryId = when {
            item.has("category_id") -> item.optString("category_id")
            item.has("id") -> item.optString("id")
            item.has("cat_id") -> item.optString("cat_id")
            item.has("series_category_id") -> item.optString("series_category_id")
            else -> fallbackId
        }?.trim()

        val categoryName = when {
            item.has("category_name") -> item.optString("category_name")
            item.has("name") -> item.optString("name")
            item.has("title") -> item.optString("title")
            item.has("category") -> item.optString("category")
            else -> null
        }?.trim()

        if (categoryId.isNullOrBlank() || categoryName.isNullOrBlank()) {
            return null
        }

        val parentId = when {
            item.has("parent_id") && !item.isNull("parent_id") -> item.optInt("parent_id", 0)
            else -> null
        }

        // Quantidade de itens embutida na categoria se o servidor fornecer
        val itemCount = when {
            item.has("count") && !item.isNull("count") -> item.optInt("count", -1)
            item.has("stream_count") && !item.isNull("stream_count") -> item.optInt("stream_count", -1)
            item.has("total") && !item.isNull("total") -> item.optInt("total", -1)
            item.has("series_count") && !item.isNull("series_count") -> item.optInt("series_count", -1)
            item.has("channels") && !item.isNull("channels") -> item.optInt("channels", -1)
            item.has("num") && !item.isNull("num") -> item.optInt("num", -1)
            else -> -1
        }.takeIf { it >= 0 }

        return XtreamCategory(
            categoryId = categoryId,
            categoryName = categoryName,
            parentId = parentId,
            type = type,
            itemCount = itemCount
        )
    }

    /**
     * PARTE 2: Trata especificamente a resolução e contagem real de Séries.
     * Caso o endpoint 'get_series_categories' retorne vazio ou caso as categorias
     * ainda não possuam as contagens reais de séries, consulta 'action=get_series'.
     */
    private fun resolveSeriesCategoriesAndCounts(
        dns: String,
        username: String,
        password: String,
        categoriesResult: CategoryLoadResult
    ): CategoryLoadResult {
        val seriesData = fetchSeriesData(dns, username, password)

        // Se a consulta get_series_categories retornou categorias válidas
        if (categoriesResult is CategoryLoadResult.Success && categoriesResult.categories.isNotEmpty()) {
            if (seriesData != null && seriesData.seriesCountByCatId.isNotEmpty()) {
                // Atualiza cada categoria com a contagem real de séries correspondente
                val updatedCategories = categoriesResult.categories.map { category ->
                    val realCount = seriesData.seriesCountByCatId[category.categoryId]
                    if (realCount != null) {
                        category.copy(itemCount = realCount)
                    } else {
                        category
                    }
                }
                Log.i(TAG, "Contagens reais de séries associadas com sucesso a ${updatedCategories.size} categorias")
                return CategoryLoadResult.Success(updatedCategories, CategoryType.SERIES)
            }
            return categoriesResult
        }

        // Se 'get_series_categories' retornou vazio (Empty) mas o servidor possui séries reais em 'get_series'
        if (categoriesResult is CategoryLoadResult.Empty || categoriesResult is CategoryLoadResult.ParseError) {
            if (seriesData != null && seriesData.extractedCategories.isNotEmpty()) {
                Log.i(
                    TAG,
                    "Categorias de séries recuperadas diretamente via 'get_series': ${seriesData.extractedCategories.size} categorias com séries reais"
                )
                return CategoryLoadResult.Success(seriesData.extractedCategories, CategoryType.SERIES)
            }
        }

        return categoriesResult
    }

    /**
     * Estrutura interna para transporte dos dados consolidados de séries.
     */
    private data class SeriesExtractionResult(
        val seriesCountByCatId: Map<String, Int>,
        val extractedCategories: List<XtreamCategory>
    )

    /**
     * Consulta 'action=get_series' sem carregar detalhes pesados como episódios ou temporadas.
     * Relaciona category_id e calcula o número de séries por categoria.
     */
    private fun fetchSeriesData(dns: String, username: String, password: String): SeriesExtractionResult? {
        val url = try {
            buildCategoryUrl(dns, username, password, "get_series")
        } catch (_: Exception) {
            return null
        }

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "LCPlayer/1.0 (Android TV)")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.code != 200) return null
                val body = response.body?.string().orEmpty().trim()
                if (!body.startsWith("[")) return null

                val jsonArray = JSONArray(body)
                val countMap = HashMap<String, Int>()
                val categoryNameMap = HashMap<String, String>()

                for (i in 0 until jsonArray.length()) {
                    val seriesObj = jsonArray.optJSONObject(i) ?: continue
                    val catId = when {
                        seriesObj.has("category_id") -> seriesObj.optString("category_id")
                        seriesObj.has("cat_id") -> seriesObj.optString("cat_id")
                        else -> null
                    }?.trim() ?: continue

                    if (catId.isBlank()) continue

                    val currentCount = countMap.getOrDefault(catId, 0)
                    countMap[catId] = currentCount + 1

                    val catName = when {
                        seriesObj.has("category_name") -> seriesObj.optString("category_name")
                        seriesObj.has("category") -> seriesObj.optString("category")
                        else -> null
                    }?.trim()

                    if (!catName.isNullOrBlank() && !categoryNameMap.containsKey(catId)) {
                        categoryNameMap[catId] = catName
                    }
                }

                val categories = countMap.map { (catId, count) ->
                    val name = categoryNameMap[catId] ?: "Séries (Cat. $catId)"
                    XtreamCategory(
                        categoryId = catId,
                        categoryName = name,
                        parentId = null,
                        type = CategoryType.SERIES,
                        itemCount = count
                    )
                }.sortedBy { it.categoryName }

                Log.i(TAG, "Leitura de 'get_series' concluída: total de ${jsonArray.length()} séries mapeadas em ${categories.size} categorias distintas")
                SeriesExtractionResult(countMap, categories)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Não foi possível obter dados complementares de 'get_series': ${e.message}")
            null
        }
    }

    private fun getEmptyMessage(type: CategoryType): String = when (type) {
        CategoryType.LIVE -> "Nenhuma categoria de TV encontrada"
        CategoryType.VOD -> "Nenhuma categoria de filmes encontrada"
        CategoryType.SERIES -> "Nenhuma categoria de séries encontrada"
    }

    private fun getParseErrorMessage(type: CategoryType): String = when (type) {
        CategoryType.LIVE -> "Erro ao interpretar categorias de TV"
        CategoryType.VOD -> "Erro ao interpretar categorias de filmes"
        CategoryType.SERIES -> "Erro ao interpretar categorias de séries"
    }

    private fun getNetworkErrorMessage(type: CategoryType): String = when (type) {
        CategoryType.LIVE -> "Não foi possível carregar a TV ao vivo"
        CategoryType.VOD -> "Não foi possível carregar os filmes"
        CategoryType.SERIES -> "Não foi possível carregar as séries"
    }
}
