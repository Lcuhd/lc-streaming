package com.example.ui.lists

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tv
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ActivationStatus
import com.example.domain.model.ConnectionTestDialogState
import com.example.domain.model.DeviceListModel
import com.example.domain.model.ListStatus
import com.example.domain.model.XtreamAuthResult
import com.example.ui.activation.ActivationUiState
import com.example.ui.activation.ActivationViewModel
import com.example.ui.activation.AppScreenSection
import com.example.ui.components.tvFocusable
import com.example.ui.theme.StatusActiveBg
import com.example.ui.theme.StatusActiveBorder
import com.example.ui.theme.StatusActiveText
import com.example.ui.theme.StatusBlockedBg
import com.example.ui.theme.StatusBlockedBorder
import com.example.ui.theme.StatusBlockedText
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

@Composable
fun DeviceListsScreen(
    viewModel: ActivationViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val refreshFocusRequester = remember { FocusRequester() }

    // Intercepta o botão voltar do controle remoto para retornar à tela de ativação
    BackHandler {
        viewModel.navigateToSection(AppScreenSection.ACTIVATION)
    }

    LaunchedEffect(uiState.toastFeedback) {
        uiState.toastFeedback?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToastFeedback()
        }
    }

    // Foca automaticamente no botão de ação da TV
    LaunchedEffect(Unit) {
        try {
            refreshFocusRequester.requestFocus()
        } catch (_: Exception) {
        }
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
        // Brilho ambiente TV
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(400.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x1A0084FF), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Barra Superior de Navegação TV
            ListsHeaderBar(
                deviceId = uiState.deviceId,
                onBackClick = { viewModel.navigateToSection(AppScreenSection.ACTIVATION) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Barra de Ação Superior (Título + Botão Atualizar Listas)
            ListsActionHeader(
                isUpdating = uiState.isLoadingLists || uiState.deletingListId != null || uiState.testingConnectionListId != null,
                onRefreshClick = { viewModel.fetchDeviceLists(navigateToSectionIfActive = false) },
                refreshFocusRequester = refreshFocusRequester
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Conteúdo principal baseado no status do dispositivo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                when (uiState.status) {
                    ActivationStatus.ACTIVE -> {
                        ActiveDeviceListsContent(
                            uiState = uiState,
                            onListSelect = { viewModel.selectList(it) },
                            onOpenContent = { viewModel.openContentCategories(it) },
                            onTestConnection = { viewModel.testListConnection(it) },
                            onDeleteRequest = { viewModel.requestDeleteList(it) },
                            onRefreshClick = { viewModel.fetchDeviceLists(navigateToSectionIfActive = false) }
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
                .padding(bottom = 24.dp)
        )

        // Diálogo de confirmação obrigatória de exclusão de lista (Requisito 4)
        uiState.listPendingDeletion?.let { listToConfirm ->
            DeleteListConfirmDialog(
                list = listToConfirm,
                isDeleting = uiState.deletingListId == listToConfirm.id,
                onConfirm = { viewModel.confirmDeleteList(listToConfirm) },
                onDismiss = { viewModel.cancelDeleteList() }
            )
        }

        // Diálogo de resultado do teste de conexão Xtream (Requisitos 6, 7, 8, 9)
        uiState.connectionTestDialogState?.let { dialogState ->
            ConnectionTestResultDialog(
                dialogState = dialogState,
                onDismiss = { viewModel.dismissConnectionTestDialog() }
            )
        }
    }
}

@Composable
private fun ListsHeaderBar(
    deviceId: String,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 1100.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Botão Voltar para a tela de Identificação
            Box(
                modifier = Modifier
                    .size(42.dp)
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
                    contentDescription = "Voltar para identificação",
                    tint = TvSecondaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = "LC Player",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Minhas listas",
                    color = TvSecondaryBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Chip de identificação do dispositivo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(TvSurfaceVariant)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = TvSecondaryBlue,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Device ID: $deviceId",
                color = TvTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ListsActionHeader(
    isUpdating: Boolean,
    onRefreshClick: () -> Unit,
    refreshFocusRequester: FocusRequester
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 1100.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Minhas listas",
                color = TvTextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Selecione uma lista de conteúdo, teste a conexão ou gerencie as opções",
                color = TvTextSecondary,
                fontSize = 13.sp
            )
        }

        // Botão Atualizar Listas (requisito 13)
        Box(
            modifier = Modifier
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isUpdating) Color(0xFF1B2433) else TvPrimaryBlue)
                .focusRequester(refreshFocusRequester)
                .tvFocusable(
                    enabled = !isUpdating,
                    shape = RoundedCornerShape(12.dp),
                    focusBorderColor = TvFocusBorder,
                    focusedScale = if (!isUpdating) 1.05f else 1.0f,
                    onClick = if (!isUpdating) onRefreshClick else null
                )
                .padding(horizontal = 18.dp)
                .testTag("btn_refresh_lists"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        color = TvSecondaryBlue,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Atualizando...",
                        color = TvSecondaryBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Atualizar listas",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Atualizar listas",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveDeviceListsContent(
    uiState: ActivationUiState,
    onListSelect: (String) -> Unit,
    onOpenContent: (DeviceListModel) -> Unit,
    onTestConnection: (DeviceListModel) -> Unit,
    onDeleteRequest: (DeviceListModel) -> Unit,
    onRefreshClick: () -> Unit
) {
    if (uiState.isLoadingLists && uiState.lists.isEmpty()) {
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
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Carregando listas vinculadas...",
                    color = TvTextSecondary,
                    fontSize = 15.sp
                )
            }
        }
    } else if (uiState.lists.isEmpty()) {
        // Requisito 11 e Requisito 7: Dispositivo ativo sem listas
        EmptyListsView(onRefreshClick = onRefreshClick)
    } else {
        // Grid de listas para visualização e navegação em TV
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 340.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1100.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
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
    val (statusBg, statusBorder, statusText) = when (listModel.status) {
        ListStatus.ACTIVE -> Triple(StatusActiveBg, StatusActiveBorder, StatusActiveText)
        ListStatus.BLOCKED -> Triple(StatusBlockedBg, StatusBlockedBorder, StatusBlockedText)
        ListStatus.EXPIRED -> Triple(StatusExpiredBg, StatusExpiredBorder, StatusExpiredText)
    }

    val isBlockedOrExpired = listModel.status == ListStatus.BLOCKED || listModel.status == ListStatus.EXPIRED
    val isBusy = isDeleting || isTesting

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TvCardBackground)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) TvPrimaryBlue else TvSurfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .tvFocusable(
                enabled = !isBusy,
                shape = RoundedCornerShape(16.dp),
                focusBorderColor = TvFocusBorder,
                focusedScale = 1.03f,
                onClick = onClick
            )
            .padding(18.dp)
            .testTag("list_card_${listModel.id}")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cabeçalho do Card: Título + Badge de Seleção
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = listModel.title,
                    color = Color.White,
                    fontSize = 19.sp,
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
                            .clip(RoundedCornerShape(12.dp))
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

            // Nome da fonte (servidor)
            Text(
                text = listModel.sourceName,
                color = TvTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            // Linha de Status da lista + Data de validade
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Status da lista: Ativa, Bloqueada ou Vencida
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .border(1.dp, statusBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusText)
                    )
                    Text(
                        text = listModel.status.label,
                        color = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Validade
                Text(
                    text = "Validade: ${listModel.formattedExpiration}",
                    color = if (isBlockedOrExpired) statusText else TvTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Indicador de resultado recente do teste de conexão (se houver)
            if (lastTestResult != null) {
                val (badgeColor, badgeIcon, badgeText) = when (lastTestResult) {
                    is XtreamAuthResult.Success -> Triple(StatusActiveText, Icons.Default.CheckCircle, "Servidor conectado")
                    is XtreamAuthResult.InvalidCredentials -> Triple(Color(0xFFFF5252), Icons.Default.ErrorOutline, "Usuário ou senha inválidos")
                    is XtreamAuthResult.Unavailable -> Triple(Color(0xFFFFB300), Icons.Default.SignalWifiOff, "Não foi possível conectar")
                    is XtreamAuthResult.IncompatibleResponse -> Triple(Color(0xFFFF7043), Icons.Default.Warning, "Resposta não reconhecida")
                    is XtreamAuthResult.NotAllowed -> Triple(StatusBlockedText, Icons.Default.Lock, lastTestResult.displayMessage)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF161E2E))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Linha de Ações da Lista: Conteúdo + Testar Conexão + Excluir Lista
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ações para listas ativas: Conteúdo + Testar Conexão
                if (listModel.status == ListStatus.ACTIVE) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Ação Primária: Conteúdo (categorias)
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TvPrimaryBlue)
                                .tvFocusable(
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(8.dp),
                                    focusBorderColor = TvFocusBorder,
                                    focusedScale = 1.06f,
                                    onClick = onOpenContent
                                )
                                .padding(horizontal = 12.dp)
                                .testTag("btn_content_${listModel.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = "Ver conteúdo",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Conteúdo",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Ação Secundária: Testar Conexão
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isTesting) Color(0xFF1B2433) else Color(0xFF13223A))
                                .border(1.dp, if (isTesting) TvFocusBorder else TvPrimaryBlue.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .tvFocusable(
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(8.dp),
                                    focusBorderColor = TvFocusBorder,
                                    focusedScale = 1.06f,
                                    onClick = onTestConnection
                                )
                                .padding(horizontal = 10.dp)
                                .testTag("btn_test_connection_${listModel.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(
                                        color = TvSecondaryBlue,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Testando...",
                                        color = TvSecondaryBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.NetworkCheck,
                                        contentDescription = "Testar conexão",
                                        tint = TvSecondaryBlue,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Testar",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Indicação de lista bloqueada ou vencida
                    Text(
                        text = "Conexão indisponível",
                        color = TvTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Ação 2: Excluir Lista
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF23161C))
                        .border(1.dp, Color(0x66FF3D71), RoundedCornerShape(8.dp))
                        .tvFocusable(
                            enabled = !isBusy,
                            shape = RoundedCornerShape(8.dp),
                            focusBorderColor = Color(0xFFFF5252),
                            focusedScale = 1.06f,
                            onClick = onDeleteClick
                        )
                        .padding(horizontal = 12.dp)
                        .testTag("btn_delete_list_${listModel.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Excluir lista ${listModel.title}",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Excluir",
                            color = Color(0xFFFF8A80),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Overlay de carregamento durante a exclusão da lista
        if (isDeleting) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xDD090D16)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFFF5252),
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Excluindo lista...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Diálogo de resultado do teste de autenticação com o servidor Xtream.
 * Exibe apenas informações seguras e não sensíveis de diagnóstico.
 * NUNCA exibe username, password ou URL com credenciais.
 */
@Composable
private fun ConnectionTestResultDialog(
    dialogState: ConnectionTestDialogState,
    onDismiss: () -> Unit
) {
    val result = dialogState.result

    val (icon, iconTint, iconBg, titleText) = when (result) {
        is XtreamAuthResult.Success -> Quadruple(
            Icons.Default.CheckCircle,
            StatusActiveText,
            StatusActiveBg,
            result.displayMessage // "Servidor conectado"
        )
        is XtreamAuthResult.InvalidCredentials -> Quadruple(
            Icons.Default.ErrorOutline,
            Color(0xFFFF5252),
            Color(0x33FF3D71),
            result.displayMessage // "Usuário ou senha inválidos"
        )
        is XtreamAuthResult.Unavailable -> Quadruple(
            Icons.Default.SignalWifiOff,
            Color(0xFFFFB300),
            Color(0x33FFB300),
            result.displayMessage // "Não foi possível conectar ao servidor"
        )
        is XtreamAuthResult.IncompatibleResponse -> Quadruple(
            Icons.Default.Warning,
            Color(0xFFFF7043),
            Color(0x33FF7043),
            result.displayMessage // "Resposta do servidor não reconhecida"
        )
        is XtreamAuthResult.NotAllowed -> Quadruple(
            Icons.Default.Lock,
            StatusBlockedText,
            StatusBlockedBg,
            result.displayMessage
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TvSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = titleText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                    Text(
                        text = "Lista: ${dialogState.listTitle}",
                        color = TvSecondaryBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (result) {
                    is XtreamAuthResult.Success -> {
                        Text(
                            text = "A lista autenticou com sucesso diretamente no servidor Xtream.",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Informações não sensíveis úteis para diagnóstico
                        DiagnosticRow(label = "Status da conta", value = result.accountStatus)
                        result.expiration?.let {
                            DiagnosticRow(label = "Validade no servidor", value = it)
                        }
                        result.maxConnections?.let {
                            DiagnosticRow(label = "Conexões máximas", value = it)
                        }
                    }
                    is XtreamAuthResult.InvalidCredentials -> {
                        Text(
                            text = "O servidor rejeitou as credenciais cadastradas.",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Verifique o usuário e a senha configurados para esta lista no painel LC Admin.",
                            color = TvTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    is XtreamAuthResult.Unavailable -> {
                        Text(
                            text = "Não foi possível estabelecer comunicação com o servidor da lista.",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Verifique se o DNS configurado está acessível ou se o servidor está temporariamente offline.",
                            color = TvTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    is XtreamAuthResult.IncompatibleResponse -> {
                        Text(
                            text = "O endereço do servidor respondeu, mas não retornou a estrutura esperada da API Xtream.",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Verifique se o DNS corresponde a um servidor compatível com Xtream Codes.",
                            color = TvTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    is XtreamAuthResult.NotAllowed -> {
                        Text(
                            text = result.displayMessage,
                            color = TvTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TvPrimaryBlue)
                    .tvFocusable(
                        shape = RoundedCornerShape(10.dp),
                        focusBorderColor = TvFocusBorder,
                        focusedScale = 1.05f,
                        onClick = onDismiss
                    )
                    .padding(horizontal = 24.dp)
                    .testTag("btn_close_test_dialog"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Fechar",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    )
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(TvSurfaceVariant)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TvTextSecondary,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

/**
 * Diálogo de Confirmação Obrigatória antes da exclusão de qualquer lista.
 */
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
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33FF3D71)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "Excluir lista?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "A lista \"${list.title}\" será removida deste dispositivo.",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "A fonte/servidor não será excluída do LC Admin.",
                    color = TvTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDeleting) Color(0xFF4A1A22) else Color(0xFFD32F2F))
                    .tvFocusable(
                        enabled = !isDeleting,
                        shape = RoundedCornerShape(10.dp),
                        focusBorderColor = Color(0xFFFF8A80),
                        focusedScale = 1.05f,
                        onClick = if (!isDeleting) onConfirm else null
                    )
                    .padding(horizontal = 20.dp)
                    .testTag("btn_confirm_delete"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Excluindo...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Excluir",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        dismissButton = {
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TvSurfaceVariant)
                    .tvFocusable(
                        enabled = !isDeleting,
                        shape = RoundedCornerShape(10.dp),
                        focusBorderColor = TvFocusBorder,
                        focusedScale = 1.05f,
                        onClick = if (!isDeleting) onDismiss else null
                    )
                    .padding(horizontal = 18.dp)
                    .testTag("btn_cancel_delete"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cancelar",
                    color = TvTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    )
}

@Composable
private fun EmptyListsView(
    onRefreshClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .padding(top = 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(TvCardBackground)
            .border(1.dp, TvSurfaceVariant, RoundedCornerShape(20.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TvSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistAddCheck,
                    contentDescription = null,
                    tint = TvSecondaryBlue,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "Nenhuma lista configurada",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Adicione uma lista pelo LC Admin.",
                color = TvTextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TvPrimaryBlue)
                    .tvFocusable(
                        shape = RoundedCornerShape(12.dp),
                        focusBorderColor = TvFocusBorder,
                        focusedScale = 1.05f,
                        onClick = onRefreshClick
                    )
                    .padding(horizontal = 20.dp),
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
                        text = "Verificar novamente",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceStatusBlockedView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .padding(top = 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(TvCardBackground)
            .border(1.5.dp, StatusBlockedBorder, RoundedCornerShape(20.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(StatusBlockedBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = StatusBlockedText,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "Dispositivo bloqueado",
                color = StatusBlockedText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "O acesso deste aparelho foi bloqueado no LC Admin. Entre em contato com o administrador para regularizar.",
                color = TvTextSecondary,
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
            .widthIn(max = 600.dp)
            .padding(top = 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(TvCardBackground)
            .border(1.5.dp, StatusExpiredBorder, RoundedCornerShape(20.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(StatusExpiredBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassEmpty,
                    contentDescription = null,
                    tint = StatusExpiredText,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "Acesso vencido",
                color = StatusExpiredText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "O período de acesso deste dispositivo expirou. Renove sua assinatura no LC Admin para liberar as listas.",
                color = TvTextSecondary,
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
            .widthIn(max = 600.dp)
            .padding(top = 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(TvCardBackground)
            .border(1.5.dp, StatusWaitingBorder, RoundedCornerShape(20.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(StatusWaitingBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = StatusWaitingText,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "Aguardando ativação",
                color = StatusWaitingText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Cadastre este dispositivo no LC Admin para continuar.",
                color = TvTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TvPrimaryBlue)
                    .tvFocusable(
                        shape = RoundedCornerShape(12.dp),
                        focusBorderColor = TvFocusBorder,
                        focusedScale = 1.05f,
                        onClick = onGoToActivation
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ver Identificação do Aparelho",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
