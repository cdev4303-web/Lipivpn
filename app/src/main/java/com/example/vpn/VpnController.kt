package com.example.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import com.example.data.model.VpnServer
import com.example.data.model.VpnState
import com.example.data.model.VpnStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Singleton state controller managing the VPN service lifecycle,
 * state broadcasts, live bandwidth metrics, and public IP resolution.
 */
object VpnController {
    private const val TAG = "VpnController"

    private val _vpnState = MutableStateFlow<VpnState>(VpnState.Disconnected)
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    private val _vpnStats = MutableStateFlow(VpnStats())
    val vpnStats: StateFlow<VpnStats> = _vpnStats.asStateFlow()

    private val _selectedServer = MutableStateFlow<VpnServer?>(null)
    val selectedServer: StateFlow<VpnServer?> = _selectedServer.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var durationTickerJob: Job? = null
    private var ipLookupJob: Job? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    fun setSelectedServer(server: VpnServer?) {
        _selectedServer.value = server
    }

    fun isPermissionRequired(context: Context): Boolean {
        return VpnService.prepare(context) != null
    }

    fun getPrepareIntent(context: Context): Intent? {
        return VpnService.prepare(context)
    }

    /**
     * Initiates VPN connection to the given server.
     * Enforces strict validation: will never report "Connected" on placeholder/unconfigured servers.
     */
    fun startVpn(context: Context, server: VpnServer, killSwitch: Boolean = false, autoReconnect: Boolean = true) {
        if (_vpnState.value.isConnecting || _vpnState.value.isConnected) {
            Log.w(TAG, "Connection already active or in progress. Preventing duplicate connection.")
            return
        }

        // Validate server configuration strictly per requirements
        if (!server.isConfigured()) {
            _vpnState.value = VpnState.Error("Server '${server.name}' is an unconfigured placeholder. Please configure a valid server IP/host or use the test endpoint.")
            return
        }

        _vpnState.value = VpnState.Connecting
        _selectedServer.value = server
        _vpnStats.value = VpnStats()

        val intent = Intent(context, FreeShieldVpnService::class.java).apply {
            action = FreeShieldVpnService.ACTION_CONNECT
            putExtra(FreeShieldVpnService.EXTRA_SERVER_ID, server.id)
            putExtra(FreeShieldVpnService.EXTRA_SERVER_NAME, server.name)
            putExtra(FreeShieldVpnService.EXTRA_SERVER_HOST, server.host)
            putExtra(FreeShieldVpnService.EXTRA_SERVER_PORT, server.port)
            putExtra(FreeShieldVpnService.EXTRA_SERVER_PROTOCOL, server.protocol)
            putExtra(FreeShieldVpnService.EXTRA_SERVER_PUBLIC_KEY, server.publicKey)
            putExtra(FreeShieldVpnService.EXTRA_SERVER_CLIENT_IP, server.clientIp)
            putExtra(FreeShieldVpnService.EXTRA_SERVER_DNS, server.dns)
            putExtra(FreeShieldVpnService.EXTRA_SERVER_MTU, server.mtu)
            putExtra(FreeShieldVpnService.EXTRA_KILL_SWITCH, killSwitch)
            putExtra(FreeShieldVpnService.EXTRA_AUTO_RECONNECT, autoReconnect)
        }

        try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start FreeShieldVpnService: ${e.message}", e)
            _vpnState.value = VpnState.Error("Could not start VPN service: ${e.localizedMessage}")
        }
    }

    fun stopVpn(context: Context) {
        if (_vpnState.value is VpnState.Disconnected) return

        _vpnState.value = VpnState.Disconnecting
        stopDurationTicker()

        val intent = Intent(context, FreeShieldVpnService::class.java).apply {
            action = FreeShieldVpnService.ACTION_DISCONNECT
        }
        try {
            context.startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send disconnect to VPN service: ${e.message}")
            onServiceDisconnected()
        }
    }

    fun reconnect(context: Context, killSwitch: Boolean, autoReconnect: Boolean) {
        val server = _selectedServer.value
        if (server == null) {
            _vpnState.value = VpnState.Error("No server selected to reconnect.")
            return
        }
        stopVpn(context)
        scope.launch {
            delay(500)
            startVpn(context, server, killSwitch, autoReconnect)
        }
    }

    // Callbacks from FreeShieldVpnService
    fun onServiceConnected(connectedAt: Long) {
        _vpnState.value = VpnState.Connected(connectedAt)
        startDurationTicker(connectedAt)
        fetchPublicIp()
    }

    fun onServiceDisconnected() {
        stopDurationTicker()
        _vpnState.value = VpnState.Disconnected
        _vpnStats.value = _vpnStats.value.copy(isFetchingIp = false)
    }

    fun onServiceError(errorMessage: String) {
        stopDurationTicker()
        _vpnState.value = VpnState.Error(errorMessage)
        _vpnStats.value = _vpnStats.value.copy(isFetchingIp = false)
    }

    fun updateTrafficStats(bytesIn: Long, bytesOut: Long) {
        _vpnStats.value = _vpnStats.value.copy(
            bytesIn = bytesIn,
            bytesOut = bytesOut
        )
    }

    private fun startDurationTicker(startTime: Long) {
        durationTickerJob?.cancel()
        durationTickerJob = scope.launch {
            while (isActive) {
                val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
                _vpnStats.value = _vpnStats.value.copy(durationSeconds = elapsedSeconds)
                delay(1000)
            }
        }
    }

    private fun stopDurationTicker() {
        durationTickerJob?.cancel()
        durationTickerJob = null
    }

    fun fetchPublicIp() {
        ipLookupJob?.cancel()
        _vpnStats.value = _vpnStats.value.copy(isFetchingIp = true, ipFetchError = null)

        ipLookupJob = scope.launch(Dispatchers.IO) {
            val endpoints = listOf(
                "https://api.ipify.org",
                "https://icanhazip.com",
                "https://ifconfig.me/ip"
            )

            var resolvedIp: String? = null
            var lastErr: String? = null

            for (endpoint in endpoints) {
                try {
                    val request = Request.Builder()
                        .url(endpoint)
                        .header("User-Agent", "FreeShieldVPN/1.0")
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string()?.trim()
                            if (!body.isNullOrBlank() && (body.contains(".") || body.contains(":"))) {
                                resolvedIp = body
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    lastErr = e.localizedMessage
                }
            }

            withContext(Dispatchers.Main) {
                if (resolvedIp != null) {
                    _vpnStats.value = _vpnStats.value.copy(
                        currentPublicIp = resolvedIp,
                        isFetchingIp = false,
                        ipFetchError = null
                    )
                } else {
                    _vpnStats.value = _vpnStats.value.copy(
                        isFetchingIp = false,
                        ipFetchError = lastErr ?: "IP check timed out"
                    )
                }
            }
        }
    }
}
