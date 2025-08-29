package com.example.chatferit.feature.users

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.chatferit.model.FriendRequest
import com.example.chatferit.model.UserProfile
import com.example.chatferit.ui.theme.DarkGray

@Composable
fun UsersTabContent(
    friendsList: List<UserProfile>,
    friendRequestsList: List<FriendRequest>,
    searchQuery: String,
    showAddFriendDialog: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onFriendClicked: (UserProfile) -> Unit,
    onAddFriendClicked: () -> Unit,
    onDismissAddFriendDialog: () -> Unit,
    onSubmitSendFriendRequest: (String) -> Unit,
    onAcceptRequest: (FriendRequest) -> Unit,
    onDeclineRequest: (FriendRequest) -> Unit
) {

    Scaffold (
        floatingActionButton = {
            FloatingActionButton(onClick = onAddFriendClicked) {
                Icon(Icons.Filled.Add, contentDescription = "Add Friend")
            }
        }, containerColor = Color.Black
    ){ paddingValues ->
        Column(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()) {
            Text(
                text = "Friends & Requests",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
            )
            if (friendRequestsList.isNotEmpty()) {
                SectionTitle("Friend Requests (${friendRequestsList.size})")
                LazyColumn(modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)){
                    items(friendRequestsList, key = {it.id}){request ->
                        FriendRequestItem(request = request, onAccept = {onAcceptRequest(request)},
                            onDecline = {onDeclineRequest(request)}
                        )
                        HorizontalDivider(color = DarkGray.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            // Friends section
            SectionTitle("Your friends (${friendsList.size})")
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(text = "Search") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.Gray,
                    unfocusedBorderColor = DarkGray,
                    focusedContainerColor = DarkGray.copy(alpha = 0.3f),
                    unfocusedContainerColor = DarkGray.copy(0.3f),
                    focusedPlaceholderColor = Color.Gray, unfocusedPlaceholderColor = DarkGray
                )
            )
//            SearchBar(searchQuery = searchQuery, onSearchQueryChanged = {onSearchQueryChanged})
            if (friendsList.isEmpty() && searchQuery.isBlank() && friendRequestsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ){
                    Text(
                        text = "No friends or requests yet. Click '+' to add friends!",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }else if (friendsList.isEmpty() && searchQuery.isNotBlank())
            {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ){
                    Text(
                        text = "No friends found for \"$searchQuery\"",
                        color = Color.Gray
                    )
                }
            }else if(friendsList.isNotEmpty()){
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(friendsList, key = { it.uid }) { userProfile ->
                        FriendItem(
                            userProfile = userProfile,
                            onClick = { onFriendClicked(userProfile) }
                        )
                        HorizontalDivider(
                            color = DarkGray.copy(alpha = 0.5f)
                        )
                    }
                }
            } else if (friendRequestsList.isNotEmpty() && friendsList.isEmpty() && searchQuery.isBlank()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ){
                    Text(
                        text = "You have no friends yet. Add some or accept requests!",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
    if (showAddFriendDialog) {
        AddFriendDialog(onDismiss = onDismissAddFriendDialog,
            onConfirm = { inputText ->
                onSubmitSendFriendRequest(inputText) }
        )
    }
}

@Composable
fun SectionTitle(title : String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.LightGray,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun FriendRequestItem(
    request: FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = request.senderProfileImageUrl
                        ?: "https://via.placeholder.com/150/CCCCCC/808080?Text=User"
                ),
                contentDescription = "${request.senderName}'s profile picture.",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = request.senderName, color = Color.White, fontWeight = FontWeight.Medium)
        }
        Row {
            IconButton(onClick = onAccept, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Check, contentDescription = "Accept request", tint = Color.Green.copy(alpha = 0.75f))
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onDecline, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Decline Request", tint = Color.Red.copy(alpha = 0.75f))
            }
        }
    }
}

@Composable
fun FriendItem(userProfile: UserProfile, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    )
    {
        Image(
            painter = rememberAsyncImagePainter(
                model = userProfile.profileImageUrl
                    ?: "https://via.placeholder.com/150/CCCCCC/808080?Text=User"
            ),
            contentDescription = "${userProfile.displayName}'s profile picture.",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(DarkGray),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column{
            Text(
                text = userProfile.displayName,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp
            )
            /*Text(
                text = userProfile.status,
                color = if (userProfile.status == "Online") Color.Green.copy(alpha = 0.75f) else Color.Gray,
                fontSize = 14.sp
            )*/
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Add New Friend", color = Color.White)
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(text = "Enter friend's email or username", color = Color.Gray, fontSize = 12.sp)},
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.LightGray.copy(alpha = 0.3f), unfocusedContainerColor = DarkGray.copy(0.3f),
                    disabledContainerColor = DarkGray.copy(0.3f),
                    focusedBorderColor = Color.LightGray, unfocusedBorderColor = Color.LightGray,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    cursorColor = Color.White
                )
            )
        },
        confirmButton = {
            Button(onClick = { if (text.isNotBlank()) onConfirm(text) })
            {
                Text(text = "Send Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = Color.LightGray)
            }
        },
        containerColor = DarkGray.copy(alpha = 0.9f)
    )
}