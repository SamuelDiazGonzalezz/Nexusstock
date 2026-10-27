package com.nexusstock.data.repository

import com.nexusstock.data.model.Product
import kotlinx.coroutines.flow.Flow

/** Contrato del repositorio: la UI y el bot solo dependen de esta interfaz. */
interface InventoryRepository {
    fun observeProducts(): Flow<List<Product>>
    suspend fun getAll(): List<Product>
    suspend fun findByName(name: String): Product?
    suspend fun search(query: String): List<Product>
    suspend fun save(product: Product)
    suspend fun delete(product: Product)

    /** Suma (o resta, si [delta] es negativo) unidades. Crea el producto si no existe y delta > 0. */
    suspend fun adjustStock(name: String, delta: Int): Product?
}
