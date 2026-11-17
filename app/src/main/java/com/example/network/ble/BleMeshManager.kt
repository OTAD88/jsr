package com.example.network.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.example.data.model.MeshPacket
import com.example.data.model.TransportType
import com.example.network.model.PeerNode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BleMeshManager(
    private val context: Context,
    private val getMyJesrId: () -> String,
    private val getMetadataJson: () -> String,
    private val onPacketReceived: (MeshPacket) -> Unit,
    private val onPeerDiscovered: (PeerNode) -> Unit
) {
    private val TAG = "JesrBleMesh"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("0000FE55-0000-1000-8000-00805F9B34FB")
        val RX_CHAR_UUID: UUID = UUID.fromString("0000FE56-0000-1000-8000-00805F9B34FB") // Write packets
        val TX_CHAR_UUID: UUID = UUID.fromString("0000FE57-0000-1000-8000-00805F9B34FB") // Read node identity & keys
        private const val MAX_RX_BUFFER_BYTES = 512 * 1024
        private const val GATT_OP_TIMEOUT_MS = 12_000L
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null
    private var gattServer: BluetoothGattServer? = null

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising = _isAdvertising.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val stateMutex = Mutex()
    private val gattClientMutex = Mutex()

    private val discoveredDevices = ConcurrentHashMap<String, BluetoothDevice>()
    private val metadataFetchedForAddress = ConcurrentHashMap<String, Long>()
    private val connectedGatts = ConcurrentHashMap<String, BluetoothGatt>()
    private val incomingPacketBuffers = ConcurrentHashMap<String, StringBuilder>()

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            _isAdvertising.value = true
            Log.d(TAG, "BLE Mesh Advertising started for ${getMyJesrId()}")
        }

        override fun onStartFailure(errorCode: Int) {
            _isAdvertising.value = false
            Log.w(TAG, "BLE Mesh Advertising failed with code: $errorCode")
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            val device = result.device ?: return
            val record = result.scanRecord ?: return

            val serviceData = record.getServiceData(ParcelUuid(SERVICE_UUID))
            val advertisedId = if (serviceData != null && serviceData.isNotEmpty()) {
                String(serviceData, StandardCharsets.UTF_8).trim()
            } else {
                record.deviceName ?: runCatching { device.name }.getOrNull() ?: ""
            }.trim()

            if (advertisedId.startsWith("JESR-") && advertisedId != getMyJesrId()) {
                discoveredDevices[advertisedId] = device
                val peer = PeerNode(
                    id = advertisedId,
                    name = advertisedId,
                    transportType = TransportType.BLE,
                    rssi = result.rssi,
                    bleAddress = device.address,
                    lastSeen = System.currentTimeMillis(),
                    nearbyVisible = false // Until authenticated metadata confirms nearbyVisible
                )
                onPeerDiscovered(peer)

                // Read peer's ECDH/ECDSA public keys & nearby visibility over TX_CHAR_UUID (throttled)
                val lastFetched = metadataFetchedForAddress[device.address] ?: 0L
                if (System.currentTimeMillis() - lastFetched > 60_000L) {
                    metadataFetchedForAddress[device.address] = System.currentTimeMillis()
                    fetchPeerMetadataOverGatt(device, result.rssi)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            Log.w(TAG, "BLE Scan failed with error code: $errorCode")
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            if (device != null && newState == BluetoothProfile.STATE_DISCONNECTED) {
                incomingPacketBuffers.remove(device.address)
            }
            Log.d(TAG, "GATT Server state: device=${device?.address}, status=$status, newState=$newState")
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            if (device == null) return
            if (characteristic?.uuid == TX_CHAR_UUID) {
                val fullBytes = getMetadataJson().toByteArray(StandardCharsets.UTF_8)
                val slice = if (offset >= 0 && offset < fullBytes.size) {
                    fullBytes.copyOfRange(offset, fullBytes.size)
                } else {
                    ByteArray(0)
                }
                try {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, slice)
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending GATT read response", e)
                }
            } else {
                try {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
                } catch (_: Exception) {}
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            if (responseNeeded && device != null) {
                try {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending GATT write response", e)
                }
            }

            if (value != null && device != null) {
                val chunk = String(value, StandardCharsets.UTF_8)
                val buffer = incomingPacketBuffers.getOrPut(device.address) { StringBuilder() }

                synchronized(buffer) {
                    if (buffer.length + chunk.length > MAX_RX_BUFFER_BYTES) {
                        buffer.setLength(0)
                        Log.w(TAG, "BLE RX buffer overflow guard triggered for ${device.address}")
                        return
                    }
                    buffer.append(chunk)
                    while (true) {
                        val endIdx = buffer.indexOf("\n__END__\n")
                        if (endIdx < 0) break
                        val packetJson = buffer.substring(0, endIdx)
                        buffer.delete(0, endIdx + "\n__END__\n".length)
                        val packet = MeshPacket.fromJson(packetJson)
                        if (packet != null) {
                            onPacketReceived(packet)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startMesh() {
        scope.launch {
            stateMutex.withLock {
                val adapter = bluetoothAdapter
                if (adapter == null || !adapter.isEnabled) {
                    Log.w(TAG, "Bluetooth is disabled or not available")
                    return@withLock
                }

                try {
                    if (gattServer == null && bluetoothManager != null) {
                        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
                        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

                        val rxChar = BluetoothGattCharacteristic(
                            RX_CHAR_UUID,
                            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                            BluetoothGattCharacteristic.PERMISSION_WRITE
                        )
                        val txChar = BluetoothGattCharacteristic(
                            TX_CHAR_UUID,
                            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                            BluetoothGattCharacteristic.PERMISSION_READ
                        )

                        service.addCharacteristic(rxChar)
                        service.addCharacteristic(txChar)
                        gattServer?.addService(service)
                    }

                    if (advertiser == null) {
                        advertiser = adapter.bluetoothLeAdvertiser
                    }

                    if (!_isAdvertising.value && advertiser != null) {
                        val settings = AdvertiseSettings.Builder()
                            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                            .setConnectable(true)
                            .setTimeout(0)
                            .build()

                        val pUuid = ParcelUuid(SERVICE_UUID)
                        // Split across Adv (UUID, 18B) + ScanResponse (ServiceData ID, 18B) to stay <= 31B each
                        val advData = AdvertiseData.Builder()
                            .setIncludeDeviceName(false)
                            .addServiceUuid(pUuid)
                            .build()
                        val scanResponse = AdvertiseData.Builder()
                            .setIncludeDeviceName(false)
                            .addServiceData(pUuid, getMyJesrId().toByteArray(StandardCharsets.UTF_8))
                            .build()

                        advertiser?.startAdvertising(settings, advData, scanResponse, advertiseCallback)
                    }

                    if (scanner == null) {
                        scanner = adapter.bluetoothLeScanner
                    }

                    if (!_isScanning.value && scanner != null) {
                        val filters = listOf(
                            ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()
                        )
                        val scanSettings = ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .build()

                        scanner?.startScan(filters, scanSettings, scanCallback)
                        _isScanning.value = true
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "Missing Bluetooth permissions", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Exception starting BLE mesh", e)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopMesh() {
        scope.launch {
            stateMutex.withLock {
                try {
                    if (_isScanning.value && scanner != null) {
                        scanner?.stopScan(scanCallback)
                        _isScanning.value = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping scan", e)
                }

                try {
                    if (_isAdvertising.value && advertiser != null) {
                        advertiser?.stopAdvertising(advertiseCallback)
                        _isAdvertising.value = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping advertising", e)
                }

                try {
                    gattServer?.close()
                    gattServer = null
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing gatt server", e)
                }

                connectedGatts.values.forEach {
                    runCatching {
                        it.disconnect()
                        it.close()
                    }
                }
                connectedGatts.clear()
                incomingPacketBuffers.clear()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchPeerMetadataOverGatt(device: BluetoothDevice, rssi: Int) {
        scope.launch {
            gattClientMutex.withLock {
                val done = CompletableDeferred<Unit>()
                var activeGatt: BluetoothGatt? = null
                try {
                    activeGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
                        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                            connectedGatts[device.address] = gatt
                            if (newState == BluetoothProfile.STATE_CONNECTED) {
                                try {
                                    gatt.requestMtu(512)
                                } catch (_: Exception) {
                                    gatt.discoverServices()
                                }
                            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                connectedGatts.remove(device.address)
                                runCatching { gatt.close() }
                                done.complete(Unit)
                            }
                        }

                        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                            gatt.discoverServices()
                        }

                        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                val txChar = gatt.getService(SERVICE_UUID)?.getCharacteristic(TX_CHAR_UUID)
                                if (txChar != null && gatt.readCharacteristic(txChar)) {
                                    return
                                }
                            }
                            runCatching { gatt.disconnect() }
                            done.complete(Unit)
                        }

                        @Suppress("DEPRECATION")
                        override fun onCharacteristicRead(
                            gatt: BluetoothGatt,
                            characteristic: BluetoothGattCharacteristic,
                            status: Int
                        ) {
                            handleReadBytes(gatt, characteristic.value, status)
                        }

                        override fun onCharacteristicRead(
                            gatt: BluetoothGatt,
                            characteristic: BluetoothGattCharacteristic,
                            value: ByteArray,
                            status: Int
                        ) {
                            handleReadBytes(gatt, value, status)
                        }

                        private fun handleReadBytes(gatt: BluetoothGatt, bytes: ByteArray?, status: Int) {
                            if (status == BluetoothGatt.GATT_SUCCESS && bytes != null && bytes.isNotEmpty()) {
                                runCatching {
                                    val json = JSONObject(String(bytes, StandardCharsets.UTF_8))
                                    val peerId = json.optString("id").trim()
                                    if (peerId.startsWith("JESR-") && peerId != getMyJesrId()) {
                                        discoveredDevices[peerId] = device
                                        onPeerDiscovered(
                                            PeerNode(
                                                id = peerId,
                                                name = json.optString("name", peerId),
                                                transportType = TransportType.BLE,
                                                rssi = rssi,
                                                bleAddress = device.address,
                                                publicKeyBase64 = json.optString("pubKey", "").ifBlank { null },
                                                signingPublicKeyBase64 = json.optString("sigKey", "").ifBlank { null },
                                                lastSeen = System.currentTimeMillis(),
                                                nearbyVisible = json.optBoolean("nearbyVisible", false)
                                            )
                                        )
                                    }
                                }
                            }
                            runCatching { gatt.disconnect() }
                            done.complete(Unit)
                        }
                    }, BluetoothDevice.TRANSPORT_LE)
                    withTimeoutOrNull(GATT_OP_TIMEOUT_MS) { done.await() }
                } catch (_: Exception) {
                    done.complete(Unit)
                } finally {
                    activeGatt?.let { runCatching { it.close() } }
                }
            }
        }
    }

    /**
     * Sends a packet to a target peer over BLE GATT client with mutex serialization & timeout.
     */
    @SuppressLint("MissingPermission")
    fun sendPacket(targetPeerId: String, packet: MeshPacket, onComplete: (Boolean) -> Unit) {
        scope.launch {
            val device = discoveredDevices[targetPeerId]
            if (device == null) {
                onComplete(false)
                return@launch
            }

            gattClientMutex.withLock {
                val result = CompletableDeferred<Boolean>()
                var activeGatt: BluetoothGatt? = null
                try {
                    val packetString = packet.toJson() + "\n__END__\n"
                    val payloadBytes = packetString.toByteArray(StandardCharsets.UTF_8)

                    activeGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
                        private var offset = 0
                        private var chunkSize = 180

                        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                            connectedGatts[device.address] = gatt
                            if (newState == BluetoothProfile.STATE_CONNECTED) {
                                try {
                                    gatt.requestMtu(512)
                                } catch (_: Exception) {
                                    gatt.discoverServices()
                                }
                            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                connectedGatts.remove(device.address)
                                runCatching { gatt.close() }
                                result.complete(false)
                            }
                        }

                        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                            if (status == BluetoothGatt.GATT_SUCCESS && mtu > 23) {
                                chunkSize = (mtu - 12).coerceIn(20, 480)
                            }
                            gatt.discoverServices()
                        }

                        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                val service = gatt.getService(SERVICE_UUID)
                                val rxChar = service?.getCharacteristic(RX_CHAR_UUID)
                                if (rxChar != null) {
                                    offset = 0
                                    writeNext(gatt, rxChar)
                                } else {
                                    result.complete(false)
                                    runCatching { gatt.disconnect() }
                                }
                            } else {
                                result.complete(false)
                                runCatching { gatt.disconnect() }
                            }
                        }

                        @Suppress("DEPRECATION")
                        private fun writeNext(gatt: BluetoothGatt, rxChar: BluetoothGattCharacteristic) {
                            if (offset < payloadBytes.size) {
                                val length = minOf(chunkSize, payloadBytes.size - offset)
                                val chunk = ByteArray(length)
                                System.arraycopy(payloadBytes, offset, chunk, 0, length)
                                offset += length

                                rxChar.value = chunk
                                rxChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                                val initiated = gatt.writeCharacteristic(rxChar)
                                if (!initiated) {
                                    result.complete(false)
                                    runCatching { gatt.disconnect() }
                                }
                            } else {
                                result.complete(true)
                                runCatching { gatt.disconnect() }
                            }
                        }

                        override fun onCharacteristicWrite(
                            gatt: BluetoothGatt,
                            characteristic: BluetoothGattCharacteristic,
                            status: Int
                        ) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                writeNext(gatt, characteristic)
                            } else {
                                result.complete(false)
                                runCatching { gatt.disconnect() }
                            }
                        }
                    }, BluetoothDevice.TRANSPORT_LE)

                    val ok = withTimeoutOrNull(GATT_OP_TIMEOUT_MS) { result.await() } ?: false
                    onComplete(ok)
                } catch (e: Exception) {
                    Log.e(TAG, "Exception sending BLE packet to $targetPeerId", e)
                    onComplete(false)
                } finally {
                    activeGatt?.let { runCatching { it.close() } }
                }
            }
        }
    }
}
