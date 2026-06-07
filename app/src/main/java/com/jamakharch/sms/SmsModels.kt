package com.jamakharch.sms

data class RawSms(
    val body: String,
    val timestamp: Long,
    val address: String
)

data class ParsedSms(
    val amount: Double,
    val merchant: String,
    val timestamp: Long,
    val rawSnippet: String
)
