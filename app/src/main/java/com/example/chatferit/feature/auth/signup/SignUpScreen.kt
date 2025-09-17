package com.example.chatferit.feature.auth.signup

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
fun SignUpScreen(navController: NavController) {
    val viewModel : AuthViewModel = hiltViewModel()
    val authScreenState by viewModel.authScreenState.collectAsState()
    val keySetupState by viewModel.keySetupState.collectAsState()

    var name by remember {
        mutableStateOf(value = "")
    }
    var surname by remember {
        mutableStateOf(value = "")
    }
    var email by remember {
        mutableStateOf(value = "")
    }
    var password by remember {
        mutableStateOf(value = "")
    }
    var confirmPassword by remember {
        mutableStateOf(value = "")
    }
    val context = LocalContext.current

    LaunchedEffect(authScreenState) {
        when (val currentAuthScreenState = authScreenState) {
            is AuthScreenState.AuthSuccess -> {
                Toast.makeText(context, "Sign Up Fully Successful!", Toast.LENGTH_SHORT).show()
                navController.navigate("home") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
                viewModel.resetAuthScreenState() // Reset after handling
            }
            is AuthScreenState.AuthError -> {
                Toast.makeText(
                    context,
                    "Sign Up Error: ${currentAuthScreenState.message}",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetAuthScreenState() // Reset after handling
            }
            AuthScreenState.Loading -> {
                Log.d("SignUpScreen", "AuthScreenState is Loading")
            }
            AuthScreenState.Idle -> {
                Log.d("SignUpScreen", "AuthScreenState is Idle")
            }
        }
    }

    LaunchedEffect(keySetupState) {
        if (keySetupState is KeySetupState.Error) {
            Toast.makeText(
                context,
                (keySetupState as KeySetupState.Error).message,
                Toast.LENGTH_LONG
            ).show()
            navController.navigate("retry")
        }
    }

    Scaffold (modifier = Modifier.fillMaxSize()) {paddingValues ->

        Column (modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)
            .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Image(painter = painterResource(id = R.drawable.logo), contentDescription = null,
                modifier = Modifier
                    .size(150.dp)
            )

            Spacer(modifier = Modifier.size(24.dp))

            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorLabelColor = MaterialTheme.colorScheme.error,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                errorTextColor = MaterialTheme.colorScheme.onErrorContainer,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(text = "Name") },
                label = { Text(text = "Name") },
                colors = textFieldColors
            )
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedTextField(
                value = surname,
                onValueChange = { surname = it },
                placeholder = { Text(text = "Surname") },
                label = { Text(text = "Surname") },
                colors = textFieldColors
            )
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text(text = "E-mail") },
                label = { Text(text = "E-mail") },
                colors = textFieldColors
            )
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text(text = "Password") },
                label = { Text(text = "Password") },
                visualTransformation = PasswordVisualTransformation(),
                colors = textFieldColors
            )
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = { Text(text = "Confirm password") },
                label = { Text(text = "Confirm password") },
                visualTransformation = PasswordVisualTransformation(),
                isError = password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword,
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.size(16.dp))

            if (authScreenState is AuthScreenState.Loading || keySetupState is KeySetupState.Loading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        viewModel.signUpWithEmailAndPassword(
                            name = name,
                            surname = surname,
                            email = email,
                            password = password
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotEmpty() && surname.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty() && password == confirmPassword,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)
                    )
                ) {
                    Text(text = "Sign up")
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            TextButton(onClick = {navController.popBackStack()}, colors = ButtonDefaults.textButtonColors(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground
            ) ) {
                Text(text = "Already have an account? Sign In!")
            }
        }
    }
}

@Preview
@Composable
private fun PreviewSignUpScreen() {

    SignUpScreen(rememberNavController())

}