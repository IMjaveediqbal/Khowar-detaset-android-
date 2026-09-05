package com.example.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AuthScreen(viewModel: AuthViewModel = viewModel()) {
    var createAccount by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Khowar Dataset")
        Spacer(Modifier.height(8.dp))
        Text(if (createAccount) "Create your contributor account" else "Sign in to continue")
        Spacer(Modifier.height(24.dp))

        if (createAccount) {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Full name") }, singleLine = true)
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        if (createAccount) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(confirmPassword, { confirmPassword = it }, Modifier.fillMaxWidth(), label = { Text("Confirm password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        }
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (createAccount) viewModel.signUp(name, email, password, confirmPassword)
                else viewModel.signIn(email, password)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is AuthState.Loading
        ) { Text(if (createAccount) "Create account" else "Login") }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { /* Google credential launcher will be added with the project's Firebase client ID */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Continue with Google")
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = { createAccount = !createAccount }) { Text(if (createAccount) "Already have an account? Login" else "Create an account") }
        }

        if (!createAccount) {
            TextButton(onClick = { viewModel.resetPassword(email) }, modifier = Modifier.fillMaxWidth()) {
                Text("Forgot password?")
            }
        }

        when (val s = state) {
            is AuthState.Error -> {
                Spacer(Modifier.height(12.dp))
                Text(s.message)
            }
            AuthState.Loading -> {
                Spacer(Modifier.height(12.dp))
                Text("Signing you in…")
            }
            else -> Unit
        }
    }
}
