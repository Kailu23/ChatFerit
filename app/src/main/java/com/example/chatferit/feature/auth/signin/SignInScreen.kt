package com.example.chatferit.feature.auth.signin

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.chatferit.R
import com.example.chatferit.feature.auth.AuthScreenState
import com.example.chatferit.feature.auth.AuthViewModel
import com.example.chatferit.feature.auth.KeySetupState


@Composable
fun SignInScreen(navController: NavController) {

    val viewModel: AuthViewModel = hiltViewModel()
    val authScreenState by viewModel.authScreenState.collectAsState()
    val keySetupState by viewModel.keySetupState.collectAsState()

    var email by remember {
        mutableStateOf(value = "")
    }
    var password by remember {
        mutableStateOf(value = "")
    }
    val context = LocalContext.current

    LaunchedEffect(key1 = authScreenState, key2 = keySetupState)
    {
        if (authScreenState is AuthScreenState.AuthSuccess && keySetupState is KeySetupState.Success) {
            Toast.makeText(context, "Sign In Successful!", Toast.LENGTH_SHORT).show()
            navController.navigate("home") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
    }
    LaunchedEffect(authScreenState) {
        if (authScreenState is AuthScreenState.AuthError) {
            Toast.makeText(
                context,
                (authScreenState as AuthScreenState.AuthError).message,
                Toast.LENGTH_LONG
            ).show()

        }
    }

    LaunchedEffect(keySetupState) {
        if (keySetupState is KeySetupState.Error) {
            Toast.makeText(
                context,
                (keySetupState as KeySetupState.Error).message,
                Toast.LENGTH_LONG
            ).show()
//            navController.navigate("retry") // TODO(): Retry button
        }
    }
    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo), contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .background(Color.White)
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(text = "E-mail") })
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(text = "Password") },
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.size(16.dp))

            if (authScreenState is AuthScreenState.Loading || keySetupState is KeySetupState.Loading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.signInWithEmailAndPassword(email, password) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = email.isNotEmpty() && password.isNotEmpty()
                ) {
                    Text(text = "Sign in")
                }
            }
            TextButton(onClick = { navController.navigate("signup") }) {
                Text(text = "Don't have an account? Sign up.")
            }
        }
    }
}

@Preview
@Composable
private fun PreviewSignInScreen() {
    SignInScreen(navController = rememberNavController( ))
}