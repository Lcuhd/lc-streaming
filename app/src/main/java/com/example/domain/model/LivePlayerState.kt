package com.example.domain.model

/**
 * Estado completo do player de reprodução de Live TV.
 */
data class LivePlayerState(
    val channel: LiveStreamChannel,
    val listId: String,
    val listTitle: String,
    val playbackUrl: String,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val errorMessage: String? = null,
    val showControls: Boolean = true,
    val lastUserInteractionTime: Long = System.currentTimeMillis()
)
