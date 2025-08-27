package com.example.chatferit.feature.home

import android.R
import android.widget.Space
import androidx.compose.foundation.Image
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.chatferit.model.Channel
import com.example.chatferit.ui.theme.DarkGray


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val viewModel = hiltViewModel<HomeViewModel>()
    val channels by viewModel.channels.collectAsState()
    val showDialogState by viewModel.showAddChannelDialog.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    val selectedScreenRoute by viewModel.selectedScreenRoute.collectAsState()
    val currentSearchQuery by viewModel.searchQuery.collectAsState()

    if (showDialogState) {
        ModalBottomSheet(onDismissRequest = {viewModel.onDismissAddChannelDialog()}, sheetState = sheetState) {
            AddChannelDialog { channelName ->
                viewModel.addChannel(channelName)
            }
        }
    }

    Scaffold (
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Blue.copy(alpha = 0.65f))
                    .clickable()
                    {
                        viewModel.onAddChannelClicked()
                    }
            ){
                Text(
                    text = "Add channel",
                    modifier = Modifier.padding(16.dp),
                    color = Color.White
                )
            }
        },
        containerColor = Color.Black,
        bottomBar = {
            NavigationBar(containerColor = DarkGray,
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
                        label = {Text(item.label)},
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

    ){ paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            ChatsScreenContent(
                channels = channels,
                searchQuery = currentSearchQuery,
                onSearchQueryChanged = { query -> viewModel.onSearchQueryChanged(query) },
                onChannelClick = { channelId, channelName -> navController.navigate("chat/$channelId&$channelName") })

            }
        }
    }


@Composable
fun ChatsScreenContent(
    channels: List<Channel>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onChannelClick: (String, String) -> Unit
) {
    LazyColumn {
        item {
            Text(
                text = "Messages",
                color = Color.Gray,
                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Black),
                modifier = Modifier.padding(16.dp)
            )
        }

        item {
            TextField(
                value = searchQuery, onValueChange = {onSearchQueryChanged(it)},
                placeholder = { Text(text = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(40.dp)),

                textStyle = TextStyle(color = Color.Gray),
                colors = TextFieldDefaults.colors().copy(
                    focusedContainerColor = DarkGray,
                    unfocusedContainerColor = DarkGray,
                    focusedTextColor = Color.Gray,
                    unfocusedTextColor = Color.Gray,
                    focusedPlaceholderColor = Color.Gray,
                    unfocusedPlaceholderColor = Color.Gray,
                    focusedIndicatorColor = Color.Gray
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null
                    ) }
            )
        }

        items(channels.filter { it.name.contains(searchQuery, ignoreCase = true) }) { channel ->
            Column {
                ChannelItem(
                    channelName = channel.name,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    onClick = { onChannelClick(channel.id, channel.name) })
            }
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
        Text(text = "Add Channel", fontSize = 26.sp)
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
