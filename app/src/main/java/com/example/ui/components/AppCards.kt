package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.SeriesProgressEntity
import com.example.domain.model.DeviceListModel
import com.example.domain.model.EpisodeItem
import com.example.domain.model.XtreamCategory
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
 * Card de conteúdo padrão e responsivo para Canais, Filmes e Séries.
 * Adapta-se automaticamente a diferentes larguras de grade (Mobile Portrait vs TV Landscape 16:9).
 */
@Composable
fun AppContentCard(
    title: String,
    coverUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rating: String? = null,
    badgeText: String? = null,
    aspectRatio: Float = 0.72f,
    shape: Shape = AppCorners.md
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(TvCardBackground)
            .border(1.dp, TvSurfaceVariant, shape)
            .tvFocusable(
                shape = shape,
                focusBorderColor = TvFocusBorder,
                focusedScale = 1.05f,
                onClick = onClick
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clip(shape)
                    .background(Color(0xFF101826)),
                contentAlignment = Alignment.Center
            ) {
                if (!coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = "Capa de $title",
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

                // Badge opcional (ex: "AO VIVO", "HD", "S1E3")
                if (!badgeText.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(AppSpacing.xs)
                            .clip(AppCorners.xs)
                            .background(Color(0xDD0B121C))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = TvSecondaryBlue,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.sm)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!rating.isNullOrBlank() && rating != "0") {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
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
                            text = rating,
                            color = TvTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Chip seletor de Categoria.
 */
@Composable
fun AppCategoryChip(
    category: XtreamCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 38.dp,
    shape: Shape = AppCorners.pill
) {
    val bgColor = if (isSelected) TvPrimaryBlue else TvSurface
    val borderColor = if (isSelected) Color.White else TvSurfaceVariant

    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .tvFocusable(
                shape = shape,
                focusBorderColor = TvFocusBorder,
                focusedScale = 1.06f,
                onClick = onClick
            )
            .padding(horizontal = AppSpacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = category.categoryName,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * Card de Lista de Servidor do Dispositivo.
 */
@Composable
fun AppListCard(
    list: DeviceListModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = AppCorners.lg
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(TvCardBackground)
            .border(1.dp, TvSurfaceVariant, shape)
            .tvFocusable(
                shape = shape,
                focusBorderColor = TvFocusBorder,
                focusedScale = 1.03f,
                onClick = onClick
            )
            .padding(AppSpacing.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = list.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Fonte: ${list.sourceName}",
                    color = TvTextSecondary,
                    fontSize = 12.sp
                )
                if (list.formattedExpiration.isNotBlank()) {
                    Text(
                        text = "Validade: ${list.formattedExpiration}",
                        color = TvTextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            AppStatusBadge(status = list.status)
        }
    }
}

/**
 * Card de Episódio de Série com progresso de reprodução.
 */
@Composable
fun AppEpisodeCard(
    episode: EpisodeItem,
    progress: SeriesProgressEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = AppCorners.md
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(shape)
            .background(TvCardBackground)
            .border(1.dp, TvSurfaceVariant, shape)
            .tvFocusable(
                shape = shape,
                focusBorderColor = TvFocusBorder,
                focusedScale = 1.03f,
                onClick = onClick
            )
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp, 60.dp)
                    .clip(AppCorners.sm)
                    .background(Color(0xFF142033)),
                contentAlignment = Alignment.Center
            ) {
                if (!episode.image.isNullOrBlank()) {
                    AsyncImage(
                        model = episode.image,
                        contentDescription = "Thumbnail do episódio",
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
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
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

                    if (progress?.completed == true) {
                        Box(
                            modifier = Modifier
                                .clip(AppCorners.xs)
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
                                .clip(AppCorners.xs)
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
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = episode.plot ?: "Sem sinopse disponível.",
                    color = TvTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
