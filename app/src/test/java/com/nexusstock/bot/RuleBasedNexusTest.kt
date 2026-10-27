package com.nexusstock.bot

import com.nexusstock.data.model.Product
import com.nexusstock.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Repositorio en memoria para probar el bot sin base de datos. */
internal class FakeRepository : InventoryRepository {
    val products = MutableStateFlow<List<Product>>(emptyList())
    private var nextId = 1L

    override fun observeProducts(): Flow<List<Product>> = products
    override suspend fun getAll() = products.value
    override suspend fun findByName(name: String) =
        products.value.firstOrNull { it.name.equals(name, true) } ?: search(name).firstOrNull()
    override suspend fun search(query: String) = products.value.filter { it.name.contains(query, true) }
    override suspend fun save(product: Product) {
        products.value = products.value.filter { it.id != product.id } + product.copy(id = product.id.takeIf { it != 0L } ?: nextId++)
    }
    override suspend fun delete(product: Product) { products.value = products.value.filter { it.id != product.id } }
    override suspend fun adjustStock(name: String, delta: Int): Product? {
        val existing = findByName(name)
        val updated = when {
            existing != null -> existing.copy(quantity = (existing.quantity + delta).coerceAtLeast(0))
            delta > 0 -> Product(id = nextId++, name = name, quantity = delta)
            else -> return null
        }
        products.value = products.value.filter { it.id != updated.id } + updated
        return updated
    }
}

class RuleBasedNexusTest {
    private val repo = FakeRepository()
    private val bot = RuleBasedNexus(repo)

    @Test
    fun `añade, retira, consulta y elimina`() = runBlocking {
        bot.reply("Añade 10 tornillos")
        assertEquals(10, repo.findByName("tornillos")?.quantity)

        bot.reply("quita 3 unidades de tornillos")
        assertEquals(7, repo.findByName("tornillos")?.quantity)

        assertTrue(bot.reply("¿Cuántos tornillos hay?").contains("7"))

        bot.reply("elimina tornillos")
        assertNull(repo.findByName("tornillos"))
    }

    @Test
    fun `conserva tildes en el nombre del producto`() = runBlocking {
        bot.reply("agrega 2 cajas de cartón")
        assertEquals("cajas de cartón", repo.getAll().single().name)
    }

    @Test
    fun `comando desconocido devuelve la ayuda`() = runBlocking {
        assertEquals(RuleBasedNexus.HELP, bot.reply("xyz"))
    }
}
