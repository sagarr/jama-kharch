package com.jamakharch.ui.list

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jamakharch.category.Category
import com.jamakharch.category.Classifier
import com.jamakharch.data.AppDatabase
import com.jamakharch.data.ExpenseEntity
import com.jamakharch.data.MappingBackup
import com.jamakharch.sms.SmsParser
import com.jamakharch.sms.SmsFilterSettings
import com.jamakharch.sms.SmsReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class ExpenseListVM(private val appContext: Context) : ViewModel() {

    private val dao = AppDatabase.getInstance(appContext).expenseDao()

    val expenses: StateFlow<List<ExpenseEntity>> = dao.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage: StateFlow<String?> = _scanMessage.asStateFlow()

    private val _hasScanned = MutableStateFlow(false)
    val hasScanned: StateFlow<Boolean> = _hasScanned.asStateFlow()

    fun scanSms() {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            _scanMessage.value = null
            try {
                val since = scanStartTimestamp()
                val patterns = SmsFilterSettings.getPatterns(appContext)

                val existingSnippets = dao.getAllFlow().first()
                    .map { it.smsSnippet }
                    .toSet()

                val count = executeScan(since, patterns) { it !in existingSnippets }

                _scanMessage.value = if (count > 0) "Added $count new expenses" else "No new expenses found"
                _hasScanned.value = true
            } catch (e: Exception) {
                _scanMessage.value = "Error: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun rescanAll() {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            _scanMessage.value = null
            try {
                dao.clearAll()
                val since = scanStartTimestamp()
                val patterns = SmsFilterSettings.getPatterns(appContext)
                val count = executeScan(since, patterns) { true }
                _scanMessage.value = "Rescanned: $count expenses"
                _hasScanned.value = true
            } catch (e: Exception) {
                _scanMessage.value = "Error: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    private suspend fun executeScan(
        since: Long,
        patterns: List<String>,
        filter: (String) -> Boolean
    ): Int {
        val rawSmsList = SmsReader.readSms(appContext, since, patterns)
        val overrideLookup = buildOverrideLookup()
        var count = 0
        rawSmsList.forEach { raw ->
            val parsed = SmsParser.parse(raw.body, raw.timestamp) ?: return@forEach
            if (!filter(parsed.rawSnippet)) return@forEach
            val expense = ExpenseEntity(
                amount = parsed.amount,
                merchant = parsed.merchant,
                category = Classifier.classify(parsed.merchant, overrideLookup).name,
                timestamp = parsed.timestamp,
                smsSnippet = parsed.rawSnippet
            )
            dao.insertAll(listOf(expense))
            count++
        }
        return count
    }

    private suspend fun buildOverrideLookup(): (String) -> Category? {
        val overrideDao = AppDatabase.getInstance(appContext).merchantOverrideDao()
        val overrideMap: Map<String, Category> = overrideDao.getAllOverrides().first()
            .mapNotNull { entity ->
                try { entity.merchant to Category.valueOf(entity.category) } catch (_: Exception) { null }
            }.toMap()
        return { overrideMap[it.lowercase()] }
    }

    private fun scanStartTimestamp(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.MONTH, Calendar.MAY)
        cal.set(Calendar.YEAR, 2026)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun dismissMessage() {
        _scanMessage.value = null
    }

    fun exportMappings(uri: Uri) {
        viewModelScope.launch {
            _scanMessage.value = try {
                val n = MappingBackup.exportTo(appContext, uri)
                "Exported $n mappings"
            } catch (e: Exception) {
                "Export failed: ${e.message}"
            }
        }
    }

    fun importMappings(uri: Uri) {
        viewModelScope.launch {
            _scanMessage.value = try {
                val n = MappingBackup.importFrom(appContext, uri)
                "Imported $n mappings"
            } catch (e: Exception) {
                "Import failed: ${e.message}"
            }
        }
    }

    fun autoBackupPath(): String = MappingBackup.autoFile(appContext).absolutePath

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExpenseListVM(context.applicationContext) as T
        }
    }
}
