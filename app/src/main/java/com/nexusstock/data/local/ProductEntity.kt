package com.nexusstock.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nexusstock.data.model.Product

@Entity(tableName = "products", indices = [Index(value = ["name"], unique = true)])
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val quantity: Int,
    val minStock: Int,
    val price: Double,
    val updatedAt: Long
)

fun ProductEntity.toDomain() = Product(id, name, category, quantity, minStock, price, updatedAt)

fun Product.toEntity() = ProductEntity(id, name, category, quantity, minStock, price, updatedAt)
