package com.voipcall.model

data class TrunkConfig(
    val username: String = "",
    val serverIp: String = "",
    val serverPort: Int = 5060,
    val localPort: Int = 5060
)
