package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "seen_packets")
data class SeenPacketEntity(
    @PrimaryKey val packetId: String,
    val seenAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 48 * 3600 * 1000L
)
