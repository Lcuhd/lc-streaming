package com.example.domain.model

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Status individual de cada lista atribuída ao dispositivo.
 */
enum class ListStatus(val label: String) {
    ACTIVE("Ativa"),
    BLOCKED("Bloqueada"),
    EXPIRED("Vencida");

    companion object {
        fun fromString(value: String?): ListStatus {
            return when (value?.lowercase()?.trim()) {
                "active", "ativo", "ativa" -> ACTIVE
                "blocked", "bloqueado", "bloqueada" -> BLOCKED
                "expired", "vencido", "vencida" -> EXPIRED
                else -> ACTIVE
            }
        }
    }
}

/**
 * Modelo de lista vinculada ao dispositivo no LC Admin.
 *
 * NOTA DE SEGURANÇA:
 * dns, username e password são mantidos exclusivamente em memória para conexões
 * futuras do player e NUNCA são exibidos na interface gráfica nem impressos em logs.
 */
data class DeviceListModel(
    val id: String,
    val title: String,
    val sourceName: String,
    val dns: String = "",
    val username: String = "",
    val password: String = "",
    val status: ListStatus = ListStatus.ACTIVE,
    val rawStatus: String = "active",
    val expiresAt: String? = null,
    val formattedExpiration: String = formatExpiresAt(expiresAt),
    val displayOrder: Int = 0
)

/**
 * Converte strings ISO 8601 (ex: "2026-11-30T23:59:59Z") para formato legível "30/11/2026".
 * Caso não exista validade definida, retorna "Sem validade definida".
 * Não calcula vencimento localmente; a fonte de verdade é o status retornado pelo backend.
 */
fun formatExpiresAt(rawDate: String?): String {
    if (rawDate.isNullOrBlank()) return "Sem validade definida"
    return try {
        // Extrai a parte da data YYYY-MM-DD
        val datePart = if (rawDate.contains("T")) {
            rawDate.substringBefore("T")
        } else {
            rawDate.take(10)
        }

        if (datePart.length == 10 && datePart[4] == '-' && datePart[7] == '-') {
            val year = datePart.substring(0, 4)
            val month = datePart.substring(5, 7)
            val day = datePart.substring(8, 10)
            "$day/$month/$year"
        } else {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = parser.parse(datePart)
            if (date != null) {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
            } else {
                "Sem validade definida"
            }
        }
    } catch (_: Exception) {
        "Sem validade definida"
    }
}
