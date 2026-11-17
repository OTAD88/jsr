package com.example.network.wifip2p

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import android.util.Log
import com.example.data.model.MeshPacket
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
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

class WifiDirectMeshManager(
    private val context: Context,
    private val getMyJesrId: () -> String,
    private val getMetadataJson: () -> String,
    private val onPacketReceived: (MeshPacket) -> Unit,
    private val onPeerDiscovered: (PeerNode) -> Unit
) {
    private val TAG = "JesrWifiDirect"
    private val PORT = 8888
    private val HELLO_PREFIX = "__JESR_HELLO__:"
    private val MAX_FRAME_BYTES = 4 * 1024 * 1024
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val p2pManager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: WifiP2pManager.Channel? = null

    private val _isWifiP2pEnabled = MutableStateFlow(false)
    val isWifiP2pEnabled = _isWifiP2pEnabled.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _isGroupOwner = MutableStateFlow(false)
    val isGroupOwner = _isGroupOwner.asStateFlow()

    private val activeDiscoveredDevices = ConcurrentHashMap<String, WifiP2pDevice>()
    private val peerSocketsByJesrId = ConcurrentHashMap<String, Socket>()
    private val activeClientSockets = ConcurrentHashMap<String, Socket>()
    private var serverSocket: ServerSocket? = null
    private var clientSocketToGo: Socket? = null

    private var isReceiverRegistered = false
    @Volatile
    private var lastConnectAttemptMs = 0L

    private val p2pReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(c: Context?, intent: Intent?) {
            intent ?: return
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    _isWifiP2pEnabled.value = (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
                    Log.d(TAG, "Wi-Fi P2P State Changed: enabled=${_isWifiP2pEnabled.value}")
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    val ch = channel ?: return
                    p2pManager?.requestPeers(ch) { peers: WifiP2pDeviceList? ->
                        peers?.deviceList?.forEach { device ->
                            handleDiscoveredDevice(device)
                        }
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val ch = channel ?: return
                    val networkInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO, NetworkInfo::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                    }

                    if (networkInfo?.isConnected == true) {
                        p2pManager?.requestConnectionInfo(ch) { info: WifiP2pInfo? ->
                            handleConnectionEstablished(info)
                        }
                    } else {
                        handleDisconnected()
                    }
                }
            }
        }
    }

    init {
        initChannel()
    }

    private fun initChannel() {
        if (channel == null && p2pManager != null) {
            channel = p2pManager.initialize(context, Looper.getMainLooper()) {
                Log.w(TAG, "Wi-Fi Direct channel was lost, reinitializing...")
                channel = null
                initChannel()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        val ch = channel ?: return
        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter().apply {
                addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            }
            context.registerReceiver(p2pReceiver, intentFilter)
            isReceiverRegistered = true
        }
        discoverPeersWithBackoff(retry = 0)
    }

    @SuppressLint("MissingPermission")
    private fun discoverPeersWithBackoff(retry: Int) {
        val ch = channel ?: return
        p2pManager?.stopPeerDiscovery(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = launchDiscoverPeers(ch, retry)
            override fun onFailure(reason: Int) = launchDiscoverPeers(ch, retry)
        })
    }

    @SuppressLint("MissingPermission")
    private fun launchDiscoverPeers(ch: WifiP2pManager.Channel, retry: Int) {
        p2pManager?.discoverPeers(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Wi-Fi Direct discoverPeers started")
            }

            override fun onFailure(reason: Int) {
                Log.w(TAG, "discoverPeers failed: reason=$reason (BUSY=2)")
                if (reason == WifiP2pManager.BUSY && retry < 3) {
                    scope.launch {
                        delay(1000L * (retry + 1))
                        discoverPeersWithBackoff(retry + 1)
                    }
                }
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun connectToPeerDevice(deviceAddress: String) {
        val ch = channel ?: return
        val device = activeDiscoveredDevices[deviceAddress] ?: return
        val now = System.currentTimeMillis()
        if (_isConnected.value || now - lastConnectAttemptMs < 15_000L) return
        lastConnectAttemptMs = now

        val config = WifiP2pConfig().apply {
            this.deviceAddress = device.deviceAddress
        }
        p2pManager?.connect(ch, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Initiated Wi-Fi P2P connect to ${device.deviceName}")
            }

            override fun onFailure(reason: Int) {
                Log.w(TAG, "Wi-Fi P2P connect failed with reason=$reason")
            }
        })
    }

    private fun handleDiscoveredDevice(device: WifiP2pDevice) {
        activeDiscoveredDevices[device.deviceAddress] = device
        // Only register as a direct JISR peer if device name exposes genuine JISR ID;
        // real JISR identity & key exchange completes over HELLO socket handshake after P2P connect.
        if (device.deviceName.startsWith("JESR-") && device.deviceName != getMyJesrId()) {
            onPeerDiscovered(
                PeerNode(
                    id = device.deviceName,
                    name = device.deviceName,
                    transportType = TransportType.WIFI_DIRECT,
                    lastSeen = System.currentTimeMillis(),
                    nearbyVisible = false
                )
            )
            if (!_isConnected.value) {
                connectToPeerDevice(device.deviceAddress)
            }
        }
    }

    private fun handleConnectionEstablished(info: WifiP2pInfo?) {
        info ?: return
        _isConnected.value = true
        _isGroupOwner.value = info.isGroupOwner

        if (info.groupFormed) {
            if (info.isGroupOwner) {
                startServerSocket()
            } else {
                val goHost = info.groupOwnerAddress?.hostAddress ?: "192.168.49.1"
                connectToGroupOwner(goHost)
            }
        }
    }

    private fun startServerSocket() {
        scope.launch {
            try {
                serverSocket?.close()
                val server = ServerSocket(PORT)
                serverSocket = server
                Log.d(TAG, "Wi-Fi Direct ServerSocket listening on port $PORT")

                while (isActive && !server.isClosed) {
                    val client = server.accept()
                    val remoteAddr = client.inetAddress.hostAddress ?: "unknown"
                    activeClientSockets[remoteAddr] = client
                    sendHelloFrame(client)
                    startSocketReader(client, remoteAddr)
                }
            } catch (e: Exception) {
                if (isActive) Log.e(TAG, "ServerSocket error", e)
            }
        }
    }

    private fun connectToGroupOwner(host: String) {
        scope.launch {
            try {
                clientSocketToGo?.close()
                val socket = Socket()
                withContext(Dispatchers.IO) {
                    socket.connect(InetSocketAddress(host, PORT), 5000)
                }
                clientSocketToGo = socket
                Log.d(TAG, "Connected to Group Owner at $host:$PORT")
                sendHelloFrame(socket)
                startSocketReader(socket, host)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to Group Owner at $host", e)
            }
        }
    }

    private fun sendHelloFrame(socket: Socket) {
        runCatching {
            val frameBytes = (HELLO_PREFIX + getMetadataJson()).toByteArray(StandardCharsets.UTF_8)
            synchronized(socket) {
                val out = DataOutputStream(socket.getOutputStream())
                out.writeInt(frameBytes.size)
                out.write(frameBytes)
                out.flush()
            }
        }
    }

    private fun startSocketReader(socket: Socket, identifier: String) {
        scope.launch {
            var boundJesrId: String? = null
            try {
                val input = DataInputStream(socket.getInputStream())
                while (isActive && socket.isConnected && !socket.isClosed) {
                    val length = input.readInt()
                    if (length <= 0 || length > MAX_FRAME_BYTES) break
                    val buffer = ByteArray(length)
                    input.readFully(buffer)
                    val frameStr = String(buffer, StandardCharsets.UTF_8)
                    if (frameStr.startsWith(HELLO_PREFIX)) {
                        runCatching {
                            val json = JSONObject(frameStr.removePrefix(HELLO_PREFIX))
                            val peerId = json.optString("id").trim()
                            if (peerId.startsWith("JESR-") && peerId != getMyJesrId()) {
                                boundJesrId = peerId
                                peerSocketsByJesrId[peerId] = socket
                                onPeerDiscovered(
                                    PeerNode(
                                        id = peerId,
                                        name = json.optString("name", peerId),
                                        transportType = TransportType.WIFI_DIRECT,
                                        ipAddress = socket.inetAddress?.hostAddress,
                                        port = PORT,
                                        publicKeyBase64 = json.optString("pubKey", "").ifBlank { null },
                                        signingPublicKeyBase64 = json.optString("sigKey", "").ifBlank { null },
                                        lastSeen = System.currentTimeMillis(),
                                        nearbyVisible = json.optBoolean("nearbyVisible", false)
                                    )
                                )
                            }
                        }
                    } else {
                        val packet = MeshPacket.fromJson(frameStr)
                        if (packet != null) {
                            onPacketReceived(packet)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Socket reader closed for $identifier: ${e.message}")
            } finally {
                boundJesrId?.let { peerSocketsByJesrId.remove(it, socket) }
                activeClientSockets.remove(identifier)
                runCatching { socket.close() }
            }
        }
    }

    fun sendPacket(packet: MeshPacket, onComplete: (Boolean) -> Unit) {
        sendPacket(targetPeerId = packet.recipientId, packet = packet, onComplete = onComplete)
    }

    fun sendPacket(targetPeerId: String?, packet: MeshPacket, onComplete: (Boolean) -> Unit) {
        scope.launch {
            var sent = false
            val jsonBytes = packet.toJson().toByteArray(StandardCharsets.UTF_8)
            val targetedSocket = targetPeerId?.let { peerSocketsByJesrId[it] }

            if (targetedSocket != null && targetedSocket.isConnected && !targetedSocket.isClosed) {
                sent = writeFrame(targetedSocket, jsonBytes)
            }

            if (!sent) {
                val toGo = clientSocketToGo
                if (toGo != null && toGo.isConnected && !toGo.isClosed) {
                    if (writeFrame(toGo, jsonBytes)) sent = true
                }
                activeClientSockets.values.forEach { client ->
                    if (client.isConnected && !client.isClosed) {
                        if (writeFrame(client, jsonBytes)) sent = true
                    }
                }
            }

            onComplete(sent)
        }
    }

    private fun writeFrame(socket: Socket, bytes: ByteArray): Boolean {
        return try {
            synchronized(socket) {
                val out = DataOutputStream(socket.getOutputStream())
                out.writeInt(bytes.size)
                out.write(bytes)
                out.flush()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error writing socket frame", e)
            false
        }
    }

    private fun handleDisconnected() {
        _isConnected.value = false
        _isGroupOwner.value = false
        runCatching { serverSocket?.close() }
        serverSocket = null
        activeClientSockets.values.forEach { runCatching { it.close() } }
        activeClientSockets.clear()
        peerSocketsByJesrId.clear()
        runCatching { clientSocketToGo?.close() }
        clientSocketToGo = null
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        val ch = channel ?: return
        if (isReceiverRegistered) {
            runCatching { context.unregisterReceiver(p2pReceiver) }
            isReceiverRegistered = false
        }
        try {
            p2pManager?.stopPeerDiscovery(ch, null)
            p2pManager?.removeGroup(ch, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Wi-Fi Direct", e)
        }
        handleDisconnected()
    }
}
