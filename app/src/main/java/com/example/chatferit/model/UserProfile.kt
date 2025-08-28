package com.example.chatferit.model

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val createdAt: Long = 0L,

    val publicKeys: Map<String, String>? = null
)