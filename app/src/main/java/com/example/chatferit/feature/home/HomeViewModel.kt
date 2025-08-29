package com.example.chatferit.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatferit.data.repository.UserRepository
import com.example.chatferit.data.repository.iFriendRepository
import com.example.chatferit.model.Channel
import com.example.chatferit.model.ChannelType
import com.example.chatferit.model.FriendRequest
import com.example.chatferit.model.ParticipantDetails
import com.example.chatferit.model.UserProfile
import com.example.chatferit.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private val _privateChannels = MutableStateFlow<List<Channel>>(emptyList())
    val privateChannels = _privateChannels.asStateFlow()

    private val _groupChannels = MutableStateFlow<List<Channel>>(emptyList())
    val groupChannels: StateFlow<List<Channel>> = _groupChannels.asStateFlow()

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
    val sentFriendRequests: StateFlow<List<FriendRequest>> = _sentFriendRequests.asStateFlow()

    private val _showAddFriendDialog = MutableStateFlow(false)
    val showAddFriendDialog = _showAddFriendDialog.asStateFlow()
    private val _addFriendSearchQuery = MutableStateFlow("")
    val addFriendSearchQuery: StateFlow<String> = _addFriendSearchQuery.asStateFlow()
    private val _addFriendSearchResults = MutableStateFlow<List<UserProfile>>(emptyList())
    val addFriendSearchResults: StateFlow<List<UserProfile>> = _addFriendSearchResults.asStateFlow()
    private val _isSearchingUsers = MutableStateFlow(false)
    val isSearchingUsers: StateFlow<Boolean> = _isSearchingUsers.asStateFlow()

    private var searchJob: Job? = null
    private val currentUserId: String? = firebaseAuth.currentUser?.uid

    private val usersRef = firebaseDatabase.getReference("users")

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _navigateToLogin = MutableStateFlow<Boolean>(false)
    val navigateToLogin: StateFlow<Boolean> = _navigateToLogin.asStateFlow()

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
        listenForUserPrivateChannels()
        listenForUserGroupChannels()
        listenForAllUsers()
        listenForCurrentUserFriends()
        listenForIncomingFriendRequests()
    }

    private fun listenForUserPrivateChannels() {
        val currentUserId = currentUserId ?: run {
            Log.w("HomeViewModel", "User not logged in, cannot listen for private channels.")
            _privateChannels.value = emptyList()
            return
        }

        val ref = firebaseDatabase.getReference("channels")
        ref.orderByChild("type").equalTo(ChannelType.PRIVATE).addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val channelList = mutableListOf<Channel>()
                snapshot.children.forEach { dataSnapshot ->
                    try {
                        val channel = dataSnapshot.getValue(Channel::class.java)?.copy(id = dataSnapshot?.key ?: "")

                        if (channel != null && channel.type == ChannelType.PRIVATE &&
                            channel.participants?.containsKey(currentUserId) == true) {
                            channelList.add(channel)
                        }
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Error deserializing private channel: ${dataSnapshot.key}", e)
                    }
                }
                _privateChannels.value = channelList.sortedByDescending { it.createdAt }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeViewModel", "Firebase private channels listener cancelled.", error.toException())
            }
        })
    }

    private fun listenForUserGroupChannels() {
        val currentUid = currentUserId ?: run {
            Log.w("HomeViewModel", "User not logged in, cannot listen for private channels.")
            _privateChannels.value = emptyList()
            return
        }
        val ref = firebaseDatabase.getReference("groupChannels")
        ref.orderByChild("type").equalTo(ChannelType.GROUP).addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val channelList = mutableListOf<Channel>()
                snapshot.children.forEach { dataSnapshot ->
                    try {
                        val channel = dataSnapshot.getValue(Channel::class.java)?.copy(id = dataSnapshot?.key ?: "")

                        if (channel != null && channel.type == ChannelType.GROUP &&
                            channel.participants?.containsKey(currentUserId) == true) {
                            channelList.add(channel)
                        }
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Error deserializing group channel: ${dataSnapshot.key}", e)
                    }
                }
                _groupChannels.value = channelList.sortedByDescending { it.createdAt }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeViewModel", "Firebase group channels listener cancelled.", error.toException())
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
        usersRef.addValueEventListener(object : ValueEventListener{
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
        val currentUserId = firebaseAuth.currentUser?.uid ?: return
        val currentUserDisplayName = firebaseAuth?.currentUser?.displayName ?: "Unknown"
        if (name.isBlank()) {
            Log.w("Add Channel", "Channel name cannot be blank.")
            _snackbarMessage.value = "Channel name cannot be empty."
            return
        }

        val channelRef = firebaseDatabase.getReference("groupChannels").push()
        val channelId = channelRef.key ?: run {
            Log.e("Add Channel", "Failed to generate key for group channel")
            return
        }

        val initialParticipantDetails = ParticipantDetails(displayName = currentUserDisplayName)

        val newChannel = Channel(
            id = channelId,
            name = name,
            type = ChannelType.GROUP,
            participants = mapOf(currentUserId to initialParticipantDetails),
            createdAt = System.currentTimeMillis(),
            createdBy = currentUserId,
        )

        channelRef.setValue(newChannel)
            .addOnSuccessListener {
                Log.d("Add Channel", "Group channel '$name' added successfully.")
                _snackbarMessage.value = "Group '$name' created successfully!"
                onDismissAddChannelDialog()
            }
            .addOnFailureListener { e ->
                Log.e("Add Channel", "Failed to add group channel '$name'", e)
                _snackbarMessage.value = "Failed to create group: ${e.message}"
            }
    }

    suspend fun getOrCreatePrivateChannel(otherUserUid: String): Result<Pair<String, String?>> {
        val currentUserUid = currentUserId ?: return Result.failure(Exception("User not logged in. Cannot create/get 1-to-1 channel."))
        if (currentUserUid == otherUserUid) return Result.failure(Exception("Cannot create a chat with yourself."))
        if (otherUserUid.isBlank()) return Result.failure(Exception("Other user ID is blank."))


        val channelId = createDeterministicOneToOneChannelId(currentUserUid, otherUserUid)
        val channelRef = firebaseDatabase.getReference("channels").child(channelId)

        return try {
            val snapshot = channelRef.get().await()
            if (snapshot.exists()) {
                var existingChannel = snapshot.getValue(Channel::class.java)
                var channelName = existingChannel?.name

                if (existingChannel?.type == ChannelType.PRIVATE) {
                    val otherParticipantId = existingChannel.participants?.keys?.firstOrNull { it != currentUserUid }
                    if (otherParticipantId != null) {
                        val otherParticipantDetails = existingChannel.participants[otherParticipantId]
                        channelName = otherParticipantDetails?.displayName ?: getDisplayNameForUser(otherUserUid)
                    }
                } else if (channelName.isNullOrBlank() && existingChannel?.type == ChannelType.PRIVATE) {
                    val otherParticipantId = existingChannel.participants?.keys?.firstOrNull { it != currentUserUid }
                    if (otherParticipantId != null) {
                        channelName = getDisplayNameForUser(otherParticipantId)
                    } else {
                        channelName = "Chat"
                    }
                }

                Log.d("HomeViewModel", "Found existing PRIVATE channel: $channelId")
                Result.success(Pair(channelId, channelName))
            } else {
                val currentUserDisplayName = getDisplayNameForUser(currentUserUid)
                val otherUserDisplayName = getDisplayNameForUser(otherUserUid)

                val initialChannelName = otherUserDisplayName

                val newChannel = Channel(
                    id = channelId,
                    name = initialChannelName,
                    type = ChannelType.PRIVATE,
                    participants = mapOf(
                        currentUserUid to ParticipantDetails(displayName = currentUserDisplayName),
                        otherUserUid to ParticipantDetails(displayName = otherUserDisplayName)
                    ),
                    createdBy = currentUserId,
                    createdAt = System.currentTimeMillis()
                )
                channelRef.setValue(newChannel).await()
                Log.d("HomeViewModel", "Created new PRIVATE channel: $channelId")
                Result.success(Pair(channelId, otherUserDisplayName))
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error in getOrCreateOneToOneChannel for otherUserUid: $otherUserUid", e)
            Result.failure(e)
        }
    }

    private suspend fun getDisplayNameForUser(userId: String): String {
        return try {
            val userSnapshot = usersRef
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

    fun onAddFriendClicked() {
        _showAddFriendDialog.value = true
    }

    fun onDismissAddFriendDialog() {
        _showAddFriendDialog.value = false
        clearAddFriendSearch()
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

    fun submitSendFriendRequest(targetUserIdentifier: String) {
        viewModelScope.launch {
            val currentFirebaseUserId = firebaseAuth.currentUser?.uid
            if (currentFirebaseUserId == null) {
                _snackbarMessage.value = "Error: Could not identify current user."
                return@launch
            }
            if (targetUserIdentifier.isBlank()) {
                Log.w("HomeViewModel", "Target user identifier is blank.")
                _snackbarMessage.value = "Please enter an email or name to search"
                return@launch
            }

            val targetUid = targetUserIdentifier

            if (targetUid == currentFirebaseUserId) {
                Log.w("HomeViewModel", "Cannot send a friend request to yourself.")
                _snackbarMessage.value = "You cannot send a friend request to yourself."
                return@launch
            }

            val result = friendRepository.sendFriendRequest(currentFirebaseUserId, targetUid)

            when(result){
                is Resource.Success -> {
                    Log.d("HomeViewModel", "Friend request sent successfully to $targetUid")
                    _snackbarMessage.value = "Friend request sent!"
                    _showAddFriendDialog.value = false
                }
                is Resource.Error -> {
                    _snackbarMessage.value = "Error sending friend request."
                    Log.e("HomeViewModel", "Error sending friend request: ${result.message}")
                }
                is Resource.Loading -> TODO("Not yet implemented")
            }

            if (actualFriends.value.any { friend -> friend.uid == targetUid }) {
                _snackbarMessage.value = "You are already friends with this user"
            }
            if (incomingFriendRequests.value.any { request -> request.senderId == targetUid }) {
                _snackbarMessage.value = "This user has already sent you a request."
                return@launch
            }
            /*if (_sentFriendRequests.value.any { friend -> friend. == targetUid }) {
                _snackbarMessage.value = "You've already sent a request to this user."
                return@launch
            }*/ // TODO(Change friend request model to include senderId)
        }
    }

    fun acceptFriendRequest(request: FriendRequest) {
        viewModelScope.launch {
            val currentUid = firebaseAuth.currentUser?.uid ?: return@launch
            val result = friendRepository.acceptFriendRequest(currentUid, request)
            when (result) {
                is Resource.Success -> {
                    Log.d("HomeViewModel", "Friend request from ${request.senderName} accepted.")
                    _snackbarMessage.value = "${request.senderName} is now your friend!"
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
                    _snackbarMessage.value = "Failed to accept request."
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
                    _snackbarMessage.value = "Request from ${request.senderName} declined."
                }

                is Resource.Error -> {
                    Log.e("HomeViewModel", "Failed to decline friend request: ${result.message}")
                    _snackbarMessage.value = "Failed to decline request.}"
                }
                is Resource.Loading -> {
                    TODO()
                }
            }
        }
    }

    fun onAddFriendSearchQueryChanged(query: String) {
        _addFriendSearchQuery.value = query
        searchJob?.cancel()

        if (query.length > 3) {
            searchJob = viewModelScope.launch {
                delay(300)
                _isSearchingUsers.value = true
                _addFriendSearchResults.value = emptyList()

                try {
                    _addFriendSearchResults.value = searchUsers(query)
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error searching users", e)
                    _addFriendSearchResults.value = emptyList()
                } finally {
                    _isSearchingUsers.value = false
                }
            }
        } else {
            _addFriendSearchResults.value = emptyList()
        }
    }

    suspend fun searchUsers(query: String): List<UserProfile> {
        if(query.isBlank() || query.length < 3) return emptyList()

        val normalizedQuery = query.lowercase().trim()
        val foundUsers = mutableSetOf<UserProfile>()

        try {
            val allUsersSnapshot = usersRef
                .get().await()

            allUsersSnapshot.children.forEach { dataSnapshot ->
                dataSnapshot.getValue(UserProfile::class.java)?.let { user ->
                    val userEmailLower = user.email.lowercase()
                    val userNameLower = user.displayName.lowercase()

                    if (userEmailLower.contains(normalizedQuery) || userNameLower.contains(normalizedQuery)) {
                        foundUsers.add(user.copy(uid = dataSnapshot.key ?: ""))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Firebase user search failed", e)
            return emptyList()
        }

        val currentUserId = firebaseAuth.currentUser?.uid
        return foundUsers.filterNot { it.uid == currentUserId}.toList().sortedBy { it.displayName }
    }

    fun clearAddFriendSearch() {
        _addFriendSearchQuery.value = ""
        _addFriendSearchResults.value = emptyList()
        _isSearchingUsers.value = false
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    fun logoutUser() {
        firebaseAuth.signOut()
        _privateChannels.value = emptyList()
        _groupChannels.value = emptyList()
        _friendUids.value = emptySet()
        _incomingFriendRequests.value = emptyList()
        _sentFriendRequests.value = emptyList()
        _allUsers.value = emptyList()
        _searchQuery.value = ""
        clearAddFriendSearch()
        _snackbarMessage.value = "Logged out successfully."

        // Signal navigation
        _navigateToLogin.value = true
        Log.d("HomeViewModel", "User logged out.")
    }

    fun onLoginNavigationComplete() {
        _navigateToLogin.value = false
    }
}