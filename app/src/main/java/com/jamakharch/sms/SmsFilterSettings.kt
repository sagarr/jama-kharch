package com.jamakharch.sms

import android.content.Context

object SmsFilterSettings {

    private const val PREFS_NAME = "sms_filter"
    private const val KEY_PATTERNS = "sender_patterns"

    val defaultPatterns = listOf(
        ".*ICICI.*",
        ".*SBI.*",
    )

    fun getPatterns(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getStringSet(KEY_PATTERNS, null)
        return if (stored != null) {
            stored.toList().sorted()
        } else {
            defaultPatterns
        }
    }

    fun setPatterns(context: Context, patterns: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_PATTERNS, patterns.toSet()).apply()
    }

    fun addPattern(context: Context, pattern: String) {
        val current = getPatterns(context).toMutableSet()
        current.add(pattern.trim())
        setPatterns(context, current.toList())
    }

    fun removePattern(context: Context, pattern: String) {
        val current = getPatterns(context).toMutableSet()
        current.remove(pattern)
        setPatterns(context, current.toList())
    }

    fun resetToDefaults(context: Context) {
        setPatterns(context, defaultPatterns)
    }
}
