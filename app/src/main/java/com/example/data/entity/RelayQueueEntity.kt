package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "relay_queue")
data class RelayQueueEntity(
    @PrimaryKey val messageId: String,
    val targetPeerId: String,
    val packetJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val nextAttemptTime: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val maxRetries: Int = 5
)
