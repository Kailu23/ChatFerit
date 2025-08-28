package com.example.chatferit.model

data class Message(
    val id: String = "",
    val senderId: String? = "",
    val senderName: String? = "",
    val senderImage: String? = null,
    val imageUrl: String? = null,

    //Encrypted
    val encryptedMessageForSender: String? = null,
    val encryptedMessageForRecipient: String? = null,
    val messageEncrypted: Boolean = false,

    val plainTextMessage: String? = null,

    val receiverId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
){
    constructor() : this(
        id = "",
        senderId = null,
        senderName = null,
        senderImage = null,
        imageUrl = null,
        encryptedMessageForSender = null,
        encryptedMessageForRecipient = null,
        messageEncrypted = false,
        plainTextMessage = null,
        receiverId = null,
        createdAt = 0L
    )
}
