package com.example.chatferit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.chatferit.ui.theme.ChatFeritTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ChatFeritTheme {
                navController = rememberNavController()
                MainApp(navController)
            }
        }

            Log.d("MainActivity", "onCreate: Handling initial intent.")
            handleIntentExtras(intent, initialLaunch = true)
    }
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "onNewIntent called with action: ${intent?.action} and data: ${intent?.dataString}")
        intent?.let {
            setIntent(it)
            handleIntentExtras(it, initialLaunch = false)
        }
    }

    private fun handleIntentExtras(intent: Intent, initialLaunch: Boolean) {
        val action = intent.action
        val data = intent.dataString
        Log.d("MainActivity", "handleIntentExtras called. Action: $action, Data: $data, InitialLaunch: $initialLaunch")

        val channelId = intent.getStringExtra("channelId")
        val channelName = intent.getStringExtra("channelName")
        val receiverId = intent.getStringExtra("receiverId")

        Log.d("MainActivity", "Intent extras: channelId=$channelId, channelName=$channelName, receiverId=$receiverId")

        if (channelId != null) {
            if (::navController.isInitialized) {
                val route = "chat/$channelId/${channelName ?: "Chat"}/${receiverId ?: "unknown_receiver"}"

                Log.d("MainActivity", "Navigating to: $route")
                navController.navigate(route) {
                    launchSingleTop = true
                }
            } else {
                Log.e("MainActivity", "NavController not initialized when trying to handle intent extras.")
            }

            intent.removeExtra("channelId")
            intent.removeExtra("channelName")
            intent.removeExtra("receiverId")

        } else {
            Log.d("MainActivity", "No 'channelId' extra found in intent. No navigation from notification.")
        }
    }

}