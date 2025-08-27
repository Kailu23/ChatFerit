package com.example.chatferit.feature.auth

import com.example.chatferit.model.UserPublicKeys
import com.google.firebase.auth.FirebaseUser


sealed class KeySetupState{
    object Idle: KeySetupState()
    object Loading: KeySetupState()
    data class Success(val keys: UserPublicKeys): KeySetupState()
    data class Error(val message: String): KeySetupState()
}

sealed class AuthScreenState {
    object Idle: AuthScreenState()
    object Loading: AuthScreenState()
    data class AuthSuccess(val user: FirebaseUser): AuthScreenState()
    data class AuthError(val message: String): AuthScreenState()
}