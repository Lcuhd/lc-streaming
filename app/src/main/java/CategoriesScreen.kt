
package com.example.ui.categories

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import coil.compose.AsyncImage
import com.example.ui.activation.HomeContinueItem
import com.example.ui.activation.HomeRecentItem
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
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
import androidx.compose.runtime.produceState
import com.example.domain.model.VIRTUAL_FAVORITES_CATEGORY_ID
import com.example.domain.model.createFavoritesCategory
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.util.AppTimeFormatter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.CategoryType
import com.example.domain.model.ListContentCategoriesState
import com.example.domain.model.XtreamCategory
import com.example.ui.activation.ActivationViewModel
import com.example.ui.components.tvFocusable
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.AppCorners
import com.example.ui.theme.AppSpacing
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvCardBackground
import com.example.ui.theme.TvFocusBorder
import com.example.ui.theme.TvPrimaryBlue
import com.example.ui.theme.TvSecondaryBlue
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextMuted
import com.example.ui.theme.TvTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tela Principal de Entrada e exploração do catálogo do LC Player.
 * Estruturada com base no princípio de navegação limpa do IPTV Smarters:
 * - Áreas principais: TV ONLINE (Esq, Altura Dupla) | FILMES (Dir Topo) / SÉRIES (Dir Baixo)
 * - Configurações: Ação discreta inferior (Pill button)
 * - Leitura limpa, poucas opções, sem contagens numéricas e sem poluição visual.
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
    val wideHomeContentFocusRequester = remember { FocusRequester() }
    var showDeviceInfoDialog by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var isInnerCategoryExplorerOpen by remember { mutableStateOf(false) }
    var pendingDirectOpen by remember { mutableStateOf<CategoryType?>(null) }

    var showPinPromptDialog by remember { mutableStateOf(false) }
    var pinPromptInput by remember { mutableStateOf("") }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingTargetKey by remember { mutableStateOf<Pair<String, String>?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun executeWithParentalCheck(
        protectionType: String,
        contentType: String,
        targetId: String,
        name: String?,
        action: () -> Unit
    ) {
        coroutineScope.launch {
            val isProtected = viewModel.isContentProtected(protectionType, contentType, targetId, name)
            if (isProtected) {
                pendingAction = action
                pendingTargetKey = Pair(protectionType, targetId)
                pinPromptInput = ""
                showPinPromptDialog = true
            } else {
                action()
            }
        }
    }

    val liveFavoriteIds by produceState<Set<String>>(initialValue = emptySet(), categoriesState?.liveCategories, uiState.toastFeedback) {
        value = viewModel.getFavoriteIds("LIVE")
    }
    val vodFavoriteIds by produceState<Set<String>>(initialValue = emptySet(), categoriesState?.vodCategories, uiState.toastFeedback) {
        value = viewModel.getFavoriteIds("VOD")
    }
    val completedMovieIds by produceState<Set<String>>(initialValue = emptySet(), categoriesState?.vodStreamsState, uiState.toastFeedback) {
        value = viewModel.getCompletedMovieIdsForList()
    }
    val seriesFavoriteIds by produceState<Set<String>>(initialValue = emptySet(), categoriesState?.seriesCategories, uiState.toastFeedback) {
        value = viewModel.getFavoriteIds("SERIES")
    }

    val displayLiveCategories = remember(categoriesState?.liveCategories, liveFavoriteIds) {
        val base = categoriesState?.liveCategories ?: emptyList()
        if (liveFavoriteIds.isNotEmpty()) {
            listOf(createFavoritesCategory(CategoryType.LIVE)) + base.filter { it.categoryId != VIRTUAL_FAVORITES_CATEGORY_ID }
        } else base.filter { it.categoryId != VIRTUAL_FAVORITES_CATEGORY_ID }
    }

    val displayVodCategories = remember(categoriesState?.vodCategories, vodFavoriteIds) {
        val base = categoriesState?.vodCategories ?: emptyList()
        if (vodFavoriteIds.isNotEmpty()) {
            listOf(createFavoritesCategory(CategoryType.VOD)) + base.filter { it.categoryId != VIRTUAL_FAVORITES_CATEGORY_ID }
        } else base.filter { it.categoryId != VIRTUAL_FAVORITES_CATEGORY_ID }
    }

    val displaySeriesCategories = remember(categoriesState?.seriesCategories, seriesFavoriteIds) {
        val base = categoriesState?.seriesCategories ?: emptyList()
        if (seriesFavoriteIds.isNotEmpty()) {
            listOf(createFavoritesCategory(CategoryType.SERIES)) + base.filter { it.categoryId != VIRTUAL_FAVORITES_CATEGORY_ID }
        } else base.filter { it.categoryId != VIRTUAL_FAVORITES_CATEGORY_ID }
    }

    // Reage SOMENTE a uma ação explícita do usuário na Home (pendingDirectOpen != null)
    LaunchedEffect(
        pendingDirectOpen,
        categoriesState?.isLiveLoaded,
        categoriesState?.isVodLoaded,
        categoriesState?.isSeriesLoaded,
        displayLiveCategories,
        displayVodCategories,
        displaySeriesCategories,
        categoriesState?.liveError,
        categoriesState?.vodError,
        categoriesState?.seriesError,
        categoriesState?.selectedCategoryForChannels
    ) {
        val targetSection = pendingDirectOpen ?: return@LaunchedEffect

        if (categoriesState?.selectedCategoryForChannels == null) {
            when (targetSection) {
                CategoryType.LIVE -> {
                    if (categoriesState?.isLiveLoaded == true) {
                        val firstLive = displayLiveCategories.firstOrNull()
                        pendingDirectOpen = null
                        if (firstLive != null) {
                            viewModel.openCategory(firstLive)
                        }
                    } else if (categoriesState?.liveError != null) {
                        pendingDirectOpen = null
                    }
                }
                CategoryType.VOD -> {
                    if (categoriesState?.isVodLoaded == true) {
                        val firstVod = displayVodCategories.firstOrNull()
                        pendingDirectOpen = null
                        if (firstVod != null) {
                            viewModel.openCategory(firstVod)
                        }
                    } else if (categoriesState?.vodError != null) {
                        pendingDirectOpen = null
                    }
                }
                CategoryType.SERIES -> {
                    if (categoriesState?.isSeriesLoaded == true) {
                        val firstSeries = displaySeriesCategories.firstOrNull()
                        pendingDirectOpen = null
                        if (firstSeries != null) {
                            viewModel.openCategory(firstSeries)
                        }
                    } else if (categoriesState?.seriesError != null) {
                        pendingDirectOpen = null
                    }
                }
            }
        }
    }

    BackHandler {
        if (showSettingsScreen) {
            showSettingsScreen = false
        } else if (categoriesState?.seriesDetailsState != null) {
            viewModel.closeSeriesDetails()
        } else if (categoriesState?.vodDetailsState != null) {
            viewModel.closeVodDetails()
        } else if (categoriesState?.selectedCategoryForChannels != null) {
            pendingDirectOpen = null
            when (categoriesState.selectedCategoryForChannels?.type) {
                CategoryType.VOD -> viewModel.closeVodCategoryMovies()
                CategoryType.SERIES -> viewModel.closeSeriesCategory()
                else -> viewModel.closeLiveCategoryChannels()
            }
        } else if (isInnerCategoryExplorerOpen) {
            isInnerCategoryExplorerOpen = false
        } else {
            pendingDirectOpen = null
            viewModel.closeContentCategories()
        }
    }

    LaunchedEffect(uiState.toastFeedback) {
        uiState.toastFeedback?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToastFeedback()
        }
    }

    LaunchedEffect(Unit) {
        try {
            initialFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    if (categoriesState == null) {
        LaunchedEffect(Unit) {
            viewModel.closeContentCategories()
        }
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050810),
                        TvBackground,
                        Color(0xFF03050C)
                    )
                )
            )
    ) {
        val isWideLayout = this.maxWidth >= 600.dp

        val currentMobileSection: AppMobileNavSection = remember(
            showSettingsScreen,
            categoriesState.seriesDetailsState,
            categoriesState.vodDetailsState,
            categoriesState.selectedCategoryForChannels,
            isInnerCategoryExplorerOpen,
            categoriesState.selectedTab,
            pendingDirectOpen
        ) {
            when {
                showSettingsScreen -> AppMobileNavSection.SETTINGS
                categoriesState.seriesDetailsState != null -> AppMobileNavSection.SERIES
                categoriesState.vodDetailsState != null -> AppMobileNavSection.VOD
                categoriesState.selectedCategoryForChannels != null -> {
                    when (categoriesState.selectedCategoryForChannels?.type) {
                        CategoryType.LIVE -> AppMobileNavSection.LIVE
                        CategoryType.VOD -> AppMobileNavSection.VOD
                        CategoryType.SERIES -> AppMobileNavSection.SERIES
                        else -> AppMobileNavSection.HOME
                    }
                }
                isInnerCategoryExplorerOpen -> {
                    when (categoriesState.selectedTab) {
                        CategoryType.LIVE -> AppMobileNavSection.LIVE
                        CategoryType.VOD -> AppMobileNavSection.VOD
                        CategoryType.SERIES -> AppMobileNavSection.SERIES
                        else -> AppMobileNavSection.HOME
                    }
                }
                else -> {
                    if (pendingDirectOpen != null) {
                        when (pendingDirectOpen) {
                            CategoryType.LIVE -> AppMobileNavSection.LIVE
                            CategoryType.VOD -> AppMobileNavSection.VOD
                            CategoryType.SERIES -> AppMobileNavSection.SERIES
                            else -> AppMobileNavSection.HOME
                        }
                    } else {
                        AppMobileNavSection.HOME
                    }
                }
            }
        }
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

        // Se a tela de Configurações, o detalhe de uma série ou uma categoria estiver aberta
        if (showSettingsScreen) {
            SettingsScreen(
                viewModel = viewModel,
                onBackClick = { showSettingsScreen = false }
            )
        } else if (categoriesState.seriesDetailsState != null) {
            SeriesDetailsView(
                detailsState = categoriesState.seriesDetailsState,
                onBackClick = { viewModel.closeSeriesDetails() },
                onPrimaryButtonClick = { viewModel.openPrimarySeriesButton() },
                onSelectSeason = { seasonNum -> viewModel.selectSeasonForSeries(seasonNum) },
                onEpisodeClick = { episode -> viewModel.playSeriesEpisode(episode) },
                onRetryClick = { viewModel.retrySeriesDetails() },
                onIsFavorite = { id -> viewModel.isContentFavorite("SERIES", id) },
                onToggleFavorite = { id -> viewModel.toggleFavorite("SERIES", id) }
            )
        } else if (categoriesState.vodDetailsState != null && categoriesState.selectedCategoryForChannels == null) {
            val vodDetState = categoriesState.vodDetailsState!!
            MovieDetailsView(
                movie = vodDetState.movie,
                vodDetailsState = vodDetState,
                recommendations = emptyList(),
                onBackClick = { viewModel.closeVodDetails() },
                onRetryClick = { viewModel.retryVodDetails() },
                onPlayMovie = { movie -> viewModel.playVodMovie(movie) },
                onGetMovieProgress = { movie -> viewModel.getMovieProgress(movie) },
                onIsFavorite = { id -> viewModel.isContentFavorite("VOD", id) },
                onToggleFavorite = { id -> viewModel.toggleFavorite("VOD", id) }
            )
        } else if (categoriesState.selectedCategoryForChannels != null) {
            val selectedCat = categoriesState.selectedCategoryForChannels!!
            when (selectedCat.type) {
                CategoryType.VOD -> {
                    CategoryVodMoviesView(
                        category = selectedCat,
                        allCategories = displayVodCategories,
                        onSelectCategory = { newCat ->
                            executeWithParentalCheck("CATEGORY", newCat.type.name, newCat.categoryId, newCat.categoryName) {
                                viewModel.openCategory(newCat)
                            }
                        },
                        vodState = categoriesState.vodStreamsState,
                        vodDetailsState = categoriesState.vodDetailsState,
                        listTitle = categoriesState.listTitle,
                        onBackClick = {
                            pendingDirectOpen = null
                            viewModel.closeVodCategoryMovies()
                        },
                        onRefreshClick = { viewModel.retryVodStreamsForCurrentCategory() },
                        onOpenMovieDetails = { movie ->
                            executeWithParentalCheck("CHANNEL", "VOD", movie.streamId, movie.name) {
                                viewModel.openVodDetails(movie)
                            }
                        },
                        onCloseMovieDetails = {
                            viewModel.closeVodDetails()
                        },
                        onRetryMovieDetails = {
                            viewModel.retryVodDetails()
                        },
                        onMovieClick = { movie ->
                            executeWithParentalCheck("CHANNEL", "VOD", movie.streamId, movie.name) {
                                viewModel.playVodMovie(movie)
                            }
                        },
                        onGetMovieProgress = { movie ->
                            viewModel.getMovieProgress(movie)
                        },
                        onIsFavorite = { id -> viewModel.isContentFavorite("VOD", id) },
                        onToggleFavorite = { id -> viewModel.toggleFavorite("VOD", id) },
                        completedMovieIds = completedMovieIds
                    )
                }
                CategoryType.SERIES -> {
                    CategorySeriesView(
                        category = selectedCat,
                        allCategories = displaySeriesCategories,
                        onSelectCategory = { newCat ->
                            executeWithParentalCheck("CATEGORY", newCat.type.name, newCat.categoryId, newCat.categoryName) {
                                viewModel.openCategory(newCat)
                            }
                        },
                        seriesState = categoriesState.seriesState,
                        listTitle = categoriesState.listTitle,
                        onBackClick = {
                            pendingDirectOpen = null
                            viewModel.closeSeriesCategory()
                        },
                        onRefreshClick = { viewModel.retrySeriesForCurrentCategory() },
                        onSeriesClick = { series ->
                            executeWithParentalCheck("CATEGORY", "SERIES", series.seriesId, series.name) {
                                viewModel.openSeriesDetails(series)
                            }
                        }
                    )
                }
                CategoryType.LIVE -> {
                    val activeList = uiState.lists.firstOrNull { it.id == categoriesState.listId } ?: uiState.lists.firstOrNull()

                    CategoryChannelsView(
                        category = selectedCat,
                        allCategories = displayLiveCategories,
                        activeList = activeList,
                        onSelectCategory = { newCat ->
                            executeWithParentalCheck("CATEGORY", newCat.type.name, newCat.categoryId, newCat.categoryName) {
                                viewModel.openCategory(newCat)
                            }
                        },
                        streamsState = categoriesState.liveStreamsState,
                        channelEpgStates = uiState.channelEpgStates,
                        listTitle = categoriesState.listTitle,
                        onBackClick = {
                            pendingDirectOpen = null
                            viewModel.closeLiveCategoryChannels()
                        },
                        onRefreshClick = { viewModel.retryLiveStreamsForCurrentCategory() },
                        onChannelClick = { channel ->
                            executeWithParentalCheck("CHANNEL", "LIVE", channel.streamId, channel.name) {
                                viewModel.playLiveChannel(channel)
                            }
                        },
                        onChannelFocused = { streamId ->
                            viewModel.onChannelFocused(streamId)
                        },
                        onIsFavorite = { id -> viewModel.isContentFavorite("LIVE", id) },
                        onToggleFavorite = { id -> viewModel.toggleFavorite("LIVE", id) }
                    )
                }
            }
        } else if (isInnerCategoryExplorerOpen) {
            // EXPLORADOR DEDICADO DE CATEGORIAS
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = AppSpacing.xl, top = AppSpacing.md, end = AppSpacing.xl, bottom = AppSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CategoryExplorerHeaderBar(
                    selectedTab = categoriesState.selectedTab,
                    listTitle = categoriesState.listTitle,
                    isRefreshing = categoriesState.isAnyLoading,
                    onBackClick = { isInnerCategoryExplorerOpen = false },
                    onRefreshClick = { viewModel.refreshAllCategoriesForCurrentList() },
                    onInfoClick = { showDeviceInfoDialog = true }
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

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

                DiscreetFooterBar(deviceId = uiState.deviceId)
            }
        } else {
            // HOME PORTA DE ENTRADA
            if (isWideLayout) {
                // TV / WIDESCREEN HOME NOVA COM MENU LATERAL E SEÇÕES CONTINUAR ASSISTINDO / RECENTES
                val recentItems = uiState.homeRecentItems
                val continueMovies = remember(uiState.homeContinueItems) {
                    uiState.homeContinueItems.filter { it.contentType == "VOD" }
                }
                val continueSeries = remember(uiState.homeContinueItems) {
                    uiState.homeContinueItems.filter { it.contentType == "SERIES" }
                }
                val hasContent = recentItems.isNotEmpty() ||
                    continueMovies.isNotEmpty() ||
                    continueSeries.isNotEmpty()
                val isGlobalRefreshing = uiState.isLoadingLists ||
                    uiState.isLoadingHomeContinue ||
                    uiState.isLoadingHomeRecent ||
                    categoriesState?.isAnyLoading == true

                LaunchedEffect(categoriesState.listId) {
                    if (uiState.homeRecentItems.isEmpty() && !uiState.isLoadingHomeRecent) {
                        viewModel.refreshHomeRecent()
                    }
                }

                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Menu Lateral Expansível da TV
                    TvSideNavRail(
                        selectedItem = TvSideNavItem.HOME,
                        initialFocusRequester = initialFocusRequester,
                        contentFocusRequester = wideHomeContentFocusRequester,
                        hasFocusableContent = hasContent,
                        isRefreshing = isGlobalRefreshing,
                        onSelectItem = { item ->
                            when (item) {
                                TvSideNavItem.HOME -> { /* Já na Home */ }
                                TvSideNavItem.LIVE -> {
                                    viewModel.selectCategoryTab(CategoryType.LIVE)
                                    viewModel.ensureCategoriesLoaded(CategoryType.LIVE)
                                    pendingDirectOpen = CategoryType.LIVE
                                }
                                TvSideNavItem.VOD -> {
                                    viewModel.selectCategoryTab(CategoryType.VOD)
                                    viewModel.ensureCategoriesLoaded(CategoryType.VOD)
                                    pendingDirectOpen = CategoryType.VOD
                                }
                                TvSideNavItem.SERIES -> {
                                    viewModel.selectCategoryTab(CategoryType.SERIES)
                                    viewModel.ensureCategoriesLoaded(CategoryType.SERIES)
                                    pendingDirectOpen = CategoryType.SERIES
                                }
                                TvSideNavItem.REFRESH -> {
                                    if (!isGlobalRefreshing) {
                                        viewModel.refreshAppFromHome()
                                    }
                                }
                                TvSideNavItem.SETTINGS -> {
                                    showSettingsScreen = true
                                }
                            }
                        }
                    )

                    // Área de Conteúdo Principal da TV Home (Lado Direito)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(start = AppSpacing.lg, top = AppSpacing.md, end = AppSpacing.xl, bottom = AppSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                    ) {
                        CompactCategoriesHeaderBar(
                            listTitle = categoriesState.listTitle,
                            sourceName = categoriesState.sourceName,
                            isConnected = categoriesState.liveError == null || categoriesState.vodError == null || categoriesState.seriesError == null,
                            onBackClick = { viewModel.closeContentCategories() }
                        )

                        // SEÇÃO RECENTES (TV / WIDE)
                        if (recentItems.isNotEmpty() || uiState.isLoadingHomeRecent) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Recentes",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )

                                if (uiState.isLoadingHomeRecent && recentItems.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(165.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = TvSecondaryBlue,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                } else if (recentItems.isNotEmpty()) {
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        itemsIndexed(
                                            recentItems,
                                            key = { _, item -> "${item.contentType}_${item.contentId}" }
                                        ) { index, item ->
                                            var cardModifier: Modifier = Modifier
                                            if (index == 0) {
                                                cardModifier = cardModifier
                                                    .focusProperties { left = initialFocusRequester }
                                                    .focusRequester(wideHomeContentFocusRequester)
                                            }

                                            WideHomeRecentCardItem(
                                                item = item,
                                                onClick = {
                                                    val pType = if (item.contentType == "VOD") "CHANNEL" else "CATEGORY"
                                                    val cType = if (item.contentType == "VOD") "VOD" else "SERIES"
                                                    executeWithParentalCheck(pType, cType, item.contentId, item.title) {
                                                        viewModel.openHomeRecentItem(item)
                                                    }
                                                },
                                                modifier = cardModifier
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // SEÇÃO CONTINUAR ASSISTINDO (TV / WIDE)
                        if (continueMovies.isNotEmpty() || continueSeries.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Continuar assistindo",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )

                                if (continueMovies.isNotEmpty()) {
                                    val movieRowCount = if (continueMovies.size <= 1) 1 else 2
                                    val movieGridHeight = if (movieRowCount == 1) 220.dp else 440.dp

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Filmes",
                                            color = TvTextSecondary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        LazyHorizontalGrid(
                                            rows = GridCells.Fixed(movieRowCount),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(movieGridHeight),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            itemsIndexed(continueMovies, key = { _, item -> item.progressKey }) { index, item ->
                                                var cardModifier: Modifier = Modifier
                                                if (index <= 1) {
                                                    cardModifier = cardModifier.focusProperties { left = initialFocusRequester }
                                                }
                                                if (recentItems.isEmpty() && index == 0) {
                                                    cardModifier = cardModifier.focusRequester(wideHomeContentFocusRequester)
                                                }

                                                WideHomeContinueCardItem(
                                                    item = item,
                                                    onClick = {
                                                        executeWithParentalCheck("CHANNEL", "VOD", item.contentId, item.title) {
                                                            viewModel.playHomeContinueItem(item)
                                                        }
                                                    },
                                                    modifier = cardModifier
                                                )
                                            }
                                        }
                                    }
                                }

                                if (continueSeries.isNotEmpty()) {
                                    val seriesRowCount = if (continueSeries.size <= 1) 1 else 2
                                    val seriesGridHeight = if (seriesRowCount == 1) 220.dp else 440.dp

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Séries",
                                            color = TvTextSecondary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        LazyHorizontalGrid(
                                            rows = GridCells.Fixed(seriesRowCount),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(seriesGridHeight),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            itemsIndexed(continueSeries, key = { _, item -> item.progressKey }) { index, item ->
                                                var cardModifier: Modifier = Modifier
                                                if (index <= 1) {
                                                    cardModifier = cardModifier.focusProperties { left = initialFocusRequester }
                                                }
                                                if (recentItems.isEmpty() && continueMovies.isEmpty() && index == 0) {
                                                    cardModifier = cardModifier.focusRequester(wideHomeContentFocusRequester)
                                                }

                                                WideHomeContinueCardItem(
                                                    item = item,
                                                    onClick = {
                                                        executeWithParentalCheck("CATEGORY", "SERIES", item.contentId, item.title) {
                                                            viewModel.playHomeContinueItem(item)
                                                        }
                                                    },
                                                    modifier = cardModifier
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        DiscreetFooterBar(deviceId = uiState.deviceId)
                    }
                }
            } else {
                // MOBILE HOME: ÁREA DE CONTEÚDO COM SEÇÕES SEPARADAS
                val recentItems = uiState.homeRecentItems
                val continueMovies = remember(uiState.homeContinueItems) {
                    uiState.homeContinueItems.filter { it.contentType == "VOD" }
                }
                val continueSeries = remember(uiState.homeContinueItems) {
                    uiState.homeContinueItems.filter { it.contentType == "SERIES" }
                }

                LaunchedEffect(categoriesState.listId) {
                    if (uiState.homeRecentItems.isEmpty() && !uiState.isLoadingHomeRecent) {
                        viewModel.refreshHomeRecent()
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CompactCategoriesHeaderBar(
                        listTitle = categoriesState.listTitle,
                        sourceName = categoriesState.sourceName,
                        isConnected = categoriesState.liveError == null || categoriesState.vodError == null || categoriesState.seriesError == null,
                        onBackClick = { viewModel.closeContentCategories() }
                    )

                    // SEÇÃO RECENTES (MOBILE)
                    if (recentItems.isNotEmpty() || uiState.isLoadingHomeRecent) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Recentes",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )

                            if (uiState.isLoadingHomeRecent && recentItems.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = TvSecondaryBlue,
                                        strokeWidth = 2.dp
                                    )
                                }
                            } else if (recentItems.isNotEmpty()) {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(recentItems, key = { "${it.contentType}_${it.contentId}" }) { item ->
                                        MobileHomeRecentCardItem(
                                            item = item,
                                            onClick = {
                                                val pType = if (item.contentType == "VOD") "CHANNEL" else "CATEGORY"
                                                val cType = if (item.contentType == "VOD") "VOD" else "SERIES"
                                                executeWithParentalCheck(pType, cType, item.contentId, item.title) {
                                                    viewModel.openHomeRecentItem(item)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (continueMovies.isNotEmpty() || continueSeries.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Continuar assistindo",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )

                            if (continueMovies.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Filmes",
                                        color = TvTextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    LazyHorizontalGrid(
                                        rows = GridCells.Fixed(2),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(410.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(continueMovies, key = { it.progressKey }) { item ->
                                            MobileHomeContinueCardItem(item = item)
                                        }
                                    }
                                }
                            }

                            if (continueSeries.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Séries",
                                        color = TvTextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    LazyHorizontalGrid(
                                        rows = GridCells.Fixed(2),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(410.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(continueSeries, key = { it.progressKey }) { item ->
                                            MobileHomeContinueCardItem(item = item)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    DiscreetFooterBar(deviceId = uiState.deviceId)
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = AppSpacing.xl)
        )

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

        if (showPinPromptDialog) {
            AlertDialog(
                onDismissRequest = {
                    showPinPromptDialog = false
                    pendingAction = null
                },
                containerColor = TvSurface,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = TvSecondaryBlue
                        )
                        Text(
                            text = "Conteúdo Protegido",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Digite o PIN do Controle dos Pais para liberar este conteúdo:",
                            color = TvTextSecondary,
                            fontSize = 13.sp
                        )
                        androidx.compose.foundation.text.BasicTextField(
                            value = pinPromptInput,
                            onValueChange = { if (it.length <= 4) pinPromptInput = it },
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
                            val targetKey = pendingTargetKey
                            val success = if (targetKey != null) {
                                viewModel.unlockContentWithPin(targetKey.first, "LIVE", targetKey.second, pinPromptInput)
                            } else {
                                viewModel.unlockContentWithPin("CATEGORY", "LIVE", "ANY", pinPromptInput)
                            }
                            if (success) {
                                showPinPromptDialog = false
                                pendingAction?.invoke()
                                pendingAction = null
                            }
                        }
                    ) {
                        Text(text = "Desbloquear", color = TvSecondaryBlue, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showPinPromptDialog = false
                            pendingAction = null
                        }
                    ) {
                        Text(text = stringResource(R.string.btn_back), color = TvTextMuted, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        if (!isWideLayout) {
            MobileBottomNavigationBar(
                currentSection = currentMobileSection,
                onSectionSelected = { section ->
                    when (section) {
                        AppMobileNavSection.HOME -> {
                            showSettingsScreen = false
                            isInnerCategoryExplorerOpen = false
                            pendingDirectOpen = null
                            viewModel.closeSeriesDetails()
                            viewModel.closeVodCategoryMovies()
                            viewModel.closeSeriesCategory()
                            viewModel.closeLiveCategoryChannels()
                        }
                        AppMobileNavSection.LIVE -> {
                            showSettingsScreen = false
                            isInnerCategoryExplorerOpen = false
                            viewModel.closeSeriesDetails()
                            viewModel.selectCategoryTab(CategoryType.LIVE)
                            viewModel.ensureCategoriesLoaded(CategoryType.LIVE)
                            if (categoriesState.selectedCategoryForChannels?.type != CategoryType.LIVE) {
                                val firstLive = displayLiveCategories.firstOrNull()
                                if (firstLive != null) {
                                    viewModel.openCategory(firstLive)
                                } else {
                                    pendingDirectOpen = CategoryType.LIVE
                                }
                            }
                        }
                        AppMobileNavSection.VOD -> {
                            showSettingsScreen = false
                            isInnerCategoryExplorerOpen = false
                            viewModel.closeSeriesDetails()
                            viewModel.selectCategoryTab(CategoryType.VOD)
                            viewModel.ensureCategoriesLoaded(CategoryType.VOD)
                            if (categoriesState.selectedCategoryForChannels?.type != CategoryType.VOD) {
                                val firstVod = displayVodCategories.firstOrNull()
                                if (firstVod != null) {
                                    viewModel.openCategory(firstVod)
                                } else {
                                    pendingDirectOpen = CategoryType.VOD
                                }
                            }
                        }
                        AppMobileNavSection.SERIES -> {
                            showSettingsScreen = false
                            isInnerCategoryExplorerOpen = false
                            viewModel.closeSeriesDetails()
                            viewModel.selectCategoryTab(CategoryType.SERIES)
                            viewModel.ensureCategoriesLoaded(CategoryType.SERIES)
                            if (categoriesState.selectedCategoryForChannels?.type != CategoryType.SERIES) {
                                val firstSeries = displaySeriesCategories.firstOrNull()
                                if (firstSeries != null) {
                                    viewModel.openCategory(firstSeries)
                                } else {
                                    pendingDirectOpen = CategoryType.SERIES
                                }
                            }
                        }
                        AppMobileNavSection.SETTINGS -> {
                            isInnerCategoryExplorerOpen = false
                            viewModel.closeSeriesDetails()
                            showSettingsScreen = true
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

enum class AppMobileNavSection {
    HOME,
    LIVE,
    VOD,
    SERIES,
    SETTINGS
}

@Composable
fun MobileBottomNavigationBar(
    currentSection: AppMobileNavSection,
    onSectionSelected: (AppMobileNavSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(Color(0xFF09101D))
            .border(width = (0.8).dp, color = Color(0xFF1B273A)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MobileNavItem(
                label = "Início",
                icon = Icons.Default.Home,
                isSelected = currentSection == AppMobileNavSection.HOME,
                onClick = { onSectionSelected(AppMobileNavSection.HOME) }
            )
            MobileNavItem(
                label = "TV",
                icon = Icons.Default.LiveTv,
                isSelected = currentSection == AppMobileNavSection.LIVE,
                onClick = { onSectionSelected(AppMobileNavSection.LIVE) }
            )
            MobileNavItem(
                label = "Filmes",
                icon = Icons.Default.Movie,
                isSelected = currentSection == AppMobileNavSection.VOD,
                onClick = { onSectionSelected(AppMobileNavSection.VOD) }
            )
            MobileNavItem(
                label = "Séries",
                icon = Icons.Default.VideoLibrary,
                isSelected = currentSection == AppMobileNavSection.SERIES,
                onClick = { onSectionSelected(AppMobileNavSection.SERIES) }
            )
            MobileNavItem(
                label = "Config.",
                icon = Icons.Default.Settings,
                isSelected = currentSection == AppMobileNavSection.SETTINGS,
                onClick = { onSectionSelected(AppMobileNavSection.SETTINGS) }
            )
        }
    }
}

@Composable
private fun MobileNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF0084FF) else Color(0xFFA0B0C4),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isSelected) Color(0xFF0084FF) else Color(0xFFA0B0C4),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

/**
 * TOPO COMPACTO:
 * LC Player
 * Provedor • Servidor • Status
 * Hora | Data
 */
@Composable
private fun CompactCategoriesHeaderBar(
    listTitle: String,
    sourceName: String,
    isConnected: Boolean,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var currentTimeText by remember { mutableStateOf("") }
    var currentDateText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        while (isActive) {
            val now = Date()
            currentTimeText = AppTimeFormatter.formatCurrentTime(context)
            currentDateText = dateFormat.format(now)
            delay(1000)
        }
    }

    val statusText = if (isConnected) "● Online" else "● Indisponível"

    val displayServerInfo = if (sourceName.isNotBlank() && sourceName != listTitle) {
        "$sourceName • $listTitle • $statusText"
    } else {
        "$listTitle • $statusText"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 880.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
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
                        color = TvSecondaryBlue,
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

            // Hora | Data
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
            }
        }

        // Subcabeçalho de Contexto (Provedor • Lista • Status)
        Text(
            text = displayServerInfo,
            color = TvTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 46.dp)
        )
    }
}

/**
 * Cabeçalho do Explorador Dedicado de Categorias (exibido ao selecionar TV ONLINE, FILMES ou SÉRIES).
 */
@Composable
private fun CategoryExplorerHeaderBar(
    selectedTab: CategoryType,
    listTitle: String,
    isRefreshing: Boolean,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    val sectionTitle = when (selectedTab) {
        CategoryType.LIVE -> "Categorias de TV ao vivo"
        CategoryType.VOD -> "Categorias de Filmes"
        CategoryType.SERIES -> "Categorias de Séries"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 880.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                    contentDescription = "Voltar para a Home",
                    tint = TvSecondaryBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = sectionTitle,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "LC Player • $listTitle",
                    color = TvTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(AppCorners.md)
                    .background(if (isRefreshing) Color(0xFF1B2433) else TvSurfaceVariant)
                    .border(1.dp, if (isRefreshing) TvFocusBorder else Color(0x330084FF), AppCorners.md)
                    .tvFocusable(
                        enabled = !isRefreshing,
                        shape = AppCorners.md,
                        focusBorderColor = TvFocusBorder,
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
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar",
                            tint = TvSecondaryBlue,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = if (isRefreshing) "Carregando..." else "Atualizar",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(AppCorners.md)
                    .background(TvSurfaceVariant)
                    .border(1.dp, Color(0x22FFFFFF), AppCorners.md)
                    .tvFocusable(
                        shape = AppCorners.md,
                        focusBorderColor = TvFocusBorder,
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
                        contentDescription = "Informações",
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
 * PAINEL CENTRALIZADO DA HOME:
 * TV ONLINE (Esq, Dominante) | FILMES (Dir Topo) / SÉRIES (Dir Baixo)
 * CONFIGURAÇÕES (Ação discreta em pill button na base)
 */
@Composable
private fun CategoriesHubPanel(
    selectedTab: CategoryType,
    onSelectTab: (CategoryType) -> Unit,
    onInfoClick: () -> Unit,
    initialFocusRequester: FocusRequester
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(0.72f).widthIn(max = 700.dp)) {
        val isWideLayout = this.maxWidth >= 600.dp

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isWideLayout) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // TV ONLINE (Esq, Dominante)
                        HomeSectionAreaCard(
                            title = "",
                            subtitle = "",
                            icon = Icons.Default.LiveTv,
                            bgResId = R.drawable.bg_home_live,
                            gradientColors = listOf(Color(0xFF063A3D), Color(0xFF031C22)),
                            glowBorderColor = Color(0xFF00E5FF),
                            isSelected = selectedTab == CategoryType.LIVE,
                            isDominant = true,
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxSize()
                                .focusRequester(initialFocusRequester),
                            onClick = { onSelectTab(CategoryType.LIVE) }
                        )

                        // FILMES & SÉRIES (Coluna Direita Empilhada)
                        Column(
                            modifier = Modifier
                                .weight(0.9f)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HomeSectionAreaCard(
                                title = "",
                                subtitle = "",
                                icon = Icons.Default.Movie,
                                bgResId = R.drawable.bg_home_movies,
                                gradientColors = listOf(Color(0xFF4A101C), Color(0xFF23070E)),
                                glowBorderColor = Color(0xFFFF3B5C),
                                isSelected = selectedTab == CategoryType.VOD,
                                isDominant = false,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                onClick = { onSelectTab(CategoryType.VOD) }
                            )

                            HomeSectionAreaCard(
                                title = "",
                                subtitle = "",
                                icon = Icons.Default.VideoLibrary,
                                bgResId = R.drawable.bg_home_series,
                                gradientColors = listOf(Color(0xFF22164A), Color(0xFF100A26)),
                                glowBorderColor = Color(0xFF7044FF),
                                isSelected = selectedTab == CategoryType.SERIES,
                                isDominant = false,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                onClick = { onSelectTab(CategoryType.SERIES) }
                            )
                        }
                    }

                    // CONFIGURAÇÕES (Ação discreta abaixo das áreas principais)
                    Box(
                        modifier = Modifier
                            .clip(AppCorners.pill)
                            .background(Color(0xFF0D1C33))
                            .border(1.dp, Color(0xFF0084FF), AppCorners.pill)
                            .tvFocusable(
                                shape = AppCorners.pill,
                                focusBorderColor = Color.White,
                                focusedScale = 1.05f,
                                onClick = onInfoClick
                            )
                            .padding(horizontal = AppSpacing.xl, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configurações",
                                tint = TvSecondaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.nav_settings),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // MOBILE VERTICAL (Retrato)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HomeSectionAreaCard(
                        title = stringResource(R.string.nav_live_tv),
                        subtitle = "",
                        icon = Icons.Default.LiveTv,
                        bgResId = R.drawable.bg_home_live,
                        gradientColors = listOf(Color(0xFF063A3D), Color(0xFF031C22)),
                        glowBorderColor = Color(0xFF00E5FF),
                        isDominant = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(95.dp)
                            .focusRequester(initialFocusRequester),
                        onClick = { onSelectTab(CategoryType.LIVE) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HomeSectionAreaCard(
                            title = stringResource(R.string.nav_movies),
                            subtitle = "",
                            icon = Icons.Default.Movie,
                            bgResId = R.drawable.bg_home_movies,
                            gradientColors = listOf(Color(0xFF4A101C), Color(0xFF23070E)),
                            glowBorderColor = Color(0xFFFF3B5C),
                            isDominant = false,
                            modifier = Modifier
                                .weight(1f)
                                .height(85.dp),
                            onClick = { onSelectTab(CategoryType.VOD) }
                        )

                        HomeSectionAreaCard(
                            title = stringResource(R.string.nav_series),
                            subtitle = "",
                            icon = Icons.Default.VideoLibrary,
                            bgResId = R.drawable.bg_home_series,
                            gradientColors = listOf(Color(0xFF22164A), Color(0xFF100A26)),
                            glowBorderColor = Color(0xFF7044FF),
                            isDominant = false,
                            modifier = Modifier
                                .weight(1f)
                                .height(85.dp),
                            onClick = { onSelectTab(CategoryType.SERIES) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(AppCorners.pill)
                            .background(Color(0xFF0D1C33))
                            .border(1.dp, Color(0xFF0084FF), AppCorners.pill)
                            .tvFocusable(
                                shape = AppCorners.pill,
                                focusBorderColor = Color.White,
                                onClick = onInfoClick
                            )
                            .padding(horizontal = AppSpacing.xl, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.nav_settings),
                                tint = TvSecondaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.nav_settings),
                                color = Color.White,
                                fontSize = 13.sp,
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
private fun HomeSectionAreaCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    glowBorderColor: Color = Color(0x33FFFFFF),
    isDominant: Boolean = false,
    onClick: () -> Unit = {},
    isSelected: Boolean = false,
    bgResId: Int? = null,
    modifier: Modifier = Modifier
) {
    val borderColor =
        if (isSelected) Color.White
        else glowBorderColor.copy(alpha = 0.5f)

    val borderWidth = if (isSelected) 2.dp else 1.2.dp

    Box(
        modifier = modifier
            .clip(AppCorners.lg)
            .background(Brush.linearGradient(gradientColors))
            .border(borderWidth, borderColor, AppCorners.lg)
            .tvFocusable(
                shape = AppCorners.lg,
                focusBorderColor = Color.White,
                focusGlowColor = glowBorderColor.copy(alpha = 0.6f),
                focusedScale = 1.03f,
                onClick = onClick
            )
    ) {
        if (bgResId != null) {
            Image(
                painter = painterResource(id = bgResId),
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = AppSpacing.lg,
                    vertical = AppSpacing.xs
                )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = AppSpacing.md)
                        .size(if (isDominant) 32.dp else 26.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .border(
                            1.dp,
                            Color(0x33FFFFFF),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Acessar $title",
                        tint = Color.White,
                        modifier = Modifier.size(
                            if (isDominant) 18.dp else 14.dp
                        )
                    )
                }
            }
        }
    }
}
/**
 * RODAPÉ DISCRETO:
 * Exibe unicamente o Dispositivo / MAC.
 */
@Composable
private fun DiscreetFooterBar(deviceId: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Dispositivo: $deviceId",
            color = TvTextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CategoryGridContent(
    categoriesState: ListContentCategoriesState,
    selectedTab: CategoryType,
    onRetryType: (CategoryType) -> Unit,
    onCategoryClick: (XtreamCategory) -> Unit
) {
    val (categoriesList, isLoading, errorMessage) = when (selectedTab) {
        CategoryType.LIVE -> Triple(categoriesState.liveCategories, categoriesState.isLoadingLive, categoriesState.liveError)
        CategoryType.VOD -> Triple(categoriesState.vodCategories, categoriesState.isLoadingVod, categoriesState.vodError)
        CategoryType.SERIES -> Triple(categoriesState.seriesCategories, categoriesState.isLoadingSeries, categoriesState.seriesError)
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = TvPrimaryBlue,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(38.dp)
                    )
                    Text(
                        text = "Carregando categorias...",
                        color = TvTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }
        errorMessage != null -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = errorMessage,
                        color = TvTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(AppCorners.md)
                            .background(TvPrimaryBlue)
                            .tvFocusable(
                                shape = AppCorners.md,
                                onClick = { onRetryType(selectedTab) }
                            )
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tentar Novamente",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        categoriesList.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        tint = TvTextMuted,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Nenhuma categoria encontrada",
                        color = TvTextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        }
        else -> {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 880.dp)
            ) {
                val isWideLayout = this.maxWidth >= 600.dp
                if (isWideLayout) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 200.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(categoriesList, key = { it.categoryId }) { category ->
                            CategoryTileCard(
                                category = category,
                                onClick = { onCategoryClick(category) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 70.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categoriesList, key = { it.categoryId }) { category ->
                            CategoryTileCard(
                                category = category,
                                onClick = { onCategoryClick(category) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTileCard(
    category: XtreamCategory,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(AppCorners.md)
            .background(TvCardBackground)
            .border(1.dp, TvSurfaceVariant, AppCorners.md)
            .tvFocusable(
                shape = AppCorners.md,
                focusBorderColor = TvFocusBorder,
                focusedScale = 1.04f,
                onClick = onClick
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
                text = category.categoryName,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = TvSecondaryBlue,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun DeviceInfoDialog(
    deviceId: String,
    key: String,
    deviceStatus: String,
    currentListTitle: String,
    sourceName: String,
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
                    text = "Informações da Sessão",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                InfoRow(label = "Lista Ativa", value = currentListTitle)
                InfoRow(label = "Servidor", value = sourceName)
                InfoRow(label = "Status Aparelho", value = deviceStatus)
                InfoRow(label = "Device ID", value = deviceId)
                InfoRow(label = "KEY", value = key)
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
private fun InfoRow(label: String, value: String) {
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

@Composable
private fun MobileHomeRecentCardItem(
    item: HomeRecentItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(TvCardBackground)
            .border(1.dp, TvSurfaceVariant, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(6.dp)
            .testTag("home_recent_card_${item.contentType}_${item.contentId}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF142033)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = "Capa de ${item.title}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.placeholder_poster),
                error = painterResource(id = R.drawable.placeholder_poster)
            )

            val badgeLabel = if (item.contentType == "VOD") "FILME" else "SÉRIE"
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xDD0084FF))
                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = badgeLabel,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun MobileHomeContinueCardItem(item: HomeContinueItem) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(TvCardBackground)
            .border(1.dp, TvSurfaceVariant, RoundedCornerShape(10.dp))
            .padding(6.dp)
            .testTag("home_continue_card_${item.progressKey}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF142033)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = "Capa de ${item.title}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.placeholder_poster),
                error = painterResource(id = R.drawable.placeholder_poster)
            )

            if (item.contentType == "SERIES" && item.seasonNum != null && item.episodeNum != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xCC050810))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "T${item.seasonNum} • E${item.episodeNum}",
                        color = TvSecondaryBlue,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { item.progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = TvSecondaryBlue,
            trackColor = Color(0x33FFFFFF)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.title,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp
        )
    }
}

enum class TvSideNavItem {
    HOME,
    LIVE,
    VOD,
    SERIES,
    REFRESH,
    SETTINGS
}

@Composable
private fun TvSideNavRail(
    selectedItem: TvSideNavItem,
    onSelectItem: (TvSideNavItem) -> Unit,
    initialFocusRequester: FocusRequester? = null,
    contentFocusRequester: FocusRequester? = null,
    hasFocusableContent: Boolean = false,
    isRefreshing: Boolean = false
) {
    var isRailFocused by remember { mutableStateOf(false) }

    val widthAnimated by animateDpAsState(
        targetValue = if (isRailFocused) 200.dp else 68.dp,
        label = "SideNavWidthAnimation"
    )

    Column(
        modifier = Modifier
            .width(widthAnimated)
            .fillMaxHeight()
            .onFocusChanged { focusState ->
                isRailFocused = focusState.hasFocus
            }
            .focusGroup()
            .background(TvCardBackground)
            .border(width = 1.dp, color = TvSurfaceVariant, shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
            .padding(vertical = AppSpacing.lg, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logotipo / Marca LC Player no topo do menu lateral
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = "LC",
                color = TvSecondaryBlue,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            if (isRailFocused) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Player",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        val navButtonModifier = if (hasFocusableContent && contentFocusRequester != null) {
            Modifier.focusProperties {
                right = contentFocusRequester
            }
        } else Modifier

        val homeButtonModifier = if (initialFocusRequester != null) {
            navButtonModifier.focusRequester(initialFocusRequester)
        } else navButtonModifier

        TvSideNavButton(
            label = "Início",
            icon = Icons.Default.Home,
            isSelected = selectedItem == TvSideNavItem.HOME,
            isExpanded = isRailFocused,
            onClick = { onSelectItem(TvSideNavItem.HOME) },
            modifier = homeButtonModifier
        )

        TvSideNavButton(
            label = "TV Online",
            icon = Icons.Default.Tv,
            isSelected = selectedItem == TvSideNavItem.LIVE,
            isExpanded = isRailFocused,
            onClick = { onSelectItem(TvSideNavItem.LIVE) },
            modifier = navButtonModifier
        )

        TvSideNavButton(
            label = "Filmes",
            icon = Icons.Default.Movie,
            isSelected = selectedItem == TvSideNavItem.VOD,
            isExpanded = isRailFocused,
            onClick = { onSelectItem(TvSideNavItem.VOD) },
            modifier = navButtonModifier
        )

        TvSideNavButton(
            label = "Séries",
            icon = Icons.Default.VideoLibrary,
            isSelected = selectedItem == TvSideNavItem.SERIES,
            isExpanded = isRailFocused,
            onClick = { onSelectItem(TvSideNavItem.SERIES) },
            modifier = navButtonModifier
        )

        Spacer(modifier = Modifier.weight(1f))

        TvSideNavButton(
            label = if (isRefreshing) "Atualizando..." else "Atualizar",
            icon = Icons.Default.Refresh,
            isSelected = false,
            isExpanded = isRailFocused,
            isLoading = isRefreshing,
            onClick = { onSelectItem(TvSideNavItem.REFRESH) },
            modifier = navButtonModifier
        )

        TvSideNavButton(
            label = "Configurações",
            icon = Icons.Default.Settings,
            isSelected = selectedItem == TvSideNavItem.SETTINGS,
            isExpanded = isRailFocused,
            onClick = { onSelectItem(TvSideNavItem.SETTINGS) },
            modifier = navButtonModifier
        )
    }
}

@Composable
private fun TvSideNavButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(AppCorners.md)
            .background(if (isSelected) TvPrimaryBlue else Color.Transparent)
            .tvFocusable(
                shape = AppCorners.md,
                focusBorderColor = Color.White,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = TvSecondaryBlue,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) Color.White else Color(0xFFA0B0C4),
                    modifier = Modifier.size(22.dp)
                )
            }
            if (isExpanded) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else Color(0xFFA0B0C4),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun WideHomeRecentCardItem(
    item: HomeRecentItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(125.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(TvCardBackground)
            .border(1.dp, TvSurfaceVariant, RoundedCornerShape(10.dp))
            .tvFocusable(
                shape = RoundedCornerShape(10.dp),
                focusBorderColor = TvFocusBorder,
                focusedScale = 1.05f,
                onClick = onClick
            )
            .padding(6.dp)
            .testTag("wide_home_recent_card_${item.contentType}_${item.contentId}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(165.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF142033)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = "Capa de ${item.title}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.placeholder_poster),
                error = painterResource(id = R.drawable.placeholder_poster)
            )

            val badgeLabel = if (item.contentType == "VOD") "FILME" else "SÉRIE"
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xDD0084FF))
                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = badgeLabel,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun WideHomeContinueCardItem(
    item: HomeContinueItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(125.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(TvCardBackground)
            .border(1.dp, TvSurfaceVariant, RoundedCornerShape(10.dp))
            .tvFocusable(
                shape = RoundedCornerShape(10.dp),
                focusBorderColor = TvFocusBorder,
                focusedScale = 1.05f,
                onClick = onClick
            )
            .padding(6.dp)
            .testTag("wide_home_continue_card_${item.progressKey}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(165.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF142033)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = "Capa de ${item.title}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.placeholder_poster),
                error = painterResource(id = R.drawable.placeholder_poster)
            )

            if (item.contentType == "SERIES" && item.seasonNum != null && item.episodeNum != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xCC050810))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "T${item.seasonNum} • E${item.episodeNum}",
                        color = TvSecondaryBlue,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { item.progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = TvSecondaryBlue,
            trackColor = Color(0x33FFFFFF)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp
        )
    }
}

