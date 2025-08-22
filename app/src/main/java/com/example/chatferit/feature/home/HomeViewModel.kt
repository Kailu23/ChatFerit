package com.example.chatferit.feature.home

import android.util.Log
import android.util.Log.e
import androidx.lifecycle.ViewModel

import com.google.firebase.Firebase
import com.google.firebase.database.database
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

    }
}