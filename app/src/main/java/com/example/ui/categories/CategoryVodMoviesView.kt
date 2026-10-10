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
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
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
import com.example.domain.model.VodMovie
import com.example.domain.model.VodStreamsState
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
 * Visualização dos filmes VOD reais pertencentes a uma categoria selecionada de Filmes.
 */
@Composable
fun CategoryVodMoviesView(
    category: XtreamCategory,
    vodState: VodStreamsState?,
    listTitle: String,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onMovieClick: (VodMovie) -> Unit,
    modifier: Modifier = Modifier
) {
    val backButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            backButtonFocusRequester.requestFocus()
        } catch (_: Exception) {
        }
    }

    val isLoading = vodState?.isLoading ?: true
    val movies = vodState?.movies ?: emptyList()
    val errorMessage = vodState?.errorMessage

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 32.dp, top = 14.dp, end = 32.dp, bottom = 16.dp)
    ) {
        // Barra Superior da Categoria de Filmes
        VodTopBar(
            categoryName = category.categoryName,
            movieCount = movies.size,
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
                isLoading && movies.isEmpty() -> {
                    VodLoadingView(categoryName = category.categoryName)
                }
                errorMessage != null && movies.isEmpty() -> {
                    VodErrorView(
                        errorMessage = errorMessage,
                        onRetry = onRefreshClick,
                        onBack = onBackClick
                    )
                }
                movies.isEmpty() -> {
                    VodEmptyView(
                        categoryName = category.categoryName,
                        onBack = onBackClick
                    )
                }
                else -> {
                    VodMoviesGridView(
                        movies = movies,
                        onMovieClick = onMovieClick
                    )
                }
            }
        }
    }
}

@Composable
private fun VodTopBar(
    categoryName: String,
    movieCount: Int,
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
        // Lado esquerdo: Botão Voltar + Nome da Categoria + Contagem
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
                    .testTag("btn_back_to_movie_categories"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar para categorias de filmes",
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
                    imageVector = Icons.Default.Movie,
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
                    text = if (isLoading) "Carregando filmes..." else "$movieCount filmes disponíveis",
                    color = TvTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Lado direito: Status do Servidor + Botão Atualizar
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
                    .testTag("btn_refresh_vod_movies"),
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
                            contentDescription = "Atualizar filmes",
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
private fun VodMoviesGridView(
    movies: List<VodMovie>,
    onMovieClick: (VodMovie) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 6.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(
            items = movies,
            key = { index, movie -> "${movie.streamId}_$index" }
        ) { _, movie ->
            VodMovieCardItem(
                movie = movie,
                onClick = { onMovieClick(movie) }
            )
        }
    }
}

@Composable
private fun VodMovieCardItem(
    movie: VodMovie,
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
            .testTag("vod_movie_card_${movie.streamId}")
    ) {
        // Poster do Filme
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF142033)),
            contentAlignment = Alignment.Center
        ) {
            if (!movie.streamIcon.isNullOrBlank()) {
                AsyncImage(
                    model = movie.streamIcon,
                    contentDescription = "Poster de ${movie.name}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = TvTextSecondary,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Badge de Formato (ex: MP4, MKV)
            val ext = movie.containerExtension?.uppercase() ?: "MP4"
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xCC0B121C))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = ext,
                    color = TvSecondaryBlue,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Badge de Avaliação (ex: ★ 7.8)
            if (!movie.rating.isNullOrBlank() && movie.rating != "0") {
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
                        text = movie.rating,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Título do Filme
        Text(
            text = movie.name,
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
private fun VodLoadingView(categoryName: String) {
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
            text = "Carregando filmes...",
            color = TvTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Buscando títulos da categoria '$categoryName'",
            color = TvTextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun VodErrorView(
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
            text = "Falha ao carregar filmes",
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
private fun VodEmptyView(
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
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = TvTextMuted,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Nenhum filme nesta categoria",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "A categoria '$categoryName' não possui títulos disponíveis no momento",
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
