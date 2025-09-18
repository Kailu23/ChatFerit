package com.example.chatferit.data.repository

import com.example.chatferit.model.FriendRequest
import com.example.chatferit.util.Resource
import kotlinx.coroutines.flow.Flow

interface iFriendRepository {

    /**
     * Sends a friend request from the current user to the target user.
     * @param currentUserId The UID of the user sending the request.
     * @param targetUserId The UID of the user to receive the request.
     * @return Resource<Unit> indicating success or failure.
     */
    suspend fun sendFriendRequest(currentUserId: String, targetUserId: String): Resource<Unit>

    /**
     * Listens for incoming friend requests for the current user.
     * @param currentUserId The UID of the current user.
     * @return A Flow emitting a list of FriendRequest objects or an error.
     */
    fun getIncomingFriendRequests(currentUserId: String): Flow<Resource<List<FriendRequest>>>

    /**
     * Accepts a friend request.
     * This should:
     * 1. Add each user to the other's friend list in the database.
     * 2. Delete the friend request document.
     * @param currentUserId The UID of the user accepting the request.
     * @param request The FriendRequest object to accept.
     * @return Resource<Unit> indicating success or failure.
     */
    suspend fun acceptFriendRequest(currentUserId: String, request: FriendRequest): Resource<Unit>

    /**
     * Declines/Cancels a friend request.
     * This should delete the friend request document.
     * @param currentUserId The UID of the user declining/cancelling.
     * @param request The FriendRequest object to decline/cancel.
     * @return Resource<Unit> indicating success or failure.
     */
    suspend fun declineFriendRequest(currentUserId: String, request: FriendRequest): Resource<Unit>

    /**
     * Listens for the UIDs of the current user's friends.
     * @param currentUserId The UID of the current user.
     * @return A Flow emitting a set of friend UIDs or an error.
     */
    fun getFriendUids(currentUserId: String): Flow<Resource<Set<String>>>

    // suspend fun findUserByEmail(email: String): Resource<String?>
    // suspend fun findUserByUsername(username: String): Resource<String?>
}