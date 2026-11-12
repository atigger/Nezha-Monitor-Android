package com.atigger.status.data

import com.atigger.status.i18n.AppStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitorConfigTest {
    private val strings = AppStrings.of(AppLanguage.EN)

    @Test
    fun patDoesNotRequirePasswordEvenForExistingLoginConfig() {
        val config = MonitorConfig(
            baseUrl = "https://monitor.example",
            requiresLogin = true,
            apiToken = " nzp_test-token "
        )

        assertFalse(config.usesPasswordLogin)
        assertNull(config.validationErrorOrNull(strings))
    }

    @Test
    fun oldLoginConfigStillRequiresCredentials() {
        val config = MonitorConfig(baseUrl = "https://monitor.example", requiresLogin = true)

        assertTrue(config.usesPasswordLogin)
        assertEquals(strings.loginRequiresCredentials, config.validationErrorOrNull(strings))
        assertNull(config.copy(username = "admin", password = "test-password").validationErrorOrNull(strings))
    }

    @Test
    fun guestAndBlankPatRemainValid() {
        val config = MonitorConfig(baseUrl = "https://monitor.example", apiToken = "  ")

        assertFalse(config.usesPasswordLogin)
        assertNull(config.validationErrorOrNull(strings))
    }

    @Test
    fun malformedPatIsRejected() {
        listOf("Bearer nzp_test", "jwt-token", "nzp_", "nzp_test token", "nzp_test\r\nInjected: header").forEach { token ->
            val config = MonitorConfig(baseUrl = "https://monitor.example", apiToken = token)
            assertEquals(token, strings.invalidApiToken, config.validationErrorOrNull(strings))
        }
    }

    @Test
    fun proxyPrefixIsPreservedForEveryEndpoint() {
        val config = MonitorConfig(baseUrl = "https://monitor.example:8443/nezha/")

        assertEquals("https://monitor.example:8443/nezha/api/v1/login", config.loginUrl())
        assertEquals("https://monitor.example:8443/nezha/api/v1/server-group", config.serverGroupUrl())
        assertEquals("wss://monitor.example:8443/nezha/api/v1/ws/server", config.websocketUrl())
        assertEquals("https://monitor.example:8443", config.originUrl())
    }

    @Test
    fun pastedApiEndpointsAreNormalizedWithoutDuplication() {
        listOf("/api/v1", "/api/v1/login", "/api/v1/server", "/api/v1/server-group", "/api/v1/ws/server").forEach { suffix ->
            val config = MonitorConfig(baseUrl = "https://monitor.example/nezha$suffix/")
            assertEquals("https://monitor.example/nezha/api/v1/server-group", config.serverGroupUrl())
        }
    }

    @Test
    fun websocketInputConvertsToHttpForApiRequests() {
        val config = MonitorConfig(baseUrl = "ws://monitor.example:8008/api/v1/ws/server")

        assertEquals("http://monitor.example:8008/api/v1/login", config.loginUrl())
        assertEquals("ws://monitor.example:8008/api/v1/ws/server", config.websocketUrl())
    }

    @Test
    fun encodedProxyPrefixIsNotDoubleEncoded() {
        val config = MonitorConfig(baseUrl = "https://monitor.example/monitor%20panel")

        assertEquals("https://monitor.example/monitor%20panel/api/v1/server-group", config.serverGroupUrl())
    }

    @Test
    fun urlsContainingCredentialsQueryOrFragmentAreRejected() {
        listOf(
            "https://user:password@monitor.example",
            "https://monitor.example?token=secret",
            "https://monitor.example#fragment",
            "file:///tmp/monitor"
        ).forEach { url ->
            assertEquals(url, strings.invalidMonitorUrl, MonitorConfig(baseUrl = url).validationErrorOrNull(strings))
        }
    }
}
