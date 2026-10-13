package com.example.ui.lists

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.ActivationStatus
import com.example.domain.model.ConnectionTestDialogState
import com.example.domain.model.DeviceListModel
import com.example.domain.model.ListStatus
import com.example.domain.model.XtreamAuthResult
import com.example.ui.activation.ActivationUiState
import com.example.ui.activation.ActivationViewModel
import com.example.ui.activation.AppScreenSection
import com.example.ui.components.AppStatusBadge
import com.example.ui.components.tvFocusable
import com.example.ui.theme.AppCorners
import com.example.ui.theme.AppSpacing
import com.example.ui.theme.StatusActiveBg
import com.example.ui.theme.StatusActiveBorder
import com.example.ui.theme.StatusActiveText
import com.example.ui.theme.StatusBlockedBg
import com.example.ui.theme.StatusBlockedBorder
import com.example.ui.theme.StatusBlockedText
import com.example.ui.theme.StatusErrorBg
import com.example.ui.theme.StatusErrorBorder
import com.example.ui.theme.StatusErrorText
import com.example.ui.theme.StatusExpiredBg
import com.example.ui.theme.StatusExpiredBorder
import com.example.ui.theme.StatusExpiredText
import com.example.ui.theme.StatusWaitingBg
import com.example.ui.theme.StatusWaitingBorder
import com.example.ui.theme.StatusWaitingText
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reconstrução completa da Tela Principal (Home) do LC Player em Jetpack Compose.
 *
 * Estrutura:
 * - TOPO COMPACTO: Marca LC Player, Hora atual, Data atual e Status do Servidor (● Online / ● Indisponível).
 * - PAINEL CENTRALIZADO:
 *   - Lado Esquerdo: Card dominante "TV ONLINE" (altura equivalente a Filmes + Séries).
 *   - Lado Direito: Cards empilhados de "FILMES" e "SÉRIES".
 *   - Rodapé do Painel: Card horizontal "CONFIGURAÇÕES" abrangendo toda a largura.
 */
@Composable
fun DeviceListsScreen(
    viewModel: ActivationViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val refreshFocusRequester = remember { FocusRequester() }

    BackHandler {
        viewModel.navigateToSection(AppScreenSection.ACTIVATION)
    }

    LaunchedEffect(uiState.toastFeedback) {
        uiState.toastFeedback?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToastFeedback()
        }
    }

    LaunchedEffect(Unit) {
        try {
            refreshFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050912),
                        TvBackground,
                        Color(0xFF040810)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(450.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x1F0084FF), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.xl, vertical = AppSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOPO COMPACTO: LC Player | Hora | Data | Status
            CompactHomeHeaderBar(
                status = uiState.status,
                onBackClick = { viewModel.navigateToSection(AppScreenSection.ACTIVATION) }
            )

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                when (uiState.status) {
                    ActivationStatus.ACTIVE -> {
                        ActiveHomeDashboardContent(
                            uiState = uiState,
                            onListSelect = { viewModel.selectList(it) },
                            onOpenContent = { viewModel.openContentCategories(it) },
                            onTestConnection = { viewModel.testListConnection(it) },
                            onDeleteRequest = { viewModel.requestDeleteList(it) },
                            onRefreshClick = { viewModel.fetchDeviceLists(navigateToSectionIfActive = false) },
                            onGoToSettings = { viewModel.navigateToSection(AppScreenSection.ACTIVATION) },
                            refreshFocusRequester = refreshFocusRequester
                        )
                    }
                    ActivationStatus.BLOCKED -> {
                        DeviceStatusBlockedView()
                    }
                    ActivationStatus.EXPIRED -> {
                        DeviceStatusExpiredView()
                    }
                    else -> {
                        DeviceStatusWaitingView(
                            onGoToActivation = { viewModel.navigateToSection(AppScreenSection.ACTIVATION) }
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = AppSpacing.xl)
        )

        uiState.listPendingDeletion?.let { listToConfirm ->
            DeleteListConfirmDialog(
                list = listToConfirm,
                isDeleting = uiState.deletingListId == listToConfirm.id,
                onConfirm = { viewModel.confirmDeleteList(listToConfirm) },
                onDismiss = { viewModel.cancelDeleteList() }
            )
        }

        uiState.connectionTestDialogState?.let { dialogState ->
            ConnectionTestResultDialog(
                dialogState = dialogState,
                onDismiss = { viewModel.dismissConnectionTestDialog() }
            )
        }
    }
}

/**
 * TOPO COMPACTO:
 * LC Player | Hora local | Data local | Status do Servidor (● Online / ● Indisponível)
 */
@Composable
private fun CompactHomeHeaderBar(
    status: ActivationStatus,
    onBackClick: () -> Unit
) {
    var currentTimeText by remember { mutableStateOf("") }
    var currentDateText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        while (isActive) {
            val now = Date()
            currentTimeText = timeFormat.format(now)
            currentDateText = dateFormat.format(now)
            delay(1000)
        }
    }

    val isOnline = status == ActivationStatus.ACTIVE
    val statusText = if (isOnline) "● Online" else "● Indisponível"
    val statusColor = if (isOnline) Color(0xFF00E676) else Color(0xFFFF5252)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 880.dp)
            .padding(horizontal = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(AppCorners.md)
                    .background(TvSurfaceVariant)
                    .tvFocusable(
                        shape = AppCorners.md,
                        focusBorderColor = TvFocusBorder,
                        onClick = onBackClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = TvSecondaryBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Marca LC Player
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "LC",
                    color = TvPrimaryBlue,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Player",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Hora | Data | Status do Servidor
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (currentTimeText.isNotBlank()) {
                Text(
                    text = currentTimeText,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (currentDateText.isNotBlank()) {
                Text(
                    text = currentDateText,
                    color = TvTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .clip(AppCorners.pill)
                    .background(statusColor.copy(alpha = 0.15f))
                    .border(1.dp, statusColor.copy(alpha = 0.4f), AppCorners.pill)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * PAINEL CENTRALIZADO DA HOME:
 * Estrutura Estrutural Obrigatória:
 * TV ONLINE (Esq, Altura Dupla) | FILMES (Dir Topo) / SÉRIES (Dir Baixo)
 * CONFIGURAÇÕES (Rodapé abrangendo toda a largura)
 */
@Composable
private fun ActiveHomeDashboardContent(
    uiState: ActivationUiState,
    onListSelect: (String) -> Unit,
    onOpenContent: (DeviceListModel) -> Unit,
    onTestConnection: (DeviceListModel) -> Unit,
    onDeleteRequest: (DeviceListModel) -> Unit,
    onRefreshClick: () -> Unit,
    onGoToSettings: () -> Unit,
    refreshFocusRequester: FocusRequester
) {
    val activeList = uiState.lists.firstOrNull { it.status == ListStatus.ACTIVE } ?: uiState.lists.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 880.dp), // Painel Centralizado com largura máxima e margens laterais
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isWideLayout = this.maxWidth >= 600.dp

            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                if (isWideLayout) {
                    // PAINEL ESTRUTURAL HORIZONTAL / TV
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Linha Superior: TV ONLINE (Esq, Altura Dupla) + [FILMES / SÉRIES] (Dir, Empilhados)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(228.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Card TV ONLINE (Coluna Esquerda - Maior Destaque, Altura Dupla)
                            HomeHubCard(
                                title = "TV ONLINE",
                                subtitle = "Canais ao vivo com guia EPG",
                                icon = Icons.Default.LiveTv,
                                gradientColors = listOf(Color(0xFF00A0FF), Color(0xFF0056B3)),
                                isDominant = true,
                                onClick = { activeList?.let { onOpenContent(it) } },
                                modifier = Modifier
                                    .weight(1.1f)
                                    .fillMaxSize()
                            )

                            // Coluna Direita: FILMES (Topo) e SÉRIES (Baixo)
                            Column(
                                modifier = Modifier
                                    .weight(0.9f)
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Card FILMES
                                HomeHubCard(
                                    title = "FILMES",
                                    subtitle = "Lançamentos VOD",
                                    icon = Icons.Default.Movie,
                                    gradientColors = listOf(Color(0xFFFF3D71), Color(0xFFD32F2F)),
                                    isDominant = false,
                                    onClick = { activeList?.let { onOpenContent(it) } },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                )

                                // Card SÉRIES
                                HomeHubCard(
                                    title = "SÉRIES",
                                    subtitle = "Temporadas & Episódios",
                                    icon = Icons.Default.Tv,
                                    gradientColors = listOf(Color(0xFF7C4DFF), Color(0xFF3F51B5)),
                                    isDominant = false,
                                    onClick = { activeList?.let { onOpenContent(it) } },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                )
                            }
                        }

                        // Card CONFIGURAÇÕES (Ocupando toda a largura inferior)
                        HomeHubCard(
                            title = "CONFIGURAÇÕES",
                            subtitle = "Status do aparelho, testes e gerenciamento de listas",
                            icon = Icons.Default.Settings,
                            gradientColors = listOf(Color(0xFF1B2A45), Color(0xFF10192A)),
                            isDominant = false,
                            isCompactBar = true,
                            onClick = onGoToSettings,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                        )
                    }
                } else {
                    // LAYOUT ADAPTADO MOBILE VERTICAL
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // TV ONLINE (Topo, Largura total)
                        HomeHubCard(
                            title = "TV ONLINE",
                            subtitle = "Canais ao vivo com guia EPG",
                            icon = Icons.Default.LiveTv,
                            gradientColors = listOf(Color(0xFF00A0FF), Color(0xFF0056B3)),
                            isDominant = true,
                            onClick = { activeList?.let { onOpenContent(it) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(115.dp)
                        )

                        // FILMES & SÉRIES (Lado a lado em telas mobile)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HomeHubCard(
                                title = "FILMES",
                                subtitle = "Lançamentos VOD",
                                icon = Icons.Default.Movie,
                                gradientColors = listOf(Color(0xFFFF3D71), Color(0xFFD32F2F)),
                                isDominant = false,
                                onClick = { activeList?.let { onOpenContent(it) } },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                            )

                            HomeHubCard(
                                title = "SÉRIES",
                                subtitle = "Temporadas",
                                icon = Icons.Default.Tv,
                                gradientColors = listOf(Color(0xFF7C4DFF), Color(0xFF3F51B5)),
                                isDominant = false,
                                onClick = { activeList?.let { onOpenContent(it) } },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                            )
                        }

                        // CONFIGURAÇÕES (Abaixo, Largura total)
                        HomeHubCard(
                            title = "CONFIGURAÇÕES",
                            subtitle = "Aparelho e gerenciamento de listas",
                            icon = Icons.Default.Settings,
                            gradientColors = listOf(Color(0xFF1B2A45), Color(0xFF10192A)),
                            isDominant = false,
                            isCompactBar = true,
                            onClick = onGoToSettings,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                        )
                    }
                }
            }
        }

        // SEÇÃO DE LISTAS CONECTADAS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Minhas listas conectadas",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Selecione uma lista para explorar o catálogo completo",
                    color = TvTextSecondary,
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(AppCorners.md)
                    .background(if (uiState.isLoadingLists) Color(0xFF1B2433) else TvSurfaceVariant)
                    .border(1.dp, TvSurfaceVariant, AppCorners.md)
                    .focusRequester(refreshFocusRequester)
                    .tvFocusable(
                        enabled = !uiState.isLoadingLists,
                        shape = AppCorners.md,
                        focusBorderColor = TvFocusBorder,
                        onClick = onRefreshClick
                    )
                    .padding(horizontal = AppSpacing.md)
                    .testTag("btn_refresh_lists"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    if (uiState.isLoadingLists) {
                        CircularProgressIndicator(
                            color = TvSecondaryBlue,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = if (uiState.isLoadingLists) "Atualizando..." else "Atualizar",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (uiState.isLoadingLists && uiState.lists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    CircularProgressIndicator(
                        color = TvPrimaryBlue,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Sincronizando listas...",
                        color = TvTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else if (uiState.lists.isEmpty()) {
            EmptyListsView(onRefreshClick = onRefreshClick)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = AppSpacing.xxl),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                items(uiState.lists, key = { it.id }) { listModel ->
                    ListCardItem(
                        listModel = listModel,
                        isSelected = uiState.selectedListId == listModel.id,
                        isDeleting = uiState.deletingListId == listModel.id,
                        isTesting = uiState.testingConnectionListId == listModel.id,
                        lastTestResult = uiState.connectionTestResults[listModel.id],
                        onClick = {
                            onListSelect(listModel.id)
                            if (listModel.status == ListStatus.ACTIVE) {
                                onOpenContent(listModel)
                            }
                        },
                        onOpenContent = { onOpenContent(listModel) },
                        onTestConnection = { onTestConnection(listModel) },
                        onDeleteClick = { onDeleteRequest(listModel) }
                    )
                }
            }
        }
    }
}

/**
 * Card Reconstruído para o Painel da Home (TV ONLINE, FILMES, SÉRIES, CONFIGURAÇÕES).
 */
@Composable
private fun HomeHubCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    isDominant: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCompactBar: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(AppCorners.lg)
            .background(Brush.linearGradient(gradientColors))
            .tvFocusable(
                shape = AppCorners.lg,
                focusBorderColor = Color.White,
                focusedScale = 1.04f,
                onClick = onClick
            )
            .padding(horizontal = AppSpacing.lg, vertical = if (isCompactBar) 8.dp else AppSpacing.lg)
    ) {
        if (isCompactBar) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            color = Color(0xDDFFFFFF),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ArrowBack, // espelhado para avançar
                    contentDescription = null,
                    tint = Color(0xAAFFFFFF),
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isDominant) 48.dp else 40.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = Color.White,
                            modifier = Modifier.size(if (isDominant) 26.dp else 20.dp)
                        )
                    }

                    if (isDominant) {
                        Box(
                            modifier = Modifier
                                .clip(AppCorners.pill)
                                .background(Color(0x33FFFFFF))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "DESTAQUE",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = if (isDominant) 22.sp else 16.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = Color(0xDDFFFFFF),
                        fontSize = if (isDominant) 13.sp else 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ListCardItem(
    listModel: DeviceListModel,
    isSelected: Boolean,
    isDeleting: Boolean,
    isTesting: Boolean,
    lastTestResult: XtreamAuthResult?,
    onClick: () -> Unit,
    onOpenContent: () -> Unit,
    onTestConnection: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isBlockedOrExpired = listModel.status == ListStatus.BLOCKED || listModel.status == ListStatus.EXPIRED
    val isBusy = isDeleting || isTesting

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppCorners.lg)
            .background(TvCardBackground)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) TvPrimaryBlue else TvSurfaceVariant,
                shape = AppCorners.lg
            )
            .tvFocusable(
                enabled = !isBusy,
                shape = AppCorners.lg,
                focusBorderColor = TvFocusBorder,
                focusedScale = 1.03f,
                onClick = onClick
            )
            .padding(AppSpacing.lg)
            .testTag("list_card_${listModel.id}")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = listModel.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isSelected) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(AppCorners.pill)
                            .background(Color(0x330084FF))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selecionada",
                            tint = TvSecondaryBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Selecionada",
                            color = TvSecondaryBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = listModel.sourceName,
                color = TvTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AppStatusBadge(status = listModel.status)

                Text(
                    text = "Validade: ${listModel.formattedExpiration}",
                    color = TvTextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (lastTestResult != null) {
                val (badgeColor, badgeIcon, badgeText) = when (lastTestResult) {
                    is XtreamAuthResult.Success -> Triple(Color(0xFF00E676), Icons.Default.CheckCircle, "Servidor conectado")
                    is XtreamAuthResult.InvalidCredentials -> Triple(Color(0xFFFF5252), Icons.Default.ErrorOutline, "Usuário ou senha inválidos")
                    is XtreamAuthResult.Unavailable -> Triple(Color(0xFFFFB300), Icons.Default.SignalWifiOff, "Não foi possível conectar")
                    is XtreamAuthResult.IncompatibleResponse -> Triple(Color(0xFFFF5252), Icons.Default.ErrorOutline, lastTestResult.displayMessage)
                    is XtreamAuthResult.NotAllowed -> Triple(Color(0xFFFF5252), Icons.Default.ErrorOutline, lastTestResult.displayMessage)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppCorners.sm)
                        .background(badgeColor.copy(alpha = 0.15f))
                        .border(1.dp, badgeColor.copy(alpha = 0.4f), AppCorners.sm)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Ações do Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .height(38.dp)
                        .clip(AppCorners.md)
                        .background(if (isBlockedOrExpired || isBusy) Color(0xFF1B2433) else TvPrimaryBlue)
                        .tvFocusable(
                            enabled = !isBlockedOrExpired && !isBusy,
                            shape = AppCorners.md,
                            focusBorderColor = Color.White,
                            onClick = onOpenContent
                        )
                        .testTag("btn_open_list_${listModel.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Explorar conteúdo",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Explorar",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(AppCorners.md)
                        .background(TvSurfaceVariant)
                        .tvFocusable(
                            enabled = !isBusy,
                            shape = AppCorners.md,
                            focusBorderColor = TvFocusBorder,
                            onClick = onTestConnection
                        )
                        .testTag("btn_test_list_${listModel.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            color = TvSecondaryBlue,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Text(
                            text = "Testar",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(AppCorners.md)
                        .background(Color(0x22FF3D71))
                        .border(1.dp, Color(0x66FF3D71), AppCorners.md)
                        .tvFocusable(
                            enabled = !isBusy,
                            shape = AppCorners.md,
                            focusBorderColor = Color.White,
                            onClick = onDeleteClick
                        )
                        .testTag("btn_delete_list_${listModel.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            color = Color(0xFFFF3D71),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir lista",
                            tint = Color(0xFFFF5376),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyListsView(onRefreshClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(AppCorners.xl)
            .background(TvCardBackground)
            .border(1.dp, TvSurfaceVariant, AppCorners.xl)
            .padding(AppSpacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = TvSecondaryBlue,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Text(
                text = "Nenhuma lista configurada",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Cadastre uma lista no painel web em lcstreaming.com.br para começar.",
                color = TvTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            Box(
                modifier = Modifier
                    .height(42.dp)
                    .clip(AppCorners.md)
                    .background(TvPrimaryBlue)
                    .tvFocusable(
                        shape = AppCorners.md,
                        focusBorderColor = Color.White,
                        onClick = onRefreshClick
                    )
                    .padding(horizontal = AppSpacing.lg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Buscar Listas Novamente",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DeviceStatusBlockedView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppCorners.xl)
            .background(StatusBlockedBg)
            .border(1.5.dp, StatusBlockedBorder, AppCorners.xl)
            .padding(AppSpacing.xxl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = StatusBlockedText,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Text(
                text = "Dispositivo Bloqueado",
                color = StatusBlockedText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Acesso temporariamente bloqueado. Entre em contato com seu provedor.",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DeviceStatusExpiredView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppCorners.xl)
            .background(StatusExpiredBg)
            .border(1.5.dp, StatusExpiredBorder, AppCorners.xl)
            .padding(AppSpacing.xxl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = StatusExpiredText,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Text(
                text = "Licença Expirada",
                color = StatusExpiredText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Sua licença expirou. Renove no painel web em lcstreaming.com.br",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DeviceStatusWaitingView(
    onGoToActivation: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppCorners.xl)
            .background(StatusWaitingBg)
            .border(1.5.dp, StatusWaitingBorder, AppCorners.xl)
            .padding(AppSpacing.xxl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = StatusWaitingText,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Text(
                text = "Aguardando Ativação",
                color = StatusWaitingText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Dispositivo pendente de liberação no LC Admin.",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            Box(
                modifier = Modifier
                    .height(42.dp)
                    .clip(AppCorners.md)
                    .background(TvPrimaryBlue)
                    .tvFocusable(
                        shape = AppCorners.md,
                        focusBorderColor = Color.White,
                        onClick = onGoToActivation
                    )
                    .padding(horizontal = AppSpacing.lg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Verificar Status de Ativação",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DeleteListConfirmDialog(
    list: DeviceListModel,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        containerColor = TvSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFFF5252)
                )
                Text(
                    text = "Excluir Lista",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Text(
                    text = "Tem certeza que deseja remover a lista '${list.title}'?",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "Esta ação excluirá o vínculo da lista no LC Admin.",
                    color = TvTextSecondary,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting,
                modifier = Modifier
                    .clip(AppCorners.sm)
                    .tvFocusable(
                        enabled = !isDeleting,
                        shape = AppCorners.sm,
                        onClick = onConfirm
                    )
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        color = Color(0xFFFF5252),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = "Excluir",
                        color = Color(0xFFFF5252),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting,
                modifier = Modifier
                    .clip(AppCorners.sm)
                    .tvFocusable(
                        enabled = !isDeleting,
                        shape = AppCorners.sm,
                        onClick = onDismiss
                    )
            ) {
                Text(
                    text = "Cancelar",
                    color = TvTextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun ConnectionTestResultDialog(
    dialogState: ConnectionTestDialogState,
    onDismiss: () -> Unit
) {
    val isSuccess = dialogState.result is XtreamAuthResult.Success
    val title = if (isSuccess) "Conexão Bem-Sucedida" else "Falha no Teste"
    val message = when (val res = dialogState.result) {
        is XtreamAuthResult.Success -> "Autenticação realizada com sucesso na lista '${dialogState.listTitle}'. Status: ${res.accountStatus}."
        is XtreamAuthResult.InvalidCredentials -> res.displayMessage
        is XtreamAuthResult.Unavailable -> res.displayMessage
        is XtreamAuthResult.IncompatibleResponse -> res.displayMessage
        is XtreamAuthResult.NotAllowed -> res.displayMessage
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TvSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                val icon = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline
                val tint = if (isSuccess) Color(0xFF00E676) else Color(0xFFFF5252)
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint
                )
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Text(
                text = message,
                color = TvTextSecondary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .clip(AppCorners.sm)
                    .tvFocusable(
                        shape = AppCorners.sm,
                        onClick = onDismiss
                    )
            ) {
                Text(
                    text = stringResource(R.string.btn_close),
                    color = TvSecondaryBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
