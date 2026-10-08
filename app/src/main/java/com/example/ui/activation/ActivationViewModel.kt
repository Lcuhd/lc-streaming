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
import com.example.domain.model.XtreamAuthResult
import com.example.domain.model.XtreamCategory
import com.example.util.XtreamStreamUrlBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ActivationViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = DeviceRepository(application, database.deviceInfoDao())

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
                Log.d(TAG, "Clique em categoria de filmes '${category.categoryName}' (não implementado nesta etapa)")
                _uiState.update {
                    it.copy(toastFeedback = "Filmes da categoria '${category.categoryName}' estarão disponíveis na próxima etapa")
                }
            }
            CategoryType.SERIES -> {
                Log.d(TAG, "Clique em categoria de séries '${category.categoryName}' (não implementado nesta etapa)")
                _uiState.update {
                    it.copy(toastFeedback = "Séries da categoria '${category.categoryName}' estarão disponíveis na próxima etapa")
                }
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
                    showControls = true
                )
            )
        }
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

