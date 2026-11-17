package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.crypto.JesrCrypto
import com.example.data.entity.ConversationEntity
import com.example.data.entity.FriendEntity
import com.example.data.entity.MessageEntity
import com.example.data.model.DeliveryStatus
import com.example.data.model.FriendStatus
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

@IgnoreExtraProperties
data class CloudAccountRecord(
    @DocumentId val id: String = "",
    val uid: String = "",
    val jesrId: String = "",
    val displayName: String = "",
    val ecdhPublicKey: String = "",
    val signingPublicKey: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudPrivateIdentityRecord(
    @DocumentId val id: String = "",
    val uid: String = "",
    val jesrId: String = "",
    val encryptedKeyBundle: String = "",
    val ecdhPublicKey: String = "",
    val signingPublicKey: String = "",
    val displayName: String = "",
    val avatarBase64: String = "",
    val coverBase64: String = "",
    val language: String = "AR",
    val globalDistanceAllowed: Boolean = false,
    val globalMutualFriendsAllowed: Boolean = false,
    val nearbyVisible: Boolean = false,
    val appearanceMode: String = "AUTO",
    val cinematicThemeId: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudFriendRecord(
    @DocumentId val id: String = "",
    val jesrId: String = "",
    val displayName: String = "",
    val publicKeyBase64: String = "",
    val status: String = FriendStatus.FRIEND.name,
    val addedAt: Long = 0L,
    val isMuted: Boolean = false,
    val isBlocked: Boolean = false,
    val unblockedAwaitingResume: Boolean = false,
    val localResumeApproved: Boolean = false,
    val peerResumeApproved: Boolean = false,
    val distanceConsentEnabled: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudConversationRecord(
    @DocumentId val id: String = "",
    val convId: String = "",
    val peerName: String = "",
    val lastMessageText: String = "",
    val lastMessageTime: Long = 0L,
    val unreadCount: Int = 0,
    val hopsDistance: Int = 1,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudMessageRecord(
    @DocumentId val id: String = "",
    val messageId: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val recipientId: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val status: String = DeliveryStatus.DELIVERED.name,
    val isIncoming: Boolean = false,
    val ttl: Int = 7,
    val hopCount: Int = 0,
    val isBroadcast: Boolean = false,
    val isSos: Boolean = false,
    val attachmentName: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CloudIdentitySnapshot(
    val displayName: String,
    val avatarBase64: String,
    val coverBase64: String,
    val language: String,
    val globalDistanceAllowed: Boolean,
    val globalMutualFriendsAllowed: Boolean,
    val nearbyVisible: Boolean,
    val appearanceMode: String,
    val cinematicThemeId: String
)

data class CloudIdentitySyncResult(
    val restoredFromCloud: Boolean,
    val remoteProfile: CloudPrivateIdentityRecord?
)

data class CloudFullBackupBundle(
    val friends: List<FriendEntity>,
    val conversations: List<ConversationEntity>,
    val messages: List<MessageEntity>
)

class CloudAccountSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun currentUid(): String? = auth.currentUser?.uid

    fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
        val causes = generateSequence(exception) { it.cause }
        val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
            ?: causes.firstOrNull { it !is CancellationException }
            ?: exception

        val currentUser = auth.currentUser
        val providerInfoList = currentUser?.providerData?.map { provider ->
            JSONObject().apply {
                put("providerId", provider.providerId)
                put("email", provider.email)
            }
        } ?: emptyList()

        val errorInfoJson = JSONObject().apply {
            put("error", rootError.message ?: rootError.toString())
            put("operationType", operationType.value)
            put("path", path)
            put("authInfo", JSONObject().apply {
                put("userId", currentUser?.uid)
                put("email", currentUser?.email)
                put("providerInfo", JSONArray(providerInfoList))
            })
        }
        val json = errorInfoJson.toString()
        Log.e("CloudAccountSync", "Firestore Error: $json")
        return json
    }

    private fun safeDocId(raw: String): String =
        raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128).ifBlank { "default" }

    suspend fun syncOrRestoreIdentity(
        crypto: JesrCrypto,
        localSnapshot: CloudIdentitySnapshot
    ): Result<CloudIdentitySyncResult> {
        val uid = currentUid() ?: return Result.failure(IllegalStateException("Not signed in"))
        val privPath = "users/$uid/private/identity"
        return try {
            val docRef = db.collection("users").document(uid)
                .collection("private").document("identity")
            val existing = docRef.get().await().toObject(CloudPrivateIdentityRecord::class.java)

            val restored = if (existing != null && existing.encryptedKeyBundle.isNotBlank()) {
                crypto.importCloudIdentityBundle(existing.encryptedKeyBundle, accountSecret = uid)
            } else {
                false
            }

            if (!restored) {
                val bundle = crypto.exportCloudIdentityBundle(accountSecret = uid)
                val payload = mapOf<String, Any?>(
                    "uid" to uid,
                    "jesrId" to crypto.myJesrId,
                    "encryptedKeyBundle" to bundle,
                    "ecdhPublicKey" to crypto.myPublicKeyBase64,
                    "signingPublicKey" to crypto.mySigningPublicKeyBase64,
                    "displayName" to localSnapshot.displayName.take(60),
                    "avatarBase64" to localSnapshot.avatarBase64.take(150_000),
                    "coverBase64" to localSnapshot.coverBase64.take(180_000),
                    "language" to localSnapshot.language,
                    "globalDistanceAllowed" to localSnapshot.globalDistanceAllowed,
                    "globalMutualFriendsAllowed" to localSnapshot.globalMutualFriendsAllowed,
                    "nearbyVisible" to localSnapshot.nearbyVisible,
                    "appearanceMode" to localSnapshot.appearanceMode,
                    "cinematicThemeId" to localSnapshot.cinematicThemeId,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            }

            // Ensure public lookup directory record for this jesrId is up-to-date
            val effectiveName = if (restored && !existing?.displayName.isNullOrBlank()) {
                existing!!.displayName
            } else {
                localSnapshot.displayName
            }
            val accountRef = db.collection(COLLECTION_ACCOUNTS).document(safeDocId(crypto.myJesrId))
            val accountPayload = mapOf<String, Any?>(
                "uid" to uid,
                "jesrId" to crypto.myJesrId,
                "displayName" to effectiveName.take(60),
                "ecdhPublicKey" to crypto.myPublicKeyBase64,
                "signingPublicKey" to crypto.mySigningPublicKeyBase64,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            accountRef.set(accountPayload).await()

            Result.success(CloudIdentitySyncResult(restoredFromCloud = restored, remoteProfile = existing))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, privPath)
            Result.failure(e)
        }
    }

    suspend fun pushProfileAndSettingsUpdate(
        crypto: JesrCrypto,
        snapshot: CloudIdentitySnapshot
    ): Result<Unit> {
        val uid = currentUid() ?: return Result.success(Unit)
        return try {
            val privRef = db.collection("users").document(uid)
                .collection("private").document("identity")
            val payload = mapOf<String, Any?>(
                "uid" to uid,
                "jesrId" to crypto.myJesrId,
                "encryptedKeyBundle" to crypto.exportCloudIdentityBundle(accountSecret = uid),
                "ecdhPublicKey" to crypto.myPublicKeyBase64,
                "signingPublicKey" to crypto.mySigningPublicKeyBase64,
                "displayName" to snapshot.displayName.take(60),
                "avatarBase64" to snapshot.avatarBase64.take(150_000),
                "coverBase64" to snapshot.coverBase64.take(180_000),
                "language" to snapshot.language,
                "globalDistanceAllowed" to snapshot.globalDistanceAllowed,
                "globalMutualFriendsAllowed" to snapshot.globalMutualFriendsAllowed,
                "nearbyVisible" to snapshot.nearbyVisible,
                "appearanceMode" to snapshot.appearanceMode,
                "cinematicThemeId" to snapshot.cinematicThemeId,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            privRef.set(payload).await()

            db.collection(COLLECTION_ACCOUNTS).document(safeDocId(crypto.myJesrId))
                .set(
                    mapOf<String, Any?>(
                        "uid" to uid,
                        "jesrId" to crypto.myJesrId,
                        "displayName" to snapshot.displayName.take(60),
                        "ecdhPublicKey" to crypto.myPublicKeyBase64,
                        "signingPublicKey" to crypto.mySigningPublicKeyBase64,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/private/identity")
            Result.failure(e)
        }
    }

    suspend fun lookupAccountById(jesrId: String): Result<CloudAccountRecord?> {
        if (currentUid() == null) return Result.success(null)
        val clean = safeDocId(jesrId.trim().uppercase())
        return try {
            val snap = db.collection(COLLECTION_ACCOUNTS).document(clean).get().await()
            Result.success(snap.toObject(CloudAccountRecord::class.java))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "$COLLECTION_ACCOUNTS/$clean")
            Result.failure(e)
        }
    }

    suspend fun syncFriend(friend: FriendEntity): Result<Unit> {
        val uid = currentUid() ?: return Result.success(Unit)
        val docId = safeDocId(friend.jesrId)
        return try {
            db.collection("users").document(uid)
                .collection("friends").document(docId)
                .set(
                    mapOf(
                        "jesrId" to friend.jesrId,
                        "displayName" to friend.displayName.take(60),
                        "publicKeyBase64" to friend.publicKeyBase64,
                        "status" to friend.status.name,
                        "addedAt" to friend.addedAt,
                        "isMuted" to friend.isMuted,
                        "isBlocked" to friend.isBlocked,
                        "unblockedAwaitingResume" to friend.unblockedAwaitingResume,
                        "localResumeApproved" to friend.localResumeApproved,
                        "peerResumeApproved" to friend.peerResumeApproved,
                        "distanceConsentEnabled" to friend.distanceConsentEnabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/friends/$docId")
            Result.failure(e)
        }
    }

    suspend fun deleteFriendRecord(jesrId: String): Result<Unit> {
        val uid = currentUid() ?: return Result.success(Unit)
        val docId = safeDocId(jesrId)
        return try {
            db.collection("users").document(uid).collection("friends").document(docId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/friends/$docId")
            Result.failure(e)
        }
    }

    suspend fun syncConversation(conv: ConversationEntity): Result<Unit> {
        val uid = currentUid() ?: return Result.success(Unit)
        val docId = safeDocId(conv.id)
        return try {
            db.collection("users").document(uid)
                .collection("conversations").document(docId)
                .set(
                    mapOf(
                        "convId" to conv.id,
                        "peerName" to conv.peerName.take(60),
                        "lastMessageText" to conv.lastMessageText.take(500),
                        "lastMessageTime" to conv.lastMessageTime,
                        "unreadCount" to conv.unreadCount,
                        "hopsDistance" to conv.hopsDistance,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/conversations/$docId")
            Result.failure(e)
        }
    }

    suspend fun deleteConversationRecord(convId: String): Result<Unit> {
        val uid = currentUid() ?: return Result.success(Unit)
        val docId = safeDocId(convId)
        return try {
            db.collection("users").document(uid).collection("conversations").document(docId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/conversations/$docId")
            Result.failure(e)
        }
    }

    suspend fun syncMessage(msg: MessageEntity): Result<Unit> {
        val uid = currentUid() ?: return Result.success(Unit)
        val docId = safeDocId(msg.id)
        return try {
            db.collection("users").document(uid)
                .collection("messages").document(docId)
                .set(
                    mapOf(
                        "messageId" to msg.id,
                        "conversationId" to msg.conversationId,
                        "senderId" to msg.senderId,
                        "recipientId" to msg.recipientId,
                        "text" to msg.text.take(4000),
                        "timestamp" to msg.timestamp,
                        "status" to msg.status.name,
                        "isIncoming" to msg.isIncoming,
                        "ttl" to msg.ttl,
                        "hopCount" to msg.hopCount,
                        "isBroadcast" to msg.isBroadcast,
                        "isSos" to msg.isSos,
                        "attachmentName" to (msg.attachmentName ?: "").take(120),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/messages/$docId")
            Result.failure(e)
        }
    }

    suspend fun fetchCloudBackupBundle(): Result<CloudFullBackupBundle> {
        val uid = currentUid() ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val friendsSnap = db.collection("users").document(uid).collection("friends").get().await()
            val convSnap = db.collection("users").document(uid).collection("conversations").get().await()
            val msgSnap = db.collection("users").document(uid).collection("messages").get().await()

            val friends = friendsSnap.toObjects(CloudFriendRecord::class.java).map { rec ->
                FriendEntity(
                    jesrId = rec.jesrId.ifBlank { rec.id },
                    displayName = rec.displayName,
                    publicKeyBase64 = rec.publicKeyBase64,
                    status = runCatching { FriendStatus.valueOf(rec.status) }.getOrDefault(FriendStatus.FRIEND),
                    addedAt = rec.addedAt,
                    isMuted = rec.isMuted,
                    isBlocked = rec.isBlocked,
                    unblockedAwaitingResume = rec.unblockedAwaitingResume,
                    localResumeApproved = rec.localResumeApproved,
                    peerResumeApproved = rec.peerResumeApproved,
                    distanceConsentEnabled = rec.distanceConsentEnabled
                )
            }
            val convs = convSnap.toObjects(CloudConversationRecord::class.java).map { rec ->
                ConversationEntity(
                    id = rec.convId.ifBlank { rec.id },
                    peerName = rec.peerName,
                    lastMessageText = rec.lastMessageText,
                    lastMessageTime = rec.lastMessageTime,
                    unreadCount = rec.unreadCount,
                    isOnline = false,
                    hopsDistance = rec.hopsDistance
                )
            }
            val msgs = msgSnap.toObjects(CloudMessageRecord::class.java).map { rec ->
                MessageEntity(
                    id = rec.messageId.ifBlank { rec.id },
                    conversationId = rec.conversationId,
                    senderId = rec.senderId,
                    recipientId = rec.recipientId,
                    text = rec.text,
                    timestamp = rec.timestamp,
                    status = runCatching { DeliveryStatus.valueOf(rec.status) }.getOrDefault(DeliveryStatus.DELIVERED),
                    isIncoming = rec.isIncoming,
                    ttl = rec.ttl,
                    hopCount = rec.hopCount,
                    isBroadcast = rec.isBroadcast,
                    isSos = rec.isSos,
                    attachmentName = rec.attachmentName.ifBlank { null }
                )
            }
            Result.success(CloudFullBackupBundle(friends = friends, conversations = convs, messages = msgs))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users/$uid")
            Result.failure(e)
        }
    }

    companion object {
        const val COLLECTION_ACCOUNTS = "accounts"
    }
}
