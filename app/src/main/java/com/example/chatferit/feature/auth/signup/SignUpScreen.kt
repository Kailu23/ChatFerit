package com.example.chatferit.feature.auth.signup

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.chatferit.R

@Composable
fun SignUpScreen(navController: NavController) {
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
    Scaffold (modifier = Modifier.fillMaxSize()){
        Column (modifier = Modifier
            .padding(it)
            .padding(16.dp)
            .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Image(painter = painterResource(id = R.drawable.logo), contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .background(Color.White)
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it},
                placeholder = {Text(text = "Name")},
                label = { Text(text = "Name")})
            OutlinedTextField(
                value = surname,
                onValueChange = { surname = it},
                placeholder = {Text(text = "Surname")},
                label = { Text(text = "Surname")})
            OutlinedTextField(
                value = email,
                onValueChange = { email = it},
                placeholder = {Text(text = "E-mail")},
                label = { Text(text = "E-mail")})
            OutlinedTextField(
                value = password,
                onValueChange = { password = it},
                placeholder = {Text(text = "Password")},
                label = { Text(text = "Password")},
                visualTransformation = PasswordVisualTransformation())
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it},
                placeholder = {Text(text = "Confirm password")},
                label = { Text(text = "Confirm password")},
                visualTransformation = PasswordVisualTransformation(),
                isError = password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword)
            Spacer(modifier = Modifier.size(16.dp))

            Button(onClick = {/*TODO*/}, modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotEmpty() && surname.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty() && password == confirmPassword) {
                Text(text = "Sign up")
            }
            TextButton(onClick = {navController.popBackStack()}) {
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