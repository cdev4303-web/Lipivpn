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
  fun `vpn server placeholder detection`() {
    val placeholderServer = VpnServer(
      name = "Placeholder",
      country = "Singapore",
      countryCode = "SG",
      host = "sg-node1.placeholder.internal"
    )
    assertFalse(placeholderServer.isConfigured())

    val realServer = VpnServer(
      name = "Real Server",
      country = "Singapore",
      countryCode = "SG",
      host = "198.51.100.1",
      port = 51820,
      clientIp = "10.0.0.2"
    )
    assertTrue(realServer.isConfigured())
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
