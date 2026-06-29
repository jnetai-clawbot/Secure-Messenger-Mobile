package com.jnetaol.securemessenger.network

import com.jnetaol.securemessenger.data.model.AppSettings
import com.jnetaol.securemessenger.logger.DebugLogger
import kotlinx.coroutines.*
import java.io.*
import java.net.*
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.security.MessageDigest
import java.util.Random
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class P2PConnectionInfo(
    val localAddress: String,
    val localPort: Int,
    val publicAddress: String,
    val publicPort: Int,
    val natType: String = "unknown"
)

data class PeerInfo(
    val peerId: String,
    val address: String,
    val port: Int,
    val isIPv6: Boolean = false
)

class P2PManager(
    private val settings: AppSettings,
    private val onMessageReceived: (String, ByteArray) -> Unit,
    private val onConnectionEstablished: (String) -> Unit,
    private val onConnectionFailed: (String, String) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    private var udpChannel: DatagramChannel? = null
    private val activeConnections = mutableMapOf<String, Socket>()
    private val activeUdpSessions = mutableMapOf<String, InetSocketAddress>()
    private var localConnectionInfo: P2PConnectionInfo? = null
    private val random = Random()

    init {
        DebugLogger.i("P2PManager", "init", "SM-P2P-001", "P2PManager initialized")
    }

    fun start() {
        scope.launch {
            try {
                startTCPServer()
                startUDPListener()
                discoverPublicAddress()
                DebugLogger.i("P2PManager", "start", "SM-P2P-002", "P2P services started")
            } catch (e: Exception) {
                DebugLogger.e("P2PManager", "start", "SM-P2P-ERR-001", "Failed to start P2P", e)
            }
        }
    }

    fun stop() {
        scope.launch {
            try {
                serverSocket?.close()
                udpChannel?.close()
                activeConnections.values.forEach { try { it.close() } catch (_: Exception) {} }
                activeConnections.clear()
                activeUdpSessions.clear()
                DebugLogger.i("P2PManager", "stop", "SM-P2P-003", "P2P services stopped")
            } catch (e: Exception) {
                DebugLogger.e("P2PManager", "stop", "SM-P2P-ERR-002", "Failed to stop P2P", e)
            }
        }
    }

    private suspend fun startTCPServer() = withContext(Dispatchers.IO) {
        try {
            val port = if (settings.localPort > 0) settings.localPort else findAvailablePort()
            serverSocket = if (settings.enableIPv6) {
                ServerSocket(port, 50, Inet6Address.getByName("::"))
            } else {
                ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))
            }
            serverSocket?.reuseAddress = true
            DebugLogger.i("P2PManager", "startTCPServer", "SM-P2P-004", "TCP server on port $port")

            while (serverSocket != null && !serverSocket!!.isClosed) {
                try {
                    val client = serverSocket!!.accept()
                    scope.launch { handleTCPClient(client) }
                } catch (e: SocketException) {
                    if (serverSocket?.isClosed == true) break
                    DebugLogger.w("P2PManager", "startTCPServer", "SM-P2P-WARN-001", "Socket accept interrupted")
                }
            }
        } catch (e: Exception) {
            DebugLogger.e("P2PManager", "startTCPServer", "SM-P2P-ERR-003", "TCP server error", e)
        }
    }

    private suspend fun startUDPListener() = withContext(Dispatchers.IO) {
        if (!settings.useUDP) return@withContext
        try {
            val port = if (settings.localPort > 0) settings.localPort + 1 else findAvailablePort()
            udpChannel = DatagramChannel.open().apply {
                configureBlocking(false)
                val addr = if (settings.enableIPv6) {
                    InetSocketAddress("::", port)
                } else {
                    InetSocketAddress("0.0.0.0", port)
                }
                socket().bind(addr)
                socket().reuseAddress = true
            }
            DebugLogger.i("P2PManager", "startUDPListener", "SM-P2P-005", "UDP listener on port $port")

            val buffer = ByteBuffer.allocate(65536)
            while (udpChannel != null && udpChannel!!.isOpen) {
                buffer.clear()
                val source = udpChannel!!.receive(buffer) as? InetSocketAddress
                if (source != null) {
                    buffer.flip()
                    val data = ByteArray(buffer.remaining())
                    buffer.get(data)
                    scope.launch { handleUDPPacket(source, data) }
                } else {
                    delay(50)
                }
            }
        } catch (e: Exception) {
            DebugLogger.e("P2PManager", "startUDPListener", "SM-P2P-ERR-004", "UDP listener error", e)
        }
    }

    private suspend fun handleTCPClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

            val handshake = reader.readLine() ?: return@withContext
            if (handshake.startsWith("SM_P2P_HELLO:")) {
                val peerId = handshake.removePrefix("SM_P2P_HELLO:")
                activeConnections[peerId] = socket
                writer.write("SM_P2P_ACK\n")
                writer.flush()
                onConnectionEstablished(peerId)
                DebugLogger.i("P2PManager", "handleTCPClient", "SM-P2P-006", "Peer connected: $peerId")

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val msg = line ?: break
                    if (msg.startsWith("SM_MSG:") || msg.startsWith("SM_PAIR|")) {
                        val payload = if (msg.startsWith("SM_MSG:")) msg.removePrefix("SM_MSG:") else msg
                        onMessageReceived(peerId, payload.toByteArray(Charsets.UTF_8))
                    } else if (msg == "SM_P2P_BYE") {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            DebugLogger.w("P2PManager", "handleTCPClient", "SM-P2P-WARN-002", "TCP client disconnected: ${e.message}")
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private suspend fun handleUDPPacket(source: InetSocketAddress, data: ByteArray) {
        try {
            val msg = String(data, Charsets.UTF_8)
            if (msg.startsWith("SM_P2P_HELLO:")) {
                val peerId = msg.removePrefix("SM_P2P_HELLO:")
                activeUdpSessions[peerId] = source
                sendUDP(source, "SM_P2P_ACK".toByteArray(Charsets.UTF_8))
                onConnectionEstablished(peerId)
                DebugLogger.i("P2PManager", "handleUDPPacket", "SM-P2P-007", "UDP peer connected: $peerId")
            } else if (msg.startsWith("SM_MSG:")) {
                val payload = msg.removePrefix("SM_MSG:")
                val peerId = activeUdpSessions.entries.find { it.value == source }?.key ?: "unknown"
                onMessageReceived(peerId, payload.toByteArray(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            DebugLogger.w("P2PManager", "handleUDPPacket", "SM-P2P-WARN-003", "UDP packet error: ${e.message}")
        }
    }

    suspend fun connectToPeer(peerId: String, address: String, port: Int): Boolean {
        return try {
            connectTCP(peerId, address, port)
        } catch (e: Exception) {
            DebugLogger.e("P2PManager", "connectToPeer", "SM-P2P-ERR-005", "Connection failed", e)
            onConnectionFailed(peerId, e.message ?: "Unknown error")
            false
        }
    }

    private suspend fun connectTCP(peerId: String, address: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(address, port), 10000)
            activeConnections[peerId] = socket

            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
            writer.write("SM_P2P_HELLO:$peerId\n")
            writer.flush()

            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            socket.soTimeout = 0
            val response = reader.readLine()
            if (response == "SM_P2P_ACK") {
                onConnectionEstablished(peerId)
                DebugLogger.i("P2PManager", "connectTCP", "SM-P2P-008", "TCP connected to $peerId")
                scope.launch {
                    try {
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val msg = line ?: break
                            if (msg.startsWith("SM_MSG:") || msg.startsWith("SM_PAIR|")) {
                                val payload = if (msg.startsWith("SM_MSG:")) msg.removePrefix("SM_MSG:") else msg
                                onMessageReceived(peerId, payload.toByteArray(Charsets.UTF_8))
                            } else if (msg == "SM_P2P_BYE") break
                        }
                    } catch (_: Exception) {}
                    try { socket.close() } catch (_: Exception) {}
                    activeConnections.remove(peerId)
                }
                true
            } else {
                socket.close()
                activeConnections.remove(peerId)
                onConnectionFailed(peerId, "Handshake failed")
                false
            }
        } catch (e: Exception) {
            DebugLogger.e("P2PManager", "connectTCP", "SM-P2P-ERR-006", "TCP connect failed", e)
            activeConnections.remove(peerId)
            onConnectionFailed(peerId, e.message ?: "Connection failed")
            false
        }
    }

    private suspend fun connectUDP(peerId: String, address: String, port: Int) = withContext(Dispatchers.IO) {
        try {
            val target = InetSocketAddress(address, port)
            activeUdpSessions[peerId] = target
            sendUDP(target, "SM_P2P_HELLO:$peerId".toByteArray(Charsets.UTF_8))
            DebugLogger.i("P2PManager", "connectUDP", "SM-P2P-009", "UDP hello sent to $peerId")
        } catch (e: Exception) {
            DebugLogger.e("P2PManager", "connectUDP", "SM-P2P-ERR-007", "UDP connect failed", e)
            onConnectionFailed(peerId, e.message ?: "Connection failed")
        }
    }

    fun sendMessage(peerId: String, data: ByteArray) {
        scope.launch {
            try {
                val tcpSocket = activeConnections[peerId]
                if (tcpSocket != null && tcpSocket.isConnected) {
                    val writer = BufferedWriter(OutputStreamWriter(tcpSocket.getOutputStream()))
                    writer.write("SM_MSG:${String(data, Charsets.UTF_8)}\n")
                    writer.flush()
                    DebugLogger.d("P2PManager", "sendMessage", "SM-P2P-010", "TCP message sent to $peerId")
                    return@launch
                }

                val udpAddr = activeUdpSessions[peerId]
                if (udpAddr != null) {
                    sendUDP(udpAddr, "SM_MSG:".toByteArray(Charsets.UTF_8) + data)
                    DebugLogger.d("P2PManager", "sendMessage", "SM-P2P-011", "UDP message sent to $peerId")
                    return@launch
                }

                DebugLogger.w("P2PManager", "sendMessage", "SM-P2P-WARN-004", "No active connection for $peerId")
            } catch (e: Exception) {
                DebugLogger.e("P2PManager", "sendMessage", "SM-P2P-ERR-008", "Send failed", e)
            }
        }
    }

    fun disconnectPeer(peerId: String) {
        scope.launch {
            try {
                activeConnections[peerId]?.let { socket ->
                    try {
                        val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                        writer.write("SM_P2P_BYE\n")
                        writer.flush()
                    } catch (_: Exception) {}
                    try { socket.close() } catch (_: Exception) {}
                }
                activeConnections.remove(peerId)
                activeUdpSessions.remove(peerId)
                DebugLogger.i("P2PManager", "disconnectPeer", "SM-P2P-012", "Disconnected $peerId")
            } catch (e: Exception) {
                DebugLogger.e("P2PManager", "disconnectPeer", "SM-P2P-ERR-009", "Disconnect failed", e)
            }
        }
    }

    private suspend fun sendUDP(target: InetSocketAddress, data: ByteArray) = withContext(Dispatchers.IO) {
        try {
            val channel = DatagramChannel.open()
            channel.configureBlocking(true)
            channel.socket().reuseAddress = true
            channel.send(ByteBuffer.wrap(data), target)
            channel.close()
        } catch (e: Exception) {
            DebugLogger.e("P2PManager", "sendUDP", "SM-P2P-ERR-010", "UDP send failed", e)
        }
    }

    private suspend fun discoverPublicAddress() = withContext(Dispatchers.IO) {
        try {
            val stunServer = settings.stunServer1.removePrefix("stun:")
            val hostPort = stunServer.split(":")
            val host = hostPort[0]
            val port = hostPort.getOrElse(1) { "19302" }.toInt()

            val address = InetAddress.getByName(host)
            val channel = DatagramChannel.open()
            channel.configureBlocking(true)
            channel.socket().soTimeout = 5000

            val request = ByteArray(20)
            request[0] = 0x00.toByte()
            request[1] = 0x01.toByte()
            for (i in 4..19) request[i] = random.nextInt(256).toByte()

            channel.send(ByteBuffer.wrap(request), InetSocketAddress(address, port))

            val responseBuffer = ByteBuffer.allocate(32)
            val source = channel.receive(responseBuffer) as? InetSocketAddress
            channel.close()

            if (source != null) {
                responseBuffer.flip()
                val response = ByteArray(responseBuffer.remaining())
                responseBuffer.get(response)

                if (response.size >= 28 && response[0] == 0x01.toByte() && response[1] == 0x01.toByte()) {
                    val mappedPort = ((response[26].toInt() and 0xFF) shl 8) or (response[27].toInt() and 0xFF)
                    val mappedIp = "${response[28].toInt() and 0xFF}.${response[29].toInt() and 0xFF}.${response[30].toInt() and 0xFF}.${response[31].toInt() and 0xFF}"

                    val localPort = serverSocket?.localPort ?: 0
                    val localAddr = InetAddress.getLocalHost()?.hostAddress ?: "127.0.0.1"

                    localConnectionInfo = P2PConnectionInfo(
                        localAddress = localAddr,
                        localPort = localPort,
                        publicAddress = mappedIp,
                        publicPort = mappedPort,
                        natType = if (mappedIp == localAddr) "open" else "nat"
                    )
                    DebugLogger.i("P2PManager", "discoverPublicAddress", "SM-P2P-013",
                        "Public: $mappedIp:$mappedPort, Local: $localAddr:$localPort")
                }
            }
        } catch (e: Exception) {
            DebugLogger.w("P2PManager", "discoverPublicAddress", "SM-P2P-WARN-005",
                "STUN discovery failed: ${e.message}")
        }
    }

    fun getConnectionInfo(): P2PConnectionInfo? = localConnectionInfo

    fun getLocalPort(): Int = serverSocket?.localPort ?: 0

    private fun findAvailablePort(): Int {
        return try {
            val socket = ServerSocket(0)
            val port = socket.localPort
            socket.close()
            port
        } catch (e: Exception) {
            12345 + random.nextInt(10000)
        }
    }

    fun destroy() {
        stop()
        scope.cancel()
    }
}
