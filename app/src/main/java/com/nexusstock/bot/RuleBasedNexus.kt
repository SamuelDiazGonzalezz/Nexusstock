package com.nexusstock.bot

import com.nexusstock.data.model.Product
import com.nexusstock.data.repository.InventoryRepository
import java.text.Normalizer

/**
 * Motor de respaldo de Nexus: interpreta comandos con reglas locales (funciona sin conexión).
 */
class RuleBasedNexus(private val repository: InventoryRepository) : NexusEngine {

    override suspend fun reply(input: String): String {
        val original = input.trim().trim('¿', '¡', '?', '!', '.', ' ')
        if (original.isEmpty()) return "Escríbeme algo y te ayudo con el inventario."
        val text = normalize(original)

        fun nameFrom(match: MatchResult, group: Int): String {
            // Recuperamos el nombre del texto original para conservar tildes y mayúsculas.
            val range = match.groups[group]?.range ?: return ""
            val source = if (text.length == original.length) original else text
            return source.substring(range).trim().trimEnd('?', '.', '!')
        }

        ADD.find(text)?.let { m ->
            val qty = m.groupValues[2].toInt()
            val product = repository.adjustStock(nameFrom(m, 3), qty)
                ?: return "No he podido añadir el producto."
            return "Hecho. He añadido $qty uds. a **${product.name}**. Stock actual: ${product.quantity}."
        }

        REMOVE.find(text)?.let { m ->
            val qty = m.groupValues[2].toInt()
            val name = nameFrom(m, 3)
            val product = repository.adjustStock(name, -qty)
                ?: return "No encuentro \"$name\" en el inventario."
            val warning = if (product.isLowStock) "\n⚠ Está por debajo del stock mínimo (${product.minStock})." else ""
            return "He retirado $qty uds. de **${product.name}**. Quedan ${product.quantity}.$warning"
        }

        DELETE.find(text)?.let { m ->
            val name = nameFrom(m, 2)
            val product = repository.findByName(name) ?: return "No encuentro \"$name\" en el inventario."
            repository.delete(product)
            return "He eliminado **${product.name}** del inventario."
        }

        if (LOW_STOCK.containsMatchIn(text)) {
            val low = repository.getAll().filter { it.isLowStock }
            return if (low.isEmpty()) "Todo en orden: ningún producto está bajo mínimos."
            else "Productos con stock bajo:\n" + low.toLines()
        }

        if (LIST.containsMatchIn(text)) {
            val all = repository.getAll()
            return if (all.isEmpty()) "El inventario está vacío. Prueba con \"añade 10 tornillos\"."
            else "Tienes ${all.size} productos:\n" + all.toLines()
        }

        QUERY.find(text)?.let { m ->
            val name = nameFrom(m, 1)
            val product = repository.findByName(name) ?: return "No tengo \"$name\" registrado."
            return "Hay ${product.quantity} uds. de **${product.name}** (mínimo ${product.minStock})."
        }

        SEARCH.find(text)?.let { m ->
            val results = repository.search(nameFrom(m, 2))
            return if (results.isEmpty()) "Sin resultados." else "Encontrado:\n" + results.toLines()
        }

        if (GREETING.containsMatchIn(text)) return "¡Hola! Soy Nexus. ¿Qué gestionamos hoy?"

        return HELP
    }

    private fun List<Product>.toLines() =
        joinToString("\n") { "• ${it.name}: ${it.quantity} uds." }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")

    companion object {
        private val ADD = Regex("^(anade|agrega|suma|mete|entrada de)\\s+(\\d+)\\s+(?:unidades?\\s+|uds?\\.?\\s+)?(?:de\\s+)?(.+)$")
        private val REMOVE = Regex("^(quita|retira|resta|saca|vende|salida de)\\s+(\\d+)\\s+(?:unidades?\\s+|uds?\\.?\\s+)?(?:de\\s+)?(.+)$")
        private val DELETE = Regex("^(elimina|borra)\\s+(?:el\\s+producto\\s+)?(.+)$")
        private val QUERY = Regex("^(?:stock de|cuant[oa]s?(?:\\s+(?:hay|quedan))?(?:\\s+unidades)?(?:\\s+de)?|hay)\\s+(.+?)(?:\\s+(?:hay|quedan))?\\??$")
        private val SEARCH = Regex("^(busca|buscar)\\s+(.+)$")
        private val LOW_STOCK = Regex("stock bajo|bajo stock|bajo minimos|reponer|que falta")
        private val LIST = Regex("^(lista|listar|inventario|muestra.*inventario|ver todo)")
        private val GREETING = Regex("^(hola|buenas|hey|buenos dias)")

        val HELP = """
            Puedo ayudarte con esto:
            • "añade 10 tornillos"
            • "quita 3 tornillos"
            • "cuántos tornillos hay"
            • "lista" o "stock bajo"
            • "busca torn"
            • "elimina tornillos"
        """.trimIndent()
    }
}
