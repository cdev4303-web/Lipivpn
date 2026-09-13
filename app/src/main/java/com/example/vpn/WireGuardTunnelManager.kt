package com.example.vpn

import android.content.Context
import android.util.Log
import com.example.data.model.VpnServer
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Statistics
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages real WireGuard tunnel lifecycle using the official wireguard-android GoBackend.
 * Ensures that CONNECTED state is ONLY reported when the tunnel has performed a successful
 * handshake and validated live network routing with an external IP probe.
 */
class WireGuardTunnelManager(private val context: Context) {

    companion object {
        private const val TAG = "WireGuardTunnelManager"
        private const val TUNNEL_NAME = "FreeShieldWG"
    }

    private val backend: GoBackend by lazy {
        GoBackend(context.applicationContext)
    }

    private val tunnel: Tunnel = object : Tunnel {
        override fun getName(): String = TUNNEL_NAME

        override fun onStateChange(newState: Tunnel.State) {
            Log.i(TAG, "WireGuard tunnel onStateChange: $newState")
        }
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    private var activeJob: Job? = null
    private var statsJob: Job? = null
    private val isConnectingOrConnected = AtomicBoolean(false)

    /**
     * Starts the WireGuard tunnel and validates real traffic routing before declaring connection.
     */
    fun startTunnel(
        server: VpnServer,
        scope: CoroutineScope,
        onConnected: (connectedAt: Long, publicIp: String) -> Unit,
        onError: (errorMessage: String) -> Unit,
        onStatsUpdate: (rxBytes: Long, txBytes: Long) -> Unit
    ) {
        if (!server.isConfigured()) {
            onError("No VPN server configuration available. Please enter a valid host and WireGuard public/private keys.")
            return
        }

        stopTunnel()
        isConnectingOrConnected.set(true)

        activeJob = scope.launch(Dispatchers.IO) {
            try {
                Log.i(TAG, "Parsing WireGuard configuration for ${server.name} (${server.host}:${server.port})")
                val configString = server.toWgConfigString()
                val configStream = ByteArrayInputStream(configString.toByteArray(Charsets.UTF_8))
                val config = Config.parse(configStream)

                Log.i(TAG, "Bringing WireGuard GoBackend tunnel UP...")
                val tunnelState = backend.setState(tunnel, Tunnel.State.UP, config)
                if (tunnelState != Tunnel.State.UP) {
                    throw IllegalStateException("WireGuard backend returned state: $tunnelState")
                }

                // Verify tunnel connectivity with actual network probe
                Log.i(TAG, "Tunnel interface UP. Probing network connectivity and validating handshake...")
                val resolvedIp = probeTunnelConnectivity(maxAttempts = 5)

                if (resolvedIp == null) {
                    Log.e(TAG, "Connectivity probe failed: no response from server or DNS resolution failure")
                    // Tear down tunnel to prevent hanging disconnected state
                    try {
                        backend.setState(tunnel, Tunnel.State.DOWN, null)
                    } catch (_: Exception) {}

                    withContext(Dispatchers.Main) {
                        isConnectingOrConnected.set(false)
                        onError("VPN connection failed: Handshake timed out or unable to route traffic through ${server.host}:${server.port}. Please verify server status and keys.")
                    }
                    return@launch
                }

                val connectedAt = System.currentTimeMillis()
                Log.i(TAG, "VPN Tunnel verified successfully! Public IP: $resolvedIp")

                withContext(Dispatchers.Main) {
                    onConnected(connectedAt, resolvedIp)
                }

                // Start live traffic statistics polling
                startStatsPolling(scope, onStatsUpdate)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to establish WireGuard tunnel", e)
                try {
                    backend.setState(tunnel, Tunnel.State.DOWN, null)
                } catch (_: Exception) {}

                withContext(Dispatchers.Main) {
                    isConnectingOrConnected.set(false)
                    val msg = e.localizedMessage ?: "WireGuard initialization failed."
                    onError("VPN Error: $msg")
                }
            }
        }
    }

    /**
     * Polls external endpoints to confirm packets are actually traversing the VPN interface.
     */
    private suspend fun probeTunnelConnectivity(maxAttempts: Int): String? {
        val probeUrls = listOf(
            "https://api.ipify.org",
            "https://icanhazip.com",
            "https://ifconfig.me/ip"
        )

        // Wait brief moment for WireGuard handshake packets to exchange
        delay(1200)

        for (attempt in 1..maxAttempts) {
            for (url in probeUrls) {
                try {
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "FreeShieldVPN/1.0")
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val ip = response.body?.string()?.trim()
                            if (!ip.isNullOrBlank() && (ip.contains('.') || ip.contains(':'))) {
                                return ip
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Probe attempt $attempt on $url: ${e.message}")
                }
            }
            delay(1500)
        }
        return null
    }

    /**
     * Periodically queries the GoBackend for real RX / TX byte counts.
     */
    private fun startStatsPolling(
        scope: CoroutineScope,
        onStatsUpdate: (rxBytes: Long, txBytes: Long) -> Unit
    ) {
        statsJob?.cancel()
        statsJob = scope.launch(Dispatchers.IO) {
            while (isActive && isConnectingOrConnected.get()) {
                try {
                    val stats: Statistics = backend.getStatistics(tunnel)
                    val rx = stats.totalRx()
                    val tx = stats.totalTx()
                    withContext(Dispatchers.Main) {
                        onStatsUpdate(rx, tx)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error fetching WireGuard statistics: ${e.message}")
                }
                delay(1000)
            }
        }
    }

    /**
     * Tears down the WireGuard tunnel and cancels all polling coroutines.
     */
    fun stopTunnel() {
        isConnectingOrConnected.set(false)
        activeJob?.cancel()
        activeJob = null
        statsJob?.cancel()
        statsJob = null

        try {
            backend.setState(tunnel, Tunnel.State.DOWN, null)
            Log.i(TAG, "WireGuard tunnel brought DOWN cleanly.")
        } catch (e: Exception) {
            Log.w(TAG, "Exception bringing WireGuard tunnel DOWN: ${e.message}")
        }
    }

    fun isRunning(): Boolean {
        return try {
            backend.getState(tunnel) == Tunnel.State.UP
        } catch (_: Exception) {
            false
        }
    }
}
