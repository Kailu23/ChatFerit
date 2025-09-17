package com.example.chatferit.feature.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatferit.data.repository.IAuthRepository
import com.example.chatferit.data.repository.UserRepository
import com.example.chatferit.model.UserProfile
import com.example.chatferit.model.UserPublicKeys
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val database: FirebaseDatabase,
    private val authRepository: IAuthRepository
) : ViewModel(){
    private val _currentUser: StateFlow<FirebaseUser?> = authRepository.getAuthStateFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = authRepository.getCurrentUser()
    )
    val currentUser : StateFlow<FirebaseUser?> = _currentUser

    private val _keySetupState = MutableStateFlow<KeySetupState>(KeySetupState.Idle)
    val keySetupState : StateFlow<KeySetupState> = _keySetupState.asStateFlow()

    private val _authScreenState = MutableStateFlow<AuthScreenState>(AuthScreenState.Idle)
    val authScreenState : StateFlow<AuthScreenState> = _authScreenState.asStateFlow()

    init {
        viewModelScope.launch {
            currentUser.collect{user ->
                if (user != null) {
                    Log.d(
                        "AuthViewModel",
                        "User (${user.uid}) logged in. Calling performKeySetupIfNeeded."
                    )
                    performKeySetupIfNeededAndObserve(user.uid)
                } else {
                    Log.d("AuthViewModel", "User logged out. Resetting _keySetupState to Idle.")
                    _keySetupState.value = KeySetupState.Idle
                    _authScreenState.value = AuthScreenState.Idle
                }
            }
        }
    }

    private suspend fun internalSetupUserKeys(userId: String): Result<UserPublicKeys> {
        Log.d("AuthViewModel", "Starting internal key setup for user (suspend): $userId")
        return userRepository.setupUserKeysIfNeeded(userId)
    }

    private fun performKeySetupIfNeededAndObserve(userId: String) {
        if (_keySetupState.value is KeySetupState.Loading || _keySetupState.value is KeySetupState.Success) {

            val successState = keySetupState.value as? KeySetupState.Success
            if (successState == null) {
                Log.d("AuthViewModel", "Key setup already loading or successful, skipping.")
                return
            }
        }

        _keySetupState.value = KeySetupState.Loading
        Log.d("AuthViewModel", "Starting key setup for user: $userId")
        viewModelScope.launch {
            val setupResult = userRepository.setupUserKeysIfNeeded(userId)
            setupResult.fold(
                onSuccess = {keys ->
                    Log.i("AuthViewModel", "Key setup successful for user: $userId")
                    _keySetupState.value = KeySetupState.Success(keys)
                },
                onFailure = {exception ->
                    Log.e("AuthViewModel", "Key setup failed for user $userId: ${exception.message}", exception)
                    _keySetupState.value = KeySetupState.Error(exception.message ?: "Unknown error")
                }
            )
        }
    }

    fun signUpWithEmailAndPassword(name: String, surname: String, email: String, password: String) {
        if(_authScreenState.value is AuthScreenState.Loading) return
        _authScreenState.value = AuthScreenState.Loading
        val fullName = "$name $surname"
        Log.d("AuthViewModel", "Attempting sign up with email: $email, name: $fullName")

        viewModelScope.launch {
            try {
                val authResult =
                    firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                    ?: throw IllegalStateException("Firebase user was null after successful account creation.")
                Log.i("AuthViewModel", "Firebase Auth user created: ${firebaseUser.uid}")

                 val profileUpdates = UserProfileChangeRequest.Builder()
                     .setDisplayName(fullName)
                     .build()
                firebaseUser.updateProfile(profileUpdates).await()

                Log.i("AuthViewModel", "Firebase user profile updated: ${firebaseUser.uid}")

                val profileAndKeySetupResult = createUserProfileInRtdb(firebaseUser, fullName)

                if (profileAndKeySetupResult.isSuccess) {
                    Log.i("AuthViewModel", "Sign up, profile creation and key setup successful for user: ${firebaseUser.uid}.")
                    _authScreenState.value = AuthScreenState.AuthSuccess(firebaseUser)
                } else {
                    val error = profileAndKeySetupResult.exceptionOrNull()
                    Log.e("AuthViewModel", "Failed during profile/key setup for ${firebaseUser.uid}: ${error?.message}", error)
                    _authScreenState.value = AuthScreenState.AuthError(error?.message ?: "Profile or Key setup failed.")
                    firebaseUser.delete().await()
                    Log.w("AuthViewModel", "Partially created Firebase Auth user ${firebaseUser.uid} has been deleted due to setup failure.")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign up exception for email $email: ${e.message}", e)
                _authScreenState.value = AuthScreenState.AuthError(e.message?: "Sign up failed")
            }
        }
    }

    fun signInWithEmailAndPassword(email: String, password: String) {
        if(_authScreenState.value is AuthScreenState.Loading) return
        _authScreenState.value = AuthScreenState.Loading
        Log.d("AuthViewModel", "Attempting sign in with email: $email")
        viewModelScope.launch {
            try {
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                authResult.user?.let { firebaseUser ->
                    Log.i(
                        "AuthViewModel",
                        "Sign in successful for user: ${firebaseUser.uid}. Email: $email"
                    )
                    _authScreenState.value = AuthScreenState.AuthSuccess(firebaseUser)
                } ?: run {
                    Log.e("AuthViewModel", "Sign in failed: Firebase user is null. Email: $email")
                    _authScreenState.value =
                        AuthScreenState.AuthError("Sign in failed: User is null.")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign in exception for email $email: ${e.message}", e)
                _authScreenState.value = AuthScreenState.AuthError(e.message?:"Sign in failed.")
            }
        }
    }

    private suspend fun createUserProfileInRtdb(firebaseUser: FirebaseUser, fullName: String): Result<UserPublicKeys> {
        val userId = firebaseUser.uid
        val userEmail = firebaseUser.email ?: ""
        val userProfileRef = database.getReference("users").child(userId)

        val newUserProfile = UserProfile(
            uid = userId,
            displayName = fullName,
            email = userEmail,
            createdAt = System.currentTimeMillis()
        )
        val profileCreationDeferred = CompletableDeferred<Result<Unit>>()

        userProfileRef.setValue(newUserProfile)
            .addOnSuccessListener {
                Log.i("AuthViewModel", "User profile created in RTDB for $userId")
                profileCreationDeferred.complete(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                Log.e("AuthViewModel", "Failed to create user profile in RTDB for $userId", e)
                _authScreenState.value = AuthScreenState.AuthError("Failed to save profile.")
                profileCreationDeferred.complete(Result.failure(e))
            }

        val profileResult = profileCreationDeferred.await()
        if (profileResult.isFailure) {
            return Result.failure(profileResult.exceptionOrNull() ?: Exception("Failed to create profile in RTDB."))
        }

        Log.i("AuthViewModel", "Profile created for $userId, proceeding to key setup.")

        val keyResult = internalSetupUserKeys(userId)

        if (keyResult.isSuccess) {
            keyResult.getOrNull()?.let { keys ->
                _keySetupState.value = KeySetupState.Success(keys)
            }
        } else {
            _keySetupState.value = KeySetupState.Error(keyResult.exceptionOrNull()?.message ?: "Key setup failed during sign up.")
        }
        return keyResult
    }
    fun resetAuthScreenState() {
        _authScreenState.value = AuthScreenState.Idle
    }
}