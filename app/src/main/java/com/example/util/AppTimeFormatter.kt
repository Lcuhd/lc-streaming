package com.example.util

import android.content.Context
import com.example.data.local.AppSettingsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilitário centralizado para formatação de horários da interface do LC Player.
 * Respeita a preferência do usuário (24 Horas "18:35" vs 12 Horas "6:35 PM").
 * Preserva estritamente timestamps brutos, fuso horário, datas e durações de vídeo.
 */
object AppTimeFormatter {

    /**
     * Formata um timestamp (em milissegundos) para o relógio da UI.
     */
    fun formatTime(timestampMs: Long, is12Hour: Boolean): String {
        val pattern = if (is12Hour) "h:mm a" else "HH:mm"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }

    /**
     * Formata o horário atual do sistema consultando a preferência do contexto.
     */
    fun formatCurrentTime(context: Context): String {
        val is12Hour = AppSettingsManager.getTimeFormat(context) == "12H"
        return formatTime(System.currentTimeMillis(), is12Hour)
    }

    /**
     * Formata um horário em formato "HH:mm" ou "HH:mm:ss" para 12H ("h:mm a") se a preferência for 12H.
     * Em modo 24H, retorna "HH:mm".
     */
    fun formatDisplayTime(timeStr: String?, is12Hour: Boolean): String {
        if (timeStr.isNullOrBlank()) return ""
        val clean = timeStr.trim()

        if (!is12Hour) {
            return if (clean.length >= 5) clean.take(5) else clean
        }

        return try {
            val target = clean.take(8)
            val parser = if (target.length > 5) SimpleDateFormat("HH:mm:ss", Locale.US) else SimpleDateFormat("HH:mm", Locale.US)
            val parsed = parser.parse(target)
            if (parsed != null) {
                SimpleDateFormat("h:mm a", Locale.US).format(parsed)
            } else {
                clean
            }
        } catch (_: Exception) {
            clean
        }
    }

    /**
     * Formata um intervalo de exibição do EPG (ex: "18:00 - 19:30" -> "6:00 PM - 7:30 PM").
     */
    fun formatEpgTimeRange(startTimeStr: String?, endTimeStr: String?, is12Hour: Boolean): String {
        val startFormatted = formatDisplayTime(startTimeStr, is12Hour)
        val endFormatted = formatDisplayTime(endTimeStr, is12Hour)

        return when {
            startFormatted.isNotBlank() && endFormatted.isNotBlank() -> "$startFormatted - $endFormatted"
            startFormatted.isNotBlank() -> startFormatted
            else -> ""
        }
    }
}
