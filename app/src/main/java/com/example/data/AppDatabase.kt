package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.dao.ConversationDao
import com.example.data.dao.FriendDao
import com.example.data.dao.MessageDao
import com.example.data.dao.RelayQueueDao
import com.example.data.dao.SeenPacketDao
import com.example.data.entity.ConversationEntity
import com.example.data.entity.FriendEntity
import com.example.data.entity.MessageEntity
import com.example.data.entity.RelayQueueEntity
import com.example.data.entity.SeenPacketEntity
import com.example.data.model.DeliveryStatus
import com.example.data.model.FriendStatus

class Converters {
    @TypeConverter
    fun fromDeliveryStatus(status: DeliveryStatus): String = status.name

    @TypeConverter
    fun toDeliveryStatus(value: String): DeliveryStatus =
        try { DeliveryStatus.valueOf(value) } catch (e: Exception) { DeliveryStatus.LOCAL_PENDING }

    @TypeConverter
    fun fromFriendStatus(status: FriendStatus): String = status.name

    @TypeConverter
    fun toFriendStatus(value: String): FriendStatus =
        try { FriendStatus.valueOf(value) } catch (e: Exception) { FriendStatus.FRIEND }
}

@Database(
    entities = [
        MessageEntity::class,
        ConversationEntity::class,
        FriendEntity::class,
        RelayQueueEntity::class,
        SeenPacketEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun friendDao(): FriendDao
    abstract fun relayQueueDao(): RelayQueueDao
    abstract fun seenPacketDao(): SeenPacketDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jesr_mesh_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
