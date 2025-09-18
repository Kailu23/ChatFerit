package com.example.chatferit


import android.util.Log
import com.example.chatferit.notifications.NotificationConstants
import com.example.chatferit.notifications.NotificationHelper
import com.example.chatferit.util.CryptoManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.IOException
import java.security.GeneralSecurityException
import javax.inject.Inject

@AndroidEntryPoint
class FirebaseMessageService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var cryptoManager: CryptoManager

    private val  serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FirebaseMessageService", "From: ${remoteMessage.from}")

        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FirebaseMessageService", "Message data payload: " + remoteMessage.data)

            val data = remoteMessage.data
            val senderName = data["senderName"] ?: "New Message"
            val encryptedPayload = data["encryptedMessageForRecipient"]
            val channelIdFCM = data["channelId"]
            val originalMessageId = data["messageId"]

            val currentUserName = FirebaseAuth.getInstance().currentUser?.displayName
            if (currentUserName != null && senderName.contains(currentUserName)) {
                Log.d("FirebaseMessageService", "Notification suppressed: message from current user ($currentUserName).")
                return
            }


            if (encryptedPayload != null && channelIdFCM != null) {
                serviceScope.launch {
                    try {
                        val decryptedText = cryptoManager.decryptHybrid(encryptedPayload)
                        Log.i("FirebaseMessageService", "Decrypted message: $decryptedText")

                        notificationHelper.showSimpleNotification(
                            notificationId = NotificationConstants.NOTIFICATION_ID_NEW_MESSAGE + (channelIdFCM.hashCode()),
                            channelId = NotificationConstants.CHANNEL_ID_HIGH_IMPORTANCE,
                            title = senderName,
                            message = decryptedText,
                            extras = mapOf(
                                "channelId" to channelIdFCM,
                                "channelName" to (data["channelName"] ?: senderName),
                                "receiverId" to (data["senderId"] ?: "")
                            )
                        )

                    } catch (e: GeneralSecurityException) {
                        Log.e("FirebaseMessageService", "Decryption failed (Security Error): ${e.message}", e)
                        notificationHelper.showSimpleNotification(
                            notificationId = NotificationConstants.NOTIFICATION_ID_NEW_MESSAGE + (channelIdFCM.hashCode()),
                            channelId = NotificationConstants.CHANNEL_ID_GENERAL,
                            title = senderName,
                            message = "[Encrypted message - could not decrypt]",
                            extras = mapOf("channelId" to channelIdFCM)
                        )
                    } catch (e: IOException) {
                        Log.e("FirebaseMessageService", "Decryption failed (IO Error): ${e.message}", e)
                    } catch (e: Exception) {
                        Log.e("FirebaseMessageService", "Generic error processing/decrypting FCM message: ${e.message}", e)
                    }
                }
            } else {
                Log.w("FirebaseMessageService", "Encrypted payload or channelId missing in FCM data.")
                remoteMessage.notification?.let {
                    Log.d("FirebaseMessageService", "Fallback to notification payload: Title: ${it.title}, Body: ${it.body}")
                    notificationHelper.showSimpleNotification(
                        notificationId = NotificationConstants.NOTIFICATION_ID_NEW_MESSAGE,
                        channelId = NotificationConstants.CHANNEL_ID_GENERAL,
                        title = it.title ?: "New Message",
                        message = it.body ?: "You have a new message."
                    )
                }
            }
        } else if (remoteMessage.notification != null) {
            remoteMessage.notification?.let {
                Log.d("FirebaseMessageService", "Received message with only notification payload: Title: ${it.title}, Body: ${it.body}")
                notificationHelper.showSimpleNotification(
                    notificationId = NotificationConstants.NOTIFICATION_ID_NEW_MESSAGE,
                    channelId = NotificationConstants.CHANNEL_ID_GENERAL,
                    title = it.title ?: "New Message",
                    message = it.body ?: "You have a new message."
                )
            }
        }
    }

    override fun onNewToken(token: String) {
        Log.d("FirebaseMessageService", "Refreshed token: $token")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}