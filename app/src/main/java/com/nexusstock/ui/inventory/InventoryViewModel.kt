package com.nexusstock.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexusstock.data.model.Product
import com.nexusstock.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InventoryUiState(
    val products: List<Product> = emptyList(),
    val editing: Product? = null,
    val isEditorOpen: Boolean = false
)

class InventoryViewModel(private val repository: InventoryRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val editor = MutableStateFlow<Pair<Boolean, Product?>>(false to null)

    val uiState: StateFlow<InventoryUiState> =
        combine(repository.observeProducts(), query, editor) { products, q, (open, editing) ->
            InventoryUiState(
                products = if (q.isBlank()) products else products.filter {
                    it.name.contains(q, ignoreCase = true) || it.category.contains(q, ignoreCase = true)
                },
                editing = editing,
                isEditorOpen = open
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryUiState())

    /** Se expone aparte para que el TextField no sufra saltos de cursor. */
    val searchQuery: StateFlow<String> = query.asStateFlow()

    fun onQueryChange(value: String) { query.value = value }

    fun openEditor(product: Product? = null) { editor.value = true to product }

    fun closeEditor() { editor.value = false to null }

    fun save(product: Product) = viewModelScope.launch {
        runCatching { repository.save(product) }
        closeEditor()
    }

    fun delete(product: Product) = viewModelScope.launch {
        repository.delete(product)
        closeEditor()
    }

    fun changeStock(product: Product, delta: Int) = viewModelScope.launch {
        repository.save(product.copy(quantity = (product.quantity + delta).coerceAtLeast(0)))
    }
}
