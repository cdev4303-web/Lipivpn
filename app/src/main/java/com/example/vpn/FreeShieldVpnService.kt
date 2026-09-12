package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.VpnServer
import com.example.data.model.VpnStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class FreeShieldVpnService : VpnService() {

    private var tunnelEngine: VpnTunnelEngine? = null
    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var currentServer: VpnServer? = null
    private var isKillSwitchEnabled = false
    private var isAutoReconnectEnabled = true

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val isRunning = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.i(TAG, "onStartCommand action=$action")

        when (action) {
            ACTION_CONNECT -> {
                val serverId = intent.getLongExtra(EXTRA_SERVER_ID, 0L)
                val serverName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "VPN Server"
                val serverHost = intent.getStringExtra(EXTRA_SERVER_HOST) ?: ""
                val serverPort = intent.getIntExtra(EXTRA_SERVER_PORT, 51820)
                val serverProtocol = intent.getStringExtra(EXTRA_SERVER_PROTOCOL) ?: "WireGuard"
                val serverPublicKey = intent.getStringExtra(EXTRA_SERVER_PUBLIC_KEY) ?: ""
                val serverClientIp = intent.getStringExtra(EXTRA_SERVER_CLIENT_IP) ?: "10.0.0.2"
                val serverDns = intent.getStringExtra(EXTRA_SERVER_DNS) ?: "1.1.1.1, 8.8.8.8"
                val serverMtu = intent.getIntExtra(EXTRA_SERVER_MTU, 1420)

                isKillSwitchEnabled = intent.getBooleanExtra(EXTRA_KILL_SWITCH, false)
                isAutoReconnectEnabled = intent.getBooleanExtra(EXTRA_AUTO_RECONNECT, true)

                val server = VpnServer(
                    id = serverId,
                    name = serverName,
                    country = "",
                    countryCode = "",
                    host = serverHost,
                    port = serverPort,
                    protocol = serverProtocol,
                    publicKey = serverPublicKey,
                    clientIp = serverClientIp,
                    dns = serverDns,
                    mtu = serverMtu
                )
                currentServer = server

                connect(server)
            }
            ACTION_DISCONNECT -> {
                disconnect()
            }
        }
        return START_NOT_STICKY
    }

    private fun connect(server: VpnServer) {
        if (isRunning.get()) {
            Log.w(TAG, "VPN already running, tearing down previous connection before starting new")
            cleanUpTunnel()
        }

        // Validate configuration strictly
        if (!server.isConfigured()) {
            VpnController.onServiceError("Server configuration is not available.")
            stopSelf()
            return
        }

        // Start Foreground Notification immediately
        val initialNotification = buildNotification("Connecting to ${server.name}...")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
                ServiceCompat.startForeground(this, NOTIFICATION_ID, initialNotification, serviceType)
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground error: ${e.message}", e)
        }

        serviceScope.launch(Dispatchers.IO) {
            try {
                // Configure Android TUN Interface via Builder
                val builder = Builder()
                builder.setSession(server.name)
                builder.setMtu(server.mtu)
                builder.addAddress(server.clientIp, 24)
                builder.addRoute("0.0.0.0", 0) // Route all IPv4 network traffic

                // DNS servers
                server.dns.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { dnsIp ->
                    try {
                        builder.addDnsServer(dnsIp)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to add DNS $dnsIp: ${e.message}")
                    }
                }

                // Kill switch configuration: block non-VPN traffic where supported
                if (isKillSwitchEnabled) {
                    builder.setBlocking(true)
                }

                // Establish TUN Interface
                val pfd = builder.establish()
                if (pfd == null) {
                    throw IllegalStateException("VpnService.Builder.establish() returned null. TUN interface could not be created.")
                }
                vpnInterface = pfd
                isRunning.set(true)

                // Initialize real tunnel engine and protected socket loop
                val engine = VpnTunnelEngine(
                    vpnService = this@FreeShieldVpnService,
                    pfd = pfd,
                    server = server,
                    onStatsUpdated = { bytesIn, bytesOut ->
                        VpnController.updateTrafficStats(bytesIn, bytesOut)
                        updateNotificationTraffic(server.name, bytesIn, bytesOut)
                    },
                    onError = { errorMsg ->
                        Log.e(TAG, "Tunnel engine reported error: $errorMsg")
                        handleConnectionError(errorMsg)
                    }
                )
                tunnelEngine = engine
                engine.start(serviceScope)

                registerNetworkMonitor()

                val connectedAt = System.currentTimeMillis()
                VpnController.onServiceConnected(connectedAt)

                updateNotification("Protected via ${server.name}")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to establish VPN connection: ${e.message}", e)
                handleConnectionError("Connection failed: ${e.localizedMessage ?: "Unknown tunnel error"}")
            }
        }
    }

    private fun handleConnectionError(errorMessage: String) {
        cleanUpTunnel()
        VpnController.onServiceError(errorMessage)

        if (isAutoReconnectEnabled && currentServer != null && currentServer!!.isConfigured()) {
            serviceScope.launch {
                Log.i(TAG, "Auto-reconnect triggered in 3 seconds...")
                delay(3000)
                if (!isRunning.get() && currentServer != null) {
                    connect(currentServer!!)
                }
            }
        } else {
            stopSelf()
        }
    }

    private fun disconnect() {
        Log.i(TAG, "Disconnect requested")
        cleanUpTunnel()
        VpnController.onServiceDisconnected()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanUpTunnel() {
        isRunning.set(false)
        unregisterNetworkMonitor()

        try {
            tunnelEngine?.stop()
        } catch (_: Exception) {}
        tunnelEngine = null

        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null
    }

    override fun onRevoke() {
        Log.w(TAG, "VPN permission was revoked by the system or user in Android Settings.")
        disconnect()
        super.onRevoke()
    }

    override fun onDestroy() {
        Log.i(TAG, "FreeShieldVpnService onDestroy")
        cleanUpTunnel()
        super.onDestroy()
    }

    private fun registerNetworkMonitor() {
        if (networkCallback != null) return
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    Log.w(TAG, "Default network lost")
                    if (isRunning.get()) {
                        handleConnectionError("Network connection lost.")
                    }
                }

                override fun onAvailable(network: Network) {
                    Log.i(TAG, "Network became available")
                }
            }
            connectivityManager?.registerNetworkCallback(request, callback)
            networkCallback = callback
        } catch (e: Exception) {
            Log.w(TAG, "Could not register network callback: ${e.message}")
        }
    }

    private fun unregisterNetworkMonitor() {
        networkCallback?.let {
            try {
                connectivityManager?.unregisterNetworkCallback(it)
            } catch (_: Exception) {}
        }
        networkCallback = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "FreeShield VPN Status",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows live status of active VPN tunnel"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }

    private fun buildNotification(statusText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, FreeShieldVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this, 1, disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FreeShield VPN")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", disconnectPendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val notification = buildNotification(statusText)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private var lastTrafficUpdate = 0L
    private fun updateNotificationTraffic(serverName: String, bytesIn: Long, bytesOut: Long) {
        val now = System.currentTimeMillis()
        if (now - lastTrafficUpdate > 3000) {
            lastTrafficUpdate = now
            val down = VpnStats.formatBytes(bytesIn)
            val up = VpnStats.formatBytes(bytesOut)
            updateNotification("Connected to $serverName • ↓$down ↑$up")
        }
    }

    companion object {
        const val ACTION_CONNECT = "com.example.vpn.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.ACTION_DISCONNECT"

        const val EXTRA_SERVER_ID = "extra_server_id"
        const val EXTRA_SERVER_NAME = "extra_server_name"
        const val EXTRA_SERVER_HOST = "extra_server_host"
        const val EXTRA_SERVER_PORT = "extra_server_port"
        const val EXTRA_SERVER_PROTOCOL = "extra_server_protocol"
        const val EXTRA_SERVER_PUBLIC_KEY = "extra_server_public_key"
        const val EXTRA_SERVER_CLIENT_IP = "extra_server_client_ip"
        const val EXTRA_SERVER_DNS = "extra_server_dns"
        const val EXTRA_SERVER_MTU = "extra_server_mtu"
        const val EXTRA_KILL_SWITCH = "extra_kill_switch"
        const val EXTRA_AUTO_RECONNECT = "extra_auto_reconnect"

        private const val CHANNEL_ID = "freeshield_vpn_channel"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "FreeShieldVpnService"
    }
}
