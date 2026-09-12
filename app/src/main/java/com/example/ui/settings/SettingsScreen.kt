package com.example.ui.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SettingsUiState
import com.example.ui.theme.AmberConnecting
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldProtected
import com.example.ui.theme.ShieldSurfaceBorder
import com.example.ui.theme.ShieldSurfaceVariantDark

@Composable
fun SettingsScreen(
    settings: SettingsUiState,
    onAutoConnectToggle: (Boolean) -> Unit,
    onAutoReconnectToggle: (Boolean) -> Unit,
    onKillSwitchToggle: (Boolean) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "VPN Configuration & Security",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        // General VPN switches card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, ShieldSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Auto Connect
                SettingSwitchRow(
                    icon = Icons.Default.Power,
                    title = "Auto-Connect on Launch",
                    subtitle = "Automatically establish VPN tunnel when FreeShield is opened.",
                    checked = settings.autoConnect,
                    onCheckedChange = onAutoConnectToggle,
                    tag = "setting_auto_connect"
                )

                androidx.compose.material3.HorizontalDivider(color = ShieldSurfaceBorder, thickness = 1.dp)

                // Auto Reconnect
                SettingSwitchRow(
                    icon = Icons.Default.Refresh,
                    title = "Auto-Reconnect",
                    subtitle = "Re-establishes connection upon Wi-Fi / LTE network interruptions.",
                    checked = settings.autoReconnect,
                    onCheckedChange = onAutoReconnectToggle,
                    tag = "setting_auto_reconnect"
                )

                androidx.compose.material3.HorizontalDivider(color = ShieldSurfaceBorder, thickness = 1.dp)

                // Notifications
                SettingSwitchRow(
                    icon = Icons.Default.Notifications,
                    title = "Foreground Status Notifications",
                    subtitle = "Display live upload/download bandwidth and quick disconnect in notification shade.",
                    checked = settings.notifications,
                    onCheckedChange = onNotificationsToggle,
                    tag = "setting_notifications"
                )
            }
        }

        // Kill Switch Section
        KillSwitchCard(
            killSwitch = settings.killSwitch,
            onKillSwitchToggle = onKillSwitchToggle,
            onOpenAndroidVpnSettings = {
                try {
                    val intent = Intent(Settings.ACTION_VPN_SETTINGS)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to general wireless settings
                    try {
                        val fallback = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                        context.startActivity(fallback)
                    } catch (_: Exception) {}
                }
            }
        )

        // Guide: How to deploy free VPN server
        WireGuardDeploymentGuideCard()

        // Privacy & Architecture Card
        PrivacyPledgeCard()

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ElectricCyan,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ElectricCyan,
                checkedTrackColor = ElectricCyan.copy(alpha = 0.3f)
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun KillSwitchCard(
    killSwitch: Boolean,
    onKillSwitchToggle: (Boolean) -> Unit,
    onOpenAndroidVpnSettings: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (killSwitch) CrimsonAlert.copy(alpha = 0.6f) else ShieldSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = if (killSwitch) CrimsonAlert else AmberConnecting,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "VPN Kill Switch",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Switch(
                    checked = killSwitch,
                    onCheckedChange = onKillSwitchToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CrimsonAlert,
                        checkedTrackColor = CrimsonAlert.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.testTag("setting_kill_switch")
                )
            }

            Text(
                text = "Blocks non-VPN traffic using VpnService.Builder.setBlocking(true). Prevents accidental network leaks during reconnects or tunnel drops.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            // Warning callout mandated by Requirement 8
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AmberConnecting.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AmberConnecting.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = AmberConnecting, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Android System Leak Protection Notice",
                            color = AmberConnecting,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "For 100% OS-level hardware leak protection on Android ${Build.VERSION.RELEASE}, tap below to open Android system VPN settings and enable 'Always-on VPN' and 'Block connections without VPN'.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onOpenAndroidVpnSettings,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberConnecting),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberConnecting.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().testTag("open_system_vpn_settings_button")
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Android System VPN Settings", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun WireGuardDeploymentGuideCard() {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                Text(
                    text = "How to Deploy a Free VPN Server",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Text(
                text = "You can host your own free WireGuard server on AWS Free Tier, Oracle Cloud Always Free, or a \$4 VPS:",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ShieldSurfaceVariantDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "curl -O https://raw.githubusercontent.com/angristan/wireguard-install/master/wireguard-install.sh\nchmod +x wireguard-install.sh\n./wireguard-install.sh",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = ElectricCyan,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Text(
                text = "Copy the server Endpoint IP, Port (51820), and Public Key into the FreeShield Server Editor to connect immediately.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun PrivacyPledgeCard() {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = EmeraldProtected, modifier = Modifier.size(18.dp))
                Text(
                    text = "About FreeShield VPN v1.0",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Text(
                text = "• Zero-Log Architecture: No DNS queries, destinations, or packet contents are stored.\n" +
                        "• Official Android VpnService: Direct kernel-level TUN encapsulation.\n" +
                        "• No Plaintext Credentials: Keys stored securely in local Room database.\n" +
                        "• Transparent Routing: No payload modification, spyware, or trackers.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}
