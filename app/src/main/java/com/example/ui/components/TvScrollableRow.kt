package com.example.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Componente horizontal rolável reutilizável otimizado para Android TV / TV Box
 * e dispositivos com tela sensível ao toque.
 *
 * Características principais:
 * - Em landscape/16:9 (quando todos os itens couberem na viewport):
 *   Não realiza rolagem visual desnecessária.
 * - Em portrait / larguras menores (quando a largura disponível não for suficiente):
 *   Permite rolagem horizontal livre por gesto de toque (touch).
 * - Em Android TV / D-pad:
 *   Ao navegar com as setas do controle remoto (esquerda/direita), o item focado
 *   é automaticamente trazido para a área visível (bring-into-view), garantindo
 *   que o foco nunca fique escondido fora da tela.
 * - Reutilizável para barras de abas, filtros, seletores e trilhas horizontais no LC Player.
 */
@Composable
fun TvScrollableRow(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(14.dp),
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.horizontalScroll(scrollState),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        content = content
    )
}
