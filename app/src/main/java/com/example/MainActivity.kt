package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.activation.ActivationScreen
import com.example.ui.activation.ActivationViewModel
import com.example.ui.activation.AppScreenSection
import com.example.ui.categories.CategoriesScreen
import com.example.ui.lists.DeviceListsScreen
import com.example.ui.player.LivePlayerScreen
import com.example.ui.theme.LcPlayerTheme

class MainActivity : ComponentActivity() {

    private val activationViewModel: ActivationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LcPlayerTheme {
                val uiState by activationViewModel.uiState.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    when (uiState.currentSection) {
                        AppScreenSection.ACTIVATION -> {
                            ActivationScreen(
                                viewModel = activationViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        AppScreenSection.MY_LISTS -> {
                            DeviceListsScreen(
                                viewModel = activationViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        AppScreenSection.CONTENT_CATEGORIES -> {
                            CategoriesScreen(
                                viewModel = activationViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        AppScreenSection.LIVE_PLAYER -> {
                            val playerState = uiState.livePlayerState
                            if (playerState != null) {
                                LivePlayerScreen(
                                    playerState = playerState,
                                    onBackClick = { activationViewModel.closeLivePlayer() },
                                    onPlayerStateChange = { isPlaying, isBuffering, error ->
                                        activationViewModel.updatePlayerState(isPlaying, isBuffering, error)
                                    },
                                    onRetryFallbackFormat = { fallbackExtension ->
                                        activationViewModel.retryPlaybackWithAlternativeFormat(fallbackExtension)
                                    }
                                )
                            } else {
                                activationViewModel.closeLivePlayer()
                            }
                        }
                    }
                }
            }
        }
    }
}
