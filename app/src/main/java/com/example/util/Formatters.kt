package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.entity.TripEntity
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

    fun generateReceiptText(trip: TripEntity): String {
        return """
            ====================================
               EXECUTIVOGO - RECIBO DE VIAGEM
            ====================================
            Recibo Nº: #TRIP-${trip.id}
            Data: ${formatDateTime(trip.dateTimeMillis)}
            
            [EMPRESA CLIENTE]
            Empresa: ${trip.companyName}
            Solicitante: ${trip.requesterName}
            
            [DETALHES DO TRAJETO]
            Origem: ${trip.origin}
            Destino: ${trip.destination}
            
            [MOTORISTA & VEÍCULO]
            Motorista: ${trip.driverName}
            
            [PAGAMENTO]
            Forma de Pagamento: ${trip.paymentMethodName}
            Prazo de Pagamento: ${formatDate(trip.clientPaymentDueDateMillis)}
            Valor Total: ${formatCurrency(trip.price)}
            Status da Viagem: ${trip.status}
            
            Observações: ${if (trip.notes.isNotBlank()) trip.notes else "Nenhuma"}
            ====================================
            ExecutivoGo Transportes Corporativos
            www.executivogo.com.br
            ====================================
        """.trimIndent()
    }

    fun shareReceiptWhatsApp(context: Context, trip: TripEntity) {
        val text = generateReceiptText(trip)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.whatsapp")
        }
        try {
            context.startActivity(sendIntent)
        } catch (e: Exception) {
            // Fallback to standard share sheet if WhatsApp app is not installed
            val genericIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_SUBJECT, "Recibo de Viagem ExecutivoGo #${trip.id}")
            }
            context.startActivity(Intent.createChooser(genericIntent, "Compartilhar Recibo ExecutivoGo"))
        }
    }

    fun shareReceiptGeneric(context: Context, trip: TripEntity) {
        val text = generateReceiptText(trip)
        val genericIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "Recibo ExecutivoGo #${trip.id}")
        }
        context.startActivity(Intent.createChooser(genericIntent, "Enviar Recibo"))
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
