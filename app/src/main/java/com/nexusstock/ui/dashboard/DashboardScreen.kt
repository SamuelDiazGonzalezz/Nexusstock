package com.nexusstock.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexusstock.ui.AppViewModelProvider
import com.nexusstock.ui.components.NexusCard
import com.nexusstock.ui.theme.NexusWarning
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    onOpenNexus: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES"))

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("NexusStock", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Resumen de tu inventario",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Productos", state.totalProducts.toString(), Modifier.weight(1f))
                StatCard("Unidades", state.totalUnits.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Valor", currency.format(state.totalValue), Modifier.weight(1f))
                StatCard("Stock bajo", state.lowStock.size.toString(), Modifier.weight(1f), highlight = state.lowStock.isNotEmpty())
            }
        }
        item {
            NexusCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("Nexus", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Gestiona tu inventario hablando",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = onOpenNexus, shape = RoundedCornerShape(12.dp)) { Text("Abrir") }
                }
            }
        }
        if (state.lowStock.isNotEmpty()) {
            item {
                Text(
                    "Necesitan reposición",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(state.lowStock, key = { it.id }) { product ->
                NexusCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(product.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Text("${product.quantity} / ${product.minStock}", color = NexusWarning)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    NexusCard(modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = if (highlight) NexusWarning else MaterialTheme.colorScheme.onSurface
        )
    }
}
