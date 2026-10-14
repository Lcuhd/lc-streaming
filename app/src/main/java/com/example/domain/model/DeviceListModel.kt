package com.example.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
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
 * Formata a data de validade da ativação no LC Player respeitando a regra centralizada de exibição:
 * 1. Data nula, vazia ou em branco -> "Ilimitado"
 * 2. Data no futuro a 10 anos ou mais em relação à data atual -> "Ilimitado"
 * 3. Data no futuro a menos de 10 anos -> data real formatada em "dd/MM/yyyy"
 * 4. Data vencida no passado -> data real formatada em "dd/MM/yyyy" (não ilimitado)
 *
 * O valor bruto retornado pela API permanece 100% inalterado.
 */
fun formatAppExpiration(rawDate: String?): String {
    if (rawDate.isNullOrBlank()) return "Ilimitado"

    return try {
        val datePart = if (rawDate.contains("T")) {
            rawDate.substringBefore("T")
        } else {
            rawDate.take(10)
        }

        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsedDate = parser.parse(datePart) ?: return "Ilimitado"

        val tenYearsFromNow = Calendar.getInstance().apply {
            add(Calendar.YEAR, 10)
        }.time

        if (!parsedDate.before(tenYearsFromNow)) {
            "Ilimitado"
        } else {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formatter.format(parsedDate)
        }
    } catch (_: Exception) {
        "Ilimitado"
    }
}

/**
 * Alias mantido para compatibilidade retroativa com todo o projeto.
 */
fun formatExpiresAt(rawDate: String?): String = formatAppExpiration(rawDate)
