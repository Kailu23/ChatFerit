package com.example.chatferit.feature.chat

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatferit.data.repository.iUserRepository
import com.example.chatferit.model.Message
import com.example.chatferit.util.CryptoManager
import com.google.firebase.auth.FirebaseAuth
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
        val currentSenderName = firebaseAuth.currentUser?.displayName

        if (isEncrypted) {
            viewModelScope.launch {
                try {
                    val recipientKeysResult = userRepository.getUserPublicKeys(receiverId)
                    val senderKeysResult = userRepository.getUserPublicKeys(senderId)

                    val recipientPublicKeys = recipientKeysResult.getOrNull()
                    val senderPublicKeys = senderKeysResult.getOrNull()

                    if (recipientPublicKeys?.hybridPublicKey != null && senderPublicKeys?.hybridPublicKey != null) {
                        try {
                            val ciphertextForRecipient = cryptoManager.encryptHybrid(
                                plaintext = sendText,
                                publicKey = recipientPublicKeys.hybridPublicKey
                            )
                            val ciphertextForSender = cryptoManager.encryptHybrid(
                                plaintext = sendText,
                                publicKey = senderPublicKeys.hybridPublicKey

                            )
                            val message = Message(
                                id = messageId,
                                senderId = senderId,
                                senderName = currentSenderName,
                                senderImage = null,
                                imageUrl = null,
                                encryptedMessageForSender = ciphertextForSender,
                                encryptedMessageForRecipient = ciphertextForRecipient,
                                messageEncrypted = true,
                                plainTextMessage = null,
                                receiverId = receiverId,
                                createdAt = System.currentTimeMillis(),
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
                            viewModelScope.launch { _sendMessageError.emit("Encryption failed: ${e.message}") }
                        }
                    } else {
                        var errorReason = "Key fetch failed: "
                        if(recipientPublicKeys?.hybridPublicKey == null) errorReason += "Recipient key missing. "
                        if(senderPublicKeys?.hybridPublicKey == null) errorReason += "Sender key missing for self-encryption. "
                        Log.w("ChatViewModel", errorReason)
                        viewModelScope.launch { _sendMessageError.emit(errorReason) }
                    }


                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Error in sendText (E2EE path outer try-catch)", e)
                    viewModelScope.launch { _sendMessageError.emit("Unexpected error occurred while preparing E2EE message.") }
                }
            }
        } else {
            val message = Message(
                id = messageId,
                senderId = senderId,
                senderName = currentSenderName,
                senderImage = null,
                imageUrl = null,
                encryptedMessageForSender = null,
                encryptedMessageForRecipient = null,
                messageEncrypted = false,
                plainTextMessage = sendText,
                receiverId = receiverId,
                createdAt = System.currentTimeMillis(),
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
                        encryptedMessageForSender = null,
                        encryptedMessageForRecipient = null,
                        messageEncrypted = false,
                        plainTextMessage = null,
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
                val newMessagesFromDb = mutableListOf<Message>()
                snapshot.children.forEach { dataSnapshot ->
                    val firebaseMessage = dataSnapshot.getValue(Message::class.java)
                    firebaseMessage?.let { message ->
                        newMessagesFromDb.add(message.copy(id = dataSnapshot.key ?: message.id))
                    }
                }
                val processedMessages = newMessagesFromDb.mapNotNull { firebaseMessage ->
                    var finalDisplayContent: String?
                    var successfullyProcessed = true

                    if (firebaseMessage.messageEncrypted) {
                        val cipherText: String? =
                            if(firebaseMessage.senderId == currentUserId) {
                                firebaseMessage.encryptedMessageForSender
                            } else {
                                firebaseMessage.encryptedMessageForRecipient
                            }
                        if (cipherText != null) {
                            try {
                                finalDisplayContent = cryptoManager.decryptHybrid(cipherText)
                            } catch (e: Exception) {
                                Log.e(
                                    "ChatViewModel",
                                    "Decryption failed for message ${firebaseMessage.id}",
                                    e
                                )
                                viewModelScope.launch { _decryptionError.emit("Could not decrypt message.") }
                                finalDisplayContent = "[Decryption failed]"
                                successfullyProcessed = false
                            }
                        } else {
                            Log.w("ChatViewModel", "E2EE Message ${firebaseMessage.id} missing relevant cipthertext")
                            finalDisplayContent = "[Encrypted: Data Missing]"
                        }
                    } else if (firebaseMessage.imageUrl != null) {
                        finalDisplayContent = null
                    } else {
                        finalDisplayContent = firebaseMessage.plainTextMessage
                    }

                    if (successfullyProcessed || finalDisplayContent != null || firebaseMessage.imageUrl != null) {
                        firebaseMessage.copy(
                            plainTextMessage = finalDisplayContent,
                            messageEncrypted = false,
                            encryptedMessageForSender = null,
                            encryptedMessageForRecipient = null
                        )
                    }
                    else {
                        Log.e("ChatViewModel", "Message ${firebaseMessage.id} couldn't be processed for UI")
                        null
                    }
                }. sortedBy{it.createdAt}
                _messages.value = processedMessages
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