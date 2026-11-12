package com.atigger.status.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.atigger.status.data.AppLanguage
import com.atigger.status.data.LiveUpdateMetric
import com.atigger.status.data.MonitorConfig
import com.atigger.status.data.ServerGroupUiModel
import com.atigger.status.data.ServerUiModel
import com.atigger.status.i18n.AppStrings
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

sealed interface StatusUiState {
    data class Loading(
        val stage: StatusLoadingStage,
        val retryCount: Int = 0,
        val maxRetries: Int = 3,
        val requiresLogin: Boolean = false,
        val authDurationMs: Long? = null,
        val groupFallbackActive: Boolean = false,
        val groupsDurationMs: Long? = null,
        val webSocketConnectDurationMs: Long? = null,
        val notice: String? = null
    ) : StatusUiState
    data object SetupRequired : StatusUiState
    data class Error(val message: String) : StatusUiState
    data class Success(
        val servers: List<ServerUiModel>,
        val lastUpdated: String,
        val groups: List<ServerGroupUiModel>
    ) : StatusUiState
}

enum class StatusLoadingStage {
    FetchingToken,
    FetchingGroups,
    ConnectingLiveUpdates,
    WaitingForFirstSnapshot
}

enum class NotificationPrompt {
    REQUEST_PERMISSION,
    OPEN_SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    uiState: StatusUiState,
    favoriteServerId: Int?,
    monitorConfig: MonitorConfig,
    showSettings: Boolean,
    settingsError: String?,
    notificationPrompt: NotificationPrompt?,
    onToggleFavorite: (Int) -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onSaveSettings: (MonitorConfig) -> Unit,
    onCancelSettings: () -> Unit,
    onConfirmNotificationPrompt: () -> Unit,
    onDismissNotificationPrompt: () -> Unit,
    onBack: () -> Unit
) {
    val strings = AppStrings.of(monitorConfig.language)
    val shouldShowSettings = showSettings || !monitorConfig.isConfigured

    BackHandler(onBack = onBack)

    notificationPrompt?.let { prompt ->
        AlertDialog(
            onDismissRequest = onDismissNotificationPrompt,
            title = { Text(strings.notificationPermissionTitle) },
            text = {
                Text(
                    if (prompt == NotificationPrompt.REQUEST_PERMISSION) {
                        strings.notificationPermissionMessage
                    } else {
                        strings.notificationSettingsMessage
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmNotificationPrompt) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissNotificationPrompt) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (shouldShowSettings) {
        SettingsScaffold(
            strings = strings,
            monitorConfig = monitorConfig,
            errorMessage = settingsError,
            canCancel = monitorConfig.isConfigured,
            onSaveSettings = onSaveSettings,
            onCancelSettings = onCancelSettings
        )
        return
    }

    var showOnlineOnly by rememberSaveable { mutableStateOf(false) }
    var selectedGroupId by rememberSaveable { mutableStateOf<Int?>(null) }
    val favoriteServer = (uiState as? StatusUiState.Success)
        ?.servers
        ?.find { it.id == favoriteServerId }
    val groups = (uiState as? StatusUiState.Success)?.groups.orEmpty()
    val selectedTabIndex = groups.indexOfFirst { it.id == selectedGroupId }
        .takeIf { it >= 0 }
        ?.plus(1)
        ?: 0
    LaunchedEffect(groups) {
        if (selectedGroupId != null && groups.none { it.id == selectedGroupId }) {
            selectedGroupId = null
        }
    }
    val state = when (uiState) {
        is StatusUiState.Success -> uiState.copy(
            servers = uiState.servers
                .let { servers -> if (showOnlineOnly) servers.filter { it.isOnline } else servers }
        )
        else -> uiState
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            Column {
                MonitorHeader(strings, onRetry, onOpenSettings)
                if (uiState is StatusUiState.Success && LocalConfiguration.current.screenHeightDp >= 480) {
                    FleetOverview(uiState.servers, uiState.lastUpdated, strings)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.dashboardSubtitle, modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilterChip(
                        selected = showOnlineOnly,
                        onClick = { showOnlineOnly = !showOnlineOnly },
                        label = { Text(strings.online) },
                        shape = RoundedCornerShape(8.dp),
                        border = null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
                if (state is StatusUiState.Success && state.groups.isNotEmpty()) {
                    GroupTabs(state.groups, selectedTabIndex, { selectedGroupId = it }, strings)
                }
            }
        }
    ) { innerPadding ->
        when (state) {
            is StatusUiState.Loading -> LoadingPane(innerPadding, state, strings)
            StatusUiState.SetupRequired -> SetupHintPane(innerPadding, onOpenSettings, strings)
            is StatusUiState.Error -> ErrorPane(innerPadding, state.message, onRetry, strings)
            is StatusUiState.Success -> SwipeableServerListPane(
                innerPadding = innerPadding,
                servers = state.servers,
                groups = state.groups,
                selectedGroupId = selectedGroupId,
                favoriteServer = favoriteServer,
                favoriteServerId = favoriteServerId,
                onToggleFavorite = onToggleFavorite,
                onRetry = onRetry,
                onSelectGroup = { selectedGroupId = it },
                strings = strings
            )
        }
    }
}

@Composable
private fun SwipeableServerListPane(
    innerPadding: PaddingValues,
    servers: List<ServerUiModel>,
    groups: List<ServerGroupUiModel>,
    selectedGroupId: Int?,
    favoriteServer: ServerUiModel?,
    favoriteServerId: Int?,
    onToggleFavorite: (Int) -> Unit,
    onRetry: () -> Unit,
    onSelectGroup: (Int?) -> Unit,
    strings: AppStrings
) {
    val coroutineScope = rememberCoroutineScope()
    var dragOffsetPx by rememberSaveable(selectedGroupId, groups) { mutableStateOf(0f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val pageWidthPx = constraints.maxWidth.toFloat()
        val currentIndex = groups.indexOfFirst { it.id == selectedGroupId } + 1
        val adjacentIndex = when {
            dragOffsetPx < 0f && currentIndex < groups.size -> currentIndex + 1
            dragOffsetPx > 0f && currentIndex > 0 -> currentIndex - 1
            else -> null
        }
        val swipeModifier = if (groups.isNotEmpty() && pageWidthPx > 0f) {
            Modifier.pointerInput(groups, selectedGroupId, pageWidthPx) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val proposedOffset = dragOffsetPx + dragAmount
                        dragOffsetPx = when {
                            proposedOffset < 0f && currentIndex >= groups.size -> 0f
                            proposedOffset > 0f && currentIndex <= 0 -> 0f
                            else -> proposedOffset.coerceIn(-pageWidthPx, pageWidthPx)
                        }
                    },
                    onDragEnd = {
                        val threshold = pageWidthPx * 0.18f
                        val targetIndex = when {
                            dragOffsetPx <= -threshold && currentIndex < groups.size -> currentIndex + 1
                            dragOffsetPx >= threshold && currentIndex > 0 -> currentIndex - 1
                            else -> currentIndex
                        }
                        coroutineScope.launch {
                            val targetOffset = when {
                                targetIndex > currentIndex -> -pageWidthPx
                                targetIndex < currentIndex -> pageWidthPx
                                else -> 0f
                            }
                            animateOffset(from = dragOffsetPx, to = targetOffset) { value ->
                                dragOffsetPx = value
                            }
                            if (targetIndex != currentIndex) {
                                onSelectGroup(groups.getOrNull(targetIndex - 1)?.id)
                            }
                            dragOffsetPx = 0f
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            animateOffset(from = dragOffsetPx, to = 0f) { value ->
                                dragOffsetPx = value
                            }
                            dragOffsetPx = 0f
                        }
                    }
                )
            }
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(swipeModifier)
        ) {
            adjacentIndex?.let { index ->
                val adjacentGroupId = groups.getOrNull(index - 1)?.id
                val adjacentOffset = if (dragOffsetPx < 0f) {
                    pageWidthPx + dragOffsetPx
                } else {
                    -pageWidthPx + dragOffsetPx
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset(adjacentOffset.roundToInt(), 0) }
                ) {
                    ServerListPane(
                        innerPadding = innerPadding,
                        servers = servers,
                        groups = groups,
                        selectedGroupId = adjacentGroupId,
                        favoriteServer = favoriteServer,
                        favoriteServerId = favoriteServerId,
                        onToggleFavorite = onToggleFavorite,
                        onRetry = onRetry,
                        strings = strings
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(dragOffsetPx.roundToInt(), 0) }
            ) {
                ServerListPane(
                    innerPadding = innerPadding,
                    servers = servers,
                    groups = groups,
                    selectedGroupId = selectedGroupId,
                    favoriteServer = favoriteServer,
                    favoriteServerId = favoriteServerId,
                    onToggleFavorite = onToggleFavorite,
                    onRetry = onRetry,
                    strings = strings
                )
            }
        }
    }
}

private suspend fun animateOffset(
    from: Float,
    to: Float,
    onUpdate: (Float) -> Unit
) {
    animate(
        initialValue = from,
        targetValue = to,
        animationSpec = tween(durationMillis = 220)
    ) { value, _ ->
        onUpdate(value)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScaffold(
    strings: AppStrings,
    monitorConfig: MonitorConfig,
    errorMessage: String?,
    canCancel: Boolean,
    onSaveSettings: (MonitorConfig) -> Unit,
    onCancelSettings: () -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("NEZHA / SETTINGS", modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (canCancel) {
                    TextButton(onClick = onCancelSettings) { Text("‹  ${strings.cancel}") }
                }
            }
        }
    ) { innerPadding ->
        MonitorConfigPane(
            strings = strings,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            initialConfig = monitorConfig,
            errorMessage = errorMessage,
            onSaveSettings = onSaveSettings
        )
    }
}

@Composable
private fun MonitorConfigPane(
    strings: AppStrings,
    modifier: Modifier = Modifier,
    initialConfig: MonitorConfig,
    errorMessage: String?,
    onSaveSettings: (MonitorConfig) -> Unit
) {
    val initialUrlDraft = splitMonitorUrlDraft(initialConfig.baseUrl)
    var baseUrlScheme by rememberSaveable(initialConfig.baseUrl) { mutableStateOf(initialUrlDraft.scheme) }
    var baseUrlAddress by rememberSaveable(initialConfig.baseUrl) { mutableStateOf(initialUrlDraft.address) }
    var requiresLogin by rememberSaveable(initialConfig.requiresLogin) { mutableStateOf(initialConfig.requiresLogin) }
    var username by rememberSaveable(initialConfig.username) { mutableStateOf(initialConfig.username) }
    var password by rememberSaveable(initialConfig.password) { mutableStateOf(initialConfig.password) }
    var apiToken by rememberSaveable(initialConfig.apiToken) { mutableStateOf(initialConfig.apiToken) }
    var languageName by rememberSaveable(initialConfig.language.name) { mutableStateOf(initialConfig.language.name) }
    var baseUrlSchemeMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var liveUpdateMetricName by rememberSaveable(initialConfig.liveUpdateMetric.name) {
        mutableStateOf(initialConfig.liveUpdateMetric.name)
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MonitorMark()
        Spacer(Modifier.height(4.dp))
        Text(strings.monitorSettings, style = MaterialTheme.typography.headlineLarge)
        Text(strings.settingsSubtitle, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        FormSectionHeader("01", strings.connection)
        Text(strings.connectionHint, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                OutlinedButton(
                    onClick = { baseUrlSchemeMenuExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) { Text("$baseUrlScheme://") }
                DropdownMenu(
                    expanded = baseUrlSchemeMenuExpanded,
                    onDismissRequest = { baseUrlSchemeMenuExpanded = false }
                ) {
                    listOf("https", "http").forEach { scheme ->
                        DropdownMenuItem(
                            text = { Text("$scheme://") },
                            onClick = {
                                baseUrlScheme = scheme
                                baseUrlSchemeMenuExpanded = false
                            }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = baseUrlAddress,
                onValueChange = { baseUrlAddress = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                label = { Text(strings.monitorUrl) },
                placeholder = { Text("nezha.example.com") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
        }

        FormSectionHeader("02", strings.authenticationSection)
        OutlinedTextField(
            value = apiToken,
            onValueChange = { apiToken = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            label = { Text(strings.apiToken) },
            supportingText = { Text(strings.apiTokenHint) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(strings.requireLogin, style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f))
            Switch(
                checked = requiresLogin,
                onCheckedChange = { requiresLogin = it },
                enabled = apiToken.isBlank()
            )
        }
        if (requiresLogin && apiToken.isBlank()) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                label = { Text(strings.username) }
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                label = { Text(strings.password) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
        }

        FormSectionHeader("03", strings.appearanceSection)
        Text(strings.languageLabel, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = languageName == AppLanguage.ZH.name,
                onClick = { languageName = AppLanguage.ZH.name },
                shape = RoundedCornerShape(9.dp),
                label = { Text(strings.chinese) }
            )
            FilterChip(
                selected = languageName == AppLanguage.EN.name,
                onClick = { languageName = AppLanguage.EN.name },
                shape = RoundedCornerShape(9.dp),
                label = { Text(strings.english) }
            )
        }
        Text(strings.liveUpdateDisplayLabel, style = MaterialTheme.typography.titleSmall)
        Text(strings.liveUpdateDisplayHint, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                LiveUpdateMetric.CPU to strings.metricCpu,
                LiveUpdateMetric.MEMORY to strings.metricMemory,
                LiveUpdateMetric.NETWORK to strings.metricNetwork
            ).forEach { (metric, label) ->
                FilterChip(
                    selected = liveUpdateMetricName == metric.name,
                    onClick = { liveUpdateMetricName = metric.name },
                    shape = RoundedCornerShape(9.dp),
                    label = { Text(label) }
                )
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp))
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                onSaveSettings(
                    MonitorConfig(
                        baseUrl = buildMonitorUrl(baseUrlScheme, baseUrlAddress),
                        requiresLogin = requiresLogin,
                        username = username.trim(),
                        password = password,
                        apiToken = apiToken.trim(),
                        language = AppLanguage.valueOf(languageName),
                        liveUpdateMetric = LiveUpdateMetric.valueOf(liveUpdateMetricName)
                    )
                )
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(strings.saveConfig) }
        Spacer(Modifier.height(24.dp))
    }
}

private data class MonitorUrlDraft(
    val scheme: String,
    val address: String
)

private fun splitMonitorUrlDraft(baseUrl: String): MonitorUrlDraft {
    val trimmed = baseUrl.trim()
    val lowercased = trimmed.lowercase()
    return when {
        lowercased.startsWith("https://") -> MonitorUrlDraft("https", trimmed.substring(8))
        lowercased.startsWith("http://") -> MonitorUrlDraft("http", trimmed.substring(7))
        else -> MonitorUrlDraft("https", trimmed)
    }
}

private fun buildMonitorUrl(scheme: String, address: String): String {
    val trimmed = address.trim()
    if (trimmed.isBlank()) return ""
    val normalizedAddress = when {
        trimmed.lowercase().startsWith("https://") -> trimmed.substring(8)
        trimmed.lowercase().startsWith("http://") -> trimmed.substring(7)
        else -> trimmed
    }
    return "$scheme://$normalizedAddress"
}

@Composable
private fun LoadingPane(
    innerPadding: PaddingValues,
    state: StatusUiState.Loading,
    strings: AppStrings
) {
    val stage = state.stage
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            MonitorMark(modifier = Modifier.padding(bottom = 12.dp))
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            Text(
                text = stage.title(strings),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stage.detail(strings),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (state.retryCount > 0) {
                Text(
                    text = strings.loadingRetry(state.retryCount, state.maxRetries),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (!state.notice.isNullOrBlank()) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text(
                        text = state.notice,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (state.requiresLogin) {
                        LoadingStepRow(
                            label = buildStepLabel(strings.loadingStepFetchingToken, state.authDurationMs),
                            state = when (stage) {
                                StatusLoadingStage.FetchingToken -> LoadingStepState.Active
                                StatusLoadingStage.FetchingGroups,
                                StatusLoadingStage.ConnectingLiveUpdates,
                                StatusLoadingStage.WaitingForFirstSnapshot -> LoadingStepState.Done
                            },
                            strings = strings
                        )
                    }
                    LoadingStepRow(
                        label = buildStepLabel(strings.loadingStepFetchingGroups, state.groupsDurationMs),
                        state = when (stage) {
                            StatusLoadingStage.FetchingToken -> LoadingStepState.Pending
                            StatusLoadingStage.FetchingGroups -> LoadingStepState.Active
                            StatusLoadingStage.ConnectingLiveUpdates,
                            StatusLoadingStage.WaitingForFirstSnapshot -> {
                                if (state.groupFallbackActive) LoadingStepState.Fallback
                                else LoadingStepState.Done
                            }
                        },
                        strings = strings
                    )
                    LoadingStepRow(
                        label = buildStepLabel(
                            strings.loadingStepConnectingWs,
                            state.webSocketConnectDurationMs
                        ),
                        state = when (stage) {
                            StatusLoadingStage.FetchingToken,
                            StatusLoadingStage.FetchingGroups -> LoadingStepState.Pending
                            StatusLoadingStage.ConnectingLiveUpdates -> LoadingStepState.Active
                            StatusLoadingStage.WaitingForFirstSnapshot -> LoadingStepState.Done
                        },
                        strings = strings
                    )
                    LoadingStepRow(
                        label = strings.loadingStepWaitingFirstFrame,
                        state = when (stage) {
                            StatusLoadingStage.WaitingForFirstSnapshot -> LoadingStepState.Active
                            StatusLoadingStage.FetchingToken,
                            StatusLoadingStage.FetchingGroups,
                            StatusLoadingStage.ConnectingLiveUpdates -> LoadingStepState.Pending
                        },
                        strings = strings
                    )
                }
            }
        }
    }
}

private enum class LoadingStepState {
    Done,
    Active,
    Pending,
    Fallback
}

@Composable
private fun LoadingStepRow(
    label: String,
    state: LoadingStepState,
    strings: AppStrings
) {
    val color = when (state) {
        LoadingStepState.Done -> MaterialTheme.colorScheme.secondary
        LoadingStepState.Active -> MaterialTheme.colorScheme.primary
        LoadingStepState.Pending -> MaterialTheme.colorScheme.outline
        LoadingStepState.Fallback -> Color(0xFFB26A00)
    }
    val statusText = when (state) {
        LoadingStepState.Done -> strings.loadingStatusDone
        LoadingStepState.Active -> strings.loadingStatusActive
        LoadingStepState.Pending -> strings.loadingStatusPending
        LoadingStepState.Fallback -> strings.loadingStatusFallback
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelLarge,
            color = color
        )
    }
}

private fun buildStepLabel(label: String, durationMs: Long?): String {
    val durationText = durationMs?.let(::formatStepDuration) ?: return label
    return "$label  $durationText"
}

private fun formatStepDuration(durationMs: Long): String {
    return when {
        durationMs < 1000 -> "${durationMs}ms"
        durationMs < 10_000 -> String.format("%.1fs", durationMs / 1000.0)
        else -> "${durationMs / 1000}s"
    }
}

private fun StatusLoadingStage.title(strings: AppStrings): String = when (this) {
    StatusLoadingStage.FetchingToken -> strings.loadingStageFetchingTokenTitle
    StatusLoadingStage.FetchingGroups -> strings.loadingStageFetchingGroupsTitle
    StatusLoadingStage.ConnectingLiveUpdates -> strings.loadingStageConnectingWsTitle
    StatusLoadingStage.WaitingForFirstSnapshot -> strings.loadingStageWaitingFirstFrameTitle
}

private fun StatusLoadingStage.detail(strings: AppStrings): String = when (this) {
    StatusLoadingStage.FetchingToken -> strings.loadingStageFetchingTokenDetail
    StatusLoadingStage.FetchingGroups -> strings.loadingStageFetchingGroupsDetail
    StatusLoadingStage.ConnectingLiveUpdates -> strings.loadingStageConnectingWsDetail
    StatusLoadingStage.WaitingForFirstSnapshot -> strings.loadingStageWaitingFirstFrameDetail
}

@Composable
private fun SetupHintPane(innerPadding: PaddingValues, onOpenSettings: () -> Unit, strings: AppStrings) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(strings.noMonitorConfig, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = strings.setupHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(onClick = onOpenSettings) {
                Text(strings.openSettings)
            }
        }
    }
}

@Composable
private fun ErrorPane(
    innerPadding: PaddingValues,
    message: String,
    onRetry: () -> Unit,
    strings: AppStrings
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(strings.loadFailed, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(onClick = onRetry) {
                Text(strings.retry)
            }
        }
    }
}

@Composable
private fun ServerListPane(
    innerPadding: PaddingValues,
    servers: List<ServerUiModel>,
    groups: List<ServerGroupUiModel>,
    selectedGroupId: Int?,
    favoriteServer: ServerUiModel?,
    favoriteServerId: Int?,
    onToggleFavorite: (Int) -> Unit,
    onRetry: () -> Unit,
    strings: AppStrings
) {
    val visibleServers = selectedGroupId
        ?.let { groupId -> servers.filter { groupId in it.groupIds } }
        ?: servers
    val listState = rememberLazyListState()
    LaunchedEffect(selectedGroupId) {
        listState.scrollToItem(0)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (selectedGroupId == null) {
            item {
                PinnedServerStrip(
                    favoriteServer = favoriteServer,
                    favoriteServerId = favoriteServerId,
                    onToggleFavorite = onToggleFavorite,
                    strings = strings
                )
            }
        }
        item {
            Text(
                text = strings.nodes(visibleServers.size),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (visibleServers.isEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = strings.noNodes,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(onClick = onRetry) {
                            Text(strings.reconnect)
                        }
                    }
                }
            }
        } else if (groups.isNotEmpty()) {
            val visibleGroups = groups.filter { group ->
                selectedGroupId == null || selectedGroupId == group.id
            }
            visibleGroups.forEach { group ->
                val groupServers = visibleServers.filter { server -> group.id in server.groupIds }
                if (groupServers.isNotEmpty()) {
                    item(key = "group-${group.id}") {
                        GroupHeader(group.name)
                    }
                    items(groupServers, key = { "${group.id}-${it.id}" }) { server ->
                        ServerRow(
                            server = server,
                            isFavorite = favoriteServerId == server.id,
                            onToggleFavorite = onToggleFavorite,
                            strings = strings
                        )
                    }
                }
            }

            val ungroupedServers = visibleServers.filter { it.groupIds.isEmpty() }
            if (selectedGroupId == null && ungroupedServers.isNotEmpty()) {
                item(key = "group-ungrouped") {
                    GroupHeader(strings.ungrouped)
                }
                items(ungroupedServers, key = { "ungrouped-${it.id}" }) { server ->
                    ServerRow(
                        server = server,
                        isFavorite = favoriteServerId == server.id,
                        onToggleFavorite = onToggleFavorite,
                        strings = strings
                    )
                }
            }
        } else {
            items(visibleServers, key = { it.id }) { server ->
                ServerRow(
                    server = server,
                    isFavorite = favoriteServerId == server.id,
                    onToggleFavorite = onToggleFavorite,
                    strings = strings
                )
            }
        }
    }
}

@Composable
private fun GroupHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(4.dp, 14.dp).clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.primary))
        Text(title, style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GroupTabs(
    groups: List<ServerGroupUiModel>,
    selectedTabIndex: Int,
    onSelectGroup: (Int?) -> Unit,
    strings: AppStrings
) {
    GroupNavigation(groups, selectedTabIndex, onSelectGroup, strings)
}
