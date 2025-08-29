package com.example.chatferit.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.chatferit.MainActivity
import com.example.chatferit.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(@ApplicationContext private val context: Context) {
    companion object {
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val generalChannel = NotificationChannel(
                    NotificationConstants.CHANNEL_ID_GENERAL,
                    NotificationConstants.CHANNEL_NAME_GENERAL,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Channel for general app notifications"
                }

                val highImportanceChannel = NotificationChannel(
                    NotificationConstants.CHANNEL_ID_HIGH_IMPORTANCE,
                    NotificationConstants.CHANNEL_NAME_HIGH_IMPORTANCE,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for urgent alerts that should appear as head-up."
                }

                val notificationManager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(generalChannel)
                notificationManager.createNotificationChannel(highImportanceChannel)
            }
        }
    }


    fun showSimpleNotification(
        notificationId: Int,
        channelId: String,
        title: String,
        message: String,
        extras: Map<String, String>? = null,
        targetActivityClass: Class<*> = MainActivity::class.java,
    ) {
        val intent = Intent(context, targetActivityClass).apply{
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            extras?.forEach { (key, value) ->
                this.putExtra(key, value)
                Log.d("NotificationHelper", "Adding extra to intent: $key = $value")
            }
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            pendingIntentFlags
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // TODO: Replace with your actual app icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(if (channelId == NotificationConstants.CHANNEL_ID_HIGH_IMPORTANCE) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    //TODO() : Request the POST_NOTIFICATIONS permission from the user.

                    Log.w("NotificationHelper", "POST_NOTIFICATIONS permission not granted. Cannot show notification.")
                    return
                }
            }
            try {
                notify(notificationId, builder.build())
                Log.d("NotificationHelper", "Notification shown: ID $notificationId, title = $title")
            } catch (e: Exception) {
                Log.e("NotificationHelper", "Error showing notification: ID${notificationId}", e)
            }
        }
    }
}
