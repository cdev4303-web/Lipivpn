package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.VpnServer
import com.example.data.model.VpnState
import com.example.data.model.VpnStats
import com.example.ui.home.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun freeshield_home_screenshot() {
    val sampleServer = VpnServer(
      id = 1L,
      name = "Singapore Cloud Gateway",
      country = "Singapore",
      countryCode = "SG",
      host = "sg-node1.placeholder.internal",
      port = 51820
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        HomeScreen(
          vpnState = VpnState.Disconnected,
          vpnStats = VpnStats(),
          selectedServer = sampleServer,
          autoReconnect = true,
          onAutoReconnectChange = {},
          onConnectToggle = {},
          onSelectServerClick = {},
          onConfigureServerClick = {},
          onRefreshIpClick = {},
          onDismissError = {},
          isErrorDismissed = false
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
