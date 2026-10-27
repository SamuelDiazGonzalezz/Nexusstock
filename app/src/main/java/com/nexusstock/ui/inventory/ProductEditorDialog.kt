package com.nexusstock.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nexusstock.data.model.Product

@Composable
fun ProductEditorDialog(
    product: Product?,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit,
    onDelete: (Product) -> Unit
) {
    var name by remember { mutableStateOf(product?.name.orEmpty()) }
    var category by remember { mutableStateOf(product?.category ?: "General") }
    var quantity by remember { mutableStateOf(product?.quantity?.toString() ?: "0") }
    var minStock by remember { mutableStateOf(product?.minStock?.toString() ?: "5") }
    var price by remember { mutableStateOf(product?.price?.toString() ?: "0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text(if (product == null) "Nuevo producto" else "Editar producto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(category, { category = it }, label = { Text("Categoría") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Cantidad", quantity, { quantity = it }, Modifier.weight(1f))
                    NumberField("Mínimo", minStock, { minStock = it }, Modifier.weight(1f))
                }
                NumberField("Precio (€)", price, { price = it }, Modifier.fillMaxWidth(), decimal = true)
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        (product ?: Product(name = "")).copy(
                            name = name.trim(),
                            category = category.trim().ifBlank { "General" },
                            quantity = quantity.toIntOrNull() ?: 0,
                            minStock = minStock.toIntOrNull() ?: 0,
                            price = price.replace(',', '.').toDoubleOrNull() ?: 0.0
                        )
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (product != null) {
                    TextButton(onClick = { onDelete(product) }) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier, decimal: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier
    )
}
