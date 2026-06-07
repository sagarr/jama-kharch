package com.jamakharch.ui.summary

import kotlinx.coroutines.ExperimentalCoroutinesApi

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jamakharch.data.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryVM(private val appContext: Context) : ViewModel() {

    private val dao = AppDatabase.getInstance(appContext).expenseDao()

    private val _currentMonth = MutableStateFlow(Calendar.getInstance())

    val currentMonthLabel: StateFlow<String> = _currentMonth.map { cal ->
        "${cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())} ${cal.get(Calendar.YEAR)}"
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val currentMonthStart: StateFlow<Long> = _currentMonth.map { cal ->
        val start = cal.clone() as Calendar
        start.set(Calendar.DAY_OF_MONTH, 1)
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)
        start.timeInMillis
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val currentMonthEnd: StateFlow<Long> = _currentMonth.map { cal ->
        val start = cal.clone() as Calendar
        start.set(Calendar.DAY_OF_MONTH, 1)
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)
        start.add(Calendar.MONTH, 1)
        start.timeInMillis
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val categoryTotals: StateFlow<List<CategoryTotal>> = _currentMonth.flatMapLatest { cal ->
        val startOfMonth = cal.clone() as Calendar
        startOfMonth.set(Calendar.DAY_OF_MONTH, 1)
        startOfMonth.set(Calendar.HOUR_OF_DAY, 0)
        startOfMonth.set(Calendar.MINUTE, 0)
        startOfMonth.set(Calendar.SECOND, 0)
        startOfMonth.set(Calendar.MILLISECOND, 0)

        val startOfNextMonth = startOfMonth.clone() as Calendar
        startOfNextMonth.add(Calendar.MONTH, 1)

        dao.getByMonth(startOfMonth.timeInMillis, startOfNextMonth.timeInMillis).map { list ->
            list.groupBy { it.category }
                .map { (cat, expenses) ->
                    CategoryTotal(cat, expenses.sumOf { it.amount }, expenses.size)
                }
                .sortedByDescending { it.total }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val grandTotal: StateFlow<Double> = categoryTotals.map { list ->
        list.sumOf { it.total }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _currentMonthIndex = MutableStateFlow(0)
    val currentMonthIndex: StateFlow<Int> = _currentMonthIndex.asStateFlow()

    fun previousMonth() {
        _currentMonth.update { current ->
            Calendar.getInstance().apply {
                timeInMillis = current.timeInMillis
                add(Calendar.MONTH, -1)
            }
        }
        _currentMonthIndex.update { it - 1 }
    }

    fun nextMonth() {
        _currentMonth.update { current ->
            Calendar.getInstance().apply {
                timeInMillis = current.timeInMillis
                add(Calendar.MONTH, 1)
            }
        }
        _currentMonthIndex.update { it + 1 }
    }

    data class CategoryTotal(val category: String, val total: Double, val count: Int)

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SummaryVM(context.applicationContext) as T
        }
    }
}
