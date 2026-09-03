package com.example.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    authService: AuthService,
    onAuthenticated: (displayName: String, username: String, region: String) -> Unit
) {
    var signUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("Chitral") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Khowar Dataset", style = MaterialTheme.typography.headlineMedium)
        Text(
            if (signUp) "Create a verified contributor account" else "Sign in to contribute",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(16.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                if (signUp) {
                    OutlinedTextField(displayName, { displayName = it }, Modifier.fillMaxWidth(), label = { Text("Display name") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text("Username") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(region, { region = it }, Modifier.fillMaxWidth(), label = { Text("Region") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                }

                OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    password,
                    { password = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(Modifier.height(16.dp))

                Button(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        message = null
                        scope.launch {
                            val result = if (signUp) {
                                authService.signUp(email, password)
                            } else {
                                authService.signIn(email, password).map { verified ->
                                    if (!verified) throw IllegalStateException("Please verify your email before contributing.")
                                }
                            }
                            busy = false
                            result.onSuccess {
                                if (signUp) {
                                    message = "Verification email sent. Verify your email, then sign in."
                                } else {
                                    onAuthenticated(displayName, username, region)
                                }
                            }.onFailure { message = it.message ?: "Authentication failed." }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (signUp) "Create account" else "Sign in")
                }

                TextButton(onClick = { signUp = !signUp; message = null }) {
                    Text(if (signUp) "Already have an account? Sign in" else "Create a contributor account")
                }

                TextButton(
                    enabled = !busy && email.isNotBlank(),
                    onClick = {
                        busy = true
                        scope.launch {
                            authService.sendPasswordReset(email)
                                .onSuccess { message = "Password reset email sent." }
                                .onFailure { message = it.message ?: "Could not send reset email." }
                            busy = false
                        }
                    }
                ) { Text("Forgot password?") }

                message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
