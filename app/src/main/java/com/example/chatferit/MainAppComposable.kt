package com.example.chatferit

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import com.google.firebase.auth.FirebaseUser


@Composable
fun observeFirebaseAuth(): State<FirebaseUser?> {
    val firebaseUser = remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, FirebaseAuth.getInstance()) {
        val authListener = FirebaseAuth.AuthStateListener { auth ->
            firebaseUser.value = auth.currentUser
            Log.d("MainApp", "Auth state changed. User: ${auth.currentUser?.uid}")
        }
        FirebaseAuth.getInstance().addAuthStateListener(authListener)

        onDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(authListener)
            Log.d("MainApp", "Auth state listener removed.")
        }
    }
    return firebaseUser
}

@Composable
fun MainApp()
{
    Surface(modifier = Modifier.fillMaxSize())
    {
        val navController = rememberNavController()

        val currentUserState: State<FirebaseUser?> = observeFirebaseAuth()
        val currentUser: FirebaseUser? by currentUserState

        val startDestination = if (currentUser != null) "home" else "signin"
        Log.d("MainApp", "Recalculating startDestination. CurrentUser: ${currentUser?.uid}, StartDest: $startDestination")


        LaunchedEffect(startDestination, navController) {
            val currentGraphStartRoute = navController.graph.findStartDestination().route
            if (currentGraphStartRoute != startDestination) {
                Log.d("MainApp", "Start destination changed from $currentGraphStartRoute to $startDestination. Navigating.")
                navController.navigate(startDestination) {
                    popUpTo(navController.graph.id) { // Pop the entire current graph
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            } else {
                Log.d("MainApp", "Start destination ($startDestination) matches current graph start ($currentGraphStartRoute). No explicit navigation needed by this LaunchedEffect.")
            }
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
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