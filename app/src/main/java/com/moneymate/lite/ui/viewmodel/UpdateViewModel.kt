package com.moneymate.lite.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

data class AppUpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val updateUrl: String,
    val changelog: String
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadStatus = MutableStateFlow("")
    val downloadStatus: StateFlow<String> = _downloadStatus.asStateFlow()

    fun checkForUpdates(onComplete: (AppUpdateInfo?) -> Unit = {}) {
        viewModelScope.launch {
            _isChecking.value = true
            val info = fetchUpdateInfo()
            _updateInfo.value = info
            _isChecking.value = false
            
            // Return update details if a newer version exists
            val currentCode = getAppVersionCode()
            if (info != null && info.latestVersionCode > currentCode) {
                onComplete(info)
            } else {
                onComplete(null)
            }
        }
    }

    private suspend fun fetchUpdateInfo(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val spec = "https://raw.githubusercontent.com/Naresh-Selvan/MoneyMate-Lite/master/version.json"
        var connection: HttpURLConnection? = null
        try {
            val url = URL(spec)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.useCaches = false

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val json = JSONObject(response.toString())
                AppUpdateInfo(
                    latestVersionCode = json.getInt("latestVersionCode"),
                    latestVersionName = json.getString("latestVersionName"),
                    updateUrl = json.getString("updateUrl"),
                    changelog = json.optString("changelog", "New updates are available!")
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("UpdateViewModel", "Error checking for updates: ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }

    fun downloadAndInstallUpdate(apkUrl: String) {
        viewModelScope.launch {
            _isDownloading.value = true
            _downloadStatus.value = "Downloading update..."
            val result = downloadApk(apkUrl)
            _isDownloading.value = false
            result.fold(
                onSuccess = { apkFile ->
                    _downloadStatus.value = "Installing..."
                    if (canInstallPackages()) {
                        installApk(apkFile)
                    } else {
                        _downloadStatus.value = "Settings permission required. Please enable 'Allow from this source'."
                        launchUnknownSourcesSettings()
                    }
                },
                onFailure = { e ->
                    Log.e("UpdateViewModel", "Download failed", e)
                    _downloadStatus.value = "Download failed: ${e.message}"
                }
            )
        }
    }

    fun canInstallPackages(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun launchUnknownSourcesSettings() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("UpdateViewModel", "Failed to launch source settings", e)
            }
        }
    }

    private suspend fun downloadApk(spec: String): Result<java.io.File> = withContext(Dispatchers.IO) {
        try {
            val url = URL(spec)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("HTTP error: ${connection.responseCode}"))
            }

            val file = java.io.File(context.externalCacheDir ?: context.cacheDir, "update.apk")
            if (file.exists()) {
                file.delete()
            }

            val inputStream = connection.inputStream
            val outputStream = java.io.FileOutputStream(file)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            val totalLength = connection.contentLength
            var downloadedLength = 0
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloadedLength += bytesRead
                if (totalLength > 0) {
                    val progressPercent = (downloadedLength * 100L / totalLength).toInt()
                    _downloadStatus.value = "Downloading: $progressPercent%"
                }
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun installApk(file: java.io.File) {
        try {
            val authority = "${context.packageName}.provider"
            val apkUri: Uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("UpdateViewModel", "Failed to launch installer: ${e.message}", e)
            _downloadStatus.value = "Failed to open installer. Try opening standard downloads folder."
        }
    }

    fun getAppVersionCode(): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                (pInfo.longVersionCode and 0xFFFFFFFFL).toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    fun getAppVersionName(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }
}
