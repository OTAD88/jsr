package com.example.network.routing

import android.util.Log
import com.example.crypto.JesrCrypto
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
import com.example.data.model.MeshPacket
import com.example.data.model.MessageType
import com.example.data.model.TransportType
import com.example.network.model.PeerNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class RouteEntry(
    val destinationId: String,
    val nextHopPeerId: String,
    val hopDistance: Int,
    val transportType: TransportType,
    val expiresAt: Long
)

class MeshRouter(
    private val getMyJesrId: () -> String,
    private val crypto: JesrCrypto,
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val friendDao: FriendDao,
    private val relayQueueDao: RelayQueueDao,
    private val seenPacketDao: SeenPacketDao,
    private val isDistanceSharingForPeer: suspend (peerId: String) -> Boolean = { false },
    private val isFriendRequestBlocked: (senderId: String) -> Boolean = { false },
    private val isFriendRequestMuted: (senderId: String) -> Boolean = { false },
    private val sendViaBle: (targetPeerId: String, packet: MeshPacket, onResult: (Boolean) -> Unit) -> Unit,
    private val sendViaWifi: (targetPeerId: String?, packet: MeshPacket, onResult: (Boolean) -> Unit) -> Unit,
    private val decryptPayload: (packet: MeshPacket) -> String?,
    private val saveReceivedAttachment: suspend (messageId: String, fileName: String, base64Data: String) -> String? = { _, _, _ -> null },
    private val onIncomingFriendRequest: (senderId: String) -> Unit = {},
    private val onDecryptedMessageReceived: (message: MessageEntity) -> Unit = {}
) {
    private val TAG = "JesrMeshRouter"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val myJesrId: String get() = getMyJesrId()

    private val _neighbors = MutableStateFlow<Map<String, PeerNode>>(emptyMap())
    val neighbors = _neighbors.asStateFlow()

    private val routingTable = ConcurrentHashMap<String, RouteEntry>()

    private val processedMessageIds: MutableSet<String> =
        Collections.newSetFromMap(object : LinkedHashMap<String, Boolean>(2000, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean {
                return size > 2000
            }
        })

    private val notifiedRequestSenders = ConcurrentHashMap.newKeySet<String>()

    private val _relayedPacketsCount = MutableStateFlow(0)
    val relayedPacketsCount = _relayedPacketsCount.asStateFlow()

    init {
        startPeriodicMaintenance()
    }

    fun onNeighborDiscovered(peer: PeerNode) {
        if (peer.id == myJesrId || peer.id.isBlank()) return
        val existing = _neighbors.value[peer.id]
        val merged = peer.copy(
            publicKeyBase64 = peer.publicKeyBase64 ?: existing?.publicKeyBase64,
            signingPublicKeyBase64 = peer.signingPublicKeyBase64 ?: existing?.signingPublicKeyBase64,
            name = if (peer.name != peer.id) peer.name else (existing?.name ?: peer.name),
            nearbyVisible = peer.nearbyVisible || (existing?.nearbyVisible == true && peer.publicKeyBase64 == null)
        )
        val current = _neighbors.value.toMutableMap()
        current[peer.id] = merged
        _neighbors.value = current

        routingTable[peer.id] = RouteEntry(
            destinationId = peer.id,
            nextHopPeerId = peer.id,
            hopDistance = 1,
            transportType = peer.transportType,
            expiresAt = System.currentTimeMillis() + 120_000L
        )

        pumpRelayQueueForPeer(peer.id)
    }

    fun handleIncomingPacket(packet: MeshPacket) {
        scope.launch {
            processIncomingPacketSuspend(packet)
        }
    }

    suspend fun processIncomingPacketSuspend(packet: MeshPacket) {
        val now = System.currentTimeMillis()
        if (now > packet.expiresAt) {
            Log.d(TAG, "Dropping expired packet ${packet.id}")
            return
        }

        if (packet.senderId == myJesrId) {
            return
        }

        // Verify cryptographic ECDSA signature whenever present (and reject spoofed keys)
        if (packet.signature.isNotBlank() && !packet.hasValidSignature(crypto)) {
            Log.w(TAG, "Rejecting packet ${packet.id} from ${packet.senderId}: invalid ECDSA signature")
            return
        }

        val alreadySeenMem = synchronized(processedMessageIds) {
            if (processedMessageIds.contains(packet.id)) true
            else {
                processedMessageIds.add(packet.id)
                false
            }
        }
        if (alreadySeenMem || seenPacketDao.exists(packet.id)) {
            return
        }
        seenPacketDao.markSeen(SeenPacketEntity(packetId = packet.id, seenAt = now, expiresAt = packet.expiresAt))

        // Learn reverse route toward sender through previousHopId
        val hopVia = packet.previousHopId.ifBlank { packet.senderId }
        val existingRoute = routingTable[packet.senderId]
        val candidateHopDist = packet.hopCount + 1
        if (existingRoute == null || existingRoute.expiresAt < now || candidateHopDist <= existingRoute.hopDistance) {
            routingTable[packet.senderId] = RouteEntry(
                destinationId = packet.senderId,
                nextHopPeerId = hopVia,
                hopDistance = candidateHopDist,
                transportType = _neighbors.value[hopVia]?.transportType ?: TransportType.BLE,
                expiresAt = now + 120_000L
            )
        }

        if (packet.type == MessageType.CHAT || packet.type == MessageType.EMERGENCY_SOS) {
            sendHopAck(packet)
        }

        if (packet.recipientId == myJesrId || packet.recipientId == "BROADCAST") {
            processLocallySuspend(packet)
        } else {
            forwardMultiHopSuspend(packet)
        }
    }

    private suspend fun sendHopAck(packet: MeshPacket) {
        val target = packet.previousHopId.ifBlank { packet.senderId }
        if (target == myJesrId) return
        val hopAck = MeshPacket(
            id = UUID.randomUUID().toString(),
            senderId = myJesrId,
            recipientId = target,
            type = MessageType.ACK_HOP,
            payload = packet.id,
            ttl = 1,
            hopCount = 0
        ).signedWith(crypto)
        dispatchDirect(target, hopAck)
    }

    private suspend fun processLocallySuspend(packet: MeshPacket) {
        when (packet.type) {
            MessageType.CHAT, MessageType.EMERGENCY_SOS -> {
                val existingFriend = friendDao.getFriend(packet.senderId)
                if (existingFriend != null &&
                    (existingFriend.isBlocked || existingFriend.unblockedAwaitingResume)
                ) {
                    return
                }

                // Require end-to-end encryption for all 1-to-1 private chats
                val rawPlaintext: String
                if (packet.recipientId != "BROADCAST") {
                    if (packet.iv.isBlank()) {
                        Log.w(TAG, "Rejecting unencrypted private chat packet ${packet.id} from ${packet.senderId}")
                        return
                    }
                    val decrypted = decryptPayload(packet)
                    if (decrypted == null) {
                        Log.w(TAG, "Failed E2EE decryption / authentication tag check for packet ${packet.id}")
                        return
                    }
                    rawPlaintext = decrypted
                } else {
                    rawPlaintext = packet.payload
                }

                // Parse optional E2EE V2 attachment envelope
                var finalDisplayText = rawPlaintext
                var savedAttachmentUri: String? = null
                var receivedAttachmentName: String? = null
                if (rawPlaintext.startsWith("{")) {
                    runCatching {
                        val obj = JSONObject(rawPlaintext)
                        if (obj.optInt("v", 0) == 2) {
                            finalDisplayText = obj.optString("text", "")
                            receivedAttachmentName = obj.optString("attachmentName", "").ifBlank { null }
                            val b64 = obj.optString("attachmentDataBase64", "").ifBlank { null }
                            if (receivedAttachmentName != null && b64 != null) {
                                savedAttachmentUri = saveReceivedAttachment(packet.id, receivedAttachmentName!!, b64)
                            }
                        }
                    }
                }

                // Send signed end-to-end delivery ACK back to original sender
                val deliveryAck = MeshPacket(
                    id = UUID.randomUUID().toString(),
                    senderId = myJesrId,
                    recipientId = packet.senderId,
                    type = MessageType.ACK_DELIVERY,
                    payload = packet.id,
                    ttl = 7
                ).signedWith(crypto)
                routePacketSuspend(deliveryAck)

                val isSos = packet.type == MessageType.EMERGENCY_SOS
                val convId = if (packet.recipientId == "BROADCAST") "BROADCAST" else packet.senderId

                if (packet.senderId != myJesrId && packet.distanceConsent) {
                    friendDao.updateDistanceConsent(packet.senderId, true)
                }

                val senderFriend = friendDao.getFriend(packet.senderId)
                val remoteConsented = packet.distanceConsent || (senderFriend?.distanceConsentEnabled == true)
                val localConsented = isDistanceSharingForPeer(packet.senderId)
                val isMutual = remoteConsented && localConsented

                val neighborPeer = _neighbors.value[packet.previousHopId] ?: _neighbors.value[packet.senderId]
                val rssi = neighborPeer?.rssi ?: (if (packet.senderRssi != 0) packet.senderRssi else -68)
                val distMeters = if (isMutual && packet.hopCount <= 1) {
                    com.example.network.DistanceEstimator.estimateMeters(rssi)
                } else null

                val existingConv = conversationDao.getConversationById(convId)
                val peerName = if (convId == "BROADCAST") {
                    "قناة البث العام (SOS)"
                } else {
                    senderFriend?.displayName ?: packet.senderId
                }

                if (existingConv == null) {
                    conversationDao.upsertConversation(
                        ConversationEntity(
                            id = convId,
                            peerName = peerName,
                            lastMessageText = finalDisplayText,
                            lastMessageTime = packet.timestamp,
                            unreadCount = if (senderFriend?.isMuted == true) 0 else 1,
                            isOnline = true,
                            hopsDistance = packet.hopCount
                        )
                    )
                } else {
                    conversationDao.updateLastMessage(convId, finalDisplayText, packet.timestamp)
                }

                val entity = MessageEntity(
                    id = packet.id,
                    conversationId = convId,
                    senderId = packet.senderId,
                    recipientId = packet.recipientId,
                    text = finalDisplayText,
                    timestamp = packet.timestamp,
                    status = DeliveryStatus.DELIVERED,
                    isIncoming = true,
                    ttl = packet.ttl,
                    hopCount = packet.hopCount,
                    isBroadcast = packet.recipientId == "BROADCAST",
                    isSos = isSos,
                    estimatedDistanceMeters = distMeters,
                    isDistanceConsented = isMutual,
                    attachmentUri = savedAttachmentUri,
                    attachmentName = receivedAttachmentName
                )
                messageDao.insertMessage(entity)
                if (senderFriend?.isMuted != true) {
                    onDecryptedMessageReceived(entity)
                }
            }

            MessageType.ACK_HOP -> {
                val originalMessageId = packet.payload
                messageDao.updateMessageStatus(originalMessageId, DeliveryStatus.SENDING)
            }

            MessageType.ACK_DELIVERY -> {
                val originalMessageId = packet.payload
                messageDao.updateMessageStatus(originalMessageId, DeliveryStatus.DELIVERED)
                relayQueueDao.remove(originalMessageId)
            }

            MessageType.DISTANCE_CONSENT_REQUEST -> {
                val peerConsented = packet.distanceConsent || packet.payload == "true"
                friendDao.updateDistanceConsent(packet.senderId, peerConsented)

                val localAllowed = isDistanceSharingForPeer(packet.senderId)
                val reply = MeshPacket(
                    id = UUID.randomUUID().toString(),
                    senderId = myJesrId,
                    recipientId = packet.senderId,
                    type = MessageType.DISTANCE_CONSENT_UPDATE,
                    payload = localAllowed.toString(),
                    distanceConsent = localAllowed,
                    ttl = 2
                ).signedWith(crypto)
                routePacketSuspend(reply)
            }

            MessageType.DISTANCE_CONSENT_UPDATE -> {
                val peerConsented = packet.distanceConsent || packet.payload == "true"
                friendDao.updateDistanceConsent(packet.senderId, peerConsented)
            }

            MessageType.FRIEND_REQUEST -> {
                if (isFriendRequestBlocked(packet.senderId)) {
                    return
                }

                val existing = friendDao.getFriend(packet.senderId)
                if (existing != null && existing.status == FriendStatus.FRIEND) {
                    return
                }

                val isAlreadyReceived = existing?.status == FriendStatus.REQUEST_RECEIVED
                val neighborName = _neighbors.value[packet.senderId]?.name
                val senderPubKey = packet.payload.ifBlank { packet.senderPublicKey }
                friendDao.upsertFriend(
                    FriendEntity(
                        jesrId = packet.senderId,
                        displayName = existing?.displayName ?: neighborName ?: "مستخدم جسر (${packet.senderId})",
                        publicKeyBase64 = senderPubKey,
                        status = FriendStatus.REQUEST_RECEIVED,
                        addedAt = existing?.addedAt ?: System.currentTimeMillis(),
                        distanceConsentEnabled = packet.distanceConsent,
                        muteRequestsOverride = existing?.muteRequestsOverride,
                        blockRequestsOverride = existing?.blockRequestsOverride
                    )
                )

                if (!isFriendRequestMuted(packet.senderId) &&
                    !isAlreadyReceived &&
                    notifiedRequestSenders.add(packet.senderId)
                ) {
                    onIncomingFriendRequest(packet.senderId)
                }
            }

            MessageType.FRIEND_ACCEPT -> {
                val existing = friendDao.getFriend(packet.senderId)
                val pubKey = packet.payload.ifBlank { packet.senderPublicKey }.ifBlank { existing?.publicKeyBase64 ?: "" }
                friendDao.upsertFriend(
                    (existing ?: FriendEntity(jesrId = packet.senderId, displayName = packet.senderId)).copy(
                        publicKeyBase64 = pubKey,
                        status = FriendStatus.FRIEND,
                        addedAt = System.currentTimeMillis(),
                        distanceConsentEnabled = packet.distanceConsent || (existing?.distanceConsentEnabled == true),
                        isBlocked = false,
                        unblockedAwaitingResume = false
                    )
                )
                val name = existing?.displayName ?: packet.senderId
                conversationDao.upsertConversation(
                    ConversationEntity(
                        id = packet.senderId,
                        peerName = name,
                        lastMessageText = "تم قبول طلب الصداقة، التواصل جاهز عبر Mesh",
                        lastMessageTime = System.currentTimeMillis()
                    )
                )
            }

            MessageType.FRIEND_REMOVE -> {
                messageDao.deleteMessagesForConversation(packet.senderId)
                conversationDao.deleteConversation(packet.senderId)
                friendDao.deleteFriend(packet.senderId)
            }

            MessageType.RESUME_APPROVAL -> {
                val existing = friendDao.getFriend(packet.senderId) ?: return
                if (existing.isBlocked) return
                val bothApproved = existing.localResumeApproved
                friendDao.updateBlockAndResumeState(
                    jesrId = packet.senderId,
                    blocked = false,
                    awaitingResume = !bothApproved,
                    localApproved = existing.localResumeApproved,
                    peerApproved = true
                )
            }

            else -> {}
        }
    }

    private suspend fun forwardMultiHopSuspend(packet: MeshPacket) {
        if (packet.ttl <= 1) {
            Log.d(TAG, "TTL expired for packet ${packet.id}, dropping")
            return
        }

        val forwardedPacket = packet.copy(
            ttl = packet.ttl - 1,
            hopCount = packet.hopCount + 1,
            previousHopId = myJesrId
        )

        _relayedPacketsCount.value += 1
        routePacketSuspend(forwardedPacket)
    }

    fun routePacket(packet: MeshPacket) {
        scope.launch {
            routePacketSuspend(packet)
        }
    }

    suspend fun routePacketSuspend(packet: MeshPacket) {
        val now = System.currentTimeMillis()
        if (now > packet.expiresAt) return

        // Mark self-originated packet as seen so other relays echoing it don't loop back
        if (packet.senderId == myJesrId) {
            synchronized(processedMessageIds) {
                processedMessageIds.add(packet.id)
            }
            seenPacketDao.markSeen(SeenPacketEntity(packetId = packet.id, seenAt = now, expiresAt = packet.expiresAt))
        }

        val direct = _neighbors.value[packet.recipientId]
        val route = routingTable[packet.recipientId]
        val validRoute = route != null && route.expiresAt > now && _neighbors.value.containsKey(route.nextHopPeerId)

        if (packet.recipientId == "BROADCAST") {
            _neighbors.value.keys
                .filter { it != packet.previousHopId && it != packet.senderId }
                .forEach { neighborId ->
                    dispatchDirect(neighborId, packet)
                }
        } else if (direct != null) {
            dispatchDirect(direct.id, packet)
            enqueueUnackedRelayIfImportant(packet, now)
        } else if (validRoute && route != null) {
            dispatchDirect(route.nextHopPeerId, packet)
            enqueueUnackedRelayIfImportant(packet, now)
        } else {
            // True multi-hop controlled flooding: forward to all neighboring relays except sender/prevHop
            val relayCandidates = _neighbors.value.keys.filter {
                it != packet.previousHopId && it != packet.senderId
            }
            if (relayCandidates.isNotEmpty() && packet.ttl > 0) {
                relayCandidates.forEach { relayId ->
                    dispatchDirect(relayId, packet)
                }
                if (packet.senderId == myJesrId || packet.type == MessageType.CHAT || packet.type == MessageType.EMERGENCY_SOS) {
                    enqueueUnackedRelayIfImportant(packet, now)
                    messageDao.updateMessageStatus(packet.id, DeliveryStatus.SENDING)
                }
            } else {
                Log.d(TAG, "No neighbors/route to ${packet.recipientId}. Enqueueing in Store-and-Forward.")
                enqueueUnackedRelayIfImportant(packet, now, queueStatus = DeliveryStatus.QUEUED_RELAY)
            }
        }
    }

    private suspend fun enqueueUnackedRelayIfImportant(
        packet: MeshPacket,
        now: Long,
        queueStatus: DeliveryStatus? = null
    ) {
        if (packet.type == MessageType.ACK_HOP || packet.type == MessageType.ACK_DELIVERY) return
        relayQueueDao.enqueue(
            RelayQueueEntity(
                messageId = packet.id,
                targetPeerId = packet.recipientId,
                packetJson = packet.toJson(),
                createdAt = now,
                nextAttemptTime = now + 10_000L,
                retryCount = 0,
                maxRetries = 12
            )
        )
        if (queueStatus != null) {
            messageDao.updateMessageStatus(packet.id, queueStatus)
        }
    }

    private fun dispatchDirect(peerId: String, packet: MeshPacket) {
        val neighbor = _neighbors.value[peerId]
        if (neighbor?.transportType == TransportType.WIFI_DIRECT) {
            sendViaWifi(peerId, packet) { success ->
                if (!success) {
                    sendViaBle(peerId, packet) {}
                }
            }
        } else {
            sendViaBle(peerId, packet) { success ->
                if (!success) {
                    sendViaWifi(peerId, packet) {}
                }
            }
        }
    }

    fun pumpRelayQueueForPeer(peerId: String) {
        scope.launch {
            pumpRelayQueueForPeerSuspend(peerId)
        }
    }

    suspend fun pumpRelayQueueForPeerSuspend(peerId: String) {
        val now = System.currentTimeMillis()
        val readyPackets = relayQueueDao.getReadyPackets(now)
        for (queued in readyPackets) {
            val packet = MeshPacket.fromJson(queued.packetJson) ?: continue
            if (now > packet.expiresAt) {
                relayQueueDao.remove(queued.messageId)
                messageDao.updateMessageStatus(queued.messageId, DeliveryStatus.FAILED)
                continue
            }
            val shouldForwardToPeer = queued.targetPeerId == peerId ||
                queued.targetPeerId == "BROADCAST" ||
                (queued.targetPeerId != myJesrId && peerId != packet.senderId && peerId != packet.previousHopId)

            if (shouldForwardToPeer) {
                dispatchDirect(peerId, packet)
                // Do NOT remove until ACK_DELIVERY confirms receipt! Update retry backoff instead.
                val nextAttempt = now + 15_000L
                relayQueueDao.incrementRetry(queued.messageId, nextAttempt)
                messageDao.updateMessageStatus(queued.messageId, DeliveryStatus.SENDING)
            }
        }
    }

    private fun startPeriodicMaintenance() {
        scope.launch {
            while (isActive) {
                delay(15_000L)
                val now = System.currentTimeMillis()

                // Remove stale neighbors and expired routes
                val cutoff = now - 90_000L
                val current = _neighbors.value.toMutableMap()
                val removed = current.filter { it.value.lastSeen < cutoff }.keys
                if (removed.isNotEmpty()) {
                    removed.forEach { current.remove(it) }
                    _neighbors.value = current
                }
                routingTable.entries.removeIf { it.value.expiresAt < now }
                runCatching { seenPacketDao.pruneExpired(now) }

                try {
                    val ready = relayQueueDao.getReadyPackets(now)
                    for (queued in ready) {
                        val packet = MeshPacket.fromJson(queued.packetJson)
                        if (packet == null || now > packet.expiresAt || queued.retryCount >= queued.maxRetries) {
                            relayQueueDao.remove(queued.messageId)
                            messageDao.updateMessageStatus(queued.messageId, DeliveryStatus.FAILED)
                        } else {
                            val nextAttempt = now + (10_000L * (1 shl queued.retryCount.coerceAtMost(6)))
                            relayQueueDao.incrementRetry(queued.messageId, nextAttempt)
                            routePacketSuspend(packet)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing relay queue maintenance", e)
                }
            }
        }
    }
}
