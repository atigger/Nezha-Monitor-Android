package com.atigger.status.data

import okhttp3.Request

internal fun Request.Builder.monitorAuthentication(
    config: MonitorConfig,
    authToken: String?
): Request.Builder = apply {
    removeHeader("Authorization")
    removeHeader("Cookie")
    val personalAccessToken = config.apiToken.trim().takeIf { it.isNotEmpty() }
    val token = personalAccessToken ?: authToken?.takeIf { config.usesPasswordLogin && it.isNotBlank() }
    if (token != null) {
        header("Authorization", "Bearer $token")
        if (personalAccessToken == null) {
            header("Cookie", "nz-jwt=$token")
        }
    }
}
