package com.example

import com.aistudio.executivogo.trnsp.data.AppUser
import com.aistudio.executivogo.trnsp.data.PaymentTerms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testDriverOnlineStatusField() {
        val user = AppUser(name = "Carlos", isOnline = true)
        assertTrue(user.isOnline)
    }

    @Test
    fun testTripCommissionCalculation100Percent() {
        val price = 900.0
        val commissionPercentage = 100.0
        val driverCommission = price * (commissionPercentage / 100.0)
        assertEquals(900.0, driverCommission, 0.001)
    }

    @Test
    fun testTripCommissionCalculation70Percent() {
        val price = 900.0
        val commissionPercentage = 70.0
        val driverCommission = price * (commissionPercentage / 100.0)
        assertEquals(630.0, driverCommission, 0.001)
    }

    @Test
    fun testPaymentTermsDays() {
        assertEquals(0, PaymentTerms.getDays(PaymentTerms.A_VISTA))
        assertEquals(7, PaymentTerms.getDays(PaymentTerms.D7))
        assertEquals(15, PaymentTerms.getDays(PaymentTerms.D15))
        assertEquals(30, PaymentTerms.getDays(PaymentTerms.D30))
        assertEquals(60, PaymentTerms.getDays(PaymentTerms.D60))
    }
}
