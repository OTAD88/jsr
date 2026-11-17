package com.example.data.model

import com.example.crypto.JesrCrypto
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Universal network packet structure exchanged across BLE and Wi-Fi Direct transports.
 * Includes ECDSA signature fields for sender authenticity & replay/tamper resistance.
 */
data class MeshPacket(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val recipientId: String,
    val type: MessageType,
    val payload: String = "",       // Encrypted ciphertext (Base64) or command payload
    val iv: String = "",            // Base64 Initialization Vector for AES-GCM
    val ttl: Int = 7,               // Remaining hops before dropping
    val hopCount: Int = 0,          // Hops traversed so far
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 48 * 3600 * 1000L, // 48h expiration
    val previousHopId: String = senderId,
    val distanceConsent: Boolean = false,
    val senderRssi: Int = 0,
    val senderPublicKey: String = "",
    val signingPublicKey: String = "",
    val signature: String = ""
) {
    fun canonicalSigningBytes(): ByteArray {
        val canonical = listOf(
            id,
            senderId,
            recipientId,
            type.name,
            payload,
            iv,
            timestamp.toString(),
            expiresAt.toString(),
            distanceConsent.toString(),
            senderPublicKey,
            signingPublicKey
        ).joinToString("|")
        return canonical.toByteArray(StandardCharsets.UTF_8)
    }

    fun aadString(): String = "$id|$senderId|$recipientId|${type.name}|$timestamp"

    fun signedWith(crypto: JesrCrypto): MeshPacket {
        val withKeys = copy(
            senderPublicKey = senderPublicKey.ifBlank { crypto.myPublicKeyBase64 },
            signingPublicKey = signingPublicKey.ifBlank { crypto.mySigningPublicKeyBase64 }
        )
        val sig = crypto.signData(withKeys.canonicalSigningBytes())
        return withKeys.copy(signature = sig)
    }

    fun hasValidSignature(crypto: JesrCrypto): Boolean {
        if (signature.isBlank() || signingPublicKey.isBlank()) return false
        if (senderPublicKey.isNotBlank()) {
            val derivedId = crypto.deriveJesrIdFromPublicKeyBase64(senderPublicKey)
            if (derivedId != null && derivedId != senderId) return false
        }
        return crypto.verifySignature(canonicalSigningBytes(), signature, signingPublicKey)
    }

    fun toJson(): String {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("senderId", senderId)
        obj.put("recipientId", recipientId)
        obj.put("type", type.name)
        obj.put("payload", payload)
        obj.put("iv", iv)
        obj.put("ttl", ttl)
        obj.put("hopCount", hopCount)
        obj.put("timestamp", timestamp)
        obj.put("expiresAt", expiresAt)
        obj.put("previousHopId", previousHopId)
        obj.put("distanceConsent", distanceConsent)
        obj.put("senderRssi", senderRssi)
        if (senderPublicKey.isNotEmpty()) obj.put("senderPublicKey", senderPublicKey)
        if (signingPublicKey.isNotEmpty()) obj.put("signingPublicKey", signingPublicKey)
        if (signature.isNotEmpty()) obj.put("signature", signature)
        return obj.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): MeshPacket? {
            return try {
                val obj = JSONObject(jsonStr)
                MeshPacket(
                    id = obj.getString("id"),
                    senderId = obj.getString("senderId"),
                    recipientId = obj.getString("recipientId"),
                    type = MessageType.valueOf(obj.getString("type")),
                    payload = obj.optString("payload", ""),
                    iv = obj.optString("iv", ""),
                    ttl = obj.optInt("ttl", 7),
                    hopCount = obj.optInt("hopCount", 0),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    expiresAt = obj.optLong("expiresAt", System.currentTimeMillis() + 48 * 3600 * 1000L),
                    previousHopId = obj.optString("previousHopId", obj.getString("senderId")),
                    distanceConsent = obj.optBoolean("distanceConsent", false),
                    senderRssi = obj.optInt("senderRssi", 0),
                    senderPublicKey = obj.optString("senderPublicKey", ""),
                    signingPublicKey = obj.optString("signingPublicKey", ""),
                    signature = obj.optString("signature", "")
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
