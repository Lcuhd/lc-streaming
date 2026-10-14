package com.example.ui.categories

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as lazyGridItemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LiveTv
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.domain.model.DeviceListModel
import com.example.domain.model.LiveStreamChannel
import com.example.domain.model.LiveStreamsState
import com.example.domain.model.ShortEpgState
import com.example.domain.model.XtreamCategory
import com.example.ui.components.tvFocusable
import com.example.ui.theme.AppCorners
import com.example.ui.theme.AppSpacing
import com.example.ui.theme.StatusActiveText
import com.example.ui.theme.TvCardBackground
import com.example.ui.theme.TvFocusBorder
import com.example.ui.theme.TvPrimaryBlue
import com.example.ui.theme.TvSecondaryBlue
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextMuted
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.util.XtreamStreamUrlBuilder
import kotlinx.coroutines.delay

/**
 * Visualização dos canais de TV ao vivo com suporte responsivo:
 * - Telas Largas (TV 16:9 / Landscape): Layout em 3 colunas (Categorias | Canais | Preview + EPG) com debounce de 700ms no preview.
 * - Telas Estreitas (Celular Portrait): Layout mobile aprovado mantido 100% intacto.
 */
@Composable
fun CategoryChannelsView(
    category: XtreamCategory,
    allCategories: List<XtreamCategory> = emptyList(),
    activeList: DeviceListModel? = null,
    onSelectCategory: ((XtreamCategory) -> Unit)? = null,
    streamsState: LiveStreamsState?,
    channelEpgStates: Map<String, ShortEpgState> = emptyMap(),
    listTitle: String,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onChannelClick: (LiveStreamChannel) -> Unit,
    onChannelFocused: ((streamId: String) -> Unit)? = null,
    onIsFavorite: (suspend (String) -> Boolean)? = null,
    onToggleFavorite: (suspend (String) -> Boolean)? = null,
    modifier: Modifier = Modifier
) {
    val backButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            backButtonFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    val isLoading = streamsState?.isLoading ?: true
    val channels = streamsState?.channels ?: emptyList()
    val errorMessage = streamsState?.errorMessage

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideLayout = this.maxWidth >= 660.dp

        if (isWideLayout && allCategories.isNotEmpty()) {
            WideThreePaneChannelsView(
                category = category,
                allCategories = allCategories,
                activeList = activeList,
                onSelectCategory = onSelectCategory,
                channels = channels,
                isLoading = isLoading,
                errorMessage = errorMessage,
                epgStates = channelEpgStates,
                listTitle = listTitle,
                onBackClick = onBackClick,
                onRefreshClick = onRefreshClick,
                onChannelClick = onChannelClick,
                onChannelFocused = onChannelFocused,
                onIsFavorite = onIsFavorite,
                onToggleFavorite = onToggleFavorite,
                backFocusRequester = backButtonFocusRequester
            )
        } else {
            MobileChannelsView(
                categoryName = category.categoryName,
                channels = channels,
                isLoading = isLoading,
                errorMessage = errorMessage,
                epgStates = channelEpgStates,
                listTitle = listTitle,
                onBackClick = onBackClick,
                onRefreshClick = onRefreshClick,
                onChannelClick = onChannelClick,
                onChannelFocused = onChannelFocused,
                backFocusRequester = backButtonFocusRequester
            )
        }
    }
}

/**
 * Layout de 3 Áreas para Telas Largas / TV 16:9:
 * Coluna 1: Categorias | Coluna 2: Canais | Coluna 3: Preview + EPG
 */
@OptIn(UnstableApi::class)
@Composable
private fun WideThreePaneChannelsView(
    category: XtreamCategory,
    allCategories: List<XtreamCategory>,
    activeList: DeviceListModel?,
    onSelectCategory: ((XtreamCategory) -> Unit)?,
    channels: List<LiveStreamChannel>,
    isLoading: Boolean,
    errorMessage: String?,
    epgStates: Map<String, ShortEpgState>,
    listTitle: String,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onChannelClick: (LiveStreamChannel) -> Unit,
    onChannelFocused: ((streamId: String) -> Unit)?,
    onIsFavorite: (suspend (String) -> Boolean)? = null,
    onToggleFavorite: (suspend (String) -> Boolean)? = null,
    backFocusRequester: FocusRequester
) {
    var focusedChannel by remember(category.categoryId, channels) {
        mutableStateOf(channels.firstOrNull())
    }
    var previewStreamUrl by remember { mutableStateOf<String?>(null) }

    // Debounce de 700ms para iniciar a reprodução do preview de vídeo ao navegar pelo D-pad
    LaunchedEffect(focusedChannel, activeList) {
        previewStreamUrl = null
        if (focusedChannel != null && activeList != null && activeList.dns.isNotBlank()) {
            delay(700)
            val url = XtreamStreamUrlBuilder.buildLiveStreamUrl(
                rawDns = activeList.dns,
                username = activeList.username,
                password = activeList.password,
                streamId = focusedChannel!!.streamId,
                containerExtension = "m3u8"
            )
            previewStreamUrl = url
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = AppSpacing.xl, top = AppSpacing.md, end = AppSpacing.xl, bottom = AppSpacing.md)
    ) {
        ChannelsTopBar(
            categoryName = category.categoryName,
            channelCount = channels.size,
            isLoading = isLoading,
            listTitle = listTitle,
            onBackClick = onBackClick,
            onRefreshClick = onRefreshClick,
            backFocusRequester = backFocusRequester
        )

        Spacer(modifier = Modifier.height(AppSpacing.md))

        // Estrutura de 3 Colunas (Categorias | Canais | Preview + EPG)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            // COLUNA 1 — CATEGORIAS (200.dp)
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .clip(AppCorners.lg)
                    .background(TvCardBackground)
                    .border(1.dp, TvSurfaceVariant, AppCorners.lg)
                    .padding(AppSpacing.md)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    Text(
                        text = "CATEGORIAS",
                        color = TvTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(allCategories, key = { it.categoryId }) { catItem ->
                            val isSelected = catItem.categoryId == category.categoryId
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clip(AppCorners.md)
                                    .background(if (isSelected) TvPrimaryBlue else TvSurface)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color.White else TvSurfaceVariant,
                                        shape = AppCorners.md
                                    )
                                    .tvFocusable(
                                        shape = AppCorners.md,
                                        focusBorderColor = Color.White,
                                        onClick = { onSelectCategory?.invoke(catItem) }
                                    )
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = catItem.categoryName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // COLUNA 2 — CANAIS DA CATEGORIA (300.dp)
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .clip(AppCorners.lg)
                    .background(TvCardBackground)
                    .border(1.dp, TvSurfaceVariant, AppCorners.lg)
                    .padding(AppSpacing.md)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    Text(
                        text = "CANAIS (${channels.size})",
                        color = TvTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )

                    when {
                        isLoading && channels.isEmpty() -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = TvSecondaryBlue, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                            }
                        }
                        channels.isEmpty() -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "Nenhum canal encontrado", color = TvTextMuted, fontSize = 13.sp)
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(items = channels, key = { index: Int, channel: LiveStreamChannel -> "${channel.streamId}_$index" }) { index: Int, channel: LiveStreamChannel ->
                                    val isFocused = focusedChannel?.streamId == channel.streamId

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .clip(AppCorners.md)
                                            .background(if (isFocused) Color(0xFF132A4A) else TvSurface)
                                            .border(
                                                width = if (isFocused) 1.5.dp else 1.dp,
                                                color = if (isFocused) TvSecondaryBlue else TvSurfaceVariant,
                                                shape = AppCorners.md
                                            )
                                            .onFocusChanged { focusState ->
                                                if (focusState.isFocused) {
                                                    focusedChannel = channel
                                                    onChannelFocused?.invoke(channel.streamId)
                                                }
                                            }
                                            .tvFocusable(
                                                shape = AppCorners.md,
                                                focusBorderColor = Color.White,
                                                onClick = { onChannelClick(channel) }
                                            )
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(AppCorners.xs)
                                                    .background(Color(0xFF142033)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!channel.streamIcon.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = channel.streamIcon,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(2.dp),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Tv,
                                                        contentDescription = null,
                                                        tint = TvTextSecondary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    if (channel.num != null) {
                                                        Text(
                                                            text = "#${channel.num}",
                                                            color = TvSecondaryBlue,
                                                            fontSize = 11.sp,
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
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                val epg = epgStates[channel.streamId]
                                                if (epg?.hasEpg == true && epg.currentProgram != null) {
                                                    Text(
                                                        text = epg.currentProgram!!.title,
                                                        color = StatusActiveText,
                                                        fontSize = 10.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // COLUNA 3 — PREVIEW + EPG (Peso 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(AppCorners.lg)
                    .background(TvCardBackground)
                    .border(1.dp, TvSurfaceVariant, AppCorners.lg)
                    .padding(AppSpacing.lg)
            ) {
                if (focusedChannel != null) {
                    val curChannel = focusedChannel!!
                    val epg = epgStates[curChannel.streamId]

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                    ) {
                        // Janela de Preview de Vídeo
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(AppCorners.md)
                                .background(Color.Black)
                                .border(1.dp, TvSurfaceVariant, AppCorners.md),
                            contentAlignment = Alignment.Center
                        ) {
                            if (previewStreamUrl != null) {
                                LiveChannelPreviewPlayer(streamUrl = previewStreamUrl!!)
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(color = TvSecondaryBlue, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = "Aguardando foco para preview...", color = TvTextMuted, fontSize = 11.sp)
                                }
                            }
                        }

                        // Informações do Canal Selecionado + Botão Favoritar [♥]
                        var isChannelFavorite by remember { mutableStateOf(false) }
                        val coroutineScope = rememberCoroutineScope()

                        LaunchedEffect(curChannel.streamId) {
                            if (onIsFavorite != null) {
                                try {
                                    isChannelFavorite = onIsFavorite(curChannel.streamId)
                                } catch (_: Exception) {}
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(AppCorners.md)
                                        .background(Color(0xFF142033)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!curChannel.streamIcon.isNullOrBlank()) {
                                        AsyncImage(
                                            model = curChannel.streamIcon,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().padding(3.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Icon(imageVector = Icons.Default.LiveTv, contentDescription = null, tint = TvSecondaryBlue, modifier = Modifier.size(22.dp))
                                    }
                                }

                                Column {
                                    Text(
                                        text = "${curChannel.num?.let { "#$it • " } ?: ""}${curChannel.name}",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(text = "Sintonizado na TV ao vivo", color = TvSecondaryBlue, fontSize = 11.sp)
                                }
                            }

                            // Botão Favoritar Canal [♥]
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(AppCorners.md)
                                    .background(if (isChannelFavorite) Color(0x33FF3D71) else TvSurface)
                                    .border(1.dp, if (isChannelFavorite) Color(0xFFFF3D71) else TvSurfaceVariant, AppCorners.md)
                                    .tvFocusable(
                                        shape = AppCorners.md,
                                        focusBorderColor = Color.White,
                                        onClick = {
                                            coroutineScope.launch {
                                                if (onToggleFavorite != null) {
                                                    try {
                                                        isChannelFavorite = onToggleFavorite(curChannel.streamId)
                                                    } catch (_: Exception) {}
                                                } else {
                                                    isChannelFavorite = !isChannelFavorite
                                                }
                                            }
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isChannelFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favoritar canal",
                                    tint = if (isChannelFavorite) Color(0xFFFF3D71) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // EPG
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppCorners.md)
                                .background(TvSurface)
                                .padding(AppSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (epg?.hasEpg == true) {
                                val curProg = epg.currentProgram
                                val nextProg = epg.nextProgram

                                if (curProg != null) {
                                    Text(text = "● AO VIVO AGORA", color = StatusActiveText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(text = curProg.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    if (curProg.timeRangeFormatted.isNotBlank()) {
                                        Text(text = "Horário: ${curProg.timeRangeFormatted}", color = TvTextSecondary, fontSize = 11.sp)
                                    }
                                }

                                if (nextProg != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "PRÓXIMO PROGRAMA", color = TvTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "${nextProg.title}${if (!nextProg.startTimeFormatted.isNullOrBlank()) " (${nextProg.startTimeFormatted})" else ""}", color = TvTextSecondary, fontSize = 12.sp)
                                }
                            } else {
                                Text(text = "Guia EPG Indisponível", color = TvTextMuted, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Botão Ação Fullscreen
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(AppCorners.md)
                                .background(TvPrimaryBlue)
                                .tvFocusable(
                                    shape = AppCorners.md,
                                    focusBorderColor = Color.White,
                                    onClick = { onChannelClick(curChannel) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Text(text = "Assistir em Tela Cheia (OK)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Player leve para preview de vídeo em janela dentro da coluna 3.
 */
@OptIn(UnstableApi::class)
@Composable
private fun LiveChannelPreviewPlayer(streamUrl: String) {
    val context = LocalContext.current

    val previewExoPlayer = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    DisposableEffect(streamUrl, previewExoPlayer) {
        val mediaItem = MediaItem.fromUri(streamUrl)
        previewExoPlayer.setMediaItem(mediaItem)
        previewExoPlayer.prepare()
        previewExoPlayer.playWhenReady = true

        onDispose {
            previewExoPlayer.stop()
            previewExoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = previewExoPlayer
                useController = false
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Layout Mobile Aprovado (Preservado 100%).
 */
@Composable
private fun MobileChannelsView(
    categoryName: String,
    channels: List<LiveStreamChannel>,
    isLoading: Boolean,
    errorMessage: String?,
    epgStates: Map<String, ShortEpgState>,
    listTitle: String,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onChannelClick: (LiveStreamChannel) -> Unit,
    onChannelFocused: ((streamId: String) -> Unit)?,
    backFocusRequester: FocusRequester
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 32.dp, top = 14.dp, end = 32.dp, bottom = 16.dp)
    ) {
        ChannelsTopBar(
            categoryName = categoryName,
            channelCount = channels.size,
            isLoading = isLoading,
            listTitle = listTitle,
            onBackClick = onBackClick,
            onRefreshClick = onRefreshClick,
            backFocusRequester = backFocusRequester
        )

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                isLoading && channels.isEmpty() -> {
                    ChannelsLoadingView(categoryName = categoryName)
                }
                errorMessage != null && channels.isEmpty() -> {
                    ChannelsErrorView(
                        errorMessage = errorMessage,
                        onRetry = onRefreshClick,
                        onBack = onBackClick
                    )
                }
                channels.isEmpty() -> {
                    ChannelsEmptyView(
                        categoryName = categoryName,
                        onBack = onBackClick
                    )
                }
                else -> {
                    ChannelsGridView(
                        channels = channels,
                        epgStates = epgStates,
                        onChannelClick = onChannelClick,
                        onChannelFocused = onChannelFocused
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelsTopBar(
    categoryName: String,
    channelCount: Int,
    isLoading: Boolean,
    listTitle: String,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    backFocusRequester: FocusRequester
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TvSurface)
                    .border(1.dp, TvSurfaceVariant, RoundedCornerShape(10.dp))
                    .tvFocusable(
                        shape = RoundedCornerShape(10.dp),
                        focusBorderColor = TvFocusBorder,
                        focusedScale = 1.08f,
                        onClick = onBackClick
                    )
                    .focusRequester(backFocusRequester)
                    .testTag("btn_back_to_categories"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF142033)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = null,
                    tint = TvSecondaryBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = categoryName,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isLoading) "Carregando canais..." else "$channelCount canais disponíveis",
                    color = TvTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1A00C853))
                    .border(1.dp, Color(0x3300C853), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = listTitle.ifBlank { "Servidor" },
                    color = TvTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(StatusActiveText)
                )
            }

            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TvSurface)
                    .border(1.dp, TvSurfaceVariant, RoundedCornerShape(10.dp))
                    .tvFocusable(
                        shape = RoundedCornerShape(10.dp),
                        focusBorderColor = TvFocusBorder,
                        focusedScale = 1.05f,
                        onClick = onRefreshClick
                    )
                    .padding(horizontal = 12.dp)
                    .testTag("btn_refresh_channels"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = TvSecondaryBlue,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar canais",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "Atualizar",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelsLoadingView(categoryName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(
                color = TvPrimaryBlue,
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "Buscando canais de $categoryName...",
                color = TvTextSecondary,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun ChannelsErrorView(
    errorMessage: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = errorMessage,
                color = TvTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TvPrimaryBlue)
                        .tvFocusable(
                            shape = RoundedCornerShape(10.dp),
                            onClick = onRetry
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Tentar Novamente", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TvSurface)
                        .border(1.dp, TvSurfaceVariant, RoundedCornerShape(10.dp))
                        .tvFocusable(
                            shape = RoundedCornerShape(10.dp),
                            onClick = onBack
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Voltar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun ChannelsEmptyView(
    categoryName: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                tint = TvTextMuted,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "Nenhum canal encontrado nesta categoria",
                color = TvTextMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TvSurface)
                    .border(1.dp, TvSurfaceVariant, RoundedCornerShape(10.dp))
                    .tvFocusable(
                        shape = RoundedCornerShape(10.dp),
                        onClick = onBack
                    )
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Voltar para Categorias", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ChannelsGridView(
    channels: List<LiveStreamChannel>,
    epgStates: Map<String, ShortEpgState>,
    onChannelClick: (LiveStreamChannel) -> Unit,
    onChannelFocused: ((streamId: String) -> Unit)?
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 340.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 6.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        lazyGridItemsIndexed(
            items = channels,
            key = { index, channel -> "${channel.streamId}_$index" }
        ) { index, channel ->
            ChannelCardItem(
                channel = channel,
                index = index,
                epgState = epgStates[channel.streamId],
                onClick = { onChannelClick(channel) },
                onFocus = { onChannelFocused?.invoke(channel.streamId) }
            )
        }
    }
}

@Composable
private fun ChannelCardItem(
    channel: LiveStreamChannel,
    index: Int,
    epgState: ShortEpgState?,
    onClick: () -> Unit,
    onFocus: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 82.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TvCardBackground)
            .border(1.dp, TvSurfaceVariant, RoundedCornerShape(12.dp))
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onFocus()
                }
            }
            .tvFocusable(
                shape = RoundedCornerShape(12.dp),
                focusBorderColor = TvFocusBorder,
                focusedScale = 1.03f,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("channel_card_${channel.streamId}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F1826)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = channel.num?.toString() ?: "${index + 1}",
                    color = TvSecondaryBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF142033)),
                contentAlignment = Alignment.Center
            ) {
                if (!channel.streamIcon.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.streamIcon,
                        contentDescription = "Logo do canal ${channel.name}",
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
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = channel.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1B2E4B))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = (channel.streamType ?: "LIVE").uppercase(),
                            color = TvSecondaryBlue,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                when {
                    epgState?.isLoading == true -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                color = TvSecondaryBlue,
                                strokeWidth = 1.5.dp
                            )
                            Text(
                                text = "Buscando programação...",
                                color = TvTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    epgState?.hasEpg == true -> {
                        val current = epgState.currentProgram
                        if (current != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = current.title,
                                    color = StatusActiveText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (current.timeRangeFormatted.isNotBlank()) {
                                    Text(
                                        text = "• ${current.timeRangeFormatted}",
                                        color = TvTextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        Text(
                            text = "Sem guia EPG disponível",
                            color = TvTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
