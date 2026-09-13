package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.KhowarViewModel

@Composable
fun ProfileScreen(viewModel: KhowarViewModel, modifier: Modifier = Modifier) {
    val user by viewModel.currentUser.collectAsState()
    val operations by viewModel.syncOperations.collectAsState()
    var email by rememberSaveable(user?.id) { mutableStateOf(user?.email.orEmpty()) }
    var name by rememberSaveable(user?.id) { mutableStateOf(user?.displayName.orEmpty()) }
    var region by rememberSaveable(user?.id) { mutableStateOf(user?.region ?: "Chitral") }
    // Password is intentionally neither saved nor persisted.
    var password by remember { mutableStateOf("") }
    var create by rememberSaveable { mutableStateOf(false) }
    var confirmWithdrawal by remember { mutableStateOf(false) }
    LazyColumn(modifier.fillMaxSize(), contentPadding=PaddingValues(20.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Text(if(user == null) "Sign in" else "Your profile", style=MaterialTheme.typography.headlineSmall) }
        if (user == null) {
            item { OutlinedTextField(email,{email=it},label={Text("Email")},singleLine=true,modifier=Modifier.fillMaxWidth()) }
            item { OutlinedTextField(password,{password=it},label={Text("Password")},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth()) }
            if(create) item { OutlinedTextField(name,{name=it},label={Text("Display name")},modifier=Modifier.fillMaxWidth()) }
            item { Button(onClick={viewModel.authenticate(email,password,create,name);password=""},enabled=email.isNotBlank()&&password.length>=8&&(!create||name.length>=2)) { Text(if(create) "Create account" else "Sign in") } }
            item { TextButton(onClick={create=!create}) { Text(if(create) "Already have an account? Sign in" else "Create a new account") } }
        } else {
            item { Text("${user!!.email} · ${user!!.role}") }
            item { OutlinedTextField(name,{name=it},label={Text("Display name")},modifier=Modifier.fillMaxWidth()) }
            item { OutlinedTextField(region,{region=it},label={Text("Region")},modifier=Modifier.fillMaxWidth()) }
            item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { Button(onClick={viewModel.loginOrRegister(email,name,user!!.username,user!!.role,region)}) { Text("Save profile") }; TextButton(onClick={viewModel.signOut()}) { Text("Sign out") } } }
            item { Text("Uploads",style=MaterialTheme.typography.titleLarge); Text("Saved on this phone until the server confirms upload."); TextButton(onClick={viewModel.retrySync()}) { Text("Sync / retry failed uploads") } }
            items(operations,key={it.key}) { operation ->
                Column { Text("${operation.collection} · ${operation.state}"); if(operation.error.isNotEmpty()) Text(operation.error,color=MaterialTheme.colorScheme.error) }
            }
            item { OutlinedButton(onClick={confirmWithdrawal=true}) { Text("Withdraw consent for all my contributions") } }
        }
    }
    if(confirmWithdrawal) AlertDialog(onDismissRequest={confirmWithdrawal=false}, title={Text("Withdraw your contributions?")},text={Text("This archives your cloud records and excludes them from future exports. Previously downloaded copies cannot be recalled. An internet connection is required.")},confirmButton={TextButton(onClick={confirmWithdrawal=false;viewModel.withdrawConsent("ALL_USER_RECORDS",user!!.id)}){Text("Withdraw")}},dismissButton={TextButton(onClick={confirmWithdrawal=false}){Text("Cancel")}})
}
