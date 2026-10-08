package com.example.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.domain.model.LivePlayerState
import com.example.ui.components.tvFocusable
import com.example.ui.theme.TvAccentRed
import com.example.ui.theme.TvFocusBorder
import com.example.ui.theme.TvSecondaryBlue
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextMuted
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import kotlinx.coroutines.delay

/**
 * Tela de reprodução em tela cheia de TV ao vivo com AndroidX Media3 ExoPlayer.
 *
 * Características:
 * - Edge-to-edge / Fullscreen video playback.
 * - Gerenciamento de ciclo de vida do ExoPlayer via DisposableEffect (libera recursos ao sair).
 * - Tratamento de estados: Loading/Buffering, Playing, Paused e Erros de rede/codec com fallback.
 * - Suporte completo a controle remoto Android TV (D-pad: OK para pausar/retomar, Back para sair).
 * - OSD (On-Screen Display) translúcido automático que esconde após 5 segundos de inatividade.
 */
@OptIn(UnstableApi::class)
@Composable
fun LivePlayerScreen(
    playerState: LivePlayerState,
    onBackClick: () -> Unit,
    onPlayerStateChange: (isPlaying: Boolean, isBuffering: Boolean, error: String?) -> Unit,
    onRetryFallbackFormat: (extension: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isControlsVisible by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var retryCount by remember { mutableStateOf(0) }

    val backButtonFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }

    // Intercepta o botão voltar nativo do Android / controle remoto
    BackHandler {
        onBackClick()
    }

    // Cria e gerencia a instância do ExoPlayer
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build()
    }

    // Listener para eventos do ExoPlayer
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                        playbackError = null
                        onPlayerStateChange(isPlaying, true, null)
                    }
                    Player.STATE_READY -> {
                        isBuffering = false
                        playbackError = null
                        val playing = exoPlayer.isPlaying
                        isPlaying = playing
                        onPlayerStateChange(playing, false, null)
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        isPlaying = false
                        onPlayerStateChange(false, false, null)
                    }
                    Player.STATE_IDLE -> {
                        // Estado ocioso
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                onPlayerStateChange(playing, isBuffering, playbackError)
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                isPlaying = false
                val errorDesc = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                        "Falha na conexão com o servidor de stream"
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                        "Servidor recusou a transmissão (HTTP error)"
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
                        "Formato do canal incompatível ou codec indisponível"
                    else -> "Erro na reprodução: ${error.localizedMessage ?: "Erro desconhecido"}"
                }
                playbackError = errorDesc
                onPlayerStateChange(false, false, errorDesc)
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Carrega a URL do canal no ExoPlayer sempre que playbackUrl mudar
    LaunchedEffect(playerState.playbackUrl, retryCount) {
        try {
            isBuffering = true
            playbackError = null
            onPlayerStateChange(false, true, null)

            val mediaItem = MediaItem.fromUri(playerState.playbackUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } catch (e: Exception) {
            isBuffering = false
            playbackError = "Falha ao preparar o stream: ${e.message}"
            onPlayerStateChange(false, false, playbackError)
        }
    }

    // Auto-ocultação do OSD após 5 segundos de reprodução
    LaunchedEffect(isControlsVisible, isPlaying, isBuffering) {
        if (isControlsVisible && isPlaying && !isBuffering && playbackError == null) {
            delay(5000)
            isControlsVisible = false
        }
    }

    // Foco automático nos controles quando exibidos
    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible) {
            try {
                playPauseFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                isControlsVisible = !isControlsVisible
            }
            .onKeyEvent { keyEvent ->
                // Tratamento de teclas do controle remoto da TV
                when (keyEvent.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (!isControlsVisible) {
                            isControlsVisible = true
                            true
                        } else {
                            false
                        }
                    }
                    Key.DirectionUp, Key.DirectionDown -> {
                        if (!isControlsVisible) {
                            isControlsVisible = true
                            true
                        } else {
                            false
                        }
                    }
                    Key.MediaPlayPause -> {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                        }
                        isControlsVisible = true
                        true
                    }
                    Key.MediaPlay -> {
                        exoPlayer.play()
                        isControlsVisible = true
                        true
                    }
                    Key.MediaPause -> {
                        exoPlayer.pause()
                        isControlsVisible = true
                        true
                    }
                    else -> false
                }
            }
    ) {
        // Player Surface (Media3 PlayerView integrado com Compose)
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Usamos nossa própria UI TV-first em Compose
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Indicador de Carregamento / Buffering
        if (isBuffering && playbackError == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x77000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(54.dp),
                        color = TvSecondaryBlue,
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Carregando ${playerState.channel.name}...",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Conectando ao stream ao vivo",
                        color = TvTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Card de Erro de Reprodução com botões de Recuperação
        if (playbackError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(TvSurface)
                        .border(1.dp, TvAccentRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = TvAccentRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Não foi possível reproduzir este canal",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = playbackError ?: "Erro desconhecido",
                        color = TvTextSecondary,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Tentar novamente (.m3u8)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(TvSecondaryBlue)
                                .tvFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    focusBorderColor = Color.White,
                                    focusedScale = 1.05f,
                                    onClick = {
                                        retryCount++
                                        playbackError = null
                                        isBuffering = true
                                    }
                                )
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tentar novamente",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Tentar formato TS alternativo
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(TvSurfaceVariant)
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                                .tvFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    focusBorderColor = TvFocusBorder,
                                    focusedScale = 1.05f,
                                    onClick = {
                                        onRetryFallbackFormat("ts")
                                        retryCount++
                                    }
                                )
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Alternar formato (TS)",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Voltar para a lista de canais
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF263238))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                                .tvFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    focusBorderColor = TvFocusBorder,
                                    focusedScale = 1.05f,
                                    onClick = onBackClick
                                )
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Voltar aos canais",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Overlay de Controles (OSD) com animação suave
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xCC000000),
                                Color.Transparent,
                                Color.Transparent,
                                Color(0xEE000000)
                            )
                        )
                    )
            ) {
                // Barra Superior do Player
                PlayerTopBar(
                    channelName = playerState.channel.name,
                    listTitle = playerState.listTitle,
                    onBackClick = onBackClick,
                    focusRequester = backButtonFocusRequester,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(start = 24.dp, top = 20.dp, end = 24.dp)
                )

                // Barra Inferior de Informações e Controles
                PlayerBottomControls(
                    channel = playerState.channel,
                    isPlaying = isPlaying,
                    onPlayPauseToggle = {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                        }
                    },
                    onRefresh = {
                        retryCount++
                    },
                    playPauseFocusRequester = playPauseFocusRequester,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                )
            }
        }
    }
}

/**
 * Barra superior do player com botão voltar e identificação do canal.
 */
@Composable
private fun PlayerTopBar(
    channelName: String,
    listTitle: String,
    onBackClick: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Botão Voltar para Canais
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .tvFocusable(
                        shape = CircleShape,
                        focusBorderColor = TvFocusBorder,
                        focusedScale = 1.1f,
                        onClick = onBackClick
                    )
                    .focusRequester(focusRequester)
                    .testTag("btn_player_back"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar para lista de canais",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text = channelName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "LC Player • ${listTitle.ifBlank { "TV ao Vivo" }}",
                    color = TvTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Badge indicador "AO VIVO"
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0x33FF1744))
                .border(1.dp, Color(0x66FF1744), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF1744))
            )
            Text(
                text = "AO VIVO",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Controles e barra informativa inferior do player.
 */
@Composable
private fun PlayerBottomControls(
    channel: com.example.domain.model.LiveStreamChannel,
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onRefresh: () -> Unit,
    playPauseFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x99101622))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo + Número + Nome do canal
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Logo do canal
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF142033)),
                contentAlignment = Alignment.Center
            ) {
                if (!channel.streamIcon.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.streamIcon,
                        contentDescription = "Logo de ${channel.name}",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = TvTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (channel.num != null) {
                        Text(
                            text = "#${channel.num}",
                            color = TvSecondaryBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = channel.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Pressione OK para pausar / reproduzir",
                    color = TvTextMuted,
                    fontSize = 12.sp
                )
            }
        }

        // Botões de Ação Play/Pause e Atualizar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Botão Play/Pause
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(TvSecondaryBlue)
                    .tvFocusable(
                        shape = CircleShape,
                        focusBorderColor = Color.White,
                        focusedScale = 1.1f,
                        onClick = onPlayPauseToggle
                    )
                    .focusRequester(playPauseFocusRequester)
                    .testTag("btn_player_play_pause"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Botão Recarregar Stream
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(TvSurface)
                    .border(1.dp, TvSurfaceVariant, CircleShape)
                    .tvFocusable(
                        shape = CircleShape,
                        focusBorderColor = TvFocusBorder,
                        focusedScale = 1.1f,
                        onClick = onRefresh
                    )
                    .testTag("btn_player_reload"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Recarregar stream",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
