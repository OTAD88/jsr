package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.MessageEntity
import com.example.data.model.DeliveryStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    fun getMessagesForConversation(convId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateMessageStatus(id: String, status: DeliveryStatus)

    @Query("UPDATE messages SET status = :status, hopCount = :hopCount WHERE id = :id")
    suspend fun updateMessageStatusAndHop(id: String, status: DeliveryStatus, hopCount: Int)

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: String): MessageEntity?

    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(limit: Int = 20): Flow<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM messages WHERE status = 'DELIVERED'")
    fun getDeliveredMessagesCount(): Flow<Int>

    @Query("DELETE FROM messages WHERE conversationId = :convId")
    suspend fun deleteMessagesForConversation(convId: String)
}
