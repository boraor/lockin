package com.oruncak.lockin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.oruncak.lockin.data.Repository

/**
 * Local-only account sheet. "Continue with Google" is wired to log a mock sign-in —
 * to make it real, add the Google Identity Services / Credential Manager library,
 * a Firebase project, google-services.json, and your release + debug SHA-1 fingerprints,
 * then swap onGoogleClick's body for a real Credential Manager request.
 */
@Composable
fun AuthSheet(repo: Repository, onDismiss: () -> Unit) {
    var mode by remember { mutableStateOf("signin") } // "signin" | "register"
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Text("Account", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(48.dp))
            }
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = mode == "signin",
                    onClick = { mode = "signin" },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Sign in") }
                SegmentedButton(
                    selected = mode == "register",
                    onClick = { mode = "register" },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Create account") }
            }
            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = {
                    // TODO: replace with a real Credential Manager / Google Sign-In call.
                    repo.signIn(name = "Oruncak", method = "google")
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Continue with Google") }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    val name = if (email.contains("@")) email.substringBefore("@") else "Account"
                    repo.signIn(name = name.replaceFirstChar { it.uppercase() }, method = "email")
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text(if (mode == "register") "Create account" else "Sign in") }
            Spacer(Modifier.height(12.dp))
            Text(
                "By continuing you agree to LockIn's Terms & Privacy Policy",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
