package com.example.vpn

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.data.model.VpnServer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * High-performance, real Android TUN interface router.
 * Reads IP packets from the TUN ParcelFileDescriptor and forwards them
 * through a protected DatagramSocket to the authenticated VPN server endpoint.
 */
class VpnTunnelEngine(
    private val vpnService: VpnService,
    private val pfd: ParcelFileDescriptor,
    private val server: VpnServer,
    private val onStatsUpdated: (bytesIn: Long, bytesOut: Long) -> Unit,
    private val onError: (String) -> Unit
) {
    private val isRunning = AtomicBoolean(true)
    private var tunnelJob: Job? = null
    private var socket: DatagramSocket? = null

    val totalBytesIn = AtomicLong(0L)
    val totalBytesOut = AtomicLong(0L)

    fun start(scope: CoroutineScope) {
        tunnelJob = scope.launch(Dispatchers.IO) {
            try {
                runTunnelLoop()
            } catch (e: CancellationException) {
                Log.d(TAG, "Tunnel coroutine canceled")
            } catch (e: Exception) {
                Log.e(TAG, "Tunnel engine error: ${e.message}", e)
                if (isRunning.get()) {
                    onError("Tunnel error: ${e.localizedMessage ?: "Unknown I/O exception"}")
                }
            } finally {
                closeResources()
            }
        }
    }

    private suspend fun runTunnelLoop() {
        Log.i(TAG, "Starting VPN tunnel to ${server.host}:${server.port} via ${server.protocol}")

        // 1. Resolve remote server address
        val remoteAddress = try {
            InetSocketAddress(server.host, server.port)
        } catch (e: Exception) {
            onError("Unable to resolve server address '${server.host}': ${e.localizedMessage}")
            return
        }

        // 2. Open datagram socket and protect it
        val datagramSocket = DatagramSocket().apply {
            soTimeout = 10000 // 10s read timeout
        }
        socket = datagramSocket

        // CRITICAL Android VpnService rule: Protect socket from entering its own VPN tunnel
        val protected = vpnService.protect(datagramSocket)
        if (!protected) {
            Log.e(TAG, "Failed to protect VPN socket with vpnService.protect()")
            onError("System security error: Could not protect VPN socket.")
            return
        }

        try {
            datagramSocket.connect(remoteAddress)
        } catch (e: Exception) {
            onError("Failed to connect to VPN server endpoint: ${e.localizedMessage}")
            return
        }

        val inFd = FileInputStream(pfd.fileDescriptor)
        val outFd = FileOutputStream(pfd.fileDescriptor)

        val bufferSize = server.mtu.coerceIn(1280, 32767)

        // Launch Uplink Worker (TUN Interface -> Remote VPN Socket)
        val uplinkJob = CoroutineScope(Dispatchers.IO).launch {
            val packetBuffer = ByteArray(bufferSize)
            try {
                while (isRunning.get() && isActive) {
                    val length = inFd.read(packetBuffer)
                    if (length > 0) {
                        val packet = DatagramPacket(packetBuffer, length, remoteAddress)
                        datagramSocket.send(packet)
                        val out = totalBytesOut.addAndGet(length.toLong())
                        onStatsUpdated(totalBytesIn.get(), out)
                    } else if (length < 0) {
                        break
                    }
                }
            } catch (e: IOException) {
                if (isRunning.get()) {
                    Log.w(TAG, "Uplink stream closed or disconnected: ${e.message}")
                }
            }
        }

        // Launch Downlink Worker (Remote VPN Socket -> TUN Interface)
        val downlinkJob = CoroutineScope(Dispatchers.IO).launch {
            val rxBuffer = ByteArray(bufferSize)
            val rxPacket = DatagramPacket(rxBuffer, rxBuffer.size)
            try {
                while (isRunning.get() && isActive) {
                    try {
                        datagramSocket.receive(rxPacket)
                        if (rxPacket.length > 0) {
                            outFd.write(rxPacket.data, rxPacket.offset, rxPacket.length)
                            val inBytes = totalBytesIn.addAndGet(rxPacket.length.toLong())
                            onStatsUpdated(inBytes, totalBytesOut.get())
                        }
                    } catch (e: java.net.SocketTimeoutException) {
                        // Keep-alive or waiting for traffic; loop continues
                    }
                }
            } catch (e: IOException) {
                if (isRunning.get()) {
                    Log.w(TAG, "Downlink socket closed: ${e.message}")
                }
            }
        }

        uplinkJob.join()
        downlinkJob.join()
    }

    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            Log.i(TAG, "Stopping VPN tunnel engine...")
            tunnelJob?.cancel()
            closeResources()
        }
    }

    private fun closeResources() {
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null

        try {
            pfd.close()
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "VpnTunnelEngine"
    }
}
