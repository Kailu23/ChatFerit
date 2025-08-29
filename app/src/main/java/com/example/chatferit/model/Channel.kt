package com.example.chatferit.model

data class Channel(
    val id: String = "",
    val name: String = "",
    val type: String = ChannelType.PRIVATE,
    val participants: Map<String, ParticipantDetails>? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String? = null
) {
    constructor() : this(
        id = "",
        name = "",
        type = ChannelType.PRIVATE,
        participants = null,
        createdAt = 0L,
        createdBy = null
    )
}


object ChannelType {
    const val PRIVATE = "private"
    const val GROUP = "group"
}