package com.example.network.model

import com.example.data.model.TransportType

data class PeerNode(
    val id: String,                 // Jesr ID (e.g. JESR-7K92-X4PM)
    val name: String,               // Display name or device name
    val transportType: TransportType,
    val rssi: Int = -60,
    val ipAddress: String? = null,
    val port: Int = 8888,
    val bleAddress: String? = null,
    val publicKeyBase64: String? = null,
    val signingPublicKeyBase64: String? = null,
    val lastSeen: Long = System.currentTimeMillis(),
    val hopDistance: Int = 1,
    val nearbyVisible: Boolean = true
)
