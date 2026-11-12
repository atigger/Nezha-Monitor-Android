package com.atigger.status.data

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonitorAuthenticationTest {
    @Test
    fun patUsesBearerWithoutJwtCookie() {
        val config = MonitorConfig(apiToken = " nzp_test-token ")
        val request = Request.Builder().url("https://monitor.example/api/v1/server-group")
            .monitorAuthentication(config, null).build()

        assertEquals("Bearer nzp_test-token", request.header("Authorization"))
        assertNull(request.header("Cookie"))
    }

    @Test
    fun patOverridesPasswordLoginAndPreviouslyResolvedJwt() {
        val config = MonitorConfig(requiresLogin = true, apiToken = "nzp_test-token")
        val request = Request.Builder().url(config.copy(baseUrl = "https://monitor.example").websocketUrl())
            .header("Cookie", "nz-jwt=stale-token")
            .monitorAuthentication(config, "old-jwt").build()

        assertEquals("Bearer nzp_test-token", request.header("Authorization"))
        assertNull(request.header("Cookie"))
    }

    @Test
    fun passwordLoginSendsBearerAndLegacyCookie() {
        val config = MonitorConfig(requiresLogin = true)
        val request = Request.Builder().url("https://monitor.example")
            .monitorAuthentication(config, "test-jwt").build()

        assertEquals("Bearer test-jwt", request.header("Authorization"))
        assertEquals("nz-jwt=test-jwt", request.header("Cookie"))
    }

    @Test
    fun guestDoesNotSendStaleCredentials() {
        val request = Request.Builder().url("https://monitor.example")
            .header("Authorization", "Bearer stale-token")
            .header("Cookie", "nz-jwt=stale-token")
            .monitorAuthentication(MonitorConfig(), "old-jwt").build()

        assertNull(request.header("Authorization"))
        assertNull(request.header("Cookie"))
    }

    @Test
    fun missingJwtDoesNotSendEmptyAuthentication() {
        val request = Request.Builder().url("https://monitor.example")
            .monitorAuthentication(MonitorConfig(requiresLogin = true), "").build()

        assertNull(request.header("Authorization"))
        assertNull(request.header("Cookie"))
    }
}
