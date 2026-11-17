package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.SeenPacketEntity

@Dao
interface SeenPacketDao {
    @Query("SELECT EXISTS(SELECT 1 FROM seen_packets WHERE packetId = :packetId)")
    suspend fun exists(packetId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markSeen(entity: SeenPacketEntity)

    @Query("DELETE FROM seen_packets WHERE expiresAt < :now")
    suspend fun pruneExpired(now: Long)
}
