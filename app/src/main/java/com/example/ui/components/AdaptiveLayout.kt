package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppSpacing

/**
 * Grid de conteúdo altamente responsivo e adaptativo.
 * Calcula automaticamente o número de colunas ideal e o tamanho dos min-cards
 * com base na largura disponível do dispositivo (Portrait Mobile, Landscape Tablet, TV 16:9).
 */
@Composable
fun AdaptiveContentGrid(
    modifier: Modifier = Modifier,
    minCardWidth: Dp = 140.dp,
    contentPadding: PaddingValues = PaddingValues(AppSpacing.lg),
    horizontalSpacing: Dp = AppSpacing.md,
    verticalSpacing: Dp = AppSpacing.md,
    content: LazyGridScope.() -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minCardWidth),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        content = content
    )
}

/**
 * Layout de dois painéis adaptativo (TwoPane):
 * - Em telas largas / Paisagem (TV, Tablet, Celular em Landscape): Exibe lado a lado (Row).
 * - Em telas estreitas / Retrato (Celular em Portrait): Exibe empilhado verticalmente (Column).
 */
@Composable
fun AdaptiveTwoPane(
    primaryPane: @Composable () -> Unit,
    secondaryPane: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    breakpointWidth: Dp = 600.dp,
    spacing: Dp = AppSpacing.lg
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = this.maxWidth >= breakpointWidth

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    primaryPane()
                }
                Box(modifier = Modifier.weight(1.5f)) {
                    secondaryPane()
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    primaryPane()
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    secondaryPane()
                }
            }
        }
    }
}
