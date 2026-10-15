package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Sistema centralizado de Design Tokens do LC Player.
 * Define dimensões, espaçamentos, cantos arredondados e paleta temática
 * unificada para Android TV, Google TV, Celulares e Tablets.
 */
object AppSpacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 48.dp
}

object AppCorners {
    val xs = RoundedCornerShape(4.dp)
    val sm = RoundedCornerShape(8.dp)
    val md = RoundedCornerShape(12.dp)
    val lg = RoundedCornerShape(16.dp)
    val xl = RoundedCornerShape(24.dp)
    val pill = RoundedCornerShape(50.dp)
}

object AppElevation {
    val none: Dp = 0.dp
    val low: Dp = 4.dp
    val medium: Dp = 8.dp
    val high: Dp = 16.dp
}

/**
 * Modelo com definições de cor e estilização para status do dispositivo e de listas.
 */
data class StatusThemeStyle(
    val backgroundColor: Color,
    val borderColor: Color,
    val textColor: Color
)

object AppStatusStyles {
    val active = StatusThemeStyle(
        backgroundColor = StatusActiveBg,
        borderColor = StatusActiveBorder,
        textColor = StatusActiveText
    )

    val waiting = StatusThemeStyle(
        backgroundColor = StatusWaitingBg,
        borderColor = StatusWaitingBorder,
        textColor = StatusWaitingText
    )

    val blocked = StatusThemeStyle(
        backgroundColor = StatusBlockedBg,
        borderColor = StatusBlockedBorder,
        textColor = StatusBlockedText
    )

    val expired = StatusThemeStyle(
        backgroundColor = StatusExpiredBg,
        borderColor = StatusExpiredBorder,
        textColor = StatusExpiredText
    )

    val error = StatusThemeStyle(
        backgroundColor = StatusErrorBg,
        borderColor = StatusErrorBorder,
        textColor = StatusErrorText
    )
}
