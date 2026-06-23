package com.moneymate.lite.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moneymate.lite.ui.viewmodel.AppUpdateInfo
import com.moneymate.lite.ui.viewmodel.UpdateViewModel

@Composable
fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    updateViewModel: UpdateViewModel,
    onDismiss: () -> Unit
) {
    val isDownloading by updateViewModel.isDownloading.collectAsState()
    val downloadStatus by updateViewModel.downloadStatus.collectAsState()

    AlertDialog(
        onDismissRequest = if (isDownloading) ({}) else onDismiss, // Prevent dismiss during download
        title = { Text("Update Available (v${updateInfo.latestVersionName})") },
        text = {
            Column {
                Text(
                    "A new update is available. Here's what's new:\n\n${updateInfo.changelog}\n\nWould you like to download and install it now?"
                )
                if (downloadStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = downloadStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!isDownloading) {
                        val directUrl = if (updateInfo.updateUrl.endsWith(".apk")) {
                            updateInfo.updateUrl
                        } else {
                            "https://github.com/Naresh-Selvan/MoneyMate-Lite/releases/download/v${updateInfo.latestVersionName}/app-release.apk"
                        }
                        updateViewModel.downloadAndInstallUpdate(directUrl)
                    }
                },
                enabled = !isDownloading
            ) {
                Text(if (isDownloading) "Downloading..." else "Update Now")
            }
        },
        dismissButton = {
            if (!isDownloading) {
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            }
        }
    )
}
