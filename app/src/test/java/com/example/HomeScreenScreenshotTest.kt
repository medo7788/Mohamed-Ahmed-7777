package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.ClevCalcTheme
import com.example.ui.theme.getThemeColors
import com.example.ui.theme.AppThemeKey
import com.example.viewmodel.MainViewModel
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
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class HomeScreenScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun home_screen_light_screenshot() {
    val themeKey = AppThemeKey.LIGHT
    val colors = getThemeColors(themeKey)
    val mockViewModel = MainViewModel()

    composeTestRule.setContent {
      ClevCalcTheme(themeKey = themeKey) {
        HomeScreen(
          colors = colors,
          viewModel = mockViewModel,
          onSelectCalc = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/home_screen_light.png")
  }

  @Test
  fun home_screen_dark_screenshot() {
    val themeKey = AppThemeKey.ELEGANT_DARK
    val colors = getThemeColors(themeKey)
    val mockViewModel = MainViewModel()

    composeTestRule.setContent {
      ClevCalcTheme(themeKey = themeKey) {
        HomeScreen(
          colors = colors,
          viewModel = mockViewModel,
          onSelectCalc = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/home_screen_dark.png")
  }
}
