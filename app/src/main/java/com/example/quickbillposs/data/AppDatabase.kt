package com.example.quickbillposs.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.quickbillposs.data.dao.ProductDao
import com.example.quickbillposs.data.dao.SaleDao
import com.example.quickbillposs.data.model.Product
import com.example.quickbillposs.data.model.Sale
import com.example.quickbillposs.data.model.SaleItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Sale::class, SaleItem::class, Product::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun saleDao(): SaleDao
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quickbill_pos.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed common products on first launch
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.productDao()?.insertAll(SEED_PRODUCTS)
                            }
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }

        /** Common Indian retail items for smart suggestions on first install */
        private val SEED_PRODUCTS = listOf(
            Product(name = "Tea", price = 10.0, category = "Beverages", frequency = 50),
            Product(name = "Coffee", price = 20.0, category = "Beverages", frequency = 40),
            Product(name = "Biscuits", price = 10.0, category = "Snacks", frequency = 35),
            Product(name = "Bread", price = 40.0, category = "Bakery", frequency = 30),
            Product(name = "Milk (500ml)", price = 28.0, category = "Dairy", frequency = 45),
            Product(name = "Milk (1L)", price = 56.0, category = "Dairy", frequency = 40),
            Product(name = "Curd", price = 30.0, category = "Dairy", frequency = 25),
            Product(name = "Eggs (6 pcs)", price = 42.0, category = "Dairy", frequency = 20),
            Product(name = "Sugar (1kg)", price = 42.0, category = "Grocery", frequency = 15),
            Product(name = "Salt", price = 20.0, category = "Grocery", frequency = 10),
            Product(name = "Rice (1kg)", price = 60.0, category = "Grocery", frequency = 20),
            Product(name = "Atta (1kg)", price = 50.0, category = "Grocery", frequency = 20),
            Product(name = "Chips", price = 20.0, category = "Snacks", frequency = 30),
            Product(name = "Cold Drink (250ml)", price = 20.0, category = "Beverages", frequency = 35),
            Product(name = "Cold Drink (600ml)", price = 40.0, category = "Beverages", frequency = 30),
            Product(name = "Water Bottle (1L)", price = 20.0, category = "Beverages", frequency = 25),
            Product(name = "Maggi (70g)", price = 14.0, category = "Instant Food", frequency = 30),
            Product(name = "Soap", price = 30.0, category = "Personal Care", frequency = 15),
            Product(name = "Shampoo Sachet", price = 5.0, category = "Personal Care", frequency = 20),
            Product(name = "Toothpaste (100g)", price = 65.0, category = "Personal Care", frequency = 10),
            Product(name = "Paneer (200g)", price = 70.0, category = "Dairy", frequency = 15),
            Product(name = "Butter (100g)", price = 55.0, category = "Dairy", frequency = 12),
            Product(name = "Coconut Oil", price = 140.0, category = "Grocery", frequency = 8),
            Product(name = "Mustard Oil (1L)", price = 160.0, category = "Grocery", frequency = 8),
            Product(name = "Chocolate", price = 20.0, category = "Snacks", frequency = 25),
        )
    }
}
