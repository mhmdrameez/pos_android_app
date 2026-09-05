package com.example.quickbillposs.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val price: Double,
    val category: String = "General",
    val frequency: Int = 0,       // how often sold
    val lastUsed: Long = 0L,      // timestamp of last sale
    val isActive: Boolean = true
)

/** Lightweight data class used only in UI (not persisted separately) */
data class CartItem(
    val id: Long = System.currentTimeMillis(),
    val label: String,
    val amount: Double,   // unit price
    val quantity: Int = 1
) {
    val lineTotal: Double get() = amount * quantity
}
