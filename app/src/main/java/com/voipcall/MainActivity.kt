package com.voipcall

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.voipcall.model.CallState
import com.voipcall.ui.screens.CallScreen
import com.voipcall.ui.screens.DialScreen
import com.voipcall.ui.screens.LoginScreen
import com.voipcall.ui.screens.SettingsScreen
import com.voipcall.ui.theme.VoIPCallTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CallViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(
                this,
                "Permissions required for VoIP calls",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()

        setContent {
            VoIPCallTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VoIPCallApp(viewModel)
                }
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.READ_PHONE_STATE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.MANAGE_OWN_CALLS)
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}

@Composable
fun VoIPCallApp(viewModel: CallViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val callState by viewModel.callState.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val currentVoiceType by viewModel.currentVoiceType.collectAsState()
    val trunkConfig by viewModel.trunkConfig.collectAsState()
    val username by viewModel.username.collectAsState()

    var showSettings by remember { mutableStateOf(false) }

    when {
        !isLoggedIn -> {
            LoginScreen(
                onLoginClick = { user, ip, port ->
                    viewModel.login(user, ip, port)
                }
            )
        }
        showSettings -> {
            SettingsScreen(
                trunkConfig = trunkConfig,
                username = username,
                onTrunkConfigChange = { config ->
                    viewModel.updateTrunkConfig(config)
                },
                onLogoutClick = { viewModel.logout() },
                onBackClick = { showSettings = false }
            )
        }
        callState == CallState.IDLE || callState == CallState.ENDED -> {
            LaunchedEffect(callState) {
                if (callState == CallState.ENDED) {
                    kotlinx.coroutines.delay(1500)
                    viewModel.resetCallState()
                }
            }

            DialScreen(
                phoneNumber = phoneNumber,
                onPhoneNumberChange = { viewModel.updatePhoneNumber(it) },
                onCallClick = { viewModel.makeCall() },
                onSettingsClick = { showSettings = true }
            )
        }
        else -> {
            CallScreen(
                phoneNumber = phoneNumber,
                callState = callState,
                isMuted = isMuted,
                isSpeakerOn = isSpeakerOn,
                currentVoiceType = currentVoiceType,
                onHangupClick = { viewModel.hangupCall() },
                onMuteClick = { viewModel.toggleMute() },
                onSpeakerClick = { viewModel.toggleSpeaker() },
                onVoiceTypeChange = { viewModel.changeVoiceType(it) }
            )
        }
    }
}
