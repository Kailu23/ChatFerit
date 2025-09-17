package com.example.chatferit

object AppRoutes {
    const val AUTH_GRAPH_ROUTE = "auth_graph"
    const val SIGN_IN = "signin"
    const val SIGN_UP = "signup"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val CHAT_ROUTE = "chat/{channelId}/{channelName}/{receiverId}"

    fun chatScreen(channelId: String, channelName: String, receiverId: String) = "chat/$channelId/$channelName/$receiverId"
}