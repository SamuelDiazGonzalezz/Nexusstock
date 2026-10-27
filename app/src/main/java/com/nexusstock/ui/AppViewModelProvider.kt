package com.nexusstock.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nexusstock.NexusStockApp
import com.nexusstock.ui.chat.ChatViewModel
import com.nexusstock.ui.dashboard.DashboardViewModel
import com.nexusstock.ui.inventory.InventoryViewModel

/** Fábrica central de ViewModels: obtiene las dependencias del [com.nexusstock.di.AppContainer]. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { DashboardViewModel(app().container.inventoryRepository) }
        initializer { InventoryViewModel(app().container.inventoryRepository) }
        initializer { ChatViewModel(app().container.nexusBot) }
    }
}

private fun CreationExtras.app(): NexusStockApp = this[APPLICATION_KEY] as NexusStockApp
