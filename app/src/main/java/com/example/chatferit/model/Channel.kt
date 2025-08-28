package com.example.chatferit.model

data class Channel(
    val id : String = "",
    val name : String = "",
    val type: String = ChannelType.PRIVATE,
    val participants: Map<String, Boolean>? = null,
    val createdAt : Long = System.currentTimeMillis()
)

object ChannelType {
    const val PRIVATE = "private"
    const val GROUP = "group"
}