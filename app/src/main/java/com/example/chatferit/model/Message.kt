package com.example.chatferit.model

data class Message(
    val id: String = "",
    val senderId: String? = "",
    val senderName: String? = "",
    val senderImage: String? = null,
    val imageUrl: String? = null,

    //Encrypted
    val encryptedMessage: String? = null,
    val isMessageEncrypted: Boolean = false,

    val message: String? = null,

    val receiverId: String? = null,

    val createdAt: Long = System.currentTimeMillis()
){
    constructor() : this(
        id = "",
        senderId = null,
        senderName = null,
        senderImage = null,
        imageUrl = null,
        encryptedMessage = null,
        isMessageEncrypted = false,
        message = null,
        receiverId = null,
        createdAt = 0L
    )
}
