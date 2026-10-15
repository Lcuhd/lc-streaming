package com.example.util

import java.text.Normalizer

/**
 * Helper utilitário para busca e filtragem contextual no LC Player.
 * Normaliza textos ignorando maiúsculas/minúsculas, acentos e caracteres especiais.
 */
object SearchUtils {

    /**
     * Normaliza uma string removendo acentos e convertendo para minúsculas.
     * Exemplo: "Ação" -> "acao"
     */
    fun normalizeForSearch(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val normalized = Normalizer.normalize(text.lowercase().trim(), Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    /**
     * Verifica se o texto contém o termo de busca ignorando acentos e maiúsculas.
     */
    fun matchesSearchQuery(text: String?, query: String): Boolean {
        val cleanQuery = normalizeForSearch(query)
        if (cleanQuery.isBlank()) return true
        if (text.isNullOrBlank()) return false
        return normalizeForSearch(text).contains(cleanQuery)
    }
}
