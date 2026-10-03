package com.example.ui.activation

import com.example.domain.model.ActivationStatus

data class ActivationUiState(
    val deviceId: String = "--:--:--:--:--:--",
    val key: String = "------",
    val status: ActivationStatus = ActivationStatus.WAITING_ACTIVATION,
    val isChecking: Boolean = false,
    val lastCheckedTimestamp: Long? = null,
    val toastFeedback: String? = null,
    val showSystemInfoModal: Boolean = false
)
