package com.example.quickbillposs.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val total: Double,
    val paymentMethod: String, // "CASH", "UPI", "CARD"
    val itemCount: Int,
    val amountTendered: Double = 0.0,
    val changeGiven: Double = 0.0,
    val receiptText: String = "",
    val notes: String = ""
)
