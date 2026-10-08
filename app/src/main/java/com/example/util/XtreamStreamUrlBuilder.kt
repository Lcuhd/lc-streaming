package com.example.util

import android.net.Uri
import android.util.Log

/**
 * Utilitário para construção de URLs de reprodução do ecossistema Xtream Codes.
 */
object XtreamStreamUrlBuilder {

    private const val TAG = "LCPlayer_StreamUrl"

    /**
     * Constrói a URL de reprodução de TV ao vivo no padrão Xtream Codes:
     * <DNS>/live/<USERNAME>/<PASSWORD>/<STREAM_ID>.<FORMATO> ou <DNS>/<USERNAME>/<PASSWORD>/<STREAM_ID>
     *
     * @param rawDns DNS ou URL base cadastrada na lista (ex: "http://painel.exemplo.com:8080")
     * @param username Usuário autenticado da lista
     * @param password Senha da lista
     * @param streamId ID numérico ou string do canal
     * @param containerExtension Extensão opcional (padrão "m3u8" ou "ts", ou vazio)
     * @return URL completa pronta para reprodução no ExoPlayer
     */
    fun buildLiveStreamUrl(
        rawDns: String,
        username: String,
        password: String,
        streamId: String,
        containerExtension: String = "m3u8"
    ): String {
        val sanitizedDns = sanitizeDns(rawDns)
        val cleanUser = username.trim()
        val cleanPass = password.trim()
        val cleanStreamId = streamId.trim()

        val extensionPart = if (containerExtension.isNotBlank()) {
            if (containerExtension.startsWith(".")) containerExtension else ".$containerExtension"
        } else {
            ""
        }

        // Padrão Xtream Codes padrão: http://dns:port/live/user/pass/stream_id.m3u8
        return "$sanitizedDns/live/$cleanUser/$cleanPass/$cleanStreamId$extensionPart"
    }

    /**
     * Formata URLs alternativas de fallback caso a primeira extensão falhe (ex: .ts ou sem extensão)
     */
    fun buildAlternativeLiveUrls(
        rawDns: String,
        username: String,
        password: String,
        streamId: String
    ): List<String> {
        val sanitizedDns = sanitizeDns(rawDns)
        val cleanUser = username.trim()
        val cleanPass = password.trim()
        val cleanStreamId = streamId.trim()

        return listOf(
            "$sanitizedDns/live/$cleanUser/$cleanPass/$cleanStreamId.m3u8",
            "$sanitizedDns/live/$cleanUser/$cleanPass/$cleanStreamId.ts",
            "$sanitizedDns/live/$cleanUser/$cleanPass/$cleanStreamId",
            "$sanitizedDns/$cleanUser/$cleanPass/$cleanStreamId"
        )
    }

    /**
     * Sanitiza a URL base / DNS:
     * - Garante prefixo http:// ou https://
     * - Remove barras finais
     * - Trunca paths residuais comuns (/player_api.php, etc.)
     */
    fun sanitizeDns(rawDns: String): String {
        var trimmed = rawDns.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed = "http://$trimmed"
        }

        return try {
            val uri = Uri.parse(trimmed)
            val scheme = uri.scheme ?: "http"
            val host = uri.host ?: ""
            val port = if (uri.port != -1) ":${uri.port}" else ""

            if (host.isNotEmpty()) {
                "$scheme://$host$port"
            } else {
                trimmed.trimEnd('/')
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao sanitizar URI, utilizando fallback", e)
            trimmed.trimEnd('/')
        }
    }
}
