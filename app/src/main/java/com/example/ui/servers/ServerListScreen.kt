package com.example.ui.servers

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ServerStatus
import com.example.data.model.VpnServer
import com.example.ui.theme.AmberConnecting
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldProtected
import com.example.ui.theme.ShieldSurfaceBorder
import com.example.ui.theme.ShieldSurfaceVariantDark

@Composable
fun ServerListScreen(
    servers: List<VpnServer>,
    selectedServer: VpnServer?,
    onServerSelect: (VpnServer) -> Unit,
    onEditServer: (VpnServer) -> Unit,
    onAddServer: () -> Unit,
    onPingAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Info Banner explaining real server requirement
                AdminNoteBanner()
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Available Gateways (${servers.size})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    OutlinedButton(
                        onClick = onPingAll,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                        modifier = Modifier.testTag("ping_all_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Ping", fontSize = 12.sp)
                    }
                }
            }

            items(servers, key = { it.id }) { server ->
                val isSelected = selectedServer?.id == server.id
                ServerCard(
                    server = server,
                    isSelected = isSelected,
                    onSelect = { onServerSelect(server) },
                    onEdit = { onEditServer(server) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Padding for FAB
            }
        }

        // FAB to add custom server
        FloatingActionButton(
            onClick = onAddServer,
            containerColor = ElectricCyan,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_server_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Custom Server")
        }
    }
}

@Composable
private fun AdminNoteBanner() {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ShieldSurfaceVariantDark.copy(alpha = 0.6f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = "WireGuard Server Configuration",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Select a gateway or tap the pencil icon to configure your WireGuard host, keys, or import a .conf file. Tap '+' to add a new server.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun ServerCard(
    server: VpnServer,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit
) {
    val borderColor = if (isSelected) ElectricCyan else ShieldSurfaceBorder
    val containerColor = if (isSelected) ShieldSurfaceVariantDark else MaterialTheme.colorScheme.surface

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect)
            .testTag("server_item_${server.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Country flag
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(ShieldSurfaceVariantDark),
                contentAlignment = Alignment.Center
            ) {
                Text(text = server.flagEmoji, fontSize = 22.sp)
            }

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = server.country.ifEmpty { "Server" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (server.isCustom) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ElectricCyan.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "CUSTOM",
                                color = ElectricCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = server.name,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Protocol badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = server.protocol,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Status & Ping
                    StatusBadge(server = server)
                }
            }

            // Action: Edit & Select
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp).testTag("edit_server_${server.id}")) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Configuration", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = Color.Black, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .border(1.5.dp, ShieldSurfaceBorder, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(server: VpnServer) {
    if (!server.isConfigured()) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = AmberConnecting.copy(alpha = 0.15f)
        ) {
            Text(
                text = "Setup Needed",
                color = AmberConnecting,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    } else if (server.pingMs != null && server.pingMs > 0) {
        val pingColor = if (server.pingMs < 120) EmeraldProtected else if (server.pingMs < 300) AmberConnecting else CrimsonAlert
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = null, tint = pingColor, modifier = Modifier.size(12.dp))
            Text(
                text = "${server.pingMs} ms",
                color = pingColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    } else {
        Text(
            text = "Configured",
            color = EmeraldProtected,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
