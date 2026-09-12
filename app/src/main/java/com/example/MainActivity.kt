package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.VpnServer
import com.example.data.model.VpnState
import com.example.ui.VpnViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.servers.ServerEditDialog
import com.example.ui.servers.ServerListScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldProtected
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ShieldDarkBackground
import com.example.ui.theme.ShieldSurfaceBorder
import com.example.ui.theme.ShieldSurfaceDark
import com.example.vpn.VpnController

enum class NavTab {
    HOME,
    SERVERS,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: VpnViewModel = viewModel()
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(NavTab.HOME) }

    val vpnState by viewModel.vpnState.collectAsStateWithLifecycle()
    val vpnStats by viewModel.vpnStats.collectAsStateWithLifecycle()
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val selectedServer by viewModel.selectedServer.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isErrorDismissed by viewModel.userDismissedError.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var serverToEdit by remember { mutableStateOf<VpnServer?>(null) }

    // Launcher for VPN Permission via VpnService.prepare()
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.connect(context)
        } else {
            VpnController.onServiceError("VPN system permission was denied by the user.")
        }
    }

    // Launcher for Notifications on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Continue regardless of notification permission result
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (settings.autoConnect && vpnState is VpnState.Disconnected) {
            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent == null) {
                viewModel.connect(context)
            }
        }
    }

    fun initiateVpnToggle() {
        if (vpnState.isConnected || vpnState.isConnecting) {
            viewModel.disconnect(context)
        } else {
            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent != null) {
                vpnPermissionLauncher.launch(prepareIntent)
            } else {
                viewModel.connect(context)
            }
        }
    }

    Scaffold(
        containerColor = ShieldDarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FreeShield VPN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = Color.White
                    )
                },
                actions = {
                    if (vpnState.isConnected) {
                        Text(
                            text = "● SECURED",
                            color = EmeraldProtected,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ShieldDarkBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = ShieldSurfaceDark,
                contentColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == NavTab.HOME,
                    onClick = { currentTab = NavTab.HOME },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == NavTab.HOME) Icons.Filled.Security else Icons.Outlined.Security,
                            contentDescription = "Shield Home"
                        )
                    },
                    label = { Text("Shield") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = ElectricCyan,
                        indicatorColor = ElectricCyan,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_home")
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.SERVERS,
                    onClick = { currentTab = NavTab.SERVERS },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == NavTab.SERVERS) Icons.Filled.Dns else Icons.Outlined.Dns,
                            contentDescription = "Servers"
                        )
                    },
                    label = { Text("Servers") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = ElectricCyan,
                        indicatorColor = ElectricCyan,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_servers")
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.SETTINGS,
                    onClick = { currentTab = NavTab.SETTINGS },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == NavTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = ElectricCyan,
                        indicatorColor = ElectricCyan,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavTab.HOME -> {
                    HomeScreen(
                        vpnState = vpnState,
                        vpnStats = vpnStats,
                        selectedServer = selectedServer,
                        autoReconnect = settings.autoReconnect,
                        onAutoReconnectChange = { viewModel.setAutoReconnect(it) },
                        onConnectToggle = { initiateVpnToggle() },
                        onSelectServerClick = { currentTab = NavTab.SERVERS },
                        onConfigureServerClick = { server ->
                            serverToEdit = server
                            showEditDialog = true
                        },
                        onRefreshIpClick = { viewModel.refreshPublicIp() },
                        onDismissError = { viewModel.dismissError() },
                        isErrorDismissed = isErrorDismissed
                    )
                }

                NavTab.SERVERS -> {
                    ServerListScreen(
                        servers = servers,
                        selectedServer = selectedServer,
                        onServerSelect = { server ->
                            viewModel.selectServer(server)
                            currentTab = NavTab.HOME
                        },
                        onEditServer = { server ->
                            serverToEdit = server
                            showEditDialog = true
                        },
                        onAddServer = {
                            serverToEdit = null
                            showEditDialog = true
                        },
                        onPingAll = { viewModel.pingAllServers() }
                    )
                }

                NavTab.SETTINGS -> {
                    SettingsScreen(
                        settings = settings,
                        onAutoConnectToggle = { viewModel.setAutoConnect(it) },
                        onAutoReconnectToggle = { viewModel.setAutoReconnect(it) },
                        onKillSwitchToggle = { viewModel.setKillSwitch(it) },
                        onNotificationsToggle = { viewModel.setNotifications(it) }
                    )
                }
            }

            if (showEditDialog) {
                ServerEditDialog(
                    server = serverToEdit,
                    onDismiss = { showEditDialog = false },
                    onSave = { updatedServer ->
                        viewModel.saveServer(updatedServer) {
                            Toast.makeText(context, "Server configuration saved.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDelete = { serverToDelete ->
                        viewModel.deleteServer(serverToDelete)
                        Toast.makeText(context, "Server removed.", Toast.LENGTH_SHORT).show()
                    },
                    onTestPing = { srv ->
                        viewModel.repository.pingServer(srv)
                    }
                )
            }
        }
    }
}
