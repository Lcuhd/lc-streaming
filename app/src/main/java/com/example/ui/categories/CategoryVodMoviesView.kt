package com.example.ui.categories

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.entity.SeriesProgressEntity
import com.example.domain.model.VodMovie
import com.example.domain.model.VodStreamsState
import com.example.domain.model.XtreamCategory
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
 * Visualização dos filmes VOD com tela de detalhes cinematográfica (Hero Backdrop + Assistir + Favoritar)
 * e suporte a layout integrado responsivo.
 */
@Composable
fun CategoryVodMoviesView(
    category: XtreamCategory,
    allCategories: List<XtreamCategory> = emptyList(),
    onSelectCategory: ((XtreamCategory) -> Unit)? = null,
    vodState: VodStreamsState?,
    listTitle: String,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onMovieClick: (VodMovie) -> Unit,
    onGetMovieProgress: (suspend (VodMovie) -> SeriesProgressEntity?)? = null,
    onIsFavorite: (suspend (String) -> Boolean)? = null,
    onToggleFavorite: (suspend (String) -> Boolean)? = null,
    completedMovieIds: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    val backButtonFocusRequester = remember { FocusRequester() }
    var selectedMovieForDetails by remember { mutableStateOf<VodMovie?>(null) }

    BackHandler(enabled = selectedMovieForDetails != null) {
        selectedMovieForDetails = null
    }

    LaunchedEffect(Unit) {
        try {
            backButtonFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    val isLoading = vodState?.isLoading ?: true
    val movies = vodState?.movies ?: emptyList()
    val errorMessage = vodState?.errorMessage

    if (selectedMovieForDetails != null) {
        MovieDetailsView(
            movie = selectedMovieForDetails!!,
            onBackClick = { selectedMovieForDetails = null },
            onPlayMovie = { movie ->
                selectedMovieForDetails = null
                onMovieClick(movie)
            },
            onGetMovieProgress = onGetMovieProgress,
            onIsFavorite = onIsFavorite,
            onToggleFavorite = onToggleFavorite
        )
    } else {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val isWideLayout = this.maxWidth >= 660.dp

            if (isWideLayout && allCategories.isNotEmpty()) {
                WideIntegratedVodMoviesView(
                    category = category,
                    allCategories = allCategories,
                    onSelectCategory = onSelectCategory,
                    movies = movies,
                    completedMovieIds = completedMovieIds,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    listTitle = listTitle,
                    onBackClick = onBackClick,
                    onRefreshClick = onRefreshClick,
                    onMovieClick = { movie -> selectedMovieForDetails = movie },
                    backFocusRequester = backButtonFocusRequester
                )
            } else {
                MobileVodMoviesView(
                    categoryName = category.categoryName,
                    movies = movies,
                    completedMovieIds = completedMovieIds,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    listTitle = listTitle,
                    onBackClick = onBackClick,
                    onRefreshClick = onRefreshClick,
                    onMovieClick = { movie -> selectedMovieForDetails = movie },
                    backFocusRequester = backButtonFocusRequester
                )
            }
        }
    }
}

/**
 * Tela de Detalhes Cinematográfica do Filme (Hero Backdrop + Assistir + Favoritar + Sinopse)
 */
@Composable
fun MovieDetailsView(
    movie: VodMovie,
    onBackClick: () -> Unit,
    onPlayMovie: (VodMovie) -> Unit,
    onGetMovieProgress: (suspend (VodMovie) -> SeriesProgressEntity?)? = null,
    onIsFavorite: (suspend (String) -> Boolean)? = null,
    onToggleFavorite: (suspend (String) -> Boolean)? = null,
    modifier: Modifier = Modifier
) {
    var isFavorite by remember { mutableStateOf(false) }
    var movieProgress by remember { mutableStateOf<SeriesProgressEntity?>(null) }
    val backButtonFocusRequester = remember { FocusRequester() }
    val playButtonFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(movie.streamId) {
        if (onGetMovieProgress != null) {
            try {
                movieProgress = onGetMovieProgress(movie)
            } catch (_: Exception) {}
        }
        if (onIsFavorite != null) {
            try {
                isFavorite = onIsFavorite(movie.streamId)
            } catch (_: Exception) {}
        }
    }

    val isResumeAvailable = movieProgress != null &&
                            !movieProgress!!.completed &&
                            movieProgress!!.positionMs > 0 &&
                            movieProgress!!.positionMs < 0.92 * movieProgress!!.durationMs

    val watchButtonLabel = if (isResumeAvailable) {
        stringResource(R.string.btn_continue)
    } else {
        stringResource(R.string.btn_watch)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Color(0xFF050810))) {
        val isWideLayout = this.maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = AppSpacing.xl, top = AppSpacing.md, end = AppSpacing.xl, bottom = AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            // Hero Backdrop Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isWideLayout) 280.dp else 230.dp)
                    .clip(AppCorners.lg)
                    .background(Color(0xFF080D1A))
                    .border(1.dp, TvSurfaceVariant, AppCorners.lg)
            ) {
                if (!movie.streamIcon.isNullOrBlank()) {
                    AsyncImage(
                        model = movie.streamIcon,
                        contentDescription = "Backdrop de ${movie.name}",
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

                // Botão Voltar sobreposto no canto superior esquerdo
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
                        .focusRequester(backButtonFocusRequester),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Conteúdo Hero (Título, Nota, Ações e Sinopse)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppSpacing.lg),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = movie.name,
                        color = Color.White,
                        fontSize = if (isWideLayout) 26.sp else 20.sp,
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
                        if (!movie.rating.isNullOrBlank() && movie.rating != "0") {
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
                                    text = movie.rating,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(AppCorners.xs)
                                .background(Color(0x330084FF))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = (movie.containerExtension ?: "MP4").uppercase(),
                                color = TvSecondaryBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botões de Ação: [ Assistir ] + [ Favoritar ♥ ]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Botão Principal "Assistir"
                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(AppCorners.md)
                                .background(TvPrimaryBlue)
                                .tvFocusable(
                                    shape = AppCorners.md,
                                    focusBorderColor = Color.White,
                                    focusedScale = 1.05f,
                                    onClick = { onPlayMovie(movie) }
                                )
                                .focusRequester(playButtonFocusRequester)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = watchButtonLabel,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Botão Secundário "Favoritar"
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
                                            if (onToggleFavorite != null) {
                                                try {
                                                    isFavorite = onToggleFavorite(movie.streamId)
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
                }
            }

            // Bloco de Sinopse
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppCorners.lg)
                    .background(TvCardBackground)
                    .border(1.dp, TvSurfaceVariant, AppCorners.lg)
                    .padding(AppSpacing.lg)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.lbl_synopsis),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Disponível em alta definição no LC Player. Clique em Assistir para iniciar a reprodução imediata do filme.",
                        color = TvTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * Tela Integrada de Filmes para Telas Largas / TV 16:9 / Landscape:
 * Coluna 1: Categorias de Filmes | Coluna 2: Catálogo de Filmes com Capas.
 */
@Composable
private fun WideIntegratedVodMoviesView(
    category: XtreamCategory,
    allCategories: List<XtreamCategory>,
    onSelectCategory: ((XtreamCategory) -> Unit)?,
    movies: List<VodMovie>,
    completedMovieIds: Set<String> = emptySet(),
    isLoading: Boolean,
    errorMessage: String?,
    listTitle: String,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onMovieClick: (VodMovie) -> Unit,
    backFocusRequester: FocusRequester
) {
    var categorySearchQuery by remember { mutableStateOf("") }
    var movieSearchQuery by remember(category.categoryId) { mutableStateOf("") }

    val filteredCategories = remember(allCategories, categorySearchQuery) {
        if (categorySearchQuery.isBlank()) allCategories
        else allCategories.filter { SearchUtils.matchesSearchQuery(it.categoryName, categorySearchQuery) }
    }

    val filteredMovies = remember(movies, movieSearchQuery) {
        if (movieSearchQuery.isBlank()) movies
        else movies.filter { SearchUtils.matchesSearchQuery(it.name, movieSearchQuery) }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = AppSpacing.xl, top = AppSpacing.md, end = AppSpacing.xl, bottom = AppSpacing.md)
    ) {
        VodIntegratedTopBar(
            categoryName = category.categoryName,
            movieCount = movies.size,
            isLoading = isLoading,
            listTitle = listTitle,
            onBackClick = onBackClick,
            onRefreshClick = onRefreshClick,
            backFocusRequester = backFocusRequester
        )

        Spacer(modifier = Modifier.height(AppSpacing.md))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
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

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SearchFilterBar(
                        query = movieSearchQuery,
                        onQueryChange = { movieSearchQuery = it },
                        placeholderText = "Filtrar filmes..."
                    )

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
                        filteredMovies.isEmpty() -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (movieSearchQuery.isNotBlank()) stringResource(R.string.msg_no_results_found) else "Nenhum filme encontrado",
                                    color = TvTextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        else -> {
                            VodMoviesGridView(
                                movies = filteredMovies,
                                completedMovieIds = completedMovieIds,
                                onMovieClick = onMovieClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VodIntegratedTopBar(
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
                text = "FILMES",
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
                text = if (isLoading) "(Carregando...)" else "($movieCount títulos)",
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileVodMoviesView(
    categoryName: String,
    movies: List<VodMovie>,
    completedMovieIds: Set<String> = emptySet(),
    isLoading: Boolean,
    errorMessage: String?,
    listTitle: String,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onMovieClick: (VodMovie) -> Unit,
    backFocusRequester: FocusRequester
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 32.dp, top = 14.dp, end = 32.dp, bottom = 16.dp)
    ) {
        VodTopBar(
            categoryName = categoryName,
            movieCount = movies.size,
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
                isLoading && movies.isEmpty() -> {
                    VodLoadingView(categoryName = categoryName)
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
                        categoryName = categoryName,
                        onBack = onBackClick
                    )
                }
                else -> {
                    VodMoviesGridView(
                        movies = movies,
                        completedMovieIds = completedMovieIds,
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
    completedMovieIds: Set<String> = emptySet(),
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
                isCompleted = movie.streamId in completedMovieIds,
                onClick = { onMovieClick(movie) }
            )
        }
    }
}

@Composable
private fun VodMovieCardItem(
    movie: VodMovie,
    isCompleted: Boolean = false,
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
                    contentDescription = "Capa do filme ${movie.name}",
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

            // Indicador discreto de Conteúdo Concluído (✓ Verde) no canto superior esquerdo
            if (isCompleted) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xDD00C853))
                        .border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Assistido",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (movie.rating != null && movie.rating != "0" && movie.rating.isNotBlank()) {
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
                            text = movie.rating,
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
            text = movie.name,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VodLoadingView(categoryName: String) {
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
                text = "Buscando filmes de $categoryName...",
                color = TvTextSecondary,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun VodErrorView(
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
private fun VodEmptyView(
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
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = TvTextMuted,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "Nenhum filme encontrado nesta categoria",
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
