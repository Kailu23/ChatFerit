package com.example.chatferit.feature.chat

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatferit.data.repository.UserRepository
import com.example.chatferit.data.repository.iUserRepository
import com.example.chatferit.model.Message
import com.example.chatferit.util.CryptoManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseDatabase : FirebaseDatabase,
    private val firebaseStorage: FirebaseStorage,
    private val userRepository: iUserRepository,
    private val cryptoManager: CryptoManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _decryptionError = MutableSharedFlow<String>()
    val decryptionError : SharedFlow<String> = _decryptionError.asSharedFlow()

    private val _sendMessageError = MutableSharedFlow<String>()
    val sendMessageError : SharedFlow<String> = _sendMessageError.asSharedFlow()

    private var messagesListener: ValueEventListener? = null
    private var currentListeningChannelId: String? = null

    private val currentUserId: String? get() = firebaseAuth.currentUser?.uid

    fun SendMessage(channelId: String, receiverId: String, sendText: String, isEncrypted: Boolean) {
        val senderId = currentUserId ?: run{
            viewModelScope.launch { _sendMessageError.emit("User not logged in.") }
            return
        }
        if (sendText.isBlank()) {
            viewModelScope.launch { _sendMessageError.emit("Message cannot be empty.") }
            return
        }

        val messagesRef = firebaseDatabase.reference.child("messages").child(channelId)
        val messagePushRef = messagesRef.push()
        val messageId = messagePushRef.key ?: UUID.randomUUID().toString()

        val senderName = firebaseAuth.currentUser?.displayName

        if (isEncrypted) {
            viewModelScope.launch {
                try {
                    val recipientKeysResult = userRepository.getUserPublicKeys(receiverId)
                    recipientKeysResult.fold(
                        onSuccess = { recipientPublicKeys ->
                            if (recipientPublicKeys?.hybridPublicKey != null) {
                                try {
                                    val encryptedData = cryptoManager.encryptHybrid(
                                        plaintext = sendText,
                                        recipientPublicKey = recipientPublicKeys.hybridPublicKey
                                    )
                                    val message = Message(
                                        id = messageId,
                                        senderId = senderId,
                                        senderName = senderName,
                                        senderImage = null,
                                        imageUrl = null,
                                        encryptedMessage = encryptedData,
                                        isMessageEncrypted = true,
                                        message = null,
                                        receiverId = receiverId,
                                        createdAt = System.currentTimeMillis()
                                    )
                                    messagePushRef.setValue(message).addOnSuccessListener {
                                        Log.d(
                                            "ChatViewModel",
                                            "E2EE Message sent to $channelId"
                                        )
                                    }.addOnFailureListener { e ->
                                        Log.e(
                                            "ChatViewModel",
                                            "Failed to send E2EE Message to $channelId",
                                            e
                                        )
                                        viewModelScope.launch { _sendMessageError.emit("Failed to send E2EE Message.") }
                                    }
                                } catch (e: Exception) {
                                    Log.e(
                                        "ChatViewModel",
                                        "Encryption failed for message $messageId",
                                        e
                                    )
                                    viewModelScope.launch { _sendMessageError.emit("Encryption failed") }
                                }
                            } else {
                                Log.w(
                                    "ChatViewModel",
                                    "Recipient public key for E2EE not found for $receiverId."
                                )
                                viewModelScope.launch { _sendMessageError.emit("Cannot send secure message. Recipient's key unavailable.") }
                            }
                        },
                        onFailure = { exception ->
                            Log.e(
                                "ChatViewModel",
                                "Failed to fetch recipient key for $receiverId for E2EE.",
                                exception
                            )
                            viewModelScope.launch { _sendMessageError.emit("Error preparing secure message.") }
                        }
                    )
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Error in sendText (E2EE path)", e)
                    viewModelScope.launch { _sendMessageError.emit("Unexpected error occurred.") }
                }
            }
        } else {
            val message = Message(
                id = messageId,
                senderId = senderId,
                senderName = senderName,
                senderImage = null,
                imageUrl = null,
                encryptedMessage = null,
                isMessageEncrypted = true,
                message = sendText,
                receiverId = receiverId,
                createdAt = System.currentTimeMillis()
            )
            messagePushRef.setValue(message)
                .addOnSuccessListener { Log.d("ChatViewModel", "Plaintext Message sent to $channelId") }
                .addOnFailureListener { e ->
                    Log.e("ChatViewModel", "Plaintext Message failed to send to $channelId", e)
                    viewModelScope.launch { _sendMessageError.emit("Failed to send message.") }
                }
        }
    }

    fun SendImageMessage(uri: Uri, channelId: String, receiverId: String) {
        val senderId = currentUserId ?: run {
            viewModelScope.launch { _sendMessageError.emit("User not logged in for image send.") }
            return
        }
        val messagesRef = firebaseDatabase.reference.child("messages").child(channelId)
        val messagePushRef = messagesRef.push()
        val messageId = messagePushRef.key ?: UUID.randomUUID().toString()

        val senderName = firebaseAuth.currentUser?.displayName

        val imageFileRef = firebaseStorage.reference.child("images/${UUID.randomUUID()}")
        imageFileRef.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                imageFileRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result.toString()
                    val message = Message(
                        id = messageId,
                        senderId = senderId,
                        senderName = senderName,
                        senderImage = null,
                        imageUrl = downloadUri,
                        encryptedMessage = null,
                        isMessageEncrypted = false,
                        message = null,
                        receiverId = receiverId,
                        createdAt = System.currentTimeMillis()
                    )
                    messagePushRef.setValue(message)
                        .addOnSuccessListener { Log.d("ChatViewModel", "Image Message sent to $channelId") }
                        .addOnFailureListener { e ->
                            Log.e("ChatViewModel", "Image Message failed to send to $channelId", e)
                            viewModelScope.launch { _sendMessageError.emit("Failed to send image message.") }
                        }
                } else {
                    Log.e("ChatViewModel", "Failed to get image download URL.", task.exception)
                    viewModelScope.launch { _sendMessageError.emit("Failed to upload image.") }
                }
            }
    }

    fun ListenForMessages(channelId : String) {
        if (currentListeningChannelId == channelId && messagesListener != null) {
            Log.d("ChatViewModel", "Already listening for messages in channel $channelId")
            return
        }
        clearMessageListener()
        currentListeningChannelId = channelId

        val query = firebaseDatabase.getReference("messages").child(channelId).orderByChild("createdAt")
        messagesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newMessages = mutableListOf<Message>()
                snapshot.children.forEach { dataSnapshot ->
                    val firebaseMessage = dataSnapshot.getValue(Message::class.java)
                    firebaseMessage?.let { message ->
                        if (message.isMessageEncrypted && message.encryptedMessage != null) {
                            try {
                                val decryptedText =
                                    cryptoManager.decryptHybrid(message.encryptedMessage)
                                newMessages.add(
                                    message.copy(message = decryptedText, encryptedMessage = null)
                                )
                            } catch (e: Exception) {
                                Log.e(
                                    "ChatViewModel",
                                    "Decryption failed for message ${message.id}",
                                    e
                                )
                                viewModelScope.launch { _decryptionError.emit("Could not decrypt message.") }
                                newMessages.add(
                                    message.copy(
                                        message = "[Message decryption failed]",
                                        encryptedMessage = null
                                    )
                                )
                            }
                        } else {
                            newMessages.add(message)
                        }
                    }
                }
                _messages.value = newMessages
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("ChatViewModel", "Listen for messages cancelled for $channelId", error.toException())
            }
        }
        query.addValueEventListener(messagesListener!!)
        SubscribeForNotification(channelId)

    }
    private fun clearMessageListener() {
        currentListeningChannelId?.let {
            if (messagesListener != null) {
                firebaseDatabase.getReference("messages").child(it).removeEventListener(messagesListener!!)
                Log.d("ChatViewModel", "Removed message listener for channel $it")
            }
        }
        messagesListener = null
        currentListeningChannelId = null
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

    override fun onCleared() {
        super.onCleared()
        clearMessageListener()
    }
}