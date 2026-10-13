package com.example.ui.activation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.ActivationStatus
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
import com.example.ui.theme.TvSidebarBackground
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextMuted
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Função utilitária para identificar se o dispositivo é Android TV / TV Box.
 */
fun isTvDevice(context: Context): Boolean {
    val pm = context.packageManager
    return pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK) ||
           pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TELEVISION)
}

/**
 * Tela de Ativação / Identificação do Dispositivo com linguagem visual baseada no Mockup oficial LC Player.
 *
 * Estrutura:
 * - Área Principal (65-70% em Landscape): Logotipo da marca, "Ative seu dispositivo", Device ID/MAC, KEY, Link do Painel e Ações.
 * - Sidebar Profunda (30-35% em Landscape / Bloco inferior em Portrait): "COMO ATIVAR" em passos e espaço reservado para QR Code.
 */
@Composable
fun ActivationScreen(
    viewModel: ActivationViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isTv = remember(context) { isTvDevice(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val primaryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.toastFeedback) {
        uiState.toastFeedback?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearToastFeedback()
        }
    }

    LaunchedEffect(Unit) {
        try {
            primaryFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF040810),
                        TvBackground,
                        Color(0xFF03060C)
                    )
                )
            )
    ) {
        // Brilho azul decorativo ambiente nos cantos
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(420.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x1F0084FF), Color.Transparent)
                    )
                )
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideLayout = this.maxWidth >= 680.dp

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = AppSpacing.lg, vertical = AppSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)
            ) {
                // Cabeçalho com o Logotipo de marca estilizado
                item {
                    HeaderBrandBar(isWideLayout = isWideLayout)
                }

                // Layout em 2 Áreas
                item {
                    if (isWideLayout) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 1060.dp),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Área Principal (Dominante ~68%)
                            Column(
                                modifier = Modifier.weight(1.35f),
                                verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
                            ) {
                                ActivationMainCard(
                                    uiState = uiState,
                                    isTv = isTv,
                                    primaryFocusRequester = primaryFocusRequester,
                                    viewModel = viewModel,
                                    context = context
                                )
                            }

                            // Sidebar Azul Escuro (~32%)
                            Column(
                                modifier = Modifier.weight(0.65f),
                                verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
                            ) {
                                SidebarGuideAndQrCard()
                            }
                        }
                    } else {
                        // Layout Retrato (Empilhado verticalmente)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 540.dp),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ActivationMainCard(
                                uiState = uiState,
                                isTv = isTv,
                                primaryFocusRequester = primaryFocusRequester,
                                viewModel = viewModel,
                                context = context
                            )

                            SidebarGuideAndQrCard()
                        }
                    }
                }

                item {
                    FooterAdminNotice()
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = AppSpacing.xl)
        )
    }

    if (uiState.showSystemInfoModal) {
        SystemInfoDialog(
            deviceId = uiState.deviceId,
            key = uiState.key,
            onDismiss = { viewModel.closeSystemInfo() }
        )
    }
}

/**
 * Logotipo estilizado da marca LC Player exatamente como no Mockup.
 */
@Composable
private fun HeaderBrandBar(isWideLayout: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 1060.dp)
            .padding(horizontal = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Logotipo LC (Azul Elétrico) + Player (Branco)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "LC",
                color = TvPrimaryBlue,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Player",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            modifier = Modifier
                .clip(AppCorners.pill)
                .background(TvSurfaceVariant)
                .padding(horizontal = AppSpacing.md, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = "Dispositivo",
                tint = TvSecondaryBlue,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isWideLayout) "Android TV • LC Admin" else "LC Admin Ready",
                color = TvTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Área Principal Dominante da Tela de Ativação.
 */
@Composable
private fun ActivationMainCard(
    uiState: ActivationUiState,
    isTv: Boolean,
    primaryFocusRequester: FocusRequester,
    viewModel: ActivationViewModel,
    context: Context
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppCorners.xl)
            .background(TvCardBackground)
            .border(1.dp, TvSurfaceVariant, AppCorners.xl)
            .padding(horizontal = AppSpacing.xl, vertical = AppSpacing.xxl)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Título e Subtítulo
            Column {
                Text(
                    text = "Ative seu dispositivo",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Digite os dados fornecidos pelo seu provedor para acessar o conteúdo.",
                    color = TvTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            // Cards de Credenciais (Device ID e KEY)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                CredentialBox(
                    label = "Device ID (MAC)",
                    value = uiState.deviceId,
                    description = "Identificador exclusivo deste aparelho",
                    isKey = false,
                    showCopyButton = !isTv,
                    onCopyClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Device ID", uiState.deviceId)
                        clipboard.setPrimaryClip(clip)
                        viewModel.onCopiedToClipboard()
                    },
                    modifier = Modifier.testTag("device_id_box")
                )

                CredentialBox(
                    label = "KEY (Token)",
                    value = uiState.key,
                    description = "Chave de acesso de 6 dígitos",
                    isKey = true,
                    showCopyButton = !isTv,
                    onCopyClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("KEY", uiState.key)
                        clipboard.setPrimaryClip(clip)
                        viewModel.onCopiedToClipboard()
                    },
                    modifier = Modifier.testTag("device_key_box")
                )
            }

            // Endereço do Painel Web destacado
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppCorners.md)
                    .background(Color(0x1A0084FF))
                    .border(1.dp, TvFocusBorder.copy(alpha = 0.5f), AppCorners.md)
                    .padding(horizontal = AppSpacing.lg, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Painel Web",
                    tint = TvSecondaryBlue,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Acesse para ativar:",
                    color = TvTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "lcstreaming.com.br",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Status do Dispositivo
            StatusIndicatorCard(
                status = uiState.status,
                modifier = Modifier.testTag("status_indicator_card")
            )

            // Feedback da última verificação
            AnimatedVisibility(
                visible = uiState.lastFeedbackMessage != null && !uiState.isChecking,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.lastFeedbackMessage?.let { msg ->
                    val isSuccess = uiState.status == ActivationStatus.ACTIVE
                    val isWaiting = uiState.status == ActivationStatus.WAITING_ACTIVATION
                    val isBlockedOrExpired = uiState.status == ActivationStatus.BLOCKED || uiState.status == ActivationStatus.EXPIRED

                    val badgeBg = when {
                        isSuccess -> StatusActiveBg
                        isWaiting -> StatusWaitingBg
                        isBlockedOrExpired -> StatusBlockedBg
                        else -> StatusErrorBg
                    }
                    val badgeBorder = when {
                        isSuccess -> StatusActiveBorder
                        isWaiting -> StatusWaitingBorder
                        isBlockedOrExpired -> StatusBlockedBorder
                        else -> StatusErrorBorder
                    }
                    val badgeText = when {
                        isSuccess -> StatusActiveText
                        isWaiting -> StatusWaitingText
                        isBlockedOrExpired -> StatusBlockedText
                        else -> StatusErrorText
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppCorners.md)
                            .background(badgeBg)
                            .border(1.dp, badgeBorder, AppCorners.md)
                            .padding(vertical = AppSpacing.md, horizontal = AppSpacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = badgeText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.sm))
                        Text(
                            text = msg,
                            color = badgeText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Progress Indicator durante verificação
            AnimatedVisibility(
                visible = uiState.isChecking,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppCorners.md)
                        .background(Color(0x220084FF))
                        .border(1.dp, TvFocusBorder, AppCorners.md)
                        .padding(vertical = AppSpacing.md, horizontal = AppSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = TvSecondaryBlue,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.md))
                    Text(
                        text = "Consultando status no LC Admin...",
                        color = TvSecondaryBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Botão Principal "ATIVAR DISPOSITIVO" / "VERIFICAR ATIVAÇÃO" estilizado como no Mockup
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(AppCorners.md)
                    .background(if (!uiState.isChecking) TvPrimaryBlue else Color(0xFF1E2D42))
                    .tvFocusable(
                        enabled = !uiState.isChecking,
                        shape = AppCorners.md,
                        focusBorderColor = Color.White,
                        focusedScale = 1.04f,
                        onClick = { viewModel.onVerifyActivationClicked() }
                    )
                    .focusRequester(primaryFocusRequester)
                    .testTag("btn_verify_activation"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    if (uiState.isChecking) {
                        CircularProgressIndicator(
                            color = TvSecondaryBlue,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = if (uiState.isChecking) "VERIFICANDO..." else "VERIFICAR ATIVAÇÃO",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Botão "Acessar Minhas Listas" quando ativo
            AnimatedVisibility(
                visible = uiState.status == ActivationStatus.ACTIVE,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(AppCorners.md)
                        .background(
                            Brush.horizontalGradient(
                                listOf(TvPrimaryBlue, TvSecondaryBlue)
                            )
                        )
                        .tvFocusable(
                            shape = AppCorners.md,
                            focusBorderColor = Color.White,
                            focusedScale = 1.04f,
                            onClick = { viewModel.navigateToSection(AppScreenSection.MY_LISTS) }
                        )
                        .testTag("btn_go_to_my_lists"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Acessar Minhas Listas",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Ações secundárias
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isTv) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(AppCorners.md)
                            .background(TvSurfaceVariant)
                            .tvFocusable(
                                shape = AppCorners.md,
                                focusBorderColor = TvFocusBorder,
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText(
                                        "LC Player Credentials",
                                        "Device ID: ${uiState.deviceId}\nKEY: ${uiState.key}"
                                    )
                                    clipboard.setPrimaryClip(clip)
                                    viewModel.onCopiedToClipboard()
                                }
                            )
                            .testTag("btn_copy_credentials"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Copiar Código",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(AppCorners.md)
                        .background(TvSurfaceVariant)
                        .tvFocusable(
                            shape = AppCorners.md,
                            focusBorderColor = TvFocusBorder,
                            onClick = { viewModel.openSystemInfo() }
                        )
                        .testTag("btn_device_info"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Info do Sistema",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            uiState.lastCheckedTimestamp?.let { timestamp ->
                val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                Text(
                    text = "Última verificação: ${formatter.format(Date(timestamp))}",
                    color = TvTextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Sidebar com tom azul-marinho profundo e organização em passos.
 */
@Composable
private fun SidebarGuideAndQrCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppCorners.xl)
            .background(TvSidebarBackground)
            .border(1.dp, TvSurfaceVariant, AppCorners.xl)
            .padding(AppSpacing.xl)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
        ) {
            // Título
            Text(
                text = "COMO ATIVAR",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            // Passos organizados
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                StepRow(
                    stepNumber = "1",
                    title = "Acesse lcstreaming.com.br",
                    description = "Abra o painel web no celular ou computador."
                )
                StepRow(
                    stepNumber = "2",
                    title = "Informe o Device ID e a KEY",
                    description = "Digite as credenciais exibidas ao lado."
                )
                StepRow(
                    stepNumber = "3",
                    title = "Aguarde a liberação",
                    description = "Clique em 'Verificar Ativação' para iniciar."
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Título Pareamento Rápido
            Text(
                text = "PAREAMENTO RÁPIDO",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            // Espaço Reservado para o Futuro QR Code
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppCorners.lg)
                    .background(Color(0xFF060B14))
                    .border(1.dp, Color(0xFF1A2B47), AppCorners.lg)
                    .padding(AppSpacing.lg),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CropFree,
                        contentDescription = "Espaço QR Code",
                        tint = TvSecondaryBlue,
                        modifier = Modifier.size(44.dp)
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    Text(
                        text = "Pareamento por QR Code",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Box(
                        modifier = Modifier
                            .clip(AppCorners.xs)
                            .background(Color(0x220084FF))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Em breve • Token seguro",
                            color = TvSecondaryBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.xs))

                    Text(
                        text = "Escaneie com o celular para cadastrar este dispositivo.",
                        color = TvTextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StepRow(
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(TvPrimaryBlue),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = TvTextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun CredentialBox(
    label: String,
    value: String,
    description: String,
    isKey: Boolean,
    showCopyButton: Boolean,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppCorners.lg)
            .background(TvSurface)
            .border(
                width = 1.5.dp,
                color = if (isKey) TvPrimaryBlue.copy(alpha = 0.5f) else TvSurfaceVariant,
                shape = AppCorners.lg
            )
            .padding(AppSpacing.lg)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label.uppercase(Locale.getDefault()),
                    color = if (isKey) TvSecondaryBlue else TvTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                if (showCopyButton) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x220084FF))
                            .clickable { onCopyClick() }
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar $label",
                            tint = TvSecondaryBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Text(
                text = value,
                color = if (isKey) Color.White else TvTextPrimary,
                fontSize = if (isKey) 26.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )

            Text(
                text = description,
                color = TvTextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun StatusIndicatorCard(
    status: ActivationStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, borderColor, textColor) = when (status) {
        ActivationStatus.WAITING_ACTIVATION -> Triple(StatusWaitingBg, StatusWaitingBorder, StatusWaitingText)
        ActivationStatus.ACTIVE -> Triple(StatusActiveBg, StatusActiveBorder, StatusActiveText)
        ActivationStatus.BLOCKED -> Triple(StatusBlockedBg, StatusBlockedBorder, StatusBlockedText)
        ActivationStatus.EXPIRED -> Triple(StatusExpiredBg, StatusExpiredBorder, StatusExpiredText)
        ActivationStatus.CONNECTION_ERROR -> Triple(StatusErrorBg, StatusErrorBorder, StatusErrorText)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppCorners.md)
            .background(bgColor)
            .border(width = 1.5.dp, color = borderColor, shape = AppCorners.md)
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(textColor)
                )

                Text(
                    text = "Status: ",
                    color = TvTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = status.title,
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Icon(
                imageVector = when (status) {
                    ActivationStatus.WAITING_ACTIVATION -> Icons.Default.HourglassEmpty
                    ActivationStatus.ACTIVE -> Icons.Default.CheckCircle
                    else -> Icons.Default.Info
                },
                contentDescription = status.title,
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FooterAdminNotice() {
    Text(
        text = "LC Player v1.0 • Pronto para integração com LC Admin",
        color = TvTextMuted,
        fontSize = 12.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SystemInfoDialog(
    deviceId: String,
    key: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TvSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TvSecondaryBlue
                )
                Text(
                    text = stringResource(R.string.system_info_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                SystemInfoRow(label = "Aplicativo", value = "LC Player 1.0 (TV Edition)")
                SystemInfoRow(label = "Device ID", value = deviceId)
                SystemInfoRow(label = "KEY", value = key)
                SystemInfoRow(label = "Aparelho", value = "${Build.MANUFACTURER} ${Build.MODEL}")
                SystemInfoRow(label = "Android", value = "Versão ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                SystemInfoRow(label = "Armazenamento", value = "SQLite / Room Local Database")
                SystemInfoRow(label = "Controle Remoto", value = "Navegação por D-Pad habilitada")
            }
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

@Composable
private fun SystemInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TvTextSecondary,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
