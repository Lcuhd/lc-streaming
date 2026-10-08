package com.example.ui.categories

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.CategoryType
import com.example.domain.model.ListContentCategoriesState
import com.example.domain.model.XtreamCategory
import com.example.ui.activation.ActivationViewModel
import com.example.ui.components.TvScrollableRow
import com.example.ui.components.tvFocusable
import com.example.ui.theme.StatusActiveText
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvCardBackground
import com.example.ui.theme.TvFocusBorder
import com.example.ui.theme.TvPrimaryBlue
import com.example.ui.theme.TvSecondaryBlue
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextMuted
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary

/**
 * Tela de visualização das categorias reais do servidor Xtream (TV ao vivo, Filmes e Séries).
 *
 * Características principais:
 * - Cabeçalho compacto otimizado para TV (liberando espaço para o catálogo).
 * - Indicador discreto da lista atual e status do servidor ("TrendUHD ●").
 * - Device ID e informações técnicas acessíveis sob demanda na área de informações.
 * - Separação estrita entre Live TV, Filmes e Séries.
 * - Abas funcionais via controle remoto D-pad.
 */
@Composable
fun CategoriesScreen(
    viewModel: ActivationViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categoriesState = uiState.contentCategoriesState
    val snackbarHostState = remember { SnackbarHostState() }
    val initialFocusRequester = remember { FocusRequester() }
    var showDeviceInfoDialog by remember { mutableStateOf(false) }

    // Retorna para a tela de Minhas Listas ou fecha canais ao pressionar Voltar no controle remoto
    BackHandler {
        if (categoriesState?.selectedCategoryForChannels != null) {
            viewModel.closeLiveCategoryChannels()
        } else {
            viewModel.closeContentCategories()
        }
    }

    LaunchedEffect(uiState.toastFeedback) {
        uiState.toastFeedback?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToastFeedback()
        }
    }

    // Foco automático ao abrir a tela
    LaunchedEffect(Unit) {
        try {
            initialFocusRequester.requestFocus()
        } catch (_: Exception) {
        }
    }

    if (categoriesState == null) {
        // Fallback defensivo: se não houver estado, retorna para Minhas Listas
        LaunchedEffect(Unit) {
            viewModel.closeContentCategories()
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070B13),
                        TvBackground,
                        Color(0xFF060910)
                    )
                )
            )
    ) {
        // Brilho ambiente sutil
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(450.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x180084FF), Color.Transparent)
                    )
                )
        )

        // Se uma categoria de TV foi selecionada, exibe a visualização de seus canais reais
        if (categoriesState.selectedCategoryForChannels != null) {
            CategoryChannelsView(
                category = categoriesState.selectedCategoryForChannels!!,
                streamsState = categoriesState.liveStreamsState,
                listTitle = categoriesState.listTitle,
                onBackClick = { viewModel.closeLiveCategoryChannels() },
                onRefreshClick = { viewModel.retryLiveStreamsForCurrentCategory() },
                onChannelClick = { channel ->
                    viewModel.playLiveChannel(channel)
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 32.dp, top = 14.dp, end = 32.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Barra superior compacta (← LC Player • TrendUHD ●           Atualizar  Info)
                CategoriesTopBar(
                    listTitle = categoriesState.listTitle,
                    isConnected = categoriesState.liveError == null || categoriesState.vodError == null || categoriesState.seriesError == null,
                    isRefreshing = categoriesState.isAnyLoading,
                    onBackClick = { viewModel.closeContentCategories() },
                    onRefreshClick = { viewModel.refreshAllCategoriesForCurrentList() },
                    onInfoClick = { showDeviceInfoDialog = true }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Seletor de Tipo de Conteúdo (TV ao vivo, Filmes, Séries)
                ContentSectionSelector(
                    categoriesState = categoriesState,
                    onSelectTab = { viewModel.selectCategoryTab(it) },
                    initialFocusRequester = initialFocusRequester
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Área de exibição das categorias da aba selecionada (prioridade total para o catálogo)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    CategoryGridContent(
                        categoriesState = categoriesState,
                        selectedTab = categoriesState.selectedTab,
                        onRetryType = { viewModel.refreshCategoryType(it) },
                        onCategoryClick = { category ->
                            viewModel.openCategory(category)
                        }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )

        // Modal de Informações do Dispositivo (sob demanda)
        if (showDeviceInfoDialog) {
            DeviceInfoDialog(
                deviceId = uiState.deviceId,
                key = uiState.key,
                deviceStatus = uiState.status.title,
                currentListTitle = categoriesState.listTitle,
                sourceName = categoriesState.sourceName,
                onDismiss = { showDeviceInfoDialog = false }
            )
        }
    }
}

/**
 * Cabeçalho compacto em linha única:
 * - Esquerda: Botão Voltar [←], nome LC Player, separador e "TrendUHD ●".
 * - Direita: Botão Atualizar e Botão Info (Informações do dispositivo).
 */
@Composable
private fun CategoriesTopBar(
    listTitle: String,
    isConnected: Boolean,
    isRefreshing: Boolean,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 1400.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Lado Esquerdo: Botão Voltar + LC Player + Lista Atual com Ponto Discreto
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Botão Voltar para Minhas Listas
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TvSurfaceVariant)
                    .tvFocusable(
                        shape = RoundedCornerShape(10.dp),
                        focusBorderColor = TvFocusBorder,
                        focusedScale = 1.08f,
                        onClick = onBackClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar para Minhas Listas",
                    tint = TvSecondaryBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = "LC Player",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "•",
                color = TvTextMuted,
                fontSize = 14.sp
            )

            // Indicador discreto da lista atual e status do servidor ("TrendUHD ●")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0x220084FF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = listTitle,
                    color = TvSecondaryBlue,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Ponto indicador discreto de conexão com o servidor
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) Color(0xFF00E676) else Color(0xFFFFB300))
                )
            }
        }

        // Lado Direito: Ações (Atualizar + Informações do Dispositivo)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Botão Atualizar categorias
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isRefreshing) Color(0xFF1B2433) else TvSurfaceVariant)
                    .border(
                        width = 1.dp,
                        color = if (isRefreshing) TvFocusBorder else Color(0x330084FF),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .tvFocusable(
                        enabled = !isRefreshing,
                        shape = RoundedCornerShape(10.dp),
                        focusBorderColor = TvFocusBorder,
                        focusedScale = 1.05f,
                        onClick = if (!isRefreshing) onRefreshClick else null
                    )
                    .padding(horizontal = 14.dp)
                    .testTag("btn_refresh_categories"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            color = TvSecondaryBlue,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Carregando...",
                            color = TvSecondaryBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar categorias",
                            tint = TvSecondaryBlue,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Atualizar",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Botão discreto para consultar Informações do Dispositivo
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TvSurfaceVariant)
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                    .tvFocusable(
                        shape = RoundedCornerShape(10.dp),
                        focusBorderColor = TvFocusBorder,
                        focusedScale = 1.05f,
                        onClick = onInfoClick
                    )
                    .padding(horizontal = 12.dp)
                    .testTag("btn_device_info"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Informações do dispositivo",
                        tint = TvTextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Info",
                        color = TvTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Seletor horizontal rolável com as 3 abas principais: TV ao vivo, Filmes e Séries.
 *
 * Características:
 * - Preserva largura, espaçamento e legibilidade dos botões sem compressão em portrait.
 * - Suporta rolagem suave por toque em telas móveis/touchscreens.
 * - Suporta navegação por D-pad em Android TV / TV Box com rolagem automática para a viewport
 *   ao mover o foco (bring-into-view), garantindo que o item focado nunca fique escondido.
 * - Em landscape/16:9, se todos os itens couberem, não realiza scroll visual.
 */
@Composable
private fun ContentSectionSelector(
    categoriesState: ListContentCategoriesState,
    onSelectTab: (CategoryType) -> Unit,
    initialFocusRequester: FocusRequester
) {
    val tab0Requester = initialFocusRequester
    val tab1Requester = remember { FocusRequester() }
    val tab2Requester = remember { FocusRequester() }

    TvScrollableRow(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 1400.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryTabItem(
            title = "TV ao vivo",
            icon = Icons.Default.LiveTv,
            type = CategoryType.LIVE,
            count = categoriesState.liveCategories.size,
            isLoading = categoriesState.isLoadingLive,
            hasError = categoriesState.liveError != null,
            isSelected = categoriesState.selectedTab == CategoryType.LIVE,
            modifier = Modifier
                .focusRequester(tab0Requester)
                .focusProperties {
                    right = tab1Requester
                },
            onClick = { onSelectTab(CategoryType.LIVE) }
        )

        CategoryTabItem(
            title = "Filmes",
            icon = Icons.Default.Movie,
            type = CategoryType.VOD,
            count = categoriesState.vodCategories.size,
            isLoading = categoriesState.isLoadingVod,
            hasError = categoriesState.vodError != null,
            isSelected = categoriesState.selectedTab == CategoryType.VOD,
            modifier = Modifier
                .focusRequester(tab1Requester)
                .focusProperties {
                    left = tab0Requester
                    right = tab2Requester
                },
            onClick = { onSelectTab(CategoryType.VOD) }
        )

        CategoryTabItem(
            title = "Séries",
            icon = Icons.Default.VideoLibrary,
            type = CategoryType.SERIES,
            count = categoriesState.seriesCategories.size,
            isLoading = categoriesState.isLoadingSeries,
            hasError = categoriesState.seriesError != null,
            isSelected = categoriesState.selectedTab == CategoryType.SERIES,
            modifier = Modifier
                .focusRequester(tab2Requester)
                .focusProperties {
                    left = tab1Requester
                },
            onClick = { onSelectTab(CategoryType.SERIES) }
        )
    }
}

@Composable
private fun CategoryTabItem(
    title: String,
    icon: ImageVector,
    type: CategoryType,
    count: Int,
    isLoading: Boolean,
    hasError: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFF132A4A) else TvCardBackground
    val borderColor = if (isSelected) TvPrimaryBlue else TvSurfaceVariant

    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .tvFocusable(
                shape = RoundedCornerShape(12.dp),
                focusBorderColor = TvFocusBorder,
                focusedScale = 1.05f,
                onClick = onClick
            )
            .padding(horizontal = 18.dp)
            .testTag("tab_${type.name.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF40C4FF) else TvTextSecondary,
                modifier = Modifier.size(19.dp)
            )

            Text(
                text = title,
                color = if (isSelected) Color.White else TvTextSecondary,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )

            // Badge com a quantidade de categorias ou status de loading/erro
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            isLoading -> Color(0x330084FF)
                            hasError -> Color(0x33FF5252)
                            isSelected -> Color(0x440084FF)
                            else -> TvSurfaceVariant
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                if (isLoading) {
                    Text(
                        text = "...",
                        color = TvSecondaryBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (hasError) {
                    Text(
                        text = "!",
                        color = Color(0xFFFF8A80),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "$count",
                        color = if (isSelected) Color.White else TvTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Diálogo de Informações do Dispositivo / Configurações.
 * Apresenta Device ID, KEY, status do aparelho e informações técnicas
 * de forma organizada, sem ocupar espaço fixo na tela principal.
 */
@Composable
private fun DeviceInfoDialog(
    deviceId: String,
    key: String,
    deviceStatus: String,
    currentListTitle: String,
    sourceName: String,
    onDismiss: () -> Unit
) {
    val closeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            closeFocusRequester.requestFocus()
        } catch (_: Exception) {
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = TvCardBackground,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = TvSecondaryBlue,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Informações do Dispositivo",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoFieldRow(label = "Device ID", value = deviceId)
                InfoFieldRow(label = "KEY", value = key)
                InfoFieldRow(label = "Status do Dispositivo", value = deviceStatus)
                InfoFieldRow(label = "Lista Selecionada", value = currentListTitle)
                if (sourceName.isNotBlank()) {
                    InfoFieldRow(label = "Fonte do Conteúdo", value = sourceName)
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(TvPrimaryBlue)
                    .tvFocusable(
                        shape = RoundedCornerShape(8.dp),
                        focusBorderColor = TvFocusBorder,
                        focusedScale = 1.05f,
                        onClick = onDismiss
                    )
                    .focusRequester(closeFocusRequester)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Fechar",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun InfoFieldRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TvSurface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = TvTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Grid de categorias da aba selecionada (Live TV, Filmes ou Séries).
 */
@Composable
private fun CategoryGridContent(
    categoriesState: ListContentCategoriesState,
    selectedTab: CategoryType,
    onRetryType: (CategoryType) -> Unit,
    onCategoryClick: (XtreamCategory) -> Unit
) {
    val categories = categoriesState.getCategoriesForType(selectedTab)
    val isLoading = categoriesState.isLoadingForType(selectedTab)
    val error = categoriesState.getErrorForType(selectedTab)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 1400.dp)
    ) {
        // Título da seção + contagem
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = selectedTab.displayName,
                color = TvTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            if (!isLoading && error == null) {
                Text(
                    text = "${categories.size} categorias encontradas",
                    color = TvTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                isLoading && categories.isEmpty() -> {
                    // Estado de Carregamento (Requisito 8)
                    CategoryLoadingView(selectedTab = selectedTab)
                }
                error != null && categories.isEmpty() -> {
                    // Tratamento de Erros (Requisito 9)
                    CategoryErrorView(
                        errorMessage = error,
                        onRetry = { onRetryType(selectedTab) }
                    )
                }
                categories.isEmpty() -> {
                    // Estado Vazio (Requisito 7)
                    CategoryEmptyView(selectedTab = selectedTab)
                }
                else -> {
                    // Lista / Grid de categorias reais (Requisito 4, 5, 6)
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 280.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(categories, key = { "${it.type.name}_${it.categoryId}" }) { category ->
                            CategoryCardItem(
                                category = category,
                                onClick = {
                                    onCategoryClick(category)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCardItem(
    category: XtreamCategory,
    onClick: () -> Unit
) {
    val icon = when (category.type) {
        CategoryType.LIVE -> Icons.Default.LiveTv
        CategoryType.VOD -> Icons.Default.Movie
        CategoryType.SERIES -> Icons.Default.VideoLibrary
    }

    val countLabel = category.itemCount?.let { count ->
        when (category.type) {
            CategoryType.LIVE -> if (count == 1) "1 canal" else "$count canais"
            CategoryType.VOD -> if (count == 1) "1 filme" else "$count filmes"
            CategoryType.SERIES -> if (count == 1) "1 série" else "$count séries"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(TvCardBackground)
            .border(1.dp, TvSurfaceVariant, RoundedCornerShape(14.dp))
            .tvFocusable(
                shape = RoundedCornerShape(14.dp),
                focusBorderColor = TvFocusBorder,
                focusedScale = 1.05f,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("category_card_${category.categoryId}")
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Ícone temático da categoria
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF142033)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TvSecondaryBlue,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Nome da categoria e contagem de itens
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = category.categoryName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (countLabel != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = countLabel,
                        color = TvSecondaryBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Badge com contagem ou ID da categoria
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(TvSurfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = countLabel ?: "#${category.categoryId}",
                    color = if (countLabel != null) Color.White else TvTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CategoryLoadingView(selectedTab: CategoryType) {
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
                text = "Carregando categorias de ${selectedTab.displayName}...",
                color = TvTextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CategoryEmptyView(selectedTab: CategoryType) {
    val emptyMessage = when (selectedTab) {
        CategoryType.LIVE -> "Nenhuma categoria de TV encontrada"
        CategoryType.VOD -> "Nenhuma categoria de filmes encontrada"
        CategoryType.SERIES -> "Nenhuma categoria de séries encontrada"
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(TvSurface)
                .padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(TvSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inbox,
                    contentDescription = null,
                    tint = TvSecondaryBlue,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = emptyMessage,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "O servidor não retornou categorias registradas para este tipo de conteúdo.",
                color = TvTextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CategoryErrorView(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(TvSurface)
                .padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FF5252)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = errorMessage,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TvPrimaryBlue)
                    .tvFocusable(
                        shape = RoundedCornerShape(10.dp),
                        focusBorderColor = TvFocusBorder,
                        focusedScale = 1.06f,
                        onClick = onRetry
                    )
                    .padding(horizontal = 24.dp)
                    .testTag("btn_retry_categories"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Tentar novamente",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
