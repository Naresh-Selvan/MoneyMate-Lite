package com.moneymate.lite.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.moneymate.lite.ui.viewmodel.AppUpdateInfo

@Composable
fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Available (v${updateInfo.latestVersionName})") },
        text = {
            Text(
                "A new update is available. Here's what's new:\n\n${updateInfo.changelog}\n\nWould you like to download it now?"
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.updateUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback if browser can't open
                    }
                    onDismiss()
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}
