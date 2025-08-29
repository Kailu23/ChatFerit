package com.example.chatferit.feature.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.chatferit.feature.users.UsersTabContent
import com.example.chatferit.model.Channel
import com.example.chatferit.model.UserProfile
import com.example.chatferit.ui.theme.DarkGray
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {

    val viewModel: HomeViewModel = hiltViewModel()

    val privateChats by viewModel.privateChannels.collectAsState()
    val groupsChats by viewModel.groupChannels.collectAsState()
    val selectedScreenRoute by viewModel.selectedScreenRoute.collectAsState()
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid }
    val coroutineScope = rememberCoroutineScope()


    val actualFriendsList by viewModel.actualFriends.collectAsState()
    val incomingRequests by viewModel.incomingFriendRequests.collectAsState()
    val currentSearchQuery by viewModel.searchQuery.collectAsState()
    val showAddFriendDialog by viewModel.showAddFriendDialog.collectAsState()
    val addFriendSearchQuery by viewModel.addFriendSearchQuery.collectAsState()
    val addFriendSearchResults by viewModel.addFriendSearchResults.collectAsState()
    val isSearchingUsers by viewModel.isSearchingUsers.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val showAddGroupChannelDialog by viewModel.showAddChannelDialog.collectAsState()
    val addGroupChannelSheetState = rememberModalBottomSheetState()


    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            Log.d("HomeScreen", "Showing snackbar: $message")
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            viewModel.clearSnackbarMessage()
        }
    }

    if (showAddGroupChannelDialog) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onDismissAddChannelDialog() },
            sheetState = addGroupChannelSheetState
        ) {
            AddChannelDialog { channelName ->
                viewModel.addGroupChannel(channelName)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            when (selectedScreenRoute) {
                BottomNavItem.Groups.route -> {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Blue.copy(alpha = 0.65f))
                            .clickable()
                            {
                                viewModel.onAddChannelClicked()
                            }
                    ) {
                        Text(
                            text = "Add group",
                            modifier = Modifier.padding(16.dp),
                            color = Color.White
                        )
                    }
                }
            }

        },
        containerColor = Color.Black,
        bottomBar = {
            NavigationBar(
                containerColor = DarkGray,
            ) {
                bottomNavItemsList.forEach { item ->
                    NavigationBarItem(
                        selected = selectedScreenRoute == item.route,
                        onClick = {
                            viewModel.onBottomNavItemSelected(item.route)
                            /*if (selectedScreenRoute != item.route) {
                            }*/
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = Color.White,
                            unselectedTextColor = Color.DarkGray,
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.DarkGray,
                            indicatorColor = Color.DarkGray

                        )
                    )
                }
            }
        }

    ) { paddingValues ->
        Column (
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {

            when (selectedScreenRoute) {
                BottomNavItem.Chats.route -> {
                    SearchBar(
                        searchQuery = currentSearchQuery,
                        onSearchQueryChanged = { query -> viewModel.onSearchQueryChanged(query) }
                    )
                    ChatsScreenContent(
                        privateChats = privateChats,
                        searchQuery = currentSearchQuery,
                        currentUserId = currentUserId,
                        allUsers = viewModel.allUsers.collectAsState().value,
                        onChannelClick = { channelId, displayName, receiverId ->
                            if (currentUserId == null) {
                                Log.e("HomeScreen", "Cannot navigate: Current user ID is null.")
                                return@ChatsScreenContent
                            }

                            if (receiverId.isNotEmpty()) {
                                navController.navigate("chat/$channelId/$displayName/$receiverId")
                            } else {
                                Log.e(
                                    "HomeScreen",
                                    "Cannot navigate: Receiver ID is empty for: $channelId"
                                )
                            }
                        }
                    )
                }

                BottomNavItem.Users.route -> {
                    UsersTabContent(
                        friendsList = actualFriendsList,
                        friendRequestsList = incomingRequests,

                        searchQuery = currentSearchQuery,
                        onSearchQueryChanged = { query -> viewModel.onSearchQueryChanged(query) },

                        showAddFriendDialog = showAddFriendDialog,
                        onAddFriendClicked = { viewModel.onAddFriendClicked() },
                        onDismissAddFriendDialog = {
                            viewModel.onDismissAddFriendDialog()
                            viewModel.clearAddFriendSearch()
                        },

                        addFriendSearchQuery = addFriendSearchQuery,
                        addFriendSearchResults = addFriendSearchResults,
                        isSearchingUsers = isSearchingUsers,
                        onAddFriendSearchQueryChanged = { newDialogQuery ->
                            viewModel.onAddFriendSearchQueryChanged(newDialogQuery)
                        },
                        onClearAddFriendSearch = { viewModel.clearAddFriendSearch() },
                        onFriendClicked = { userProfile ->
                            coroutineScope.launch {
                                viewModel.getOrCreatePrivateChannel(userProfile.uid).fold(
                                    onSuccess = { (channelId, channelName) ->
                                        navController.navigate("chat/$channelId/$channelName/${userProfile.uid}")
                                    },
                                    onFailure = { error ->
                                        Log.e(
                                            "HomeScreen",
                                            "Failed to get/create DM",
                                            error
                                        )
                                    }
                                )
                            }
                        },
                        onSubmitSendFriendRequestWithUid = { selectedUserId ->
                            viewModel.submitSendFriendRequest(
                                selectedUserId
                            )
                        },
                        onAcceptRequest = { request -> viewModel.acceptFriendRequest(request) },
                        onDeclineRequest = { request -> viewModel.declineFriendRequest(request) },
                    )
                }

                BottomNavItem.Settings.route -> {
                    SettingsScreen(rememberNavController())
                }

            }
        }
    }
}
@Composable
fun SearchBar(searchQuery: String, onSearchQueryChanged: (String) -> Unit) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = { onSearchQueryChanged(it) },

        placeholder = { Text(text = "Search") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
        singleLine = true,
        shape = RoundedCornerShape(32.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color.LightGray,
            unfocusedBorderColor = DarkGray,
            focusedContainerColor = DarkGray.copy(alpha = 0.7f),
            unfocusedContainerColor = DarkGray.copy(0.1f),
            focusedPlaceholderColor = Color.Gray, unfocusedPlaceholderColor = DarkGray
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun ChatsScreenContent(
    privateChats: List<Channel>,
    searchQuery: String,
    currentUserId: String?,
    allUsers: List<UserProfile>,
    onChannelClick: (channelId: String, displayName: String, receiverId: String) -> Unit
) {
    if (privateChats.isEmpty()) {
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), contentAlignment = Alignment.Center) {
            Text(text = "No private chats yet. Start a new one from the Users tab!")
        }
        return
    }

    val chatDisplayItems = remember(privateChats, currentUserId, allUsers) {
        privateChats.mapNotNull { channel ->
            var displayName: String

            val otherParticipantId = channel.participants?.keys?.firstOrNull { it != currentUserId }

            if (otherParticipantId != null) {
                displayName = channel.participants.get(otherParticipantId)?.displayName ?: "Chat"
                ChannelDisplayWrapper(channel = channel, displayName = displayName, navigationReceiverId = otherParticipantId)
            } else {
                Log.w(
                    "ChatsScreenContent",
                    "Private channel ${channel.id} missing other participant id."
                )
                null
            }
        }
    }

    val filteredChats = if (searchQuery.isBlank()) {
        chatDisplayItems
    } else {
        chatDisplayItems.filter { channel ->
            channel.displayName.contains(searchQuery, ignoreCase = true)
        }
    }

    if (filteredChats.isEmpty()) {
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), contentAlignment = Alignment.Center) {
            Text(text = "No chats found for \"$searchQuery\"")
        }
    }

    LazyColumn {
        item {
            Text(
                text = "Private Chats",
                color = Color.Gray,
                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Black),
                modifier = Modifier.padding(16.dp)
            )
        }

        items(filteredChats, key = {it.channel.id}) { itemWrapper->
            ChannelItem(
                channelName = itemWrapper.displayName,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                onClick = { onChannelClick(
                    itemWrapper.channel.id,
                    itemWrapper.displayName,
                    itemWrapper.navigationReceiverId
                ) })

        }
    }

}

@Composable
fun ChannelItem(channelName : String, onClick : () -> Unit, modifier: Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkGray)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .size(70.dp)
                .clip(CircleShape)
                .background(Color.Yellow.copy(alpha = 0.3f))
        ) {
            Text(
                text = channelName[0].uppercase(),
                modifier = Modifier
                    .align(Alignment.Center),
                color = Color.White,
                style = TextStyle(fontSize = 32.sp),
                textAlign = TextAlign.Center,
            )
        }

        Text(
            text = channelName,
            modifier = Modifier
                .padding(8.dp),
            color = Color.White

        )


    }

}

@Composable
fun AddChannelDialog(onAddChannel: (String) -> Unit) {
    val channelName = remember {
        mutableStateOf("")
    }
    Column (
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(text = "Add Group Channel", fontSize = 26.sp)
        Spacer(modifier = Modifier.padding(8.dp))
        TextField(
            value = channelName.value, onValueChange = {
                channelName.value = it
            }, label = { Text(text = "Channel Name:")},
            singleLine = true
        )
        Spacer(modifier = Modifier.padding(8.dp))
        Button(onClick = { onAddChannel(channelName.value) }, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Add", fontSize = 24.sp)

        }

    }

}

private data class ChannelDisplayWrapper(
    val channel: Channel,
    val displayName: String,
    val navigationReceiverId: String
)