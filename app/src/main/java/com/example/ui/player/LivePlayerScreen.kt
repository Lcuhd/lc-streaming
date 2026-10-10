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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.domain.model.LivePlayerState
import com.example.domain.model.ShortEpgState
import com.example.ui.components.tvFocusable
import com.example.ui.theme.StatusActiveText
import com.example.ui.theme.TvAccentRed
import com.example.ui.theme.TvFocusBorder
import com.example.ui.theme.TvPrimaryBlue
import com.example.ui.theme.TvSecondaryBlue
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextMuted
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import kotlinx.coroutines.delay

/**
 * Modelo interno de transporte para faixas de áudio detectadas no Media3.
 */
data class AudioTrackInfo(
    val group: Tracks.Group,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val isSelected: Boolean
)

/**
 * Modelo interno de transporte para faixas de legenda detectadas no Media3.
 */
data class SubtitleTrackInfo(
    val group: Tracks.Group,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val isSelected: Boolean
)

/**
 * Tela de reprodução em tela cheia compartilhada para TV ao vivo, Filmes e Séries.
 */
@OptIn(UnstableApi::class)
@Composable
fun LivePlayerScreen(
    playerState: LivePlayerState,
    onBackClick: () -> Unit,
    onPreviousChannel: () -> Unit = {},
    onNextChannel: () -> Unit = {},
    onSaveProgress: ((positionMs: Long, durationMs: Long, completed: Boolean) -> Unit)? = null,
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
    var hasExecutedInitialSeek by remember(playerState.playbackUrl) { mutableStateOf(false) }

    // Progresso e Faixas do Player
    var currentPositionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var audioTracks by remember { mutableStateOf<List<AudioTrackInfo>>(emptyList()) }
    var subtitleTracks by remember { mutableStateOf<List<SubtitleTrackInfo>>(emptyList()) }
    var showAudioModal by remember { mutableStateOf(false) }
    var showSubtitlesModal by remember { mutableStateOf(false) }

    val streamTypeClean = playerState.channel.streamType?.lowercase()?.trim().orEmpty()
    val isVodContent = streamTypeClean == "movie" ||
                       streamTypeClean == "series" ||
                       streamTypeClean == "episode" ||
                       streamTypeClean == "vod" ||
                       playerState.seriesId != null ||
                       playerState.episodeId != null

    val backButtonFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }

    BackHandler {
        if (showAudioModal || showSubtitlesModal) {
            showAudioModal = false
            showSubtitlesModal = false
        } else {
            onBackClick()
        }
    }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build()
    }

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

                        val dur = exoPlayer.duration
                        if (dur > 0) durationMs = dur

                        if (!hasExecutedInitialSeek && playerState.startPositionMs > 0) {
                            hasExecutedInitialSeek = true
                            exoPlayer.seekTo(playerState.startPositionMs)
                            android.util.Log.i(
                                "LCPlayer_SeriesProgress",
                                "Seek inicial para posição salva executado: ${playerState.startPositionMs}ms"
                            )
                        }
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        isPlaying = false
                        onPlayerStateChange(false, false, null)
                        if (playerState.seriesId != null) {
                            val dur = exoPlayer.duration
                            onSaveProgress?.invoke(if (dur > 0) dur else 0L, if (dur > 0) dur else 0L, true)
                        }
                    }
                    Player.STATE_IDLE -> {}
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                onPlayerStateChange(playing, isBuffering, playbackError)
            }

            override fun onTracksChanged(tracks: Tracks) {
                audioTracks = extractAudioTracks(tracks)
                subtitleTracks = extractSubtitleTracks(tracks)
                android.util.Log.i(
                    "LCPlayer_Tracks",
                    "Tracks detectadas: ${audioTracks.size} faixas de áudio, ${subtitleTracks.size} legendas"
                )
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
            if (playerState.seriesId != null) {
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                if (pos > 0 && dur > 0) {
                    val completed = pos >= 0.92 * dur
                    onSaveProgress?.invoke(if (completed) dur else pos, dur, completed)
                }
            }
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Atualização do progresso atual e salvamento a cada 500ms
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            if (pos >= 0) currentPositionMs = pos
            if (dur > 0) durationMs = dur

            if (playerState.seriesId != null && pos > 0 && dur > 0) {
                val completed = pos >= 0.92 * dur
                onSaveProgress?.invoke(if (completed) dur else pos, dur, completed)
            }
            delay(500)
        }
    }

    // Carrega a URL da mídia no ExoPlayer
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

    // Auto-ocultação do OSD após 5 segundos de inatividade
    LaunchedEffect(isControlsVisible, isPlaying, isBuffering, showAudioModal, showSubtitlesModal) {
        if (isControlsVisible && isPlaying && !isBuffering && playbackError == null && !showAudioModal && !showSubtitlesModal) {
            delay(5000)
            isControlsVisible = false
        }
    }

    // Foco automático nos controles quando exibidos
    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible && !showAudioModal && !showSubtitlesModal) {
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
                if (keyEvent.type != KeyEventType.KeyDown) {
                    return@onKeyEvent false
                }
                if (showAudioModal || showSubtitlesModal) {
                    return@onKeyEvent false
                }

                when (keyEvent.key) {
                    Key.DirectionUp, Key.ChannelUp, Key.PageUp -> {
                        if (!isVodContent || streamTypeClean == "series") {
                            onPreviousChannel()
                            isControlsVisible = true
                            true
                        } else {
                            isControlsVisible = true
                            false
                        }
                    }
                    Key.DirectionDown, Key.ChannelDown, Key.PageDown -> {
                        if (!isVodContent || streamTypeClean == "series") {
                            onNextChannel()
                            isControlsVisible = true
                            true
                        } else {
                            isControlsVisible = true
                            false
                        }
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        isControlsVisible = !isControlsVisible
                        true
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
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading / Buffering
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
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Carregando transmissão...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Overlay de Erro
        if (playbackError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE090D14)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FF1744)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = TvAccentRed,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Erro no Player",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = playbackError ?: "Falha ao carregar a transmissão",
                        color = TvTextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .height(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TvSecondaryBlue)
                                .tvFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    focusBorderColor = Color.White,
                                    onClick = {
                                        retryCount++
                                        playbackError = null
                                    }
                                )
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tentar Novamente",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TvSurface)
                                .border(1.dp, TvSurfaceVariant, RoundedCornerShape(10.dp))
                                .tvFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    focusBorderColor = TvFocusBorder,
                                    onClick = { onRetryFallbackFormat("ts") }
                                )
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Formato Alternativo (.ts)",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TvSurface)
                                .border(1.dp, TvSurfaceVariant, RoundedCornerShape(10.dp))
                                .tvFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    focusBorderColor = TvFocusBorder,
                                    onClick = onBackClick
                                )
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Voltar",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Controles OSD
        AnimatedVisibility(
            visible = isControlsVisible && playbackError == null,
            enter = fadeIn(),
            exit = fadeOut()
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
                PlayerTopBar(
                    channelName = playerState.channel.name,
                    listTitle = playerState.listTitle,
                    streamType = playerState.channel.streamType,
                    onBackClick = onBackClick,
                    focusRequester = backButtonFocusRequester,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(start = 24.dp, top = 20.dp, end = 24.dp)
                )

                PlayerBottomControls(
                    channel = playerState.channel,
                    epgState = playerState.epgState,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    audioTracksCount = audioTracks.size,
                    subtitleTracksCount = subtitleTracks.size,
                    onPreviousChannel = onPreviousChannel,
                    onNextChannel = onNextChannel,
                    onSeekRelative = { offsetMs ->
                        seekRelative(exoPlayer, offsetMs)
                    },
                    onSeekToPosition = { newPos ->
                        exoPlayer.seekTo(newPos)
                    },
                    onPlayPauseToggle = {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                        }
                    },
                    onRefresh = { retryCount++ },
                    onOpenAudioModal = { showAudioModal = true },
                    onOpenSubtitlesModal = { showSubtitlesModal = true },
                    playPauseFocusRequester = playPauseFocusRequester,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                )
            }
        }

        // Modal de Seleção de Áudio
        if (showAudioModal) {
            AudioTrackSelectionModal(
                audioTracks = audioTracks,
                onSelectTrack = { track ->
                    selectAudioTrack(exoPlayer, track)
                },
                onDismiss = { showAudioModal = false }
            )
        }

        // Modal de Seleção de Legendas
        if (showSubtitlesModal) {
            SubtitleTrackSelectionModal(
                subtitleTracks = subtitleTracks,
                onSelectTrack = { track ->
                    selectSubtitleTrack(exoPlayer, track)
                },
                onDisableSubtitles = {
                    disableSubtitles(exoPlayer)
                },
                onDismiss = { showSubtitlesModal = false }
            )
        }
    }
}

private fun seekRelative(exoPlayer: ExoPlayer, offsetMs: Long) {
    val duration = exoPlayer.duration
    val current = exoPlayer.currentPosition
    val target = (current + offsetMs).coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)
    exoPlayer.seekTo(target)
}

fun formatMsToTime(ms: Long): String {
    if (ms <= 0) return "00:00:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600

    return String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

fun extractAudioTracks(tracks: Tracks): List<AudioTrackInfo> {
    val list = mutableListOf<AudioTrackInfo>()
    for (group in tracks.groups) {
        if (group.type == C.TRACK_TYPE_AUDIO) {
            val mediaGroup = group.mediaTrackGroup
            for (i in 0 until group.length) {
                val format = mediaGroup.getFormat(i)
                val isSelected = group.isTrackSelected(i)
                val lang = format.language?.uppercase() ?: "AUD"
                val rawLabel = format.label?.ifBlank { null }
                val friendlyLang = getFriendlyLanguageName(format.language)
                val displayLabel = rawLabel ?: friendlyLang ?: "Faixa ${list.size + 1}"
                list.add(AudioTrackInfo(group, i, displayLabel, lang, isSelected))
            }
        }
    }
    return list
}

fun extractSubtitleTracks(tracks: Tracks): List<SubtitleTrackInfo> {
    val list = mutableListOf<SubtitleTrackInfo>()
    for (group in tracks.groups) {
        if (group.type == C.TRACK_TYPE_TEXT) {
            val mediaGroup = group.mediaTrackGroup
            for (i in 0 until group.length) {
                val format = mediaGroup.getFormat(i)
                val isSelected = group.isTrackSelected(i)
                val lang = format.language?.uppercase() ?: "SUB"
                val rawLabel = format.label?.ifBlank { null }
                val friendlyLang = getFriendlyLanguageName(format.language)
                val displayLabel = rawLabel ?: friendlyLang ?: "Legenda ${list.size + 1}"
                list.add(SubtitleTrackInfo(group, i, displayLabel, lang, isSelected))
            }
        }
    }
    return list
}

fun getFriendlyLanguageName(langCode: String?): String? {
    if (langCode.isNullOrBlank()) return null
    val code = langCode.lowercase().trim()
    return when {
        code.startsWith("por") || code.startsWith("pt") -> "Português"
        code.startsWith("eng") || code.startsWith("en") -> "Inglês"
        code.startsWith("spa") || code.startsWith("es") -> "Espanhol"
        code.startsWith("fre") || code.startsWith("fra") || code.startsWith("fr") -> "Francês"
        code.startsWith("ger") || code.startsWith("deu") || code.startsWith("de") -> "Alemão"
        code.startsWith("ita") || code.startsWith("it") -> "Italiano"
        code.startsWith("jpn") || code.startsWith("ja") -> "Japonês"
        code.startsWith("chi") || code.startsWith("zho") || code.startsWith("zh") -> "Chinês"
        code.startsWith("rus") || code.startsWith("ru") -> "Russo"
        code.startsWith("und") -> null
        else -> langCode.uppercase()
    }
}

fun selectAudioTrack(exoPlayer: ExoPlayer, audioTrack: AudioTrackInfo) {
    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
        .buildUpon()
        .setOverrideForType(TrackSelectionOverride(audioTrack.group.mediaTrackGroup, audioTrack.trackIndex))
        .build()
}

fun selectSubtitleTrack(exoPlayer: ExoPlayer, subtitleTrack: SubtitleTrackInfo) {
    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
        .buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        .setOverrideForType(TrackSelectionOverride(subtitleTrack.group.mediaTrackGroup, subtitleTrack.trackIndex))
        .build()
}

fun disableSubtitles(exoPlayer: ExoPlayer) {
    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
        .buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        .build()
}

@Composable
private fun PlayerTopBar(
    channelName: String,
    listTitle: String,
    streamType: String? = null,
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
                    contentDescription = "Voltar",
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
                    text = "LC Player • ${listTitle.ifBlank { "Mídia" }}",
                    color = TvTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Badge indicador "AO VIVO", "FILME" ou "SÉRIE"
        val badgeLabel = when (streamType) {
            "movie" -> "FILME"
            "series" -> "SÉRIE"
            else -> "AO VIVO"
        }
        val badgeBg = when (streamType) {
            "movie", "series" -> Color(0x330084FF)
            else -> Color(0x33FF1744)
        }
        val badgeDotColor = when (streamType) {
            "movie", "series" -> TvSecondaryBlue
            else -> Color(0xFFFF1744)
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(badgeBg)
                .border(1.dp, badgeDotColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(badgeDotColor)
            )
            Text(
                text = badgeLabel,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PlayerBottomControls(
    channel: com.example.domain.model.LiveStreamChannel,
    epgState: ShortEpgState? = null,
    isPlaying: Boolean,
    currentPositionMs: Long = 0L,
    durationMs: Long = 0L,
    audioTracksCount: Int = 0,
    subtitleTracksCount: Int = 0,
    onPreviousChannel: () -> Unit = {},
    onNextChannel: () -> Unit = {},
    onSeekRelative: (offsetMs: Long) -> Unit = {},
    onSeekToPosition: (positionMs: Long) -> Unit = {},
    onPlayPauseToggle: () -> Unit,
    onRefresh: () -> Unit,
    onOpenAudioModal: () -> Unit = {},
    onOpenSubtitlesModal: () -> Unit = {},
    playPauseFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val streamTypeClean = channel.streamType?.lowercase()?.trim().orEmpty()
    val isVodContent = streamTypeClean == "movie" ||
                       streamTypeClean == "series" ||
                       streamTypeClean == "episode" ||
                       streamTypeClean == "vod"
    val isLiveTv = !isVodContent

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xDD101622))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // TOPO — BARRA INTERATIVA DE PROGRESSO E TEMPO (VOD)
        if (isVodContent && durationMs > 0) {
            var isDragging by remember { mutableStateOf(false) }
            var dragPositionMs by remember { mutableStateOf(0f) }

            val displayPosition = if (isDragging) dragPositionMs.toLong() else currentPositionMs

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = formatMsToTime(displayPosition),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Slider(
                    value = displayPosition.toFloat().coerceIn(0f, durationMs.toFloat()),
                    onValueChange = { newValue ->
                        isDragging = true
                        dragPositionMs = newValue
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        onSeekToPosition(dragPositionMs.toLong())
                    },
                    valueRange = 0f..durationMs.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = TvSecondaryBlue,
                        activeTrackColor = TvSecondaryBlue,
                        inactiveTrackColor = Color(0xFF1E2D42)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("vod_progress_slider")
                )

                Text(
                    text = formatMsToTime(durationMs),
                    color = TvTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // CENTRO — CONTROLES DE AÇÃO
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botão Anterior (Exibido para Live TV e Séries)
                if (isLiveTv || channel.streamType == "series") {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(TvSurface)
                            .border(1.dp, TvSurfaceVariant, CircleShape)
                            .tvFocusable(
                                shape = CircleShape,
                                focusBorderColor = TvFocusBorder,
                                focusedScale = 1.1f,
                                onClick = onPreviousChannel
                            )
                            .testTag("btn_player_prev_channel"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Anterior",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Botão -10s (Apenas para VOD)
                if (isVodContent) {
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(TvSurface)
                            .border(1.dp, TvSurfaceVariant, RoundedCornerShape(20.dp))
                            .tvFocusable(
                                shape = RoundedCornerShape(20.dp),
                                focusBorderColor = TvFocusBorder,
                                focusedScale = 1.08f,
                                onClick = { onSeekRelative(-10000L) }
                            )
                            .padding(horizontal = 10.dp)
                            .testTag("btn_player_seek_back_10"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "-10s",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Botão Play/Pause (Centralizado e em destaque)
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

                // Botão +10s (Apenas para VOD)
                if (isVodContent) {
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(TvSurface)
                            .border(1.dp, TvSurfaceVariant, RoundedCornerShape(20.dp))
                            .tvFocusable(
                                shape = RoundedCornerShape(20.dp),
                                focusBorderColor = TvFocusBorder,
                                focusedScale = 1.08f,
                                onClick = { onSeekRelative(10000L) }
                            )
                            .padding(horizontal = 10.dp)
                            .testTag("btn_player_seek_forward_10"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+10s",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Botão Próximo (Exibido para Live TV e Séries)
                if (isLiveTv || channel.streamType == "series") {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(TvSurface)
                            .border(1.dp, TvSurfaceVariant, CircleShape)
                            .tvFocusable(
                                shape = CircleShape,
                                focusBorderColor = TvFocusBorder,
                                focusedScale = 1.1f,
                                onClick = onNextChannel
                            )
                            .testTag("btn_player_next_channel"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Próximo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Botão Áudio (Apenas para VOD com mais de 1 faixa de áudio)
                if (isVodContent && audioTracksCount > 1) {
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(TvSurface)
                            .border(1.dp, TvFocusBorder, RoundedCornerShape(10.dp))
                            .tvFocusable(
                                shape = RoundedCornerShape(10.dp),
                                focusBorderColor = Color.White,
                                focusedScale = 1.08f,
                                onClick = onOpenAudioModal
                            )
                            .padding(horizontal = 10.dp)
                            .testTag("btn_player_audio_tracks"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = "Faixas de Áudio",
                                tint = TvSecondaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Áudio",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Botão Legendas (Apenas para VOD com faixas de legenda)
                if (isVodContent && subtitleTracksCount > 0) {
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(TvSurface)
                            .border(1.dp, TvFocusBorder, RoundedCornerShape(10.dp))
                            .tvFocusable(
                                shape = RoundedCornerShape(10.dp),
                                focusBorderColor = Color.White,
                                focusedScale = 1.08f,
                                onClick = onOpenSubtitlesModal
                            )
                            .padding(horizontal = 10.dp)
                            .testTag("btn_player_subtitle_tracks"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = "Legendas",
                                tint = TvSecondaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Legendas",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Botão Recarregar Stream (Compartilhado)
                Box(
                    modifier = Modifier
                        .size(38.dp)
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
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // RODAPÉ — INFORMAÇÕES DO CONTEÚDO (Título e Sinopse)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF142033)),
                contentAlignment = Alignment.Center
            ) {
                if (!channel.streamIcon.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.streamIcon,
                        contentDescription = "Logo de ${channel.name}",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = when (channel.streamType) {
                            "movie" -> Icons.Default.Movie
                            "series" -> Icons.Default.Tv
                            else -> Icons.Default.LiveTv
                        },
                        contentDescription = null,
                        tint = TvTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Título sempre visível em no máximo 1 linha com Ellipsis (...)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (channel.num != null) {
                        Text(
                            text = "#${channel.num}",
                            color = TvSecondaryBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = channel.name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                // Subtítulo / Sinopse inicialmente resumido em 1 linha com expansão/recolhimento
                var isSynopsisExpanded by remember { mutableStateOf(false) }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val subtitleText = when {
                        channel.streamType == "movie" ->
                            "FILME VOD ${if (durationMs > 0) "• " + formatMsToTime(durationMs) else ""}"
                        channel.streamType == "series" ->
                            "EPISÓDIO DE SÉRIE ${if (durationMs > 0) "• " + formatMsToTime(durationMs) else ""}"
                        epgState?.isLoading == true ->
                            "Carregando programação..."
                        epgState?.hasEpg == true -> {
                            val current = epgState.currentProgram
                            val next = epgState.nextProgram
                            val curT = current?.title.orEmpty()
                            val nextT = next?.let { " • Próximo: ${it.title}" }.orEmpty()
                            "$curT$nextT"
                        }
                        else -> "Programação indisponível"
                    }

                    Text(
                        text = subtitleText,
                        color = TvTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = if (isSynopsisExpanded) 4 else 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (channel.streamType == "series" || epgState?.hasEpg == true) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x22FFFFFF))
                                .clickable { isSynopsisExpanded = !isSynopsisExpanded }
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (isSynopsisExpanded) "Menos" else "...",
                                color = TvSecondaryBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioTrackSelectionModal(
    audioTracks: List<AudioTrackInfo>,
    onSelectTrack: (AudioTrackInfo) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF101826))
                .border(1.dp, TvFocusBorder, RoundedCornerShape(16.dp))
                .padding(20.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { }
        ) {
            Text(
                text = "Idioma do Áudio",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            audioTracks.forEach { track ->
                val isSelected = track.isSelected
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFF1A355B) else TvSurface)
                        .border(
                            1.dp,
                            if (isSelected) TvSecondaryBlue else TvSurfaceVariant,
                            RoundedCornerShape(10.dp)
                        )
                        .tvFocusable(
                            shape = RoundedCornerShape(10.dp),
                            focusBorderColor = Color.White,
                            onClick = {
                                onSelectTrack(track)
                                onDismiss()
                            }
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = track.label,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isSelected) {
                            Text(
                                text = "✓ Ativo",
                                color = StatusActiveText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SubtitleTrackSelectionModal(
    subtitleTracks: List<SubtitleTrackInfo>,
    onSelectTrack: (SubtitleTrackInfo) -> Unit,
    onDisableSubtitles: () -> Unit,
    onDismiss: () -> Unit
) {
    val isAnySelected = subtitleTracks.any { it.isSelected }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF101826))
                .border(1.dp, TvFocusBorder, RoundedCornerShape(16.dp))
                .padding(20.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { }
        ) {
            Text(
                text = "Legendas",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!isAnySelected) Color(0xFF1A355B) else TvSurface)
                    .border(
                        1.dp,
                        if (!isAnySelected) TvSecondaryBlue else TvSurfaceVariant,
                        RoundedCornerShape(10.dp)
                    )
                    .tvFocusable(
                        shape = RoundedCornerShape(10.dp),
                        focusBorderColor = Color.White,
                        onClick = {
                            onDisableSubtitles()
                            onDismiss()
                        }
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Desativadas",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (!isAnySelected) FontWeight.Bold else FontWeight.Medium
                    )
                    if (!isAnySelected) {
                        Text(
                            text = "✓ Ativo",
                            color = StatusActiveText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            subtitleTracks.forEach { track ->
                val isSelected = track.isSelected
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFF1A355B) else TvSurface)
                        .border(
                            1.dp,
                            if (isSelected) TvSecondaryBlue else TvSurfaceVariant,
                            RoundedCornerShape(10.dp)
                        )
                        .tvFocusable(
                            shape = RoundedCornerShape(10.dp),
                            focusBorderColor = Color.White,
                            onClick = {
                                onSelectTrack(track)
                                onDismiss()
                            }
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = track.label,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isSelected) {
                            Text(
                                text = "✓ Ativo",
                                color = StatusActiveText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
