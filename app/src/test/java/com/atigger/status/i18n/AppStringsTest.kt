package com.atigger.status.i18n

import com.atigger.status.data.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppStringsTest {
    @Test
    fun networkCriticalTextUsesDirectionalArrowsInBothLanguages() {
        for (language in AppLanguage.entries) {
            assertEquals(
                "↓1.0KB/s ↑2.0KB/s",
                AppStrings.of(language).networkCriticalText("1.0KB/s", "2.0KB/s")
            )
        }
    }

    @Test
    fun networkCriticalTextOmitsMissingDirections() {
        val strings = AppStrings.of(AppLanguage.EN)

        assertEquals("↓1.0KB/s", strings.networkCriticalText("1.0KB/s", null))
        assertEquals("↑2.0KB/s", strings.networkCriticalText(null, "2.0KB/s"))
    }

    @Test
    fun networkCriticalTextRemainsNullWithoutSpeeds() {
        assertNull(AppStrings.of(AppLanguage.EN).networkCriticalText(null, null))
    }

    @Test
    fun networkCriticalTextPreservesZeroSpeeds() {
        assertEquals(
            "↓0B/s ↑0B/s",
            AppStrings.of(AppLanguage.ZH).networkCriticalText("0B/s", "0B/s")
        )
    }
}
