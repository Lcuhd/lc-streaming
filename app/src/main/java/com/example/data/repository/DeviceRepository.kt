package com.example.data.repository

import android.content.Context
import com.example.data.generator.DeviceIdentifierGenerator
import com.example.data.local.DeviceIdentityStorage
import com.example.data.local.dao.DeviceInfoDao
import com.example.data.local.dao.FavoriteDao
import com.example.data.local.dao.ParentalProtectionDao
import com.example.data.local.dao.SeriesProgressDao
import com.example.data.local.entity.DeviceInfoEntity
import com.example.data.local.entity.FavoriteEntity
import com.example.data.local.entity.ParentalProtectionEntity
import com.example.data.local.entity.SeriesProgressEntity
import com.example.data.remote.LcAdminApiClient
import com.example.data.remote.LcAdminDeleteResponse
import com.example.data.remote.LcAdminListsResponse
import com.example.data.remote.LcAdminResponse
import com.example.data.remote.XtreamAuthClient
import com.example.data.remote.XtreamCategoryClient
import com.example.data.remote.XtreamLiveStreamsClient
import com.example.data.remote.XtreamSeriesClient
import com.example.data.remote.XtreamVodStreamsClient
import com.example.domain.model.ActivationStatus
import com.example.domain.model.CategoryLoadResult
import com.example.domain.model.CategoryType
import com.example.domain.model.DeviceCredentials
import com.example.domain.model.DeviceListModel
import com.example.domain.model.ListStatus
import com.example.domain.model.LiveStreamsLoadResult
import com.example.domain.model.SeriesDetailsLoadResult
import com.example.domain.model.SeriesLoadResult
import com.example.domain.model.VodDetailsLoadResult
import com.example.domain.model.VodStreamsLoadResult
import com.example.domain.model.XtreamAuthResult
import com.example.domain.model.XtreamCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repositório responsável por gerenciar a identidade, persistência do dispositivo
 * e comunicação com o LC Admin e servidores de conteúdo Xtream.
 */
class DeviceRepository(
    private val context: Context,
    private val deviceInfoDao: DeviceInfoDao,
    private val seriesProgressDao: SeriesProgressDao? = null,
    private val favoriteDao: FavoriteDao? = null,
    private val parentalProtectionDao: ParentalProtectionDao? = null,
    private val apiClient: LcAdminApiClient = LcAdminApiClient(),
    private val xtreamAuthClient: XtreamAuthClient = XtreamAuthClient(),
    private val xtreamCategoryClient: XtreamCategoryClient = XtreamCategoryClient(),
    private val xtreamLiveStreamsClient: XtreamLiveStreamsClient = XtreamLiveStreamsClient(),
    private val xtreamVodStreamsClient: XtreamVodStreamsClient = XtreamVodStreamsClient(),
    private val xtreamSeriesClient: XtreamSeriesClient = XtreamSeriesClient(),
    private val identityStorage: DeviceIdentityStorage = DeviceIdentityStorage(context, deviceInfoDao)
) {

    /**
     * Flow observável das credenciais do dispositivo.
     */
    val deviceCredentialsFlow: Flow<DeviceCredentials?> = deviceInfoDao.getDeviceInfoFlow()
        .map { entity ->
            entity?.let {
                DeviceCredentials(
                    deviceId = it.deviceId,
                    key = it.activationKey,
                    status = ActivationStatus.fromStorage(it.status),
                    createdAt = it.createdAt,
                    lastCheckedAt = it.lastCheckedAt
                )
            }
        }

    /**
     * Retorna as credenciais persistidas através do armazenamento dual (SharedPreferences e Room).
     *
     * Regras estritas:
     * - Se existir um Device ID e/ou KEY já persistidos, reutiliza-os SEMPRE.
     * - NUNCA sobrescreve valores persistidos com recálculos determinísticos.
     * - Somente gera nova identidade na primeiríssima execução em dispositivo limpo.
     * - Nunca regenera em abertura, refresh, mudança de rede ou atualização normal.
     */
    suspend fun getOrCreateDeviceCredentials(): DeviceCredentials =
        identityStorage.getOrInitializeCredentials()

    /**
     * Realiza a chamada HTTP real ao LC Admin utilizando o Device ID e KEY persistentes.
     * Atualiza o status no armazenamento local sincronizado caso retorne com sucesso.
     */
    suspend fun checkDeviceStatusOnline(deviceId: String, key: String): LcAdminResponse = withContext(Dispatchers.IO) {
        val response = apiClient.checkDeviceStatus(deviceId, key)
        val now = System.currentTimeMillis()

        when (response) {
            is LcAdminResponse.Success -> {
                // Atualiza o status sincronizado em ambas as camadas de persistência
                identityStorage.updateStatus(
                    status = response.activationStatus,
                    lastCheckedAt = now
                )
            }
            is LcAdminResponse.Error -> {
                // Caso ocorra falha de rede/timeout, registra timestamp da tentativa
                if (response.isNetworkOrTimeout) {
                    identityStorage.updateStatus(
                        status = ActivationStatus.CONNECTION_ERROR,
                        lastCheckedAt = now
                    )
                }
            }
        }

        response
    }

    /**
     * Consulta as listas vinculadas ao dispositivo no LC Admin.
     * Preserva Device ID e KEY atuais.
     */
    suspend fun fetchDeviceListsOnline(deviceId: String, key: String): LcAdminListsResponse = withContext(Dispatchers.IO) {
        val response = apiClient.fetchDeviceLists(deviceId, key)
        val now = System.currentTimeMillis()

        if (response is LcAdminListsResponse.Success) {
            deviceInfoDao.updateStatus(
                status = response.deviceStatus.name,
                lastCheckedAt = now
            )
        }

        response
    }

    /**
     * Exclui uma lista vinculada ao dispositivo através do endpoint /delete-device-list.
     * Envia device_id, key e list_id. Preserva Device ID e KEY atuais.
     */
    suspend fun deleteDeviceListOnline(deviceId: String, key: String, listId: String): LcAdminDeleteResponse = withContext(Dispatchers.IO) {
        apiClient.deleteDeviceList(deviceId, key, listId)
    }

    /**
     * Testa a autenticação direta com o servidor Xtream usando as credenciais da lista.
     * Somente permitido para listas ativas.
     */
    suspend fun testXtreamAuthenticationOnline(list: DeviceListModel): XtreamAuthResult = withContext(Dispatchers.IO) {
        if (list.status != ListStatus.ACTIVE) {
            val label = list.status.label
            return@withContext XtreamAuthResult.NotAllowed("Lista $label. Teste de conexão não permitido.")
        }
        xtreamAuthClient.testAuthentication(list.dns, list.username, list.password)
    }

    /**
     * Consulta as categorias reais disponíveis no servidor Xtream para a lista ativa selecionada.
     * Requisito 2: Somente permitido se a lista estiver com status 'active'.
     * Categorias de TV ao vivo, Filmes e Séries são tratadas separadamente.
     */
    suspend fun fetchCategoriesOnline(list: DeviceListModel, type: CategoryType): CategoryLoadResult = withContext(Dispatchers.IO) {
        if (list.status != ListStatus.ACTIVE) {
            val label = list.status.label
            return@withContext CategoryLoadResult.Error(
                displayMessage = "Lista $label. Consulta de categorias não permitida.",
                type = type
            )
        }
        xtreamCategoryClient.fetchCategories(list.dns, list.username, list.password, type)
    }

    /**
     * Consulta os canais reais pertencentes a uma categoria de TV ao vivo no servidor Xtream.
     * Somente permitido para listas ativas.
     */
    suspend fun fetchLiveStreamsForCategoryOnline(
        list: DeviceListModel,
        category: XtreamCategory
    ): LiveStreamsLoadResult = withContext(Dispatchers.IO) {
        if (list.status != ListStatus.ACTIVE) {
            val label = list.status.label
            return@withContext LiveStreamsLoadResult.Error(
                displayMessage = "Lista $label. Consulta não permitida.",
                categoryId = category.categoryId
            )
        }
        xtreamLiveStreamsClient.fetchLiveStreamsForCategory(
            dns = list.dns,
            username = list.username,
            password = list.password,
            categoryId = category.categoryId,
            categoryName = category.categoryName
        )
    }

    /**
     * Consulta os filmes reais (VOD) pertencentes a uma categoria no servidor Xtream.
     * Somente permitido para listas ativas.
     */
    suspend fun fetchVodStreamsForCategoryOnline(
        list: DeviceListModel,
        category: XtreamCategory
    ): VodStreamsLoadResult = withContext(Dispatchers.IO) {
        if (list.status != ListStatus.ACTIVE) {
            val label = list.status.label
            return@withContext VodStreamsLoadResult.Error(
                displayMessage = "Lista $label. Consulta não permitida.",
                categoryId = category.categoryId
            )
        }
        xtreamVodStreamsClient.fetchVodStreamsForCategory(
            dns = list.dns,
            username = list.username,
            password = list.password,
            categoryId = category.categoryId,
            categoryName = category.categoryName
        )
    }

    /**
     * Consulta os detalhes completos de um filme VOD (action=get_vod_info).
     * Somente permitido para listas ativas.
     */
    suspend fun fetchVodDetailsOnline(
        list: DeviceListModel,
        vodId: String
    ): VodDetailsLoadResult = withContext(Dispatchers.IO) {
        if (list.status != ListStatus.ACTIVE) {
            val label = list.status.label
            return@withContext VodDetailsLoadResult.Error(
                displayMessage = "Lista $label. Consulta não permitida."
            )
        }
        xtreamVodStreamsClient.fetchVodDetails(
            dns = list.dns,
            username = list.username,
            password = list.password,
            vodId = vodId
        )
    }

    /**
     * Consulta a lista de séries (action=get_series) pertencentes a uma categoria no servidor Xtream.
     */
    suspend fun fetchSeriesForCategoryOnline(
        list: DeviceListModel,
        category: XtreamCategory
    ): SeriesLoadResult = withContext(Dispatchers.IO) {
        if (list.status != ListStatus.ACTIVE) {
            val label = list.status.label
            return@withContext SeriesLoadResult.Error(
                displayMessage = "Lista $label. Consulta não permitida.",
                categoryId = category.categoryId
            )
        }
        xtreamSeriesClient.fetchSeriesForCategory(
            dns = list.dns,
            username = list.username,
            password = list.password,
            categoryId = category.categoryId,
            categoryName = category.categoryName
        )
    }

    /**
     * Consulta os detalhes de uma série (action=get_series_info) incluindo temporadas e episódios.
     */
    suspend fun fetchSeriesDetailsOnline(
        list: DeviceListModel,
        seriesId: String
    ): SeriesDetailsLoadResult = withContext(Dispatchers.IO) {
        if (list.status != ListStatus.ACTIVE) {
            val label = list.status.label
            return@withContext SeriesDetailsLoadResult.Error(
                displayMessage = "Lista $label. Consulta não permitida."
            )
        }
        xtreamSeriesClient.fetchSeriesDetails(
            dns = list.dns,
            username = list.username,
            password = list.password,
            seriesId = seriesId
        )
    }

    /**
     * Salva o progresso local de reprodução de um filme VOD ou episódio de série.
     */
    suspend fun saveEpisodeProgress(
        seriesId: String,
        episodeId: String,
        seasonNum: Int,
        episodeNum: Int,
        positionMs: Long,
        durationMs: Long,
        completed: Boolean,
        progressKey: String = episodeId,
        listId: String? = null,
        contentType: String? = null,
        contentId: String? = null,
        contentTitle: String? = null,
        contentImage: String? = null
    ) = withContext(Dispatchers.IO) {
        val dao = seriesProgressDao ?: return@withContext
        val entry = SeriesProgressEntity(
            progressKey = progressKey,
            listId = listId,
            contentType = contentType,
            contentId = contentId,
            contentTitle = contentTitle,
            contentImage = contentImage,
            episodeId = episodeId,
            seriesId = seriesId,
            seasonNum = seasonNum,
            episodeNum = episodeNum,
            positionMs = positionMs,
            durationMs = durationMs,
            completed = completed,
            updatedAt = System.currentTimeMillis()
        )
        dao.saveProgress(entry)
        android.util.Log.i(
            "LCPlayer_SeriesProgress",
            "Progresso salvo: key=$progressKey, listId=$listId, type=$contentType, contentId=$contentId, series_id=$seriesId, episode_id=$episodeId, season=$seasonNum, ep=$episodeNum, pos=${positionMs}ms, duration=${durationMs}ms, completed=$completed"
        )
    }

    /**
     * Resgata o histórico local de progresso de episódios de uma série para a lista ativa.
     * Combina os registros list-scoped com registros legados (listId IS NULL), deduplicando por episodeId.
     */
    suspend fun getSeriesProgress(listId: String, seriesId: String): List<SeriesProgressEntity> = withContext(Dispatchers.IO) {
        val dao = seriesProgressDao ?: return@withContext emptyList()
        val listScoped = dao.getProgressForListSeries(listId, seriesId)
        val legacy = dao.getLegacyProgressForSeries(seriesId)

        if (legacy.isEmpty()) {
            listScoped
        } else {
            val scopedEpisodeIds = listScoped.map { it.episodeId }.toSet()
            val filteredLegacy = legacy.filter { it.episodeId !in scopedEpisodeIds }
            (listScoped + filteredLegacy).sortedByDescending { it.updatedAt }
        }
    }

    /**
     * Resgata o histórico local de progresso de episódios de uma série.
     */
    suspend fun getSeriesProgress(seriesId: String): List<SeriesProgressEntity> = withContext(Dispatchers.IO) {
        seriesProgressDao?.getProgressForSeries(seriesId) ?: emptyList()
    }

    /**
     * Resgata o Flow observável de progresso local de episódios de uma série.
     */
    fun getSeriesProgressFlow(seriesId: String): Flow<List<SeriesProgressEntity>> {
        return seriesProgressDao?.getProgressFlowForSeries(seriesId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    /**
     * Resgata o progresso salvo de um episódio específico por chave ou episodeId.
     */
    suspend fun getEpisodeProgress(progressKeyOrEpisodeId: String): SeriesProgressEntity? = withContext(Dispatchers.IO) {
        val dao = seriesProgressDao ?: return@withContext null
        dao.getProgressByKey(progressKeyOrEpisodeId) ?: dao.getProgressForEpisode(progressKeyOrEpisodeId)
    }

    /**
     * Limpa o progresso salvo de uma série para a lista ativa e registros legados.
     */
    suspend fun clearSeriesProgress(listId: String, seriesId: String) = withContext(Dispatchers.IO) {
        val dao = seriesProgressDao ?: return@withContext
        dao.clearSeriesProgressForList(listId, seriesId)
        dao.clearLegacySeriesProgress(seriesId)
    }

    /**
     * Limpa o progresso salvo de uma série.
     */
    suspend fun clearSeriesProgress(seriesId: String) = withContext(Dispatchers.IO) {
        seriesProgressDao?.clearProgressForSeries(seriesId)
    }

    /**
     * Retorna os conteúdos em andamento recentes para a lista ativa, deduplicados por (contentType + contentId).
     */
    suspend fun getRecentInProgressForList(listId: String, limit: Int = 12): List<SeriesProgressEntity> = withContext(Dispatchers.IO) {
        val dao = seriesProgressDao ?: return@withContext emptyList()
        val rawList = dao.getRecentInProgressForList(listId, limit = 50)

        val seenKeys = mutableSetOf<String>()
        val result = mutableListOf<SeriesProgressEntity>()

        for (item in rawList) {
            val key = "${item.contentType}_${item.contentId}"
            if (seenKeys.add(key)) {
                result.add(item)
                if (result.size >= limit) break
            }
        }
        result
    }

    /**
     * Atualiza o timestamp de verificação local.
     */
    suspend fun refreshLocalStatus() = withContext(Dispatchers.IO) {
        val current = deviceInfoDao.getDeviceInfoDirect() ?: return@withContext
        val now = System.currentTimeMillis()
        deviceInfoDao.updateStatus(
            status = current.status,
            lastCheckedAt = now
        )
    }

    /**
     * Alterna o estado de favorito de um conteúdo (LIVE, VOD ou SERIES) para a lista ativa.
     * Retorna true se foi adicionado aos favoritos ou false se foi removido.
     */
    suspend fun toggleFavorite(listId: String, contentType: String, contentId: String): Boolean = withContext(Dispatchers.IO) {
        val dao = favoriteDao ?: return@withContext false
        val favoriteId = "${listId}_${contentType}_${contentId}"
        val exists = dao.isFavorite(favoriteId)
        if (exists) {
            dao.removeFavoriteById(favoriteId)
            false
        } else {
            dao.addFavorite(FavoriteEntity(favoriteId, listId, contentType, contentId))
            true
        }
    }

    /**
     * Verifica se um conteúdo específico é favorito na lista ativa.
     */
    suspend fun isFavorite(listId: String, contentType: String, contentId: String): Boolean = withContext(Dispatchers.IO) {
        val dao = favoriteDao ?: return@withContext false
        val favoriteId = "${listId}_${contentType}_${contentId}"
        dao.isFavorite(favoriteId)
    }

    /**
     * Retorna o conjunto de IDs favoritados para o tipo de conteúdo e lista informados.
     */
    suspend fun getFavoriteIds(listId: String, contentType: String): Set<String> = withContext(Dispatchers.IO) {
        val dao = favoriteDao ?: return@withContext emptySet()
        dao.getFavorites(listId, contentType).map { it.contentId }.toSet()
    }

    /**
     * Adiciona uma proteção manual do Controle dos Pais para uma categoria ou canal.
     */
    suspend fun addParentalProtection(listId: String, protectionType: String, contentType: String, targetId: String) = withContext(Dispatchers.IO) {
        val dao = parentalProtectionDao ?: return@withContext
        val id = "${listId}_${protectionType}_${contentType}_${targetId}"
        dao.addProtection(ParentalProtectionEntity(id, listId, protectionType, contentType, targetId))
    }

    /**
     * Remove uma proteção manual do Controle dos Pais.
     */
    suspend fun removeParentalProtection(listId: String, protectionType: String, contentType: String, targetId: String) = withContext(Dispatchers.IO) {
        val dao = parentalProtectionDao ?: return@withContext
        val id = "${listId}_${protectionType}_${contentType}_${targetId}"
        dao.removeProtectionById(id)
    }

    /**
     * Verifica se uma categoria ou canal possui proteção manual ativa.
     */
    suspend fun isContentProtected(listId: String, protectionType: String, contentType: String, targetId: String): Boolean = withContext(Dispatchers.IO) {
        val dao = parentalProtectionDao ?: return@withContext false
        val id = "${listId}_${protectionType}_${contentType}_${targetId}"
        dao.isProtected(id)
    }

    /**
     * Retorna a lista de proteções manuais ativas para a lista informada.
     */
    suspend fun getProtectionsForList(listId: String): List<ParentalProtectionEntity> = withContext(Dispatchers.IO) {
        parentalProtectionDao?.getProtectionsForList(listId) ?: emptyList()
    }
}
