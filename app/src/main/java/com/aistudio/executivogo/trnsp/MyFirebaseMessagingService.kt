package com.aistudio.executivogo.trnsp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Collections
import java.util.LinkedHashMap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        try {
            Log.d(TAG, "Mensagem FCM recebida de: ${remoteMessage.from}")

            val title = remoteMessage.notification?.title
                ?: remoteMessage.data["title"]
                ?: "ExecutivoGo"

            val body = remoteMessage.notification?.body
                ?: remoteMessage.data["body"]
                ?: "Nova notificação corporativa."

            val tripId = remoteMessage.data["tripId"]
            val type = remoteMessage.data["type"] ?: "NEW_TRIP"
            val action = remoteMessage.data["action"] ?: "VIEW_TRIP"
            Log.d(TAG, "Notificação recebida - Título: $title, Corpo: $body, TripId: $tripId, Type: $type")

            showNotification(this, title, body, tripId, type, action)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar mensagem FCM: ${e.message}")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Novo token FCM obtido via onNewToken: $token")
        saveTokenLocally(this, token)
        syncTokenWithFirestore(this, token)
    }

    companion object {
        const val TAG = "MyFirebaseMsgService"
        const val CHANNEL_ID = "executivo_go_trips_channel"
        private const val PREFS_NAME = "executivogo_fcm_prefs"
        private const val KEY_FCM_TOKEN = "cached_fcm_token"

        // Cache sincronizado de notificações recentes para evitar alertas duplicados
        private val recentNotifications = Collections.synchronizedMap(
            object : LinkedHashMap<String, Long>(50, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                    return size > 50
                }
            }
        )

        fun showNotification(context: Context, title: String, messageBody: String, tripId: String? = null, type: String = "NEW_TRIP", action: String = "VIEW_TRIP") {
            try {
                val now = System.currentTimeMillis()
                val notifKey = "$title|$messageBody|${tripId.orEmpty()}"
                val lastShown = recentNotifications[notifKey]
                if (lastShown != null && (now - lastShown) < 7000L) {
                    Log.d(TAG, "Notificação duplicada evitada (já exibida há menos de 7s): $title")
                    return
                }
                recentNotifications[notifKey] = now

                val intent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (!tripId.isNullOrBlank()) {
                        putExtra("EXTRA_TRIP_ID", tripId)
                    }
                    putExtra("EXTRA_NOTIF_TYPE", type)
                    putExtra("EXTRA_ACTION", action)
                }

                val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

                // ID determinístico baseado na viagem e no título para que updates atualizem a mesma notificação
                val notificationId = if (!tripId.isNullOrBlank()) {
                    (tripId + title).hashCode() and 0x7FFFFFFF
                } else {
                    (title + messageBody).hashCode() and 0x7FFFFFFF
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId,
                    intent,
                    pendingIntentFlags
                )

                createNotificationChannel(context)

                // Carrega ícone oficial do aplicativo ExecutivoGo para imagem grande e ícone pequeno
                val largeIconBitmap = try {
                    BitmapFactory.decodeResource(context.resources, R.drawable.img_executivo_icon)
                        ?: BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
                } catch (e: Exception) {
                    null
                }

                val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .apply {
                        if (largeIconBitmap != null) {
                            setLargeIcon(largeIconBitmap)
                        }
                    }
                    .setColor(0xFF059669.toInt()) // Cor Emerald oficial ExecutivoGo
                    .setContentTitle(title)
                    .setContentText(messageBody)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setContentIntent(pendingIntent)

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(notificationId, notificationBuilder.build())
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao exibir notificação: ${e.message}")
            }
        }

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
                if (existingChannel == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "Notificações ExecutivoGo",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Notificações prioritárias de viagens, despachos e faturamento"
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 400, 200, 400)
                    }
                    notificationManager.createNotificationChannel(channel)
                }
            }
        }

        fun saveTokenLocally(context: Context, token: String) {
            if (token.isBlank()) return
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
        }

        fun getCachedToken(context: Context): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_FCM_TOKEN, null)
        }

        fun syncTokenWithFirestore(context: Context? = null, token: String, explicitUid: String? = null) {
            val targetUid = explicitUid?.takeIf { it.isNotBlank() }
                ?: FirebaseAuth.getInstance().currentUser?.uid

            if (targetUid.isNullOrBlank() || token.isBlank()) {
                Log.d(TAG, "syncTokenWithFirestore postergado: targetUid=$targetUid, tokenVazio=${token.isBlank()}")
                return
            }

            val updates = mapOf(
                "fcmToken" to token,
                "fcmTokenAndroid" to token,
                "updatedAtFCM" to FieldValue.serverTimestamp()
            )

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(targetUid)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Token FCM sincronizado no Firestore para usuário $targetUid: $token")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Erro ao sincronizar token FCM: ${e.message}")
                }
        }

        fun fetchAndSyncToken(context: Context, explicitUid: String? = null, onTokenReceived: ((String) -> Unit)? = null) {
            createNotificationChannel(context)
            val targetUid = explicitUid?.takeIf { it.isNotBlank() }
                ?: FirebaseAuth.getInstance().currentUser?.uid

            val cachedToken = getCachedToken(context)
            val fallbackToken = cachedToken ?: run {
                val newFallback = "fcm_android_${targetUid?.take(8) ?: "guest"}_${System.currentTimeMillis()}"
                saveTokenLocally(context, newFallback)
                newFallback
            }

            if (!targetUid.isNullOrBlank()) {
                syncTokenWithFirestore(context, fallbackToken, targetUid)
                onTokenReceived?.invoke(fallbackToken)
            }

            // Tenta obter o token real do Firebase Cloud Messaging em background (caso GCP/Play Services esteja disponível)
            try {
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        if (!token.isNullOrBlank()) {
                            Log.d(TAG, "FCM Token obtido do FirebaseMessaging: $token")
                            saveTokenLocally(context, token)
                            if (!targetUid.isNullOrBlank()) {
                                syncTokenWithFirestore(context, token, targetUid)
                            }
                            onTokenReceived?.invoke(token)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.d(TAG, "FCM token retrieval unavailable (using local fallback): ${e.message}")
                    }
            } catch (e: Exception) {
                Log.d(TAG, "FCM token exception handled: ${e.message}")
            }
        }
    }
}
