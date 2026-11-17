package com.moneymate.lite.ui.screens

import android.app.Activity
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.moneymate.lite.ui.viewmodel.AuthViewModel
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val isOfflineMode by authViewModel.isOfflineMode.collectAsState()
    val isPasswordChangeRequired by authViewModel.isPasswordChangeRequired.collectAsState()
    val authError by authViewModel.authError.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Owner, 1: Worker

    // Worker login form state
    var workerUsername by remember { mutableStateOf("") }
    var workerPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isWorkerLoggingIn by remember { mutableStateOf(false) }

    // Password change dialog state (for mandatory first login change)
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var passwordChangeError by remember { mutableStateOf<String?>(null) }
    var isChangingPassword by remember { mutableStateOf(false) }

    // Show auth error toast if any
    LaunchedEffect(authError) {
        authError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            authViewModel.clearAuthError()
        }
    }

    // Auto-navigate if signed in (unless mandatory password change is pending) or in offline mode
    LaunchedEffect(currentUser, isOfflineMode, isPasswordChangeRequired) {
        if ((currentUser != null && !isPasswordChangeRequired) || isOfflineMode) {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    val googleSignInClient = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.moneymate.lite.R.string.default_web_client_id))
            .requestEmail()
            .build()
            .let { GoogleSignIn.getClient(context, it) }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            try {
                val account = withContext(Dispatchers.IO) {
                    Tasks.await(
                        GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    )
                }
                val idToken = account.idToken ?: throw Exception("Google ID Token is null. Ensure Google Play Services are updated and SHA-1 is configured in Firebase console.")
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                withContext(Dispatchers.IO) {
                    Tasks.await(
                        FirebaseAuth.getInstance().signInWithCredential(credential)
                    )
                }
            } catch (e: Exception) {
                val apiException = e as? com.google.android.gms.common.api.ApiException
                val isCancelled = apiException?.statusCode == 12501 || apiException?.statusCode == 16
                if (!isCancelled) {
                    Toast.makeText(
                        context,
                        "Sign in failed: ${e.localizedMessage ?: "Unknown error"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                try {
                    withContext(Dispatchers.IO) {
                        Tasks.await(googleSignInClient.signOut())
                    }
                } catch (ex: Exception) {
                    // Ignore
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.AccountBalance,
            contentDescription = "App logo",
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "MoneyMate Lite",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Personal & Shared Business Loan Manager",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Tabs: Owner vs Worker
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Owner Login") }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Worker Login") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedTabIndex == 0) {
            // Owner Login
            Button(
                onClick = {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                Tasks.await(googleSignInClient.signOut())
                            }
                        } catch (e: Exception) {
                            // Ignore
                        }
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4285F4)
                )
            ) {
                Text(
                    text = "Sign in with Google",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    authViewModel.setOfflineMode(true)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = "Continue Offline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            // Worker Login
            OutlinedTextField(
                value = workerUsername,
                onValueChange = { workerUsername = it },
                label = { Text("Worker Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = workerPassword,
                onValueChange = { workerPassword = it },
                label = { Text("Temporary or Current Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    isWorkerLoggingIn = true
                    authViewModel.signInWorker(
                        username = workerUsername,
                        password = workerPassword,
                        onSuccess = {
                            isWorkerLoggingIn = false
                        },
                        onError = { errorMsg ->
                            isWorkerLoggingIn = false
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isWorkerLoggingIn && workerUsername.isNotBlank() && workerPassword.isNotBlank()
            ) {
                if (isWorkerLoggingIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp).size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = if (isWorkerLoggingIn) "Signing in..." else "Sign in as Worker",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // Mandatory Password Change Dialog on First Login
    if (isPasswordChangeRequired && currentUser != null) {
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss without changing password */ },
            title = { Text("Change Temporary Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "For security reasons, you must change your temporary password before accessing the system.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            passwordChangeError = null
                        },
                        label = { Text("New Password (min 6 characters)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                                Icon(
                                    if (isNewPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        }
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            passwordChangeError = null
                        },
                        label = { Text("Confirm New Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    if (passwordChangeError != null) {
                        Text(
                            text = passwordChangeError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.length < 6) {
                            passwordChangeError = "Password must be at least 6 characters"
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            passwordChangeError = "Passwords do not match"
                            return@Button
                        }

                        isChangingPassword = true
                        authViewModel.changePassword(
                            newPassword = newPassword,
                            onSuccess = {
                                isChangingPassword = false
                                Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                isChangingPassword = false
                                passwordChangeError = err
                            }
                        )
                    },
                    enabled = !isChangingPassword && newPassword.isNotBlank() && confirmPassword.isNotBlank()
                ) {
                    if (isChangingPassword) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp).size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text("Save & Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { authViewModel.signOut() }) {
                    Text("Cancel & Sign Out")
                }
            }
        )
    }
}
