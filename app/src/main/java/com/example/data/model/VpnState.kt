package com.example.data.model

/**
 * VPN connection state representation.
 */
sealed class VpnState {
    object Disconnected : VpnState()
    object Connecting : VpnState()
    data class Connected(val connectedAt: Long) : VpnState()
    object Disconnecting : VpnState()
    data class Error(val message: String, val timestamp: Long = System.currentTimeMillis()) : VpnState()

    val isConnected: Boolean
        get() = this is Connected

    val isConnecting: Boolean
        get() = this is Connecting

    val isIdle: Boolean
        get() = this is Disconnected || this is Error
}
