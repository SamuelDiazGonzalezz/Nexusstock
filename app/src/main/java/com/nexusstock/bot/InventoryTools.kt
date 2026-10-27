package com.nexusstock.bot

import com.nexusstock.data.model.Product
import com.nexusstock.data.repository.InventoryRepository
import org.json.JSONArray
import org.json.JSONObject

/**
 * Herramientas que el modelo puede invocar (tool calling).
 * El modelo decide qué función llamar; la app la ejecuta contra el repositorio y le devuelve el resultado.
 */
class InventoryTools(private val repository: InventoryRepository) {

    /** Definiciones en formato JSON Schema que se envían a Groq. */
    val definitions: JSONArray = JSONArray().apply {
        put(tool("listar_productos", "Devuelve todos los productos del inventario."))
        put(tool("resumen_inventario", "Totales: número de productos, unidades, valor y cuántos están bajo mínimos."))
        put(tool("stock_bajo", "Productos cuya cantidad es menor o igual a su stock mínimo."))
        put(tool("buscar_productos", "Busca productos cuyo nombre contenga el texto.", required = listOf("consulta")) {
            put("consulta", prop("string", "Texto a buscar"))
        })
        put(tool("consultar_producto", "Devuelve los datos de un producto concreto.", required = listOf("nombre")) {
            put("nombre", prop("string", "Nombre del producto"))
        })
        put(tool(
            "ajustar_stock",
            "Suma o resta unidades a un producto. Cantidad positiva = entrada, negativa = salida. " +
                "Si el producto no existe y la cantidad es positiva, se crea.",
            required = listOf("nombre", "cantidad")
        ) {
            put("nombre", prop("string", "Nombre del producto"))
            put("cantidad", prop("integer", "Unidades a sumar (positivo) o restar (negativo)"))
        })
        put(tool(
            "guardar_producto",
            "Crea un producto o actualiza sus datos. Solo se modifican los campos indicados.",
            required = listOf("nombre")
        ) {
            put("nombre", prop("string", "Nombre del producto"))
            put("nuevo_nombre", prop("string", "Nuevo nombre, si se quiere renombrar"))
            put("categoria", prop("string", "Categoría"))
            put("cantidad", prop("integer", "Cantidad exacta en stock"))
            put("stock_minimo", prop("integer", "Stock mínimo antes de avisar"))
            put("precio", prop("number", "Precio unitario en euros"))
        })
        put(tool("eliminar_producto", "Elimina un producto del inventario de forma permanente.", required = listOf("nombre")) {
            put("nombre", prop("string", "Nombre del producto"))
        })
    }

    /** Ejecuta la herramienta [name] y devuelve el resultado como JSON (texto) para el modelo. */
    suspend fun execute(name: String, rawArguments: String): String {
        val args = runCatching { JSONObject(rawArguments.ifBlank { "{}" }) }.getOrElse { JSONObject() }
        val result = runCatching {
            when (name) {
                "listar_productos" -> ok("productos", repository.getAll().toJson())
                "resumen_inventario" -> summary()
                "stock_bajo" -> ok("productos", repository.getAll().filter { it.isLowStock }.toJson())
                "buscar_productos" -> ok("productos", repository.search(args.getString("consulta")).toJson())
                "consultar_producto" -> resolve(args.getString("nombre"))
                    ?.let { ok("producto", it.toJson()) } ?: notFound(args.getString("nombre"))
                "ajustar_stock" -> adjustStock(args.getString("nombre"), args.getInt("cantidad"))
                "guardar_producto" -> saveProduct(args)
                "eliminar_producto" -> resolve(args.getString("nombre"))?.let {
                    repository.delete(it)
                    ok("eliminado", it.name)
                } ?: notFound(args.getString("nombre"))
                else -> error("Herramienta desconocida: $name")
            }
        }.getOrElse { error(it.message ?: "Error al ejecutar $name") }
        return result.toString()
    }

    private suspend fun saveProduct(args: JSONObject): JSONObject {
        val name = args.getString("nombre").trim()
        // Para crear/editar solo se acepta coincidencia exacta, así nunca se sobrescribe otro producto.
        val base = repository.getAll().firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Product(name = name)
        val updated = base.copy(
            name = args.optString("nuevo_nombre").ifBlank { base.name },
            category = args.optString("categoria").ifBlank { base.category },
            quantity = if (args.has("cantidad")) args.getInt("cantidad").coerceAtLeast(0) else base.quantity,
            minStock = if (args.has("stock_minimo")) args.getInt("stock_minimo").coerceAtLeast(0) else base.minStock,
            price = if (args.has("precio")) args.getDouble("precio").coerceAtLeast(0.0) else base.price
        )
        repository.save(updated)
        return ok("producto", (resolve(updated.name) ?: updated).toJson())
    }

    private suspend fun adjustStock(name: String, delta: Int): JSONObject {
        val product = resolve(name)
        if (product == null && delta <= 0) return notFound(name)
        val updated = repository.adjustStock(product?.name ?: name.trim(), delta) ?: return notFound(name)
        return ok("producto", updated.toJson())
    }

    /**
     * Busca un producto por nombre exacto o, si no, por coincidencia parcial única.
     * Si hay varias coincidencias lanza un error para que el modelo pregunte al usuario.
     */
    private suspend fun resolve(name: String): Product? {
        val query = name.trim()
        repository.getAll().firstOrNull { it.name.equals(query, ignoreCase = true) }?.let { return it }
        val matches = repository.search(query)
        if (matches.size > 1) {
            throw IllegalStateException("\"$query\" es ambiguo, coincide con: ${matches.joinToString { it.name }}")
        }
        return matches.firstOrNull()
    }

    private suspend fun summary(): JSONObject {
        val all = repository.getAll()
        return JSONObject()
            .put("ok", true)
            .put("productos", all.size)
            .put("unidades", all.sumOf { it.quantity })
            .put("valor_eur", all.sumOf { it.quantity * it.price })
            .put("bajo_minimos", all.count { it.isLowStock })
    }

    private fun Product.toJson() = JSONObject()
        .put("nombre", name)
        .put("categoria", category)
        .put("cantidad", quantity)
        .put("stock_minimo", minStock)
        .put("precio", price)
        .put("bajo_minimos", isLowStock)

    private fun List<Product>.toJson() = JSONArray().also { array -> forEach { array.put(it.toJson()) } }

    private fun ok(key: String, value: Any) = JSONObject().put("ok", true).put(key, value)
    private fun notFound(name: String) = error("No existe ningún producto llamado \"$name\"")
    private fun error(message: String) = JSONObject().put("ok", false).put("error", message)

    private fun prop(type: String, description: String) = JSONObject().put("type", type).put("description", description)

    private fun tool(
        name: String,
        description: String,
        required: List<String> = emptyList(),
        properties: JSONObject.() -> Unit = {}
    ) = JSONObject()
        .put("type", "function")
        .put(
            "function", JSONObject()
                .put("name", name)
                .put("description", description)
                .put(
                    "parameters", JSONObject()
                        .put("type", "object")
                        .put("properties", JSONObject().apply(properties))
                        .put("required", JSONArray(required))
                )
        )
}
