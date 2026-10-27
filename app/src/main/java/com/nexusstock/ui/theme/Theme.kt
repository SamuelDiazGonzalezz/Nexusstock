package com.nexusstock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NexusColorScheme = darkColorScheme(
    primary = NexusBlue,
    onPrimary = NexusWhite,
    primaryContainer = NexusBlueDeep,
    onPrimaryContainer = NexusWhite,
    secondary = NexusBlueBright,
    onSecondary = NexusWhite,
    background = NexusBlack,
    onBackground = NexusWhite,
    surface = NexusSurface,
    onSurface = NexusWhite,
    surfaceVariant = NexusSurfaceHigh,
    onSurfaceVariant = NexusTextSecondary,
    surfaceContainer = NexusSurface,
    surfaceContainerHigh = NexusSurfaceHigh,
    outline = NexusOutline,
    outlineVariant = NexusGrid,
    error = NexusError,
    onError = NexusWhite
)

/** La app es siempre oscura para mantener la identidad visual de Nexus. */
@Composable
fun NexusStockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NexusColorScheme,
        typography = NexusTypography,
        content = content
    )
}
