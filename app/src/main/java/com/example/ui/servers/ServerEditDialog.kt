package com.example.ui.servers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.VpnKey
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
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
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair
import kotlinx.coroutines.launch

@Composable
fun ServerEditDialog(
    server: VpnServer?,
    onDismiss: () -> Unit,
    onSave: (VpnServer) -> Unit,
    onDelete: (VpnServer) -> Unit,
    onTestPing: suspend (VpnServer) -> Long
) {
    val context = LocalContext.current
    val isNew = server == null

    var name by remember { mutableStateOf(server?.name ?: "") }
    var country by remember { mutableStateOf(server?.country ?: "Custom Gateway") }
    var countryCode by remember { mutableStateOf(server?.countryCode ?: "US") }
    var host by remember { mutableStateOf(server?.host ?: "") }
    var portText by remember { mutableStateOf((server?.port ?: 51820).toString()) }
    var protocol by remember { mutableStateOf(server?.protocol ?: "WireGuard") }
    var publicKey by remember { mutableStateOf(server?.publicKey ?: "") }
    var presharedKey by remember { mutableStateOf(server?.presharedKey ?: "") }
    var clientPrivateKey by remember { mutableStateOf(server?.clientPrivateKey ?: "") }
    var clientPublicKey by remember { mutableStateOf(server?.clientPublicKey ?: "") }
    var clientIp by remember { mutableStateOf(server?.clientIp ?: "10.0.0.2/32") }
    var allowedIps by remember { mutableStateOf(server?.allowedIps ?: "0.0.0.0/0, ::/0") }
    var dns by remember { mutableStateOf(server?.dns ?: "1.1.1.1, 8.8.8.8") }
    var mtuText by remember { mutableStateOf((server?.mtu ?: 1420).toString()) }
    var keepaliveText by remember { mutableStateOf((server?.persistentKeepalive ?: 25).toString()) }
    var notes by remember { mutableStateOf(server?.notes ?: "") }

    var showConfImporter by remember { mutableStateOf(false) }
    var rawConfInput by remember { mutableStateOf("") }

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
                    text = if (isNew) "Add WireGuard Server" else "Configure WireGuard Node",
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
                // Info Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ElectricCyan.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Enter your legitimate WireGuard server Endpoint IP/Host and cryptographic keys, or import from a .conf file.",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Quick Action: Import .conf
                            OutlinedButton(
                                onClick = { showConfImporter = !showConfImporter },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f).testTag("toggle_conf_import_button")
                            ) {
                                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (showConfImporter) "Hide Importer" else "Import .conf", fontSize = 11.sp)
                            }

                            // Quick Action: Generate Client Keypair
                            OutlinedButton(
                                onClick = {
                                    val (privKey, pubKey) = VpnServer.generateKeyPair()
                                    clientPrivateKey = privKey
                                    clientPublicKey = pubKey
                                    Toast.makeText(context, "New WireGuard keypair generated.", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldProtected),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldProtected.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f).testTag("generate_keys_button")
                            ) {
                                Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gen Keys", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Collapsible WireGuard .conf Importer Box
                AnimatedVisibility(visible = showConfImporter) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ShieldSurfaceVariantDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldSurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Paste wg0.conf or client config content:",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            OutlinedTextField(
                                value = rawConfInput,
                                onValueChange = { rawConfInput = it },
                                placeholder = { Text("[Interface]\nPrivateKey = ...\nAddress = ...\n[Peer]\nPublicKey = ...\nEndpoint = ...", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                colors = customTextFieldColors()
                            )
                            Button(
                                onClick = {
                                    if (rawConfInput.isNotBlank()) {
                                        val parsed = VpnServer.parseFromWgConfig(rawConfInput, defaultName = name.ifBlank { "Imported Server" })
                                        if (parsed.host.isNotBlank()) host = parsed.host
                                        portText = parsed.port.toString()
                                        if (parsed.publicKey.isNotBlank()) publicKey = parsed.publicKey
                                        if (parsed.presharedKey.isNotBlank()) presharedKey = parsed.presharedKey
                                        if (parsed.clientPrivateKey.isNotBlank()) clientPrivateKey = parsed.clientPrivateKey
                                        if (parsed.clientPublicKey.isNotBlank()) clientPublicKey = parsed.clientPublicKey
                                        if (parsed.clientIp.isNotBlank()) clientIp = parsed.clientIp
                                        if (parsed.allowedIps.isNotBlank()) allowedIps = parsed.allowedIps
                                        if (parsed.dns.isNotBlank()) dns = parsed.dns
                                        mtuText = parsed.mtu.toString()
                                        keepaliveText = parsed.persistentKeepalive.toString()
                                        showConfImporter = false
                                        validationError = null
                                        Toast.makeText(context, "Config parsed and fields populated!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black),
                                modifier = Modifier.fillMaxWidth().testTag("apply_conf_button")
                            ) {
                                Text("Apply Parsed Configuration", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                    placeholder = { Text("e.g. Singapore Production Node") },
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
                        placeholder = { Text("SG") },
                        modifier = Modifier.weight(1f),
                        colors = customTextFieldColors()
                    )
                }

                // Host and Port
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Endpoint Host / IP") },
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

                // Server Public Key
                OutlinedTextField(
                    value = publicKey,
                    onValueChange = { publicKey = it },
                    label = { Text("Server Public Key (Base64)") },
                    placeholder = { Text("WireGuard Peer PublicKey") },
                    modifier = Modifier.fillMaxWidth().testTag("input_server_public_key"),
                    colors = customTextFieldColors()
                )

                // Client Private Key with auto derivation
                OutlinedTextField(
                    value = clientPrivateKey,
                    onValueChange = {
                        clientPrivateKey = it
                        try {
                            if (it.length >= 40) {
                                val key = Key.fromBase64(it.trim())
                                clientPublicKey = KeyPair(key).publicKey.toBase64()
                            }
                        } catch (_: Exception) {}
                    },
                    label = { Text("Client Private Key (Base64)") },
                    placeholder = { Text("Interface PrivateKey") },
                    modifier = Modifier.fillMaxWidth().testTag("input_client_private_key"),
                    colors = customTextFieldColors()
                )

                // Client Public Key display (User copies this to add to server wg0.conf [Peer] section)
                if (clientPublicKey.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ShieldSurfaceVariantDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldSurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Client Public Key (Add this to your server wg0.conf):", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                Text(clientPublicKey, color = EmeraldProtected, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("WireGuard Public Key", clientPublicKey))
                                    Toast.makeText(context, "Copied Client Public Key to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Tunnel IP & MTU
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = clientIp,
                        onValueChange = { clientIp = it },
                        label = { Text("Interface IP") },
                        placeholder = { Text("10.0.0.2/32") },
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

                // DNS & Allowed IPs
                OutlinedTextField(
                    value = dns,
                    onValueChange = { dns = it },
                    label = { Text("DNS Servers") },
                    placeholder = { Text("1.1.1.1, 8.8.8.8") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = customTextFieldColors()
                )

                OutlinedTextField(
                    value = allowedIps,
                    onValueChange = { allowedIps = it },
                    label = { Text("Allowed IPs") },
                    placeholder = { Text("0.0.0.0/0, ::/0") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = customTextFieldColors()
                )

                // Reachability Test
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
                    if (host.isBlank() || host.contains("placeholder") || host.contains("example.com")) {
                        validationError = "Please enter a legitimate server host or IP address (not a placeholder)."
                        return@Button
                    }
                    val port = portText.toIntOrNull()
                    if (port == null || port !in 1..65535) {
                        validationError = "Port must be a valid number between 1 and 65535."
                        return@Button
                    }
                    if (publicKey.isBlank() || publicKey.length < 32) {
                        validationError = "Please enter a valid Base64 WireGuard Server Public Key."
                        return@Button
                    }
                    if (clientPrivateKey.isBlank() || clientPrivateKey.length < 32) {
                        validationError = "Please enter or generate a Client Private Key."
                        return@Button
                    }
                    val mtu = mtuText.toIntOrNull() ?: 1420
                    val keepalive = keepaliveText.toIntOrNull() ?: 25

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
                        presharedKey = presharedKey,
                        clientPrivateKey = clientPrivateKey,
                        clientPublicKey = clientPublicKey,
                        clientIp = clientIp,
                        allowedIps = allowedIps,
                        dns = dns,
                        mtu = mtu,
                        persistentKeepalive = keepalive,
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
