package com.example.quickbillposs.data.dao

import androidx.room.*
import com.example.quickbillposs.data.model.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY frequency DESC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("""
        SELECT * FROM products 
        WHERE isActive = 1 
          AND price BETWEEN :minPrice AND :maxPrice 
        ORDER BY frequency DESC, lastUsed DESC
        LIMIT :limit
    """)
    suspend fun getProductsInPriceRange(
        minPrice: Double,
        maxPrice: Double,
        limit: Int = 10
    ): List<Product>

    @Query("""
        SELECT * FROM products 
        WHERE isActive = 1 
          AND (name LIKE :query OR price = :exactPrice)
        ORDER BY frequency DESC
        LIMIT :limit
    """)
    suspend fun searchProducts(
        query: String,
        exactPrice: Double,
        limit: Int = 8
    ): List<Product>

    @Query("""
        UPDATE products 
        SET frequency = frequency + 1, lastUsed = :timestamp 
        WHERE id = :productId
    """)
    suspend fun incrementFrequency(productId: Long, timestamp: Long = System.currentTimeMillis())

    /** Upsert by name+price: update freq if exists, otherwise insert */
    @Query("SELECT * FROM products WHERE name = :name AND price = :price LIMIT 1")
    suspend fun findByNameAndPrice(name: String, price: Double): Product?

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY frequency DESC LIMIT :limit")
    suspend fun getTopProducts(limit: Int = 8): List<Product>

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: Long)
}
