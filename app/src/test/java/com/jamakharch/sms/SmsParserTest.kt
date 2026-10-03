package com.jamakharch.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SmsParserTest {

    @Test
    fun `bank transfer with ampersand separator parses`() {
        val body = "ICICI Bank Acct XX407 debited with Rs 30,000.00 on 02-Sep-26 & Acct XX989 " +
            "credited.IMPS:624518374643. Call 18002662 for dispute or SMS BLOCK 407 to 9215676766"

        val parsed = SmsParser.parse(body, 0L)

        assertNotNull(parsed)
        assertEquals(30000.00, parsed!!.amount, 0.001)
        assertEquals("Bank Transfer to XX989", parsed.merchant)
    }

    @Test
    fun `second bank transfer parses`() {
        val body = "ICICI Bank Acct XX407 debited with Rs 51,000.00 on 02-Aug-26 & Acct XX624 " +
            "credited.IMPS:621409606423. Call 18002662 for dispute or SMS BLOCK 407 to 9215676766"

        val parsed = SmsParser.parse(body, 0L)

        assertNotNull(parsed)
        assertEquals(51000.00, parsed!!.amount, 0.001)
        assertEquals("Bank Transfer to XX624", parsed.merchant)
    }

    @Test
    fun `upi p2p with period separator still yields UPI Transfer`() {
        val body = "ICICI Bank Acct XXX407 debited with INR 2500.00 on 07-Jun-26. Acct XXX906 " +
            "credited.UPI:615855745941.Call 18002662 for dispute or SMS BLOCK 407 to 9215676766."

        val parsed = SmsParser.parse(body, 0L)

        assertNotNull(parsed)
        assertEquals(2500.00, parsed!!.amount, 0.001)
        assertEquals("UPI Transfer", parsed.merchant)
    }

    @Test
    fun `upi merchant payment still parses`() {
        val body = "ICICI Bank Acct XX407 debited for Rs 488.00 on 06-Jun-26; SUNTOSH CATERER " +
            "credited. UPI:307501051847. Call 18002662 for dispute. SMS BLOCK 407 to 9215676766."

        val parsed = SmsParser.parse(body, 0L)

        assertNotNull(parsed)
        assertEquals(488.00, parsed!!.amount, 0.001)
        assertEquals("SUNTOSH CATERER", parsed.merchant)
    }

    @Test
    fun `neft bank transfer with Info reference parses`() {
        val body = "ICICI Bank Acc XX407 debited Rs. 12,500.00 on 03-Oct-26 InfoIMB*INFT*0000.Avl Bal Rs. " +
            "2,58,065.90.To dispute call 18002662 or SMS BLOCK 407 to 9215676766"

        val parsed = SmsParser.parse(body, 0L)

        assertNotNull(parsed)
        assertEquals(12500.00, parsed!!.amount, 0.001)
        assertEquals("Bank Transfer InfoIMB*INFT*0000", parsed.merchant)
    }
}
