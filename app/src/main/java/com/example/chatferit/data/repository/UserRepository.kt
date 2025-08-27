package com.example.chatferit.data.repository

import android.content.Context
import android.util.Log
import com.example.chatferit.model.UserPublicKeys
import com.example.chatferit.util.CryptoManager
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okio.IOException
import java.lang.Exception
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton





@Singleton
class UserRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val database: FirebaseDatabase,
) : iUserRepository {

    companion object {
        const val USERS_NODE = "users"
        const val PUBLIC_KEYS_PATH = "publicKeys"
    }

    override suspend fun setupUserKeysIfNeeded(userId: String): Result<UserPublicKeys> = withContext(Dispatchers.IO) {
            try {
                val userKeysRef = database.getReference(USERS_NODE).child(userId).child(PUBLIC_KEYS_PATH)
                val existingKeyResult = getUserPublicKeys(userId)
                var keysSuccessfullyValidated: UserPublicKeys? = null
                existingKeyResult.fold(
                    onSuccess = { keys ->
                        if (keys != null) {
                            try {
                                CryptoManager.getHybridPublicKeyHandle(appContext)
                                CryptoManager.getSignatureVerificationKeyHandle(appContext)
                                keysSuccessfullyValidated = keys
                            } catch (e: Exception) {
                                Log.w(
                                    "UserRepository",
                                    "Local keys issue despite Firestore record for $userId. Regenerating."
                                )
                            }
                        }
                    }, onFailure = {
                        Log.w(
                            "UserRepository",
                            "Failed to fetch existing keys for $userId, attempting generation.",
                            it
                        )
                    }
                )

                if (keysSuccessfullyValidated != null) {
                    return@withContext Result.success(keysSuccessfullyValidated!!)
                }

                Log.i("UserRepositoryImpl", "Generating new E2EE keys for user: $userId")

                val hybridPublicKeyHandle = CryptoManager.getHybridPublicKeyHandle(appContext)
                val serializedHybridPublicKey =
                    CryptoManager.serializeKeysetHandleToJson(hybridPublicKeyHandle)

                val signatureVerificationKeyHandle =
                    CryptoManager.getSignatureVerificationKeyHandle(appContext)
                val serializedSignatureVerificationKey =
                    CryptoManager.serializeKeysetHandleToJson(signatureVerificationKeyHandle)

                val publicKeysToStore = UserPublicKeys(
                    hybridPublicKey = serializedHybridPublicKey,
                    signatureVerificationKey = serializedSignatureVerificationKey
                )

                userKeysRef.setValue(publicKeysToStore).await()
                Log.i("UserRepositoryImpl", "Successfully stored new public keys for $userId in Realtime Database.")

                Result.success(publicKeysToStore)

            } catch (e: GeneralSecurityException) {
                Log.e("UserRepository", "Security exception during key setup for $userId", e)
                Result.failure(Exception("Failed to set up security keys: Cryptographic error.", e))
            } catch (e: IOException) {
                Log.e("UserRepository", "IO exception during key setup for $userId", e)
                Result.failure(
                    Exception(
                        "Failed to set up security keys: Network or storage error.",
                        e
                    )
                )
            } catch (e: Exception) {
                Log.e("UserRepository", "Unexpected error during key setup for $userId", e)
                Result.failure(
                    Exception(
                        "An unexpected error occurred while setting up security keys.",
                        e
                    )
                )
            }
        }

    override suspend fun getUserPublicKeys(userId: String): Result<UserPublicKeys?> = withContext(Dispatchers.IO){
        try {
            val userKeysRef = database.getReference(USERS_NODE).child(userId).child(PUBLIC_KEYS_PATH)
            val dataSnapshot = userKeysRef.get().await()

            if (dataSnapshot.exists()) {
                val publicKeys = dataSnapshot.getValue<UserPublicKeys>() // Deserialize from RTDB
                if (publicKeys != null) {
                    return@withContext Result.success(publicKeys)
                } else {
                    Log.w("UserRepositoryImpl", "User public keys node for $userId exists but data is null or malformed in Realtime DB.")
                    Result.success(null)
                }
            } else {
                Log.i("UserRepositoryImpl", "No public keys node found for $userId in Realtime Database.")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e("UserRepositoryImpl", "Error fetching public keys for $userId from Realtime Database", e)
            Result.failure(Exception("Failed to retrieve user public keys from Realtime Database.", e))
        }
    }
}