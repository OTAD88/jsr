package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DeliveryStatus

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val recipientId: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: DeliveryStatus = DeliveryStatus.LOCAL_PENDING,
    val isIncoming: Boolean = false,
    val ttl: Int = 7,
    val hopCount: Int = 0,
    val isBroadcast: Boolean = false,
    val isSos: Boolean = false,
    val estimatedDistanceMeters: Float? = null,
    val isDistanceConsented: Boolean = false,
    val attachmentUri: String? = null,
    val attachmentName: String? = null
)
