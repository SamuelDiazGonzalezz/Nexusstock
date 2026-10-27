package com.nexusstock.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexusstock.data.model.Product
import com.nexusstock.ui.AppViewModelProvider
import com.nexusstock.ui.components.NexusCard
import com.nexusstock.ui.theme.NexusWarning

@Composable
fun InventoryScreen(viewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Text("Inventario", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 20.dp))
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                placeholder = { Text("Buscar producto o categoría") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            if (state.products.isEmpty()) {
                Text(
                    "No hay productos. Pulsa + o pídeselo a Nexus.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp)
                )
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(state.products, key = { it.id }) { product ->
                    ProductRow(
                        product = product,
                        onClick = { viewModel.openEditor(product) },
                        onIncrease = { viewModel.changeStock(product, 1) },
                        onDecrease = { viewModel.changeStock(product, -1) }
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = { viewModel.openEditor() },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) { Icon(Icons.Outlined.Add, contentDescription = "Añadir producto") }
    }

    if (state.isEditorOpen) {
        ProductEditorDialog(
            product = state.editing,
            onDismiss = viewModel::closeEditor,
            onSave = { viewModel.save(it) },
            onDelete = { viewModel.delete(it) }
        )
    }
}

@Composable
private fun ProductRow(product: Product, onClick: () -> Unit, onIncrease: () -> Unit, onDecrease: () -> Unit) {
    NexusCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    product.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDecrease) { Icon(Icons.Outlined.Remove, contentDescription = "Restar") }
            Text(
                product.quantity.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = if (product.isLowStock) NexusWarning else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(44.dp)
            )
            IconButton(onClick = onIncrease) {
                Icon(Icons.Outlined.Add, contentDescription = "Sumar", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
