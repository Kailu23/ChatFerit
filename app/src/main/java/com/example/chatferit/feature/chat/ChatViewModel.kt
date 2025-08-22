package com.example.chatferit.feature.chat

import androidx.compose.animation.core.snap
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import com.example.chatferit.model.Message
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val firebaseDatabase : FirebaseDatabase
) : ViewModel() {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    fun sendMessage(channelId: String, messageText: String) {
        val message = Message(
            id = firebaseDatabase.reference.push().key?: UUID.randomUUID().toString(),
            message = messageText,
            senderId = Firebase.auth.currentUser?.uid,
            senderName = Firebase.auth.currentUser?.displayName,
            senderImage = null,
            imageUrl = null
        )
        val key = firebaseDatabase.getReference("messages").child(channelId).push().setValue(message)
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