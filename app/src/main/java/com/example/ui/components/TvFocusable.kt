package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TvFocusBorder
import com.example.ui.theme.TvFocusGlow

/**
 * Modifier específico para Android TV, Google TV e TV Box.
 * Fornece:
 * - Ampliação visual suave ao receber foco (Scale)
 * - Borda com brilho azul/ciano característico de players de TV
 * - Sombra/elevação destacada
 * - Rolagem automática da viewport para o item focado (Bring Into View)
 * - Compatibilidade direta com botões D-Pad (OK/Enter/Center) e toque
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun Modifier.tvFocusable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    focusBorderColor: Color = TvFocusBorder,
    focusGlowColor: Color = TvFocusGlow,
    unfocusedBorderColor: Color = Color.Transparent,
    focusedBorderWidth: Dp = 2.5.dp,
    unfocusedBorderWidth: Dp = 0.dp,
    focusedScale: Float = 1.04f,
    onClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(isFocused) {
        if (isFocused && enabled) {
            try {
                bringIntoViewRequester.bringIntoView()
            } catch (_: Exception) {
            }
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isFocused && enabled) focusedScale else 1.0f,
        animationSpec = tween(durationMillis = 180),
        label = "tv_scale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused && enabled) focusBorderColor else unfocusedBorderColor,
        animationSpec = tween(durationMillis = 180),
        label = "tv_border_color"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isFocused && enabled) focusedBorderWidth else unfocusedBorderWidth,
        animationSpec = tween(durationMillis = 180),
        label = "tv_border_width"
    )

    val elevation by animateDpAsState(
        targetValue = if (isFocused && enabled) 14.dp else 0.dp,
        animationSpec = tween(durationMillis = 180),
        label = "tv_elevation"
    )

    return this
        .bringIntoViewRequester(bringIntoViewRequester)
        .scale(scale)
        .shadow(
            elevation = elevation,
            shape = shape,
            spotColor = focusGlowColor,
            ambientColor = focusGlowColor
        )
        .then(
            if (borderWidth > 0.dp) {
                Modifier.border(
                    border = BorderStroke(borderWidth, borderColor),
                    shape = shape
                )
            } else {
                Modifier
            }
        )
        .focusable(enabled = enabled, interactionSource = interactionSource)
        .then(
            if (onClick != null && enabled) {
                Modifier
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.DirectionCenter ||
                            keyEvent.key == Key.Enter ||
                            keyEvent.key == Key.NumPadEnter
                        ) {
                            onClick()
                            true
                        } else {
                            false
                        }
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
            } else {
                Modifier
            }
        )
}
