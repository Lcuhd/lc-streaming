package com.example.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.AppSettingsManager
import com.example.domain.model.ListStatus
import com.example.ui.activation.ActivationViewModel
import com.example.ui.activation.AppScreenSection
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
import com.example.ui.theme.TvTextSecondary

/**
 * Enumeração das seções do menu de Configurações.
 */
enum class SettingsSection(val titleResId: Int, val icon: ImageVector) {
    GENERAL(R.string.settings_section_general, Icons.Default.Language),
    PLAYBACK(R.string.settings_section_playback, Icons.Default.Settings),
    ACCOUNT(R.string.settings_section_account, Icons.Default.FormatListBulleted),
    PARENTAL(R.string.settings_section_parental, Icons.Default.Lock),
    DEVICE(R.string.settings_section_device, Icons.Default.PhoneAndroid),
    ABOUT(R.string.settings_section_about, Icons.Default.Info)
}

/**
 * Tela Dedicada de Configurações do LC Player.
 * Suporta layout integrado de 2 colunas para TV / Widescreen (Sidebar de Seções à esquerda | Painel de Opções à direita)
 * e Grid compacto responsivo para Mobile.
 * Preserva 100% das funções e callbacks existentes.
 */
@Composable
fun SettingsScreen(
    viewModel: ActivationViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backButtonFocusRequester = remember { FocusRequester() }

    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showDeviceModal by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showToggleParentalDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showParentalManagementModal by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf(SettingsSection.GENERAL) }
    var selectedMobileSection by remember { mutableStateOf<SettingsSection?>(null) }

    // Estados de Preferência Persistidos
    var selectedTimeFormat by remember { mutableStateOf(AppSettingsManager.getTimeFormat(context)) }
    var selectedLanguage by remember { mutableStateOf(AppSettingsManager.getLanguage(context)) }
    var selectedLiveFormat by remember { mutableStateOf(AppSettingsManager.getLiveStreamFormat(context)) }
    var selectedAutoNextEpisode by remember { mutableStateOf(AppSettingsManager.isAutoNextEpisodeEnabled(context)) }

    val appVersionName = remember(context) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    val activeListsCount = remember(uiState.lists) {
        uiState.lists.count { it.status == ListStatus.ACTIVE }
    }

    BackHandler {
        if (selectedMobileSection != null) {
            selectedMobileSection = null
        } else if (showParentalManagementModal) {
            showParentalManagementModal = false
        } else if (showChangePinDialog) {
            showChangePinDialog = false
        } else if (showToggleParentalDialog) {
            showToggleParentalDialog = false
        } else if (showLogoutDialog) {
            showLogoutDialog = false
        } else if (showDeviceModal) {
            showDeviceModal = false
        } else if (showClearCacheDialog) {
            showClearCacheDialog = false
        } else {
            onBackClick()
        }
    }

    LaunchedEffect(Unit) {
        try {
            backButtonFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050810))
    ) {
        val isWideLayout = this.maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = AppSpacing.xl, top = AppSpacing.md, end = AppSpacing.xl, bottom = AppSpacing.md)
        ) {
            // Cabeçalho da Tela de Configurações
            SettingsTopBar(
                listTitle = uiState.contentCategoriesState?.listTitle ?: "",
                onBackClick = onBackClick,
                backFocusRequester = backButtonFocusRequester
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            if (isWideLayout) {
                // TV / WIDESCREEN: LAYOUT INTEGRADO DE 2 PAINÉIS (Menu Lateral Esq | Painel Direita)
                WideTwoPaneSettingsView(
                    selectedSection = selectedSection,
                    onSelectSection = { selectedSection = it },
                    selectedTimeFormat = selectedTimeFormat,
                    onToggleTimeFormat = {
                        val nextFormat = if (selectedTimeFormat == "24H") "12H" else "24H"
                        selectedTimeFormat = nextFormat
                        AppSettingsManager.setTimeFormat(context, nextFormat)
                    },
                    selectedLanguage = selectedLanguage,
                    onToggleLanguage = {
                        val nextLang = when (selectedLanguage) {
                            "PT_BR" -> "EN"
                            "EN" -> "ES"
                            else -> "PT_BR"
                        }
                        selectedLanguage = nextLang
                        AppSettingsManager.setLanguage(context, nextLang)
                    },
                    selectedLiveFormat = selectedLiveFormat,
                    onToggleLiveFormat = {
                        val nextFormat = if (selectedLiveFormat == "TS") "HLS" else "TS"
                        selectedLiveFormat = nextFormat
                        AppSettingsManager.setLiveStreamFormat(context, nextFormat)
                    },
                    selectedAutoNextEpisode = selectedAutoNextEpisode,
                    onToggleAutoNextEpisode = {
                        val nextState = !selectedAutoNextEpisode
                        selectedAutoNextEpisode = nextState
                        AppSettingsManager.setAutoNextEpisodeEnabled(context, nextState)
                    },
                    activeListsCount = activeListsCount,
                    onSwitchList = {
                        val activeLists = uiState.lists.filter { it.status == ListStatus.ACTIVE }
                        when {
                            activeLists.size > 1 -> viewModel.navigateToSection(AppScreenSection.MY_LISTS)
                            activeLists.size == 1 -> viewModel.showToastFeedback(context.getString(R.string.msg_no_other_list))
                            else -> viewModel.showToastFeedback(context.getString(R.string.msg_no_active_list))
                        }
                    },
                    onRefreshLists = { viewModel.refreshDeviceListsFromSettings() },
                    onOpenDeviceModal = { showDeviceModal = true },
                    onOpenClearCacheModal = { showClearCacheDialog = true },
                    onLogoutList = { showLogoutDialog = true },
                    onToggleParental = { showToggleParentalDialog = true },
                    onChangePin = { showChangePinDialog = true },
                    onOpenParentalManagement = { showParentalManagementModal = true },
                    appVersionName = appVersionName,
                    uiState = uiState,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            } else {
                // MOBILE SETTINGS: LAYOUT DE UMA COLUNA COM SUB-TELAS
                MobileSettingsView(
                    selectedSection = selectedMobileSection,
                    onSelectSection = { selectedMobileSection = it },
                    selectedTimeFormat = selectedTimeFormat,
                    onToggleTimeFormat = {
                        val nextFormat = if (selectedTimeFormat == "24H") "12H" else "24H"
                        selectedTimeFormat = nextFormat
                        AppSettingsManager.setTimeFormat(context, nextFormat)
                    },
                    selectedLanguage = selectedLanguage,
                    onToggleLanguage = {
                        val nextLang = when (selectedLanguage) {
                            "PT_BR" -> "EN"
                            "EN" -> "ES"
                            else -> "PT_BR"
                        }
                        selectedLanguage = nextLang
                        AppSettingsManager.setLanguage(context, nextLang)
                    },
                    selectedLiveFormat = selectedLiveFormat,
                    onToggleLiveFormat = {
                        val nextFormat = if (selectedLiveFormat == "TS") "HLS" else "TS"
                        selectedLiveFormat = nextFormat
                        AppSettingsManager.setLiveStreamFormat(context, nextFormat)
                    },
                    selectedAutoNextEpisode = selectedAutoNextEpisode,
                    onToggleAutoNextEpisode = {
                        val nextState = !selectedAutoNextEpisode
                        selectedAutoNextEpisode = nextState
                        AppSettingsManager.setAutoNextEpisodeEnabled(context, nextState)
                    },
                    activeListsCount = activeListsCount,
                    onSwitchList = {
                        val activeLists = uiState.lists.filter { it.status == ListStatus.ACTIVE }
                        when {
                            activeLists.size > 1 -> viewModel.navigateToSection(AppScreenSection.MY_LISTS)
                            activeLists.size == 1 -> viewModel.showToastFeedback(context.getString(R.string.msg_no_other_list))
                            else -> viewModel.showToastFeedback(context.getString(R.string.msg_no_active_list))
                        }
                    },
                    onRefreshLists = { viewModel.refreshDeviceListsFromSettings() },
                    onOpenDeviceModal = { showDeviceModal = true },
                    onOpenClearCacheModal = { showClearCacheDialog = true },
                    onLogoutList = { showLogoutDialog = true },
                    onToggleParental = { showToggleParentalDialog = true },
                    onChangePin = { showChangePinDialog = true },
                    onOpenParentalManagement = { showParentalManagementModal = true },
                    appVersionName = appVersionName,
                    uiState = uiState,
                    viewModel = viewModel,
                    onBackClick = onBackClick,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }

        // Modal para Ativar / Desativar Controle dos Pais
        if (showToggleParentalDialog) {
            var pinInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showToggleParentalDialog = false },
                containerColor = TvSurface,
                title = {
                    Text(
                        text = if (AppSettingsManager.isParentalControlEnabled(context)) "Desativar Controle dos Pais?" else "Ativar Controle dos Pais?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Digite o PIN atual para continuar:",
                            color = TvTextSecondary,
                            fontSize = 13.sp
                        )
                        androidx.compose.foundation.text.BasicTextField(
                            value = pinInput,
                            onValueChange = { if (it.length <= 4) pinInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(AppCorners.md)
                                .background(TvCardBackground)
                                .border(1.dp, TvSurfaceVariant, AppCorners.md)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val success = viewModel.toggleParentalControlState(pinInput)
                            if (success) {
                                showToggleParentalDialog = false
                            }
                        }
                    ) {
                        Text(text = "Confirmar", color = TvSecondaryBlue, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showToggleParentalDialog = false }) {
                        Text(text = stringResource(R.string.btn_back), color = TvTextMuted, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Modal para Alterar PIN do Controle dos Pais
        if (showChangePinDialog) {
            var currentPinInput by remember { mutableStateOf("") }
            var newPinInput by remember { mutableStateOf("") }
            var confirmPinInput by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showChangePinDialog = false },
                containerColor = TvSurface,
                title = {
                    Text(
                        text = "Alterar PIN do Controle dos Pais",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "PIN Atual:", color = TvTextSecondary, fontSize = 12.sp)
                        androidx.compose.foundation.text.BasicTextField(
                            value = currentPinInput,
                            onValueChange = { if (it.length <= 4) currentPinInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(AppCorners.md)
                                .background(TvCardBackground)
                                .border(1.dp, TvSurfaceVariant, AppCorners.md)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
                        )

                        Text(text = "Novo PIN (4 dígitos):", color = TvTextSecondary, fontSize = 12.sp)
                        androidx.compose.foundation.text.BasicTextField(
                            value = newPinInput,
                            onValueChange = { if (it.length <= 4) newPinInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(AppCorners.md)
                                .background(TvCardBackground)
                                .border(1.dp, TvSurfaceVariant, AppCorners.md)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
                        )

                        Text(text = "Confirmar Novo PIN:", color = TvTextSecondary, fontSize = 12.sp)
                        androidx.compose.foundation.text.BasicTextField(
                            value = confirmPinInput,
                            onValueChange = { if (it.length <= 4) confirmPinInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(AppCorners.md)
                                .background(TvCardBackground)
                                .border(1.dp, TvSurfaceVariant, AppCorners.md)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val success = viewModel.changeParentalPin(currentPinInput, newPinInput, confirmPinInput)
                            if (success) {
                                showChangePinDialog = false
                            }
                        }
                    ) {
                        Text(text = "Salvar PIN", color = TvSecondaryBlue, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showChangePinDialog = false }) {
                        Text(text = stringResource(R.string.btn_back), color = TvTextMuted, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Modal para Gerenciar Conteúdo Protegido
        if (showParentalManagementModal) {
            AlertDialog(
                onDismissRequest = { showParentalManagementModal = false },
                containerColor = TvSurface,
                title = {
                    Text(
                        text = "Conteúdo Protegido",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Categorias e canais contendo termos adultos (ex: 18+, XXX, Adultos) são bloqueados automaticamente pelo sistema do Controle dos Pais.",
                            color = TvTextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Para bloquear ou liberar categorias/canais específicos, você também pode utilizar a proteção direta durante a navegação das seções de TV, Filmes e Séries.",
                            color = TvTextMuted,
                            fontSize = 12.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showParentalManagementModal = false }) {
                        Text(text = stringResource(R.string.btn_close), color = TvSecondaryBlue, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Modal de Informações do Dispositivo (Device ID, KEY e Pareamento)
        if (showDeviceModal) {
            AlertDialog(
                onDismissRequest = { showDeviceModal = false },
                containerColor = TvSurface,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = TvSecondaryBlue
                        )
                        Text(
                            text = stringResource(R.string.settings_device_dialog_title),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Device ID / MAC
                        SettingsCopyableCard(
                            label = "Device ID / MAC",
                            value = uiState.deviceId,
                            onCopy = {
                                copyToClipboard(context, "Device ID", uiState.deviceId)
                                viewModel.showToastFeedback(context.getString(R.string.msg_device_id_copied))
                            }
                        )

                        // KEY
                        SettingsCopyableCard(
                            label = "KEY do Dispositivo",
                            value = uiState.key,
                            onCopy = {
                                copyToClipboard(context, "KEY", uiState.key)
                                viewModel.showToastFeedback(context.getString(R.string.msg_key_copied))
                            }
                        )

                        // Pareamento por QR Code (Futuro)
                        SettingsDisabledCard(
                            title = stringResource(R.string.settings_qr_pairing),
                            subtitle = stringResource(R.string.settings_qr_pairing_subtitle),
                            icon = Icons.Default.QrCode,
                            badgeText = stringResource(R.string.settings_coming_soon)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDeviceModal = false }) {
                        Text(
                            text = stringResource(R.string.btn_close),
                            color = TvSecondaryBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }

        // Modal de Confirmação para Sair da Lista Atual
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                containerColor = TvSurface,
                title = {
                    Text(
                        text = stringResource(R.string.settings_logout_dialog_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.settings_logout_dialog_msg),
                        color = TvTextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            viewModel.logoutCurrentList()
                            onBackClick()
                        }
                    ) {
                        Text(text = stringResource(R.string.settings_logout_btn), color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text(text = stringResource(R.string.btn_back), color = TvSecondaryBlue, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Modal de Confirmação para Limpar Cache
        if (showClearCacheDialog) {
            AlertDialog(
                onDismissRequest = { showClearCacheDialog = false },
                containerColor = TvSurface,
                title = {
                    Text(
                        text = stringResource(R.string.settings_clear_cache_dialog_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.settings_clear_cache_dialog_msg),
                        color = TvTextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearCacheDialog = false
                            viewModel.clearCategoriesCache()
                        }
                    ) {
                        Text(text = stringResource(R.string.settings_clear_cache_btn), color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCacheDialog = false }) {
                        Text(text = stringResource(R.string.btn_back), color = TvSecondaryBlue, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

/**
 * Layout de 2 Painéis para TV / Widescreen (Sidebar de Seções à esquerda | Linhas de Opções à direita)
 */
@Composable
private fun WideTwoPaneSettingsView(
    selectedSection: SettingsSection,
    onSelectSection: (SettingsSection) -> Unit,
    selectedTimeFormat: String,
    onToggleTimeFormat: () -> Unit,
    selectedLanguage: String,
    onToggleLanguage: () -> Unit,
    selectedLiveFormat: String,
    onToggleLiveFormat: () -> Unit,
    selectedAutoNextEpisode: Boolean,
    onToggleAutoNextEpisode: () -> Unit,
    activeListsCount: Int,
    onSwitchList: () -> Unit,
    onRefreshLists: () -> Unit,
    onOpenDeviceModal: () -> Unit,
    onOpenClearCacheModal: () -> Unit,
    onLogoutList: () -> Unit,
    onToggleParental: () -> Unit,
    onChangePin: () -> Unit,
    onOpenParentalManagement: () -> Unit,
    appVersionName: String,
    uiState: com.example.ui.activation.ActivationUiState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        // COLUNA ESQUERDA — MENU LATERAL DE SEÇÕES (220.dp)
        Box(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .clip(AppCorners.lg)
                .background(TvCardBackground)
                .border(1.dp, TvSurfaceVariant, AppCorners.lg)
                .padding(AppSpacing.md)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.settings_section_sidebar_header),
                    color = TvTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                SettingsSection.values().forEach { section ->
                    val isSelected = section == selectedSection
                    val sectionTitle = stringResource(section.titleResId)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
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
                                onClick = { onSelectSection(section) }
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = section.icon,
                                contentDescription = sectionTitle,
                                tint = if (isSelected) Color.White else TvSecondaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = sectionTitle,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // COLUNA DIREITA — ÁREA DE CONTEÚDO DA SEÇÃO SELECIONADA
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(AppCorners.lg)
                .background(TvCardBackground)
                .border(1.dp, TvSurfaceVariant, AppCorners.lg)
                .padding(AppSpacing.lg)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(selectedSection.titleResId).uppercase(),
                    color = TvSecondaryBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                when (selectedSection) {
                    SettingsSection.GENERAL -> {
                        val langLabel = when (selectedLanguage) {
                            "EN" -> "English"
                            "ES" -> "Español"
                            else -> "Português (BR)"
                        }
                        SettingsOptionRow(
                            title = stringResource(R.string.settings_language_title),
                            value = langLabel,
                            icon = Icons.Default.Language,
                            onClick = onToggleLanguage
                        )

                        val timeLabel = if (selectedTimeFormat == "24H") "24 Horas (14:30)" else "12 Horas (02:30 PM)"
                        SettingsOptionRow(
                            title = stringResource(R.string.settings_time_format_title),
                            value = timeLabel,
                            icon = Icons.Default.AccessTime,
                            onClick = onToggleTimeFormat
                        )
                    }
                    SettingsSection.PLAYBACK -> {
                        val liveFormatLabel = if (selectedLiveFormat == "TS") "MPEG-TS (.ts)" else "HLS / M3U8 (.m3u8)"
                        SettingsOptionRow(
                            title = stringResource(R.string.settings_live_format_title),
                            value = liveFormatLabel,
                            icon = Icons.Default.LiveTv,
                            badgeText = selectedLiveFormat,
                            onClick = onToggleLiveFormat
                        )

                        SettingsOptionRow(
                            title = "Próximo episódio automático",
                            value = if (selectedAutoNextEpisode) "Ativado (Avança na mesma temporada)" else "Desativado",
                            icon = Icons.Default.PlayArrow,
                            badgeText = if (selectedAutoNextEpisode) "Ativado" else "Desativado",
                            badgeColor = if (selectedAutoNextEpisode) Color(0xFF00E676) else TvSecondaryBlue,
                            onClick = onToggleAutoNextEpisode
                        )

                        SettingsOptionRow(
                            title = stringResource(R.string.settings_player_title),
                            value = "ExoPlayer (AndroidX Media3)",
                            icon = Icons.Default.Settings,
                            badgeText = "EXOPLAYER"
                        )
                    }
                    SettingsSection.ACCOUNT -> {
                        val currentList = uiState.lists.firstOrNull { it.id == uiState.contentCategoriesState?.listId }
                            ?: uiState.lists.firstOrNull { it.id == uiState.selectedListId }
                            ?: uiState.lists.firstOrNull()

                        // 1. DADOS DA CONTA ATUAL
                        if (currentList != null && (currentList.sourceName.isNotBlank() || currentList.title.isNotBlank())) {
                            SettingsAccountInfoRow(
                                label = stringResource(R.string.settings_server),
                                value = currentList.sourceName.ifBlank { currentList.title }
                            )
                        }

                        if (currentList != null && currentList.username.isNotBlank()) {
                            SettingsAccountInfoRow(
                                label = stringResource(R.string.settings_login),
                                value = currentList.username
                            )
                        }

                        if (currentList != null && !currentList.expiresAt.isNullOrBlank()) {
                            SettingsAccountInfoRow(
                                label = stringResource(R.string.settings_account_validity),
                                value = currentList.formattedExpiration
                            )
                        }

                        SettingsAccountInfoRow(
                            label = stringResource(R.string.settings_app_validity),
                            value = stringResource(R.string.settings_unlimited)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 2. AÇÕES DA CONTA
                        SettingsOptionRow(
                            title = stringResource(R.string.settings_switch_list),
                            value = "$activeListsCount lista(s) ativa(s)",
                            icon = Icons.Default.FormatListBulleted,
                            onClick = onSwitchList
                        )
                        SettingsOptionRow(
                            title = stringResource(R.string.settings_refresh_lists),
                            value = "Sincronizar LC Admin",
                            icon = Icons.Default.Refresh,
                            onClick = onRefreshLists
                        )
                        SettingsOptionRow(
                            title = stringResource(R.string.settings_logout_list),
                            value = stringResource(R.string.settings_logout_list_desc),
                            icon = Icons.Default.ExitToApp,
                            badgeText = stringResource(R.string.settings_logout_btn),
                            badgeColor = Color(0xFFFF5252),
                            onClick = onLogoutList
                        )
                    }
                    SettingsSection.PARENTAL -> {
                        val isParentalEnabled = AppSettingsManager.isParentalControlEnabled(LocalContext.current)
                        SettingsOptionRow(
                            title = stringResource(R.string.settings_parental_protection_title),
                            value = if (isParentalEnabled) stringResource(R.string.settings_parental_protection_enabled) else stringResource(R.string.settings_parental_protection_disabled),
                            icon = Icons.Default.Lock,
                            badgeText = if (isParentalEnabled) stringResource(R.string.settings_parental_enabled_badge) else stringResource(R.string.settings_parental_disabled_badge),
                            badgeColor = if (isParentalEnabled) Color(0xFF00E676) else TvSecondaryBlue,
                            onClick = onToggleParental
                        )
                        SettingsOptionRow(
                            title = stringResource(R.string.settings_parental_change_pin),
                            value = stringResource(R.string.settings_parental_change_pin_desc),
                            icon = Icons.Default.Lock,
                            onClick = onChangePin
                        )
                        SettingsOptionRow(
                            title = stringResource(R.string.settings_parental_protected_content),
                            value = stringResource(R.string.settings_parental_protected_content_desc),
                            icon = Icons.Default.Lock,
                            onClick = onOpenParentalManagement
                        )
                    }
                    SettingsSection.DEVICE -> {
                        SettingsOptionRow(
                            title = stringResource(R.string.settings_device_info_title),
                            value = stringResource(R.string.settings_device_info_desc),
                            icon = Icons.Default.PhoneAndroid,
                            onClick = onOpenDeviceModal
                        )
                    }
                    SettingsSection.ABOUT -> {
                        SettingsOptionRow(
                            title = stringResource(R.string.settings_clear_cache),
                            value = stringResource(R.string.settings_clear_cache_desc),
                            icon = Icons.Default.DeleteSweep,
                            badgeText = stringResource(R.string.settings_clear_cache_btn),
                            badgeColor = Color(0xFFFF5252),
                            onClick = onOpenClearCacheModal
                        )
                        SettingsOptionRow(
                            title = "LC Player",
                            value = stringResource(R.string.settings_about_app_desc),
                            icon = Icons.Default.Info,
                            badgeText = "v$appVersionName",
                            badgeColor = Color(0xFF00E676)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileSettingsView(
    selectedSection: SettingsSection?,
    onSelectSection: (SettingsSection?) -> Unit,
    selectedTimeFormat: String,
    onToggleTimeFormat: () -> Unit,
    selectedLanguage: String,
    onToggleLanguage: () -> Unit,
    selectedLiveFormat: String,
    onToggleLiveFormat: () -> Unit,
    selectedAutoNextEpisode: Boolean,
    onToggleAutoNextEpisode: () -> Unit,
    activeListsCount: Int,
    onSwitchList: () -> Unit,
    onRefreshLists: () -> Unit,
    onOpenDeviceModal: () -> Unit,
    onOpenClearCacheModal: () -> Unit,
    onLogoutList: () -> Unit,
    onToggleParental: () -> Unit,
    onChangePin: () -> Unit,
    onOpenParentalManagement: () -> Unit,
    appVersionName: String,
    uiState: com.example.ui.activation.ActivationUiState,
    viewModel: ActivationViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val localCtx = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 70.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selectedSection == null) {
            // TELA PRINCIPAL DE CONFIGURAÇÕES MOBILE (Lista de Seções)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(AppCorners.md)
                        .background(TvSurface)
                        .border(1.dp, TvSurfaceVariant, AppCorners.md)
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.btn_back),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.settings_title),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Lista Vertical de Seções Mobile
            SettingsSection.values().forEach { section ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(AppCorners.md)
                        .background(TvSurface)
                        .border(1.dp, TvSurfaceVariant, AppCorners.md)
                        .clickable { onSelectSection(section) }
                        .padding(horizontal = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = section.icon,
                                contentDescription = null,
                                tint = TvSecondaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(section.titleResId),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = TvTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        } else {
            // TELA DE SUB-SEÇÃO SELECIONADA MOBILE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(AppCorners.md)
                        .background(TvSurface)
                        .border(1.dp, TvSurfaceVariant, AppCorners.md)
                        .clickable { onSelectSection(null) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = stringResource(selectedSection.titleResId),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (selectedSection) {
                SettingsSection.GENERAL -> {
                    val langLabel = when (selectedLanguage) {
                        "EN" -> "English"
                        "ES" -> "Español"
                        else -> "Português (BR)"
                    }
                    SettingsOptionRow(
                        title = stringResource(R.string.settings_language_title),
                        value = langLabel,
                        icon = Icons.Default.Language,
                        onClick = onToggleLanguage
                    )

                    val timeLabel = if (selectedTimeFormat == "24H") "24 Horas (14:30)" else "12 Horas (02:30 PM)"
                    SettingsOptionRow(
                        title = stringResource(R.string.settings_time_format_title),
                        value = timeLabel,
                        icon = Icons.Default.AccessTime,
                        onClick = onToggleTimeFormat
                    )
                }
                SettingsSection.PLAYBACK -> {
                    val liveFormatLabel = if (selectedLiveFormat == "TS") "MPEG-TS (.ts)" else "HLS / M3U8 (.m3u8)"
                    SettingsOptionRow(
                        title = stringResource(R.string.settings_live_format_title),
                        value = liveFormatLabel,
                        icon = Icons.Default.LiveTv,
                        badgeText = selectedLiveFormat,
                        onClick = onToggleLiveFormat
                    )

                    SettingsOptionRow(
                        title = "Próximo episódio automático",
                        value = if (selectedAutoNextEpisode) "Ativado (Avança na mesma temporada)" else "Desativado",
                        icon = Icons.Default.PlayArrow,
                        badgeText = if (selectedAutoNextEpisode) "Ativado" else "Desativado",
                        badgeColor = if (selectedAutoNextEpisode) Color(0xFF00E676) else TvSecondaryBlue,
                        onClick = onToggleAutoNextEpisode
                    )

                    SettingsOptionRow(
                        title = stringResource(R.string.settings_player_title),
                        value = "ExoPlayer (AndroidX Media3)",
                        icon = Icons.Default.Settings,
                        badgeText = "EXOPLAYER"
                    )
                }
                SettingsSection.ACCOUNT -> {
                    val currentList = uiState.lists.firstOrNull { it.id == uiState.contentCategoriesState?.listId }
                        ?: uiState.lists.firstOrNull { it.id == uiState.selectedListId }
                        ?: uiState.lists.firstOrNull()

                    if (currentList != null && (currentList.sourceName.isNotBlank() || currentList.title.isNotBlank())) {
                        SettingsAccountInfoRow(
                            label = stringResource(R.string.settings_server),
                            value = currentList.sourceName.ifBlank { currentList.title }
                        )
                    }

                    if (currentList != null && currentList.username.isNotBlank()) {
                        SettingsAccountInfoRow(
                            label = stringResource(R.string.settings_login),
                            value = currentList.username
                        )
                    }

                    if (currentList != null && !currentList.expiresAt.isNullOrBlank()) {
                        SettingsAccountInfoRow(
                            label = stringResource(R.string.settings_account_validity),
                            value = currentList.formattedExpiration
                        )
                    }

                    SettingsAccountInfoRow(
                        label = stringResource(R.string.settings_app_validity),
                        value = stringResource(R.string.settings_unlimited)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SettingsOptionRow(
                        title = stringResource(R.string.settings_switch_list),
                        value = "$activeListsCount lista(s) ativa(s)",
                        icon = Icons.Default.FormatListBulleted,
                        onClick = onSwitchList
                    )
                    SettingsOptionRow(
                        title = stringResource(R.string.settings_refresh_lists),
                        value = "Sincronizar LC Admin",
                        icon = Icons.Default.Refresh,
                        onClick = onRefreshLists
                    )
                    SettingsOptionRow(
                        title = stringResource(R.string.settings_logout_list),
                        value = stringResource(R.string.settings_logout_list_desc),
                        icon = Icons.Default.ExitToApp,
                        badgeText = stringResource(R.string.settings_logout_btn),
                        badgeColor = Color(0xFFFF5252),
                        onClick = onLogoutList
                    )
                }
                SettingsSection.PARENTAL -> {
                    val isParentalEnabled = AppSettingsManager.isParentalControlEnabled(LocalContext.current)
                    SettingsOptionRow(
                        title = stringResource(R.string.settings_parental_protection_title),
                        value = if (isParentalEnabled) stringResource(R.string.settings_parental_protection_enabled) else stringResource(R.string.settings_parental_protection_disabled),
                        icon = Icons.Default.Lock,
                        badgeText = if (isParentalEnabled) stringResource(R.string.settings_parental_enabled_badge) else stringResource(R.string.settings_parental_disabled_badge),
                        badgeColor = if (isParentalEnabled) Color(0xFF00E676) else TvSecondaryBlue,
                        onClick = onToggleParental
                    )
                    SettingsOptionRow(
                        title = stringResource(R.string.settings_parental_change_pin),
                        value = stringResource(R.string.settings_parental_change_pin_desc),
                        icon = Icons.Default.Lock,
                        onClick = onChangePin
                    )
                    SettingsOptionRow(
                        title = stringResource(R.string.settings_parental_protected_content),
                        value = stringResource(R.string.settings_parental_protected_content_desc),
                        icon = Icons.Default.Lock,
                        onClick = onOpenParentalManagement
                    )
                }
                SettingsSection.DEVICE -> {
                    SettingsCopyableCard(
                        label = "Device ID / MAC",
                        value = uiState.deviceId,
                        onCopy = {
                            copyToClipboard(localCtx, "Device ID", uiState.deviceId)
                            viewModel.showToastFeedback(localCtx.getString(R.string.msg_device_id_copied))
                        }
                    )

                    SettingsCopyableCard(
                        label = "KEY do Dispositivo",
                        value = uiState.key,
                        onCopy = {
                            copyToClipboard(localCtx, "KEY", uiState.key)
                            viewModel.showToastFeedback(localCtx.getString(R.string.msg_key_copied))
                        }
                    )

                    SettingsDisabledCard(
                        title = stringResource(R.string.settings_qr_pairing),
                        subtitle = stringResource(R.string.settings_qr_pairing_subtitle),
                        icon = Icons.Default.QrCode,
                        badgeText = stringResource(R.string.settings_coming_soon)
                    )
                }
                SettingsSection.ABOUT -> {
                    SettingsOptionRow(
                        title = stringResource(R.string.settings_clear_cache),
                        value = stringResource(R.string.settings_clear_cache_desc),
                        icon = Icons.Default.DeleteSweep,
                        badgeText = stringResource(R.string.settings_clear_cache_btn),
                        badgeColor = Color(0xFFFF5252),
                        onClick = onOpenClearCacheModal
                    )
                    SettingsOptionRow(
                        title = "LC Player",
                        value = stringResource(R.string.settings_about_app_desc),
                        icon = Icons.Default.Info,
                        badgeText = "v$appVersionName",
                        badgeColor = Color(0xFF00E676)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsAccountInfoRow(
    label: String,
    value: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(AppCorners.md)
            .background(TvSurface)
            .border(1.dp, TvSurfaceVariant, AppCorners.md)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TvTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SettingsOptionRow(
    title: String,
    value: String,
    icon: ImageVector,
    badgeText: String? = null,
    badgeColor: Color = TvSecondaryBlue,
    isDisabled: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(AppCorners.md)
            .background(if (isDisabled) Color(0xFF09101C) else TvSurface)
            .border(
                width = 1.dp,
                color = if (isDisabled) Color(0x11FFFFFF) else TvSurfaceVariant,
                shape = AppCorners.md
            )
            .then(
                if (!isDisabled && onClick != null) {
                    Modifier.tvFocusable(
                        shape = AppCorners.md,
                        focusBorderColor = Color.White,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isDisabled) Color(0x11FFFFFF) else Color(0x220084FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isDisabled) TvTextMuted else TvSecondaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        color = if (isDisabled) TvTextMuted else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = value,
                        color = TvTextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!badgeText.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(AppCorners.pill)
                        .background(if (isDisabled) Color(0x22FFFFFF) else badgeColor.copy(alpha = 0.2f))
                        .border(1.dp, if (isDisabled) Color(0x33FFFFFF) else badgeColor.copy(alpha = 0.5f), AppCorners.pill)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = if (isDisabled) TvTextMuted else badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
@Composable
private fun MobileSettingsGridView(
    selectedTimeFormat: String,
    onToggleTimeFormat: () -> Unit,
    selectedLanguage: String,
    onToggleLanguage: () -> Unit,
    selectedLiveFormat: String,
    onToggleLiveFormat: () -> Unit,
    activeListsCount: Int,
    onSwitchList: () -> Unit,
    onRefreshLists: () -> Unit,
    onOpenDeviceModal: () -> Unit,
    onOpenClearCacheModal: () -> Unit,
    appVersionName: String
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = AppSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsGridTileCard(
                title = stringResource(R.string.settings_my_account),
                subtitle = "Dispositivo Ativo",
                icon = Icons.Default.AccountCircle,
                badgeText = stringResource(R.string.settings_coming_soon),
                isDisabled = true
            )
        }
        item {
            SettingsGridTileCard(
                title = stringResource(R.string.settings_switch_list),
                subtitle = "$activeListsCount lista(s)",
                icon = Icons.Default.FormatListBulleted,
                onClick = onSwitchList
            )
        }
        item {
            SettingsGridTileCard(
                title = stringResource(R.string.settings_refresh_lists),
                subtitle = "Sincronizar",
                icon = Icons.Default.Refresh,
                onClick = onRefreshLists
            )
        }
        item {
            val liveFormatLabel = if (selectedLiveFormat == "TS") "MPEG-TS (.ts)" else "HLS / M3U8 (.m3u8)"
            SettingsGridTileCard(
                title = stringResource(R.string.settings_live_format_title),
                subtitle = liveFormatLabel,
                icon = Icons.Default.LiveTv,
                badgeText = selectedLiveFormat,
                onClick = onToggleLiveFormat
            )
        }
        item {
            val langLabel = when (selectedLanguage) {
                "EN" -> "English"
                "ES" -> "Español"
                else -> "Português"
            }
            SettingsGridTileCard(
                title = stringResource(R.string.settings_language_title),
                subtitle = langLabel,
                icon = Icons.Default.Language,
                badgeText = selectedLanguage,
                onClick = onToggleLanguage
            )
        }
        item {
            SettingsGridTileCard(
                title = stringResource(R.string.settings_time_format_title),
                subtitle = selectedTimeFormat,
                icon = Icons.Default.AccessTime,
                badgeText = selectedTimeFormat,
                onClick = onToggleTimeFormat
            )
        }
        item {
            SettingsGridTileCard(
                title = stringResource(R.string.settings_device_dialog_title),
                subtitle = "ID e KEY",
                icon = Icons.Default.PhoneAndroid,
                onClick = onOpenDeviceModal
            )
        }
        item {
            SettingsGridTileCard(
                title = stringResource(R.string.settings_clear_cache),
                subtitle = "Categorias",
                icon = Icons.Default.DeleteSweep,
                iconColor = Color(0xFFFF5252),
                onClick = onOpenClearCacheModal
            )
        }
        item {
            SettingsGridTileCard(
                title = "LC Player",
                subtitle = "v$appVersionName",
                icon = Icons.Default.Info,
                badgeText = "v$appVersionName",
                badgeColor = Color(0xFF00E676)
            )
        }
    }
}

@Composable
private fun SettingsGridTileCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color = TvSecondaryBlue,
    badgeText: String? = null,
    badgeColor: Color = TvSecondaryBlue,
    isDisabled: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(AppCorners.lg)
            .background(if (isDisabled) Color(0xFF09101C) else TvCardBackground)
            .border(
                width = 1.dp,
                color = if (isDisabled) Color(0x11FFFFFF) else TvSurfaceVariant,
                shape = AppCorners.lg
            )
            .then(
                if (!isDisabled && onClick != null) {
                    Modifier.tvFocusable(
                        shape = AppCorners.lg,
                        focusBorderColor = TvFocusBorder,
                        focusedScale = 1.04f,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isDisabled) Color(0x11FFFFFF) else Color(0x220084FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isDisabled) TvTextMuted else iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (!badgeText.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(AppCorners.pill)
                            .background(if (isDisabled) Color(0x22FFFFFF) else badgeColor.copy(alpha = 0.2f))
                            .border(1.dp, if (isDisabled) Color(0x33FFFFFF) else badgeColor.copy(alpha = 0.5f), AppCorners.pill)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = if (isDisabled) TvTextMuted else badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column {
                Text(
                    text = title,
                    color = if (isDisabled) TvTextMuted else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TvTextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SettingsTopBar(
    listTitle: String,
    onBackClick: () -> Unit,
    backFocusRequester: FocusRequester
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 920.dp)
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(AppCorners.md)
                    .background(TvSurface)
                    .border(1.dp, TvSurfaceVariant, AppCorners.md)
                    .tvFocusable(
                        shape = AppCorners.md,
                        focusBorderColor = TvFocusBorder,
                        focusedScale = 1.08f,
                        onClick = onBackClick
                    )
                    .focusRequester(backFocusRequester)
                    .testTag("btn_back_to_home_from_settings"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.btn_back),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = stringResource(R.string.settings_title),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }

        if (listTitle.isNotBlank()) {
            Row(
                modifier = Modifier
                    .clip(AppCorners.pill)
                    .background(Color(0x1A00C853))
                    .border(1.dp, Color(0x3300C853), AppCorners.pill)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = listTitle,
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
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
    } catch (_: Exception) {}
}

@Composable
private fun SettingsCopyableCard(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppCorners.md)
            .background(TvCardBackground)
            .border(1.dp, TvSurfaceVariant, AppCorners.md)
            .padding(AppSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = TvTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value.ifBlank { "Não disponível" },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(AppCorners.sm)
                    .background(TvSurface)
                    .border(1.dp, TvSurfaceVariant, AppCorners.sm)
                    .tvFocusable(
                        shape = AppCorners.sm,
                        focusBorderColor = TvFocusBorder,
                        onClick = onCopy
                    )
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.settings_copy),
                        tint = TvSecondaryBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.settings_copy),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
@Composable
private fun SettingsDisabledCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppCorners.md)
            .background(Color(0xFF09101C))
            .border(1.dp, Color(0x11FFFFFF), AppCorners.md)
            .padding(AppSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x11FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = TvTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TvTextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TvTextMuted,
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(AppCorners.pill)
                    .background(Color(0x22FFFFFF))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    color = TvTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
