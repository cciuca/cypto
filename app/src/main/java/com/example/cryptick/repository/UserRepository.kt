package com.example.cryptick.repository

import com.example.cryptick.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            // Primero autenticamos con Firebase Auth
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            
            // Si la autenticación es exitosa, buscamos los datos del usuario en Firestore
            val userId = authResult.user?.uid ?: throw Exception("Error de autenticación")
            
            // Buscamos el documento del usuario usando su ID
            val documentSnapshot = usersCollection.document(userId).get().await()

            if (!documentSnapshot.exists()) {
                Result.failure(Exception("Datos de usuario no encontrados"))
            } else {
                val user = documentSnapshot.toObject(User::class.java)
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(Exception("Error al convertir los datos del usuario"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de autenticación: ${e.message}"))
        }
    }

    suspend fun registerUser(email: String, password: String, username: String): Result<User> {
        return try {
            // Crear usuario en Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("Error al crear usuario")

            // Crear objeto de usuario para Firestore
            val user = User(
                id = userId,
                email = email,
                user = username,
                favorites = emptyList(),
                image = "https://img.freepik.com/premium-vector/avatar-profile-icon-flat-style-male-user-profile-vector-illustration_157943-38764.jpg?semt=ais_hybrid&w=740",
                language = "EN",
                theme = "light"
            )

            // Guardar datos del usuario en Firestore
            usersCollection.document(userId).set(user).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception("Error al registrar usuario: ${e.message}"))
        }
    }

    suspend fun updateFavorites(userId: String, favorites: List<String>): Result<Unit> {
        return try {
            usersCollection.document(userId)
                .update("favorites", favorites)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Error al actualizar favoritos: ${e.message}"))
        }
    }

    suspend fun getFavorites(userId: String): Result<List<String>> {
        return try {
            val documentSnapshot = usersCollection.document(userId).get().await()
            val user = documentSnapshot.toObject(User::class.java)
            Result.success(user?.favorites ?: emptyList())
        } catch (e: Exception) {
            Result.failure(Exception("Error al obtener favoritos: ${e.message}"))
        }
    }

    fun getCurrentUser() = auth.currentUser

    fun logout() {
        auth.signOut()
    }
} 