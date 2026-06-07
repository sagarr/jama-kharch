package com.jamakharch.sms

import android.content.Context
import android.net.Uri

object SmsReader {

    private val inboxUri: Uri = Uri.parse("content://sms/inbox")

    fun readSms(
        context: Context,
        sinceTimestamp: Long,
        senderPatterns: List<String>
    ): List<RawSms> {
        val compiled = senderPatterns.mapNotNull { pattern ->
            try {
                pattern.toRegex(RegexOption.IGNORE_CASE)
            } catch (_: Exception) {
                null
            }
        }

        if (compiled.isEmpty()) return emptyList()

        val smsList = mutableListOf<RawSms>()
        val cursor = context.contentResolver.query(
            inboxUri, null, null, null, "date DESC"
        ) ?: return smsList

        cursor.use {
            while (it.moveToNext()) {
                val addressIdx = it.getColumnIndex("address")
                val bodyIdx = it.getColumnIndex("body")
                val dateIdx = it.getColumnIndex("date")

                if (addressIdx < 0 || bodyIdx < 0 || dateIdx < 0) continue

                val address = it.getString(addressIdx) ?: ""
                val body = it.getString(bodyIdx) ?: ""
                val timestamp = it.getLong(dateIdx)

                if (timestamp < sinceTimestamp) continue
                if (!compiled.any { regex -> regex.matches(address) }) continue

                smsList.add(RawSms(body, timestamp, address))
            }
        }

        return smsList
    }
}
