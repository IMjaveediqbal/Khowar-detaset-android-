package com.example.auth

import android.content.Context
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(viewModel: AuthViewModel = viewModel()) {
    var createAccount by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    val state by viewModel.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Khowar Dataset", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(if (createAccount) "Create your contributor account" else "Welcome back")
        Spacer(Modifier.height(24.dp))

        if (createAccount) {
            OutlinedTextField(
                value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Full name") }, singleLine = true
            )
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(
            value = email, onValueChange = { email = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true
        )
        if (createAccount) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = confirmPassword, onValueChange = { confirmPassword = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Confirm password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true
            )
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

        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = {
                scope.launch { signInWithGoogle(context, viewModel) }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is AuthState.Loading
        ) { Text("Continue with Google") }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = { createAccount = !createAccount }) {
                Text(if (createAccount) "Already have an account? Login" else "Create an account")
            }
        }

        if (!createAccount) {
            TextButton(onClick = { viewModel.resetPassword(email) }, modifier = Modifier.fillMaxWidth()) {
                Text("Forgot password?")
            }
        }

        when (val s = state) {
            is AuthState.Error -> {
                Spacer(Modifier.height(12.dp))
                Text(s.message, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            }
            AuthState.Loading -> {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                    Text("Authenticating…")
                }
            }
            else -> Unit
        }
    }
}

private suspend fun signInWithGoogle(context: Context, viewModel: AuthViewModel) {
    try {
        val credentialManager = CredentialManager.create(context)
        val googleOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(com.example.R.string.default_web_client_id))
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            viewModel.signInWithGoogleIdToken(googleCredential.idToken)
        } else {
            viewModel.signInWithGoogleIdToken("")
        }
    } catch (e: Exception) {
        viewModel.googleSignInFailed(e)
    }
}
