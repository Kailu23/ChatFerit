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
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
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
fun MainApp(navController: NavHostController)
{
    Surface(modifier = Modifier.fillMaxSize())
    {
        val currentUserState: State<FirebaseUser?> = observeFirebaseAuth()
        val currentUser: FirebaseUser? by currentUserState
        var hasPerformedInitialNavigation = remember { mutableStateOf(false) }

        val targetGraphRoute = if (currentUser != null) AppRoutes.HOME else AppRoutes.AUTH_GRAPH_ROUTE

        LaunchedEffect(currentUser, navController) { // Key on currentUser directly
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            Log.d("MainApp", "Auth Nav Check: User=${currentUser?.uid}, TargetGraph=$targetGraphRoute, CurrentRoute=$currentRoute, InitialNavDone=$hasPerformedInitialNavigation")

            if (!hasPerformedInitialNavigation.value) {
                navController.navigate(targetGraphRoute) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
                hasPerformedInitialNavigation.value = true
                Log.d("MainApp", "Performed initial navigation to $targetGraphRoute")
            } else {
                if (currentUser != null) {
                    if (currentRoute == AppRoutes.SIGN_IN || currentRoute == AppRoutes.SIGN_UP) {
                        Log.d("MainApp", "User logged IN, but on auth screen ($currentRoute). Deferring main navigation.")
                    } else if (currentRoute != AppRoutes.HOME && !currentRoute?.startsWith("chat/")!!) { // And not already home or in chat
                        Log.d("MainApp", "User logged IN, not on auth screen. Navigating to HOME from $currentRoute.")
                        navController.navigate(AppRoutes.HOME) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                } else {
                    if (currentRoute != AppRoutes.SIGN_IN && currentRoute != AppRoutes.AUTH_GRAPH_ROUTE) { // And not already on an auth screen
                        Log.d("MainApp", "User logged OUT. Navigating to AUTH_GRAPH from $currentRoute.")
                        navController.navigate(AppRoutes.AUTH_GRAPH_ROUTE) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = AppRoutes.AUTH_GRAPH_ROUTE,
        ) {
            navigation(
                route = AppRoutes.AUTH_GRAPH_ROUTE,
                startDestination = AppRoutes.SIGN_IN
            ) {
                composable(AppRoutes.SIGN_IN){
                    SignInScreen(navController)
                }
                composable(AppRoutes.SIGN_UP){
                    SignUpScreen(navController)
                }
            }
            composable(AppRoutes.HOME)
            {
                HomeScreen(navController)
            }
            composable(AppRoutes.SETTINGS)
            {
                SettingsScreen(navController)
            }
            composable(
                route = AppRoutes.CHAT_ROUTE,
                arguments = listOf(
                    navArgument("channelId") { type = NavType.StringType},
                    navArgument("channelName") { type = NavType.StringType},
                    navArgument("receiverId") { type = NavType.StringType}
                )
            ) { backStackEntry ->
                val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                val channelName = backStackEntry.arguments?.getString("channelName") ?: "Chat"
                val receiverId = backStackEntry.arguments?.getString("receiverId") ?: ""

                Log.d("MainAppNav", "ChatScreenNav: channelId = '$channelId', channelName = '$channelName', receiverId = '$receiverId'")

                if (channelId.isNotEmpty()) {
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