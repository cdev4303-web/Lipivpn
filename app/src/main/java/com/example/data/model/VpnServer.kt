package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ServerStatus {
    ONLINE,
    DEGRADED,
    OFFLINE,
    CONFIG_REQUIRED
}

@Entity(tableName = "vpn_servers")
data class VpnServer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val country: String,
    val countryCode: String,
    val host: String,
    val port: Int = 51820,
    val protocol: String = "WireGuard",
    val publicKey: String = "",
    val clientIp: String = "10.0.0.2",
    val dns: String = "1.1.1.1, 8.8.8.8",
    val mtu: Int = 1420,
    val pingMs: Long? = null,
    val isCustom: Boolean = false,
    val notes: String = ""
) {
    /**
     * Checks if this server has legitimate, valid configuration (not placeholder).
     * Requirements state: Do not invent credentials or hard-code fake functional servers.
     * Real connection requires an actual reachable host/IP and valid port.
     */
    fun isConfigured(): Boolean {
        if (host.isBlank() || host.contains("placeholder") || host.contains("example.com")) {
            return false
        }
        if (port !in 1..65535) {
            return false
        }
        if (clientIp.isBlank()) {
            return false
        }
        return true
    }

    val displayStatus: ServerStatus
        get() {
            return if (!isConfigured()) {
                ServerStatus.CONFIG_REQUIRED
            } else if (pingMs != null && pingMs > 500) {
                ServerStatus.DEGRADED
            } else if (pingMs != null && pingMs < 0) {
                ServerStatus.OFFLINE
            } else {
                ServerStatus.ONLINE
            }
        }

    val flagEmoji: String
        get() = countryCodeToEmoji(countryCode)

    companion object {
        fun countryCodeToEmoji(countryCode: String): String {
            if (countryCode.length != 2) return "🌐"
            val firstChar = Character.codePointAt(countryCode.uppercase(), 0) - 0x41 + 0x1F1E6
            val secondChar = Character.codePointAt(countryCode.uppercase(), 1) - 0x41 + 0x1F1E6
            return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
        }
    }
}
