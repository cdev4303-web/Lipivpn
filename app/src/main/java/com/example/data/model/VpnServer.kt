package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair

enum class ServerStatus {
    ONLINE,
    DEGRADED,
    OFFLINE,
    NOT_CONFIGURED
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
    val presharedKey: String = "",
    val clientPrivateKey: String = "",
    val clientPublicKey: String = "",
    val clientIp: String = "10.0.0.2/32",
    val allowedIps: String = "0.0.0.0/0, ::/0",
    val dns: String = "1.1.1.1, 8.8.8.8",
    val mtu: Int = 1420,
    val persistentKeepalive: Int = 25,
    val pingMs: Long? = null,
    val isCustom: Boolean = false,
    val notes: String = ""
) {
    /**
     * Checks if this server has legitimate, valid WireGuard configuration.
     * Rejects placeholder domains, blank endpoints, or missing cryptographic keys.
     */
    fun isConfigured(): Boolean {
        if (host.isBlank() || host.contains("placeholder") || host.contains("example.com") || host.endsWith(".internal")) {
            return false
        }
        if (port !in 1..65535) {
            return false
        }
        if (clientIp.isBlank()) {
            return false
        }
        if (publicKey.isBlank() || publicKey.contains("INSERT_") || publicKey.length < 32) {
            return false
        }
        if (clientPrivateKey.isBlank() || clientPrivateKey.contains("INSERT_") || clientPrivateKey.length < 32) {
            return false
        }
        return true
    }

    val displayStatus: ServerStatus
        get() {
            return if (!isConfigured()) {
                ServerStatus.NOT_CONFIGURED
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

    /**
     * Generates a standard WireGuard INI configuration string for wireguard-go.
     */
    fun toWgConfigString(): String {
        val address = if (clientIp.contains("/")) clientIp else "$clientIp/32"
        val keepaliveLine = if (persistentKeepalive > 0) "PersistentKeepalive = $persistentKeepalive\n" else ""
        val pskLine = if (presharedKey.isNotBlank()) "PresharedKey = ${presharedKey.trim()}\n" else ""
        val mtuLine = if (mtu in 1280..65535) "MTU = $mtu\n" else ""
        val dnsLine = if (dns.isNotBlank()) "DNS = ${dns.trim()}\n" else ""

        return """
[Interface]
PrivateKey = ${clientPrivateKey.trim()}
Address = $address
$dnsLine$mtuLine
[Peer]
PublicKey = ${publicKey.trim()}
$pskLine
Endpoint = ${host.trim()}:$port
AllowedIPs = ${allowedIps.ifBlank { "0.0.0.0/0, ::/0" }}
$keepaliveLine
""".trimIndent()
    }

    companion object {
        fun countryCodeToEmoji(countryCode: String): String {
            if (countryCode.length != 2) return "🌐"
            val firstChar = Character.codePointAt(countryCode.uppercase(), 0) - 0x41 + 0x1F1E6
            val secondChar = Character.codePointAt(countryCode.uppercase(), 1) - 0x41 + 0x1F1E6
            return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
        }

        /**
         * Generates a fresh cryptographically secure WireGuard Curve25519 keypair.
         * Returns (PrivateKeyBase64, PublicKeyBase64).
         */
        fun generateKeyPair(): Pair<String, String> {
            val keyPair = KeyPair()
            return Pair(keyPair.privateKey.toBase64(), keyPair.publicKey.toBase64())
        }

        /**
         * Parses standard WireGuard .conf content into a VpnServer entity.
         */
        fun parseFromWgConfig(confContent: String, defaultName: String = "Imported Gateway"): VpnServer {
            var privateKey = ""
            var address = "10.0.0.2/32"
            var dns = "1.1.1.1, 8.8.8.8"
            var mtu = 1420
            var publicKey = ""
            var endpointHost = ""
            var endpointPort = 51820
            var allowedIps = "0.0.0.0/0, ::/0"
            var presharedKey = ""
            var keepalive = 25

            confContent.lines().forEach { rawLine ->
                val line = rawLine.substringBefore('#').trim()
                if (line.contains('=')) {
                    val key = line.substringBefore('=').trim().lowercase()
                    val value = line.substringAfter('=').trim()
                    when (key) {
                        "privatekey" -> privateKey = value
                        "address" -> address = value
                        "dns" -> dns = value
                        "mtu" -> mtu = value.toIntOrNull() ?: 1420
                        "publickey" -> publicKey = value
                        "presharedkey" -> presharedKey = value
                        "allowedips" -> allowedIps = value
                        "persistentkeepalive" -> keepalive = value.toIntOrNull() ?: 25
                        "endpoint" -> {
                            if (value.contains(':')) {
                                endpointHost = value.substringBeforeLast(':').trim('[', ']')
                                endpointPort = value.substringAfterLast(':').toIntOrNull() ?: 51820
                            } else {
                                endpointHost = value
                            }
                        }
                    }
                }
            }

            var derivedPublicKey = ""
            if (privateKey.isNotBlank()) {
                try {
                    val key = Key.fromBase64(privateKey)
                    derivedPublicKey = KeyPair(key).publicKey.toBase64()
                } catch (_: Exception) {}
            }

            return VpnServer(
                name = defaultName,
                country = "Custom",
                countryCode = "US",
                host = endpointHost,
                port = endpointPort,
                publicKey = publicKey,
                presharedKey = presharedKey,
                clientPrivateKey = privateKey,
                clientPublicKey = derivedPublicKey,
                clientIp = address,
                allowedIps = allowedIps,
                dns = dns,
                mtu = mtu,
                persistentKeepalive = keepalive,
                isCustom = true
            )
        }
    }
}
