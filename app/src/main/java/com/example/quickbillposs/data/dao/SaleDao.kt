package com.example.quickbillposs.data.dao

import androidx.room.*
import com.example.quickbillposs.data.model.Sale
import com.example.quickbillposs.data.model.SaleItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItem>)

    @Transaction
    suspend fun insertSaleWithItems(sale: Sale, items: List<SaleItem>): Long {
        val saleId = insertSale(sale)
        insertSaleItems(items.map { it.copy(saleId = saleId) })
        return saleId
    }

    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSales(limit: Int = 50): Flow<List<Sale>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSale(saleId: Long): List<SaleItem>

    @Query("DELETE FROM sales WHERE id = :saleId")
    suspend fun deleteSale(saleId: Long)

    @Query("SELECT SUM(total) FROM sales WHERE timestamp >= :fromTimestamp")
    suspend fun getTotalRevenueSince(fromTimestamp: Long): Double?

    @Query("SELECT COUNT(*) FROM sales WHERE timestamp >= :fromTimestamp")
    suspend fun getSaleCountSince(fromTimestamp: Long): Int

    @Query("""
        SELECT SUM(total) FROM sales 
        WHERE timestamp >= :startOfDay AND timestamp < :endOfDay
    """)
    fun getDailyTotal(startOfDay: Long, endOfDay: Long): Flow<Double?>

    @Query("SELECT * FROM sales ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastSale(): Sale?
}
