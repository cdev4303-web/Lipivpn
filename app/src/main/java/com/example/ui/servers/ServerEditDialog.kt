package com.example.ui.servers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.VpnServer
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldProtected
import com.example.ui.theme.ShieldSurfaceBorder
import com.example.ui.theme.ShieldSurfaceDark
import com.example.ui.theme.ShieldSurfaceVariantDark
import kotlinx.coroutines.launch

@Composable
fun ServerEditDialog(
    server: VpnServer?,
    onDismiss: () -> Unit,
    onSave: (VpnServer) -> Unit,
    onDelete: (VpnServer) -> Unit,
    onTestPing: suspend (VpnServer) -> Long
) {
    val isNew = server == null
    var name by remember { mutableStateOf(server?.name ?: "") }
    var country by remember { mutableStateOf(server?.country ?: "United States") }
    var countryCode by remember { mutableStateOf(server?.countryCode ?: "US") }
    var host by remember { mutableStateOf(server?.host ?: "") }
    var portText by remember { mutableStateOf((server?.port ?: 51820).toString()) }
    var protocol by remember { mutableStateOf(server?.protocol ?: "WireGuard") }
    var publicKey by remember { mutableStateOf(server?.publicKey ?: "") }
    var clientIp by remember { mutableStateOf(server?.clientIp ?: "10.0.0.2") }
    var dns by remember { mutableStateOf(server?.dns ?: "1.1.1.1, 8.8.8.8") }
    var mtuText by remember { mutableStateOf((server?.mtu ?: 1420).toString()) }
    var notes by remember { mutableStateOf(server?.notes ?: "") }

    var testPingResult by remember { mutableStateOf<Long?>(null) }
    var isTestingPing by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .testTag("server_edit_dialog"),
        containerColor = ShieldSurfaceDark,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isNew) "Add VPN Gateway" else "Configure Server Endpoint",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                if (!isNew && server?.isCustom == true) {
                    IconButton(
                        onClick = {
                            server?.let { onDelete(it) }
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonAlert)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Developer / Admin instruction note
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ElectricCyan.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                        Column {
                            Text(
                                text = "Enter the legitimate IP address or domain of your WireGuard/VPN server, or click 'Fill Test Endpoint' below to test the Android VPN tunnel immediately.",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    host = "1.1.1.1"
                                    portText = "51820"
                                    publicKey = "YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE="
                                    clientIp = "10.0.0.2"
                                    dns = "1.1.1.1, 8.8.8.8"
                                    validationError = null
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                                modifier = Modifier.testTag("fill_test_endpoint_button")
                            ) {
                                Text("⚡ Fill Test Endpoint (1.1.1.1)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = CrimsonAlert,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Server Label") },
                    placeholder = { Text("e.g. My Secure Node 1") },
                    modifier = Modifier.fillMaxWidth().testTag("input_server_name"),
                    colors = customTextFieldColors()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country") },
                        modifier = Modifier.weight(2f),
                        colors = customTextFieldColors()
                    )
                    OutlinedTextField(
                        value = countryCode,
                        onValueChange = { if (it.length <= 2) countryCode = it.uppercase() },
                        label = { Text("Code") },
                        placeholder = { Text("US") },
                        modifier = Modifier.weight(1f),
                        colors = customTextFieldColors()
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Server Host / IP") },
                        placeholder = { Text("e.g. 198.51.100.24") },
                        modifier = Modifier.weight(2.5f).testTag("input_server_host"),
                        colors = customTextFieldColors()
                    )
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it },
                        label = { Text("Port") },
                        placeholder = { Text("51820") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.5f),
                        colors = customTextFieldColors()
                    )
                }

                OutlinedTextField(
                    value = publicKey,
                    onValueChange = { publicKey = it },
                    label = { Text("Server Public Key") },
                    placeholder = { Text("Base64 WireGuard Public Key") },
                    modifier = Modifier.fillMaxWidth().testTag("input_server_public_key"),
                    colors = customTextFieldColors()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = clientIp,
                        onValueChange = { clientIp = it },
                        label = { Text("Tunnel Client IP") },
                        placeholder = { Text("10.0.0.2") },
                        modifier = Modifier.weight(1f),
                        colors = customTextFieldColors()
                    )
                    OutlinedTextField(
                        value = mtuText,
                        onValueChange = { mtuText = it },
                        label = { Text("MTU") },
                        placeholder = { Text("1420") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = customTextFieldColors()
                    )
                }

                OutlinedTextField(
                    value = dns,
                    onValueChange = { dns = it },
                    label = { Text("DNS Servers") },
                    placeholder = { Text("1.1.1.1, 8.8.8.8") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = customTextFieldColors()
                )

                // Ping Test Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            val portInt = portText.toIntOrNull() ?: 51820
                            val testServer = (server ?: VpnServer(
                                name = name,
                                country = country,
                                countryCode = countryCode,
                                host = host,
                                port = portInt
                            )).copy(host = host, port = portInt)

                            isTestingPing = true
                            coroutineScope.launch {
                                val latency = onTestPing(testServer)
                                testPingResult = latency
                                isTestingPing = false
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                    ) {
                        if (isTestingPing) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = ElectricCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Test Reachability", fontSize = 12.sp)
                    }

                    if (testPingResult != null) {
                        val latency = testPingResult!!
                        Text(
                            text = if (latency > 0) "$latency ms (Reachable)" else "Unreachable",
                            color = if (latency > 0) EmeraldProtected else CrimsonAlert,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        validationError = "Please enter a server name."
                        return@Button
                    }
                    if (host.isBlank()) {
                        validationError = "Please enter a valid server host or IP address."
                        return@Button
                    }
                    val port = portText.toIntOrNull()
                    if (port == null || port !in 1..65535) {
                        validationError = "Port must be a valid number between 1 and 65535."
                        return@Button
                    }
                    val mtu = mtuText.toIntOrNull() ?: 1420

                    val updatedServer = (server ?: VpnServer(
                        name = name,
                        country = country,
                        countryCode = countryCode,
                        host = host,
                        isCustom = true
                    )).copy(
                        name = name,
                        country = country,
                        countryCode = countryCode,
                        host = host,
                        port = port,
                        protocol = protocol,
                        publicKey = publicKey,
                        clientIp = clientIp,
                        dns = dns,
                        mtu = mtu,
                        notes = notes
                    )
                    onSave(updatedServer)
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black),
                modifier = Modifier.testTag("save_server_button")
            ) {
                Text("Save Configuration", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ElectricCyan,
    unfocusedBorderColor = ShieldSurfaceBorder,
    focusedLabelColor = ElectricCyan,
    unfocusedLabelColor = Color(0xFF94A3B8),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = ShieldSurfaceVariantDark,
    unfocusedContainerColor = ShieldSurfaceVariantDark
)
