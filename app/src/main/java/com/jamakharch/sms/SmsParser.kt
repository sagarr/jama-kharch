package com.jamakharch.sms

object SmsParser {

    private val patternDebitCard = Regex(
        """Rs\.?\s*([\d,]+\.?\d*)\s+debited\s+from\s+.*?\s+on\s+\d{2}-\w{3}-\d{2}\s+(.+?)\.\s+Bal""",
        RegexOption.IGNORE_CASE
    )

    private val patternUpi = Regex(
        """debited\s+for\s+Rs\.?\s*([\d,]+\.?\d*)\s+on\s+\d{2}-\w{3}-\d{2};\s*(.+?)\s+credited""",
        RegexOption.IGNORE_CASE
    )

    private val patternGeneric = Regex(
        """(?:Rs\.?|INR)\s*([\d,]+\.?\d*)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(body: String, smsTimestamp: Long): ParsedSms? {
        val cleanBody = body.replace(Regex("""\s+"""), " ").trim()

        patternDebitCard.find(cleanBody)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val merchant = match.groupValues[2].trim().replace(Regex("""\.$"""), "")
            return ParsedSms(amount, merchant, smsTimestamp, cleanBody)
        }

        patternUpi.find(cleanBody)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val merchant = match.groupValues[2].trim()
            return ParsedSms(amount, merchant, smsTimestamp, cleanBody)
        }

        return null
    }

    fun parseWithFallback(body: String, smsTimestamp: Long): ParsedSms? {
        parse(body, smsTimestamp)?.let { return it }

        val cleanBody = body.replace(Regex("""\s+"""), " ").trim()
        if (!body.contains("debited", ignoreCase = true) && body.contains("credite", ignoreCase = true)) {
            return null
        }

        patternGeneric.find(cleanBody)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val merchant = "Unknown"
            return ParsedSms(amount, merchant, smsTimestamp, cleanBody)
        }

        return null
    }
}
