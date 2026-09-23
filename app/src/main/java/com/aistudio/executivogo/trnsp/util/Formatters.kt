package com.aistudio.executivogo.trnsp.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object Formatters {

    private val ptBrLocale = Locale("pt", "BR")
    private val currencyFormat = NumberFormat.getCurrencyInstance(ptBrLocale)
    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", ptBrLocale)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", ptBrLocale)

    fun formatCurrency(value: Double): String {
        return currencyFormat.format(value)
    }

    fun formatDateTime(millis: Long): String {
        return dateTimeFormat.format(Date(millis))
    }

    fun formatDate(millis: Long): String {
        return dateFormat.format(Date(millis))
    }

    fun generateReceiptText(
        tripId: String,
        dateTimeMillis: Long,
        companyName: String,
        passengerName: String,
        passengerPhone: String,
        origin: String,
        destination: String,
        driverName: String,
        vehiclePlate: String,
        paymentMethodName: String,
        clientPaymentDueDateMillis: Long,
        price: Double,
        status: String,
        notes: String
    ): String {
        return """
            ====================================
               EXECUTIVOGO - RECIBO DE VIAGEM
            ====================================
            Recibo Nº: #TRIP-${tripId.take(8)}
            Data: ${formatDateTime(dateTimeMillis)}
            
            [EMPRESA CLIENTE]
            Empresa: $companyName
            Passageiro: $passengerName (Tel: $passengerPhone)
            
            [DETALHES DO TRAJETO]
            Origem: $origin
            Destino: $destination
            
            [MOTORISTA & VEÍCULO]
            Motorista: $driverName ($vehiclePlate)
            
            [PAGAMENTO]
            Forma de Pagamento: $paymentMethodName
            Prazo de Pagamento: ${formatDate(clientPaymentDueDateMillis)}
            Valor Total: ${formatCurrency(price)}
            Status da Viagem: $status
            
            Observações: ${if (notes.isNotBlank()) notes else "Nenhuma"}
            ====================================
            ExecutivoGo Transportes Corporativos
            www.executivogo.com.br
            ====================================
        """.trimIndent()
    }

    fun openInGoogleMaps(context: Context, latitude: Double, longitude: Double, label: String) {
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($label)")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.setPackage("com.google.android.apps.maps")
        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
            context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    }
}