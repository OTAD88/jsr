package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.FriendEntity
import com.example.data.model.FriendStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends WHERE status = 'FRIEND' ORDER BY addedAt DESC")
    fun getAllFriends(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends ORDER BY addedAt DESC")
    fun getAllEntries(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE status = 'REQUEST_RECEIVED' ORDER BY addedAt DESC")
    fun getPendingReceivedRequests(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE status = 'REQUEST_SENT' ORDER BY addedAt DESC")
    fun getPendingSentRequests(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE jesrId = :jesrId LIMIT 1")
    suspend fun getFriend(jesrId: String): FriendEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFriend(friend: FriendEntity)

    @Query("UPDATE friends SET status = :status WHERE jesrId = :jesrId")
    suspend fun updateStatus(jesrId: String, status: FriendStatus)

    @Query("UPDATE friends SET distanceConsentEnabled = :enabled WHERE jesrId = :jesrId")
    suspend fun updateDistanceConsent(jesrId: String, enabled: Boolean)

    @Query("UPDATE friends SET distancePermissionOverride = :overrideVal WHERE jesrId = :jesrId")
    suspend fun updateDistanceOverride(jesrId: String, overrideVal: Boolean?)

    @Query("UPDATE friends SET mutualFriendsPermissionOverride = :overrideVal WHERE jesrId = :jesrId")
    suspend fun updateMutualFriendsOverride(jesrId: String, overrideVal: Boolean?)

    @Query("UPDATE friends SET isMuted = :muted WHERE jesrId = :jesrId")
    suspend fun updateMuted(jesrId: String, muted: Boolean)

    @Query("UPDATE friends SET isBlocked = :blocked, unblockedAwaitingResume = :awaitingResume, localResumeApproved = :localApproved, peerResumeApproved = :peerApproved WHERE jesrId = :jesrId")
    suspend fun updateBlockAndResumeState(
        jesrId: String,
        blocked: Boolean,
        awaitingResume: Boolean,
        localApproved: Boolean,
        peerApproved: Boolean
    )

    @Query("UPDATE friends SET muteRequestsOverride = :overrideVal WHERE jesrId = :jesrId")
    suspend fun updateMuteRequestsOverride(jesrId: String, overrideVal: Boolean?)

    @Query("UPDATE friends SET blockRequestsOverride = :overrideVal WHERE jesrId = :jesrId")
    suspend fun updateBlockRequestsOverride(jesrId: String, overrideVal: Boolean?)

    @Query("DELETE FROM friends WHERE status = 'REQUEST_RECEIVED'")
    suspend fun deleteAllIncomingRequests()

    @Query("DELETE FROM friends WHERE jesrId = :jesrId")
    suspend fun deleteFriend(jesrId: String)
}
