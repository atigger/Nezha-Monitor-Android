package com.atigger.status.data

import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NezhaApiCompatibilityTest {
    private val gson = Gson()

    @Test
    fun patIsResolvedWithoutNetworkOrPasswordLogin() = runBlocking {
        val token = StatusRepository().fetchAuthToken(
            MonitorConfig(requiresLogin = true, apiToken = " nzp_test-token ")
        )

        assertEquals("nzp_test-token", token)
    }

    @Test
    fun guestDoesNotAttemptLogin() = runBlocking {
        assertNull(StatusRepository().fetchAuthToken(MonitorConfig()))
    }

    @Test
    fun newApiErrorFieldIsParsed() {
        val response = gson.fromJson(
            """{"success":false,"error":"permission denied"}""",
            ServerGroupResponse::class.java
        )

        assertEquals("permission denied", response.message)
    }

    @Test
    fun oldMessageFieldIsStillParsed() {
        val response = gson.fromJson(
            """{"success":false,"message":"login required"}""",
            ServerGroupResponse::class.java
        )

        assertEquals("login required", response.message)
    }

    @Test
    fun emptyGroupsMayBeNull() {
        val response = gson.fromJson("""{"success":true,"data":null}""", ServerGroupResponse::class.java)

        assertTrue(response.data.orEmpty().toUiModels().isEmpty())
    }

    @Test
    fun groupServerMembershipRemainsCompatible() {
        val response = gson.fromJson(
            """{"success":true,"data":[{"group":{"id":7,"name":"production"},"servers":[1,2]}]}""",
            ServerGroupResponse::class.java
        )
        val groups = response.data.orEmpty().toUiModels()

        assertEquals(1, groups.size)
        assertEquals(7, groups.single().id)
        assertEquals("production", groups.single().name)
    }
}
