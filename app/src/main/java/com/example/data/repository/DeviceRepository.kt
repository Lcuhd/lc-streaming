package com.example.data.repository

import android.content.Context
import com.example.data.generator.DeviceIdentifierGenerator
import com.example.data.local.DeviceIdentityStorage
import com.example.data.local.dao.DeviceInfoDao
import com.example.data.local.dao.SeriesProgressDao
import com.example.data.local.entity.DeviceInfoEntity
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
     * Salva o progresso local de reprodução de um episódio de série.
     */
    suspend fun saveEpisodeProgress(
        seriesId: String,
        episodeId: String,
        seasonNum: Int,
        episodeNum: Int,
        positionMs: Long,
        durationMs: Long,
        completed: Boolean
    ) = withContext(Dispatchers.IO) {
        val dao = seriesProgressDao ?: return@withContext
        val entry = SeriesProgressEntity(
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
            "Progresso salvo: series_id=$seriesId, episode_id=$episodeId, season=$seasonNum, ep=$episodeNum, pos=${positionMs}ms, duration=${durationMs}ms, completed=$completed"
        )
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
     * Resgata o progresso salvo de um episódio específico.
     */
    suspend fun getEpisodeProgress(episodeId: String): SeriesProgressEntity? = withContext(Dispatchers.IO) {
        seriesProgressDao?.getProgressForEpisode(episodeId)
    }

    /**
     * Limpa o progresso salvo de uma série.
     */
    suspend fun clearSeriesProgress(seriesId: String) = withContext(Dispatchers.IO) {
        seriesProgressDao?.clearProgressForSeries(seriesId)
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
}
