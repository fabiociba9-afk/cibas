package com.aistudio.executivogo.trnsp.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object NavigationHelper {

    fun openWaze(context: Context, address: String) = openWazeNavigation(context, address)
    fun openGoogleMaps(context: Context, address: String) = openGoogleMapsNavigation(context, address)

    /**
     * Tenta abrir no Waze. Se não estiver instalado, faz fallback para Google Maps ou Navegador.
     */
    fun openWazeNavigation(context: Context, address: String) {
        val encoded = Uri.encode(address)
        val wazeUri = Uri.parse("waze://?q=$encoded&navigate=yes")
        val wazeIntent = Intent(Intent.ACTION_VIEW, wazeUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(wazeIntent)
        } catch (e: Exception) {
            // Fallback para Google Maps
            Toast.makeText(context, "Waze não encontrado. Abrindo Google Maps...", Toast.LENGTH_SHORT).show()
            openGoogleMapsNavigation(context, address)
        }
    }

    /**
     * Tenta abrir no Google Maps. Se não conseguir, abre no navegador.
     */
    fun openGoogleMapsNavigation(context: Context, address: String) {
        val encoded = Uri.encode(address)
        val mapsUri = Uri.parse("google.navigation:q=$encoded")
        val mapsIntent = Intent(Intent.ACTION_VIEW, mapsUri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(mapsIntent)
        } catch (e: Exception) {
            try {
                val genericGeo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(genericGeo)
            } catch (ex: Exception) {
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$encoded")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(browserIntent)
                } catch (lastEx: Exception) {
                    Toast.makeText(context, "Nenhum aplicativo de navegação ou navegador disponível.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Disca ou abre ligação com o telefone informado
     */
    fun dialPhone(context: Context, phone: String) {
        val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
        if (cleanPhone.isBlank()) {
            Toast.makeText(context, "Telefone não informado", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Não foi possível abrir o discador.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Abre WhatsApp com o passageiro
     */
    fun openWhatsApp(context: Context, phone: String, message: String = "Olá! Sou seu motorista executivo.") {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        if (cleanPhone.isBlank()) {
            Toast.makeText(context, "Telefone não informado", Toast.LENGTH_SHORT).show()
            return
        }
        val fullPhone = if (cleanPhone.startsWith("55")) cleanPhone else "55$cleanPhone"
        val encodedMsg = Uri.encode(message)
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$fullPhone&text=$encodedMsg")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp não disponível.", Toast.LENGTH_SHORT).show()
        }
    }
}