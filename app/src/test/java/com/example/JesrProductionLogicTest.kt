package com.example

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.util.Base64
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.JesrCrypto
import com.example.data.AppDatabase
import com.example.data.entity.FriendEntity
import com.example.data.entity.MessageEntity
import com.example.data.model.DeliveryStatus
import com.example.data.model.FriendStatus
import com.example.data.model.MeshPacket
import com.example.data.model.MessageType
import com.example.data.model.TransportType
import com.example.network.model.PeerNode
import com.example.network.routing.MeshRouter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JesrProductionLogicTest {

    private lateinit var baseContext: Context
    private val openDbs = mutableListOf<AppDatabase>()

    @Before
    fun setUp() {
        baseContext = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        openDbs.forEach { runCatching { it.close() } }
        openDbs.clear()
    }

    private fun isolatedContext(tag: String): Context = object : ContextWrapper(baseContext) {
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
            return baseContext.getSharedPreferences("${tag}_$name", mode)
        }
    }

    private fun newInMemoryDb(): AppDatabase =
        Room.inMemoryDatabaseBuilder(baseContext, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            .also { openDbs.add(it) }

    @Test
    fun `ECDSA signature verification accepts authentic packet and rejects tampered payload or spoofed sender`() {
        val ctxA = isolatedContext("sig_a")
        val ctxB = isolatedContext("sig_b")
        val cryptoA = JesrCrypto(ctxA)
        val cryptoB = JesrCrypto(ctxB)

        val packet = MeshPacket(
            id = UUID.randomUUID().toString(),
            senderId = cryptoA.myJesrId,
            recipientId = cryptoB.myJesrId,
            type = MessageType.CHAT,
            payload = "ciphertext-payload",
            iv = "iv-123",
            ttl = 7
        ).signedWith(cryptoA)

        assertTrue(packet.hasValidSignature(cryptoA))
        assertTrue(packet.hasValidSignature(cryptoB))

        // Tamper with payload
        val tamperedPayload = packet.copy(payload = "forged-payload")
        assertFalse(tamperedPayload.hasValidSignature(cryptoB))

        // Spoof senderId using A's keys
        val spoofedSender = packet.copy(senderId = "JESR-FAKE-9999")
        assertFalse(spoofedSender.hasValidSignature(cryptoB))

        // Router drops packet with invalid signature before storing message
        runBlocking {
            val dbB = newInMemoryDb()
            val routerB = MeshRouter(
                getMyJesrId = { cryptoB.myJesrId },
                crypto = cryptoB,
                messageDao = dbB.messageDao(),
                conversationDao = dbB.conversationDao(),
                friendDao = dbB.friendDao(),
                relayQueueDao = dbB.relayQueueDao(),
                seenPacketDao = dbB.seenPacketDao(),
                sendViaBle = { _, _, cb -> cb(true) },
                sendViaWifi = { _, _, cb -> cb(true) },
                decryptPayload = { _ -> "should-never-decrypt" }
            )
            routerB.processIncomingPacketSuspend(tamperedPayload)
            assertNull(dbB.messageDao().getMessageById(packet.id))
            assertFalse(dbB.seenPacketDao().exists(packet.id))
        }
    }

    @Test
    fun `encrypted cloud identity export and import restores exact JISR ID, ECDH, and ECDSA keys`() {
        val srcCtx = isolatedContext("cloud_src")
        val dstCtx = isolatedContext("cloud_dst")
        val srcCrypto = JesrCrypto(srcCtx)
        val dstCrypto = JesrCrypto(dstCtx)

        val originalId = srcCrypto.myJesrId
        val originalEcdhPub = srcCrypto.myPublicKeyBase64
        val originalEcdsaPub = srcCrypto.mySigningPublicKeyBase64

        val secret = "firebase-uid-987654321"
        val bundle = srcCrypto.exportCloudIdentityBundle(secret)

        // Wrong secret fails cleanly
        assertFalse(dstCrypto.importCloudIdentityBundle(bundle, "wrong-uid"))

        // Correct secret restores identical keys & Jesr ID
        assertTrue(dstCrypto.importCloudIdentityBundle(bundle, secret))
        assertEquals(originalId, dstCrypto.myJesrId)
        assertEquals(originalEcdhPub, dstCrypto.myPublicKeyBase64)
        assertEquals(originalEcdsaPub, dstCrypto.mySigningPublicKeyBase64)

        // Restored signing key signs packets verified as original identity
        val packet = MeshPacket(
            senderId = dstCrypto.myJesrId,
            recipientId = "JESR-2222-3333",
            type = MessageType.CHAT,
            payload = "hello",
            iv = "iv"
        ).signedWith(dstCrypto)
        assertTrue(packet.hasValidSignature(srcCrypto))
    }

    @Test
    fun `persistent SeenPacketDao prevents duplicate replay across router restart`() = runBlocking {
        val ctxA = isolatedContext("replay_a")
        val ctxB = isolatedContext("replay_b")
        val cryptoA = JesrCrypto(ctxA)
        val cryptoB = JesrCrypto(ctxB)
        val dbB = newInMemoryDb()

        val peerPubA = cryptoB.parsePublicKey(cryptoA.myPublicKeyBase64)!!
        val msgId = UUID.randomUUID().toString()
        val ts = 1700000050000L
        val aad = "$msgId|${cryptoA.myJesrId}|${cryptoB.myJesrId}|${MessageType.CHAT.name}|$ts"
        val enc = cryptoA.encrypt("رسالة لا تتكرر", cryptoA.parsePublicKey(cryptoB.myPublicKeyBase64)!!, aad)
        val packet = MeshPacket(
            id = msgId,
            senderId = cryptoA.myJesrId,
            recipientId = cryptoB.myJesrId,
            type = MessageType.CHAT,
            payload = enc.ciphertextBase64,
            iv = enc.ivBase64,
            timestamp = ts
        ).signedWith(cryptoA)

        var deliveredCount = 0
        fun buildRouterB() = MeshRouter(
            getMyJesrId = { cryptoB.myJesrId },
            crypto = cryptoB,
            messageDao = dbB.messageDao(),
            conversationDao = dbB.conversationDao(),
            friendDao = dbB.friendDao(),
            relayQueueDao = dbB.relayQueueDao(),
            seenPacketDao = dbB.seenPacketDao(),
            sendViaBle = { _, _, cb -> cb(true) },
            sendViaWifi = { _, _, cb -> cb(true) },
            decryptPayload = { pkt ->
                cryptoB.decrypt(pkt.payload, pkt.iv, peerPubA, pkt.aadString())
            },
            onDecryptedMessageReceived = { deliveredCount++ }
        )

        // First router instance processes packet
        val routerB1 = buildRouterB()
        routerB1.processIncomingPacketSuspend(packet)
        assertEquals(1, deliveredCount)
        assertTrue(dbB.seenPacketDao().exists(msgId))

        // Fresh MeshRouter instance (simulating app restart with empty in-memory set) drops replay
        val routerB2 = buildRouterB()
        routerB2.processIncomingPacketSuspend(packet)
        assertEquals(1, deliveredCount)
    }

    @Test
    fun `real A to B to C to D multi-hop forwarding, hop tracking, and delivery ACK cleanup`() = runBlocking {
        val cryptoA = JesrCrypto(isolatedContext("hop_a"))
        val cryptoB = JesrCrypto(isolatedContext("hop_b"))
        val cryptoC = JesrCrypto(isolatedContext("hop_c"))
        val cryptoD = JesrCrypto(isolatedContext("hop_d"))

        val dbA = newInMemoryDb()
        val dbB = newInMemoryDb()
        val dbC = newInMemoryDb()
        val dbD = newInMemoryDb()

        val pubA = cryptoA.myPublicKeyBase64
        val pubD = cryptoD.myPublicKeyBase64

        val wireQueueB = mutableListOf<MeshPacket>()
        val wireQueueC = mutableListOf<MeshPacket>()
        val wireQueueD = mutableListOf<MeshPacket>()
        val wireQueueA = mutableListOf<MeshPacket>()

        val routerA = MeshRouter(
            getMyJesrId = { cryptoA.myJesrId },
            crypto = cryptoA,
            messageDao = dbA.messageDao(),
            conversationDao = dbA.conversationDao(),
            friendDao = dbA.friendDao(),
            relayQueueDao = dbA.relayQueueDao(),
            seenPacketDao = dbA.seenPacketDao(),
            sendViaBle = { peer, pkt, cb ->
                if (peer == cryptoB.myJesrId) wireQueueB.add(pkt)
                cb(true)
            },
            sendViaWifi = { _, _, cb -> cb(false) },
            decryptPayload = { null }
        )

        val routerB = MeshRouter(
            getMyJesrId = { cryptoB.myJesrId },
            crypto = cryptoB,
            messageDao = dbB.messageDao(),
            conversationDao = dbB.conversationDao(),
            friendDao = dbB.friendDao(),
            relayQueueDao = dbB.relayQueueDao(),
            seenPacketDao = dbB.seenPacketDao(),
            sendViaBle = { peer, pkt, cb ->
                when (peer) {
                    cryptoA.myJesrId -> wireQueueA.add(pkt)
                    cryptoC.myJesrId -> wireQueueC.add(pkt)
                }
                cb(true)
            },
            sendViaWifi = { _, _, cb -> cb(false) },
            decryptPayload = { null }
        )

        val routerC = MeshRouter(
            getMyJesrId = { cryptoC.myJesrId },
            crypto = cryptoC,
            messageDao = dbC.messageDao(),
            conversationDao = dbC.conversationDao(),
            friendDao = dbC.friendDao(),
            relayQueueDao = dbC.relayQueueDao(),
            seenPacketDao = dbC.seenPacketDao(),
            sendViaBle = { peer, pkt, cb ->
                when (peer) {
                    cryptoB.myJesrId -> wireQueueB.add(pkt)
                    cryptoD.myJesrId -> wireQueueD.add(pkt)
                }
                cb(true)
            },
            sendViaWifi = { _, _, cb -> cb(false) },
            decryptPayload = { null }
        )

        val routerD = MeshRouter(
            getMyJesrId = { cryptoD.myJesrId },
            crypto = cryptoD,
            messageDao = dbD.messageDao(),
            conversationDao = dbD.conversationDao(),
            friendDao = dbD.friendDao(),
            relayQueueDao = dbD.relayQueueDao(),
            seenPacketDao = dbD.seenPacketDao(),
            sendViaBle = { peer, pkt, cb ->
                if (peer == cryptoC.myJesrId) wireQueueC.add(pkt)
                cb(true)
            },
            sendViaWifi = { _, _, cb -> cb(false) },
            decryptPayload = { pkt ->
                cryptoD.decryptWithBase64Key(pkt.payload, pkt.iv, pubA, pkt.aadString())
            }
        )

        // Topology: A <-> B <-> C <-> D
        routerA.onNeighborDiscovered(PeerNode(cryptoB.myJesrId, "B", TransportType.BLE))
        routerB.onNeighborDiscovered(PeerNode(cryptoA.myJesrId, "A", TransportType.BLE))
        routerB.onNeighborDiscovered(PeerNode(cryptoC.myJesrId, "C", TransportType.BLE))
        routerC.onNeighborDiscovered(PeerNode(cryptoB.myJesrId, "B", TransportType.BLE))
        routerC.onNeighborDiscovered(PeerNode(cryptoD.myJesrId, "D", TransportType.BLE))
        routerD.onNeighborDiscovered(PeerNode(cryptoC.myJesrId, "C", TransportType.BLE))

        val msgId = UUID.randomUUID().toString()
        val ts = System.currentTimeMillis()
        val aad = "$msgId|${cryptoA.myJesrId}|${cryptoD.myJesrId}|${MessageType.CHAT.name}|$ts"
        val enc = cryptoA.encrypt("رسالة عبر 3 قفزات", cryptoA.parsePublicKey(pubD)!!, aad)

        dbA.messageDao().insertMessage(
            MessageEntity(
                id = msgId,
                conversationId = cryptoD.myJesrId,
                senderId = cryptoA.myJesrId,
                recipientId = cryptoD.myJesrId,
                text = "رسالة عبر 3 قفزات",
                timestamp = ts,
                status = DeliveryStatus.LOCAL_PENDING
            )
        )

        val initialPacket = MeshPacket(
            id = msgId,
            senderId = cryptoA.myJesrId,
            recipientId = cryptoD.myJesrId,
            type = MessageType.CHAT,
            payload = enc.ciphertextBase64,
            iv = enc.ivBase64,
            ttl = 7,
            hopCount = 0,
            timestamp = ts
        ).signedWith(cryptoA)

        // Pump A -> B -> C -> D and ACK_DELIVERY back D -> C -> B -> A
        routerA.routePacketSuspend(initialPacket)
        while (wireQueueB.isNotEmpty() || wireQueueC.isNotEmpty() || wireQueueD.isNotEmpty() || wireQueueA.isNotEmpty()) {
            val batchB = wireQueueB.toList().also { wireQueueB.clear() }
            batchB.forEach { routerB.processIncomingPacketSuspend(it) }

            val batchC = wireQueueC.toList().also { wireQueueC.clear() }
            batchC.forEach { routerC.processIncomingPacketSuspend(it) }

            val batchD = wireQueueD.toList().also { wireQueueD.clear() }
            batchD.forEach { routerD.processIncomingPacketSuspend(it) }

            val batchA = wireQueueA.toList().also { wireQueueA.clear() }
            batchA.forEach { routerA.processIncomingPacketSuspend(it) }
        }

        val deliveredAtD = dbD.messageDao().getMessageById(msgId)
        assertNotNull(deliveredAtD)
        assertEquals("رسالة عبر 3 قفزات", deliveredAtD?.text)
        assertEquals(2, deliveredAtD?.hopCount) // B (+1) -> C (+1) -> D receives hopCount=2 (3 links)
        assertTrue(routerB.relayedPacketsCount.value >= 1)
        assertTrue(routerC.relayedPacketsCount.value >= 1)

        // Delivery ACK must have reached A and cleaned up relay queue
        val senderMsg = dbA.messageDao().getMessageById(msgId)
        assertEquals(DeliveryStatus.DELIVERED, senderMsg?.status)
        assertEquals(0, dbA.relayQueueDao().getQueueCount().first())
    }

    @Test
    fun `store-and-forward holds queued packet until peer arrives and ACK_DELIVERY confirms receipt`() = runBlocking {
        val cryptoA = JesrCrypto(isolatedContext("sf_a"))
        val cryptoB = JesrCrypto(isolatedContext("sf_b"))
        val dbA = newInMemoryDb()
        val dispatchedToB = mutableListOf<MeshPacket>()

        val routerA = MeshRouter(
            getMyJesrId = { cryptoA.myJesrId },
            crypto = cryptoA,
            messageDao = dbA.messageDao(),
            conversationDao = dbA.conversationDao(),
            friendDao = dbA.friendDao(),
            relayQueueDao = dbA.relayQueueDao(),
            seenPacketDao = dbA.seenPacketDao(),
            sendViaBle = { peer, pkt, cb ->
                if (peer == cryptoB.myJesrId) dispatchedToB.add(pkt)
                cb(true)
            },
            sendViaWifi = { _, _, cb -> cb(false) },
            decryptPayload = { null }
        )

        val msgId = UUID.randomUUID().toString()
        val ts = System.currentTimeMillis()
        dbA.messageDao().insertMessage(
            MessageEntity(
                id = msgId,
                conversationId = cryptoB.myJesrId,
                senderId = cryptoA.myJesrId,
                recipientId = cryptoB.myJesrId,
                text = "بانتظار الاتصال",
                timestamp = ts,
                status = DeliveryStatus.LOCAL_PENDING
            )
        )

        val pkt = MeshPacket(
            id = msgId,
            senderId = cryptoA.myJesrId,
            recipientId = cryptoB.myJesrId,
            type = MessageType.CHAT,
            payload = "enc",
            iv = "iv",
            timestamp = ts
        ).signedWith(cryptoA)

        // No neighbors online -> Store-and-Forward queues packet
        routerA.routePacketSuspend(pkt)
        assertEquals(DeliveryStatus.QUEUED_RELAY, dbA.messageDao().getMessageById(msgId)?.status)
        assertEquals(1, dbA.relayQueueDao().getQueueCount().first())

        // Make queued packet immediately eligible and simulate B coming online
        dbA.relayQueueDao().incrementRetry(msgId, System.currentTimeMillis() - 1000L)
        routerA.pumpRelayQueueForPeerSuspend(cryptoB.myJesrId)
        assertEquals(1, dispatchedToB.size)

        // Queue entry MUST remain stored until ACK_DELIVERY is received!
        assertEquals(1, dbA.relayQueueDao().getQueueCount().first())
        assertEquals(DeliveryStatus.SENDING, dbA.messageDao().getMessageById(msgId)?.status)

        // B replies with signed ACK_DELIVERY -> queue entry removed and message marked DELIVERED
        val ack = MeshPacket(
            id = UUID.randomUUID().toString(),
            senderId = cryptoB.myJesrId,
            recipientId = cryptoA.myJesrId,
            type = MessageType.ACK_DELIVERY,
            payload = msgId,
            ttl = 7
        ).signedWith(cryptoB)
        routerA.processIncomingPacketSuspend(ack)
        assertEquals(0, dbA.relayQueueDao().getQueueCount().first())
        assertEquals(DeliveryStatus.DELIVERED, dbA.messageDao().getMessageById(msgId)?.status)
    }

    @Test
    fun `encrypted V2 image attachment decodes cleanly and saves under filesDir attachments`() = runBlocking {
        val cryptoA = JesrCrypto(isolatedContext("att_a"))
        val cryptoB = JesrCrypto(isolatedContext("att_b"))
        val dbB = newInMemoryDb()

        val sampleBmp = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        val bos = ByteArrayOutputStream()
        sampleBmp.compress(Bitmap.CompressFormat.JPEG, 70, bos)
        val rawB64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)

        val v2Envelope = JSONObject().apply {
            put("v", 2)
            put("text", "صورة من الشبكة\n📎 photo.jpg")
            put("attachmentName", "photo.jpg")
            put("attachmentDataBase64", rawB64)
        }.toString()

        val msgId = UUID.randomUUID().toString()
        val ts = System.currentTimeMillis()
        val aad = "$msgId|${cryptoA.myJesrId}|${cryptoB.myJesrId}|${MessageType.CHAT.name}|$ts"
        val enc = cryptoA.encrypt(v2Envelope, cryptoA.parsePublicKey(cryptoB.myPublicKeyBase64)!!, aad)

        val pkt = MeshPacket(
            id = msgId,
            senderId = cryptoA.myJesrId,
            recipientId = cryptoB.myJesrId,
            type = MessageType.CHAT,
            payload = enc.ciphertextBase64,
            iv = enc.ivBase64,
            timestamp = ts
        ).signedWith(cryptoA)

        val routerB = MeshRouter(
            getMyJesrId = { cryptoB.myJesrId },
            crypto = cryptoB,
            messageDao = dbB.messageDao(),
            conversationDao = dbB.conversationDao(),
            friendDao = dbB.friendDao(),
            relayQueueDao = dbB.relayQueueDao(),
            seenPacketDao = dbB.seenPacketDao(),
            sendViaBle = { _, _, cb -> cb(true) },
            sendViaWifi = { _, _, cb -> cb(true) },
            decryptPayload = { p -> cryptoB.decryptWithBase64Key(p.payload, p.iv, cryptoA.myPublicKeyBase64, p.aadString()) },
            saveReceivedAttachment = { id, name, dataB64 ->
                val bytes = Base64.decode(dataB64, Base64.NO_WRAP)
                val dir = File(baseContext.filesDir, "attachments_test").apply { mkdirs() }
                val out = File(dir, "${id}_$name")
                out.writeBytes(bytes)
                out.absolutePath
            }
        )

        routerB.processIncomingPacketSuspend(pkt)
        val saved = dbB.messageDao().getMessageById(msgId)
        assertNotNull(saved)
        assertEquals("photo.jpg", saved?.attachmentName)
        assertNotNull(saved?.attachmentUri)
        assertTrue(File(saved!!.attachmentUri!!).exists())
    }
}
