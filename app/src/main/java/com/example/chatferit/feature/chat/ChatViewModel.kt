package com.example.chatferit.feature.chat

import android.net.Uri
import android.widget.ImageView
import androidx.compose.animation.core.snap
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.text.LinkAnnotation
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import com.example.chatferit.model.Message
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val firebaseDatabase : FirebaseDatabase,
    private val firebaseStorage: FirebaseStorage
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

    }
}