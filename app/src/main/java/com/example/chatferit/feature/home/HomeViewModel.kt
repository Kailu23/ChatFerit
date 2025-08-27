package com.example.chatferit.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.chatferit.model.Channel
import com.google.firebase.database.FirebaseDatabase
import javax.inject.Inject




@HiltViewModel
class HomeViewModel @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase
) : ViewModel() {

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels = _channels.asStateFlow()

    private val _selectedScreenRoute = MutableStateFlow(BottomNavItem.Chats.route) // Default to Chats
    val selectedScreenRoute = _selectedScreenRoute.asStateFlow()

    private val _showAddChannelDialog = MutableStateFlow(false)
    val showAddChannelDialog= _showAddChannelDialog.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery= _searchQuery.asStateFlow()


    init {
        getChannels()
    }

    private fun getChannels() {
        firebaseDatabase.getReference("channel").get().addOnSuccessListener {
            val list = mutableListOf<Channel>()
            it.children.forEach { data ->
                val channel = Channel(data.key!!, data.value.toString())
                list.add(channel)
            }
            _channels.value = list
        }
    }
    fun addChannel(name: String) {
        val ref = firebaseDatabase.getReference("channel").push()
        val key = ref.key ?: run { Log.e("Add Channel", "Failed to generate key")
        return}
        ref.setValue(name)
            .addOnSuccessListener {
                getChannels()
            }
        _showAddChannelDialog.value = false
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
        // Optionally trigger search/filter logic here
    }
}