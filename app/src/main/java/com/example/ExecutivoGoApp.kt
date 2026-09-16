package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class ExecutivoGoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            Log.d("ExecutivoGoApp", "Firebase inicializado com sucesso.")
        } catch (e: Exception) {
            Log.w("ExecutivoGoApp", "Aviso ao inicializar Firebase: ${e.message}")
        }
    }
}
