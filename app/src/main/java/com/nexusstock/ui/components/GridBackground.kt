package com.nexusstock.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.nexusstock.ui.theme.NexusBlack
import com.nexusstock.ui.theme.NexusBlue
import com.nexusstock.ui.theme.NexusGrid

/** Fondo negro con rejilla gris sutil y una línea azul brillante arriba (estilo de la marca). */
@Composable
fun GridBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(NexusBlack)) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 24.dp.toPx()
            val gridColor = NexusGrid.copy(alpha = 0.35f)
            var x = 0f
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f); x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f); y += step
            }
            // Resplandor azul superior
            drawRect(
                Brush.verticalGradient(listOf(NexusBlue.copy(alpha = 0.18f), NexusBlack.copy(alpha = 0f)), endY = 160.dp.toPx())
            )
            drawLine(NexusBlue, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 2.dp.toPx())
        }
        content()
    }
}
