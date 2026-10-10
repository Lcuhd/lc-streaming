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
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.SeriesProgressEntity
import com.example.domain.model.EpisodeItem
import com.example.domain.model.SeasonItem
import com.example.domain.model.SeriesButtonState
import com.example.domain.model.SeriesDetailsState
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
 * Visualização dos detalhes de uma série, seleção de temporadas e listagem de episódios.
 */
@Composable
fun SeriesDetailsView(
    detailsState: SeriesDetailsState?,
    onBackClick: () -> Unit,
    onPrimaryButtonClick: () -> Unit,
    onSelectSeason: (seasonNum: Int) -> Unit,
    onEpisodeClick: (EpisodeItem) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            backButtonFocusRequester.requestFocus()
        } catch (_: Exception) {
        }
    }

    val isLoading = detailsState?.isLoading ?: true
    val seriesItem = detailsState?.seriesItem
    val details = detailsState?.details
    val errorMessage = detailsState?.errorMessage

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 32.dp, top = 14.dp, end = 32.dp, bottom = 16.dp)
    ) {
        // Barra Superior do Detalhe da Série
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                    .focusRequester(backButtonFocusRequester)
                    .testTag("btn_back_to_series_grid"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar para séries",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = details?.title ?: seriesItem?.name ?: "Detalhes da Série",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                    // Header com Poster + Metadados + Botão Principal "Iniciar / Continuar" + Sinopse
                    SeriesHeaderCard(
                        cover = details.cover ?: seriesItem?.cover,
                        title = details.title,
                        rating = details.rating ?: seriesItem?.rating,
                        genre = details.genre ?: seriesItem?.genre,
                        releaseDate = details.releaseDate ?: seriesItem?.releaseDate,
                        plot = details.plot ?: seriesItem?.plot,
                        buttonState = detailsState.primaryButtonState,
                        onPrimaryButtonClick = onPrimaryButtonClick
                    )

                    Spacer(modifier = Modifier.height(14.dp))

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

                        Spacer(modifier = Modifier.height(14.dp))
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

@Composable
private fun SeriesHeaderCard(
    cover: String?,
    title: String,
    rating: String?,
    genre: String?,
    releaseDate: String?,
    plot: String?,
    buttonState: SeriesButtonState,
    onPrimaryButtonClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TvCardBackground)
            .border(1.dp, TvSurfaceVariant, RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Poster da Série
        Box(
            modifier = Modifier
                .width(90.dp)
                .height(130.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF142033)),
            contentAlignment = Alignment.Center
        ) {
            if (!cover.isNullOrBlank()) {
                AsyncImage(
                    model = cover,
                    contentDescription = "Poster de $title",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = TvTextSecondary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Metadados + Botão Principal + Sinopse
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

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
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = rating,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (!genre.isNullOrBlank()) {
                            Text(
                                text = "• $genre",
                                color = TvSecondaryBlue,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!releaseDate.isNullOrBlank()) {
                            Text(
                                text = "• $releaseDate",
                                color = TvTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Botão Principal "Iniciar série" / "Continuar" / "Assistir novamente"
                SeriesPrimaryActionButton(
                    buttonState = buttonState,
                    onClick = onPrimaryButtonClick
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = plot ?: "Sem descrição disponível.",
                color = TvTextSecondary,
                fontSize = 12.sp,
                maxLines = 3,
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
            .clip(RoundedCornerShape(10.dp))
            .background(TvPrimaryBlue)
            .tvFocusable(
                shape = RoundedCornerShape(10.dp),
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
    val bgColor = if (isSelected) TvPrimaryBlue else TvSurface
    val borderColor = if (isSelected) Color.White else TvSurfaceVariant

    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .tvFocusable(
                shape = RoundedCornerShape(10.dp),
                focusBorderColor = TvFocusBorder,
                focusedScale = 1.06f,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${season.name} (${season.episodeCount})",
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
            .height(84.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(TvCardBackground)
            .border(1.dp, TvSurfaceVariant, RoundedCornerShape(10.dp))
            .tvFocusable(
                shape = RoundedCornerShape(10.dp),
                focusBorderColor = TvFocusBorder,
                focusedScale = 1.03f,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("episode_card_${episode.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Thumbnail / Ícone do Episódio
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF142033)),
                contentAlignment = Alignment.Center
            ) {
                if (!episode.image.isNullOrBlank()) {
                    AsyncImage(
                        model = episode.image,
                        contentDescription = "Imagem do episódio ${episode.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = TvSecondaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Extensão
                val ext = episode.containerExtension?.uppercase() ?: "MKV"
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xCC0B121C))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = ext,
                        color = TvSecondaryBlue,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Título, Duração, Progresso e Sinopse do Episódio
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "E${episode.episodeNum} • ${episode.title}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Badge de Progresso (✓ Concluído ou % assistido)
                    if (progress?.completed == true) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x1A00C853))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "✓ Concluído",
                                color = StatusActiveText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (progress != null && progress.positionMs > 0 && progress.durationMs > 0) {
                        val pct = (progress.positionMs * 100 / progress.durationMs).coerceIn(1, 99)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x1A0084FF))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "$pct% assistido",
                                color = TvSecondaryBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!episode.duration.isNullOrBlank()) {
                        Text(
                            text = episode.duration,
                            color = TvTextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = episode.plot ?: "Sem sinopse disponível para este episódio.",
                    color = TvTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun DetailsLoadingView(seriesName: String) {
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
            text = "Carregando temporadas e episódios...",
            color = TvTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Buscando detalhes da série '$seriesName'",
            color = TvTextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DetailsErrorView(
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
            text = "Falha ao carregar detalhes da série",
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
