package com.example.data.remote

import android.util.Log
import com.example.domain.model.XtreamAuthResult
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Cliente responsável pela autenticação direta com servidores compatíveis com a API Xtream.
 *
 * Constrói a URL no formato:
 * <DNS>/player_api.php?username=<USERNAME>&password=<PASSWORD>
 *
 * NUNCA escreve username ou password em logs ou saídas do sistema.
 */
class XtreamAuthClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .build()
) {

    companion object {
        private const val TAG = "LCPlayer_Xtream"

        /**
         * Constrói a URL de autenticação Xtream de forma segura e normalizada:
         * - Preserva rigorosamente o protocolo (http:// ou https://) cadastrado
         * - Remove barras finais duplicadas do DNS
         * - Realiza URL encoding no username e password
         */
        fun buildAuthUrl(dns: String, username: String, password: String): String {
            val cleanDns = dns.trim().trimEnd('/')
            val encodedUser = URLEncoder.encode(username.trim(), "UTF-8")
            val encodedPass = URLEncoder.encode(password.trim(), "UTF-8")
            return "$cleanDns/player_api.php?username=$encodedUser&password=$encodedPass"
        }
    }

    /**
     * Executa a autenticação com o servidor Xtream e valida a estrutura da resposta.
     */
    fun testAuthentication(dns: String, username: String, password: String): XtreamAuthResult {
        if (dns.isBlank() || username.isBlank() || password.isBlank()) {
            return XtreamAuthResult.InvalidCredentials("Usuário ou senha inválidos")
        }

        val requestUrl = try {
            buildAuthUrl(dns, username, password)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao construir URL de autenticação", e)
            return XtreamAuthResult.Unavailable("Não foi possível conectar ao servidor")
        }

        // Extrai apenas o host para fins de log de depuração, sem expor login/senha
        val hostPreview = try {
            val clean = dns.trim().removePrefix("http://").removePrefix("https://").substringBefore("/")
            clean
        } catch (_: Exception) {
            "servidor"
        }

        Log.d(TAG, "Iniciando teste de autenticação Xtream com o host: $hostPreview")

        val request = Request.Builder()
            .url(requestUrl)
            .header("User-Agent", "LCPlayer/1.0 (Android TV)")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val httpCode = response.code
                val bodyString = response.body?.string().orEmpty().trim()

                Log.d(TAG, "HTTP status recebido do servidor Xtream: $httpCode")

                // Servidores com erro interno (5xx)
                if (httpCode in 500..599) {
                    Log.w(TAG, "Servidor retornou erro interno HTTP $httpCode")
                    return XtreamAuthResult.Unavailable("Não foi possível conectar ao servidor")
                }

                if (httpCode == 401 || httpCode == 403) {
                    Log.w(TAG, "Servidor retornou código HTTP de acesso negado ($httpCode)")
                    return XtreamAuthResult.InvalidCredentials("Usuário ou senha inválidos")
                }

                if (httpCode != 200) {
                    Log.w(TAG, "Servidor retornou código inesperado: $httpCode")
                    return XtreamAuthResult.Unavailable("Não foi possível conectar ao servidor")
                }

                // Validação estrita da estrutura da resposta
                parseXtreamResponse(bodyString)
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Timeout ao conectar ao servidor Xtream", e)
            XtreamAuthResult.Unavailable("Não foi possível conectar ao servidor")
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Falha ao resolver DNS do servidor Xtream", e)
            XtreamAuthResult.Unavailable("Não foi possível conectar ao servidor")
        } catch (e: ConnectException) {
            Log.e(TAG, "Conexão recusada pelo servidor Xtream", e)
            XtreamAuthResult.Unavailable("Não foi possível conectar ao servidor")
        } catch (e: IOException) {
            Log.e(TAG, "Erro I/O de rede ao comunicar com servidor Xtream", e)
            XtreamAuthResult.Unavailable("Não foi possível conectar ao servidor")
        } catch (e: Exception) {
            Log.e(TAG, "Exceção inesperada durante autenticação Xtream", e)
            XtreamAuthResult.Unavailable("Não foi possível conectar ao servidor")
        }
    }

    private fun parseXtreamResponse(bodyString: String): XtreamAuthResult {
        if (bodyString.isEmpty() || !bodyString.startsWith("{")) {
            Log.w(TAG, "Resposta do servidor não é um JSON válido")
            return XtreamAuthResult.IncompatibleResponse("Resposta do servidor não reconhecida")
        }

        val json = try {
            JSONObject(bodyString)
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao decodificar JSON da resposta do servidor", e)
            return XtreamAuthResult.IncompatibleResponse("Resposta do servidor não reconhecida")
        }

        // Deve conter o objeto obrigatório user_info
        if (!json.has("user_info") || json.isNull("user_info")) {
            Log.w(TAG, "JSON não contém o campo 'user_info' esperado na API Xtream")
            return XtreamAuthResult.IncompatibleResponse("Resposta do servidor não reconhecida")
        }

        val userInfo = json.optJSONObject("user_info")
        if (userInfo == null) {
            Log.w(TAG, "'user_info' não é um objeto JSON")
            return XtreamAuthResult.IncompatibleResponse("Resposta do servidor não reconhecida")
        }

        val auth = userInfo.opt("auth")
        val status = userInfo.optString("status", "").trim()

        val isAuthorized = when (auth) {
            is Int -> auth == 1
            is Long -> auth == 1L
            is String -> auth == "1" || auth.equals("true", ignoreCase = true)
            is Boolean -> auth
            else -> false
        } || status.equals("Active", ignoreCase = true)

        return if (isAuthorized) {
            Log.i(TAG, "Autenticação Xtream realizada com sucesso! Status: $status")

            val rawExpDate = if (userInfo.isNull("exp_date")) null else userInfo.optString("exp_date", null)
            val formattedExp = formatXtreamExpDate(rawExpDate)
            val maxConnections = if (userInfo.isNull("max_connections")) null else userInfo.optString("max_connections", null)

            val accountStatus = if (status.isNotBlank()) status else "Ativa"

            XtreamAuthResult.Success(
                displayMessage = "Servidor conectado",
                accountStatus = accountStatus,
                expiration = formattedExp,
                maxConnections = maxConnections
            )
        } else {
            Log.w(TAG, "Autenticação Xtream falhou: auth=$auth, status=$status")
            XtreamAuthResult.InvalidCredentials("Usuário ou senha inválidos")
        }
    }

    private fun formatXtreamExpDate(raw: String?): String? {
        if (raw.isNullOrBlank() || raw.equals("null", ignoreCase = true)) return null
        return try {
            val seconds = raw.toLongOrNull()
            if (seconds != null && seconds > 0) {
                val date = Date(seconds * 1000L)
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
            } else if (raw.length >= 10 && raw.contains("-")) {
                val datePart = raw.take(10)
                val year = datePart.substring(0, 4)
                val month = datePart.substring(5, 7)
                val day = datePart.substring(8, 10)
                "$day/$month/$year"
            } else {
                raw
            }
        } catch (_: Exception) {
            null
        }
    }
}
