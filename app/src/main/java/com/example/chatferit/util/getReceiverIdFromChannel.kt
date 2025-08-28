package com.example.chatferit.util

import android.util.Log

fun getReceiverIdFromChannel(channelId: String, currentUserId: String): String? {
    val participants = channelId.split("_").filter { it.isNotBlank() }
    // This assumes your 1-to-1 channel IDs are like "userId1_userId2"
    if (participants.size == 2) {
        return participants.firstOrNull { it != currentUserId }
    }
    Log.w("getReceiverId", "ChannelId '$channelId' format is not 'userId1_userId2' or current user '$currentUserId' not found.")
    return null // Return null if it's not a 1-to-1 chat or format is different
}