package com.example.ui.categories

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.entity.SeriesProgressEntity
import com.example.domain.model.EpisodeItem
import com.example.domain.model.SeasonItem
import com.example.domain.model.SeriesButtonState
import com.example.domain.model.SeriesDetailsState
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

/**
 * Visualização dos detalhes de uma série com Hero Cinematográfico responsivo,
 * ações principais ("Iniciar série" / "Continuar"), Favoritar, seleção de temporadas e episódios.
 */
@Composable
fun SeriesDetailsView(
    detailsState: SeriesDetailsState?,
    onBackClick: () -> Unit,
    onPrimaryButtonClick: () -> Unit,
    onSelectSeason: (seasonNum: Int) -> Unit,
    onEpisodeClick: (EpisodeItem) -> Unit,
    onRetryClick: () -> Unit,
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

    val isLoading = detailsState?.isLoading ?: true
    val seriesItem = detailsState?.seriesItem
    val details = detailsState?.details
    val errorMessage = detailsState?.errorMessage

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Color(0xFF050810))) {
        val isWideLayout = this.maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = AppSpacing.xl, top = AppSpacing.md, end = AppSpacing.xl, bottom = AppSpacing.md)
        ) {
            when {
                isLoading && details == null -> {
                    DetailsLoadingView(seriesName = seriesItem?.name ?: "Série")
                }
                errorMessage != null && details == null -> {
                    DetailsErrorView(
                        errorMessage = errorMessage,
                        onRetry = onRetryClick,
                        onBack = onBackClick
                    )
                }
                details != null -> {
                    val seasons = details.seasons
                    val selectedSeasonNum = detailsState?.selectedSeasonNum ?: seasons.firstOrNull()?.seasonNumber ?: 1
                    val episodes = details.episodesBySeason[selectedSeasonNum] ?: emptyList()
                    val progressMap = detailsState?.savedProgressList?.associateBy { it.episodeId } ?: emptyMap()

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Hero Cinematográfico com Backdrop + Degradê + Metadados + Botões (Iniciar/Continuar + Favoritar)
                        SeriesHeroHeaderCard(
                            seriesId = seriesItem?.seriesId.orEmpty(),
                            cover = details.cover ?: seriesItem?.backdropPath?.firstOrNull() ?: seriesItem?.cover,
                            title = details.title,
                            rating = details.rating ?: seriesItem?.rating,
                            genre = details.genre ?: seriesItem?.genre,
                            releaseDate = details.releaseDate ?: seriesItem?.releaseDate,
                            plot = details.plot ?: seriesItem?.plot,
                            seasonsCount = seasons.size,
                            buttonState = detailsState.primaryButtonState,
                            isWideLayout = isWideLayout,
                            onBackClick = onBackClick,
                            onPrimaryButtonClick = onPrimaryButtonClick,
                            onIsFavorite = onIsFavorite,
                            onToggleFavorite = onToggleFavorite,
                            backButtonFocusRequester = backButtonFocusRequester
                        )

                        Spacer(modifier = Modifier.height(AppSpacing.md))

                        // Seletor de Temporadas
                        if (seasons.isNotEmpty()) {
                            Text(
                                text = "Temporadas",
                                color = TvTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(seasons) { season ->
                                    SeasonChipItem(
                                        season = season,
                                        isSelected = season.seasonNumber == selectedSeasonNum,
                                        onClick = { onSelectSeason(season.seasonNumber) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(AppSpacing.md))
                        }

                        // Lista de Episódios da Temporada Selecionada
                        Text(
                            text = "Episódios (${episodes.size})",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (episodes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nenhum episódio encontrado para esta temporada",
                                    color = TvTextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 280.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(
                                    items = episodes,
                                    key = { index, ep -> "${ep.id}_$index" }
                                ) { _, episode ->
                                    EpisodeCardItem(
                                        episode = episode,
                                        progress = progressMap[episode.id],
                                        onClick = { onEpisodeClick(episode) }
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

/**
 * Hero Cinematográfico de Séries (Backdrop + Degradê Amplo + Metadados + Iniciar/Continuar + Favoritar + Sinopse)
 */
@Composable
private fun SeriesHeroHeaderCard(
    seriesId: String,
    cover: String?,
    title: String,
    rating: String?,
    genre: String?,
    releaseDate: String?,
    plot: String?,
    seasonsCount: Int,
    buttonState: SeriesButtonState,
    isWideLayout: Boolean,
    onBackClick: () -> Unit,
    onPrimaryButtonClick: () -> Unit,
    onIsFavorite: (suspend (String) -> Boolean)? = null,
    onToggleFavorite: (suspend (String) -> Boolean)? = null,
    backButtonFocusRequester: FocusRequester
) {
    var isFavorite by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(seriesId) {
        if (onIsFavorite != null && seriesId.isNotBlank()) {
            try {
                isFavorite = onIsFavorite(seriesId)
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isWideLayout) 260.dp else 230.dp)
            .clip(AppCorners.lg)
            .background(Color(0xFF080D1A))
            .border(1.dp, TvSurfaceVariant, AppCorners.lg)
    ) {
        // Backdrop Image
        if (!cover.isNullOrBlank()) {
            AsyncImage(
                model = cover,
                contentDescription = "Hero de $title",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.placeholder_poster),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Overlay de Degradê Cinematográfico
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x33050810),
                            Color(0x88050810),
                            Color(0xFE050810)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xF0050810),
                            Color(0x77050810),
                            Color.Transparent
                        )
                    )
                )
        )

        // Botão Voltar no canto superior esquerdo
        Box(
            modifier = Modifier
                .padding(AppSpacing.md)
                .size(38.dp)
                .clip(AppCorners.md)
                .background(Color(0x88000000))
                .border(1.dp, Color(0x33FFFFFF), AppCorners.md)
                .tvFocusable(
                    shape = AppCorners.md,
                    focusBorderColor = Color.White,
                    onClick = onBackClick
                )
                .focusRequester(backButtonFocusRequester)
                .testTag("btn_back_to_series_grid"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Voltar",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        // Conteúdo Hero (Título, Metadados, Ações, Sinopse)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppSpacing.lg),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = if (isWideLayout) 24.sp else 18.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Metadados
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!rating.isNullOrBlank() && rating != "0") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = rating,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (seasonsCount > 0) {
                    Text(text = "• $seasonsCount Temp.", color = TvSecondaryBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                if (!genre.isNullOrBlank()) {
                    Text(text = "• $genre", color = TvTextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                if (!releaseDate.isNullOrBlank()) {
                    Text(text = "• $releaseDate", color = TvTextMuted, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botões de Ação: [ Iniciar / Continuar ] + [ Favoritar ♥ ]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SeriesPrimaryActionButton(
                    buttonState = buttonState,
                    onClick = onPrimaryButtonClick
                )

                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(AppCorners.md)
                        .background(if (isFavorite) Color(0x33FF3D71) else TvSurface)
                        .border(1.dp, if (isFavorite) Color(0xFFFF3D71) else TvSurfaceVariant, AppCorners.md)
                        .tvFocusable(
                            shape = AppCorners.md,
                            focusBorderColor = Color.White,
                            onClick = {
                                coroutineScope.launch {
                                    if (onToggleFavorite != null && seriesId.isNotBlank()) {
                                        try {
                                            isFavorite = onToggleFavorite(seriesId)
                                        } catch (_: Exception) {}
                                    } else {
                                        isFavorite = !isFavorite
                                    }
                                }
                            }
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(R.string.btn_favorite),
                            tint = if (isFavorite) Color(0xFFFF3D71) else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isFavorite) stringResource(R.string.btn_favorited) else stringResource(R.string.btn_favorite),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = plot ?: "Sem descrição disponível.",
                color = TvTextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun SeriesPrimaryActionButton(
    buttonState: SeriesButtonState,
    onClick: () -> Unit
) {
    val (label, icon, sublabel) = when (buttonState) {
        is SeriesButtonState.StartSeries -> Triple(
            "Iniciar série",
            Icons.Default.PlayArrow,
            "Temp. ${buttonState.firstEpisode.seasonNum} • Ep. ${buttonState.firstEpisode.episodeNum}"
        )
        is SeriesButtonState.ContinueSeries -> Triple(
            "Continuar • Temp. ${buttonState.targetSeasonNum} - Ep. ${buttonState.targetEpisodeNum}",
            Icons.Default.PlayArrow,
            buttonState.targetEpisode.title
        )
        is SeriesButtonState.RestartSeries -> Triple(
            "Assistir novamente",
            Icons.Default.Refresh,
            "Recomeçar da 1ª temporada"
        )
        is SeriesButtonState.NoEpisodesAvailable -> Triple(
            "Sem episódios disponíveis",
            Icons.Default.PlayArrow,
            ""
        )
    }

    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(AppCorners.md)
            .background(TvPrimaryBlue)
            .tvFocusable(
                shape = AppCorners.md,
                focusBorderColor = Color.White,
                focusedScale = 1.05f,
                onClick = if (buttonState !is SeriesButtonState.NoEpisodesAvailable) onClick else null
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (sublabel.isNotBlank()) {
                    Text(
                        text = sublabel,
                        color = Color(0xDDFFFFFF),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SeasonChipItem(
    season: SeasonItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(38.dp)
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
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = season.name.ifBlank { "Temporada ${season.seasonNumber}" },
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun EpisodeCardItem(
    episode: EpisodeItem,
    progress: SeriesProgressEntity?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppCorners.md)
            .background(TvCardBackground)
            .border(1.dp, TvSurfaceVariant, AppCorners.md)
            .tvFocusable(
                shape = AppCorners.md,
                focusBorderColor = TvFocusBorder,
                focusedScale = 1.03f,
                onClick = onClick
            )
            .padding(10.dp)
            .testTag("episode_card_${episode.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(48.dp)
                    .clip(AppCorners.sm)
                    .background(Color(0xFF142033)),
                contentAlignment = Alignment.Center
            ) {
                if (!episode.image.isNullOrBlank()) {
                    AsyncImage(
                        model = episode.image,
                        contentDescription = "Episódio ${episode.episodeNum}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = TvTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (progress != null && progress.durationMs > 0) {
                    val progressFraction = (progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color(0x66000000))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressFraction)
                                .background(TvSecondaryBlue)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ep. ${episode.episodeNum} • ${episode.title}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = episode.duration?.let { "Duração: $it min" } ?: "Pronto para assistir",
                    color = TvTextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun DetailsLoadingView(seriesName: String) {
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
                text = "Carregando detalhes de $seriesName...",
                color = TvTextSecondary,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun DetailsErrorView(
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
                        .clip(AppCorners.md)
                        .background(TvPrimaryBlue)
                        .tvFocusable(
                            shape = AppCorners.md,
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
                        .clip(AppCorners.md)
                        .background(TvSurface)
                        .border(1.dp, TvSurfaceVariant, AppCorners.md)
                        .tvFocusable(
                            shape = AppCorners.md,
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
