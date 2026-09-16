package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.entity.TripEntity
import com.example.data.entity.TripStatus
import com.example.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context matches ExecutivoGo`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ExecutivoGo", appName)
  }

  @Test
  fun `receipt generator produces valid receipt text`() {
    val sampleTrip = TripEntity(
      id = 101L,
      companyId = 1L,
      companyName = "Banco Safira",
      requesterUserId = 1L,
      requesterName = "Dr. Roberto Silveira",
      dateTimeMillis = 1710000000000L,
      origin = "Av. Paulista, 1000",
      destination = "Aeroporto de Congonhas",
      price = 280.0,
      paymentMethodId = 1L,
      paymentMethodName = "Faturado 15 Dias",
      clientPaymentDueDateMillis = 1711209600000L,
      driverId = 1L,
      driverName = "Carlos Eduardo Lima",
      driverCommissionAmount = 210.0,
      driverCommissionDueDateMillis = 1710432000000L,
      status = TripStatus.CONCLUIDA.name,
      notes = "Terminal 2",
      isClientPaid = false,
      isDriverPaid = true,
      receiptGeneratedAt = 1710000000000L
    )

    val receipt = Formatters.generateReceiptText(sampleTrip)
    assertTrue(receipt.contains("EXECUTIVOGO - RECIBO DE VIAGEM"))
    assertTrue(receipt.contains("Banco Safira"))
    assertTrue(receipt.contains("Carlos Eduardo Lima"))
    assertTrue(receipt.contains("Av. Paulista, 1000"))
  }
}
