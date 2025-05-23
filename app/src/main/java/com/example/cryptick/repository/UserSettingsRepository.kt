package com.example.cryptick.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserSettingsRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun updateLanguage(language: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return try {
            firestore.collection("users")
                .document(userId)
                .update("language", language)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateTheme(theme: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return try {
            firestore.collection("users")
                .document(userId)
                .update("theme", theme)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUserSettings(): Pair<String, String>? {
        val userId = auth.currentUser?.uid ?: return null
        return try {
            val document = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            val language = document.getString("language") ?: "EN"
            val theme = document.getString("theme") ?: "light"
            
            Pair(language, theme)
        } catch (e: Exception) {
            null
        }
    }
} 