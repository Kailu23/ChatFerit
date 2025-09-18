package com.example.chatferit.data.repository

import android.content.Context
import android.util.Log
import com.example.chatferit.model.UserProfile
import com.example.chatferit.model.UserPublicKeys
import com.example.chatferit.util.CryptoManager
import com.example.chatferit.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okio.IOException
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class UserRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val database: FirebaseDatabase,
    private val cryptoManager: CryptoManager,
    private val firebaseAuth: FirebaseAuth
) : iUserRepository {

    private val usersRef: DatabaseReference = database.getReference("users")
    companion object {
        const val USERS_NODE = "users"
        const val PUBLIC_KEYS_PATH = "publicKeys"
    }

    override suspend fun setupUserKeysIfNeeded(userId: String): Result<UserPublicKeys> = withContext(Dispatchers.IO) {
        try {
            Log.d("UserRepository", "Starting setupUserKeysIfNeeded for $userId")

            val currentLocalHybridPublicKey: String
            val currentLocalSignaturePublicKey: String

            try {
                currentLocalHybridPublicKey = cryptoManager.getLocalUserHybridPublicKey()
                currentLocalSignaturePublicKey = cryptoManager.getLocalUserSignaturePublicKey()
                Log.d(
                    "UserRepository",
                    "Current local hybrid PK for $userId (first 30): ${
                        currentLocalHybridPublicKey.take(30)
                    }"
                )
            } catch (e: Exception) {
                Log.e(
                    "UserRepository",
                    "Failed to get/generate local keys for $userId via CryptoManager",
                    e
                )
                return@withContext Result.failure(
                    Exception(
                        "Couldn't initialize local cryptographic keys.",
                        e
                    )
                )
            }

            val userKeysRef =
                database.getReference(USERS_NODE).child(userId).child(PUBLIC_KEYS_PATH)
            val snapshot = userKeysRef.get().await()
            val firebasePublicKeys: UserPublicKeys? = if (snapshot.exists()) {
                try {
                    snapshot.getValue<UserPublicKeys>()
                } catch (e: Exception) {
                    Log.w(
                        "UserRepository",
                        "Error deserializing public keys for $userId from Realtime Database. Treating as no keys exist.",
                        e
                    )
                    null
                }
            } else {
                null
            }

            if (firebasePublicKeys != null && firebasePublicKeys.hybridPublicKey == currentLocalHybridPublicKey &&
                firebasePublicKeys.signatureVerificationKey == currentLocalSignaturePublicKey
            ) {
                Log.i(
                    "UserRepository",
                    "Local keys for $userId match Firebase records. Keys are usable. "
                )
                return@withContext Result.success(firebasePublicKeys)
            } else {
                if (firebasePublicKeys == null) {
                    Log.i(
                        "UserRepository",
                        "No public keys node found for $userId in Realtime Database. "
                    )

                } else {
                    Log.w(
                        "UserRepository",
                        "Firebase keys for $userId are STALE or mismatched. Updating with current local keys."
                    )
                }

                val newPublicKeysToStore = UserPublicKeys(
                    hybridPublicKey = currentLocalHybridPublicKey,
                    signatureVerificationKey = currentLocalSignaturePublicKey
                )

                userKeysRef.setValue(newPublicKeysToStore).await()
                Log.i(
                    "UserRepository",
                    "Successfully updated public keys for $userId in Realtime Database."
                )
                return@withContext Result.success(newPublicKeysToStore)

            }
        } catch (e: DatabaseException) {
            Log.e("UserRepository", "Firebase Database exception during key setup for $userId", e)
            return@withContext Result.failure(
                Exception(
                    "A Firebase error occurred during security key setup.",
                    e
                )
            )
        } catch (e: GeneralSecurityException) {
            Log.e("UserRepository", "GeneralSecurityException during key setup for $userId", e)
            return@withContext Result.failure(
                Exception(
                    "A cryptographic security error occurred during key setup.",
                    e
                )
            )
        } catch (e: IOException) {
            Log.e(
                "UserRepository",
                "IOException during key setup for $userId (possibly network related for Firebase ops)",
                e
            )
            return@withContext Result.failure(
                Exception(
                    "A network or storage error occurred during security key setup.",
                    e
                )
            )
        } catch (e: Exception) {
            Log.e(
                "UserRepository",
                "Unexpected Exception during key setup for $userId. Type: ${e.javaClass.simpleName}",
                e
            )
            return@withContext Result.failure(
                Exception(
                    "An unexpected error occurred during security key setup: ${e.message}",
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
                    Log.w("UserRepository", "User public keys node for $userId exists but data is null or malformed in Realtime DB.")
                    Result.success(null)
                }
            } else {
                Log.i("UserRepository", "No public keys node found for $userId in Realtime Database.")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error fetching public keys for $userId from Realtime Database", e)
            Result.failure(Exception("Failed to retrieve user public keys from Realtime Database.", e))
        }
    }

    override suspend fun searchUsers(query: String): Resource<List<UserProfile>> {
            if(query.isBlank() || query.length < 3) return Resource.Success(emptyList())

            val normalizedQuery = query.lowercase().trim()
            val foundUsers = mutableSetOf<UserProfile>()

        return try {
            val allUsersSnapshot = usersRef.get().await()

            allUsersSnapshot.children.forEach { dataSnapshot ->
                val userUid = dataSnapshot.key
                dataSnapshot.getValue(UserProfile::class.java)?.let { user ->
                    if (userUid != null) {
                        val userProfileWithUid = user.copy(uid = userUid)
                        val userEmailLower = userProfileWithUid.email?.lowercase() ?: ""
                        val userNameLower = userProfileWithUid.displayName.lowercase()

                        if (userEmailLower.contains(normalizedQuery) || userNameLower.contains(
                                normalizedQuery
                            )
                        ) {
                            foundUsers.add(user.copy(uid = dataSnapshot.key ?: ""))
                        }
                    }
                }
            }
            val currentUserId = firebaseAuth.currentUser?.uid ?: ""
            val resultList = foundUsers.filterNot { it.uid == currentUserId }.toList()
                .sortedBy { it.displayName }
            Resource.Success(resultList)
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Firebase user search failed", e)
            return Resource.Success(emptyList())
        }
    }
}