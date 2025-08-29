package com.example.chatferit.data.repository

import android.util.Log
import com.example.chatferit.model.FriendRequest
import com.example.chatferit.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class FriendRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseDatabase: FirebaseDatabase
) : iFriendRepository {

    companion object {
        private const val TAG = "FriendRepository"
        private const val NODE_FRIEND_REQUESTS = "friend_requests"
        private const val NODE_USERS = "users"
        private const val NODE_FRIENDS = "friends"
    }

    override suspend fun sendFriendRequest(
        currentUserId: String,
        targetUserId: String
    ): Resource<Unit> {
        if (currentUserId == targetUserId) {
            return Resource.Error("Cannot send a friend request to yourself.")
        }
        return try {
            val currentUserProfileSnapshot =
                firebaseDatabase.getReference(NODE_USERS)
                    .child(currentUserId)
                    .get().await()
            val currentUserName : String = currentUserProfileSnapshot.child("displayName").getValue(String::class.java) ?: "A user"

           val currentUserImageSnapshot =
                firebaseDatabase.getReference(NODE_USERS).child(currentUserId)
                    .child("profileImageUrl").get().await()
            val currentUserImageUrl = currentUserImageSnapshot.value as? String ?: ""

            val requestId = UUID.randomUUID().toString()
            val friendRequest = FriendRequest(
                id = requestId,
                senderId = currentUserId,
                senderName = currentUserName,
                senderProfileImageUrl = currentUserImageUrl,
                timestamp = System.currentTimeMillis()
            )

            firebaseDatabase.getReference(NODE_FRIEND_REQUESTS)
                .child(targetUserId)
                .child(currentUserId)
                .setValue(friendRequest)
                .await()
            Log.d(TAG, "Friend request sent from ${currentUserId} to $targetUserId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending friend request", e)
            Resource.Error(e.message ?: "Failed to send friend request")
        }
    }

    override fun getIncomingFriendRequests(currentUserId: String): Flow<Resource<List<FriendRequest>>> = callbackFlow {
        val requestsRef = firebaseDatabase.getReference(NODE_FRIEND_REQUESTS).child(currentUserId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val requests = mutableListOf<FriendRequest>()
                snapshot.children.forEach { requestSnapshot ->
                    try {
                        val request = requestSnapshot.getValue(FriendRequest::class.java)
                        request?.let { requests.add(it.copy(id = requestSnapshot.key ?: it.id)) }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deserializing friend request: ${requestSnapshot.key}", e)
                        trySend(Resource.Error("Error parsing friend requests: ${e.message}"))
                        return@forEach
                    }
                }
                Log.d(TAG, "Incoming friend requests for $currentUserId: ${requests.size}")
                trySend(Resource.Success(requests.sortedByDescending { it.timestamp }))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Friend request listener cancelled for $currentUserId", error.toException())
                trySend(Resource.Error(error.message))
                close(error.toException())
            }
        }
        requestsRef.addValueEventListener(listener)
        awaitClose { requestsRef.removeEventListener(listener) }
    }

    override suspend fun acceptFriendRequest(
        currentUserId: String,
        request: FriendRequest
    ): Resource<Unit> {
        if (request.senderId == currentUserId) {
            return Resource.Error("Sender ID is missing in the request.")
        }
        return try {
            val updates = mutableMapOf<String, Any?>()

            updates["/$NODE_USERS/${request.senderId}/$NODE_FRIENDS/${currentUserId}"] = true
            updates["/$NODE_USERS/${currentUserId}/$NODE_FRIENDS/${request.senderId}"] = true
            updates["/$NODE_FRIEND_REQUESTS/${currentUserId}/${request.senderId}"] = null

            Log.d(TAG, "Accepting request. Current User (Acceptor): $currentUserId, Original Sender: ${request.senderId}")
            Log.d(TAG, "Updates to be performed: $updates")

            firebaseDatabase.reference.updateChildren(updates).await()
            Log.d(TAG, "Friend request from ${request.senderName} accepted by $currentUserId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting friend request", e)
            Resource.Error(e.message ?: "Failed to decline friend request.")
        }
    }

    override suspend fun declineFriendRequest(
        currentUserId: String,
        request: FriendRequest
    ): Resource<Unit> {
        if (request.senderId.isBlank()) {
            return Resource.Error("Sender ID is missing in the request.")
        }
        return try {
            firebaseDatabase.getReference(NODE_FRIEND_REQUESTS)
                .child(currentUserId)
                .child(request.senderId)
                .removeValue()
                .await()
            Log.d(TAG, "Friend request from ${request.senderName} declined by $currentUserId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error declining friend request", e)
            Resource.Error(e.message ?: "Failed to decline friend request.")
        }
    }

    override fun getFriendUids(currentUserId: String): Flow<Resource<Set<String>>> = callbackFlow {
        val friendsRef = firebaseDatabase.getReference(NODE_USERS).child(currentUserId).child(NODE_FRIENDS)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val uids = mutableSetOf<String>()
                snapshot.children.forEach { friendUuidSnapshot ->
                friendUuidSnapshot.key?.let { uids.add(it) }
                }
                Log.d(TAG, "Friend UIDs for $currentUserId: $uids")
                trySend(Resource.Success(uids))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Friend UIDs listener cancelled for $currentUserId", error.toException())
                trySend(Resource.Error(error.message))
                close(error.toException())
            }
        }
        friendsRef.addValueEventListener(listener)
        awaitClose { friendsRef.removeEventListener(listener) }
    }
}

