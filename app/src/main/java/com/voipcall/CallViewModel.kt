package com.voipcall

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voipcall.model.CallState
import com.voipcall.model.TrunkConfig
import com.voipcall.model.VoiceType
import com.voipcall.service.VoiceChangeService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CallViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "CallViewModel"
        private const val PREFS_NAME = "voipcall_prefs"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_SERVER_IP = "server_ip"
        private const val KEY_SERVER_PORT = "server_port"
        private const val KEY_LOCAL_PORT = "local_port"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    private val context = application.applicationContext
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val voiceChangeService = VoiceChangeService()

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean(KEY_IS_LOGGED_IN, false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _username = MutableStateFlow(prefs.getString(KEY_USERNAME, "") ?: "")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _currentVoiceType = MutableStateFlow(VoiceType.NORMAL)
    val currentVoiceType: StateFlow<VoiceType> = _currentVoiceType.asStateFlow()

    private val _callDuration = MutableStateFlow(0)
    val callDuration: StateFlow<Int> = _callDuration.asStateFlow()

    private val _trunkConfig = MutableStateFlow(loadTrunkConfig())
    val trunkConfig: StateFlow<TrunkConfig> = _trunkConfig.asStateFlow()

    private val callStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.voipcall.CALL_STATE_CHANGED" -> {
                    val state = intent.getStringExtra("state")
                    Log.d(TAG, "📱 Received call state from service: '$state'")

                    val newState = when (state) {
                        "OutgoingInit", "OutgoingProgress", "OutgoingRinging" -> {
                            Log.d(TAG, "  → Mapping to OUTGOING")
                            CallState.OUTGOING
                        }
                        "IncomingReceived", "IncomingEarlyMedia" -> {
                            Log.d(TAG, "  → Mapping to INCOMING")
                            CallState.INCOMING
                        }
                        "Connected", "StreamsRunning" -> {
                            Log.d(TAG, "  → Mapping to CONNECTED ✅")
                            CallState.CONNECTED
                        }
                        "End", "Released" -> {
                            Log.d(TAG, "  → Mapping to ENDED")
                            Log.d(TAG, "  → Resetting mute and speaker states")
                            _isMuted.value = false
                            _isSpeakerOn.value = false
                            _callDuration.value = 0
                            Log.d(TAG, "  → Will trigger auto-navigation in UI")
                            CallState.ENDED
                        }
                        "Error" -> {
                            Log.d(TAG, "  → Mapping to ERROR")
                            CallState.ERROR
                        }
                        else -> {
                            Log.w(TAG, "  → ⚠️ UNKNOWN state '$state', keeping current state: ${_callState.value}")
                            _callState.value
                        }
                    }

                    Log.d(TAG, "Setting callState from ${_callState.value} to $newState")
                    _callState.value = newState
                }
                "com.voipcall.MUTE_CHANGED" -> {
                    _isMuted.value = intent.getBooleanExtra("muted", false)
                }
                "com.voipcall.SPEAKER_CHANGED" -> {
                    _isSpeakerOn.value = intent.getBooleanExtra("speaker", false)
                }
                "com.voipcall.CALL_DURATION_UPDATE" -> {
                    val duration = intent.getIntExtra("duration", 0)
                    _callDuration.value = duration
                }
            }
        }
    }

    init {
        registerReceivers()
        if (_isLoggedIn.value) {
            startLinphoneService()
        }
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction("com.voipcall.CALL_STATE_CHANGED")
            addAction("com.voipcall.MUTE_CHANGED")
            addAction("com.voipcall.SPEAKER_CHANGED")
            addAction("com.voipcall.CALL_DURATION_UPDATE")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(callStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(callStateReceiver, filter)
        }
    }

    private fun startLinphoneService() {
        val intent = Intent(context, com.voipcall.service.LinphoneService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun updatePhoneNumber(number: String) {
        _phoneNumber.value = number
    }

    fun makeCall() {
        if (_phoneNumber.value.isEmpty()) {
            Log.w(TAG, "Cannot make call: phone number is empty")
            return
        }

        if (_trunkConfig.value.serverIp.isEmpty()) {
            Log.w(TAG, "Cannot make call: server IP not configured")
            _callState.value = CallState.ERROR
            return
        }

        _callState.value = CallState.OUTGOING

        val intent = Intent(context, com.voipcall.service.LinphoneService::class.java).apply {
            action = "MAKE_CALL"
            putExtra("number", _phoneNumber.value)
            putExtra("username", _username.value)
            putExtra("serverIp", _trunkConfig.value.serverIp)
            putExtra("serverPort", _trunkConfig.value.serverPort)
        }
        context.startService(intent)
    }

    fun hangupCall() {
        val intent = Intent(context, com.voipcall.service.LinphoneService::class.java).apply {
            action = "HANGUP_CALL"
        }
        context.startService(intent)

        _callState.value = CallState.ENDED
        _isMuted.value = false
        _isSpeakerOn.value = false
    }

    fun toggleMute() {
        val intent = Intent(context, com.voipcall.service.LinphoneService::class.java).apply {
            action = "TOGGLE_MUTE"
        }
        context.startService(intent)
    }

    fun toggleSpeaker() {
        val intent = Intent(context, com.voipcall.service.LinphoneService::class.java).apply {
            action = "TOGGLE_SPEAKER"
        }
        context.startService(intent)
    }

    fun changeVoiceType(voiceType: VoiceType) {
        Log.d(TAG, "═══ Voice change button clicked: ${voiceType.name} ═══")
        _currentVoiceType.value = voiceType

        // Extract email from username
        val email = voiceChangeService.extractEmailFromUsername(_username.value)
        if (email == null) {
            Log.e(TAG, "Failed to extract email from username: ${_username.value}")
            return
        }

        Log.d(TAG, "Calling voice change API: vmIp=98.70.40.108, email=$email, voiceType=${voiceType.name}")

        // Call the voice change API
        viewModelScope.launch {
            val success = voiceChangeService.changeVoice(
                vmIp = "98.70.40.108",
                email = email,
                voiceType = voiceType
            )

            if (success) {
                Log.d(TAG, "Voice type changed successfully to: ${voiceType.name}")
            } else {
                Log.e(TAG, "Failed to change voice type to: ${voiceType.name}")
            }
        }
    }

    fun updateTrunkConfig(config: TrunkConfig) {
        _trunkConfig.value = config
        _username.value = config.username
        saveTrunkConfig(config)

        // Update username in prefs
        prefs.edit().apply {
            putString(KEY_USERNAME, config.username)
            apply()
        }
    }

    private fun loadTrunkConfig(): TrunkConfig {
        return TrunkConfig(
            username = prefs.getString(KEY_USERNAME, "") ?: "",
            serverIp = prefs.getString(KEY_SERVER_IP, "") ?: "",
            serverPort = prefs.getInt(KEY_SERVER_PORT, 5060),
            localPort = prefs.getInt(KEY_LOCAL_PORT, 5060)
        )
    }

    private fun saveTrunkConfig(config: TrunkConfig) {
        prefs.edit().apply {
            putString(KEY_USERNAME, config.username)
            putString(KEY_SERVER_IP, config.serverIp)
            putInt(KEY_SERVER_PORT, config.serverPort)
            putInt(KEY_LOCAL_PORT, config.localPort)
            apply()
        }
    }

    fun login(username: String, serverIp: String, serverPort: Int) {
        _username.value = username

        val config = TrunkConfig(
            username = username,
            serverIp = serverIp,
            serverPort = serverPort,
            localPort = 5060
        )
        _trunkConfig.value = config
        saveTrunkConfig(config)

        prefs.edit().apply {
            putString(KEY_USERNAME, username)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }

        _isLoggedIn.value = true
        startLinphoneService()
    }

    fun logout() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            clear()
            apply()
        }

        _isLoggedIn.value = false
        _username.value = ""
        _trunkConfig.value = TrunkConfig()
        resetCallState()
    }

    fun resetCallState() {
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "resetCallState() called - navigating to dial screen")
        Log.d(TAG, "Current state: ${_callState.value} → IDLE")
        _callState.value = CallState.IDLE
        _phoneNumber.value = ""
        _isMuted.value = false
        _isSpeakerOn.value = false
        _currentVoiceType.value = VoiceType.NORMAL
        _callDuration.value = 0
        Log.d(TAG, "State reset complete - should show dial screen now")
        Log.d(TAG, "════════════════════════════════════════")
    }

    override fun onCleared() {
        super.onCleared()
        try {
            context.unregisterReceiver(callStateReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
    }
}
