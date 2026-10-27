package com.nexusstock.data.repository

import com.nexusstock.data.local.ProductDao
import com.nexusstock.data.local.toDomain
import com.nexusstock.data.local.toEntity
import com.nexusstock.data.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InventoryRepositoryImpl(private val dao: ProductDao) : InventoryRepository {

    override fun observeProducts(): Flow<List<Product>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<Product> = dao.getAll().map { it.toDomain() }

    override suspend fun findByName(name: String): Product? =
        dao.getByName(name.trim())?.toDomain() ?: dao.search(name.trim()).firstOrNull()?.toDomain()

    override suspend fun search(query: String): List<Product> = dao.search(query.trim()).map { it.toDomain() }

    override suspend fun save(product: Product) {
        val entity = product.copy(updatedAt = System.currentTimeMillis()).toEntity()
        if (product.id == 0L) dao.insert(entity) else dao.update(entity)
    }

    override suspend fun delete(product: Product) = dao.delete(product.toEntity())

    override suspend fun adjustStock(name: String, delta: Int): Product? {
        val existing = findByName(name)
        return when {
            existing != null -> {
                val updated = existing.copy(
                    quantity = (existing.quantity + delta).coerceAtLeast(0),
                    updatedAt = System.currentTimeMillis()
                )
                dao.update(updated.toEntity())
                updated
            }
            delta > 0 -> {
                val created = Product(name = name.trim().replaceFirstChar { it.uppercase() }, quantity = delta)
                val id = dao.insert(created.toEntity())
                created.copy(id = id)
            }
            else -> null
        }
    }
}
