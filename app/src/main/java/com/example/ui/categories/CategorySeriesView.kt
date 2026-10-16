package com.example.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.SeriesItem
import com.example.domain.model.SeriesState
import com.example.domain.model.XtreamCategory
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.components.SearchFilterBar
import com.example.ui.components.tvFocusable
import com.example.util.SearchUtils
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

/**
 * Visualização das séries com suporte a layout integrado responsivo:
 * - Telas Largas (TV 16:9 / Landscape): Coluna 1 (Categorias de Séries) | Coluna 2 (Catálogo de Séries com Capas).
 * - Telas Estreitas (Celular Portrait): Layout mobile aprovado mantido 100% intacto.
 */
@Composable
fun CategorySeriesView(
    category: XtreamCategory,
    allCategories: List<XtreamCategory> = emptyList(),
    onSelectCategory: ((XtreamCategory) -> Unit)? = null,
    seriesState: SeriesState?,
    listTitle: String,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onSeriesClick: (SeriesItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val backButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            backButtonFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    val isLoading = seriesState?.isLoading ?: true
    val seriesList = seriesState?.seriesList ?: emptyList()
    val errorMessage = seriesState?.errorMessage

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideLayout = this.maxWidth >= 660.dp

        if (isWideLayout && allCategories.isNotEmpty()) {
            WideIntegratedSeriesView(
                category = category,
                allCategories = allCategories,
                onSelectCategory = onSelectCategory,
                seriesList = seriesList,
                isLoading = isLoading,
                errorMessage = errorMessage,
                listTitle = listTitle,
                onBackClick = onBackClick,
                onRefreshClick = onRefreshClick,
                onSeriesClick = onSeriesClick,
                backFocusRequester = backButtonFocusRequester
            )
        } else {
            MobileSeriesView(
                categoryName = category.categoryName,
                seriesList = seriesList,
                isLoading = isLoading,
                errorMessage = errorMessage,
                listTitle = listTitle,
                onBackClick = onBackClick,
                onRefreshClick = onRefreshClick,
                onSeriesClick = onSeriesClick,
                backFocusRequester = backButtonFocusRequester
            )
        }
    }
}

/**
 * Tela Integrada de Séries para Telas Largas / TV 16:9 / Landscape:
 * Coluna 1: Categorias de Séries | Coluna 2: Catálogo de Séries com Capas.
 */
@Composable
private fun WideIntegratedSeriesView(
    category: XtreamCategory,
    allCategories: List<XtreamCategory>,
    onSelectCategory: ((XtreamCategory) -> Unit)?,
    seriesList: List<SeriesItem>,
    isLoading: Boolean,
    errorMessage: String?,
    listTitle: String,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onSeriesClick: (SeriesItem) -> Unit,
    backFocusRequester: FocusRequester
) {
    var categorySearchQuery by remember { mutableStateOf("") }
    var seriesSearchQuery by remember(category.categoryId) { mutableStateOf("") }

    val filteredCategories = remember(allCategories, categorySearchQuery) {
        if (categorySearchQuery.isBlank()) allCategories
        else allCategories.filter { SearchUtils.matchesSearchQuery(it.categoryName, categorySearchQuery) }
    }

    val filteredSeries = remember(seriesList, seriesSearchQuery) {
        if (seriesSearchQuery.isBlank()) seriesList
        else seriesList.filter { SearchUtils.matchesSearchQuery(it.name, seriesSearchQuery) }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = AppSpacing.xl, top = AppSpacing.md, end = AppSpacing.xl, bottom = AppSpacing.md)
    ) {
        // Cabeçalho compacto "SÉRIES" para otimizar espaço vertical
        SeriesIntegratedTopBar(
            categoryName = category.categoryName,
            seriesCount = seriesList.size,
            isLoading = isLoading,
            listTitle = listTitle,
            onBackClick = onBackClick,
            onRefreshClick = onRefreshClick,
            backFocusRequester = backFocusRequester
        )

        Spacer(modifier = Modifier.height(AppSpacing.md))

        // Estrutura em 2 Colunas Integradas: Categorias (Esq) | Catálogo de Capas (Dir)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            // COLUNA 1 — CATEGORIAS DE SÉRIES (220.dp)
            Box(
                modifier = Modifier
                    .width(220.dp)
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

                    SearchFilterBar(
                        query = categorySearchQuery,
                        onQueryChange = { categorySearchQuery = it },
                        placeholderText = "Filtrar..."
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredCategories, key = { it.categoryId }) { catItem ->
                            val isSelected = catItem.categoryId == category.categoryId
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
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

            // COLUNA 2 — CATÁLOGO DE SÉRIES COM CAPAS (Área Direita Dominante)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SearchFilterBar(
                        query = seriesSearchQuery,
                        onQueryChange = { seriesSearchQuery = it },
                        placeholderText = "Filtrar séries..."
                    )

                    when {
                        isLoading && seriesList.isEmpty() -> {
                            SeriesLoadingView(categoryName = category.categoryName)
                        }
                        errorMessage != null && seriesList.isEmpty() -> {
                            SeriesErrorView(
                                errorMessage = errorMessage,
                                onRetry = onRefreshClick,
                                onBack = onBackClick
                            )
                        }
                        filteredSeries.isEmpty() -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (seriesSearchQuery.isNotBlank()) stringResource(R.string.msg_no_results_found) else "Nenhuma série encontrada nesta categoria",
                                    color = TvTextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        else -> {
                            SeriesGridView(
                                seriesList = filteredSeries,
                                onSeriesClick = onSeriesClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesIntegratedTopBar(
    categoryName: String,
    seriesCount: Int,
    isLoading: Boolean,
    listTitle: String,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    backFocusRequester: FocusRequester
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
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
                    .background(TvSurface)
                    .border(1.dp, TvSurfaceVariant, AppCorners.md)
                    .tvFocusable(
                        shape = AppCorners.md,
                        focusBorderColor = TvFocusBorder,
                        focusedScale = 1.08f,
                        onClick = onBackClick
                    )
                    .focusRequester(backFocusRequester)
                    .testTag("btn_back_to_home"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar para Home",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = "SÉRIES",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )

            Text(
                text = "•",
                color = TvTextMuted,
                fontSize = 14.sp
            )

            Text(
                text = categoryName,
                color = TvSecondaryBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (isLoading) "(Carregando...)" else "($seriesCount títulos)",
                color = TvTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                    .clip(AppCorners.md)
                    .background(TvSurface)
                    .border(1.dp, TvSurfaceVariant, AppCorners.md)
                    .tvFocusable(
                        shape = AppCorners.md,
                        focusBorderColor = TvFocusBorder,
                        focusedScale = 1.05f,
                        onClick = onRefreshClick
                    )
                    .padding(horizontal = 12.dp)
                    .testTag("btn_refresh_series"),
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
                            contentDescription = "Atualizar séries",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "Atualizar",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Layout Mobile Aprovado (Preservado 100%).
 */
@Composable
private fun MobileSeriesView(
    categoryName: String,
    seriesList: List<SeriesItem>,
    isLoading: Boolean,
    errorMessage: String?,
    listTitle: String,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onSeriesClick: (SeriesItem) -> Unit,
    backFocusRequester: FocusRequester
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 32.dp, top = 14.dp, end = 32.dp, bottom = 16.dp)
    ) {
        SeriesTopBar(
            categoryName = categoryName,
            seriesCount = seriesList.size,
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
                isLoading && seriesList.isEmpty() -> {
                    SeriesLoadingView(categoryName = categoryName)
                }
                errorMessage != null && seriesList.isEmpty() -> {
                    SeriesErrorView(
                        errorMessage = errorMessage,
                        onRetry = onRefreshClick,
                        onBack = onBackClick
                    )
                }
                seriesList.isEmpty() -> {
                    SeriesEmptyView(
                        categoryName = categoryName,
                        onBack = onBackClick
                    )
                }
                else -> {
                    SeriesGridView(
                        seriesList = seriesList,
                        onSeriesClick = onSeriesClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SeriesTopBar(
    categoryName: String,
    seriesCount: Int,
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
                    .testTag("btn_back_to_series_categories"),
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
                    imageVector = Icons.Default.Tv,
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
                    text = if (isLoading) "Carregando séries..." else "$seriesCount séries disponíveis",
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
                    .testTag("btn_refresh_series"),
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
                            contentDescription = "Atualizar séries",
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
private fun SeriesGridView(
    seriesList: List<SeriesItem>,
    onSeriesClick: (SeriesItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 6.dp, bottom = 70.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(
            items = seriesList,
            key = { index, series -> "${series.seriesId}_$index" }
        ) { _, series ->
            SeriesCardItem(
                series = series,
                onClick = { onSeriesClick(series) }
            )
        }
    }
}

@Composable
private fun SeriesCardItem(
    series: SeriesItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TvCardBackground)
            .border(1.dp, TvSurfaceVariant, RoundedCornerShape(12.dp))
            .tvFocusable(
                shape = RoundedCornerShape(12.dp),
                focusBorderColor = TvFocusBorder,
                focusedScale = 1.05f,
                onClick = onClick
            )
            .padding(8.dp)
            .testTag("series_card_${series.seriesId}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF142033)),
            contentAlignment = Alignment.Center
        ) {
            if (!series.cover.isNullOrBlank()) {
                AsyncImage(
                    model = series.cover,
                    contentDescription = "Capa da série ${series.name}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = TvTextSecondary,
                    modifier = Modifier.size(36.dp)
                )
            }

            if (series.rating != null && series.rating != "0" && series.rating.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xDD0F1826))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = series.rating,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = series.name,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SeriesLoadingView(categoryName: String) {
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
                text = "Buscando séries de $categoryName...",
                color = TvTextSecondary,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun SeriesErrorView(
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
private fun SeriesEmptyView(
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
                text = "Nenhuma série encontrada nesta categoria",
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
