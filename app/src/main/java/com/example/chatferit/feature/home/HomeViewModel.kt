package com.example.chatferit.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatferit.data.repository.UserRepository
import com.example.chatferit.data.repository.iFriendRepository
import com.example.chatferit.model.Channel
import com.example.chatferit.model.ChannelType
import com.example.chatferit.model.FriendRequest
import com.example.chatferit.model.UserProfile
import com.example.chatferit.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val friendRepository: iFriendRepository
) : ViewModel() {

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels = _channels.asStateFlow()

    private val _selectedScreenRoute = MutableStateFlow(BottomNavItem.Chats.route) // Default to Chats
    val selectedScreenRoute = _selectedScreenRoute.asStateFlow()

    private val _showAddChannelDialog = MutableStateFlow(false)
    val showAddChannelDialog= _showAddChannelDialog.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery= _searchQuery.asStateFlow()

    private val _allUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val allUsers: StateFlow<List<UserProfile>> = _allUsers.asStateFlow()

    private val _friendUids = MutableStateFlow<Set<String>>(emptySet())
    val friendUids = _friendUids.asStateFlow()

    private val _incomingFriendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val incomingFriendRequests: StateFlow<List<FriendRequest>> = _incomingFriendRequests.asStateFlow()

    private val _sentFriendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val sentFriendRequests: StateFlow<List<FriendRequest>> = _incomingFriendRequests.asStateFlow()

    private val _showAddFriendDialog = MutableStateFlow(false)
    val showAddFriendDialog = _showAddFriendDialog.asStateFlow()

    private val currentUserId: String? = firebaseAuth.currentUser?.uid

    val actualFriends: StateFlow<List<UserProfile>> = combine(
        _allUsers,
        _friendUids,
        searchQuery
    ) { allUsersList, currentFriendUids, query ->
        allUsersList.filter { userProfile ->
            currentFriendUids.contains(userProfile.uid) && (query.isBlank() || userProfile.displayName.contains(query, ignoreCase = true))
        }.sortedBy { it.displayName }
    }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000), emptyList())


    init {
        listenForChannels()
        listenForAllUsers()
        listenForCurrentUserFriends()
        listenForIncomingFriendRequests()
    }

    private fun listenForChannels() {
        val currentUid = currentUserId ?: run {
            Log.w("HomeViewModel", "User not logged in, cannot listen for channels.")
            _channels.value = emptyList()
            return
        }
        val ref = firebaseDatabase.getReference("channel")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val channelList = mutableListOf<Channel>()
                snapshot.children.forEach { dataSnapshot ->
                    try {
                        val channel = dataSnapshot.getValue(Channel::class.java)?.copy(id = dataSnapshot.key ?: "")
                        if (channel != null) {
                            if (channel.type == ChannelType.GROUP || channel.type == ChannelType.PRIVATE && channel.participants?.containsKey(currentUid) == true) {
                                channelList.add(channel)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Error deserializing channel: ${dataSnapshot.key}", e)
                    }
                }
                _channels.value = channelList.sortedByDescending { it.createdAt }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeViewModel", "Firebase channel listener cancelled", error.toException())
            }
        })
    }

    private fun listenForCurrentUserFriends() {
        val currentFbUserUid = firebaseAuth.currentUser?.uid ?: run { _friendUids.value = emptySet(); return }

        viewModelScope.launch {
            friendRepository.getFriendUids(currentFbUserUid).collect{ result ->
                when (result) {
                    is Resource.Success -> _friendUids.value = result.data ?: emptySet()
                    is Resource.Error -> {
                        Log.e("HomeViewModel", "Error fetching friend UIDs: ${result.message}")
                        _friendUids.value = emptySet()
                    }
                    is Resource.Loading -> TODO()
                }
            }
        }
    }

    private fun listenForAllUsers() {
        val currentAuthUid = firebaseAuth.currentUser?.uid ?: return
        firebaseDatabase.getReference("users").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val userList = mutableListOf<UserProfile>()
                snapshot.children.forEach{userSnapshot ->
                    val userProfile = userSnapshot.getValue(UserProfile::class.java)

                    if (userProfile != null && userProfile.uid != currentAuthUid) {
                        userList.add(userProfile)
                    }
                }
                _allUsers.value = userList
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeViewModel", "Failed to listen for all users.", error.toException())
            }
        })
    }
    fun addGroupChannel(name: String) {
        if (name.isBlank()) {
            Log.w("Add Channel", "Channel name cannot be blank.")
            return
        }
        val channelRef = firebaseDatabase.getReference("channel").push()
        val channelId = channelRef.key ?: run {
            Log.e("Add Channel", "Failed to generate key for group channel")
            return
        }

        val newChannel = Channel(
            id = channelId,
            name = name,
            type = ChannelType.GROUP,
            createdAt = System.currentTimeMillis()
        )

        channelRef.setValue(newChannel)
            .addOnSuccessListener {
                Log.d("Add Channel", "Group channel '$name' added successfully.")
                onDismissAddChannelDialog()
            }
            .addOnFailureListener { e ->
                Log.e("Add Channel", "Failed to add group channel '$name'", e)
            }
    }

    suspend fun getOrCreatePrivateChannel(otherUserUid: String): Result<Pair<String, String>> {
        val currentUserUid = currentUserId ?: return Result.failure(Exception("User not logged in. Cannot create/get 1-to-1 channel."))
        if (currentUserUid == otherUserUid) return Result.failure(Exception("Cannot create a chat with yourself."))
        if (otherUserUid.isBlank()) return Result.failure(Exception("Other user ID is blank."))


        val channelId = createDeterministicOneToOneChannelId(currentUserUid, otherUserUid)
        val channelRef = firebaseDatabase.getReference("channels").child(channelId)

        return try {
            val snapshot = channelRef.get().await()
            if (snapshot.exists()) {
                val existingChannel = snapshot.getValue(Channel::class.java)

                val channelName = existingChannel?.name ?: "Chat with ${getDisplayNameForUser(otherUserUid)}"
                Log.d("HomeViewModel", "Found existing PRIVATE channel: $channelId")
                Result.success(Pair(channelId, channelName))
            } else {
                val otherUserName = getDisplayNameForUser(otherUserUid) // Placeholder
                val channelName = "Chat with $otherUserName"

                val newChannel = Channel(
                    id = channelId,
                    name = channelName,
                    type = ChannelType.PRIVATE,
                    participants = mapOf(currentUserUid to true, otherUserUid to true),
                    createdAt = System.currentTimeMillis()
                )
                channelRef.setValue(newChannel).await()
                Log.d("HomeViewModel", "Created new PRIVATE channel: $channelId")
                Result.success(Pair(channelId, channelName))
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error in getOrCreateOneToOneChannel for otherUserUid: $otherUserUid", e)
            Result.failure(e)
        }
    }

    private suspend fun getDisplayNameForUser(userId: String): String {
        return try {
            val userSnapshot = firebaseDatabase.getReference("users")
                .child(userId)
                .child("displayName")
                .get()
                .await()
            userSnapshot.value as? String ?: userId
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error fetching display name for $userId", e)
            userId
        }
    }

    fun createDeterministicOneToOneChannelId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    fun onBottomNavItemSelected(route: String) {
        _selectedScreenRoute.value = route
    }

    fun onAddChannelClicked() {
        _showAddChannelDialog.value = true
    }

    fun onDismissAddChannelDialog() {
        _showAddChannelDialog.value = false
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onAddFriendClicked() { // Renamed from onAddFriendClicked to be specific
        _showAddFriendDialog.value = true
    }

    fun onDismissAddFriendDialog() { // Renamed from onAddFriendDialogDismiss
        _showAddFriendDialog.value = false
    }

    fun listenForIncomingFriendRequests() {
        val currentFbUserUid = firebaseAuth.currentUser?.uid ?: run { _incomingFriendRequests.value= emptyList(); return }

        viewModelScope.launch {
            friendRepository.getIncomingFriendRequests(currentFbUserUid).collect { result ->
                when (result) {
                    is Resource.Success -> _incomingFriendRequests.value = result.data ?: emptyList()
                    is Resource.Error -> {
                        Log.e("HomeViewModel", "Error fetching incoming friend requests: ${result.message}")
                        _incomingFriendRequests.value = emptyList()
                    }
                    is Resource.Loading -> TODO()
                }
            }
        }
    }

    fun submitSendFriendRequest(targetUserIdentifier: String) { // e.g., email or UID
        viewModelScope.launch {
            val currentFbUserUid = firebaseAuth.currentUser?.uid ?: return@launch
            if (targetUserIdentifier.isBlank()) {
                Log.w("HomeViewModel", "Target user identifier is blank.")
                return@launch
            }

            val targetUid = targetUserIdentifier

            if (targetUid == currentFbUserUid) {
                Log.w("HomeViewModel", "Cannot send a friend request to yourself.")
                return@launch
            }

            val result = friendRepository.sendFriendRequest(currentFbUserUid, targetUid)

            when(result){
                is Resource.Success -> {
                    Log.d("HomeViewModel", "Friend request sent successfully to $targetUid")
                    _showAddFriendDialog.value = false
                }
                is Resource.Error -> Log.e("HomeViewModel", "Error sending friend request: ${result.message}")
                is Resource.Loading -> TODO()
            }
        }
    }

    fun acceptFriendRequest(request: FriendRequest) {
        viewModelScope.launch {
            val currentUid = firebaseAuth.currentUser?.uid ?: return@launch
            val result = friendRepository.acceptFriendRequest(currentUid, request)
            when (result) {
                is Resource.Success -> {
                    Log.d("HomeViewModel", "Friend request from ${request.senderName} accepted.")

                    getOrCreatePrivateChannel(request.senderId).fold(
                        onSuccess = { (channelId, channelName) ->
                            Log.d(
                                "HomeViewModel",
                                "Private channel $channelId for new friend ${request.senderName}"
                            )
                        },
                        onFailure = { error ->
                            Log.e(
                                "HomeViewModel",
                                "Failed to get/create channel for new friend ${request.senderName}",
                                error
                            )
                        }
                    )
                }
                is Resource.Error -> {
                    Log.e("HomeViewModel", "Failed to accept friend request: ${result.message}")
                }
                is Resource.Loading -> {
                    TODO()
                }
            }
        }
    }

    fun declineFriendRequest(request: FriendRequest) {
        viewModelScope.launch {
            val currentUid = firebaseAuth.currentUser?.uid ?: return@launch
            val result = friendRepository.declineFriendRequest(currentUid, request)
            when (result) {
                is Resource.Success -> {
                    Log.d("HomeViewModel", "Friend request from ${request.senderName} declined.")
                }

                is Resource.Error -> {
                    Log.e("HomeViewModel", "Failed to decline friend request: ${result.message}")
                }
                is Resource.Loading -> {
                    TODO()
                }
            }
        }
    }
}