package com.example.chatferit.data.repository

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface IAuthRepository {
    fun getAuthStateFlow(): Flow<FirebaseUser?>
    fun getCurrentUser(): FirebaseUser?
}