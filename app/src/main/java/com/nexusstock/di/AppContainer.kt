package com.nexusstock.di

import android.content.Context
import com.nexusstock.BuildConfig
import com.nexusstock.bot.GroqClient
import com.nexusstock.bot.GroqNexus
import com.nexusstock.bot.InventoryTools
import com.nexusstock.bot.NexusBot
import com.nexusstock.bot.RuleBasedNexus
import com.nexusstock.data.local.NexusDatabase
import com.nexusstock.data.repository.InventoryRepository
import com.nexusstock.data.repository.InventoryRepositoryImpl

/** Inyección de dependencias manual: un único punto donde se crean las dependencias. */
class AppContainer(context: Context) {
    private val database = NexusDatabase.create(context)

    val inventoryRepository: InventoryRepository = InventoryRepositoryImpl(database.productDao())

    val nexusBot: NexusBot = NexusBot(
        ai = BuildConfig.GROQ_API_KEY.takeIf { it.isNotBlank() }?.let { key ->
            GroqNexus(GroqClient(key), InventoryTools(inventoryRepository), BuildConfig.GROQ_MODEL)
        },
        local = RuleBasedNexus(inventoryRepository)
    )
}
