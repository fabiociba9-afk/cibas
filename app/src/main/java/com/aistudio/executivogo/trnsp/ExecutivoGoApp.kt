package com.aistudio.executivogo.trnsp

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

class ExecutivoGoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Inicializa os serviços do Firebase
        FirebaseApp.initializeApp(this)
        try {
            FirebaseMessaging.getInstance().isAutoInitEnabled = false
        } catch (_: Exception) {}
    }
}