package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.RelayQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RelayQueueDao {
    @Query("SELECT * FROM relay_queue WHERE nextAttemptTime <= :currentTime ORDER BY createdAt ASC")
    suspend fun getReadyPackets(currentTime: Long): List<RelayQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(entity: RelayQueueEntity)

    @Query("DELETE FROM relay_queue WHERE messageId = :messageId")
    suspend fun remove(messageId: String)

    @Query("UPDATE relay_queue SET retryCount = retryCount + 1, nextAttemptTime = :nextAttempt WHERE messageId = :messageId")
    suspend fun incrementRetry(messageId: String, nextAttempt: Long)

    @Query("SELECT * FROM relay_queue ORDER BY createdAt DESC")
    fun getAllQueued(): Flow<List<RelayQueueEntity>>

    @Query("SELECT COUNT(*) FROM relay_queue")
    fun getQueueCount(): Flow<Int>
}
