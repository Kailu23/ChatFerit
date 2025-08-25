package com.example.chatferit.feature.chat

import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.chatferit.R
import com.example.chatferit.model.Message
import com.example.chatferit.ui.theme.DarkGray
import com.example.chatferit.ui.theme.Purple
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import androidx.compose.ui.Alignment

@Composable
fun ChatScreen(navController: NavController, channelId : String) {

    val viewModel : ChatViewModel = hiltViewModel()
    Scaffold (
        containerColor = Color.Black
    ){

        val selectDialog = remember { mutableStateOf(false) }
        val cameraImageUri = remember { mutableStateOf<Uri?>(null) }
        val cameraImageLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) {
                success ->
                if (success) {
                    cameraImageUri.value?.let {
                        /*TODO()*/
                    }
                }
            }

        fun createImageUri(): Uri {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir =
                ContextCompat.getExternalFilesDirs(
                    navController.context,
                    Environment.DIRECTORY_PICTURES
                ).first()
            return FileProvider.getUriForFile(
                navController.context,
                "${navController.context.packageName}.provider",
                File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
                    cameraImageUri.value = Uri.fromFile(this)
                }
            )
        }
        val permissionLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    cameraImageLauncher.launch(createImageUri())
                }
            }

        Column (modifier = Modifier
            .fillMaxSize()
            .padding(it)){

            LaunchedEffect(key1 = true) {
                viewModel.ListenForMessages(channelId)
            }

            val messages = viewModel.messages.collectAsState()

            ChatMessages(
                messages = messages.value,
                onSendMessage = {message ->
                    viewModel.sendMessage(channelId, message)
                },
                onImageClicked = {selectDialog.value = true}
            )
        }


        if (selectDialog.value) {
            ContentSelectionDialog(onCameraSelected = {
                selectDialog.value = false
                if (navController.context.checkSelfPermission(Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    cameraImageLauncher.launch(createImageUri())
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }

            }, onGallerySelected = {
                selectDialog.value = false
//                imageLauncher.launch("image/*")
            })
        }
    }
}

@Composable
fun ContentSelectionDialog(onCameraSelected: () -> Unit, onGallerySelected: () -> Unit) {
    AlertDialog(onDismissRequest = {/*TODO()*/}, confirmButton = {
        TextButton (onClick = onCameraSelected) {
            Text(
                text = "Camera",
                color = DarkGray
            )
        }},
        dismissButton = {
            TextButton (onClick = onCameraSelected) {
                Text(
                    text = "Gallery",
                    color = DarkGray
                )
            }
            }, title = {Text(text = "Select your source.")},
        text = {Text(text = "Would you like to pick an image from the gallery or use the camera?") })





}

@Composable
fun ChatMessages(
    messages : List<Message>,
    onSendMessage : (String) -> Unit,
    onImageClicked: () -> Unit
) {
    val message = remember { mutableStateOf("") }

    val hideKeyboardController = LocalSoftwareKeyboardController.current
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn {
            items(messages) { message ->
                ChatBubble(message = message)
            }
        }

        Row (
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(DarkGray)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            IconButton(
                onClick = {
                    message.value = ""
                    onImageClicked()
                }
            ) {
                Image(painter = painterResource(R.drawable.outline_attach_file_24), contentDescription = "Attach File", alignment = Alignment.Center, modifier = Modifier.size(24.dp))
            }
            TextField(
                value = message.value, onValueChange = {message.value = it},
            modifier = Modifier.weight(1f),
                placeholder = { Text(text = "Type a message") },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        hideKeyboardController?.hide()
                    }
                ),
                colors = TextFieldDefaults.colors().copy(
                    focusedContainerColor = DarkGray,
                    unfocusedContainerColor = DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedPlaceholderColor = Color.White,
                    unfocusedPlaceholderColor = Color.White
                )
            )
            IconButton(
                onClick = {
                onSendMessage(message.value)
                message.value = "" },
                enabled = message.value.isNotEmpty()
                ) {
                Image(painter = painterResource(R.drawable.send), contentDescription = "Send", modifier = Modifier.size(24.dp), alignment = Alignment.Center)
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message) {
    val isCurrentUser = message.senderId == Firebase.auth.currentUser?.uid
    val bubbleColor = if (isCurrentUser) {
        Purple
    } else {
        DarkGray
    }
    Box (
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
    ){
        val alignment = if (!isCurrentUser) Alignment.CenterStart else Alignment.CenterEnd
        Row(
            modifier = Modifier
                .padding(8.dp)
                .background(color = bubbleColor, shape = RoundedCornerShape(8.dp))
                .align(alignment),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isCurrentUser) {
                Image(
                    painter = painterResource(id = R.drawable.ic_android_black_24dp),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = message.message.trim(),
                color = Color.White,
                modifier = Modifier
                    .padding(16.dp)
            )


        }


    }

}