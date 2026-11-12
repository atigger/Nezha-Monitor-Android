package com.atigger.status.data

import com.atigger.status.i18n.AppStrings
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class DashboardMetricsTest {
    @Test
    fun resourcePercentUsesFloatingPointArithmetic() {
        assertEquals(37.5, resourceUsagePercent(3, 8)!!, 0.001)
        assertEquals(0.0, resourceUsagePercent(0, 8)!!, 0.001)
        assertEquals(100.0, resourceUsagePercent(Long.MAX_VALUE, Long.MAX_VALUE)!!, 0.001)
    }

    @Test
    fun invalidOrUnknownResourcesAreNotDisplayedAsZero() {
        assertNull(resourceUsagePercent(null, 8))
        assertNull(resourceUsagePercent(3, null))
        assertNull(resourceUsagePercent(3, 0))
        assertNull(resourceUsagePercent(3, -1))
        assertNull(resourceUsagePercent(-1, 8))
    }

    @Test
    fun dashboardMetricsAreDerivedFromReportedState() {
        val now = Instant.parse("2026-11-12T02:00:00Z")
        val server = ServerDto(
            id = 1,
            name = "test-node",
            host = HostDto(memTotal = 8_000, diskTotal = 10_000),
            state = StateDto(cpu = 12.5, memUsed = 3_000, diskUsed = 8_000, netInSpeed = 1024, netOutSpeed = 2048),
            lastActive = now.toString()
        ).toUiModel(Gson(), AppStrings.of(AppLanguage.EN), now, emptySet())

        assertEquals(12.5, server.cpuPercent!!, 0.001)
        assertEquals(37.5, server.memoryPercent!!, 0.001)
        assertEquals(80.0, server.diskPercent!!, 0.001)
        assertEquals("1.0KB/s", server.downloadSpeedText)
        assertEquals("2.0KB/s", server.uploadSpeedText)
    }

    @Test
    fun missingMetricsRemainUnknown() {
        val server = ServerDto().toUiModel(Gson(), AppStrings.of(AppLanguage.EN), Instant.now(), emptySet())

        assertNull(server.cpuPercent)
        assertNull(server.memoryPercent)
        assertNull(server.diskPercent)
        assertNull(server.downloadSpeedText)
        assertNull(server.uploadSpeedText)
    }
}
