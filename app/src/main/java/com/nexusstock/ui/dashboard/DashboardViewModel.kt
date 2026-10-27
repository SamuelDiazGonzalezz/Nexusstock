package com.nexusstock.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexusstock.data.model.Product
import com.nexusstock.data.repository.InventoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val totalProducts: Int = 0,
    val totalUnits: Int = 0,
    val totalValue: Double = 0.0,
    val lowStock: List<Product> = emptyList()
)

class DashboardViewModel(repository: InventoryRepository) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = repository.observeProducts()
        .map { products ->
            DashboardUiState(
                totalProducts = products.size,
                totalUnits = products.sumOf { it.quantity },
                totalValue = products.sumOf { it.quantity * it.price },
                lowStock = products.filter { it.isLowStock }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}
