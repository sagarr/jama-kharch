package com.jamakharch.ui.categorydetail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jamakharch.data.AppDatabase
import com.jamakharch.data.ExpenseEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MerchantGroup(
    val merchant: String,
    val expenses: List<ExpenseEntity>,
    val total: Double
)

class CategoryDetailVM(
    appContext: Context,
    val category: String,
    monthStart: Long,
    monthEnd: Long
) : ViewModel() {

    private val dao = AppDatabase.getInstance(appContext).expenseDao()

    val merchantGroups: StateFlow<List<MerchantGroup>> =
        dao.getByCategoryAndMonth(category, monthStart, monthEnd).map { expenses ->
            expenses.groupBy { it.merchant }
                .map { (merchant, list) ->
                    MerchantGroup(merchant, list, list.sumOf { it.amount })
                }
                .sortedByDescending { group -> group.expenses.first().timestamp }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedExpenseId = MutableStateFlow<Long?>(null)
    val selectedExpenseId: StateFlow<Long?> = _selectedExpenseId.asStateFlow()

    fun selectExpense(id: Long) {
        _selectedExpenseId.value = id
    }

    fun clearSelection() {
        _selectedExpenseId.value = null
    }

    class Factory(
        private val context: Context,
        private val category: String,
        private val monthStart: Long,
        private val monthEnd: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CategoryDetailVM(context.applicationContext, category, monthStart, monthEnd) as T
        }
    }
}
