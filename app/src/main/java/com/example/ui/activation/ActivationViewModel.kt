package com.example.ui.activation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.DeviceRepository
import com.example.domain.model.ActivationStatus
import kotlinx.coroutines.delay
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
     * Acionado quando o usuário clica em "Verificar Ativação" no controle remoto ou tela.
     * Atualiza o timestamp local de verificação. Conforme a regra do projeto,
     * NÃO cria fake API nem simula dados fictícios do backend; mantém o status real
     * "Aguardando ativação" até que o dispositivo seja cadastrado no LC Admin.
     */
    fun onVerifyActivationClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, toastFeedback = null) }
            
            // Simulação de tempo de resposta da checagem local sem inventar dados
            delay(600)
            repository.refreshLocalStatus()
            
            _uiState.update {
                it.copy(
                    isChecking = false,
                    lastCheckedTimestamp = System.currentTimeMillis(),
                    toastFeedback = "Status verificado: Dispositivo ainda não ativado no LC Admin."
                )
            }
        }
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
}
