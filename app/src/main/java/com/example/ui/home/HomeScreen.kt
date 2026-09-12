package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VpnServer
import com.example.data.model.VpnState
import com.example.data.model.VpnStats
import com.example.ui.theme.AmberConnecting
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldProtected
import com.example.ui.theme.ShieldSurfaceBorder
import com.example.ui.theme.ShieldSurfaceVariantDark

@Composable
fun HomeScreen(
    vpnState: VpnState,
    vpnStats: VpnStats,
    selectedServer: VpnServer?,
    autoReconnect: Boolean,
    onAutoReconnectChange: (Boolean) -> Unit,
    onConnectToggle: () -> Unit,
    onSelectServerClick: () -> Unit,
    onConfigureServerClick: (VpnServer) -> Unit,
    onRefreshIpClick: () -> Unit,
    onDismissError: () -> Unit,
    isErrorDismissed: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Status Badge
        StatusPill(vpnState = vpnState)

        // Error Banner if present
        if (vpnState is VpnState.Error && !isErrorDismissed) {
            ErrorBanner(
                errorMessage = vpnState.message,
                onConfigureClick = {
                    selectedServer?.let { onConfigureServerClick(it) }
                },
                onDismiss = onDismissError
            )
        }

        // Selected Server Card
        SelectedServerCard(
            server = selectedServer,
            onClick = onSelectServerClick,
            onEditClick = { server -> onConfigureServerClick(server) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Large Central Connect / Disconnect Button
        ConnectButton(
            vpnState = vpnState,
            onClick = onConnectToggle
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Live Telemetry or Protection Status
        if (vpnState.isConnected) {
            ConnectedTelemetryCard(
                vpnStats = vpnStats,
                onRefreshIp = onRefreshIpClick
            )
        } else {
            ProtectionStateSummary(
                autoReconnect = autoReconnect,
                onAutoReconnectChange = onAutoReconnectChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StatusPill(vpnState: VpnState) {
    val (statusText, statusColor, statusIcon) = when (vpnState) {
        is VpnState.Connected -> Triple("CONNECTED • PROTECTED", EmeraldProtected, Icons.Default.CheckCircle)
        is VpnState.Connecting -> Triple("CONNECTING TUNNEL...", AmberConnecting, Icons.Default.Speed)
        is VpnState.Disconnecting -> Triple("DISCONNECTING...", AmberConnecting, Icons.Default.Speed)
        is VpnState.Error -> Triple("CONNECTION ERROR", CrimsonAlert, Icons.Default.ErrorOutline)
        is VpnState.Disconnected -> Triple("DISCONNECTED • NOT SECURED", Color(0xFF94A3B8), Icons.Default.Lock)
    }

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = statusColor.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
        modifier = Modifier.testTag("status_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun ErrorBanner(
    errorMessage: String,
    onConfigureClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CrimsonAlert.copy(alpha = 0.12f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("error_banner")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = CrimsonAlert,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Connection Failed",
                    color = CrimsonAlert,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Text("✕", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = errorMessage,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            if (errorMessage.contains("configuration", ignoreCase = true) || errorMessage.contains("not available", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onConfigureClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("configure_server_button")
                ) {
                    Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Configure Real Server Endpoint", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SelectedServerCard(
    server: VpnServer?,
    onClick: () -> Unit,
    onEditClick: (VpnServer) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldSurfaceBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("selected_server_card")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Country flag or globe
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ShieldSurfaceVariantDark),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = server?.flagEmoji ?: "🌐",
                    fontSize = 24.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server?.country?.ifEmpty { "Default Gateway" } ?: "Select Server",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = server?.name ?: "No server selected",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                if (server != null && !server.isConfigured()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AmberConnecting.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberConnecting.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable { onEditClick(server) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "⚠️ Setup Needed (Tap to configure)",
                                color = AmberConnecting,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                modifier = Modifier.testTag("change_server_button")
            ) {
                Text("Change", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ConnectButton(
    vpnState: VpnState,
    onClick: () -> Unit
) {
    val isConnected = vpnState.isConnected
    val isConnecting = vpnState.isConnecting || vpnState is VpnState.Disconnecting

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val targetColor = when {
        isConnected -> EmeraldProtected
        isConnecting -> AmberConnecting
        else -> ElectricCyan
    }
    val animatedColor by animateColorAsState(targetValue = targetColor, label = "button_color")

    Box(
        modifier = Modifier
            .size(200.dp)
            .testTag("connect_button_container"),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing ripple rings
        if (isConnected || isConnecting) {
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(animatedColor.copy(alpha = 0.15f))
            )
        }

        // Secondary ring
        Box(
            modifier = Modifier
                .size(175.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            animatedColor.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
                .border(2.dp, animatedColor.copy(alpha = 0.6f), CircleShape)
        )

        // Main tactile touchable button
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            ShieldSurfaceVariantDark
                        )
                    )
                )
                .border(3.dp, animatedColor, CircleShape)
                .clickable(onClick = onClick)
                .testTag("vpn_connect_button"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        color = AmberConnecting,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "WAIT",
                        color = AmberConnecting,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                } else {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.Security else Icons.Default.VpnKey,
                        contentDescription = if (isConnected) "Disconnect" else "Connect",
                        tint = animatedColor,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isConnected) "DISCONNECT" else "CONNECT",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectedTelemetryCard(
    vpnStats: VpnStats,
    onRefreshIp: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldSurfaceBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("telemetry_card")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header: Public IP with refresh button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                    Text("Public IP Address", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (vpnStats.isFetchingIp) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ElectricCyan)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = vpnStats.currentPublicIp ?: (vpnStats.ipFetchError ?: "Resolving..."),
                        color = if (vpnStats.currentPublicIp != null) EmeraldProtected else AmberConnecting,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    IconButton(onClick = onRefreshIp, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh IP", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                    }
                }
            }

            androidx.compose.material3.HorizontalDivider(color = ShieldSurfaceBorder, thickness = 1.dp)

            // Metrics: Duration & Data usage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Duration
                TelemetryItem(
                    icon = Icons.Default.Timer,
                    iconTint = ElectricCyan,
                    title = "Duration",
                    value = vpnStats.formattedDuration
                )

                // Download
                TelemetryItem(
                    icon = Icons.Default.ArrowDownward,
                    iconTint = EmeraldProtected,
                    title = "Download",
                    value = vpnStats.formattedDownload
                )

                // Upload
                TelemetryItem(
                    icon = Icons.Default.ArrowUpward,
                    iconTint = AmberConnecting,
                    title = "Upload",
                    value = vpnStats.formattedUpload
                )
            }
        }
    }
}

@Composable
private fun TelemetryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp))
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun ProtectionStateSummary(
    autoReconnect: Boolean,
    onAutoReconnectChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Reconnect on Network Switch",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Restores encrypted tunnel automatically when Wi-Fi or LTE reconnects.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = autoReconnect,
                    onCheckedChange = onAutoReconnectChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ElectricCyan,
                        checkedTrackColor = ElectricCyan.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.testTag("auto_reconnect_switch")
                )
            }

            androidx.compose.material3.HorizontalDivider(color = ShieldSurfaceBorder, thickness = 1.dp)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Official Android VpnService Engine active",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }
    }
}
