package com.example.chatferit.feature.friends

import android.view.PixelCopy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatferit.model.Friend
import com.example.chatferit.model.FriendRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.filterNot

@HiltViewModel
class FriendsViewModel @Inject constructor(
//    private val friendsRepository: FriendsRepository
//    private val channelRepository: ChannelRepository
) : ViewModel(){

    private val _allFriends = MutableStateFlow<List<Friend>>(emptyList())
    private val _incomingRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _showAddFriendDialog = MutableStateFlow(false)


    val friends: StateFlow<List<Friend>> =
        combine(_allFriends, _searchQuery) { friends, searchQuery ->
            FilterFriends(friends, searchQuery)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val incomingFriendRequests = _incomingRequests.asStateFlow()
    val searchQuery = _searchQuery.asStateFlow()
    val showAddFriendDialog: StateFlow<Boolean> = _showAddFriendDialog.asStateFlow()

    init {
        loadFriends()
        loadFriendRequests()
    }

    private fun loadFriends() {
        viewModelScope.launch() {
            // TODO(): Replace later with actual friendsRepository.getFriends()
            _allFriends.value = listOf(
                Friend(id = "user2", name = "Alice Wonderland", status = "Online"),
                Friend(id = "user3", name = "Bob The Builder")
            )
        }
    }
    private fun loadFriendRequests() {
        viewModelScope.launch {
            // TODO: Replace with actual friendRepository.getIncomingFriendRequests()
            _incomingRequests.value = listOf(
                FriendRequest(id = "req1", senderId = "user4", senderName = "Charlie WantsToAddU"),
                FriendRequest(id = "req2", senderId = "user5", senderName = "Diana Prince Request")
            )
        }
    }
    private fun FilterFriends(friends : List<Friend>, searchQuery : String) : List<Friend>{
        val friendsList = mutableListOf<Friend>()
        if (searchQuery.isBlank()) {
            return friends
        }
        for(friend in friends){
            if (friend.name.contains(searchQuery, ignoreCase = true)) {
                friendsList.add(friend)
            }
        }

        return friendsList
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
    fun onAddFriendClicked() {
        _showAddFriendDialog.value = true
    }
    fun onAddFriendDialogDismiss() {
        _showAddFriendDialog.value = false

    }
    fun submitSendFriendRequest(targetUser: String) {
        viewModelScope.launch {
            // TODO: 1. Validate targetUserIdOrEmail (e.g., check if user exists)
            // TODO: 2. Call friendRepository.sendFriendRequest(currentUserId, targetUserId)
            println("Sent friend request to: $targetUser")
            _showAddFriendDialog.value = false
        }
    }
    fun acceptFriendRequest(request: FriendRequest) {
        viewModelScope.launch {
            // TODO: 1. Call friendRepository.acceptFriendRequest(request)
            // TODO: 2. On success, add to _allFriends list (or refetch)
            // TODO: 3. Remove from _incomingRequests list
            // TODO: 4. IMPORTANT: Trigger creation of a direct message channel
            //          - This might involve getting the current user's ID.
            //          - val currentUserId = "your_current_user_id" // Get this from auth service
            //          - val channelName = determineDirectChatName(currentUserId, request.senderId)
            //          - val members = listOf(currentUserId, request.senderId)
            //          - channelRepository.createDirectChannel(channelName, members)
            //          - Or, the friendRepository.acceptFriendRequest could handle this internally.

            println("Accepted friend request from: ${request.senderName}")
            // Optimistic update (remove if real backend handles this)
            _incomingRequests.value = _incomingRequests.value.filterNot { it.id == request.id }
            _allFriends.value = _allFriends.value + Friend(
                id = request.senderId,
                name = request.senderName,
                profileImageUrl = request.senderProfileImageUrl
            )
        }
    }
    fun declineFriendRequest(request: FriendRequest) {
        viewModelScope.launch {
            // TODO: Call friendRepository.declineFriendRequest(request)
            // TODO: On success, remove from _incomingRequests list
            println("Declined friend request from: ${request.senderName}")
            _incomingRequests.value = _incomingRequests.value.filterNot {
                it.id == request.id }
        }
    }
    fun onFriendClicked(friend: Friend) {
        // TODO: Navigate to the direct chat channel with this friend
    }

}