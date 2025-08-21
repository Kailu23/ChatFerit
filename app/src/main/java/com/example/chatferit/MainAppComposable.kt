package com.example.chatferit

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chatferit.feature.auth.signin.SignInScreen
import com.example.chatferit.feature.auth.signup.SignUpScreen
import com.example.chatferit.feature.home.HomeScreen
import com.google.firebase.Firebase
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
        }

    }
}