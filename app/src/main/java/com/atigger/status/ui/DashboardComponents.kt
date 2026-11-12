package com.atigger.status.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atigger.status.R
import com.atigger.status.data.ServerGroupUiModel
import com.atigger.status.data.ServerUiModel
import com.atigger.status.i18n.AppStrings
import java.util.Locale

@Composable
internal fun MonitorHeader(
    strings: AppStrings,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MonitorMark()
        Column(modifier = Modifier.weight(1f)) {
            Text("NEZHA / MONITOR", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp)
            Text(strings.dashboardTitle, style = MaterialTheme.typography.headlineSmall)
        }
        IconButton(
            onClick = onRetry,
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        ) {
            Icon(painterResource(R.drawable.ic_refresh), strings.refresh, modifier = Modifier.size(20.dp))
        }
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        ) {
            Icon(painterResource(R.drawable.ic_settings), strings.settings, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
internal fun MonitorMark(modifier: Modifier = Modifier) {
    val foreground = MaterialTheme.colorScheme.onPrimary
    Canvas(modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary)) {
        val waveform = Path().apply {
            moveTo(size.width * 0.18f, size.height * 0.54f)
            lineTo(size.width * 0.34f, size.height * 0.54f)
            lineTo(size.width * 0.43f, size.height * 0.28f)
            lineTo(size.width * 0.56f, size.height * 0.73f)
            lineTo(size.width * 0.66f, size.height * 0.46f)
            lineTo(size.width * 0.82f, size.height * 0.46f)
        }
        drawPath(waveform, foreground, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
internal fun FleetOverview(servers: List<ServerUiModel>, lastUpdated: String, strings: AppStrings) {
    val online = servers.count { it.isOnline }
    val offline = servers.size - online
    val muted = Color(0xFFAEBBD5)
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            .fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF1C2842))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(strings.fleetTitle, color = muted, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f))
            StatusDot(Color(0xFF71DDB3))
            Spacer(Modifier.width(6.dp))
            Text(strings.updated(lastUpdated), color = muted, style = MaterialTheme.typography.labelSmall)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            FleetCount(servers.size, strings.totalNodes, Color.White, Modifier.weight(1f))
            FleetCount(online, strings.onlineStatus, Color(0xFF75E0B8), Modifier.weight(1f))
            FleetCount(offline, strings.offlineStatus, Color(0xFFF5B99C), Modifier.weight(1f))
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(3.dp)) {
            drawLine(Color.White.copy(alpha = 0.12f), Offset.Zero, Offset(size.width, 0f),
                strokeWidth = size.height, cap = StrokeCap.Round)
            if (servers.isNotEmpty() && online > 0) {
                drawLine(Color(0xFF75E0B8), Offset.Zero, Offset(size.width * online / servers.size, 0f),
                    strokeWidth = size.height, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun FleetCount(count: Int, label: String, color: Color, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(count.toString().padStart(2, '0'), color = color,
            style = MaterialTheme.typography.displaySmall, fontFamily = FontFamily.Monospace)
        Text(label, color = Color(0xFFAEBBD5), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun GroupNavigation(
    groups: List<ServerGroupUiModel>,
    selectedTabIndex: Int,
    onSelectGroup: (Int?) -> Unit,
    strings: AppStrings
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        GroupOption(strings.allGroups, selectedTabIndex == 0) { onSelectGroup(null) }
        groups.forEachIndexed { index, group ->
            GroupOption(group.name, selectedTabIndex == index + 1) { onSelectGroup(group.id) }
        }
    }
}

@Composable
private fun GroupOption(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.clip(RoundedCornerShape(9.dp))
            .background(if (selected) colors.primary else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) colors.onPrimary else colors.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun PinnedServerStrip(
    favoriteServer: ServerUiModel?,
    favoriteServerId: Int?,
    onToggleFavorite: (Int) -> Unit,
    strings: AppStrings
) {
    if (favoriteServerId == null) {
        Text(strings.followCompactHint, modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer)
            .padding(start = 22.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("★", color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(strings.followedNode, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(favoriteServer?.name ?: strings.followMissingHint, maxLines = 2,
                style = MaterialTheme.typography.titleSmall, overflow = TextOverflow.Ellipsis)
            favoriteServer?.networkCriticalText?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        TextButton(onClick = { onToggleFavorite(favoriteServerId) }) { Text(strings.unfollow) }
    }
}

@Composable
internal fun ServerRow(
    server: ServerUiModel,
    isFavorite: Boolean,
    onToggleFavorite: (Int) -> Unit,
    strings: AppStrings
) {
    var expanded by rememberSaveable(server.id) { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().background(colors.surface)) {
        Column(modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusDot(if (server.isOnline) colors.secondary else colors.error)
                Column(modifier = Modifier.weight(1f)) {
                    Text(server.name, style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(server.locationTag, server.platformLine).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(if (server.isOnline) strings.onlineStatus else strings.offlineStatus,
                    color = if (server.isOnline) colors.secondary else colors.error,
                    style = MaterialTheme.typography.labelMedium)
                IconButton(
                    onClick = { onToggleFavorite(server.id) },
                    modifier = Modifier.size(48.dp).semantics {
                        contentDescription = if (isFavorite) strings.unfollow else strings.follow
                    }
                ) {
                    Text(if (isFavorite) "★" else "☆", fontSize = 24.sp,
                        color = if (isFavorite) colors.primary else colors.outline)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ResourceMeter(strings.cpu, server.cpuPercent.takeIf { server.isOnline }, Modifier.weight(1f))
                ResourceMeter(strings.memory, server.memoryPercent.takeIf { server.isOnline }, Modifier.weight(1f))
                ResourceMeter(strings.disk, server.diskPercent.takeIf { server.isOnline }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("↓ ${server.downloadSpeedText.takeIf { server.isOnline } ?: "—"}   ↑ ${server.uploadSpeedText.takeIf { server.isOnline } ?: "—"}",
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) strings.hideDetails else strings.showDetails,
                        style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(6.dp))
                    Chevron(expanded)
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                    HorizontalDivider(color = colors.outlineVariant, modifier = Modifier.padding(bottom = 10.dp))
                    DetailLine(strings.cpu, server.cpuLine)
                    DetailLine(strings.usage, server.usageLine)
                    DetailLine(strings.network, server.networkLine)
                    DetailLine(strings.traffic, server.trafficLine)
                    DetailLine(strings.connectionLabel, server.connectionLine)
                    server.uptimeLine?.let { DetailLine(strings.uptime, it) }
                    server.ipLine?.let { DetailLine(strings.ip, it) }
                    server.planLine?.let { DetailLine(strings.plan, it) }
                    server.billingLine?.let { DetailLine(strings.billing, it) }
                    server.versionTag?.let { DetailLine("Agent", it) }
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 22.dp), color = colors.outlineVariant)
    }
}

@Composable
private fun ResourceMeter(label: String, percent: Double?, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val color = when {
        percent == null -> colors.outline
        percent >= 90 -> colors.error
        percent >= 70 -> Color(0xFFB57B22)
        else -> colors.primary
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Text(percent?.let { String.format(Locale.US, "%.1f%%", it) } ?: "—",
            style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace,
            color = if (percent == null) colors.outline else colors.onSurface)
        Canvas(modifier = Modifier.fillMaxWidth().height(3.dp)) {
            drawLine(colors.outlineVariant, Offset.Zero, Offset(size.width, 0f),
                strokeWidth = size.height, cap = StrokeCap.Round)
            if (percent != null && percent.isFinite() && percent > 0) {
                drawLine(color, Offset.Zero, Offset(size.width * (percent.coerceIn(0.0, 100.0) / 100).toFloat(), 0f),
                    strokeWidth = size.height, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(Modifier.size(7.dp).clip(CircleShape).background(color))
}

@Composable
private fun Chevron(expanded: Boolean) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(10.dp)) {
        val direction = if (expanded) -1f else 1f
        val path = Path().apply {
            moveTo(0f, size.height * (0.5f - direction * 0.2f))
            lineTo(size.width / 2, size.height * (0.5f + direction * 0.2f))
            lineTo(size.width, size.height * (0.5f - direction * 0.2f))
        }
        drawPath(path, color, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.width(52.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
        Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun FormSectionHeader(number: String, title: String) {
    Row(modifier = Modifier.padding(top = 22.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(number, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.labelMedium)
        Text(title, style = MaterialTheme.typography.titleMedium)
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}
