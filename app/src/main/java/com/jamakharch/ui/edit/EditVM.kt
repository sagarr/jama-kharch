package com.jamakharch.ui.edit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jamakharch.category.Category
import com.jamakharch.data.AppDatabase
import com.jamakharch.data.ExpenseEntity
import com.jamakharch.data.MappingBackup
import com.jamakharch.data.MerchantOverrideEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditVM(
    private val appContext: Context,
    private val expenseId: Long
) : ViewModel() {

    private val dao = AppDatabase.getInstance(appContext).expenseDao()

    private val _expense = MutableStateFlow<ExpenseEntity?>(null)
    val expense: StateFlow<ExpenseEntity?> = _expense.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        viewModelScope.launch {
            _expense.value = dao.getById(expenseId)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            saveCategory(category.name)
        }
    }

    fun updateCustomCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            saveCategory(name)
        }
    }

    private suspend fun saveCategory(name: String) {
        dao.updateCategory(expenseId, name)
        val merchant = _expense.value?.merchant ?: return
        val lowerMerchant = merchant.lowercase()

        val isGeneric = lowerMerchant in setOf("upi transfer", "unknown")
        if (!isGeneric) {
            val overrideDao = AppDatabase.getInstance(appContext).merchantOverrideDao()
            overrideDao.setOverride(
                MerchantOverrideEntity(merchant = lowerMerchant, category = name)
            )
            dao.updateCategoryByMerchant(lowerMerchant, name)
        }

        MappingBackup.writeAuto(appContext)
        _saved.value = true
    }

    class Factory(
        private val context: Context,
        private val expenseId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditVM(context.applicationContext, expenseId) as T
        }
    }
}
