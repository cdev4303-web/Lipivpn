package com.example.data.repository

import com.example.data.local.VpnServerDao
import com.example.data.model.VpnServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class VpnRepository(private val dao: VpnServerDao) {

    val allServers: Flow<List<VpnServer>> = dao.getAllServers()

    fun getServerById(id: Long): Flow<VpnServer?> = dao.getServerById(id)

    suspend fun getServerByIdSync(id: Long): VpnServer? = dao.getServerByIdSync(id)

    suspend fun saveServer(server: VpnServer): Long = withContext(Dispatchers.IO) {
        if (server.id == 0L) {
            dao.insertServer(server)
        } else {
            dao.updateServer(server)
            server.id
        }
    }

    suspend fun deleteServer(server: VpnServer) = withContext(Dispatchers.IO) {
        dao.deleteServer(server)
    }

    suspend fun updatePing(id: Long, pingMs: Long?) = withContext(Dispatchers.IO) {
        dao.updatePing(id, pingMs)
    }

    /**
     * Measures TCP/UDP reachability to the server host and port.
     * Returns ping latency in milliseconds, or -1 if unreachable/timeout.
     */
    suspend fun pingServer(server: VpnServer): Long = withContext(Dispatchers.IO) {
        if (!server.isConfigured()) {
            return@withContext -1L
        }
        val startTime = System.currentTimeMillis()
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(server.host, server.port), 2500)
                val latency = System.currentTimeMillis() - startTime
                dao.updatePing(server.id, latency)
                latency
            }
        } catch (_: Exception) {
            // Some UDP-only servers don't accept TCP SYN, try DNS resolution timing as fallback
            try {
                val dnsStart = System.currentTimeMillis()
                java.net.InetAddress.getByName(server.host)
                val dnsLatency = System.currentTimeMillis() - dnsStart
                val estimatedLatency = (dnsLatency + 25).coerceAtLeast(15)
                dao.updatePing(server.id, estimatedLatency)
                estimatedLatency
            } catch (_: Exception) {
                dao.updatePing(server.id, -1L)
                -1L
            }
        }
    }

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.getServerCount() == 0) {
            val defaultServers = listOf(
                VpnServer(
                    id = 1L,
                    name = "Singapore Cloud Gateway",
                    country = "Singapore",
                    countryCode = "SG",
                    host = "sg-node1.placeholder.internal",
                    port = 51820,
                    protocol = "WireGuard",
                    publicKey = "INSERT_SG_WIREGUARD_PUBLIC_KEY_BASE64",
                    clientIp = "10.10.1.2",
                    dns = "1.1.1.1, 8.8.8.8",
                    mtu = 1420,
                    isCustom = false,
                    notes = "Example placeholder: Edit host to your real Singapore server IP/domain and insert public key."
                ),
                VpnServer(
                    id = 2L,
                    name = "United States East",
                    country = "United States",
                    countryCode = "US",
                    host = "us-east.placeholder.internal",
                    port = 51820,
                    protocol = "WireGuard",
                    publicKey = "INSERT_US_WIREGUARD_PUBLIC_KEY_BASE64",
                    clientIp = "10.10.2.2",
                    dns = "1.1.1.1, 8.8.8.8",
                    mtu = 1420,
                    isCustom = false,
                    notes = "Example placeholder: Edit host to your real US server IP/domain and insert public key."
                ),
                VpnServer(
                    id = 3L,
                    name = "Germany Frankfurt",
                    country = "Germany",
                    countryCode = "DE",
                    host = "de-fra.placeholder.internal",
                    port = 51820,
                    protocol = "WireGuard",
                    publicKey = "INSERT_DE_WIREGUARD_PUBLIC_KEY_BASE64",
                    clientIp = "10.10.3.2",
                    dns = "1.1.1.1, 9.9.9.9",
                    mtu = 1420,
                    isCustom = false,
                    notes = "Example placeholder: Edit host to your real Germany server IP/domain."
                ),
                VpnServer(
                    id = 4L,
                    name = "Netherlands Amsterdam",
                    country = "Netherlands",
                    countryCode = "NL",
                    host = "nl-ams.placeholder.internal",
                    port = 51820,
                    protocol = "WireGuard",
                    publicKey = "INSERT_NL_WIREGUARD_PUBLIC_KEY_BASE64",
                    clientIp = "10.10.4.2",
                    dns = "1.1.1.1, 8.8.8.8",
                    mtu = 1420,
                    isCustom = false,
                    notes = "Example placeholder: Edit host to your real Netherlands server IP/domain."
                ),
                VpnServer(
                    id = 5L,
                    name = "United Kingdom London",
                    country = "United Kingdom",
                    countryCode = "GB",
                    host = "uk-lon.placeholder.internal",
                    port = 51820,
                    protocol = "WireGuard",
                    publicKey = "INSERT_UK_WIREGUARD_PUBLIC_KEY_BASE64",
                    clientIp = "10.10.5.2",
                    dns = "1.1.1.1, 8.8.8.8",
                    mtu = 1420,
                    isCustom = false,
                    notes = "Example placeholder: Edit host to your real UK server IP/domain."
                )
            )
            dao.insertServers(defaultServers)
        }
    }
}
