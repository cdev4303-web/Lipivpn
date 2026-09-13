package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
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

/**
 * Foreground Service managing the WireGuard tunnel lifecycle, notification,
 * bandwidth metrics, and network connectivity state transitions.
 */
class FreeShieldVpnService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val tunnelManager by lazy { WireGuardTunnelManager(this) }

    private var currentServer: VpnServer? = null
    private var isKillSwitchEnabled = false
    private var isAutoReconnectEnabled = true

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val isRunning = AtomicBoolean(false)
    private var lastTrafficNotificationUpdate = 0L

    override fun onBind(intent: Intent?): IBinder? = null

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
                val serverName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "WireGuard Server"
                val serverCountry = intent.getStringExtra(EXTRA_SERVER_COUNTRY) ?: ""
                val serverCountryCode = intent.getStringExtra(EXTRA_SERVER_COUNTRY_CODE) ?: ""
                val serverHost = intent.getStringExtra(EXTRA_SERVER_HOST) ?: ""
                val serverPort = intent.getIntExtra(EXTRA_SERVER_PORT, 51820)
                val serverProtocol = intent.getStringExtra(EXTRA_SERVER_PROTOCOL) ?: "WireGuard"
                val serverPublicKey = intent.getStringExtra(EXTRA_SERVER_PUBLIC_KEY) ?: ""
                val serverPresharedKey = intent.getStringExtra(EXTRA_SERVER_PRESHARED_KEY) ?: ""
                val serverClientPrivateKey = intent.getStringExtra(EXTRA_SERVER_CLIENT_PRIVATE_KEY) ?: ""
                val serverClientPublicKey = intent.getStringExtra(EXTRA_SERVER_CLIENT_PUBLIC_KEY) ?: ""
                val serverClientIp = intent.getStringExtra(EXTRA_SERVER_CLIENT_IP) ?: "10.0.0.2/32"
                val serverAllowedIps = intent.getStringExtra(EXTRA_SERVER_ALLOWED_IPS) ?: "0.0.0.0/0, ::/0"
                val serverDns = intent.getStringExtra(EXTRA_SERVER_DNS) ?: "1.1.1.1, 8.8.8.8"
                val serverMtu = intent.getIntExtra(EXTRA_SERVER_MTU, 1420)
                val serverKeepalive = intent.getIntExtra(EXTRA_SERVER_KEEPALIVE, 25)

                isKillSwitchEnabled = intent.getBooleanExtra(EXTRA_KILL_SWITCH, false)
                isAutoReconnectEnabled = intent.getBooleanExtra(EXTRA_AUTO_RECONNECT, true)

                val server = VpnServer(
                    id = serverId,
                    name = serverName,
                    country = serverCountry,
                    countryCode = serverCountryCode,
                    host = serverHost,
                    port = serverPort,
                    protocol = serverProtocol,
                    publicKey = serverPublicKey,
                    presharedKey = serverPresharedKey,
                    clientPrivateKey = serverClientPrivateKey,
                    clientPublicKey = serverClientPublicKey,
                    clientIp = serverClientIp,
                    allowedIps = serverAllowedIps,
                    dns = serverDns,
                    mtu = serverMtu,
                    persistentKeepalive = serverKeepalive
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
        if (!server.isConfigured()) {
            VpnController.onServiceError("No VPN server configuration available. Please configure a valid server IP/host and WireGuard keys.")
            stopSelf()
            return
        }

        // Display immediate foreground notification
        val initialNotification = buildNotification("Connecting to ${server.name}...")
        try {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            ServiceCompat.startForeground(this, NOTIFICATION_ID, initialNotification, serviceType)
        } catch (e: Exception) {
            Log.e(TAG, "startForeground error: ${e.message}", e)
        }

        isRunning.set(true)
        registerNetworkMonitor()

        tunnelManager.startTunnel(
            server = server,
            scope = serviceScope,
            onConnected = { connectedAt, publicIp ->
                updateNotification("Connected to ${server.name} • Public IP: $publicIp")
                VpnController.onServiceConnected(connectedAt, publicIp)
            },
            onError = { errorMessage ->
                handleConnectionError(errorMessage)
            },
            onStatsUpdate = { rxBytes, txBytes ->
                VpnController.updateTrafficStats(rxBytes, txBytes)
                updateNotificationTraffic(server.name, rxBytes, txBytes)
            }
        )
    }

    private fun handleConnectionError(errorMessage: String) {
        cleanUp()
        VpnController.onServiceError(errorMessage)

        if (isAutoReconnectEnabled && currentServer != null && currentServer!!.isConfigured()) {
            serviceScope.launch {
                Log.i(TAG, "Auto-reconnect scheduled in 4 seconds...")
                delay(4000)
                if (!isRunning.get() && currentServer != null) {
                    connect(currentServer!!)
                }
            }
        } else {
            stopSelf()
        }
    }

    private fun disconnect() {
        Log.i(TAG, "Disconnecting VPN tunnel...")
        cleanUp()
        VpnController.onServiceDisconnected()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanUp() {
        isRunning.set(false)
        unregisterNetworkMonitor()
        tunnelManager.stopTunnel()
    }

    override fun onDestroy() {
        Log.i(TAG, "FreeShieldVpnService onDestroy")
        cleanUp()
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
                    if (isRunning.get() && isAutoReconnectEnabled) {
                        Log.i(TAG, "Network lost while VPN active, awaiting network re-establishment...")
                    }
                }

                override fun onAvailable(network: Network) {
                    Log.i(TAG, "Network connection re-established")
                    if (isRunning.get() && !tunnelManager.isRunning() && currentServer != null) {
                        Log.i(TAG, "Reconnecting WireGuard tunnel after network change...")
                        connect(currentServer!!)
                    }
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

    private fun updateNotificationTraffic(serverName: String, rxBytes: Long, txBytes: Long) {
        val now = System.currentTimeMillis()
        if (now - lastTrafficNotificationUpdate > 3000) {
            lastTrafficNotificationUpdate = now
            val down = VpnStats.formatBytes(rxBytes)
            val up = VpnStats.formatBytes(txBytes)
            updateNotification("Connected to $serverName • ↓$down ↑$up")
        }
    }

    companion object {
        const val ACTION_CONNECT = "com.example.vpn.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.ACTION_DISCONNECT"

        const val EXTRA_SERVER_ID = "extra_server_id"
        const val EXTRA_SERVER_NAME = "extra_server_name"
        const val EXTRA_SERVER_COUNTRY = "extra_server_country"
        const val EXTRA_SERVER_COUNTRY_CODE = "extra_server_country_code"
        const val EXTRA_SERVER_HOST = "extra_server_host"
        const val EXTRA_SERVER_PORT = "extra_server_port"
        const val EXTRA_SERVER_PROTOCOL = "extra_server_protocol"
        const val EXTRA_SERVER_PUBLIC_KEY = "extra_server_public_key"
        const val EXTRA_SERVER_PRESHARED_KEY = "extra_server_preshared_key"
        const val EXTRA_SERVER_CLIENT_PRIVATE_KEY = "extra_server_client_private_key"
        const val EXTRA_SERVER_CLIENT_PUBLIC_KEY = "extra_server_client_public_key"
        const val EXTRA_SERVER_CLIENT_IP = "extra_server_client_ip"
        const val EXTRA_SERVER_ALLOWED_IPS = "extra_server_allowed_ips"
        const val EXTRA_SERVER_DNS = "extra_server_dns"
        const val EXTRA_SERVER_MTU = "extra_server_mtu"
        const val EXTRA_SERVER_KEEPALIVE = "extra_server_keepalive"
        const val EXTRA_KILL_SWITCH = "extra_kill_switch"
        const val EXTRA_AUTO_RECONNECT = "extra_auto_reconnect"

        private const val CHANNEL_ID = "freeshield_vpn_channel"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "FreeShieldVpnService"
    }
}
