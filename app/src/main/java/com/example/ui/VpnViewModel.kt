package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.model.VpnServer
import com.example.data.model.VpnState
import com.example.data.model.VpnStats
import com.example.data.repository.VpnRepository
import com.example.vpn.VpnController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val autoConnect: Boolean = false,
    val autoReconnect: Boolean = true,
    val killSwitch: Boolean = false,
    val notifications: Boolean = true
)

class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = VpnRepository(database.vpnServerDao())
    val preferences = PreferencesManager(application)

    val vpnState: StateFlow<VpnState> = VpnController.vpnState
    val vpnStats: StateFlow<VpnStats> = VpnController.vpnStats

    val servers: StateFlow<List<VpnServer>> = repository.allServers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _userDismissedError = MutableStateFlow(false)
    val userDismissedError: StateFlow<Boolean> = _userDismissedError.asStateFlow()

    // Combining selected server ID with loaded server list
    val selectedServer: StateFlow<VpnServer?> = combine(
        servers,
        preferences.selectedServerId,
        VpnController.selectedServer
    ) { serverList, selectedId, controllerServer ->
        // If controller currently has a connected/connecting server, prioritize that
        controllerServer ?: serverList.find { it.id == selectedId } ?: serverList.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val settings: StateFlow<SettingsUiState> = combine(
        preferences.autoConnect,
        preferences.autoReconnect,
        preferences.killSwitch,
        preferences.notifications
    ) { autoConnect, autoReconnect, killSwitch, notifications ->
        SettingsUiState(
            autoConnect = autoConnect,
            autoReconnect = autoReconnect,
            killSwitch = killSwitch,
            notifications = notifications
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        viewModelScope.launch {
            repository.seedIfEmpty()
        }
    }

    fun selectServer(server: VpnServer) {
        preferences.setSelectedServerId(server.id)
        VpnController.setSelectedServer(server)
    }

    fun connect(context: Context) {
        _userDismissedError.value = false
        val server = selectedServer.value
        if (server == null) {
            VpnController.onServiceError("No VPN server selected.")
            return
        }
        VpnController.startVpn(
            context = context,
            server = server,
            killSwitch = preferences.killSwitch.value,
            autoReconnect = preferences.autoReconnect.value
        )
    }

    fun disconnect(context: Context) {
        VpnController.stopVpn(context)
    }

    fun reconnect(context: Context) {
        _userDismissedError.value = false
        VpnController.reconnect(
            context = context,
            killSwitch = preferences.killSwitch.value,
            autoReconnect = preferences.autoReconnect.value
        )
    }

    fun dismissError() {
        _userDismissedError.value = true
    }

    fun saveServer(server: VpnServer, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveServer(server)
            selectServer(server.copy(id = id))
            onComplete()
        }
    }

    fun deleteServer(server: VpnServer) {
        viewModelScope.launch {
            repository.deleteServer(server)
        }
    }

    fun pingServer(server: VpnServer) {
        viewModelScope.launch {
            repository.pingServer(server)
        }
    }

    fun pingAllServers() {
        viewModelScope.launch {
            val list = servers.value
            for (srv in list) {
                repository.pingServer(srv)
            }
        }
    }

    fun refreshPublicIp() {
        VpnController.fetchPublicIp()
    }

    fun setAutoConnect(enabled: Boolean) = preferences.setAutoConnect(enabled)
    fun setAutoReconnect(enabled: Boolean) = preferences.setAutoReconnect(enabled)
    fun setKillSwitch(enabled: Boolean) = preferences.setKillSwitch(enabled)
    fun setNotifications(enabled: Boolean) = preferences.setNotifications(enabled)
}
