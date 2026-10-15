package com.example.util

/**
 * Utilitário de detecção conservadora de conteúdo adulto para o Controle dos Pais do LC Player.
 */
object ParentalControlManager {

    private val ADULT_KEYWORDS = listOf(
        "adulto", "adultos", "xxx", "18+", "+18", "porn", "porno",
        "erotic", "erotico", "erótica", "erotica", "for adults", "18 +", "+ 18"
    )

    /**
     * Identifica conservadoramente se o nome de uma categoria ou canal indica conteúdo adulto.
     * Aplica normalização de maiúsculas, minúsculas e acentos.
     */
    fun isAdultContentName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false

        val normalized = name.lowercase()
            .replace("á", "a").replace("ã", "a").replace("â", "a")
            .replace("é", "e").replace("ê", "e")
            .replace("í", "i").replace("ó", "o").replace("ô", "o").replace("õ", "o")
            .replace("ú", "u")

        return ADULT_KEYWORDS.any { keyword ->
            normalized.contains(keyword)
        }
    }
}
