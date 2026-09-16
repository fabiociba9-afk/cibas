package com.example.data.firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

/**
 * Utilitário central para acesso aos serviços do Firebase (Auth e Firestore)
 */
object FirebaseManager {
    private const val TAG = "FirebaseManager"

    val auth: FirebaseAuth
        get() = Firebase.auth

    val firestore: FirebaseFirestore
        get() = Firebase.firestore

    fun isConfigured(): Boolean {
        return try {
            auth.app != null && firestore.app != null
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar serviços Firebase", e)
            false
        }
    }
}
