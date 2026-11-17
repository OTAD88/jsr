package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.FriendStatus

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val jesrId: String,
    val displayName: String,
    val publicKeyBase64: String = "",
    val status: FriendStatus = FriendStatus.FRIEND,
    val addedAt: Long = System.currentTimeMillis(),
    val colorIndex: Int = 0,
    val avatarUri: String? = null,
    // Peer's consent to let us see their message sending distance
    val distanceConsentEnabled: Boolean = false,
    // Peer's consent to let us see their mutual friends
    val peerMutualFriendsConsentEnabled: Boolean = false,
    val mutualFriendsCount: Int = 0,
    // Individual privacy overrides (null = follow global setting, true/false = override in either direction)
    val distancePermissionOverride: Boolean? = null,
    val mutualFriendsPermissionOverride: Boolean? = null,
    // Friend management & blocking/resume states
    val isMuted: Boolean = false,
    val isBlocked: Boolean = false,
    val unblockedAwaitingResume: Boolean = false,
    val localResumeApproved: Boolean = false,
    val peerResumeApproved: Boolean = false,
    // Friend Request Privacy rules (null = follow global rule, true/false = individual exception/rule)
    val muteRequestsOverride: Boolean? = null,
    val blockRequestsOverride: Boolean? = null
)
