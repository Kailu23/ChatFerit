package com.example.chatferit.feature.chat

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.chatferit.model.Message
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

        firebaseDatabase.reference.child("messages").child(channelId).push().setValue(message)
            .addOnCompleteListener {
                Log.d("ChatViewModel", "Message sent successfully to database.")
            }
            .addOnFailureListener {
                Log.d("ChatViewModel", "Message failed to send to database.")
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
                } else {
                    Log.e("ChatViewModel", "Failed to get image download URL.", task.exception)
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

}