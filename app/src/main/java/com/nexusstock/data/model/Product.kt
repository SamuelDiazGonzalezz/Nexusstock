package com.nexusstock.data.model

/** Modelo de dominio de un producto del inventario. */
data class Product(
    val id: Long = 0,
    val name: String,
    val category: String = "General",
    val quantity: Int = 0,
    val minStock: Int = 5,
    val price: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean get() = quantity <= minStock
}
