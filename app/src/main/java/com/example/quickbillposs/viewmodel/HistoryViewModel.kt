package com.example.quickbillposs.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickbillposs.data.AppDatabase
import com.example.quickbillposs.data.model.Sale
import com.example.quickbillposs.data.model.SaleItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val saleDao = AppDatabase.getInstance(application).saleDao()

    /** All sales, newest first */
    val sales: StateFlow<List<Sale>> = saleDao
        .getRecentSales(limit = 100)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Today's total revenue */
    val todayRevenue: StateFlow<Double> = run {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = cal.timeInMillis

        saleDao.getDailyTotal(startOfDay, endOfDay)
            .map { it ?: 0.0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    }

    /** Items for a specific sale (fetched on demand) */
    private val _selectedSaleItems = MutableStateFlow<List<SaleItem>>(emptyList())
    val selectedSaleItems: StateFlow<List<SaleItem>> = _selectedSaleItems.asStateFlow()

    fun loadItemsForSale(saleId: Long) {
        viewModelScope.launch {
            _selectedSaleItems.value = saleDao.getItemsForSale(saleId)
        }
    }

    fun deleteSale(saleId: Long) {
        viewModelScope.launch {
            saleDao.deleteSale(saleId)
        }
    }
}
