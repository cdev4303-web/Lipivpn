package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("freeshield_vpn_prefs", Context.MODE_PRIVATE)

    private val _autoConnect = MutableStateFlow(prefs.getBoolean(KEY_AUTO_CONNECT, false))
    val autoConnect: StateFlow<Boolean> = _autoConnect.asStateFlow()

    private val _autoReconnect = MutableStateFlow(prefs.getBoolean(KEY_AUTO_RECONNECT, true))
    val autoReconnect: StateFlow<Boolean> = _autoReconnect.asStateFlow()

    private val _killSwitch = MutableStateFlow(prefs.getBoolean(KEY_KILL_SWITCH, false))
    val killSwitch: StateFlow<Boolean> = _killSwitch.asStateFlow()

    private val _notifications = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATIONS, true))
    val notifications: StateFlow<Boolean> = _notifications.asStateFlow()

    private val _selectedServerId = MutableStateFlow(prefs.getLong(KEY_SELECTED_SERVER_ID, 1L))
    val selectedServerId: StateFlow<Long> = _selectedServerId.asStateFlow()

    fun setAutoConnect(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CONNECT, enabled).apply()
        _autoConnect.value = enabled
    }

    fun setAutoReconnect(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_RECONNECT, enabled).apply()
        _autoReconnect.value = enabled
    }

    fun setKillSwitch(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KILL_SWITCH, enabled).apply()
        _killSwitch.value = enabled
    }

    fun setNotifications(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
        _notifications.value = enabled
    }

    fun setSelectedServerId(id: Long) {
        prefs.edit().putLong(KEY_SELECTED_SERVER_ID, id).apply()
        _selectedServerId.value = id
    }

    companion object {
        private const val KEY_AUTO_CONNECT = "pref_auto_connect"
        private const val KEY_AUTO_RECONNECT = "pref_auto_reconnect"
        private const val KEY_KILL_SWITCH = "pref_kill_switch"
        private const val KEY_NOTIFICATIONS = "pref_notifications"
        private const val KEY_SELECTED_SERVER_ID = "pref_selected_server_id"
    }
}
