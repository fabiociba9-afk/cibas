package com.example

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.entity.UserRole
import com.example.ui.components.RoleBadge
import com.example.ui.theme.ExecutivoGoTheme
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
  fun executivogo_role_badge_screenshot() {
    composeTestRule.setContent {
      ExecutivoGoTheme {
        RoleBadge(role = UserRole.SUPER_ADMIN.name)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/role_badge.png")
  }
}
