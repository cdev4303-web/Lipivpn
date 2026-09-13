package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.VpnServer
import com.example.data.model.VpnStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FreeShield VPN", appName)
  }

  @Test
  fun `vpn server placeholder detection and configuration validation`() {
    val unconfiguredServer = VpnServer(
      name = "Placeholder",
      country = "Singapore",
      countryCode = "SG",
      host = "",
      publicKey = "",
      clientPrivateKey = ""
    )
    assertFalse(unconfiguredServer.isConfigured())

    val placeholderInternalServer = VpnServer(
      name = "Old Placeholder",
      country = "Singapore",
      countryCode = "SG",
      host = "sg-node1.placeholder.internal",
      publicKey = "bm90YXJlYWxrZXlub3RhcmVhbGtleW5vdGFyZWFsa2V5MQ==",
      clientPrivateKey = "bm90YXJlYWxrZXlub3RhcmVhbGtleW5vdGFyZWFsa2V5MQ=="
    )
    assertFalse(placeholderInternalServer.isConfigured())

    val (clientPrivKey, clientPubKey) = VpnServer.generateKeyPair()
    val (serverPrivKey, serverPubKey) = VpnServer.generateKeyPair()

    val realServer = VpnServer(
      name = "Real WireGuard Node",
      country = "Singapore",
      countryCode = "SG",
      host = "198.51.100.1",
      port = 51820,
      publicKey = serverPubKey,
      clientPrivateKey = clientPrivKey,
      clientPublicKey = clientPubKey,
      clientIp = "10.0.0.2/32"
    )
    assertTrue(realServer.isConfigured())

    val wgConfig = realServer.toWgConfigString()
    assertTrue(wgConfig.contains("[Interface]"))
    assertTrue(wgConfig.contains("[Peer]"))
    assertTrue(wgConfig.contains("PrivateKey = $clientPrivKey"))
    assertTrue(wgConfig.contains("PublicKey = $serverPubKey"))
    assertTrue(wgConfig.contains("Endpoint = 198.51.100.1:51820"))
  }

  @Test
  fun `wireguard conf file parser`() {
    val confContent = """
      [Interface]
      PrivateKey = aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa=
      Address = 10.200.0.3/32
      DNS = 1.1.1.1, 9.9.9.9
      MTU = 1380

      [Peer]
      PublicKey = bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb=
      Endpoint = vpn.example.org:51820
      AllowedIPs = 0.0.0.0/0, ::/0
      PersistentKeepalive = 25
    """.trimIndent()

    val parsed = VpnServer.parseFromWgConfig(confContent, "My Work VPN")
    assertEquals("My Work VPN", parsed.name)
    assertEquals("vpn.example.org", parsed.host)
    assertEquals(51820, parsed.port)
    assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa=", parsed.clientPrivateKey)
    assertEquals("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb=", parsed.publicKey)
    assertEquals("10.200.0.3/32", parsed.clientIp)
    assertEquals("1.1.1.1, 9.9.9.9", parsed.dns)
    assertEquals(1380, parsed.mtu)
    assertEquals(25, parsed.persistentKeepalive)
    assertTrue(parsed.isConfigured())
  }

  @Test
  fun `vpn stats formatting`() {
    val stats = VpnStats(
      bytesIn = 1048576L * 5,
      bytesOut = 1024L * 50,
      durationSeconds = 125L
    )
    assertEquals("5.00 MB", stats.formattedDownload)
    assertEquals("50.0 KB", stats.formattedUpload)
    assertEquals("02:05", stats.formattedDuration)
  }
}
