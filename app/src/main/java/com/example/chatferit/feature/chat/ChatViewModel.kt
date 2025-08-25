package com.example.chatferit.feature.chat

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.chatferit.R
import com.example.chatferit.model.Message
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val firebaseDatabase : FirebaseDatabase,
    private val firebaseStorage: FirebaseStorage,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    fun SendMessage(channelId: String, messageText: String?, image: String? = null) {
        val message = Message(
            id = firebaseDatabase.reference.push().key?: UUID.randomUUID().toString(),
            message = messageText?:"",
            senderId = Firebase.auth.currentUser?.uid,
            senderName = Firebase.auth.currentUser?.displayName,
            senderImage = null,
            imageUrl = image
        )

        firebaseDatabase.reference.child("messages").child(channelId).push().setValue(message).addOnCompleteListener { task ->
            if(task.isSuccessful) {
                PostNotificationToUsers(channelId = channelId, senderName = message.senderName?: "", messageContent = message.message)
            }
        }
    }

    fun SendImageMessage(uri: Uri, channelID: String) {
        val imageRef = firebaseStorage.reference.child("images/${UUID.randomUUID()}")
        imageRef.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                imageRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    SendMessage(channelID, null, downloadUri.toString())
                }
            }
    }
    fun ListenForMessages(channelId : String) {
        firebaseDatabase.getReference("messages").child(channelId).orderByChild("createdAt")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Message>()
                    snapshot.children.forEach { dataSnapshot ->
                        val message = dataSnapshot.getValue(Message::class.java)
                        message?.let {
                            list.add(it)
                        }
                    }
                    _messages.value = list
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
        SubscribeForNotification(channelId)
    }
    private fun SubscribeForNotification(channelId : String) {
        FirebaseMessaging.getInstance().subscribeToTopic("group_$channelId").addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d("ChatViewMOdel", "Subscribed to topic: group_$channelId")
            } else {
                Log.d("ChatViewMOdel", "Failed to subscribe to topic: group_$channelId")
            }
        }
    }

    fun PostNotificationToUsers(channelId: String, senderName : String, messageContent: String) {
        val fcmUrl = "https://fcm.googleapis.com/v1/projects/chatferit/messages:send"
        val jsonBody = JSONObject().apply {
            put("message", JSONObject().apply {
                put("topic", "group_$channelId")
                put("notification", JSONObject().apply {
                    put("title", channelId)
                    put("body", "$senderName: $messageContent")
                })
            })
        }

        val requestBody = jsonBody.toString()

        val request = object : StringRequest(Method.POST, fcmUrl,Response.Listener {
                Log.d("ChatViewModel", "Notification sent successfully")
            },Response.ErrorListener {
                Log.e("ChatViewModel", "Failed to send notification")
            }) {
            override fun getBody(): ByteArray? {
                return requestBody.toByteArray()
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer ${GetAccessToken()}"
                headers["Content-Type"] = "application/json"
                return headers
            }
        }
        val queue = Volley.newRequestQueue(context)
        queue.add(request)

    }

    private fun GetAccessToken() : String {
        val inputStream = context.resources.openRawResource(R.raw.chatferit_key)
        val googleCredentials = GoogleCredentials.fromStream(inputStream)
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
        return googleCredentials.refreshAccessToken().tokenValue
    }
}