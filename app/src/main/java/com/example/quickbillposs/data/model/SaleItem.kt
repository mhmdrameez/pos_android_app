package com.example.quickbillposs.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = Sale::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("saleId")]
)
data class SaleItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleId: Long,
    val amount: Double,         // unit price
    val quantity: Double = 1.0, // quantity can be decimal (e.g. 2.5)
    val label: String = "",     // optional product name
    val lineTotal: Double = 0.0 // stored explicitly; computed before insert
)
