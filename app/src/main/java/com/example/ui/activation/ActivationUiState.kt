package com.example.ui.activation

import com.example.domain.model.ActivationStatus
import com.example.domain.model.ConnectionTestDialogState
import com.example.domain.model.DeviceListModel
import com.example.domain.model.ListContentCategoriesState
import com.example.domain.model.LivePlayerState
import com.example.domain.model.XtreamAuthResult

enum class AppScreenSection {
    ACTIVATION,
    MY_LISTS,
    CONTENT_CATEGORIES,
    LIVE_PLAYER
}

data class ActivationUiState(
    val deviceId: String = "--:--:--:--:--:--",
    val key: String = "------",
    val status: ActivationStatus = ActivationStatus.WAITING_ACTIVATION,
    val isChecking: Boolean = false,
    val lastCheckedTimestamp: Long? = null,
    val toastFeedback: String? = null,
    val lastFeedbackMessage: String? = null,
    val showSystemInfoModal: Boolean = false,

    // Seção atual da aplicação (Identificação, Minhas Listas, Categorias ou Player de TV)
    val currentSection: AppScreenSection = AppScreenSection.ACTIVATION,

    // Coleção de listas do dispositivo
    val lists: List<DeviceListModel> = emptyList(),
    val isLoadingLists: Boolean = false,
    val listsFeedbackMessage: String? = null,
    val selectedListId: String? = null,

    // Controle de exclusão de lista com confirmação obrigatória
    val listPendingDeletion: DeviceListModel? = null,
    val deletingListId: String? = null,

    // Teste de conexão Xtream da lista
    val testingConnectionListId: String? = null,
    val connectionTestDialogState: ConnectionTestDialogState? = null,
    val connectionTestResults: Map<String, XtreamAuthResult> = emptyMap(),

    // Estado das categorias da lista atualmente selecionada
    val contentCategoriesState: ListContentCategoriesState? = null,

    // Estado do player de reprodução de canal ao vivo
    val livePlayerState: LivePlayerState? = null
)
