package com.example.ui.activation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.remote.LcAdminDeleteResponse
import com.example.data.remote.LcAdminListsResponse
import com.example.data.remote.LcAdminResponse
import com.example.data.repository.DeviceRepository
import com.example.data.remote.XtreamEpgClient
import com.example.domain.model.ActivationStatus
import com.example.domain.model.CategoryLoadResult
import com.example.domain.model.CategoryType
import com.example.domain.model.ConnectionTestDialogState
import com.example.domain.model.DeviceListModel
import com.example.domain.model.ListContentCategoriesState
import com.example.domain.model.ListStatus
import com.example.domain.model.LivePlayerState
import com.example.domain.model.LiveStreamChannel
import com.example.domain.model.LiveStreamsLoadResult
import com.example.domain.model.LiveStreamsState
import com.example.domain.model.ShortEpgFetchResult
import com.example.domain.model.ShortEpgState
import com.example.domain.model.XtreamAuthResult
import com.example.domain.model.EpisodeItem
import com.example.domain.model.SeriesButtonState
import com.example.domain.model.SeriesDetailsLoadResult
import com.example.domain.model.SeriesDetailsState
import com.example.domain.model.SeriesItem
import com.example.domain.model.SeriesLoadResult
import com.example.domain.model.SeriesState
import com.example.domain.model.VodMovie
import com.example.domain.model.VodStreamsLoadResult
import com.example.domain.model.VodStreamsState
import com.example.domain.model.XtreamCategory
import com.example.util.XtreamStreamUrlBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class ActivationViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = DeviceRepository(
        context = application,
        deviceInfoDao = database.deviceInfoDao(),
        seriesProgressDao = database.seriesProgressDao()
    )

    private val xtreamEpgClient = XtreamEpgClient()
    private val epgCache = ConcurrentHashMap<String, Pair<Long, ShortEpgState>>()
    private var epgFetchJob: Job? = null
    private var currentlyFocusedStreamId: String? = null

    private val _uiState = MutableStateFlow(ActivationUiState())
    val uiState: StateFlow<ActivationUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "LCPlayer_Admin"
    }

    init {
        loadOrInitializeDevice()
        observeDeviceChanges()
    }

    private fun loadOrInitializeDevice() {
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true) }
            val credentials = repository.getOrCreateDeviceCredentials()
            _uiState.update {
                it.copy(
                    deviceId = credentials.deviceId,
                    key = credentials.key,
                    status = credentials.status,
                    lastCheckedTimestamp = credentials.lastCheckedAt,
                    isChecking = false
                )
            }

            // Se o aparelho já estiver ativo no banco local, carrega as listas previamente
            if (credentials.status == ActivationStatus.ACTIVE) {
                fetchDeviceLists(navigateToSectionIfActive = false)
            }
        }
    }

    private fun observeDeviceChanges() {
        viewModelScope.launch {
            repository.deviceCredentialsFlow.collect { credentials ->
                if (credentials != null) {
                    _uiState.update {
                        it.copy(
                            deviceId = credentials.deviceId,
                            key = credentials.key,
                            status = credentials.status,
                            lastCheckedTimestamp = credentials.lastCheckedAt
                        )
                    }
                }
            }
        }
    }

    /**
     * Acionado quando o usuário clica no botão "Verificar Ativação".
     * Executa a requisição HTTP POST para o LC Admin com o Device ID e KEY atuais.
     * Preserva integralmente o comportamento original do endpoint /device-status.
     */
    fun onVerifyActivationClicked() {
        if (_uiState.value.isChecking) {
            Log.d(TAG, "Clique no botão 'Verificar' ignorado: requisição já em andamento")
            return
        }

        Log.d(TAG, "Clique no botão 'Verificar' detectado! Iniciando verificação...")

        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, toastFeedback = null) }

            val currentDeviceId = _uiState.value.deviceId
            val currentKey = _uiState.value.key

            Log.d(TAG, "Verificando dispositivo: Device ID='$currentDeviceId', KEY='$currentKey'")

            val response = repository.checkDeviceStatusOnline(currentDeviceId, currentKey)

            when (response) {
                is LcAdminResponse.Success -> {
                    Log.i(TAG, "Resposta de sucesso processada: status='${response.statusString}', novo status Compose='${response.activationStatus.title}'")
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            status = response.activationStatus,
                            lastCheckedTimestamp = System.currentTimeMillis(),
                            toastFeedback = response.displayMessage,
                            lastFeedbackMessage = response.displayMessage
                        )
                    }

                    // Se ativo, consulta as listas e navega automaticamente para Minhas Listas
                    if (response.activationStatus == ActivationStatus.ACTIVE) {
                        fetchDeviceLists(navigateToSectionIfActive = true)
                    }
                }
                is LcAdminResponse.Error -> {
                    Log.w(TAG, "Resposta de erro processada: code=${response.httpCode}, mensagem='${response.displayMessage}'")
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            status = if (response.isNetworkOrTimeout) ActivationStatus.CONNECTION_ERROR else it.status,
                            lastCheckedTimestamp = System.currentTimeMillis(),
                            toastFeedback = response.displayMessage,
                            lastFeedbackMessage = response.displayMessage
                        )
                    }
                }
            }
        }
    }

    /**
     * Consulta o endpoint /api/public/device-lists para carregar as múltiplas listas
     * vinculadas a este dispositivo.
     */
    fun fetchDeviceLists(navigateToSectionIfActive: Boolean = false) {
        if (_uiState.value.isLoadingLists) {
            Log.d(TAG, "Consulta de listas ignorada: carregamento já em andamento")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLists = true, listsFeedbackMessage = null) }

            val currentDeviceId = _uiState.value.deviceId
            val currentKey = _uiState.value.key

            val response = repository.fetchDeviceListsOnline(currentDeviceId, currentKey)

            when (response) {
                is LcAdminListsResponse.Success -> {
                    Log.i(TAG, "Listas recebidas com sucesso. Quantidade: ${response.lists.size}, status do dispositivo: ${response.deviceStatus}")
                    _uiState.update {
                        val shouldNavigate = (response.deviceStatus == ActivationStatus.ACTIVE) &&
                                (navigateToSectionIfActive || it.currentSection == AppScreenSection.MY_LISTS)

                        it.copy(
                            isLoadingLists = false,
                            status = response.deviceStatus,
                            lists = response.lists,
                            listsFeedbackMessage = response.displayMessage,
                            currentSection = if (shouldNavigate) AppScreenSection.MY_LISTS else it.currentSection
                        )
                    }
                }
                is LcAdminListsResponse.Error -> {
                    Log.w(TAG, "Erro ao consultar listas: code=${response.httpCode}, msg='${response.displayMessage}'")
                    _uiState.update {
                        it.copy(
                            isLoadingLists = false,
                            listsFeedbackMessage = response.displayMessage,
                            toastFeedback = response.displayMessage
                        )
                    }
                }
            }
        }
    }

    fun navigateToSection(section: AppScreenSection) {
        _uiState.update { it.copy(currentSection = section) }
        // Se navegou para Minhas Listas e a lista está vazia, dispara a busca
        if (section == AppScreenSection.MY_LISTS && _uiState.value.lists.isEmpty() && !_uiState.value.isLoadingLists) {
            fetchDeviceLists(navigateToSectionIfActive = false)
        }
    }

    fun selectList(listId: String) {
        val selected = _uiState.value.lists.firstOrNull { it.id == listId }
        _uiState.update {
            it.copy(
                selectedListId = listId,
                toastFeedback = selected?.let { list -> "Lista '${list.title}' selecionada" }
            )
        }
    }

    /**
     * Solicita a exclusão de uma lista abrindo a confirmação obrigatória na interface.
     */
    fun requestDeleteList(list: DeviceListModel) {
        _uiState.update { it.copy(listPendingDeletion = list) }
    }

    /**
     * Cancela a solicitação de exclusão.
     */
    fun cancelDeleteList() {
        _uiState.update { it.copy(listPendingDeletion = null) }
    }

    /**
     * Confirma a exclusão de uma lista específica enviando device_id, key e list_id
     * para o endpoint /delete-device-list.
     * Impede requisições simultâneas e atualiza as listas após a confirmação.
     */
    fun confirmDeleteList(list: DeviceListModel) {
        if (_uiState.value.deletingListId != null) {
            Log.d(TAG, "Tentativa de exclusão ignorada: outra exclusão já está em andamento")
            return
        }

        val currentDeviceId = _uiState.value.deviceId
        val currentKey = _uiState.value.key

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    deletingListId = list.id,
                    listPendingDeletion = null
                )
            }

            Log.d(TAG, "Iniciando exclusão da lista '${list.title}' (ID: ${list.id})")
            val response = repository.deleteDeviceListOnline(currentDeviceId, currentKey, list.id)

            when (response) {
                is LcAdminDeleteResponse.Success -> {
                    Log.i(TAG, "Lista excluída com sucesso no LC Admin. Atualizando listas...")
                    _uiState.update {
                        it.copy(
                            deletingListId = null,
                            toastFeedback = "Lista excluída"
                        )
                    }
                    // Consulta novamente /device-lists para atualizar a interface
                    fetchDeviceLists(navigateToSectionIfActive = false)
                }
                is LcAdminDeleteResponse.NotFound -> {
                    Log.w(TAG, "Lista não encontrada para exclusão. Atualizando listas...")
                    _uiState.update {
                        it.copy(
                            deletingListId = null,
                            toastFeedback = "Esta lista não está mais disponível"
                        )
                    }
                    fetchDeviceLists(navigateToSectionIfActive = false)
                }
                is LcAdminDeleteResponse.NotRegistered -> {
                    Log.w(TAG, "Dispositivo não registrado durante exclusão")
                    _uiState.update {
                        it.copy(
                            deletingListId = null,
                            toastFeedback = "Não foi possível validar o dispositivo"
                        )
                    }
                }
                is LcAdminDeleteResponse.Error -> {
                    Log.w(TAG, "Erro ao excluir lista: code=${response.httpCode}, msg='${response.displayMessage}'")
                    _uiState.update {
                        it.copy(
                            deletingListId = null,
                            toastFeedback = response.displayMessage
                        )
                    }
                }
            }
        }
    }

    /**
     * Testa a conexão e autenticação com o servidor Xtream para uma lista ativa.
     * Requisito 2: Apenas listas ativas podem ser testadas.
     * Requisito 10: Impede múltiplos testes simultâneos na mesma lista.
     */
    fun testListConnection(list: DeviceListModel) {
        if (list.status != ListStatus.ACTIVE) {
            val statusLabel = list.status.label
            _uiState.update {
                it.copy(toastFeedback = "Lista $statusLabel. Teste de conexão não permitido.")
            }
            return
        }

        if (_uiState.value.testingConnectionListId != null) {
            Log.d(TAG, "Teste de conexão ignorado: outro teste já está em andamento")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(testingConnectionListId = list.id) }
            Log.d(TAG, "Iniciando teste de conexão para a lista '${list.title}'")

            val result = repository.testXtreamAuthenticationOnline(list)

            _uiState.update { state ->
                val updatedResults = state.connectionTestResults + (list.id to result)
                state.copy(
                    testingConnectionListId = null,
                    connectionTestResults = updatedResults,
                    connectionTestDialogState = ConnectionTestDialogState(
                        listTitle = list.title,
                        result = result
                    ),
                    toastFeedback = when (result) {
                        is XtreamAuthResult.Success -> result.displayMessage
                        is XtreamAuthResult.InvalidCredentials -> result.displayMessage
                        is XtreamAuthResult.IncompatibleResponse -> result.displayMessage
                        is XtreamAuthResult.Unavailable -> result.displayMessage
                        is XtreamAuthResult.NotAllowed -> result.displayMessage
                    }
                )
            }
        }
    }

    /**
     * Fecha o diálogo com o resultado do teste de conexão.
     */
    fun dismissConnectionTestDialog() {
        _uiState.update { it.copy(connectionTestDialogState = null) }
    }

    fun setDeviceStatus(status: ActivationStatus) {
        _uiState.update { it.copy(status = status) }
    }

    fun openSystemInfo() {
        _uiState.update { it.copy(showSystemInfoModal = true) }
    }

    fun closeSystemInfo() {
        _uiState.update { it.copy(showSystemInfoModal = false) }
    }

    fun clearToastFeedback() {
        _uiState.update { it.copy(toastFeedback = null) }
    }

    fun onCopiedToClipboard() {
        _uiState.update {
            it.copy(toastFeedback = "Device ID e KEY copiados com sucesso!")
        }
    }

    /**
     * Abre a tela de exploração de categorias para uma lista ativa autenticada.
     * Requisito 2: Apenas listas ativas com dispositivo ativo podem consultar categorias.
     * Requisitos 11 e 12: Descarta dados de qualquer lista anterior, garantindo isolamento total.
     */
    fun openContentCategories(list: DeviceListModel) {
        if (list.status != ListStatus.ACTIVE) {
            val label = list.status.label
            _uiState.update {
                it.copy(toastFeedback = "Lista $label. Consulta de categorias não permitida.")
            }
            return
        }

        if (_uiState.value.status != ActivationStatus.ACTIVE) {
            _uiState.update {
                it.copy(toastFeedback = "Dispositivo precisa estar ativo no LC Admin para consultar conteúdo.")
            }
            return
        }

        Log.d(TAG, "Abrindo catálogo de categorias para a lista '${list.title}' (ID: ${list.id})")

        // Reseta o estado para garantir que dados de outras listas não apareçam
        _uiState.update {
            it.copy(
                selectedListId = list.id,
                currentSection = AppScreenSection.CONTENT_CATEGORIES,
                contentCategoriesState = ListContentCategoriesState(
                    listId = list.id,
                    listTitle = list.title,
                    sourceName = list.sourceName,
                    isLoadingLive = true,
                    isLoadingVod = true,
                    isLoadingSeries = true,
                    selectedTab = CategoryType.LIVE
                )
            )
        }

        // Dispara a consulta das três estruturas separadas
        fetchCategoriesForList(list, CategoryType.LIVE)
        fetchCategoriesForList(list, CategoryType.VOD)
        fetchCategoriesForList(list, CategoryType.SERIES)
    }

    /**
     * Consulta as categorias reais do tipo especificado no servidor Xtream.
     */
    private fun fetchCategoriesForList(list: DeviceListModel, type: CategoryType) {
        viewModelScope.launch {
            val result = repository.fetchCategoriesOnline(list, type)

            // Garante que o resultado só seja aplicado se a lista ainda for a atualmente visualizada
            _uiState.update { state ->
                val currentCatState = state.contentCategoriesState
                if (currentCatState == null || currentCatState.listId != list.id) {
                    return@update state
                }

                val updatedState = when (result) {
                    is CategoryLoadResult.Success -> {
                        when (type) {
                            CategoryType.LIVE -> currentCatState.copy(
                                liveCategories = result.categories,
                                isLoadingLive = false,
                                liveError = null
                            )
                            CategoryType.VOD -> currentCatState.copy(
                                vodCategories = result.categories,
                                isLoadingVod = false,
                                vodError = null
                            )
                            CategoryType.SERIES -> currentCatState.copy(
                                seriesCategories = result.categories,
                                isLoadingSeries = false,
                                seriesError = null
                            )
                        }
                    }
                    is CategoryLoadResult.Empty -> {
                        when (type) {
                            CategoryType.LIVE -> currentCatState.copy(
                                liveCategories = emptyList(),
                                isLoadingLive = false,
                                liveError = null
                            )
                            CategoryType.VOD -> currentCatState.copy(
                                vodCategories = emptyList(),
                                isLoadingVod = false,
                                vodError = null
                            )
                            CategoryType.SERIES -> currentCatState.copy(
                                seriesCategories = emptyList(),
                                isLoadingSeries = false,
                                seriesError = null
                            )
                        }
                    }
                    is CategoryLoadResult.ParseError -> {
                        when (type) {
                            CategoryType.LIVE -> currentCatState.copy(
                                isLoadingLive = false,
                                liveError = result.displayMessage
                            )
                            CategoryType.VOD -> currentCatState.copy(
                                isLoadingVod = false,
                                vodError = result.displayMessage
                            )
                            CategoryType.SERIES -> currentCatState.copy(
                                isLoadingSeries = false,
                                seriesError = result.displayMessage
                            )
                        }
                    }
                    is CategoryLoadResult.NetworkError -> {
                        when (type) {
                            CategoryType.LIVE -> currentCatState.copy(
                                isLoadingLive = false,
                                liveError = result.displayMessage
                            )
                            CategoryType.VOD -> currentCatState.copy(
                                isLoadingVod = false,
                                vodError = result.displayMessage
                            )
                            CategoryType.SERIES -> currentCatState.copy(
                                isLoadingSeries = false,
                                seriesError = result.displayMessage
                            )
                        }
                    }
                    is CategoryLoadResult.AuthError -> {
                        when (type) {
                            CategoryType.LIVE -> currentCatState.copy(
                                isLoadingLive = false,
                                liveError = result.displayMessage
                            )
                            CategoryType.VOD -> currentCatState.copy(
                                isLoadingVod = false,
                                vodError = result.displayMessage
                            )
                            CategoryType.SERIES -> currentCatState.copy(
                                isLoadingSeries = false,
                                seriesError = result.displayMessage
                            )
                        }
                    }
                    is CategoryLoadResult.Error -> {
                        when (type) {
                            CategoryType.LIVE -> currentCatState.copy(
                                isLoadingLive = false,
                                liveError = result.displayMessage
                            )
                            CategoryType.VOD -> currentCatState.copy(
                                isLoadingVod = false,
                                vodError = result.displayMessage
                            )
                            CategoryType.SERIES -> currentCatState.copy(
                                isLoadingSeries = false,
                                seriesError = result.displayMessage
                            )
                        }
                    }
                }

                state.copy(contentCategoriesState = updatedState)
            }
        }
    }

    /**
     * Alterna a aba ativa de categorias (Live TV, Filmes ou Séries).
     */
    fun selectCategoryTab(type: CategoryType) {
        _uiState.update { state ->
            val catState = state.contentCategoriesState ?: return@update state
            state.copy(contentCategoriesState = catState.copy(selectedTab = type))
        }
    }

    /**
     * Recarrega as categorias de um tipo específico caso tenha falhado ou a pedido do usuário.
     */
    fun refreshCategoryType(type: CategoryType) {
        val catState = _uiState.value.contentCategoriesState ?: return
        val currentList = _uiState.value.lists.firstOrNull { it.id == catState.listId } ?: return

        if (catState.isLoadingForType(type)) return

        _uiState.update { state ->
            val current = state.contentCategoriesState ?: return@update state
            val updated = when (type) {
                CategoryType.LIVE -> current.copy(isLoadingLive = true, liveError = null)
                CategoryType.VOD -> current.copy(isLoadingVod = true, vodError = null)
                CategoryType.SERIES -> current.copy(isLoadingSeries = true, seriesError = null)
            }
            state.copy(contentCategoriesState = updated)
        }

        fetchCategoriesForList(currentList, type)
    }

    /**
     * Recarrega todas as três estruturas de categorias da lista atual.
     */
    fun refreshAllCategoriesForCurrentList() {
        val catState = _uiState.value.contentCategoriesState ?: return
        val currentList = _uiState.value.lists.firstOrNull { it.id == catState.listId } ?: return

        _uiState.update { state ->
            val current = state.contentCategoriesState ?: return@update state
            state.copy(
                contentCategoriesState = current.copy(
                    isLoadingLive = true,
                    isLoadingVod = true,
                    isLoadingSeries = true,
                    liveError = null,
                    vodError = null,
                    seriesError = null
                )
            )
        }

        fetchCategoriesForList(currentList, CategoryType.LIVE)
        fetchCategoriesForList(currentList, CategoryType.VOD)
        fetchCategoriesForList(currentList, CategoryType.SERIES)
    }

    /**
     * Ação de clique / seleção de uma categoria pelo usuário (via controle remoto ou toque).
     * Diagnostica e direciona a abertura de canais para TV ao vivo.
     */
    fun openCategory(category: XtreamCategory) {
        Log.i(
            TAG,
            "Callback de clique da categoria acionado: nome='${category.categoryName}', category_id='${category.categoryId}', tipo=${category.type.displayName}"
        )

        when (category.type) {
            CategoryType.LIVE -> {
                openLiveCategoryChannels(category)
            }
            CategoryType.VOD -> {
                openVodCategoryMovies(category)
            }
            CategoryType.SERIES -> {
                openSeriesCategory(category)
            }
        }
    }

    /**
     * Abre a visualização dos canais pertencentes à categoria de TV ao vivo selecionada.
     */
    fun openLiveCategoryChannels(category: XtreamCategory) {
        val catState = _uiState.value.contentCategoriesState
        if (catState == null) {
            Log.w(TAG, "Tentativa de abrir canais sem estado de categorias ativo")
            return
        }

        val currentList = _uiState.value.lists.firstOrNull { it.id == catState.listId }
        if (currentList == null) {
            Log.w(TAG, "Lista atual '${catState.listId}' não encontrada para carregar canais")
            return
        }

        Log.i(
            TAG,
            "Iniciando abertura de canais para a categoria: '${category.categoryName}' (ID: ${category.categoryId}) da lista '${currentList.title}'"
        )

        _uiState.update { state ->
            val current = state.contentCategoriesState ?: return@update state
            state.copy(
                contentCategoriesState = current.copy(
                    selectedCategoryForChannels = category,
                    liveStreamsState = LiveStreamsState(
                        categoryId = category.categoryId,
                        categoryName = category.categoryName,
                        channels = emptyList(),
                        isLoading = true,
                        errorMessage = null
                    )
                )
            )
        }

        fetchLiveStreamsForCategory(currentList, category)
    }

    /**
     * Consulta os canais da categoria no servidor Xtream.
     */
    private fun fetchLiveStreamsForCategory(list: DeviceListModel, category: XtreamCategory) {
        viewModelScope.launch {
            Log.d(
                TAG,
                "Executando requisição get_live_streams para category_id='${category.categoryId}'..."
            )

            val result = repository.fetchLiveStreamsForCategoryOnline(list, category)

            _uiState.update { state ->
                val current = state.contentCategoriesState ?: return@update state
                if (current.selectedCategoryForChannels?.categoryId != category.categoryId) {
                    Log.d(TAG, "Resultado de canais descartado: categoria ativa mudou")
                    return@update state
                }

                val updatedStreamsState = when (result) {
                    is LiveStreamsLoadResult.Success -> {
                        Log.i(
                            TAG,
                            "Canais carregados com sucesso: ${result.channels.size} canais. Estratégia utilizada: ${result.filterStrategyUsed}"
                        )
                        LiveStreamsState(
                            categoryId = category.categoryId,
                            categoryName = category.categoryName,
                            channels = result.channels,
                            isLoading = false,
                            errorMessage = null,
                            strategyUsed = result.filterStrategyUsed
                        )
                    }
                    is LiveStreamsLoadResult.Empty -> {
                        Log.i(
                            TAG,
                            "Nenhum canal encontrado na categoria '${category.categoryName}' (ID: ${category.categoryId}). Estratégia: ${result.filterStrategyUsed}"
                        )
                        LiveStreamsState(
                            categoryId = category.categoryId,
                            categoryName = category.categoryName,
                            channels = emptyList(),
                            isLoading = false,
                            errorMessage = result.displayMessage,
                            strategyUsed = result.filterStrategyUsed
                        )
                    }
                    is LiveStreamsLoadResult.NetworkError -> {
                        Log.w(
                            TAG,
                            "Erro de rede ao carregar canais da categoria '${category.categoryName}': ${result.displayMessage}"
                        )
                        LiveStreamsState(
                            categoryId = category.categoryId,
                            categoryName = category.categoryName,
                            channels = emptyList(),
                            isLoading = false,
                            errorMessage = result.displayMessage
                        )
                    }
                    is LiveStreamsLoadResult.AuthError -> {
                        Log.w(
                            TAG,
                            "Erro de autenticação ao carregar canais da categoria '${category.categoryName}': ${result.displayMessage}"
                        )
                        LiveStreamsState(
                            categoryId = category.categoryId,
                            categoryName = category.categoryName,
                            channels = emptyList(),
                            isLoading = false,
                            errorMessage = result.displayMessage
                        )
                    }
                    is LiveStreamsLoadResult.Error -> {
                        Log.w(
                            TAG,
                            "Erro geral ao carregar canais da categoria '${category.categoryName}': ${result.displayMessage}"
                        )
                        LiveStreamsState(
                            categoryId = category.categoryId,
                            categoryName = category.categoryName,
                            channels = emptyList(),
                            isLoading = false,
                            errorMessage = result.displayMessage
                        )
                    }
                }

                state.copy(
                    contentCategoriesState = current.copy(
                        liveStreamsState = updatedStreamsState
                    )
                )
            }
        }
    }

    /**
     * Tenta novamente carregar os canais da categoria atualmente aberta.
     */
    fun retryLiveStreamsForCurrentCategory() {
        val catState = _uiState.value.contentCategoriesState ?: return
        val category = catState.selectedCategoryForChannels ?: return
        val currentList = _uiState.value.lists.firstOrNull { it.id == catState.listId } ?: return

        Log.i(TAG, "Tentando novamente carregar canais da categoria '${category.categoryName}'")

        _uiState.update { state ->
            val current = state.contentCategoriesState ?: return@update state
            val streams = current.liveStreamsState ?: LiveStreamsState(category.categoryId, category.categoryName)
            state.copy(
                contentCategoriesState = current.copy(
                    liveStreamsState = streams.copy(isLoading = true, errorMessage = null)
                )
            )
        }

        fetchLiveStreamsForCategory(currentList, category)
    }

    /**
     * Fecha a visualização de canais e retorna à lista de categorias da TV ao vivo.
     */
    fun closeLiveCategoryChannels() {
        Log.i(TAG, "Fechando visualização de canais e retornando para as categorias de TV ao vivo")
        _uiState.update { state ->
            val current = state.contentCategoriesState ?: return@update state
            state.copy(
                contentCategoriesState = current.copy(
                    selectedCategoryForChannels = null,
                    liveStreamsState = null
                )
            )
        }
    }

    /**
     * Abre a visualização dos filmes pertencentes à categoria de VOD selecionada.
     */
    fun openVodCategoryMovies(category: XtreamCategory) {
        val catState = _uiState.value.contentCategoriesState ?: return
        val currentList = _uiState.value.lists.firstOrNull { it.id == catState.listId } ?: return

        Log.i(
            TAG,
            "Iniciando abertura de filmes para a categoria VOD: '${category.categoryName}' (ID: ${category.categoryId}) da lista '${currentList.title}'"
        )

        _uiState.update { state ->
            val current = state.contentCategoriesState ?: return@update state
            state.copy(
                contentCategoriesState = current.copy(
                    selectedCategoryForChannels = category,
                    vodStreamsState = VodStreamsState(
                        categoryId = category.categoryId,
                        categoryName = category.categoryName,
                        movies = emptyList(),
                        isLoading = true,
                        errorMessage = null
                    )
                )
            )
        }

        fetchVodStreamsForCategory(currentList, category)
    }

    private fun fetchVodStreamsForCategory(list: DeviceListModel, category: XtreamCategory) {
        viewModelScope.launch {
            Log.d(
                TAG,
                "Executando requisição get_vod_streams para category_id='${category.categoryId}'..."
            )

            val result = repository.fetchVodStreamsForCategoryOnline(list, category)

            _uiState.update { state ->
                val current = state.contentCategoriesState ?: return@update state
                if (current.selectedCategoryForChannels?.categoryId != category.categoryId) {
                    Log.d(TAG, "Resultado de filmes descartado: categoria ativa mudou")
                    return@update state
                }

                val updatedVodState = when (result) {
                    is VodStreamsLoadResult.Success -> {
                        Log.i(
                            TAG,
                            "Filmes VOD carregados com sucesso: ${result.movies.size} títulos. Estratégia: ${result.filterStrategyUsed}"
                        )
                        VodStreamsState(
                            categoryId = category.categoryId,
                            categoryName = category.categoryName,
                            movies = result.movies,
                            isLoading = false,
                            strategyUsed = result.filterStrategyUsed
                        )
                    }
                    is VodStreamsLoadResult.Empty -> VodStreamsState(
                        categoryId = category.categoryId,
                        categoryName = category.categoryName,
                        movies = emptyList(),
                        isLoading = false,
                        errorMessage = result.displayMessage,
                        strategyUsed = result.filterStrategyUsed
                    )
                    is VodStreamsLoadResult.Error -> VodStreamsState(
                        categoryId = category.categoryId,
                        categoryName = category.categoryName,
                        movies = emptyList(),
                        isLoading = false,
                        errorMessage = result.displayMessage
                    )
                    is VodStreamsLoadResult.NetworkError -> VodStreamsState(
                        categoryId = category.categoryId,
                        categoryName = category.categoryName,
                        movies = emptyList(),
                        isLoading = false,
                        errorMessage = result.displayMessage
                    )
                    is VodStreamsLoadResult.AuthError -> VodStreamsState(
                        categoryId = category.categoryId,
                        categoryName = category.categoryName,
                        movies = emptyList(),
                        isLoading = false,
                        errorMessage = result.displayMessage
                    )
                }

                state.copy(
                    contentCategoriesState = current.copy(vodStreamsState = updatedVodState)
                )
            }
        }
    }

    /**
     * Tenta novamente carregar os filmes da categoria VOD aberta.
     */
    fun retryVodStreamsForCurrentCategory() {
        val catState = _uiState.value.contentCategoriesState ?: return
        val category = catState.selectedCategoryForChannels ?: return
        val currentList = _uiState.value.lists.firstOrNull { it.id == catState.listId } ?: return

        _uiState.update { state ->
            val current = state.contentCategoriesState ?: return@update state
            val vods = current.vodStreamsState ?: VodStreamsState(category.categoryId, category.categoryName)
            state.copy(
                contentCategoriesState = current.copy(
                    vodStreamsState = vods.copy(isLoading = true, errorMessage = null)
                )
            )
        }

        fetchVodStreamsForCategory(currentList, category)
    }

    /**
     * Fecha a visualização de filmes VOD e retorna à lista de categorias de filmes.
     */
    fun closeVodCategoryMovies() {
        _uiState.update { state ->
            val current = state.contentCategoriesState ?: return@update state
            state.copy(
                contentCategoriesState = current.copy(
                    selectedCategoryForChannels = null,
                    vodStreamsState = null
                )
            )
        }
    }

    /**
     * Inicia a reprodução real de um filme VOD selecionado pelo usuário.
     */
    fun playVodMovie(movie: VodMovie) {
        val catState = _uiState.value.contentCategoriesState
        val listId = catState?.listId ?: _uiState.value.selectedListId
        val currentList = _uiState.value.lists.firstOrNull { it.id == listId }

        if (currentList == null) {
            Log.e(TAG, "Não foi possível iniciar o filme ${movie.name}: lista ativa não encontrada")
            _uiState.update { it.copy(toastFeedback = "Erro: lista ativa não encontrada") }
            return
        }

        val movieUrl = XtreamStreamUrlBuilder.buildVodMovieUrl(
            rawDns = currentList.dns,
            username = currentList.username,
            password = currentList.password,
            streamId = movie.streamId,
            containerExtension = movie.containerExtension
        )

        Log.i(
            TAG,
            "Iniciando reprodução do filme VOD: '${movie.name}' (stream_id=${movie.streamId}, ext=${movie.containerExtension ?: "mp4"})"
        )

        val channelModel = LiveStreamChannel(
            streamId = movie.streamId,
            name = movie.name,
            streamIcon = movie.streamIcon,
            categoryId = movie.categoryId,
            streamType = "movie"
        )

        _uiState.update { state ->
            state.copy(
                currentSection = AppScreenSection.LIVE_PLAYER,
                livePlayerState = LivePlayerState(
                    channel = channelModel,
                    listId = currentList.id,
                    listTitle = currentList.title,
                    playbackUrl = movieUrl,
                    isPlaying = false,
                    isBuffering = true,
                    errorMessage = null,
                    showControls = true,
                    epgState = null
                )
            )
        }
    }

    /**
     * Abre a visualização das séries pertencentes à categoria de Séries selecionada.
     */
    fun openSeriesCategory(category: XtreamCategory) {
        val catState = _uiState.value.contentCategoriesState ?: return
        val currentList = _uiState.value.lists.firstOrNull { it.id == catState.listId } ?: return

        Log.i(
            TAG,
            "Iniciando abertura de séries para a categoria: '${category.categoryName}' (ID: ${category.categoryId}) da lista '${currentList.title}'"
        )

        _uiState.update { state ->
            val current = state.contentCategoriesState ?: return@update state
            state.copy(
                contentCategoriesState = current.copy(
                    selectedCategoryForChannels = category,
                    seriesState = SeriesState(
                        categoryId = category.categoryId,
                        categoryName = category.categoryName,
                        seriesList = emptyList(),
                        isLoading = true,
                        errorMessage = null
                    )
                )
            )
        }

        fetchSeriesForCategory(currentList, category)
    }

    private fun fetchSeriesForCategory(list: DeviceListModel, category: XtreamCategory) {
        viewModelScope.launch {
            Log.d(
                TAG,
                "Executando requisição get_series para category_id='${category.categoryId}'..."
            )

            val result = repository.fetchSeriesForCategoryOnline(list, category)

            _uiState.update { state ->
                val current = state.contentCategoriesState ?: return@update state
                if (current.selectedCategoryForChannels?.categoryId != category.categoryId) {
                    Log.d(TAG, "Resultado de séries descartado: categoria ativa mudou")
                    return@update state
                }

                val updatedSeriesState = when (result) {
                    is SeriesLoadResult.Success -> {
                        Log.i(
                            TAG,
                            "Séries carregadas com sucesso: ${result.series.size} títulos. Estratégia: ${result.filterStrategyUsed}"
                        )
                        SeriesState(
                            categoryId = category.categoryId,
                            categoryName = category.categoryName,
                            seriesList = result.series,
                            isLoading = false,
                            strategyUsed = result.filterStrategyUsed
                        )
                    }
                    is SeriesLoadResult.Empty -> SeriesState(
                        categoryId = category.categoryId,
                        categoryName = category.categoryName,
                        seriesList = emptyList(),
                        isLoading = false,
                        errorMessage = result.displayMessage,
                        strategyUsed = result.filterStrategyUsed
                    )
                    is SeriesLoadResult.Error -> SeriesState(
                        categoryId = category.categoryId,
                        categoryName = category.categoryName,
                        seriesList = emptyList(),
                        isLoading = false,
                        errorMessage = result.displayMessage
                    )
                    is SeriesLoadResult.NetworkError -> SeriesState(
                        categoryId = category.categoryId,
                        categoryName = category.categoryName,
                        seriesList = emptyList(),
                        isLoading = false,
                        errorMessage = result.displayMessage
                    )
                    is SeriesLoadResult.AuthError -> SeriesState(
                        categoryId = category.categoryId,
                        categoryName = category.categoryName,
                        seriesList = emptyList(),
                        isLoading = false,
                        errorMessage = result.displayMessage
                    )
                }

                state.copy(
                    contentCategoriesState = current.copy(seriesState = updatedSeriesState)
                )
            }
        }
    }

    /**
     * Tenta novamente carregar as séries da categoria aberta.
     */
    fun retrySeriesForCurrentCategory() {
        val catState = _uiState.value.contentCategoriesState ?: return
        val category = catState.selectedCategoryForChannels ?: return
        val currentList = _uiState.value.lists.firstOrNull { it.id == catState.listId } ?: return

        _uiState.update { state ->
            val current = state.contentCategoriesState ?: return@update state
            val seriesSt = current.seriesState ?: SeriesState(category.categoryId, category.categoryName)
            state.copy(
                contentCategoriesState = current.copy(
                    seriesState = seriesSt.copy(isLoading = true, errorMessage = null)
                )
            )
        }

        fetchSeriesForCategory(currentList, category)
    }

    /**
     * Abre a tela de detalhes de uma série (temporadas e episódios) e carrega o histórico de progresso.
     */
    fun openSeriesDetails(series: SeriesItem) {
        val catState = _uiState.value.contentCategoriesState ?: return
        val currentList = _uiState.value.lists.firstOrNull { it.id == catState.listId } ?: return

        Log.i(
            TAG,
            "Solicitando detalhes da série: '${series.name}' (series_id=${series.seriesId})"
        )

        viewModelScope.launch {
            val progressList = repository.getSeriesProgress(series.seriesId)
            _uiState.update { state ->
                val current = state.contentCategoriesState ?: return@update state
                state.copy(
                    contentCategoriesState = current.copy(
                        seriesDetailsState = SeriesDetailsState(
                            seriesItem = series,
                            details = null,
                            selectedSeasonNum = null,
                            isLoading = true,
                            errorMessage = null,
                            savedProgressList = progressList
                        )
                    )
                )
            }
            fetchSeriesDetails(currentList, series)
        }
    }

    private fun fetchSeriesDetails(list: DeviceListModel, series: SeriesItem) {
        viewModelScope.launch {
            val result = repository.fetchSeriesDetailsOnline(list, series.seriesId)
            val progressList = repository.getSeriesProgress(series.seriesId)

            _uiState.update { state ->
                val current = state.contentCategoriesState ?: return@update state
                val currentDetailsState = current.seriesDetailsState ?: return@update state

                if (currentDetailsState.seriesItem.seriesId != series.seriesId) {
                    Log.d(TAG, "Resultado de detalhes de série descartado: série ativa mudou")
                    return@update state
                }

                val updatedDetailsState = when (result) {
                    is SeriesDetailsLoadResult.Success -> {
                        val details = result.details
                        val defaultSeason = details.seasons.firstOrNull()?.seasonNumber ?: 1
                        currentDetailsState.copy(
                            details = details,
                            selectedSeasonNum = defaultSeason,
                            isLoading = false,
                            errorMessage = null,
                            savedProgressList = progressList
                        )
                    }
                    is SeriesDetailsLoadResult.Error -> {
                        currentDetailsState.copy(
                            isLoading = false,
                            errorMessage = result.displayMessage,
                            savedProgressList = progressList
                        )
                    }
                }

                state.copy(
                    contentCategoriesState = current.copy(seriesDetailsState = updatedDetailsState)
                )
            }
        }
    }

    /**
     * Seleciona uma temporada específica para exibir seus episódios.
     */
    fun selectSeasonForSeries(seasonNum: Int) {
        _uiState.update { state ->
            val current = state.contentCategoriesState ?: return@update state
            val detailsState = current.seriesDetailsState ?: return@update state
            state.copy(
                contentCategoriesState = current.copy(
                    seriesDetailsState = detailsState.copy(selectedSeasonNum = seasonNum)
                )
            )
        }
    }

    /**
     * Tenta novamente carregar os detalhes da série.
     */
    fun retrySeriesDetails() {
        val catState = _uiState.value.contentCategoriesState ?: return
        val detailsState = catState.seriesDetailsState ?: return
        val currentList = _uiState.value.lists.firstOrNull { it.id == catState.listId } ?: return

        _uiState.update { state ->
            val current = state.contentCategoriesState ?: return@update state
            val curDet = current.seriesDetailsState ?: return@update state
            state.copy(
                contentCategoriesState = current.copy(
                    seriesDetailsState = curDet.copy(isLoading = true, errorMessage = null)
                )
            )
        }

        fetchSeriesDetails(currentList, detailsState.seriesItem)
    }

    /**
     * Fecha a visualização de detalhes da série e retorna à grade de séries da categoria.
     */
    fun closeSeriesDetails() {
        _uiState.update { state ->
            val current = state.contentCategoriesState ?: return@update state
            state.copy(
                contentCategoriesState = current.copy(seriesDetailsState = null)
            )
        }
    }

    /**
     * Fecha a visualização de séries e retorna às categorias de séries.
     */
    fun closeSeriesCategory() {
        _uiState.update { state ->
            val current = state.contentCategoriesState ?: return@update state
            state.copy(
                contentCategoriesState = current.copy(
                    selectedCategoryForChannels = null,
                    seriesState = null,
                    seriesDetailsState = null
                )
            )
        }
    }

    /**
     * Inicia a reprodução real de um episódio de série selecionado pelo usuário.
     */
    fun playSeriesEpisode(episode: EpisodeItem, startPositionMs: Long = 0L) {
        val catState = _uiState.value.contentCategoriesState
        val details = catState?.seriesDetailsState?.details
        val seriesItem = catState?.seriesDetailsState?.seriesItem
        val listId = catState?.listId ?: _uiState.value.selectedListId
        val currentList = _uiState.value.lists.firstOrNull { it.id == listId }

        if (currentList == null) {
            Log.e(TAG, "Não foi possível iniciar o episódio: lista ativa não encontrada")
            _uiState.update { it.copy(toastFeedback = "Erro: lista ativa não encontrada") }
            return
        }

        val episodeUrl = XtreamStreamUrlBuilder.buildSeriesEpisodeUrl(
            rawDns = currentList.dns,
            username = currentList.username,
            password = currentList.password,
            episodeId = episode.id,
            containerExtension = episode.containerExtension
        )

        val seriesTitle = details?.title ?: seriesItem?.name ?: "Série"
        val formattedTitle = "$seriesTitle - S${episode.seasonNum}E${episode.episodeNum}: ${episode.title}"
        val sId = details?.seriesId ?: seriesItem?.seriesId ?: ""

        Log.i(
            TAG,
            "Iniciando reprodução de episódio de série: '$formattedTitle' (series_id=$sId, episode_id=${episode.id}, season=${episode.seasonNum}, ep=${episode.episodeNum}, start_pos=${startPositionMs}ms)"
        )

        val channelModel = LiveStreamChannel(
            streamId = episode.id,
            name = formattedTitle,
            streamIcon = episode.image ?: details?.cover ?: seriesItem?.cover,
            categoryId = catState?.selectedCategoryForChannels?.categoryId ?: "",
            streamType = "series"
        )

        _uiState.update { state ->
            state.copy(
                currentSection = AppScreenSection.LIVE_PLAYER,
                livePlayerState = LivePlayerState(
                    channel = channelModel,
                    listId = currentList.id,
                    listTitle = currentList.title,
                    playbackUrl = episodeUrl,
                    isPlaying = false,
                    isBuffering = true,
                    errorMessage = null,
                    showControls = true,
                    epgState = null,
                    startPositionMs = startPositionMs,
                    seriesId = sId,
                    seasonNum = episode.seasonNum,
                    episodeNum = episode.episodeNum,
                    episodeId = episode.id
                )
            )
        }
    }

    /**
     * Ação do botão principal da série ("Iniciar série" / "Continuar" / "Assistir novamente").
     */
    fun openPrimarySeriesButton() {
        val catState = _uiState.value.contentCategoriesState ?: return
        val detailsState = catState.seriesDetailsState ?: return

        when (val btnState = detailsState.primaryButtonState) {
            is SeriesButtonState.StartSeries -> {
                playSeriesEpisode(btnState.firstEpisode, startPositionMs = 0L)
            }
            is SeriesButtonState.ContinueSeries -> {
                playSeriesEpisode(btnState.targetEpisode, startPositionMs = btnState.startPositionMs)
            }
            is SeriesButtonState.RestartSeries -> {
                viewModelScope.launch {
                    repository.clearSeriesProgress(detailsState.seriesItem.seriesId)
                    refreshSeriesDetailsProgress(detailsState.seriesItem.seriesId)
                    playSeriesEpisode(btnState.firstEpisode, startPositionMs = 0L)
                }
            }
            is SeriesButtonState.NoEpisodesAvailable -> {
                _uiState.update { it.copy(toastFeedback = "Nenhum episódio disponível para esta série") }
            }
        }
    }

    /**
     * Salva o progresso local de reprodução de um episódio durante a execução do player.
     */
    fun savePlayerEpisodeProgress(
        positionMs: Long,
        durationMs: Long,
        forceCompleted: Boolean = false
    ) {
        val playerState = _uiState.value.livePlayerState ?: return
        val seriesId = playerState.seriesId ?: return
        val episodeId = playerState.episodeId ?: return
        val seasonNum = playerState.seasonNum ?: return
        val episodeNum = playerState.episodeNum ?: return

        if (durationMs <= 0) return

        val completed = forceCompleted || (positionMs >= 0.92 * durationMs)
        val finalPos = if (completed) durationMs else positionMs

        viewModelScope.launch {
            repository.saveEpisodeProgress(
                seriesId = seriesId,
                episodeId = episodeId,
                seasonNum = seasonNum,
                episodeNum = episodeNum,
                positionMs = finalPos,
                durationMs = durationMs,
                completed = completed
            )
            refreshSeriesDetailsProgress(seriesId)
        }
    }

    private fun refreshSeriesDetailsProgress(seriesId: String) {
        viewModelScope.launch {
            val progressList = repository.getSeriesProgress(seriesId)
            _uiState.update { state ->
                val current = state.contentCategoriesState ?: return@update state
                val detState = current.seriesDetailsState ?: return@update state
                if (detState.seriesItem.seriesId != seriesId) return@update state
                state.copy(
                    contentCategoriesState = current.copy(
                        seriesDetailsState = detState.copy(savedProgressList = progressList)
                    )
                )
            }
        }
    }

    /**
     * Solicitado quando um canal de TV ao vivo recebe foco na navegação D-pad
     * ou é selecionado para reprodução.
     * Busca o EPG curto (get_short_epg, limit=2) para o stream_id especificado.
     *
     * Estratégia:
     * - Cache em memória por stream_id (TTL 5 minutos).
     * - Cancelamento de requisições pendentes se o foco mudar rapidamente.
     * - Debounce de 200ms para evitar rajadas HTTP na rolagem rápida.
     * - Execução 100% assíncrona que nunca bloqueia navegação nem reprodução.
     */
    fun onChannelFocused(streamId: String) {
        if (streamId.isBlank()) return
        if (currentlyFocusedStreamId == streamId && _uiState.value.channelEpgStates[streamId]?.isLoading == true) {
            return
        }

        currentlyFocusedStreamId = streamId
        epgFetchJob?.cancel()

        // 1. Verifica cache em memória
        val cached = epgCache[streamId]
        val now = System.currentTimeMillis()
        if (cached != null && (now - cached.first) < 5 * 60 * 1000L) {
            val cachedState = cached.second
            _uiState.update { state ->
                val updatedMap = state.channelEpgStates + (streamId to cachedState)
                if (state.livePlayerState?.channel?.streamId == streamId) {
                    state.copy(
                        channelEpgStates = updatedMap,
                        livePlayerState = state.livePlayerState.copy(epgState = cachedState)
                    )
                } else {
                    state.copy(channelEpgStates = updatedMap)
                }
            }
            return
        }

        // 2. Define estado inicial de carregamento caso não esteja no cache
        _uiState.update { state ->
            val loadingState = ShortEpgState(streamId = streamId, isLoading = true)
            state.copy(channelEpgStates = state.channelEpgStates + (streamId to loadingState))
        }

        epgFetchJob = viewModelScope.launch {
            // Delay de debounce para evitar rajadas de requisições durante navegação rápida pelo D-pad
            delay(200)

            if (!isActive || currentlyFocusedStreamId != streamId) return@launch

            val catState = _uiState.value.contentCategoriesState
            val listId = catState?.listId ?: _uiState.value.selectedListId ?: _uiState.value.livePlayerState?.listId
            val currentList = _uiState.value.lists.firstOrNull { it.id == listId }

            if (currentList == null || currentList.dns.isBlank()) {
                val noListState = ShortEpgState(streamId = streamId, isLoading = false, hasError = true, errorMessage = "Lista não encontrada")
                _uiState.update { state ->
                    state.copy(channelEpgStates = state.channelEpgStates + (streamId to noListState))
                }
                return@launch
            }

            val epgResult = xtreamEpgClient.fetchShortEpg(
                dns = currentList.dns,
                username = currentList.username,
                password = currentList.password,
                streamId = streamId
            )

            if (!isActive || currentlyFocusedStreamId != streamId) return@launch

            val finalEpgState = when (epgResult) {
                is ShortEpgFetchResult.Success -> ShortEpgState(
                    streamId = streamId,
                    currentProgram = epgResult.currentProgram,
                    nextProgram = epgResult.nextProgram,
                    isLoading = false
                )
                is ShortEpgFetchResult.Error -> ShortEpgState(
                    streamId = streamId,
                    isLoading = false,
                    hasError = true,
                    errorMessage = epgResult.message
                )
            }

            // Armazena no cache
            epgCache[streamId] = Pair(System.currentTimeMillis(), finalEpgState)

            // Atualiza UI
            _uiState.update { state ->
                val updatedMap = state.channelEpgStates + (streamId to finalEpgState)
                if (state.livePlayerState?.channel?.streamId == streamId) {
                    state.copy(
                        channelEpgStates = updatedMap,
                        livePlayerState = state.livePlayerState.copy(epgState = finalEpgState)
                    )
                } else {
                    state.copy(channelEpgStates = updatedMap)
                }
            }
        }
    }

    /**
     * Inicia a reprodução real de um canal de TV ao vivo.
     * Constrói a URL do stream a partir das credenciais da lista atualmente selecionada.
     */
    fun playLiveChannel(channel: LiveStreamChannel) {
        val catState = _uiState.value.contentCategoriesState
        val listId = catState?.listId ?: _uiState.value.selectedListId
        val currentList = _uiState.value.lists.firstOrNull { it.id == listId }

        if (currentList == null) {
            Log.e(TAG, "Não foi possível iniciar o canal ${channel.name}: lista ativa não encontrada")
            _uiState.update { it.copy(toastFeedback = "Erro: lista ativa não encontrada") }
            return
        }

        // Construção dinâmica da URL de reprodução Xtream
        val streamUrl = XtreamStreamUrlBuilder.buildLiveStreamUrl(
            rawDns = currentList.dns,
            username = currentList.username,
            password = currentList.password,
            streamId = channel.streamId,
            containerExtension = "m3u8"
        )

        Log.i(TAG, "Iniciando reprodução do canal: '${channel.name}' (stream_id=${channel.streamId}) da lista '${currentList.title}'")

        val currentEpg = _uiState.value.channelEpgStates[channel.streamId]

        _uiState.update { state ->
            state.copy(
                currentSection = AppScreenSection.LIVE_PLAYER,
                livePlayerState = LivePlayerState(
                    channel = channel,
                    listId = currentList.id,
                    listTitle = currentList.title,
                    playbackUrl = streamUrl,
                    isPlaying = false,
                    isBuffering = true,
                    errorMessage = null,
                    showControls = true,
                    epgState = currentEpg
                )
            )
        }

        // Caso o EPG do canal ainda não tenha sido consultado, dispara busca assíncrona
        if (currentEpg == null || currentEpg.isLoading) {
            onChannelFocused(channel.streamId)
        }
    }

    /**
     * Troca para o canal ou filme anterior na lista da categoria atualmente aberta.
     * Suporta navegação cíclica (do primeiro item volta para o último).
     */
    fun playPreviousChannel() {
        val currentChannel = _uiState.value.livePlayerState?.channel ?: return
        val catState = _uiState.value.contentCategoriesState

        if (currentChannel.streamType == "movie") {
            val vodList = catState?.vodStreamsState?.movies ?: emptyList()
            if (vodList.isEmpty()) return
            val currentIndex = vodList.indexOfFirst { it.streamId == currentChannel.streamId }
            val targetIndex = if (currentIndex > 0) currentIndex - 1 else vodList.lastIndex
            val targetMovie = vodList[targetIndex]
            playVodMovie(targetMovie)
            return
        }

        val channelsList = catState?.liveStreamsState?.channels ?: emptyList()
        if (channelsList.isEmpty()) {
            Log.w(TAG, "Troca de canal ignorada: lista de canais vazia na categoria")
            return
        }

        val currentIndex = channelsList.indexOfFirst { it.streamId == currentChannel.streamId }
        val targetIndex = if (currentIndex > 0) {
            currentIndex - 1
        } else {
            channelsList.lastIndex
        }

        val targetChannel = channelsList[targetIndex]
        Log.i(
            TAG,
            "Troca de canal (Anterior): de '${currentChannel.name}' [pos $currentIndex] para '${targetChannel.name}' [pos $targetIndex]"
        )
        playLiveChannel(targetChannel)
    }

    /**
     * Troca para o próximo canal ou filme na lista da categoria atualmente aberta.
     * Suporta navegação cíclica (do último item avança para o primeiro).
     */
    fun playNextChannel() {
        val currentChannel = _uiState.value.livePlayerState?.channel ?: return
        val catState = _uiState.value.contentCategoriesState

        if (currentChannel.streamType == "movie") {
            val vodList = catState?.vodStreamsState?.movies ?: emptyList()
            if (vodList.isEmpty()) return
            val currentIndex = vodList.indexOfFirst { it.streamId == currentChannel.streamId }
            val targetIndex = if (currentIndex >= 0 && currentIndex < vodList.lastIndex) currentIndex + 1 else 0
            val targetMovie = vodList[targetIndex]
            playVodMovie(targetMovie)
            return
        }

        val channelsList = catState?.liveStreamsState?.channels ?: emptyList()
        if (channelsList.isEmpty()) {
            Log.w(TAG, "Troca de canal ignorada: lista de canais vazia na categoria")
            return
        }

        val currentIndex = channelsList.indexOfFirst { it.streamId == currentChannel.streamId }
        val targetIndex = if (currentIndex >= 0 && currentIndex < channelsList.lastIndex) {
            currentIndex + 1
        } else {
            0
        }

        val targetChannel = channelsList[targetIndex]
        Log.i(
            TAG,
            "Troca de canal (Próximo): de '${currentChannel.name}' [pos $currentIndex] para '${targetChannel.name}' [pos $targetIndex]"
        )
        playLiveChannel(targetChannel)
    }

    /**
     * Tenta reproduzir com URL alternativa em caso de falha de codec/container (ex: .ts ou stream direto).
     */
    fun retryPlaybackWithAlternativeFormat(fallbackExtension: String = "ts") {
        val playerState = _uiState.value.livePlayerState ?: return
        val currentList = _uiState.value.lists.firstOrNull { it.id == playerState.listId } ?: return

        val fallbackUrl = XtreamStreamUrlBuilder.buildLiveStreamUrl(
            rawDns = currentList.dns,
            username = currentList.username,
            password = currentList.password,
            streamId = playerState.channel.streamId,
            containerExtension = fallbackExtension
        )

        Log.i(TAG, "Tentando formato alternativo ($fallbackExtension) para o canal '${playerState.channel.name}'")

        _uiState.update { state ->
            val cur = state.livePlayerState ?: return@update state
            state.copy(
                livePlayerState = cur.copy(
                    playbackUrl = fallbackUrl,
                    isBuffering = true,
                    errorMessage = null
                )
            )
        }
    }

    /**
     * Atualiza o estado interno do player durante a reprodução.
     */
    fun updatePlayerState(
        isPlaying: Boolean,
        isBuffering: Boolean,
        errorMessage: String? = null
    ) {
        _uiState.update { state ->
            val cur = state.livePlayerState ?: return@update state
            state.copy(
                livePlayerState = cur.copy(
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    errorMessage = errorMessage
                )
            )
        }
    }

    /**
     * Alterna ou define a visibilidade dos controles na tela de reprodução.
     */
    fun togglePlayerControls(visible: Boolean? = null) {
        _uiState.update { state ->
            val cur = state.livePlayerState ?: return@update state
            state.copy(
                livePlayerState = cur.copy(
                    showControls = visible ?: !cur.showControls,
                    lastUserInteractionTime = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Fecha a tela de reprodução e retorna exatamente à visualização de canais da categoria.
     */
    fun closeLivePlayer() {
        Log.i(TAG, "Fechando player e retornando à visualização de canais da categoria")
        _uiState.update { state ->
            state.copy(
                currentSection = AppScreenSection.CONTENT_CATEGORIES,
                livePlayerState = null
            )
        }
    }

    /**
     * Retorna à tela de Minhas Listas e limpa o cache de categorias em memória.
     */
    fun closeContentCategories() {
        _uiState.update {
            it.copy(
                currentSection = AppScreenSection.MY_LISTS,
                contentCategoriesState = null
            )
        }
    }
}

