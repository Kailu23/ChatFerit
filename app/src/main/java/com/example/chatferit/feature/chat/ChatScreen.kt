package com.example.chatferit.feature.chat

import android.Manifest
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.chatferit.R
import com.example.chatferit.feature.home.ChannelItem
import com.example.chatferit.model.Message
import com.example.chatferit.ui.theme.DarkGray
import com.example.chatferit.ui.theme.Purple
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    navController: NavController,
    channelId: String,
    channelName: String,
    receiverId: String
) {
    val viewModel: ChatViewModel = hiltViewModel()
    val context = LocalContext.current

    val selectDialog = remember { mutableStateOf(false) }
    val cameraImageUri = remember {mutableStateOf<Uri?>(null)}

    LaunchedEffect(Unit) {
        viewModel.sendMessageError.collect { errorMessage ->
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.decryptionError.collect { errorMessage ->
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    val cameraImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri.value?.let {
                    viewModel.SendImageMessage(it, channelId, receiverId)
            }
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
            uri?.let { viewModel.SendImageMessage(it, channelId, receiverId) }
    }

    fun createImageUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir =
            ContextCompat.getExternalFilesDirs(
                context,
                Environment.DIRECTORY_PICTURES
            ).first()
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
                cameraImageUri.value = Uri.fromFile(this)
            }
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                cameraImageLauncher.launch(createImageUri())
            } else {
                Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    Scaffold(
        containerColor = Color.Black
    ) {paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            LaunchedEffect(key1 = channelId) {
                viewModel.ListenForMessages(channelId)
            }

            val messages = viewModel.messages.collectAsState().value

            ChatMessages(
                channelName =  channelName,
                messages = messages,
                onSendMessage = { textMessage, isEncrypted ->
                    viewModel.SendMessage(channelId =  channelId, receiverId = receiverId, sendText = textMessage, isEncrypted = isEncrypted)
                },
                onImageClicked = { selectDialog.value = true }
            )
        }


        if (selectDialog.value) {
            ContentSelectionDialog(
                onDismiss = { selectDialog.value = false },
                onCameraSelected = {
                    selectDialog.value = false
                    when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                        android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                            cameraImageLauncher.launch(createImageUri())
                        }

                        else -> {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                }, onGallerySelected = {
                    selectDialog.value = false
                    imageLauncher.launch("image/*")
                }
            )
        }
    }
}

@Composable
fun ContentSelectionDialog(
    onCameraSelected: () -> Unit,
    onGallerySelected: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss },
        confirmButton = {
            TextButton(onClick = {
                onCameraSelected()
                onDismiss()
            }) {
                Text(
                    text = "Camera",
                    color = DarkGray
                )
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onGallerySelected()
                onDismiss()
            }) {
                Text(
                    text = "Gallery",
                    color = DarkGray
                )
            }
        }, title = { Text(text = "Select your source.") },
        text = { Text(text = "Would you like to pick an image from the gallery or use the camera?") })


}

@Composable
fun ChatMessages(
    channelName: String,
    messages: List<Message>,
    onSendMessage: (String, Boolean) -> Unit,
    onImageClicked: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var sendEncrypted by remember { mutableStateOf(true) }

    val hideKeyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                ChannelItem(
                    channelName,
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }
            items(messages, key = {it.id}) { message ->
                ChatBubble(message = message)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkGray)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onImageClicked
            ) {
                Image(
                    painter = painterResource(R.drawable.outline_attach_file_24),
                    contentDescription = "Attach File",
                    modifier = Modifier.size(24.dp)
                )
            }
            TextField(
                value = messageText, onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(text = "Type a message") },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText, sendEncrypted)
                            messageText = ""
                            hideKeyboardController?.hide()
                        }
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkGray,
                    unfocusedContainerColor = DarkGray,
                    disabledContainerColor = DarkGray,
                    cursorColor = Purple,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedPlaceholderColor = Color.Gray,
                    unfocusedPlaceholderColor = Color.Gray
                )
            )

            IconButton(onClick = {sendEncrypted = !sendEncrypted}) {
                Icon(
                    imageVector = if(sendEncrypted) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = if(sendEncrypted) "Send Encrypted" else "Send Unencrypted",
                    tint = if (sendEncrypted) Purple else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        onSendMessage(messageText, sendEncrypted)
                        messageText = ""
                        hideKeyboardController?.hide()
                    }
                },
                enabled = messageText.isNotBlank()
            ) {
                Image(
                    painter = painterResource(R.drawable.send),
                    contentDescription = "Send",
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message) {
    val isCurrentUser: Boolean = message.senderId == Firebase.auth.currentUser?.uid
    val bubbleColor = if (isCurrentUser) Purple else DarkGray
    val textColor = Color.White

    val bubbleAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        contentAlignment = bubbleAlignment
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .background(color = bubbleColor, shape = RoundedCornerShape(16.dp))
                .padding(
                    if (message.imageUrl != null) PaddingValues(8.dp) else PaddingValues(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    )
                ),
            verticalAlignment = Alignment.Top
        ) {
            if (!isCurrentUser && message.senderImage != null) {
                AsyncImage(
                    model = message.senderImage,
                    contentDescription = "${message.senderName}'s avatar",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
            }else if (!isCurrentUser) {
                Image(
                    painter = painterResource(id = R.drawable.ic_android_black_24dp),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(end = 8.dp)
                )
            }
            Column()
            {
                if (!isCurrentUser && message.senderName != null) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                if (message.imageUrl != null) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Send image",
                        modifier = Modifier
                            .sizeIn(maxWidth = 240.dp, maxHeight = 240.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = message.plainTextMessage ?:"",
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }


        }


    }

}