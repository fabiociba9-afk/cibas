package com.aistudio.executivogo.trnsp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.aistudio.executivogo.trnsp.data.AppUser
import com.aistudio.executivogo.trnsp.data.Trip
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WhatsAppHelper {

    private val ptBrLocale = Locale("pt", "BR")
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", ptBrLocale)
    private val timeFormat = SimpleDateFormat("HH:mm", ptBrLocale)

    /**
     * Formata a mensagem padrão da Administração/Central para o passageiro.
     */
    fun formatAdminTripNotification(trip: Trip, driver: AppUser? = null): String {
        val passenger = trip.passengerName.trim().ifBlank { "Passageiro" }
        val dateStr = if (trip.scheduledTime > 0) dateFormat.format(Date(trip.scheduledTime)) else "A combinar"
        val timeStr = if (trip.scheduledTime > 0) timeFormat.format(Date(trip.scheduledTime)) else "A combinar"
        val originStr = trip.origin.trim().ifBlank { "A definir" }
        val destinationStr = trip.destination.trim().ifBlank { "A definir" }

        val driverNameStr = trip.driverName.trim().ifBlank {
            driver?.name?.trim()?.ifBlank { "A definir pela central" } ?: "A definir pela central"
        }
        val driverPhoneStr = trip.driverPhone.trim().ifBlank {
            driver?.phone?.trim()?.ifBlank { "(Central de Atendimento)" } ?: "(Central de Atendimento)"
        }

        val vehicleModel = trip.vehicleModel.trim().ifBlank { driver?.vehicleModel?.trim() ?: "" }
        val vehicleColor = trip.vehicleColor.trim().ifBlank { driver?.vehicleColor?.trim() ?: "" }
        val vehicleStr = when {
            vehicleModel.isNotBlank() && vehicleColor.isNotBlank() -> "$vehicleModel, $vehicleColor"
            vehicleModel.isNotBlank() -> vehicleModel
            vehicleColor.isNotBlank() -> vehicleColor
            else -> "A definir"
        }

        val plateStr = trip.vehiclePlate.trim().ifBlank {
            driver?.vehiclePlate?.trim()?.ifBlank { "A definir" } ?: "A definir"
        }

        val additionalPassengersBlock = if (trip.additionalPassengers.isNotEmpty()) {
            val validAdd = trip.additionalPassengers.map { it.trim() }.filter { it.isNotBlank() }
            if (validAdd.isNotEmpty()) {
                "\n👥 Demais Passageiros: ${validAdd.joinToString(", ")}"
            } else ""
        } else ""

        val stopsBlock = if (trip.stops.isNotEmpty()) {
            val validStops = trip.stops.map { it.trim() }.filter { it.isNotBlank() }
            if (validStops.isNotEmpty()) {
                "\n🚩 Parada(s): ${validStops.joinToString(" ➔ ")}"
            } else ""
        } else ""

        return """
Olá, $passenger! 👋
Sua viagem está confirmada! 🚘

📋 DADOS DA VIAGEM
📅 Data: $dateStr
🕐 Horário: $timeStr
📍 Embarque: $originStr$stopsBlock
📍 Destino: $destinationStr$additionalPassengersBlock

👨‍✈️ MOTORISTA
Nome: $driverNameStr
📱 Telefone: $driverPhoneStr
🚗 Veículo: $vehicleStr
🔖 Placa: $plateStr

O motorista estará no local no horário programado para realizar seu atendimento.

Desejamos uma excelente viagem! 🚘
        """.trimIndent()
    }

    /**
     * Formata a mensagem padrão do Motorista para o passageiro.
     */
    fun formatDriverTripNotification(trip: Trip, driverName: String): String {
        val passenger = trip.passengerName.trim().ifBlank { "Passageiro" }
        val driver = driverName.trim().ifBlank { "Seu motorista" }
        val dateStr = if (trip.scheduledTime > 0) dateFormat.format(Date(trip.scheduledTime)) else "A combinar"
        val timeStr = if (trip.scheduledTime > 0) timeFormat.format(Date(trip.scheduledTime)) else "A combinar"
        val originStr = trip.origin.trim().ifBlank { "Local de embarque" }
        val destinationStr = trip.destination.trim().ifBlank { "Destino" }

        val stopsBlock = if (trip.stops.isNotEmpty()) {
            val validStops = trip.stops.map { it.trim() }.filter { it.isNotBlank() }
            if (validStops.isNotEmpty()) {
                "\n🚩 Parada(s): ${validStops.joinToString(" ➔ ")}"
            } else ""
        } else ""

        val additionalPassengersBlock = if (trip.additionalPassengers.isNotEmpty()) {
            val validAdd = trip.additionalPassengers.map { it.trim() }.filter { it.isNotBlank() }
            if (validAdd.isNotEmpty()) {
                "\n👥 Demais Passageiros: ${validAdd.joinToString(", ")}"
            } else ""
        } else ""

        return """
Olá, $passenger! 👋

Sou $driver, seu motorista.
Estou entrando em contato para confirmar seu atendimento:

📅 Data: $dateStr
🕐 Horário: $timeStr
📍 Embarque: $originStr$stopsBlock
📍 Destino: $destinationStr$additionalPassengersBlock

Estarei no local no horário programado para realizar seu atendimento.

Desejamos uma excelente viagem! 🚘
        """.trimIndent()
    }

    /**
     * Formata o número de telefone no padrão internacional com DDI 55 (Brasil).
     */
    fun sanitizePhoneNumber(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        if (digits.isBlank()) return ""
        return when {
            digits.startsWith("55") && digits.length in 12..13 -> digits
            digits.length in 10..11 -> "55$digits"
            else -> digits
        }
    }

    /**
     * Abre o WhatsApp com o número e o texto preenchido.
     * Se falhar ou não tiver o app, tenta via navegador e depois compartilhamento genérico.
     */
    fun openWhatsApp(context: Context, rawPhone: String, message: String) {
        val cleanPhone = sanitizePhoneNumber(rawPhone)
        val encodedMessage = try {
            URLEncoder.encode(message, "UTF-8")
        } catch (_: Exception) {
            Uri.encode(message)
        }

        val url = if (cleanPhone.isNotBlank()) {
            "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"
        } else {
            "https://api.whatsapp.com/send?text=$encodedMessage"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: Compartilhamento padrão do Android
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(shareIntent, "Enviar detalhes da viagem"))
            } catch (ex: Exception) {
                Toast.makeText(context, "Não foi possível abrir o WhatsApp", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Copia texto para a área de transferência com notificação visual.
     */
    fun copyToClipboard(context: Context, text: String, toastMessage: String = "Mensagem copiada para a área de transferência!") {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Viagem ExecutivoGo", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao copiar mensagem", Toast.LENGTH_SHORT).show()
        }
    }
}
