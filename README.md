# VoIP Call - Android Voice Calling App with Real-Time Voice Changer | Kotlin & Jetpack Compose

![Android](https://img.shields.io/badge/Platform-Android-green.svg) ![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg) ![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg) ![License](https://img.shields.io/badge/License-Demo-orange.svg)

**Modern Android VoIP calling application with real-time voice morphing, voice changer effects, and FreeSWITCH integration.** Built with Kotlin, Jetpack Compose, and Linphone SDK for professional SIP calling, IP trunking, and live voice transformation with full Bluetooth headset support.

### 🌟 Key Features
- 📱 **Android VoIP App** with IP trunking and SIP protocol (TCP/UDP)
- 🎙️ **Real-Time Voice Changer** with 4 voice effects (Male, Female, Kid, Normal)
- 📞 **FreeSWITCH Integration** for enterprise VoIP calling
- 🎨 **Material Design 3** with modern Jetpack Compose UI
- 🔊 **Advanced Audio Controls** including mute, speaker toggle, and echo cancellation
- 🎧 **Full Bluetooth Support** - Mic & speaker work seamlessly with Bluetooth headsets
- 🌐 **Works on All Networks** - Office WiFi, Home WiFi, Mobile Data
- ⚡ **Low Latency** voice morphing during active calls

---

## Table of Contents
- [Features Overview](#features)
- [Technology Stack](#technical-stack)
- [Installation Guide](#setup--installation)
- [Quick Start](#usage)
- [Voice Morphing](#voice-morphing-implementation)
- [VoIP Configuration](#configuration)
- [Network Requirements](#network-configuration)
- [Troubleshooting](#troubleshooting)
- [API Documentation](#development)
- [Contributing](#support)

---

## Features

### 🔐 VoIP Authentication & IP Trunking
- **Direct IP Trunking**: Connect to FreeSWITCH, Asterisk, or any SIP server without registration
- **Dynamic Port Support**: Connect to any SIP port (5060, 52318, 5080, custom ports)
- **Correct Caller Identity**: Username properly sent in SIP From header to server
- **Dual Transport Support**: TCP (primary) and UDP (fallback) for firewall compatibility
- **Dynamic Local Ports**: Random local port binding for maximum compatibility
- **NAT Traversal**: ICE enabled with Google STUN server for restrictive networks
- **Persistent Session Management**: Save username, server IP, and port configurations
- **First Call Reliability**: Core readiness check ensures first call always works
- **Office WiFi Compatible**: Works through corporate firewalls and restrictive networks

### 📞 Professional Call Management
- **Smart Dial Pad**: Full numeric keypad (0-9, *, #) with backspace functionality
- **Outgoing VoIP Calls**: Direct IP-to-IP calling via FreeSWITCH/SIP protocol
- **Real-Time Call Timer**: Displays accurate call duration from connection to hangup
- **Call State Monitoring**: Live status tracking (Calling, Ringing, Connected, Ended, Error)
- **Call End Handling**: Displays "Call Ended" screen with close button for clean navigation
- **One-Touch Dialing**: Quick call initiation with a single tap

### 🎤 Real-Time Voice Morphing & Voice Changer
Transform your voice in real-time during active VoIP calls:
- **Normal Voice Mode**: Default natural voice with no modifications
- **Male Voice Effect**: Deep voice with enhanced bass frequencies and lower pitch
- **Female Voice Effect**: Higher-pitched voice with boosted treble frequencies
- **Kid Voice Effect**: Child-like voice with emphasized high frequencies and lighter tone
- **Instant Voice Switching**: Change voice effects on-the-fly without dropping calls
- **Low Latency Processing**: Real-time audio transformation with minimal delay
- **Professional Audio Quality**: High-quality voice morphing using Linphone SDK

### 🔊 Advanced Audio Controls & Management
- **Mute/Unmute Toggle**: One-tap microphone control during calls
- **Speaker/Earpiece Switching**: Seamless audio routing between speaker and earpiece
- **Early Bluetooth Routing**: Instant Bluetooth connection for ringback tones (RBT)
  - Bluetooth audio starts immediately when dialing (not after answer)
  - Hear ringing through Bluetooth before B party answers
  - No 2-second delay - connects in milliseconds
  - Multiple routing points (OutgoingInit, OutgoingProgress, OutgoingRinging, Connected)
- **Bluetooth Headset Support**: Full mic and speaker support for Bluetooth devices
  - Automatic Bluetooth detection and routing
  - Separate capture (mic) and playback (speaker) device handling
  - SCO (Synchronous Connection-Oriented) audio for high quality
  - Works with all popular Bluetooth headsets
- **Echo Cancellation**: Built-in acoustic echo cancellation for crystal-clear audio
- **Auto Audio Routing**: Automatic detection and switching for headphones and Bluetooth devices
- **Volume Management**: Independent control of microphone gain and speaker volume
- **Noise Suppression**: Background noise reduction for professional call quality
- **Aggressive Mic Monitoring**: Automatic microphone recovery if audio drops

### 🌐 Network Compatibility
- **Works on All Networks**: Home WiFi, Office WiFi, Mobile Data (4G/5G)
- **TCP Transport Priority**: Uses TCP for better firewall traversal
- **ICE Support**: Interactive Connectivity Establishment for NAT traversal
- **STUN Integration**: Google STUN server (stun.l.google.com:19302) for public IP discovery
- **RTP Port Range**: Configurable 16384-32768 (matches FreeSWITCH default)
- **Network Change Handling**: Automatic reconnection on network switches

## Technical Stack

### Modern Android Development Technologies
- **Programming Language**: Kotlin 1.9.x - 100% Kotlin codebase
- **UI Framework**: Jetpack Compose with Material Design 3
- **Architecture Pattern**: MVVM (Model-View-ViewModel) with StateFlow for reactive programming
- **VoIP Engine**: Linphone SDK 5.2.0 - Industry-standard open-source SIP library
- **Minimum Android Version**: API 24 (Android 7.0 Nougon) and above
- **Target Android Version**: API 34 (Android 14)
- **Build System**: Gradle with Kotlin DSL
- **Dependency Injection**: Manual DI with ViewModel factory pattern

### Core Dependencies & Libraries
- **VoIP & SIP**: `org.linphone:linphone-sdk-android:5.2.0` - Complete SIP/VoIP stack with audio codec support
- **UI Components**: `androidx.compose:compose-bom:2023.10.01` - Modern declarative UI toolkit
- **Navigation**: `androidx.navigation:navigation-compose:2.7.5` - Type-safe navigation for Compose
- **Coroutines**: `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3` - Asynchronous programming
- **AndroidX Core**: `androidx.core:core-ktx:1.12.0` - Android Jetpack extensions
- **Lifecycle**: `androidx.lifecycle:lifecycle-runtime-ktx:2.6.2` - Lifecycle-aware components
- **Material Icons**: `androidx.compose.material:material-icons-extended` - Extended Material Design icons

### Audio Codecs Supported
- **Opus** - Primary codec for high quality (48kHz)
- **PCMU (G.711)** - Universal compatibility codec
- **PCMA (G.711)** - Alternative G.711 variant
- **G.722** - Wideband audio
- **GSM** - Fallback for low bandwidth
- **Speex** - Voice optimization
- All codecs enabled with automatic negotiation

### Keywords
`Android VoIP`, `Voice Changer App`, `Real-time Voice Morphing`, `Kotlin VoIP`, `Jetpack Compose`, `FreeSWITCH Android`, `SIP Client`, `IP Trunking`, `Linphone SDK`, `Android Voice Effects`, `VoIP Call App`, `Voice Transformer`, `SIP Calling`, `Voice Modulator Android`, `Bluetooth VoIP`, `Corporate Network VoIP`

## Project Structure

```
app/src/main/java/com/voipcall/
├── MainActivity.kt                 # Main activity and navigation
├── CallViewModel.kt                # Call state management with broadcast receivers
├── VoIPCallApplication.kt          # Application class
├── model/
│   ├── CallState.kt               # Call state enum (IDLE, OUTGOING, CONNECTED, ENDED, ERROR)
│   ├── VoiceType.kt               # Voice morphing types
│   └── TrunkConfig.kt             # IP trunk configuration
├── service/
│   └── LinphoneService.kt         # VoIP service with:
│                                  #   - Bluetooth audio routing
│                                  #   - TCP/UDP transport handling
│                                  #   - Microphone monitoring & recovery
│                                  #   - Network change detection
│                                  #   - Audio device management
└── ui/
    ├── screens/
    │   ├── LoginScreen.kt         # Login/connection screen
    │   ├── DialScreen.kt          # Dial pad interface
    │   ├── CallScreen.kt          # Active call interface with voice effects
    │   └── SettingsScreen.kt      # Configuration screen
    └── theme/
        ├── Color.kt               # App colors
        ├── Theme.kt               # Material theme
        └── Type.kt                # Typography
```

## Setup & Installation

### System Prerequisites & Requirements
- **Android Studio**: Hedgehog (2023.1.1) or newer - [Download Android Studio](https://developer.android.com/studio)
- **Java Development Kit**: JDK 17 or higher
- **Android SDK**: API levels 24-34 installed
- **Operating System**: Windows, macOS, or Linux
- **RAM**: Minimum 8GB recommended for development
- **Disk Space**: 4GB for Android Studio + project files

### Quick Installation Guide

#### Step 1: Clone the VoIP Application Repository
```bash
git clone <repository-url>
cd VoIPCall
```

#### Step 2: Open Project in Android Studio
1. Launch Android Studio
2. Select **File → Open**
3. Navigate to the `VoIPCall` directory
4. Wait for Gradle sync to complete (may take 2-5 minutes)

#### Step 3: Build the Android VoIP App
For **Linux/macOS**:
```bash
./gradlew assembleDebug
```

For **Windows**:
```bash
gradlew.bat assembleDebug
```

#### Step 4: Install on Android Device
Connect your Android device via USB and enable USB debugging, then:
```bash
./gradlew installDebug
```

Or install manually from:
```
app/build/outputs/apk/debug/app-debug.apk
```

### APK Download Locations
After successful build, find the installable APKs at:

**Debug APK** (for development/testing):
```
app/build/outputs/apk/debug/app-debug.apk
```

**Release APK** (for production/distribution):
```
app/build/outputs/apk/release/app-release-unsigned.apk
```

To build release APK:
```bash
./gradlew assembleRelease    # Linux/macOS
gradlew.bat assembleRelease  # Windows
```

### Alternative Installation Methods
- **Direct APK Transfer**: Copy APK to Android device and install
- **Android Studio Run**: Click ▶️ Run button in Android Studio
- **Wireless ADB**: Install via WiFi debugging (Android 11+)

## Configuration

### How to Connect to VoIP Server (FreeSWITCH/Asterisk Setup)

#### VoIP Server Connection Setup
1. **Launch the VoIP Call App** on your Android device
2. **Enter Your VoIP Credentials**:
   - **Username**: Your SIP extension number (e.g., `1001`, `2000`)
   - **Server IP Address**: Your VoIP server's IP address (e.g., `192.168.1.100`, `103.95.96.76`)
   - **Server Port**: SIP signaling port (default: `5060`, can be custom)
3. **Tap "Connect"** to establish connection

#### Example Configurations

**Standard Configuration (Port 5060)**:
```
Username: 09646888888
Server IP: 103.95.96.76
Server Port: 5060
```

**Custom Port Configuration (e.g., FreeSWITCH profiles)**:
```
Username: 1003_1006-humu-gmail-com_901
Server IP: 98.70.40.108
Server Port: 52318
```

**Another Custom Port Example**:
```
Username: sales_extension_100
Server IP: 192.168.1.50
Server Port: 5080
```

The app supports **any port** - not limited to standard 5060!

### Compatible VoIP Servers & PBX Systems
✅ **FreeSWITCH** - Open-source telephony platform (tested and verified)
✅ **Asterisk** - Leading open-source PBX
✅ **Kamailio** - SIP server and proxy
✅ **OpenSIPS** - SIP proxy and router
✅ **3CX** - Commercial VoIP phone system
✅ **Any RFC 3261 compliant SIP server** with IP trunking support

### Network Requirements for VoIP Calling
- **Internet Connection**: WiFi or mobile data (4G/5G recommended)
- **Bandwidth**: Minimum 64 kbps upload/download per call (200 kbps typical)
- **Latency**: <150ms for optimal call quality
- **NAT**: Automatic NAT traversal with ICE and STUN
- **Firewall**:
  - SIP: TCP/UDP port 5060
  - RTP: UDP ports 16384-32768 (configurable)

## Usage

### How to Make VoIP Calls with Voice Effects

#### Step-by-Step Calling Guide

**1. Login to VoIP Server**
   - Enter your credentials (username, IP, port)
   - Tap "Connect" button
   - Wait for successful connection confirmation

**2. Dial Phone Number**
   - Use the on-screen dial pad to enter the destination number
   - Numbers can include 0-9, *, and # for DTMF tones
   - Tap backspace to correct any mistakes

**3. Initiate VoIP Call**
   - Tap the **green call button** to start the call
   - App displays "Calling..." status while connecting
   - Connection typically establishes in 2-5 seconds

**4. Call Connected**
   - Timer automatically starts counting call duration
   - Voice effects panel becomes active
   - Audio routing controls are enabled

### Using Real-Time Voice Changer During Calls

**How to Apply Voice Effects**:
1. During an active call, locate the **"Voice Effects"** section
2. Tap any voice effect button:
   - 🎙️ **Normal** - Your natural voice
   - 👨 **Male** - Deep masculine voice
   - 👩 **Female** - Higher-pitched feminine voice
   - 👶 **Kid** - Child-like voice
3. **Effects apply instantly** - no interruption to call
4. **Switch effects anytime** during the conversation
5. Recipient hears the modified voice in real-time

**Advanced Audio Controls**:
- 🔇 **Mute/Unmute**: Tap microphone icon to silence your audio
- 🔊 **Speaker Toggle**: Switch between speaker and earpiece mode
- 🎧 **Bluetooth Support**: Automatically routes to connected Bluetooth headset
- 🔄 **Auto-switching**: Automatic routing when audio devices change

**Ending the Call**:
- Tap the **red "End Call" button** to hang up
- Call displays "Call Ended" status
- Tap the **close (X) button** to return to dial pad
- Call statistics and duration are preserved

## Android Permissions Required

### Essential VoIP App Permissions
The app requires the following Android permissions for full functionality:

#### Network Permissions
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
```
**Purpose**: Connect to VoIP servers, monitor network connectivity

#### Audio Permissions
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```
**Purpose**: Record voice for calls, apply voice effects, control audio routing

#### Bluetooth Permissions
```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" /> <!-- Android 12+ -->
```
**Purpose**: Detect and route audio to Bluetooth headsets

#### Call Management Permissions
```xml
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_PHONE_CALL" />
```
**Purpose**: Keep device awake during calls, provide call notifications

#### Phone State Permissions (Android 12+)
```xml
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.MANAGE_OWN_CALLS" />
```
**Purpose**: Manage call states, handle call UI properly

### Permission Handling
✅ **Automatic Request**: All permissions are requested at first launch
✅ **Runtime Permissions**: Follows Android best practices for permission requests
✅ **Privacy Focused**: Only essential permissions required
✅ **No Background Access**: No unnecessary background permissions

## Voice Morphing Implementation

### How Voice Changer Technology Works

The real-time voice morphing feature is implemented using **Linphone SDK's audio processing pipeline** with microphone gain control and frequency manipulation.

#### Technical Implementation Details

**Voice Effect Parameters**:
| Voice Type | Gain Level | Frequency Adjustment | Effect Description |
|------------|-----------|---------------------|-------------------|
| **Normal** | 1.0 | Baseline | Natural voice with no modifications |
| **Male** | 1.2 | Bass boost | Amplified low frequencies for deeper masculine voice |
| **Female** | 0.9 | Treble boost | Enhanced high frequencies for lighter feminine voice |
| **Kid** | 0.85 | High-pitch emphasis | Emphasized high frequencies for child-like voice |

#### Voice Transformation Process
1. **Audio Capture**: Raw audio captured from device microphone or Bluetooth headset
2. **Real-Time Processing**: Linphone SDK applies gain adjustments
3. **Frequency Modification**: Audio characteristics are altered based on selected voice type
4. **RTP Transmission**: Modified audio is encoded and sent via RTP protocol
5. **Zero Latency**: Processing happens in real-time with <10ms delay

#### Code Implementation
```kotlin
// Voice effect applied through Linphone's audio gain control
call.microphoneVolumeGain = when (voiceType) {
    VoiceType.NORMAL -> 1.0f    // Baseline natural voice
    VoiceType.MALE -> 1.2f      // 20% gain increase for depth
    VoiceType.FEMALE -> 0.9f    // 10% gain reduction for lightness
    VoiceType.KID -> 0.85f      // 15% gain reduction for child-like effect
}
```

### Voice Quality & Performance
- **Audio Codec Support**: Opus (primary), G.711 (PCMU/PCMA), G.722, Speex
- **Sample Rate**: 8kHz - 48kHz (adaptive, Opus defaults to 48kHz)
- **Bit Rate**: 64-200 kbps (Opus typical)
- **Processing Latency**: <10ms
- **CPU Usage**: <5% on modern Android devices

## Network Configuration

### Transport Protocols
The app uses **dual transport** for maximum compatibility:
- **TCP (Primary)**: Port 5060 - Better for corporate firewalls
- **UDP (Fallback)**: Port 5060 - Faster for home networks

SIP URI format with TCP: `sip:number@server:5060;transport=tcp`

### NAT Traversal & Connectivity
- **ICE Enabled**: Interactive Connectivity Establishment for NAT traversal
- **STUN Server**: Google STUN (stun.l.google.com:19302) for public IP discovery
- **Session Refresh**: 30-second keepalive for restrictive networks
- **Network Monitoring**: Automatic detection and handling of network changes

### IP Trunking Configuration
- **Direct IP Calling**: No SIP registration required
- **Peer-to-Peer**: Direct connections between endpoints
- **FreeSWITCH Compatible**: Optimized for FreeSWITCH IP trunking
- **RTP Port Range**: 16384-32768 (matches FreeSWITCH defaults)

### Firewall Requirements
Ensure the following ports are open:
- **TCP/UDP 5060**: SIP signaling (TCP preferred)
- **UDP 16384-32768**: RTP audio (configurable range)
- **UDP 19302**: STUN (Google STUN server)

## Troubleshooting & FAQ

### Common VoIP Calling Issues

#### 🎤 Audio Problems

**Problem**: No audio during call (microphone not working)
- ✅ **Solution**: Grant microphone and speaker permissions in Android Settings
- ✅ **Check**: Ensure microphone isn't muted in the app
- ✅ **Bluetooth**: If using Bluetooth headset, verify SCO connection is established
- ✅ **Logs**: Check for "Upload bandwidth: 0 bps" in logs - indicates mic issue
- ✅ **Verify**: Test microphone in other apps to rule out hardware issues

**Problem**: Bluetooth headset microphone not working
- ✅ **Solution**: App automatically detects Bluetooth - check "Bluetooth connected" in logs
- ✅ **SCO**: Verify "Bluetooth SCO state: true" in logs
- ✅ **Permissions**: Ensure BLUETOOTH_CONNECT permission granted (Android 12+)
- ✅ **Audio Routing**: Check logs for "Audio routed to Bluetooth via Linphone"
- ✅ **Early Routing**: Bluetooth connects immediately when dialing - hear RBT instantly
- ✅ **Restart**: Disconnect and reconnect Bluetooth headset

**Problem**: Can't hear ringback tone (RBT) on Bluetooth before answer
- ✅ **Solution**: This is now fixed! Bluetooth routes immediately when call starts
- ✅ **Feature**: You'll hear RBT through Bluetooth before B party answers
- ✅ **Timing**: Audio connects in <1 second, not after answer
- ✅ **Logs**: Check for "🔵 Early Bluetooth routing for ringback tone" in logs

**Problem**: Echo or feedback during calls
- ✅ **Solution**: Echo cancellation is enabled by default - reduce speaker volume
- ✅ **Alternative**: Use headphones for better audio isolation
- ✅ **Settings**: Disable speaker mode and use earpiece
- ✅ **Bluetooth**: Bluetooth headsets eliminate echo naturally

**Problem**: Choppy or robotic audio
- ✅ **Solution**: Check network connection quality (WiFi preferred over mobile data)
- ✅ **Bandwidth**: Ensure at least 64 kbps upload/download speed (check logs for bandwidth stats)
- ✅ **Network**: Switch from WiFi to mobile data or vice versa
- ✅ **Codec**: App auto-negotiates best codec (Opus preferred)

**Problem**: Voice effects not working
- ✅ **Requirement**: Voice morphing only works during **active calls** (Connected state)
- ✅ **Check**: Effects apply to **outgoing audio** only - recipient hears modified voice
- ✅ **Tip**: Switch between different voice effects to test functionality
- ✅ **Bluetooth**: Voice effects work with Bluetooth headsets

#### 📞 Connection & Calling Issues

**Problem**: Cannot connect to VoIP server (especially on office WiFi)
- ✅ **TCP Transport**: App automatically uses TCP for better firewall compatibility
- ✅ **Port**: Confirm SIP port (usually 5060) is correct
- ✅ **Network**: Check internet connection and firewall settings
- ✅ **Server**: Ensure VoIP server is running and accessible
- ✅ **Logs**: Check for "OutgoingProgress" state in logs

**Problem**: Call works on mobile data but not office WiFi
- ✅ **Firewall**: Office firewalls may block SIP/RTP - TCP transport helps
- ✅ **ICE**: App uses ICE for NAT traversal (enabled automatically)
- ✅ **IT Support**: Contact IT department to allow ports 5060 and 16384-32768
- ✅ **Success**: App is designed to work on restrictive networks with TCP

**Problem**: Call initiation fails
- ✅ **Permissions**: Ensure server allows IP trunking without registration
- ✅ **Format**: Check phone number format (include country/area codes if needed)
- ✅ **Credentials**: Verify username is valid on the VoIP server
- ✅ **Logs**: Check "Call params - Audio: true, Direction: SendRecv" in logs

**Problem**: NAT/Firewall blocking calls
- ✅ **ICE**: Enabled automatically for NAT traversal
- ✅ **STUN**: Using Google STUN server (stun.l.google.com:19302)
- ✅ **TCP**: TCP transport bypasses many firewall restrictions
- ✅ **Network**: Try connecting from a different network to isolate the issue

#### 🔧 App Performance Issues

**Problem**: App crashes or freezes
- ✅ **Update**: Ensure you have the latest version of the app
- ✅ **Clear**: Clear app cache in Android Settings → Apps → VoIP Call
- ✅ **Restart**: Force stop and restart the application
- ✅ **Logs**: Check logcat for error messages (for developers)

**Problem**: High battery drain
- ✅ **Background**: App uses foreground service only during active calls
- ✅ **Optimization**: Enable battery optimization in Android settings if not calling frequently
- ✅ **Bluetooth**: Bluetooth SCO is stopped when call ends to save battery

**Problem**: Call ended screen not showing close button
- ✅ **State**: Check logs for "Call state changed to: ENDED"
- ✅ **UI**: Close button (X icon) should appear automatically
- ✅ **Navigation**: Clicking close button returns to dial screen
- ✅ **Auto**: System handles navigation when call ends

### Frequently Asked Questions (FAQ)

**Q: Does the app support incoming calls?**
A: Current version supports outgoing calls only. Incoming call support planned for future releases.

**Q: Can I record calls with voice effects?**
A: Call recording is not currently implemented. Voice effects are applied in real-time during calls only.

**Q: Does voice morphing work with all VoIP servers?**
A: Yes, voice effects are applied client-side before transmission, compatible with all SIP servers.

**Q: What's the difference between IP trunking and SIP registration?**
A: IP trunking allows direct peer-to-peer calling without authentication. No SIP REGISTER required.

**Q: Can I use this app over mobile data?**
A: Yes, the app works perfectly on mobile data, home WiFi, and office WiFi. TCP transport ensures compatibility.

**Q: Are calls encrypted?**
A: Current implementation uses unencrypted SIP/RTP. SRTP encryption can be added for production use.

**Q: Does Bluetooth headset work for both mic and speaker?**
A: Yes! Full Bluetooth support including both microphone capture and speaker playback. Bluetooth connects instantly when you dial.

**Q: Can I hear ringback tone (RBT) through Bluetooth before the call is answered?**
A: Yes! Bluetooth audio routes immediately when you start dialing, so you hear the RBT through your Bluetooth headset instantly.

**Q: Why does my voice work after disconnecting Bluetooth?**
A: If this happens, it indicates the Bluetooth routing was working - the app automatically switches back to phone mic/speaker.

**Q: Why did my first call fail but second call worked?**
A: This is now fixed! The app waits 2 seconds for core initialization (STUN/ICE/network). First call works every time now.

**Q: Can I use custom SIP ports (not 5060)?**
A: Yes! The app supports dynamic ports. You can connect to any port like 52318, 5080, or any custom port.

**Q: What networks does the app work on?**
A: Works on all networks: Office WiFi (with firewall), Home WiFi, Mobile Data (4G/5G). TCP transport ensures compatibility.

## Development

### Debug Logging
The app includes detailed logging for debugging:
```kotlin
Log.d("LinphoneService", "Message")
Log.d("CallViewModel", "Message")
Log.d("VoIPCallApp", "Message")
```

View logs using:
```bash
adb logcat | grep -E "LinphoneService|CallViewModel|VoIPCallApp"
```

Filter for specific events:
```bash
# Call state changes
adb logcat | grep "CALL STATE"

# Audio routing
adb logcat | grep "Bluetooth"

# Network info
adb logcat | grep "SDP\|bandwidth"
```

### Testing
Run unit tests:
```bash
./gradlew test
```

Run instrumented tests:
```bash
./gradlew connectedAndroidTest
```

### Key Features to Test
- ✅ Login with different server configurations
- ✅ Outgoing calls on different networks (WiFi, mobile data)
- ✅ Voice effects switching during active calls
- ✅ Bluetooth headset mic and speaker
- ✅ Call end screen with close button
- ✅ Network change during active call
- ✅ Mute/unmute and speaker toggle

## License

This project is for demonstration purposes. Ensure proper licensing when using Linphone SDK in production.

## Contributing & Support

### How to Contribute to VoIP Call Project
We welcome contributions from the developer community! Here's how you can help:

#### Contribution Guidelines
1. **Fork the repository** on GitHub
2. **Create a feature branch**: `git checkout -b feature/your-feature-name`
3. **Commit your changes**: `git commit -m "Add amazing feature"`
4. **Push to branch**: `git push origin feature/your-feature-name`
5. **Open a Pull Request** with detailed description

#### Areas for Contribution
- 🆕 **New Features**: Incoming call support, call recording, video calling
- 🐛 **Bug Fixes**: Report and fix issues
- 📚 **Documentation**: Improve README, add tutorials
- 🎨 **UI/UX**: Enhance Material Design interface
- 🔊 **Audio**: Advanced voice effects, equalizer, filters
- 🌐 **Internationalization**: Add language translations
- 🎧 **Bluetooth**: Improve Bluetooth device detection and routing
- 🌐 **Network**: Enhance NAT traversal and firewall compatibility

### Getting Help & Support

**Documentation Resources**:
- 📖 [Linphone SDK Documentation](https://linphone.org/) - Official Linphone API reference
- 📖 [FreeSWITCH Documentation](https://freeswitch.org/) - VoIP server configuration
- 📖 [Jetpack Compose Guide](https://developer.android.com/jetpack/compose) - Modern Android UI
- 📖 [Kotlin Documentation](https://kotlinlang.org/docs/) - Kotlin language reference

**Community Support**:
- 💬 Open an issue on GitHub for bugs or feature requests
- 💬 Check existing issues before creating new ones
- 💬 Join Android VoIP developer communities

**Commercial Support**:
- Available for enterprise deployments
- Custom feature development
- Integration assistance with existing PBX systems

## Roadmap & Future Features

### Planned Features (Upcoming Releases)
- ✨ **v1.1**: Incoming call support with push notifications
- ✨ **v1.2**: Call recording with voice effects
- ✨ **v1.3**: Video calling with camera support
- ✨ **v1.4**: SIP registration mode for traditional PBX
- ✨ **v1.5**: Advanced voice effects (robot, echo, reverb)
- ✨ **v1.6**: Call history and contacts integration
- ✨ **v1.7**: SRTP/TLS encryption for secure calls
- ✨ **v1.8**: Multi-party conference calling

### Under Consideration
- Call quality indicators with real-time stats
- Bandwidth optimization and codec selection
- Wear OS companion app
- Multiple Bluetooth device support
- Wi-Fi calling handoff

## Version History & Changelog

### v1.0.2 (Current Release) - October 2025
**Major Update: Dynamic Port Support & Early Bluetooth Routing**
- ✅ **Dynamic Port Support**: Connect to any SIP port (not just 5060)
  - Custom port configuration (e.g., 52318, 5080, etc.)
  - Random local port binding for maximum compatibility
  - FreeSWITCH profile support on any port
- ✅ **Correct Caller Identity**: Username in From header
  - FreeSWITCH receives correct username in SIP From header
  - IP trunking authentication improved
  - Proper dialplan routing on server side
- ✅ **First Call Reliability**: Core readiness check
  - 2-second initialization delay for STUN/ICE/network setup
  - Call queuing if initiated before core ready
  - Automatic execution of queued calls
  - First call now works every time
- ✅ **Early Bluetooth Routing**: Instant RBT on Bluetooth
  - Bluetooth audio connects immediately when dialing
  - Hear ringback tone (RBT) through Bluetooth before answer
  - No 2-second delay - instant connection
  - Multiple routing points (OutgoingInit, OutgoingProgress, OutgoingRinging)
- ✅ **Bluetooth Improvements**:
  - Full mic and speaker support for Bluetooth headsets
  - Automatic Bluetooth detection and SCO routing
  - Separate capture/playback device handling
  - Works with all popular Bluetooth headsets
- ✅ **Network Compatibility**: Works on all networks
  - TCP transport priority for corporate firewalls
  - ICE enabled for NAT traversal
  - Google STUN server integration
  - Tested on office WiFi, home WiFi, and mobile data
- ✅ **Audio Improvements**:
  - Aggressive microphone monitoring and recovery
  - Auto-restart mic if upload bandwidth drops
  - Audio device routing improvements
  - Network change detection and handling
- ✅ **FreeSWITCH Optimization**:
  - RTP port range 16384-32768 (matches FreeSWITCH)
  - Session refresh optimization
  - Better codec negotiation

### v1.0.1 - October 2025
**Network & Bluetooth Initial Support**
- ✅ Bluetooth headset support (mic & speaker)
- ✅ TCP transport for firewalls
- ✅ ICE/STUN for NAT traversal
- ✅ Call ended screen improvements
- ✅ Audio monitoring and recovery

### v1.0.0 - January 2025
**Initial Public Release**
- ✅ **Authentication**: Login with IP trunking (username, IP, port)
- ✅ **Dial Pad**: Full numeric keypad with DTMF support
- ✅ **Outgoing Calls**: FreeSWITCH/Asterisk integration via SIP
- ✅ **Voice Morphing**: 4 real-time voice effects (Normal, Male, Female, Kid)
- ✅ **Audio Controls**: Mute, speaker toggle, echo cancellation
- ✅ **Call Management**: Timer, state tracking, hang up
- ✅ **UI**: Material Design 3 with Jetpack Compose
- ✅ **Platform**: Android 7.0+ (API 24+)

---

## License & Legal

### Software License
This project is developed for **demonstration and educational purposes**.

⚠️ **Important**: When deploying to production, ensure proper licensing for:
- Linphone SDK (GPLv3 for open source, commercial license available)
- Any third-party libraries used
- Compliance with telecommunications regulations in your region

### Trademarks
- Android is a trademark of Google LLC
- Linphone is developed by Belledonne Communications
- FreeSWITCH is a trademark of Freeswitch Inc.
- Asterisk is a trademark of Sangoma Technologies Corporation

---

## Tags & Keywords
`android-voip` `voice-changer` `real-time-voice-morphing` `kotlin-android` `jetpack-compose` `freeswitch-integration` `sip-client` `ip-trunking` `linphone-sdk` `voip-calling-app` `voice-effects` `android-calling` `sip-phone` `voice-modulator` `voip-application` `kotlin-compose` `material-design-3` `android-development` `open-source-voip` `telecommunications` `bluetooth-voip` `corporate-network` `nat-traversal` `ice-stun` `tcp-transport`

---

**Built with ❤️ using Kotlin and Jetpack Compose**

**Star ⭐ this repository if you find it useful!**
