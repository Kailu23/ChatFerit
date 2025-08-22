package com.example.chatferit.model

data class Message(
    val id : String = "",
    val senderId : String? = "",
    val message : String = "",
    val senderName : String? = "",
    val senderImage : String? = null,
    val imageUrl : String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
