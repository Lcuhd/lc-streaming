package com.example.domain.model

/**
 * Tipos de conteúdo suportados na API Xtream Codes para consulta de categorias.
 */
enum class CategoryType(val displayName: String, val actionParam: String) {
    LIVE("TV ao vivo", "get_live_categories"),
    VOD("Filmes", "get_vod_categories"),
    SERIES("Séries", "get_series_categories")
}

/**
 * Modelo de categoria retornado pelo servidor Xtream.
 *
 * Suporta category_id como texto ou número, category_name, parent_id opcional
 * e contagem real de itens (canais, filmes ou séries) associados.
 */
data class XtreamCategory(
    val categoryId: String,
    val categoryName: String,
    val parentId: Int? = null,
    val type: CategoryType,
    val itemCount: Int? = null
)

/**
 * Resultado detalhado da consulta de categorias para um determinado tipo de conteúdo.
 * Separa explicitamente estados de sucesso, lista vazia, erro de interpretação/parsing,
 * falha de rede/servidor e falha de autenticação.
 */
sealed class CategoryLoadResult {
    data class Success(
        val categories: List<XtreamCategory>,
        val type: CategoryType
    ) : CategoryLoadResult()

    data class Empty(
        val displayMessage: String,
        val type: CategoryType
    ) : CategoryLoadResult()

    data class ParseError(
        val displayMessage: String,
        val type: CategoryType
    ) : CategoryLoadResult()

    data class NetworkError(
        val displayMessage: String,
        val type: CategoryType
    ) : CategoryLoadResult()

    data class AuthError(
        val displayMessage: String,
        val type: CategoryType
    ) : CategoryLoadResult()

    data class Error(
        val displayMessage: String,
        val type: CategoryType,
        val isAuthError: Boolean = false
    ) : CategoryLoadResult()
}

/**
 * Estado em memória do catálogo de categorias associado estritamente à lista atualmente selecionada.
 * Garante separação rigorosa entre Live TV, Filmes e Séries.
 * Quando o usuário troca de lista, este estado é completamente resetado ou substituído,
 * garantindo que categorias da lista anterior nunca sejam misturadas com a nova lista.
 */
data class ListContentCategoriesState(
    val listId: String,
    val listTitle: String,
    val sourceName: String = "",
    val liveCategories: List<XtreamCategory> = emptyList(),
    val vodCategories: List<XtreamCategory> = emptyList(),
    val seriesCategories: List<XtreamCategory> = emptyList(),
    val isLoadingLive: Boolean = false,
    val isLoadingVod: Boolean = false,
    val isLoadingSeries: Boolean = false,
    val liveError: String? = null,
    val vodError: String? = null,
    val seriesError: String? = null,
    val selectedTab: CategoryType = CategoryType.LIVE,
    val selectedCategoryForChannels: XtreamCategory? = null,
    val liveStreamsState: LiveStreamsState? = null
) {
    val isViewingChannels: Boolean
        get() = selectedCategoryForChannels != null
    val isAnyLoading: Boolean
        get() = isLoadingLive || isLoadingVod || isLoadingSeries

    fun getCategoriesForType(type: CategoryType): List<XtreamCategory> = when (type) {
        CategoryType.LIVE -> liveCategories
        CategoryType.VOD -> vodCategories
        CategoryType.SERIES -> seriesCategories
    }

    fun getErrorForType(type: CategoryType): String? = when (type) {
        CategoryType.LIVE -> liveError
        CategoryType.VOD -> vodError
        CategoryType.SERIES -> seriesError
    }

    fun isLoadingForType(type: CategoryType): Boolean = when (type) {
        CategoryType.LIVE -> isLoadingLive
        CategoryType.VOD -> isLoadingVod
        CategoryType.SERIES -> isLoadingSeries
    }

    fun getCountForType(type: CategoryType): Int = when (type) {
        CategoryType.LIVE -> liveCategories.size
        CategoryType.VOD -> vodCategories.size
        CategoryType.SERIES -> seriesCategories.size
    }
}
