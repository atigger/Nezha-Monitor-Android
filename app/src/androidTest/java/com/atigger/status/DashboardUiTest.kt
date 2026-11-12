package com.atigger.status

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.atigger.status.data.AppLanguage
import com.atigger.status.data.HostDto
import com.atigger.status.data.MonitorConfig
import com.atigger.status.data.ServerDto
import com.atigger.status.data.ServerGroupUiModel
import com.atigger.status.data.StateDto
import com.atigger.status.data.toUiModel
import com.atigger.status.i18n.AppStrings
import com.atigger.status.ui.StatusScreen
import com.atigger.status.ui.StatusUiState
import com.atigger.status.ui.StatusLoadingStage
import com.atigger.status.ui.theme.StatusTheme
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class DashboardUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val strings = AppStrings.of(AppLanguage.ZH)
    private val now = Instant.now()
    private val sampleServers = listOf(
        node(1, "Tokyo · Edge 01", 18.4, 0.36, true, setOf(1)),
        node(2, "Singapore · Core", 42.7, 0.68, true, setOf(1)),
        node(3, "Frankfurt · Edge", 0.0, 0.0, false, setOf(2)),
        node(4, "Virginia · Worker", 91.2, 0.82, true, setOf(2))
    )
    private val groups = listOf(
        ServerGroupUiModel(1, "亚太区域", setOf(1, 2)),
        ServerGroupUiModel(2, "欧美区域", setOf(3, 4))
    )

    @Test
    fun lightDashboardFiltersAndExpandsDetails() {
        var followedId: Int? = null
        showScreen(onFollow = { followedId = it })
        compose.onNodeWithText(strings.dashboardTitle).assertIsDisplayed()
        snapshot("dashboard-light")
        compose.onAllNodesWithText(strings.showDetails).onFirst().performClick()
        snapshot("dashboard-details")
        compose.onNodeWithText("IP").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText(strings.hideDetails).onFirst().performScrollTo().performClick()
        compose.onNode(hasText("欧美区域") and hasClickAction()).performClick()
        snapshot("dashboard-group")
        compose.onNodeWithText("Frankfurt · Edge").assertIsDisplayed()
        compose.onNodeWithText("Tokyo · Edge 01").assertDoesNotExist()
        compose.onNodeWithText(strings.online).performClick()
        compose.onNodeWithText("Frankfurt · Edge").assertDoesNotExist()
        compose.onNodeWithText("Virginia · Worker").assertIsDisplayed()
        compose.onNodeWithContentDescription(strings.follow).performClick()
        compose.runOnIdle { assertEquals(4, followedId) }
    }

    @Test
    fun darkDashboardDisplaysRealMetrics() {
        showScreen(dark = true)
        compose.onNodeWithText("18.4%").assertIsDisplayed()
        compose.onNodeWithText("36.0%").assertIsDisplayed()
        snapshot("dashboard-dark")
    }

    @Test
    fun settingsPreservePatAndPreferences() {
        var saved: MonitorConfig? = null
        showScreen(settings = true, onSave = { saved = it })
        compose.onNodeWithText(strings.monitorSettings).assertIsDisplayed()
        snapshot("settings-light")
        compose.onNodeWithText(strings.saveConfig).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("nzp_preview-token", saved?.apiToken)
            assertEquals("https://preview.invalid/nezha", saved?.baseUrl)
        }
    }

    @Test
    fun largeTextSettingsRemainScrollable() {
        showScreen(settings = true, dark = true, fontScale = 1.3f)
        snapshot("settings-dark-large-text")
        compose.onNodeWithText(strings.saveConfig).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun largeTextDashboardRemainsUsable() {
        showScreen(fontScale = 1.3f)
        compose.onAllNodesWithText(strings.showDetails).onFirst().performScrollTo().assertIsDisplayed()
        snapshot("dashboard-large-text")
    }

    @Test
    fun englishDashboardAndEmptyStateRemainUsable() {
        showScreen(language = AppLanguage.EN, state = StatusUiState.Success(emptyList(), "10:42:08", emptyList()))
        compose.onNodeWithText("Overview").assertIsDisplayed()
        compose.onNodeWithText("Reconnect").assertIsDisplayed()
        snapshot("dashboard-empty-english")
    }

    @Test
    fun loadingStepsRemainVisible() {
        showScreen(state = StatusUiState.Loading(stage = StatusLoadingStage.ConnectingLiveUpdates))
        compose.onNodeWithText(strings.loadingStepFetchingGroups).assertIsDisplayed()
        snapshot("dashboard-loading")
    }

    @Test
    fun errorStateProvidesRetry() {
        var retries = 0
        showScreen(state = StatusUiState.Error("无法连接面板，请检查网络。"), onRetry = { retries += 1 })
        compose.onNodeWithText(strings.retry).performClick()
        compose.runOnIdle { assertEquals(1, retries) }
        snapshot("dashboard-error")
    }

    private fun showScreen(
        dark: Boolean = false,
        settings: Boolean = false,
        fontScale: Float = 1f,
        language: AppLanguage = AppLanguage.ZH,
        state: StatusUiState = StatusUiState.Success(sampleServers, "10:42:08", groups),
        onRetry: () -> Unit = {},
        onFollow: (Int) -> Unit = {},
        onSave: (MonitorConfig) -> Unit = {}
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                StatusTheme(darkTheme = dark) {
                    StatusScreen(
                        uiState = state,
                        favoriteServerId = null,
                        monitorConfig = MonitorConfig(baseUrl = "https://preview.invalid/nezha", apiToken = "nzp_preview-token", language = language),
                        showSettings = settings,
                        settingsError = null,
                        notificationPrompt = null,
                        onToggleFavorite = onFollow,
                        onRetry = onRetry,
                        onOpenSettings = {},
                        onSaveSettings = onSave,
                        onCancelSettings = {},
                        onConfirmNotificationPrompt = {},
                        onDismissNotificationPrompt = {},
                        onBack = {}
                    )
                }
            }
        }
    }

    private fun node(id: Int, name: String, cpu: Double, memory: Double, online: Boolean, groupIds: Set<Int>) =
        ServerDto(
            id = id,
            name = name,
            host = HostDto(platform = "Ubuntu", platformVersion = "24.04", arch = "x86_64",
                cpu = listOf("AMD EPYC · 4 vCPU"), memTotal = 8L * 1024 * 1024 * 1024,
                diskTotal = 80L * 1024 * 1024 * 1024, version = "1.14.0"),
            state = StateDto(cpu = cpu, memUsed = (8L * 1024 * 1024 * 1024 * memory).toLong(),
                diskUsed = 25L * 1024 * 1024 * 1024, netInSpeed = 2_621_440, netOutSpeed = 430_080,
                uptime = 864_000, tcpConnCount = 124, udpConnCount = 18),
            lastActive = if (online) now.toString() else now.minusSeconds(600).toString()
        ).toUiModel(Gson(), strings, now, groupIds).copy(ipLine = "203.0.113.$id")

    private fun snapshot(name: String) {
        compose.waitForIdle()
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "ui-review")
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use { output ->
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }
}
