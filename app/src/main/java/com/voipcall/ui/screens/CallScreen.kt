package com.voipcall.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voipcall.model.CallState
import com.voipcall.model.VoiceType
import com.voipcall.ui.theme.Green
import com.voipcall.ui.theme.Red

@Composable
fun CallScreen(
    phoneNumber: String,
    callState: CallState,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    currentVoiceType: VoiceType,
    onHangupClick: () -> Unit,
    onMuteClick: () -> Unit,
    onSpeakerClick: () -> Unit,
    onVoiceTypeChange: (VoiceType) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Call status text
            Text(
                text = when (callState) {
                    CallState.OUTGOING -> "Calling..."
                    CallState.INCOMING -> "Incoming Call"
                    CallState.CONNECTED -> "Connected"
                    CallState.ENDED -> "Call Ended"
                    CallState.ERROR -> "Error"
                    else -> ""
                },
                fontSize = 20.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Phone number
            Text(
                text = phoneNumber,
                fontSize = 36.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.weight(1f))

            // Call duration (only show when connected)
            if (callState == CallState.CONNECTED) {
                var callDuration by remember { mutableStateOf(0) }

                LaunchedEffect(Unit) {
                    while (true) {
                        kotlinx.coroutines.delay(1000)
                        callDuration++
                    }
                }

                Text(
                    text = formatDuration(callDuration),
                    fontSize = 18.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Control buttons (only show when connected)
            if (callState == CallState.CONNECTED) {
                // Audio controls row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CallControlButton(
                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (isMuted) "Unmute" else "Mute",
                        isActive = isMuted,
                        onClick = onMuteClick
                    )

                    CallControlButton(
                        icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                        label = if (isSpeakerOn) "Speaker On" else "Speaker",
                        isActive = isSpeakerOn,
                        onClick = onSpeakerClick
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Voice morphing section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Voice Effects",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        VoiceTypeButton(
                            label = "Normal",
                            isSelected = currentVoiceType == VoiceType.NORMAL,
                            onClick = { onVoiceTypeChange(VoiceType.NORMAL) }
                        )
                        VoiceTypeButton(
                            label = "Male",
                            isSelected = currentVoiceType == VoiceType.MALE,
                            onClick = { onVoiceTypeChange(VoiceType.MALE) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        VoiceTypeButton(
                            label = "Female",
                            isSelected = currentVoiceType == VoiceType.FEMALE,
                            onClick = { onVoiceTypeChange(VoiceType.FEMALE) }
                        )
                        VoiceTypeButton(
                            label = "Kid",
                            isSelected = currentVoiceType == VoiceType.KID,
                            onClick = { onVoiceTypeChange(VoiceType.KID) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            } else {
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Hang up button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Red)
                        .clickable { onHangupClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CallEnd,
                        contentDescription = "Hang up",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "End Call",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
fun CallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun VoiceTypeButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}
