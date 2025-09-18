package com.example.chatferit.data.repository

import com.example.chatferit.model.UserProfile
import com.example.chatferit.model.UserPublicKeys
import com.example.chatferit.util.Resource

interface iUserRepository {
    /**
     * Checks if E2EE keys are set up for the given user. If not, generates them,
     * stores private keys securely, and saves public keys to the backend.
     *
     * @param userId The ID of the user.
     * @return Result containing UserPublicKeys on success, or an Exception on failure.
     */
    suspend fun setupUserKeysIfNeeded(userId: String): Result<UserPublicKeys>

    /**
     * Retrieves the public E2EE keys for a given user from the backend.
     *
     * @param userId The ID of the user whose public keys are to be fetched.
     * @return Result containing UserPublicKeys if found and valid, null if not found, or an Exception on failure.
     */
    suspend fun getUserPublicKeys(userId: String) : Result<UserPublicKeys?>

    suspend fun searchUsers(query: String) : Resource<List<UserProfile>>
    //Future implementation
//    suspend fun getCurrentUserProfile() : Result<UserProfile> TODO(): Profile in settings

}