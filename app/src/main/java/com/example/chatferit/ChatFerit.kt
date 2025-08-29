package com.example.chatferit

import android.app.Application
import com.example.chatferit.notifications.NotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChatFerit : Application() {

    override fun onCreate() {
        super.onCreate()

        NotificationHelper.createNotificationChannel(this)
    }
}