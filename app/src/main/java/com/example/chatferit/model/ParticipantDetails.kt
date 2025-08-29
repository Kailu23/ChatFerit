package com.example.chatferit.model

data class ParticipantDetails(
    val displayName: String = "",
    val joinedAt: Long = System.currentTimeMillis()
)
{
    constructor() : this(
        displayName = "",
        joinedAt = 0L
    )
}
