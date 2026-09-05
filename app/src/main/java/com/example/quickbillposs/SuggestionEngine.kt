package com.example.quickbillposs

import com.example.quickbillposs.data.dao.ProductDao
import com.example.quickbillposs.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Ranks products based on:
 *  - Price proximity to input (40% weight)
 *  - Usage frequency (40% weight)
 *  - Recency of last use (20% weight)
 *
 * Mirrors the price-bucket + frequency logic from the web version.
 * All DB operations run on Dispatchers.IO to prevent main thread blocking (ANR).
 */
class SuggestionEngine(private val productDao: ProductDao) {

    /**
     * Returns up to [maxResults] suggestions for a given price input string.
     * Handles formats like "50", "50x2", "50*2".
     */
    suspend fun getSuggestions(
        input: String,
        maxResults: Int = 8
    ): List<Product> = withContext(Dispatchers.IO) {
        val unitPrice = parseUnitPrice(input) ?: return@withContext getTopProducts(maxResults)
        if (unitPrice <= 0.0) return@withContext getTopProducts(maxResults)

        // Price bucket: ±30% of entered price, plus exact neighbors
        val bucketMin = unitPrice * 0.70
        val bucketMax = unitPrice * 1.30

        val candidates = productDao.getProductsInPriceRange(
            minPrice = bucketMin,
            maxPrice = bucketMax,
            limit = 30
        )

        if (candidates.isEmpty()) return@withContext getTopProducts(maxResults)

        val maxFreq = candidates.maxOf { it.frequency }.coerceAtLeast(1)
        val maxRecency = candidates.maxOf { it.lastUsed }.coerceAtLeast(1L)

        candidates
            .map { product ->
                val priceScore = 1.0 - (abs(product.price - unitPrice) / (bucketMax - bucketMin + 1))
                val freqScore = product.frequency.toDouble() / maxFreq
                val recencyScore = if (maxRecency > 0) {
                    product.lastUsed.toDouble() / maxRecency
                } else 0.0

                val totalScore = (priceScore * 0.40) + (freqScore * 0.40) + (recencyScore * 0.20)
                Pair(product, totalScore)
            }
            .sortedByDescending { it.second }
            .take(maxResults)
            .map { it.first }
    }

    /**
     * Parses price from input string.
     * Supports: "50", "50x2", "50*2", "50.5"
     */
    private fun parseUnitPrice(input: String): Double? {
        if (input.isBlank()) return null
        val clean = input.trim()

        // Handle "price x quantity" formats
        val multiplyRegex = Regex("""^(\d+\.?\d*)\s*[xX*×]\s*(\d+\.?\d*)$""")
        val multiplyMatch = multiplyRegex.matchEntire(clean)
        if (multiplyMatch != null) {
            return multiplyMatch.groupValues[1].toDoubleOrNull()
        }

        return clean.toDoubleOrNull()
    }

    private suspend fun getTopProducts(limit: Int): List<Product> = withContext(Dispatchers.IO) {
        productDao.getTopProducts(limit)
    }

    /**
     * Records a sale of the given product to improve future suggestions.
     * Call this after each successful checkout.
     */
    suspend fun recordUsage(productId: Long) = withContext(Dispatchers.IO) {
        productDao.incrementFrequency(productId)
    }

    /**
     * Upserts a product learned from a sale (no named product, just a price).
     * This creates auto-learned entries that power suggestions over time.
     */
    suspend fun learnFromSale(price: Double, label: String = "") = withContext(Dispatchers.IO) {
        if (price <= 0) return@withContext
        val name = label.ifBlank { "₹${price.toInt()} item" }
        val existing = productDao.findByNameAndPrice(name, price)
        if (existing != null) {
            productDao.incrementFrequency(existing.id)
        } else {
            productDao.insertProduct(
                Product(
                    name = name,
                    price = price,
                    category = "Learned",
                    frequency = 1,
                    lastUsed = System.currentTimeMillis()
                )
            )
        }
    }
}
