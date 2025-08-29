package com.example.chatferit

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.chatferit.feature.auth.signin.SignInScreen
import com.example.chatferit.feature.auth.signup.SignUpScreen
import com.example.chatferit.feature.chat.ChatScreen
import com.example.chatferit.feature.home.HomeScreen
import com.example.chatferit.feature.home.SettingsScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MainApp()
{
    Surface(modifier = Modifier.fillMaxSize())
    {
        val navController = rememberNavController()

        val currentUser = FirebaseAuth.getInstance().currentUser
        val start = if (currentUser != null) "home" else "signin"
        NavHost(
            navController = navController,
            startDestination = start,
        )
        {
            composable("signin")
            {
                SignInScreen(navController)
            }
            composable("signup")
            {
                SignUpScreen(navController)
            }
            composable("home")
            {
                HomeScreen(navController)
            }
            composable("settings")
            {
                SettingsScreen(navController)
            }
            composable(
                "chat/{channelId}/{channelName}/{receiverId}", arguments = listOf(
                    navArgument(name = "channelId") {
                        type = NavType.StringType
                    },
                    navArgument("channelName")
                    {
                        type = NavType.StringType
                    },
                    navArgument("receiverId")
                    {
                        type = NavType.StringType
                    }
                )) {backStackEntry ->
                val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                val channelName = backStackEntry.arguments?.getString("channelName") ?: ""
                val receiverId = backStackEntry.arguments?.getString("receiverId") ?: ""

                Log.d("MainAppNav", "ChatScreenNav: channelId = '$channelId', channelName = '$channelName', receiverId = '$receiverId'")

                if (channelId.isNotEmpty() && receiverId.isNotEmpty()) {
                    ChatScreen(
                        navController = navController,
                        channelId = channelId,
                        channelName = channelName,
                        receiverId = receiverId
                    )
                } else {
                    Text("Error: Required chat information is missing. channelId or receiverId is empty. ")
                    Log.e("MainAppNav", "Error navigating to ChatScreen: Critical arguments missing. channelId = '$channelId', receiverId = '$receiverId'")
                }
            }
        }
    }
}