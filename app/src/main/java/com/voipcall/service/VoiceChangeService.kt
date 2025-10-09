package com.voipcall.service

import android.util.Log
import com.voipcall.model.VoiceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class VoiceChangeService {

    companion object {
        private const val TAG = "VoiceChangeService"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Extracts email from username pattern: aparty_bparty_email-gmail-com_code
     * Example: 1000_1002_humu-gmail-com_901 -> humu-gmail-com
     */
    fun extractEmailFromUsername(username: String): String? {
        val parts = username.split("_")
        if (parts.size < 3) {
            Log.w(TAG, "Username doesn't match expected pattern: $username")
            return null
        }

        // Email is the 3rd part (index 2)
        val email = parts[2]
        Log.d(TAG, "Extracted email: $email from username: $username")
        return email
    }

    /**
     * Gets the voice code for the given voice type
     */
    private fun getVoiceCode(voiceType: VoiceType): String {
        return when (voiceType) {
            VoiceType.NORMAL -> "904"
            VoiceType.MALE -> "902"
            VoiceType.FEMALE -> "901"
            VoiceType.KID -> "903"
        }
    }

    /**
     * Calls the voice change API
     * @param vmIp The VM IP address
     * @param email The email extracted from username (format: humu-gmail-com)
     * @param voiceType The voice type to set
     */
    suspend fun changeVoice(vmIp: String, email: String, voiceType: VoiceType): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val code = getVoiceCode(voiceType)
                val url = "http://$vmIp/api/set-voice-by-email?email=$email&code=$code"

                Log.d(TAG, "Calling voice change API: $url")

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val success = response.isSuccessful

                if (success) {
                    Log.d(TAG, "Voice changed successfully to ${voiceType.name} (code: $code)")
                } else {
                    Log.e(TAG, "Voice change failed. Status: ${response.code}, Message: ${response.message}")
                }

                response.close()
                success
            } catch (e: Exception) {
                Log.e(TAG, "Error changing voice", e)
                false
            }
        }
    }
}
