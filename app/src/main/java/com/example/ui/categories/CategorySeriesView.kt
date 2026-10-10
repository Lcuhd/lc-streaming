package com.example.ui.categories

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.example.ui.components.tvFocusable
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
 * Visualização das séries reais pertencentes a uma categoria selecionada de Séries.
 */
@Composable
fun CategorySeriesView(
    category: XtreamCategory,
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
        } catch (_: Exception) {
        }
    }

    val isLoading = seriesState?.isLoading ?: true
    val seriesList = seriesState?.seriesList ?: emptyList()
    val errorMessage = seriesState?.errorMessage

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 32.dp, top = 14.dp, end = 32.dp, bottom = 16.dp)
    ) {
        // Barra Superior da Categoria de Séries
        SeriesTopBar(
            categoryName = category.categoryName,
            seriesCount = seriesList.size,
            isLoading = isLoading,
            listTitle = listTitle,
            onBackClick = onBackClick,
            onRefreshClick = onRefreshClick,
            backFocusRequester = backButtonFocusRequester
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Conteúdo Principal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
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
                seriesList.isEmpty() -> {
                    SeriesEmptyView(
                        categoryName = category.categoryName,
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
                    contentDescription = "Voltar para categorias de séries",
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
        contentPadding = PaddingValues(top = 6.dp, bottom = 28.dp),
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
        // Capa / Poster da Série
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
                    contentDescription = "Capa de ${series.name}",
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

            // Badge SÉRIE
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xCC0B121C))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "SÉRIE",
                    color = TvSecondaryBlue,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Rating Badge
            if (!series.rating.isNullOrBlank() && series.rating != "0") {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xCC0B121C))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = series.rating,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Nome da Série
        Text(
            text = series.name,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun SeriesLoadingView(categoryName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = TvPrimaryBlue,
            strokeWidth = 3.dp,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Carregando séries...",
            color = TvTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Buscando catálogo da categoria '$categoryName'",
            color = TvTextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SeriesErrorView(
    errorMessage: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0x1AFF5252)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Falha ao carregar séries",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = errorMessage,
            color = TvTextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TvPrimaryBlue)
                    .tvFocusable(
                        shape = RoundedCornerShape(10.dp),
                        focusBorderColor = Color.White,
                        onClick = onRetry
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tentar novamente",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TvSurface)
                    .border(1.dp, TvSurfaceVariant, RoundedCornerShape(10.dp))
                    .tvFocusable(
                        shape = RoundedCornerShape(10.dp),
                        focusBorderColor = TvFocusBorder,
                        onClick = onBack
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

@Composable
private fun SeriesEmptyView(
    categoryName: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(TvSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = TvTextMuted,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Nenhuma série nesta categoria",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "A categoria '$categoryName' não possui séries disponíveis no momento",
            color = TvTextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(TvSurface)
                .border(1.dp, TvSurfaceVariant, RoundedCornerShape(10.dp))
                .tvFocusable(
                    shape = RoundedCornerShape(10.dp),
                    focusBorderColor = TvFocusBorder,
                    onClick = onBack
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Voltar para categorias",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
