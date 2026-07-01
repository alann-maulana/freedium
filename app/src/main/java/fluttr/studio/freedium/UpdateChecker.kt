package fluttr.studio.freedium

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

sealed class UpdateResult {
    data class UpdateAvailable(val version: String, val url: String) : UpdateResult()
    object NoUpdate : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

object UpdateChecker {
    suspend fun checkUpdate(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://github.com/alann-maulana/freedium/releases/latest")
                .header("User-Agent", NetworkClient.DESKTOP_UA)
                .build()

            val response = NetworkClient.okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext UpdateResult.Error("HTTP ${response.code}")
            }

            val finalUrl = response.request.url.toString()
            val tagIndex = finalUrl.lastIndexOf("/tag/")
            if (tagIndex == -1) {
                return@withContext UpdateResult.Error("Failed to parse release version from URL")
            }

            val tagName = finalUrl.substring(tagIndex + 5)
            val apkUrl = "https://github.com/alann-maulana/freedium/releases/download/$tagName/Freedium-$tagName.apk"
            
            val latestVersion = tagName.removePrefix("v")
            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v")

            if (latestVersion != currentVersion) {
                UpdateResult.UpdateAvailable(latestVersion, apkUrl)
            } else {
                UpdateResult.NoUpdate
            }
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "Unknown error")
        }
    }
}
