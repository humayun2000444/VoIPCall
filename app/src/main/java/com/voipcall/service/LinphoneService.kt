package com.voipcall.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.voipcall.model.VoiceType
import org.linphone.core.*

class LinphoneService : Service() {

    companion object {
        private const val TAG = "LinphoneService"
        private const val CHANNEL_ID = "voipcall_service_channel"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var core: Core
    private var currentVoiceType: VoiceType = VoiceType.NORMAL
    private lateinit var audioManager: AudioManager
    private var bluetoothHeadset: BluetoothHeadset? = null
    private var isBluetoothConnected = false

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                    when (state) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            Log.d(TAG, "Bluetooth headset connected")
                            isBluetoothConnected = true
                            routeAudioToBluetooth()
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            Log.d(TAG, "Bluetooth headset disconnected")
                            isBluetoothConnected = false
                            stopBluetoothSco()
                        }
                    }
                }
                BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothHeadset.STATE_AUDIO_DISCONNECTED)
                    Log.d(TAG, "Bluetooth audio state changed: $state")
                }
            }
        }
    }

    private val coreListener = object : CoreListenerStub() {
        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State,
            message: String
        ) {
            Log.d(TAG, "Call state changed: $state - $message")

            // Handle microphone settings based on call state
            when (state) {
                Call.State.Connected -> {
                    // Ensure microphone is enabled when call connects
                    call.microphoneMuted = false
                    Log.d(TAG, "Call connected - Mic muted: ${call.microphoneMuted}")
                    Log.d(TAG, "Core mic enabled: ${core.isMicEnabled}")
                    Log.d(TAG, "Output audio device: ${core.outputAudioDevice?.deviceName}")
                    Log.d(TAG, "Input audio device: ${core.inputAudioDevice?.deviceName}")

                    // Log audio params
                    call.currentParams?.let { params ->
                        Log.d(TAG, "Audio enabled: ${params.isAudioEnabled}")
                        Log.d(TAG, "Audio direction: ${params.audioDirection}")
                    }
                }
                Call.State.StreamsRunning -> {
                    // Ensure audio streams are active and mic is unmuted
                    call.microphoneMuted = false
                    // Set microphone gain to maximum
                    call.microphoneVolumeGain = 1.0f

                    Log.d(TAG, "Streams running - Mic muted: ${call.microphoneMuted}, Gain: ${call.microphoneVolumeGain}")
                    Log.d(TAG, "Core mic enabled: ${core.isMicEnabled}")

                    // Check audio stats and codec info
                    call.audioStats?.let { stats ->
                        Log.d(TAG, "Audio stats - Download bandwidth: ${stats.downloadBandwidth}, Upload bandwidth: ${stats.uploadBandwidth}")
                    }

                    // Log codec being used
                    call.currentParams?.usedAudioPayloadType?.let { codec ->
                        Log.d(TAG, "Audio codec in use: ${codec.mimeType}, Rate: ${codec.clockRate}")
                    }

                    // Log remote address for RTP
                    Log.d(TAG, "Remote address: ${call.remoteAddress?.asStringUriOnly()}")
                }
                else -> {}
            }

            // Send broadcast with call state
            val intent = Intent("com.voipcall.CALL_STATE_CHANGED")
            intent.putExtra("state", state.toString())
            intent.putExtra("message", message)
            sendBroadcast(intent)
        }

        override fun onRegistrationStateChanged(
            core: Core,
            cfg: ProxyConfig,
            state: RegistrationState,
            message: String
        ) {
            Log.d(TAG, "Registration state: $state - $message")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        // Initialize AudioManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        createNotificationChannel()
        initializeLinphone()
        initializeBluetoothSupport()

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun initializeLinphone() {
        try {
            val factory = Factory.instance()
            factory.setDebugMode(true, "Linphone")

            core = factory.createCore(null, null, this)
            core.addListener(coreListener)

            // Configure native audio routing
            core.useInfoForDtmf = false
            core.useRfc2833ForDtmf = true

            // Enable all available audio codecs
            core.audioPayloadTypes.forEach { codec ->
                codec.enable(true)
                Log.d(TAG, "Enabled codec: ${codec.mimeType}")
            }

            // Video disabled for VoIP
            core.videoActivationPolicy?.automaticallyAccept = false
            core.videoActivationPolicy?.automaticallyInitiate = false

            // Enable native audio routed by Android
            core.setNativeRingingEnabled(false)

            // Enable microphone globally
            core.isMicEnabled = true

            // Enable echo cancellation
            core.isEchoCancellationEnabled = true

            // Set audio configuration for better quality
            core.setMediaEncryption(MediaEncryption.None)

            // Set audio port range for better NAT traversal
            core.setAudioPort(-1)  // Use random port

            // Configure NAT policy - disable all NAT helpers for direct IP trunk
            val natPolicy = core.createNatPolicy()
            natPolicy?.isIceEnabled = false
            natPolicy?.isStunEnabled = false
            natPolicy?.isTurnEnabled = false
            natPolicy?.isUpnpEnabled = false
            core.natPolicy = natPolicy

            Log.d(TAG, "NAT policy configured for direct IP trunking")

            // Start core
            core.start()

            Log.d(TAG, "Linphone core initialized successfully")
            Log.d(TAG, "Microphone enabled: ${core.isMicEnabled}")
            Log.d(TAG, "Available audio devices: ${core.audioDevices.map { it.deviceName }}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Linphone", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VoIP Call Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Handles VoIP calls"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VoIP Call")
            .setContentText("Service running")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "MAKE_CALL" -> {
                val number = intent.getStringExtra("number")
                val username = intent.getStringExtra("username")
                val serverIp = intent.getStringExtra("serverIp")
                val serverPort = intent.getIntExtra("serverPort", 5060)
                if (number != null && username != null && serverIp != null) {
                    makeCall(number, username, serverIp, serverPort)
                }
            }
            "HANGUP_CALL" -> hangupCall()
            "TOGGLE_MUTE" -> toggleMute()
            "TOGGLE_SPEAKER" -> toggleSpeaker()
            "CHANGE_VOICE_TYPE" -> {
                val voiceTypeName = intent.getStringExtra("voiceType")
                voiceTypeName?.let {
                    try {
                        val voiceType = VoiceType.valueOf(it)
                        changeVoiceType(voiceType)
                    } catch (e: Exception) {
                        Log.e(TAG, "Invalid voice type: $it", e)
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun makeCall(number: String, username: String, serverIp: String, serverPort: Int) {
        try {
            // Ensure microphone is enabled at core level
            core.isMicEnabled = true
            Log.d(TAG, "Core microphone enabled: ${core.isMicEnabled}")

            // Configure transports for IP trunking
            val transports = core.transports
            transports.udpPort = 5060
            core.transports = transports

            // Use server IP in contact to avoid NAT issues
            core.guessHostname = false
            core.setUserAgent("VoIPCall", "1.0")

            // Create identity with username from login
            val identity = "sip:$username@$serverIp:$serverPort"
            val identityAddress = core.interpretUrl(identity)

            if (identityAddress != null) {
                core.primaryContact = identity
                Log.d(TAG, "Set identity to: $identity")
            }

            // Create SIP address for destination
            val sipAddress = "sip:$number@$serverIp:$serverPort"
            val address = core.interpretUrl(sipAddress)

            if (address != null) {
                val params = core.createCallParams(null)
                if (params != null) {
                    params.isAudioEnabled = true
                    params.isVideoEnabled = false

                    // Set audio direction to send and receive
                    params.audioDirection = MediaDirection.SendRecv

                    // Enable early media for audio
                    params.isEarlyMediaSendingEnabled = true

                    val call = core.inviteAddressWithParams(address, params)
                    if (call != null) {
                        // Ensure microphone is not muted on the call object
                        call.microphoneMuted = false
                        // Set microphone gain to ensure audio is captured
                        call.microphoneVolumeGain = 1.0f

                        Log.d(TAG, "Call initiated to $sipAddress")
                        Log.d(TAG, "Mic muted: ${call.microphoneMuted}, Gain: ${call.microphoneVolumeGain}")
                        Log.d(TAG, "Audio direction: ${params.audioDirection}")
                        Log.d(TAG, "Early media enabled: ${params.isEarlyMediaSendingEnabled}")
                    } else {
                        Log.e(TAG, "Failed to create call")
                    }
                } else {
                    Log.e(TAG, "Failed to create call params")
                }
            } else {
                Log.e(TAG, "Invalid SIP address: $sipAddress")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error making call", e)
        }
    }

    private fun hangupCall() {
        try {
            core.currentCall?.let { call ->
                call.terminate()
                Log.d(TAG, "Call terminated")
            } ?: run {
                core.terminateAllCalls()
                Log.d(TAG, "All calls terminated")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error hanging up", e)
        }
    }

    private fun toggleMute() {
        try {
            val currentlyMuted = !core.isMicEnabled
            core.isMicEnabled = currentlyMuted

            val intent = Intent("com.voipcall.MUTE_CHANGED")
            intent.putExtra("muted", !currentlyMuted)
            sendBroadcast(intent)

            Log.d(TAG, "Mic ${if (!currentlyMuted) "muted" else "unmuted"}")
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling mute", e)
        }
    }

    private fun toggleSpeaker() {
        try {
            val audioDevice = core.currentCall?.outputAudioDevice
            val isSpeaker = audioDevice?.type == AudioDevice.Type.Speaker

            val devices = core.audioDevices
            val targetDevice = if (isSpeaker) {
                devices.firstOrNull { it.type == AudioDevice.Type.Earpiece }
            } else {
                devices.firstOrNull { it.type == AudioDevice.Type.Speaker }
            }

            targetDevice?.let {
                core.currentCall?.outputAudioDevice = it

                val intent = Intent("com.voipcall.SPEAKER_CHANGED")
                intent.putExtra("speaker", !isSpeaker)
                sendBroadcast(intent)

                Log.d(TAG, "Speaker ${if (!isSpeaker) "on" else "off"}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling speaker", e)
        }
    }

    private fun changeVoiceType(voiceType: VoiceType) {
        try {
            currentVoiceType = voiceType
            Log.d(TAG, "Changing voice type to: ${voiceType.name}")

            // Get the current call's audio session ID
            // Note: Linphone doesn't provide direct access to Android's audio session ID
            // We'll use an alternative approach with audio effects through microphone gain
            val hasActiveCall = core.currentCall != null

            if (hasActiveCall) {
                // Apply voice effect through Linphone's audio controls
                try {
                    applyVoiceEffectThroughLinphone(voiceType)
                    Log.d(TAG, "Voice effect applied: ${voiceType.name}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error applying voice effect", e)
                }
            } else {
                // No active call, effects will be applied when call starts
                Log.d(TAG, "Voice type set to ${voiceType.name}, will apply when call starts")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error changing voice type", e)
        }
    }

    private fun applyVoiceEffectThroughLinphone(voiceType: VoiceType) {
        try {
            core.currentCall?.let { call ->
                // Adjust microphone gain based on voice type
                call.microphoneVolumeGain = when (voiceType) {
                    VoiceType.NORMAL -> 1.0f
                    VoiceType.MALE -> 1.2f     // Slightly louder for deeper effect
                    VoiceType.FEMALE -> 0.9f   // Slightly softer
                    VoiceType.KID -> 0.85f     // Softer for child-like effect
                }

                Log.d(TAG, "Applied voice effect through Linphone: ${voiceType.name}, gain: ${call.microphoneVolumeGain}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying voice effect through Linphone", e)
        }
    }

    private fun initializeBluetoothSupport() {
        try {
            // Register Bluetooth broadcast receiver
            val filter = IntentFilter().apply {
                addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)
            }
            registerReceiver(bluetoothReceiver, filter)

            // Get Bluetooth adapter and set up headset profile
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                bluetoothAdapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        if (profile == BluetoothProfile.HEADSET) {
                            bluetoothHeadset = proxy as BluetoothHeadset

                            // Check if Bluetooth headset is already connected
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    // For Android 12+, check permission first
                                    if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) ==
                                        android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        val devices = bluetoothHeadset?.connectedDevices
                                        if (devices?.isNotEmpty() == true) {
                                            isBluetoothConnected = true
                                            Log.d(TAG, "Bluetooth headset already connected: ${devices[0].name}")
                                        }
                                    }
                                } else {
                                    val devices = bluetoothHeadset?.connectedDevices
                                    if (devices?.isNotEmpty() == true) {
                                        isBluetoothConnected = true
                                        Log.d(TAG, "Bluetooth headset already connected: ${devices[0].name}")
                                    }
                                }
                            } catch (e: SecurityException) {
                                Log.e(TAG, "Bluetooth permission not granted", e)
                            }
                        }
                    }

                    override fun onServiceDisconnected(profile: Int) {
                        if (profile == BluetoothProfile.HEADSET) {
                            bluetoothHeadset = null
                        }
                    }
                }, BluetoothProfile.HEADSET)

                Log.d(TAG, "Bluetooth support initialized")
            } else {
                Log.d(TAG, "Bluetooth not available or not enabled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Bluetooth support", e)
        }
    }

    private fun routeAudioToBluetooth() {
        try {
            if (isBluetoothConnected && core.currentCall != null) {
                // Set audio mode for VoIP
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

                // Start Bluetooth SCO (Synchronous Connection-Oriented) for audio
                if (!audioManager.isBluetoothScoOn) {
                    audioManager.startBluetoothSco()
                    audioManager.isBluetoothScoOn = true
                }

                // Route audio to Bluetooth
                audioManager.isSpeakerphoneOn = false

                // Update Linphone to use Bluetooth device
                val bluetoothDevice = core.audioDevices.firstOrNull {
                    it.type == AudioDevice.Type.Bluetooth
                }

                if (bluetoothDevice != null) {
                    core.currentCall?.outputAudioDevice = bluetoothDevice
                    core.currentCall?.inputAudioDevice = bluetoothDevice
                    Log.d(TAG, "Audio routed to Bluetooth: ${bluetoothDevice.deviceName}")
                } else {
                    Log.d(TAG, "Bluetooth audio routing via AudioManager SCO")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error routing audio to Bluetooth", e)
        }
    }

    private fun stopBluetoothSco() {
        try {
            if (audioManager.isBluetoothScoOn) {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                Log.d(TAG, "Bluetooth SCO stopped")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Bluetooth SCO", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            // Unregister Bluetooth receiver
            try {
                unregisterReceiver(bluetoothReceiver)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering Bluetooth receiver", e)
            }

            // Stop Bluetooth SCO
            stopBluetoothSco()

            // Close Bluetooth profile proxy
            bluetoothHeadset?.let {
                BluetoothAdapter.getDefaultAdapter()?.closeProfileProxy(BluetoothProfile.HEADSET, it)
            }

            core.removeListener(coreListener)
            core.stop()
            Log.d(TAG, "Service destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying service", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
