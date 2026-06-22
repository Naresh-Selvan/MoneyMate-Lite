package com.moneymate.lite.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymate.lite.BuildConfig
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
        // Raw JSON from your repository's main branch to check updates
        val spec = "https://raw.githubusercontent.com/Naresh-Selvan/MoneyMate-Lite/main/version.json"
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
