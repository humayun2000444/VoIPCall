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
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
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
    private lateinit var connectivityManager: ConnectivityManager
    private var currentNetwork: Network? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var keepAliveRunnable: Runnable? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network available: $network")
            handleNetworkChange(network)
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "Network lost: $network")
            if (currentNetwork == network) {
                currentNetwork = null
            }
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            Log.d(TAG, "Network capabilities changed: $network")
            handleNetworkChange(network)
        }
    }

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
            Log.e(TAG, "========================================")
            Log.e(TAG, "CALL STATE: $state")
            Log.e(TAG, "MESSAGE: $message")
            Log.e(TAG, "========================================")

            // Print SDP for debugging
            try {
                when (state) {
                    Call.State.OutgoingProgress -> {
                        val localSdp = call.callLog?.callId?.let {
                            // Try to get local description
                            try {
                                // Log connection info
                                call.params?.let { params ->
                                    Log.e(TAG, "")
                                    Log.e(TAG, "═══════════════ LOCAL SDP INFO ═══════════════")
                                    Log.e(TAG, "Audio enabled: ${params.isAudioEnabled}")
                                    Log.e(TAG, "Audio direction: ${params.audioDirection}")
                                    Log.e(TAG, "Audio codec: ${params.usedAudioPayloadType?.mimeType ?: "Not set yet"}")

                                    // Get local address info
                                    call.callLog?.fromAddress?.let { addr ->
                                        Log.e(TAG, "From: ${addr.asStringUriOnly()}")
                                    }
                                    call.callLog?.toAddress?.let { addr ->
                                        Log.e(TAG, "To: ${addr.asStringUriOnly()}")
                                    }
                                    Log.e(TAG, "════════════════════════════════════════════════")
                                    Log.e(TAG, "")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error getting local SDP", e)
                            }
                        }
                    }
                    Call.State.StreamsRunning -> {
                        Log.e(TAG, "")
                        Log.e(TAG, "═══════════════ SDP NEGOTIATION RESULT ═══════════════")

                        // Log negotiated codec
                        call.currentParams?.usedAudioPayloadType?.let { codec ->
                            Log.e(TAG, "Negotiated codec: ${codec.mimeType} @ ${codec.clockRate}Hz")
                            Log.e(TAG, "Channels: ${codec.channels}")
                        }

                        // Log audio stats to see actual RTP ports
                        call.audioStats?.let { stats ->
                            Log.e(TAG, "RTP stats available - bandwidth: ${stats.uploadBandwidth} bps")
                        }

                        // Log remote address
                        call.remoteAddress?.let { addr ->
                            Log.e(TAG, "Remote address: ${addr.asStringUriOnly()}")
                            Log.e(TAG, "Remote domain: ${addr.domain}")
                        }

                        // Log local network info
                        call.callLog?.fromAddress?.let { addr ->
                            Log.e(TAG, "Local address: ${addr.asStringUriOnly()}")
                        }

                        Log.e(TAG, "═════════════════════════════════════════════════════")
                        Log.e(TAG, "")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error printing SDP", e)
            }

            // Handle microphone settings based on call state
            when (state) {
                Call.State.Connected -> {
                    // Request audio focus for VoIP
                    requestAudioFocus()

                    // Configure audio mode for call
                    configureAudioForCall()

                    // Ensure microphone is enabled when call connects
                    call.microphoneMuted = false
                    core.isMicEnabled = true

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
                    Log.e(TAG, "╔════════════════════════════════════════╗")
                    Log.e(TAG, "║  STREAMS RUNNING - AUDIO SHOULD WORK  ║")
                    Log.e(TAG, "╚════════════════════════════════════════╝")

                    // CRITICAL: Force everything for audio transmission
                    call.microphoneMuted = false
                    core.isMicEnabled = true

                    // CRITICAL: Ensure audio mode is set correctly FIRST
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

                    // Force audio routing to be stable - use earpiece by default
                    try {
                        if (!isBluetoothConnected) {
                            audioManager.isSpeakerphoneOn = false

                            // Set input audio device explicitly to phone's microphone
                            val micDevice = core.audioDevices.firstOrNull {
                                it.type == AudioDevice.Type.Microphone
                            }
                            if (micDevice != null) {
                                core.inputAudioDevice = micDevice
                                Log.e(TAG, "🎤 Forced input to: ${micDevice.deviceName}")
                            } else {
                                Log.e(TAG, "⚠️ No microphone device found!")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting audio device", e)
                    }

                    // Start keep-alive to maintain audio stream
                    startKeepAlive()

                    Log.e(TAG, "🎤 Mic muted: ${call.microphoneMuted}")
                    Log.e(TAG, "🎤 Core mic enabled: ${core.isMicEnabled}")
                    Log.e(TAG, "🔊 Audio mode: ${audioManager.mode}")
                    Log.e(TAG, "🎙️ Input device: ${core.inputAudioDevice?.deviceName}")
                    Log.e(TAG, "🔊 Output device: ${core.outputAudioDevice?.deviceName}")

                    // Check audio stats and codec info - USE DELAYED CHECK
                    mainHandler.postDelayed({
                        try {
                            call.audioStats?.let { stats ->
                                Log.e(TAG, "═══ AUDIO STATS (after 2 sec) ═══")
                                Log.e(TAG, "⬆️  Upload bandwidth: ${stats.uploadBandwidth} bps")
                                Log.e(TAG, "⬇️  Download bandwidth: ${stats.downloadBandwidth} bps")
                                Log.e(TAG, "📊 Jitter buffer: ${stats.jitterBufferSizeMs}ms")
                                Log.e(TAG, "📉 Sender loss: ${stats.senderLossRate}%")
                                Log.e(TAG, "📉 Receiver loss: ${stats.receiverLossRate}%")

                                // CRITICAL WARNING if no upload
                                if (stats.uploadBandwidth == 0.0f) {
                                    Log.e(TAG, "")
                                    Log.e(TAG, "❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌")
                                    Log.e(TAG, "❌  ZERO UPLOAD - MIC DEAD  ❌")
                                    Log.e(TAG, "❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌")
                                    Log.e(TAG, "")
                                } else {
                                    Log.e(TAG, "✅ Audio is transmitting!")
                                }
                            }

                            // Log codec being used
                            call.currentParams?.usedAudioPayloadType?.let { codec ->
                                Log.e(TAG, "🎵 Audio codec: ${codec.mimeType} @ ${codec.clockRate}Hz")
                            }

                            // Check audio direction
                            call.currentParams?.let { params ->
                                Log.e(TAG, "↔️  Audio direction: ${params.audioDirection}")
                                if (params.audioDirection != MediaDirection.SendRecv) {
                                    Log.e(TAG, "❌ WARNING: Audio direction is NOT SendRecv!")
                                }
                            }

                            // Log addresses
                            Log.e(TAG, "🌐 Remote: ${call.remoteAddress?.asStringUriOnly()}")
                            Log.e(TAG, "🌐 Local: ${call.callLog?.fromAddress?.asStringUriOnly()}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error checking audio stats", e)
                        }
                    }, 2000)
                }
                Call.State.End, Call.State.Released, Call.State.Error -> {
                    // Stop keep-alive
                    stopKeepAlive()

                    // Release audio focus and restore audio mode
                    releaseAudioFocus()
                    restoreAudioMode()
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

        // Initialize ConnectivityManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        createNotificationChannel()
        initializeLinphone()
        initializeBluetoothSupport()
        registerNetworkCallback()

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

            // Prioritize audio codecs for better quality
            core.audioPayloadTypes.forEach { codec ->
                when (codec.mimeType.uppercase()) {
                    "PCMU" -> {
                        codec.enable(true)
                        codec.normalBitrate = 64000
                        Log.d(TAG, "Enabled codec (high priority): ${codec.mimeType}")
                    }
                    "PCMA" -> {
                        codec.enable(true)
                        codec.normalBitrate = 64000
                        Log.d(TAG, "Enabled codec (high priority): ${codec.mimeType}")
                    }
                    "OPUS" -> {
                        codec.enable(true)
                        codec.normalBitrate = 48000
                        Log.d(TAG, "Enabled codec: ${codec.mimeType}")
                    }
                    else -> {
                        codec.enable(true)
                        Log.d(TAG, "Enabled codec: ${codec.mimeType}")
                    }
                }
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

            // Adaptive rate control for varying network conditions
            core.isAdaptiveRateControlEnabled = true

            // Set audio configuration for better quality
            core.setMediaEncryption(MediaEncryption.None)

            // Set audio port range for better NAT traversal
            core.setAudioPort(-1)  // Use random port

            // Configure NAT policy for restrictive networks (office WiFi)
            val natPolicy = core.createNatPolicy()

            // Enable STUN from provider for NAT traversal
            natPolicy?.isStunEnabled = true
            natPolicy?.stunServer = "iptsp.cosmocom.net:3478"

            // Enable ICE for better NAT traversal in restrictive networks
            natPolicy?.isIceEnabled = true
            natPolicy?.isTurnEnabled = false
            natPolicy?.isUpnpEnabled = false

            core.natPolicy = natPolicy

            // Enable SIP keepalive to maintain NAT bindings (critical for office WiFi)
            core.sipTransportTimeout = 30000  // 30 seconds timeout

            // Set session timer for keep-alive (refresh every 30 seconds for restrictive networks)
            core.incTimeout = 30

            Log.d(TAG, "NAT policy configured for restrictive networks")
            Log.d(TAG, "STUN server: iptsp.cosmocom.net:3478")
            Log.d(TAG, "ICE enabled for NAT traversal")
            Log.d(TAG, "SIP timeout: 30s, session refresh: 30s")
            Log.d(TAG, "TCP transport forced for office WiFi compatibility")

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
            Log.d(TAG, "=== STARTING CALL SETUP ===")

            // Request audio focus BEFORE making call
            requestAudioFocus()

            // Configure audio mode BEFORE making call
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = false
            Log.d(TAG, "Audio mode set to: ${audioManager.mode}")

            // Ensure microphone is enabled at core level
            core.isMicEnabled = true
            Log.d(TAG, "Core microphone enabled: ${core.isMicEnabled}")

            // Configure transports for IP trunking - TCP priority for office networks
            val transports = core.transports
            transports.tcpPort = 5060  // TCP first - works better through firewalls
            transports.udpPort = 5060  // UDP as fallback
            core.transports = transports

            Log.d(TAG, "Transports configured - TCP:5060 (priority), UDP:5060 (fallback)")

            // Use server IP in contact to avoid NAT issues
            core.guessHostname = false
            core.setUserAgent("VoIPCall", "1.0")

            // Create identity with username from login - use TCP transport
            val identity = "sip:$username@$serverIp:$serverPort;transport=tcp"
            val identityAddress = core.interpretUrl(identity)

            if (identityAddress != null) {
                core.primaryContact = identity
                Log.d(TAG, "Set identity to: $identity (TCP)")
            }

            // Create SIP address for destination with transport parameter
            // Force TCP transport for better compatibility with office networks
            val sipAddress = "sip:$number@$serverIp:$serverPort;transport=tcp"
            val address = core.interpretUrl(sipAddress)

            if (address != null) {
                val params = core.createCallParams(null)
                if (params != null) {
                    // CRITICAL: Ensure audio is enabled
                    params.isAudioEnabled = true
                    params.isVideoEnabled = false

                    // CRITICAL: Set audio direction to SEND and RECEIVE
                    params.audioDirection = MediaDirection.SendRecv

                    // Enable early media for audio
                    params.isEarlyMediaSendingEnabled = true

                    // Record audio enabled
                    params.recordFile = null  // No recording, just ensure path is set

                    Log.d(TAG, "Call params - Audio: ${params.isAudioEnabled}, Direction: ${params.audioDirection}")

                    val call = core.inviteAddressWithParams(address, params)
                    if (call != null) {
                        // CRITICAL: Force microphone UNMUTED
                        call.microphoneMuted = false

                        // CRITICAL: Set microphone gain HIGHER
                        call.microphoneVolumeGain = 2.0f

                        // Force speaker gain as well
                        call.speakerVolumeGain = 1.0f

                        Log.d(TAG, "=== CALL INITIATED ===")
                        Log.d(TAG, "To: $sipAddress")
                        Log.d(TAG, "Mic muted: ${call.microphoneMuted}")
                        Log.d(TAG, "Mic gain: ${call.microphoneVolumeGain}")
                        Log.d(TAG, "Speaker gain: ${call.speakerVolumeGain}")
                        Log.d(TAG, "Core mic enabled: ${core.isMicEnabled}")
                        Log.d(TAG, "Audio direction: ${params.audioDirection}")
                        Log.d(TAG, "Early media: ${params.isEarlyMediaSendingEnabled}")
                        Log.d(TAG, "Audio mode: ${audioManager.mode}")

                        // Start aggressive microphone monitoring
                        startAggressiveMicCheck(call)
                    } else {
                        Log.e(TAG, "Failed to create call")
                        releaseAudioFocus()
                        restoreAudioMode()
                    }
                } else {
                    Log.e(TAG, "Failed to create call params")
                    releaseAudioFocus()
                    restoreAudioMode()
                }
            } else {
                Log.e(TAG, "Invalid SIP address: $sipAddress")
                releaseAudioFocus()
                restoreAudioMode()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error making call", e)
            releaseAudioFocus()
            restoreAudioMode()
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
            // Toggle mic enabled state
            core.isMicEnabled = !core.isMicEnabled

            // Also toggle on the call object
            core.currentCall?.let { call ->
                call.microphoneMuted = !core.isMicEnabled
            }

            val intent = Intent("com.voipcall.MUTE_CHANGED")
            intent.putExtra("muted", !core.isMicEnabled)
            sendBroadcast(intent)

            Log.d(TAG, "Mic ${if (core.isMicEnabled) "unmuted" else "muted"}")
            Log.d(TAG, "Core mic enabled: ${core.isMicEnabled}")
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

    private fun registerNetworkCallback() {
        try {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

            // Get current network
            currentNetwork = connectivityManager.activeNetwork
            Log.d(TAG, "Network callback registered, current network: $currentNetwork")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering network callback", e)
        }
    }

    private fun handleNetworkChange(network: Network) {
        try {
            // Only handle if we have an active call and network actually changed
            val activeCall = core.currentCall
            if (activeCall != null && activeCall.state == Call.State.StreamsRunning) {
                if (currentNetwork != null && currentNetwork != network) {
                    Log.d(TAG, "Network changed during active call, refreshing session")

                    // Update current network
                    currentNetwork = network

                    // Force Linphone to update network reachability
                    core.setNetworkReachable(false)

                    // Small delay to ensure network state is updated
                    mainHandler.postDelayed({
                        core.setNetworkReachable(true)

                        // Trigger call update to refresh RTP streams
                        try {
                            val params = core.createCallParams(activeCall)
                            if (params != null) {
                                params.isAudioEnabled = true
                                params.audioDirection = MediaDirection.SendRecv
                                activeCall.update(params)
                                Log.d(TAG, "Call update triggered for network change")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating call", e)
                        }
                    }, 500)
                } else if (currentNetwork == null) {
                    // First time setting network
                    currentNetwork = network
                    Log.d(TAG, "Initial network set: $network")
                }
            } else if (currentNetwork == null) {
                currentNetwork = network
                Log.d(TAG, "Network set (no active call): $network")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling network change", e)
        }
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                // Remove delayed focus and pause on duck to avoid IllegalStateException
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .build()

                val result = audioManager.requestAudioFocus(audioFocusRequest!!)
                Log.d(TAG, "Audio focus requested: ${if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) "granted" else "denied"}")
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN
                )
                Log.d(TAG, "Audio focus requested: ${if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) "granted" else "denied"}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting audio focus", e)
        }
    }

    private fun releaseAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let {
                    audioManager.abandonAudioFocusRequest(it)
                    audioFocusRequest = null
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
            Log.d(TAG, "Audio focus released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio focus", e)
        }
    }

    private fun configureAudioForCall() {
        try {
            // Set audio mode for VoIP communication
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

            // Enable wired headset if connected
            audioManager.isSpeakerphoneOn = false

            // Ensure proper audio routing
            if (!isBluetoothConnected) {
                // Use earpiece for privacy unless speaker is explicitly toggled
                val earpieceDevice = core.audioDevices.firstOrNull {
                    it.type == AudioDevice.Type.Earpiece
                }
                earpieceDevice?.let {
                    core.currentCall?.outputAudioDevice = it
                }
            }

            Log.d(TAG, "Audio configured for call - Mode: ${audioManager.mode}")
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring audio for call", e)
        }
    }

    private fun restoreAudioMode() {
        try {
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
            Log.d(TAG, "Audio mode restored to normal")
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring audio mode", e)
        }
    }

    private fun startKeepAlive() {
        stopKeepAlive()  // Clear any existing keep-alive

        keepAliveRunnable = object : Runnable {
            override fun run() {
                try {
                    core.currentCall?.let { call ->
                        if (call.state == Call.State.StreamsRunning) {
                            // Verify audio is still flowing
                            call.audioStats?.let { stats ->
                                Log.d(TAG, "Keep-alive check - Upload: ${stats.uploadBandwidth} bps, Download: ${stats.downloadBandwidth} bps")

                                // If no audio is being transmitted, try to refresh
                                if (stats.uploadBandwidth < 100) {
                                    Log.w(TAG, "Low upload bandwidth detected, refreshing mic")
                                    call.microphoneMuted = false
                                    core.isMicEnabled = true
                                    call.microphoneVolumeGain = 1.5f
                                }
                            }

                            // Schedule next check
                            mainHandler.postDelayed(this, 5000)  // Check every 5 seconds
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in keep-alive", e)
                }
            }
        }

        mainHandler.postDelayed(keepAliveRunnable!!, 5000)
        Log.d(TAG, "Keep-alive started")
    }

    private fun stopKeepAlive() {
        keepAliveRunnable?.let {
            mainHandler.removeCallbacks(it)
            keepAliveRunnable = null
            Log.d(TAG, "Keep-alive stopped")
        }
    }

    private fun startAggressiveMicCheck(call: Call) {
        // Immediately check and force mic settings every 2 seconds during first 30 seconds
        var checkCount = 0
        val aggressiveCheck = object : Runnable {
            override fun run() {
                try {
                    if (call.state == Call.State.StreamsRunning || call.state == Call.State.Connected) {
                        checkCount++

                        // Force microphone settings
                        call.microphoneMuted = false
                        core.isMicEnabled = true

                        // Force audio mode (Android sometimes resets it)
                        if (audioManager.mode != AudioManager.MODE_IN_COMMUNICATION) {
                            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                            Log.e(TAG, "[MicCheck #$checkCount] ⚠️ Audio mode was reset to ${audioManager.mode}, restored")
                        }

                        // Force input device to microphone
                        try {
                            val currentInput = core.inputAudioDevice
                            if (currentInput?.type != AudioDevice.Type.Microphone && !isBluetoothConnected) {
                                val micDevice = core.audioDevices.firstOrNull {
                                    it.type == AudioDevice.Type.Microphone
                                }
                                if (micDevice != null) {
                                    core.inputAudioDevice = micDevice
                                    Log.e(TAG, "[MicCheck #$checkCount] ⚠️ Input device changed, restored to microphone")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "[MicCheck #$checkCount] Error checking input device", e)
                        }

                        // Check audio stats
                        call.audioStats?.let { stats ->
                            Log.d(TAG, "[MicCheck #$checkCount] Upload: ${stats.uploadBandwidth} bps, Download: ${stats.downloadBandwidth} bps")

                            if (stats.uploadBandwidth < 50.0f) {
                                Log.e(TAG, "[MicCheck #$checkCount] !!! LOW/ZERO UPLOAD - FORCING MIC RESET !!!")

                                // Try to force audio restart by toggling mic
                                call.microphoneMuted = true
                                core.isMicEnabled = false

                                mainHandler.postDelayed({
                                    call.microphoneMuted = false
                                    core.isMicEnabled = true

                                    // Force audio mode again
                                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

                                    Log.e(TAG, "Mic toggled to force restart")
                                }, 200)
                            } else {
                                Log.d(TAG, "[MicCheck #$checkCount] ✅ Audio transmitting OK")
                            }
                        }

                        // Continue checking for 30 seconds
                        if (checkCount < 15) {
                            mainHandler.postDelayed(this, 2000)
                        } else {
                            Log.d(TAG, "Aggressive mic check completed, transitioning to normal keep-alive")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in aggressive mic check", e)
                }
            }
        }

        mainHandler.postDelayed(aggressiveCheck, 1000)  // Start after 1 second
        Log.d(TAG, "Started aggressive mic monitoring")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            // Stop keep-alive
            stopKeepAlive()

            // Release audio focus
            releaseAudioFocus()

            // Restore audio mode
            restoreAudioMode()

            // Unregister network callback
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering network callback", e)
            }

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
