package com.jamakharch.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jamakharch.sms.SmsFilterSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsVM(private val appContext: Context) : ViewModel() {

    private val _patterns = MutableStateFlow(SmsFilterSettings.getPatterns(appContext))
    val patterns: StateFlow<List<String>> = _patterns.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun addPattern(pattern: String) {
        val trimmed = pattern.trim()
        if (trimmed.isEmpty()) return
        if (trimmed in _patterns.value) {
            _message.value = "Pattern already exists"
            return
        }
        val updated = _patterns.value + trimmed
        _patterns.value = updated
        SmsFilterSettings.setPatterns(appContext, updated)
        _message.value = "Pattern added"
    }

    fun removePattern(pattern: String) {
        val updated = _patterns.value - pattern
        _patterns.value = updated
        SmsFilterSettings.setPatterns(appContext, updated)
        _message.value = "Pattern removed"
    }

    fun resetToDefaults() {
        SmsFilterSettings.resetToDefaults(appContext)
        _patterns.value = SmsFilterSettings.getPatterns(appContext)
        _message.value = "Reset to defaults"
    }

    fun dismissMessage() {
        _message.value = null
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsVM(context.applicationContext) as T
        }
    }
}
