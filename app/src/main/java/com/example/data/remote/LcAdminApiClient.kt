package com.example.data.remote

import android.util.Log
import com.example.domain.model.ActivationStatus
import com.example.domain.model.DeviceListModel
import com.example.domain.model.ListStatus
import com.example.domain.model.formatExpiresAt
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

sealed class LcAdminResponse {
    data class Success(
        val httpCode: Int,
        val statusString: String,
        val activationStatus: ActivationStatus,
        val displayMessage: String,
        val responseBody: String
    ) : LcAdminResponse()

    data class Error(
        val httpCode: Int?,
        val displayMessage: String,
        val responseBody: String? = null,
        val isNetworkOrTimeout: Boolean = false,
        val technicalException: Throwable? = null
    ) : LcAdminResponse()
}

sealed class LcAdminListsResponse {
    data class Success(
        val httpCode: Int,
        val rawStatus: String,
        val deviceStatus: ActivationStatus,
        val lists: List<DeviceListModel>,
        val displayMessage: String
    ) : LcAdminListsResponse()

    data class Error(
        val httpCode: Int?,
        val displayMessage: String,
        val isNetworkOrTimeout: Boolean = false,
        val technicalException: Throwable? = null
    ) : LcAdminListsResponse()
}

sealed class LcAdminDeleteResponse {
    data class Success(
        val httpCode: Int,
        val status: String,
        val displayMessage: String = "Lista excluída"
    ) : LcAdminDeleteResponse()

    data class NotFound(
        val displayMessage: String = "Esta lista não está mais disponível"
    ) : LcAdminDeleteResponse()

    data class NotRegistered(
        val displayMessage: String = "Não foi possível validar o dispositivo"
    ) : LcAdminDeleteResponse()

    data class Error(
        val httpCode: Int?,
        val displayMessage: String,
        val isNetworkOrTimeout: Boolean = false,
        val technicalException: Throwable? = null
    ) : LcAdminDeleteResponse()
}

/**
 * Cliente HTTP responsável pela comunicação com a API pública do LC Admin.
 */
class LcAdminApiClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    companion object {
        private const val TAG = "LCPlayer_Admin"
        private const val ENDPOINT_STATUS_URL = "https://dashbordlc.lovable.app/api/public/device-status"
        private const val ENDPOINT_LISTS_URL = "https://dashbordlc.lovable.app/api/public/device-lists"
        private const val ENDPOINT_DELETE_LIST_URL = "https://dashbordlc.lovable.app/api/public/delete-device-list"
        private const val API_KEY = "sb_publishable_lAEAcvcuQfOj6UeEg7rXcw_kLmCDoQH"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    /**
     * Consulta o status atual do dispositivo no endpoint /device-status.
     * Preservado rigorosamente sem alterações na sua lógica e formato.
     */
    fun checkDeviceStatus(deviceId: String, key: String): LcAdminResponse {
        Log.d(TAG, "Início da tentativa de verificação no LC Admin [URL: $ENDPOINT_STATUS_URL]")

        val payload = JSONObject().apply {
            put("device_id", deviceId)
            put("key", key)
        }.toString()

        val request = Request.Builder()
            .url(ENDPOINT_STATUS_URL)
            .addHeader("Content-Type", "application/json")
            .addHeader("apikey", API_KEY)
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val httpCode = response.code
                val bodyString = response.body?.string().orEmpty()

                Log.d(TAG, "HTTP status recebido: $httpCode")

                val parsedJson = try {
                    JSONObject(bodyString)
                } catch (e: Exception) {
                    JSONObject()
                }

                val statusField = parsedJson.optString("status", "").lowercase()
                val errorField = parsedJson.optString("error", "").lowercase()

                Log.d(TAG, "Corpo da resposta: status='$statusField', error='$errorField'")

                when (httpCode) {
                    200 -> handleHttp200(httpCode, statusField, bodyString)
                    400 -> {
                        Log.w(TAG, "Requisição inválida (HTTP 400). Erro retornado: $errorField")
                        LcAdminResponse.Error(
                            httpCode = 400,
                            displayMessage = "Erro na verificação",
                            responseBody = bodyString
                        )
                    }
                    401 -> {
                        Log.w(TAG, "Erro de autorização (HTTP 401). Verifique a chave da API.")
                        LcAdminResponse.Error(
                            httpCode = 401,
                            displayMessage = "Erro de autorização",
                            responseBody = bodyString
                        )
                    }
                    429 -> {
                        Log.w(TAG, "Limite de requisições excedido (HTTP 429).")
                        LcAdminResponse.Error(
                            httpCode = 429,
                            displayMessage = "Muitas tentativas. Tente novamente.",
                            responseBody = bodyString
                        )
                    }
                    503 -> {
                        Log.w(TAG, "Servidor em manutenção ou indisponível (HTTP 503).")
                        LcAdminResponse.Error(
                            httpCode = 503,
                            displayMessage = "Serviço temporariamente indisponível",
                            responseBody = bodyString
                        )
                    }
                    else -> {
                        Log.w(TAG, "Código HTTP inesperado: $httpCode")
                        LcAdminResponse.Error(
                            httpCode = httpCode,
                            displayMessage = "Erro no servidor (HTTP $httpCode)",
                            responseBody = bodyString
                        )
                    }
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Ocorreu timeout ao tentar conectar ao LC Admin", e)
            LcAdminResponse.Error(
                httpCode = null,
                displayMessage = "Erro de conexão",
                isNetworkOrTimeout = true,
                technicalException = e
            )
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Ocorreu falha de DNS/rede ao tentar resolver o host do LC Admin", e)
            LcAdminResponse.Error(
                httpCode = null,
                displayMessage = "Erro de conexão",
                isNetworkOrTimeout = true,
                technicalException = e
            )
        } catch (e: IOException) {
            Log.e(TAG, "Falha de rede/comunicação I/O", e)
            LcAdminResponse.Error(
                httpCode = null,
                displayMessage = "Erro de conexão",
                isNetworkOrTimeout = true,
                technicalException = e
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exceção técnica inesperada durante verificação", e)
            LcAdminResponse.Error(
                httpCode = null,
                displayMessage = "Erro de conexão",
                isNetworkOrTimeout = true,
                technicalException = e
            )
        }
    }

    /**
     * Consulta as listas vinculadas ao dispositivo no endpoint /device-lists.
     * Preserva dados sensíveis (username e password) fora dos logs do sistema.
     */
    fun fetchDeviceLists(deviceId: String, key: String): LcAdminListsResponse {
        Log.d(TAG, "Início da consulta de listas no LC Admin [URL: $ENDPOINT_LISTS_URL]")

        val payload = JSONObject().apply {
            put("device_id", deviceId)
            put("key", key)
        }.toString()

        val request = Request.Builder()
            .url(ENDPOINT_LISTS_URL)
            .addHeader("Content-Type", "application/json")
            .addHeader("apikey", API_KEY)
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val httpCode = response.code
                val bodyString = response.body?.string().orEmpty()

                Log.d(TAG, "HTTP status /device-lists: $httpCode")

                when (httpCode) {
                    200 -> parseListsHttp200(httpCode, bodyString)
                    400 -> {
                        Log.w(TAG, "Erro HTTP 400 ao carregar listas (invalid_request)")
                        LcAdminListsResponse.Error(
                            httpCode = 400,
                            displayMessage = "Erro ao carregar listas"
                        )
                    }
                    401 -> {
                        Log.w(TAG, "Erro HTTP 401 ao carregar listas (unauthorized)")
                        LcAdminListsResponse.Error(
                            httpCode = 401,
                            displayMessage = "Erro de conexão"
                        )
                    }
                    429 -> {
                        Log.w(TAG, "Erro HTTP 429 ao carregar listas (too_many_requests)")
                        LcAdminListsResponse.Error(
                            httpCode = 429,
                            displayMessage = "Muitas tentativas. Tente novamente em alguns instantes."
                        )
                    }
                    503 -> {
                        Log.w(TAG, "Erro HTTP 503 ao carregar listas (unavailable)")
                        LcAdminListsResponse.Error(
                            httpCode = 503,
                            displayMessage = "Serviço temporariamente indisponível"
                        )
                    }
                    else -> {
                        Log.w(TAG, "Código inesperado ao carregar listas: $httpCode")
                        LcAdminListsResponse.Error(
                            httpCode = httpCode,
                            displayMessage = "Erro ao carregar listas"
                        )
                    }
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Timeout ao consultar /device-lists", e)
            LcAdminListsResponse.Error(
                httpCode = null,
                displayMessage = "Erro de conexão",
                isNetworkOrTimeout = true,
                technicalException = e
            )
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Falha de DNS/rede ao consultar /device-lists", e)
            LcAdminListsResponse.Error(
                httpCode = null,
                displayMessage = "Erro de conexão",
                isNetworkOrTimeout = true,
                technicalException = e
            )
        } catch (e: IOException) {
            Log.e(TAG, "Falha I/O de rede ao consultar /device-lists", e)
            LcAdminListsResponse.Error(
                httpCode = null,
                displayMessage = "Erro de conexão",
                isNetworkOrTimeout = true,
                technicalException = e
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exceção inesperada ao consultar /device-lists", e)
            LcAdminListsResponse.Error(
                httpCode = null,
                displayMessage = "Erro de conexão",
                isNetworkOrTimeout = true,
                technicalException = e
            )
        }
    }

    private fun parseListsHttp200(httpCode: Int, bodyString: String): LcAdminListsResponse {
        val parsedJson = try {
            JSONObject(bodyString)
        } catch (e: Exception) {
            return LcAdminListsResponse.Error(
                httpCode = httpCode,
                displayMessage = "Erro ao carregar listas"
            )
        }

        val rawStatus = parsedJson.optString("status", "").lowercase()
        Log.d(TAG, "Status do dispositivo retornado em /device-lists: '$rawStatus'")

        val deviceStatus = when (rawStatus) {
            "active", "ativo" -> ActivationStatus.ACTIVE
            "blocked", "bloqueado" -> ActivationStatus.BLOCKED
            "expired", "vencido" -> ActivationStatus.EXPIRED
            else -> ActivationStatus.WAITING_ACTIVATION
        }

        val listsArray = parsedJson.optJSONArray("lists")
        val parsedLists = mutableListOf<DeviceListModel>()

        if (listsArray != null) {
            for (i in 0 until listsArray.length()) {
                val itemObj = listsArray.optJSONObject(i) ?: continue
                val id = itemObj.optString("id", "list_$i")
                val title = itemObj.optString("title", "Lista ${i + 1}")
                val sourceName = itemObj.optString("source_name", "Servidor")
                val dns = itemObj.optString("dns", "")
                val username = itemObj.optString("username", "")
                val password = itemObj.optString("password", "")
                val itemStatusRaw = itemObj.optString("status", "active")
                val expiresAt = if (itemObj.isNull("expires_at")) null else itemObj.optString("expires_at", null)
                val displayOrder = itemObj.optInt("display_order", i + 1)

                parsedLists.add(
                    DeviceListModel(
                        id = id,
                        title = title,
                        sourceName = sourceName,
                        dns = dns,
                        username = username,
                        password = password,
                        status = ListStatus.fromString(itemStatusRaw),
                        rawStatus = itemStatusRaw,
                        expiresAt = expiresAt,
                        formattedExpiration = formatExpiresAt(expiresAt),
                        displayOrder = displayOrder
                    )
                )
            }
        }

        // Ordena visualmente pelo campo display_order
        val sortedLists = parsedLists.sortedBy { it.displayOrder }
        Log.d(TAG, "Total de listas recebidas e ordenadas: ${sortedLists.size}")

        val displayMsg = when (deviceStatus) {
            ActivationStatus.ACTIVE -> if (sortedLists.isEmpty()) "Nenhuma lista configurada" else "Listas carregadas com sucesso"
            ActivationStatus.BLOCKED -> "Dispositivo bloqueado"
            ActivationStatus.EXPIRED -> "Acesso vencido"
            else -> "Aguardando ativação"
        }

        return LcAdminListsResponse.Success(
            httpCode = httpCode,
            rawStatus = rawStatus,
            deviceStatus = deviceStatus,
            lists = sortedLists,
            displayMessage = displayMsg
        )
    }

    private fun handleHttp200(httpCode: Int, statusField: String, rawBody: String): LcAdminResponse {
        return when (statusField) {
            "active", "ativo" -> {
                Log.i(TAG, "Dispositivo verificado com sucesso: ATIVO")
                LcAdminResponse.Success(
                    httpCode = httpCode,
                    statusString = statusField,
                    activationStatus = ActivationStatus.ACTIVE,
                    displayMessage = "Dispositivo ativado",
                    responseBody = rawBody
                )
            }
            "pending", "not_registered", "waiting", "aguardando" -> {
                Log.i(TAG, "Dispositivo ainda não ativado ou pendente no LC Admin: $statusField")
                LcAdminResponse.Success(
                    httpCode = httpCode,
                    statusString = statusField,
                    activationStatus = ActivationStatus.WAITING_ACTIVATION,
                    displayMessage = "Aguardando ativação",
                    responseBody = rawBody
                )
            }
            "blocked", "bloqueado" -> {
                Log.w(TAG, "Dispositivo bloqueado no LC Admin")
                LcAdminResponse.Success(
                    httpCode = httpCode,
                    statusString = statusField,
                    activationStatus = ActivationStatus.BLOCKED,
                    displayMessage = "Dispositivo bloqueado",
                    responseBody = rawBody
                )
            }
            "expired", "vencido" -> {
                Log.w(TAG, "Acesso do dispositivo vencido no LC Admin")
                LcAdminResponse.Success(
                    httpCode = httpCode,
                    statusString = statusField,
                    activationStatus = ActivationStatus.EXPIRED,
                    displayMessage = "Acesso vencido",
                    responseBody = rawBody
                )
            }
            else -> {
                Log.i(TAG, "Status recebido: '$statusField'. Definindo como aguardando ativação.")
                LcAdminResponse.Success(
                    httpCode = httpCode,
                    statusString = statusField,
                    activationStatus = ActivationStatus.WAITING_ACTIVATION,
                    displayMessage = "Aguardando ativação",
                    responseBody = rawBody
                )
            }
        }
    }

    /**
     * Exclui uma lista específica vinculada ao dispositivo através do endpoint /delete-device-list.
     * Envia estritamente device_id, key e list_id.
     * Não imprime dados sensíveis em logs.
     */
    fun deleteDeviceList(deviceId: String, key: String, listId: String): LcAdminDeleteResponse {
        Log.d(TAG, "Início da requisição para excluir lista [URL: $ENDPOINT_DELETE_LIST_URL, list_id: $listId]")

        val payload = JSONObject().apply {
            put("device_id", deviceId)
            put("key", key)
            put("list_id", listId)
        }.toString()

        val request = Request.Builder()
            .url(ENDPOINT_DELETE_LIST_URL)
            .addHeader("Content-Type", "application/json")
            .addHeader("apikey", API_KEY)
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val httpCode = response.code
                val bodyString = response.body?.string().orEmpty()

                Log.d(TAG, "HTTP status /delete-device-list: $httpCode")

                when (httpCode) {
                    200 -> {
                        val parsedJson = try {
                            JSONObject(bodyString)
                        } catch (e: Exception) {
                            JSONObject()
                        }
                        val status = parsedJson.optString("status", "").lowercase()
                        Log.d(TAG, "Status retornado em /delete-device-list: '$status'")

                        when (status) {
                            "deleted", "removido", "excluido" -> {
                                LcAdminDeleteResponse.Success(
                                    httpCode = 200,
                                    status = status,
                                    displayMessage = "Lista excluída"
                                )
                            }
                            "not_registered" -> {
                                Log.w(TAG, "Dispositivo não registrado ao tentar excluir lista")
                                LcAdminDeleteResponse.NotRegistered(
                                    displayMessage = "Não foi possível validar o dispositivo"
                                )
                            }
                            "not_found" -> {
                                Log.w(TAG, "Lista não encontrada para exclusão (not_found)")
                                LcAdminDeleteResponse.NotFound(
                                    displayMessage = "Esta lista não está mais disponível"
                                )
                            }
                            else -> {
                                LcAdminDeleteResponse.Success(
                                    httpCode = 200,
                                    status = status,
                                    displayMessage = "Lista excluída"
                                )
                            }
                        }
                    }
                    400 -> {
                        Log.w(TAG, "Erro HTTP 400 ao excluir lista (invalid_request)")
                        LcAdminDeleteResponse.Error(
                            httpCode = 400,
                            displayMessage = "Não foi possível excluir a lista"
                        )
                    }
                    401 -> {
                        Log.w(TAG, "Erro HTTP 401 ao excluir lista (unauthorized)")
                        LcAdminDeleteResponse.Error(
                            httpCode = 401,
                            displayMessage = "Erro de autorização"
                        )
                    }
                    429 -> {
                        Log.w(TAG, "Erro HTTP 429 ao excluir lista (too_many_requests)")
                        LcAdminDeleteResponse.Error(
                            httpCode = 429,
                            displayMessage = "Muitas tentativas. Tente novamente em alguns instantes."
                        )
                    }
                    503 -> {
                        Log.w(TAG, "Erro HTTP 503 ao excluir lista (unavailable)")
                        LcAdminDeleteResponse.Error(
                            httpCode = 503,
                            displayMessage = "Serviço temporariamente indisponível"
                        )
                    }
                    else -> {
                        Log.w(TAG, "Código inesperado ao excluir lista: $httpCode")
                        LcAdminDeleteResponse.Error(
                            httpCode = httpCode,
                            displayMessage = "Não foi possível excluir a lista"
                        )
                    }
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Timeout ao excluir lista", e)
            LcAdminDeleteResponse.Error(
                httpCode = null,
                displayMessage = "Erro de conexão",
                isNetworkOrTimeout = true,
                technicalException = e
            )
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Falha de DNS ao excluir lista", e)
            LcAdminDeleteResponse.Error(
                httpCode = null,
                displayMessage = "Erro de conexão",
                isNetworkOrTimeout = true,
                technicalException = e
            )
        } catch (e: IOException) {
            Log.e(TAG, "Falha de conexão de rede ao excluir lista", e)
            LcAdminDeleteResponse.Error(
                httpCode = null,
                displayMessage = "Erro de conexão",
                isNetworkOrTimeout = true,
                technicalException = e
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exceção inesperada ao excluir lista", e)
            LcAdminDeleteResponse.Error(
                httpCode = null,
                displayMessage = "Erro de conexão",
                isNetworkOrTimeout = true,
                technicalException = e
            )
        }
    }
}
