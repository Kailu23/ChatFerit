package com.example.chatferit.model

data class FriendRequest(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderProfileImageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
