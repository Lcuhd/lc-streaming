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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivationScreen(
    viewModel: ActivationViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val primaryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.toastFeedback) {
        uiState.toastFeedback?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearToastFeedback()
        }
    }

    // Auto-foca no botão principal para navegação imediata com D-pad do controle remoto
    LaunchedEffect(Unit) {
        try {
            primaryFocusRequester.requestFocus()
        } catch (_: Exception) {
            // Caso a visualização ainda esteja se compondo
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
        // Luz ambiente decorativa azul nos cantos para efeito cinema TV
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(380.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x1A0084FF), Color.Transparent)
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Cabeçalho da TV: Logotipo e Identidade LC Player
            item {
                HeaderTvBar()
            }

            // Cartão de Ativação Principal
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 840.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(TvCardBackground)
                        .border(
                            width = 1.dp,
                            color = TvSurfaceVariant,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Título da Seção
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.activation_title),
                                color = TvTextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Identificação exclusiva desta instalação para o LC Admin",
                                color = TvTextSecondary,
                                fontSize = 14.sp
                            )
                        }

                        // Grid dos dois blocos principais: DEVICE ID e KEY
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Bloco DEVICE ID
                            CredentialBox(
                                label = stringResource(R.string.device_id_label),
                                value = uiState.deviceId,
                                description = "Identificador desta instalação",
                                isKey = false,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("device_id_box")
                            )

                            // Bloco KEY
                            CredentialBox(
                                label = stringResource(R.string.device_key_label),
                                value = uiState.key,
                                description = "Chave numérica de 6 dígitos",
                                isKey = true,
                                modifier = Modifier
                                    .weight(0.9f)
                                    .testTag("device_key_box")
                            )
                        }

                        // Seção de Status Atual
                        StatusIndicatorCard(
                            status = uiState.status,
                            modifier = Modifier.testTag("status_indicator_card")
                        )

                        // Mensagem de instrução obrigatória solicitada
                        InstructionNoticeCard()

                        // Ações D-Pad e Controle Remoto
                        ActionButtonsRow(
                            primaryFocusRequester = primaryFocusRequester,
                            isChecking = uiState.isChecking,
                            onVerifyClick = { viewModel.onVerifyActivationClicked() },
                            onCopyClick = {
                                val clipboard =
                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText(
                                    "LC Player Credentials",
                                    "Device ID: ${uiState.deviceId}\nKEY: ${uiState.key}"
                                )
                                clipboard.setPrimaryClip(clip)
                                viewModel.onCopiedToClipboard()
                            },
                            onInfoClick = { viewModel.openSystemInfo() }
                        )

                        // Data da última checagem local
                        uiState.lastCheckedTimestamp?.let { timestamp ->
                            val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                            Text(
                                text = "Última verificação: ${formatter.format(Date(timestamp))}",
                                color = TvTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Rodapé Informativo para o usuário do LC Admin
            item {
                FooterAdminNotice()
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }

    // Modal de Informações do Sistema (Android TV / Box)
    if (uiState.showSystemInfoModal) {
        SystemInfoDialog(
            deviceId = uiState.deviceId,
            key = uiState.key,
            onDismiss = { viewModel.closeSystemInfo() }
        )
    }
}

@Composable
private fun HeaderTvBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 840.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Ícone estilizado do LC Player
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(TvPrimaryBlue, Color(0xFF0056B3))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "LC Player Logo",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column {
                Text(
                    text = "LC Player",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Android TV • Google TV • TV Box",
                    color = TvSecondaryBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Indicador de modo TV
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
                contentDescription = "Dispositivo TV",
                tint = TvSecondaryBlue,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "LC Admin Ready",
                color = TvTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
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
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TvSurface)
            .border(
                width = 1.5.dp,
                color = if (isKey) TvPrimaryBlue.copy(alpha = 0.5f) else TvSurfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label.uppercase(Locale.getDefault()),
                color = if (isKey) TvSecondaryBlue else TvTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            // Texto grande e de altíssima legibilidade para visualização à distância na TV (10-foot UI)
            Text(
                text = value,
                color = if (isKey) Color.White else TvTextPrimary,
                fontSize = if (isKey) 26.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = description,
                color = TvTextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
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
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(width = 1.5.dp, color = borderColor, shape = RoundedCornerShape(14.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Indicador circular de status
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(textColor)
                )

                Text(
                    text = "Status: ",
                    color = TvTextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = status.title,
                    color = textColor,
                    fontSize = 17.sp,
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
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun InstructionNoticeCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F1522))
            .border(1.dp, Color(0xFF1C273B), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Frase mandatória solicitada
            Text(
                text = stringResource(R.string.activation_notice),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.activation_instructions),
                color = TvTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ActionButtonsRow(
    primaryFocusRequester: FocusRequester,
    isChecking: Boolean,
    onVerifyClick: () -> Unit,
    onCopyClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botão Principal: Verificar Ativação (com foco inicial na TV)
        TvActionButton(
            text = if (isChecking) stringResource(R.string.checking_status) else stringResource(R.string.btn_refresh_status),
            icon = Icons.Default.Refresh,
            isPrimary = true,
            isLoading = isChecking,
            onClick = onVerifyClick,
            modifier = Modifier
                .weight(1.3f)
                .focusRequester(primaryFocusRequester)
                .testTag("btn_verify_activation")
        )

        // Botão Secundário: Copiar Códigos
        TvActionButton(
            text = stringResource(R.string.btn_copy_credentials),
            icon = Icons.Default.ContentCopy,
            isPrimary = false,
            onClick = onCopyClick,
            modifier = Modifier
                .weight(1f)
                .testTag("btn_copy_credentials")
        )

        // Botão Informações do Sistema
        TvActionButton(
            text = stringResource(R.string.btn_device_info),
            icon = Icons.Default.Info,
            isPrimary = false,
            onClick = onInfoClick,
            modifier = Modifier
                .weight(1.1f)
                .testTag("btn_device_info")
        )
    }
}

@Composable
private fun TvActionButton(
    text: String,
    icon: ImageVector,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    val baseBgColor = if (isPrimary) TvPrimaryBlue else TvSurfaceVariant
    val contentColor = if (isPrimary) Color.White else TvTextPrimary

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(baseBgColor)
            .tvFocusable(
                shape = RoundedCornerShape(12.dp),
                focusBorderColor = TvFocusBorder,
                focusedScale = 1.05f,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = text,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    .clip(RoundedCornerShape(8.dp))
                    .tvFocusable(
                        shape = RoundedCornerShape(8.dp),
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
