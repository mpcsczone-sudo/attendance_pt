package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Approval
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceRecordDto
import com.example.data.model.StaffProfile
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftWhiteTint
import com.example.ui.theme.SubtleBorder
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

data class WeeklyAttendanceBucket(
    val label: String,
    val presentCount: Int,
    val absentCount: Int
)

@Composable
fun LiveShiftStatusBanner(
    subject: StaffProfile,
    todayRecord: AttendanceRecordDto?,
    viewingOtherStaff: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(30_000L)
        }
    }

    val isCheckedIn = todayRecord != null && !todayRecord.checkInTime.isNullOrBlank()
    val isCheckedOut = todayRecord != null && !todayRecord.checkOutTime.isNullOrBlank()

    val shiftProgress = remember(subject.shiftStartTime, subject.shiftEndTime, nowMillis, isCheckedIn, isCheckedOut) {
        if (isCheckedOut) 1f
        else computeShiftProgress(subject.shiftStartTime, subject.shiftEndTime, nowMillis)
    }
    val elapsedLabel = remember(todayRecord?.checkInTime, todayRecord?.checkOutTime, nowMillis) {
        formatDutyElapsed(todayRecord?.checkInTime, todayRecord?.checkOutTime, nowMillis)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, SubtleBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!viewingOtherStaff) Modifier.clickable(onClick = onActionClick) else Modifier
            )
            .testTag("dashboard_live_shift_banner")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = when {
                            isCheckedOut -> BlueContainer
                            isCheckedIn -> EmeraldContainer
                            else -> AmberContainer
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = "Shift Status",
                                tint = when {
                                    isCheckedOut -> MaterialTheme.colorScheme.primary
                                    isCheckedIn -> EmeraldSuccess
                                    else -> AmberWarning
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = when {
                                isCheckedOut -> "Shift Completed (${todayRecord?.checkInTime ?: "--"} → ${todayRecord?.checkOutTime ?: "--"})"
                                isCheckedIn -> "Active On-Duty • $elapsedLabel"
                                viewingOtherStaff -> "No Check-In Recorded Today"
                                else -> "Not Checked In Today • Tap to Check-In"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Mapped Shift: ${subject.shiftStartTime} – ${subject.shiftEndTime} (${subject.loginId})",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusPill(
                    label = when {
                        isCheckedOut -> "OUT"
                        isCheckedIn -> todayRecord?.status ?: "PRESENT"
                        else -> "PENDING"
                    }
                )
            }

            LinearProgressIndicator(
                progress = { shiftProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp),
                color = when {
                    isCheckedOut -> MaterialTheme.colorScheme.primary
                    isCheckedIn -> EmeraldSuccess
                    else -> AmberWarning
                },
                trackColor = SoftWhiteTint
            )
        }
    }
}

@Composable
fun AttendanceRateDonutGauge(
    attendancePercentage: Double,
    presentStreak: Int,
    longestStreak: Int,
    avgCheckInLabel: String,
    totalDutyHoursLabel: String,
    onTimeRatePct: Int,
    presentDaysInMonth: Int = 0,
    totalDaysInMonth: Int = 0,
    modifier: Modifier = Modifier
) {
    val targetSweep = ((attendancePercentage.toFloat() / 100f).coerceIn(0f, 1f)) * 360f
    val animatedSweep by animateFloatAsState(
        targetValue = targetSweep,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "attendance_ring_sweep"
    )
    val ringColor = when {
        attendancePercentage >= 75.0 -> EmeraldSuccess
        attendancePercentage >= 50.0 -> AmberWarning
        attendancePercentage > 0.0 -> CrimsonError
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        color = SoftWhiteTint,
        border = BorderStroke(1.dp, SubtleBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_donut_gauge")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                Canvas(modifier = Modifier.size(80.dp)) {
                    val strokePx = 9.dp.toPx()
                    val diameter = minOf(size.width, size.height) - strokePx
                    val topLeft = Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f
                    )
                    val arcSize = Size(diameter, diameter)

                    drawArc(
                        color = SubtleBorder,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                    if (animatedSweep > 0f) {
                        drawArc(
                            color = ringColor,
                            startAngle = -90f,
                            sweepAngle = animatedSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val pctLabel = if (attendancePercentage % 1.0 == 0.0) {
                        "${attendancePercentage.toInt()}%"
                    } else {
                        String.format(Locale.US, "%.1f%%", attendancePercentage)
                    }
                    Text(
                        text = pctLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = ringColor
                    )
                    Text(
                        text = if (totalDaysInMonth > 0) "$presentDaysInMonth/$totalDaysInMonth d" else "Rate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (totalDaysInMonth > 0) {
                    InsightLine(
                        icon = Icons.Filled.TrendingUp,
                        tint = ringColor,
                        label = "Month Rate",
                        value = "$presentDaysInMonth / $totalDaysInMonth d"
                    )
                }
                InsightLine(
                    icon = Icons.Filled.LocalFireDepartment,
                    tint = AmberWarning,
                    label = "Current Streak",
                    value = "$presentStreak d"
                )
                InsightLine(
                    icon = Icons.Filled.EmojiEvents,
                    tint = MaterialTheme.colorScheme.primary,
                    label = "Best Streak",
                    value = "$longestStreak d"
                )
                InsightLine(
                    icon = Icons.Filled.AccessTime,
                    tint = MaterialTheme.colorScheme.primary,
                    label = "Avg Check-In",
                    value = avgCheckInLabel
                )
                InsightLine(
                    icon = Icons.Filled.Timer,
                    tint = EmeraldSuccess,
                    label = "Logged Hours",
                    value = totalDutyHoursLabel
                )
                InsightLine(
                    icon = Icons.Filled.Fingerprint,
                    tint = EmeraldSuccess,
                    label = "On-Time Ratio",
                    value = "$onTimeRatePct%"
                )
            }
        }
    }
}

@Composable
fun WeeklyAttendanceBarChart(
    buckets: List<WeeklyAttendanceBucket>,
    modifier: Modifier = Modifier
) {
    val maxDayCount = remember(buckets) {
        (buckets.maxOfOrNull { maxOf(it.presentCount, it.absentCount) } ?: 1).coerceAtLeast(1)
    }

    Surface(
        color = SoftWhiteTint,
        border = BorderStroke(1.dp, SubtleBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_weekly_chart")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly P vs A Trend",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(EmeraldSuccess)
                        )
                        Text("P", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(CrimsonError)
                        )
                        Text("A", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (buckets.isEmpty()) {
                Text(
                    text = "No weekly data for this month yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(82.dp)
                ) {
                    val slotWidth = size.width / buckets.size.coerceAtLeast(1)
                    val barWidth = (slotWidth * 0.24f).coerceIn(10f, 26f)
                    val gap = 6f
                    val chartHeight = size.height - 6f

                    drawLine(
                        color = SubtleBorder,
                        start = Offset(0f, chartHeight),
                        end = Offset(size.width, chartHeight),
                        strokeWidth = 2f
                    )

                    buckets.forEachIndexed { index, bucket ->
                        val centerX = slotWidth * index + (slotWidth / 2f)
                        val pHeight = (bucket.presentCount.toFloat() / maxDayCount.toFloat()) * (chartHeight - 4f)
                        val aHeight = (bucket.absentCount.toFloat() / maxDayCount.toFloat()) * (chartHeight - 4f)

                        if (bucket.presentCount > 0) {
                            drawRoundRect(
                                color = EmeraldSuccess,
                                topLeft = Offset(centerX - barWidth - (gap / 2f), chartHeight - pHeight),
                                size = Size(barWidth, pHeight.coerceAtLeast(6f)),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }
                        if (bucket.absentCount > 0) {
                            drawRoundRect(
                                color = CrimsonError.copy(alpha = 0.85f),
                                topLeft = Offset(centerX + (gap / 2f), chartHeight - aHeight),
                                size = Size(barWidth, aHeight.coerceAtLeast(6f)),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    buckets.forEach { bucket ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = bucket.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${bucket.presentCount}P/${bucket.absentCount}A",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBreakdownStackedBar(
    present: Int,
    late: Int,
    onDuty: Int,
    halfDay: Int,
    absent: Int,
    modifier: Modifier = Modifier
) {
    val total = (present + late + onDuty + halfDay + absent).coerceAtLeast(0)
    val primaryColor = MaterialTheme.colorScheme.primary
    val segments = listOf(
        Triple("Present", present, EmeraldSuccess),
        Triple("On-Duty", onDuty, primaryColor),
        Triple("Late", late, AmberWarning),
        Triple("Half", halfDay, Color(0xFF7C3AED)),
        Triple("Absent", absent, CrimsonError)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Status Mix",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(SoftWhiteTint)
        ) {
            if (total == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(SubtleBorder)
                )
            } else {
                segments.forEach { (_, count, color) ->
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .weight(count.toFloat())
                                .fillMaxHeight()
                                .background(color)
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            segments.filter { it.second > 0 }.forEach { (label, count, color) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                    Text(
                        text = "$label $count",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AttendanceBenchmarkCard(
    presentCount: Int,
    absentCount: Int,
    totalDaysInMonth: Int = 0,
    targetPercent: Int = 75,
    modifier: Modifier = Modifier
) {
    val denominator = if (totalDaysInMonth > 0) totalDaysInMonth else (presentCount + absentCount)
    val currentPct = if (denominator > 0) (presentCount * 100.0) / denominator else 0.0
    val onTrack = denominator == 0 || currentPct >= targetPercent
    val daysNeeded = if (totalDaysInMonth > 0) {
        val targetPresentCount = ceil((targetPercent.toDouble() / 100.0) * totalDaysInMonth).toInt()
        (targetPresentCount - presentCount).coerceAtLeast(0)
    } else {
        computeRequiredPresentDaysForTarget(presentCount, absentCount, targetPercent)
    }
    val formattedPct = String.format(Locale.US, "%.1f%%", currentPct)

    Surface(
        color = when {
            denominator == 0 -> SoftWhiteTint
            onTrack -> EmeraldContainer.copy(alpha = 0.65f)
            else -> AmberContainer
        },
        border = BorderStroke(1.dp, SubtleBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_benchmark_card")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.TrendingUp,
                contentDescription = "Attendance Benchmark",
                tint = when {
                    denominator == 0 -> MaterialTheme.colorScheme.primary
                    onTrack -> EmeraldSuccess
                    else -> AmberWarning
                },
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        denominator == 0 -> "College $targetPercent% Benchmark Ready"
                        onTrack -> "On Track • $formattedPct ($presentCount/$denominator days)"
                        else -> "Needs $daysNeeded more Present day(s) for $targetPercent% of month"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        denominator == 0 -> MaterialTheme.colorScheme.onSurface
                        onTrack -> EmeraldSuccess
                        else -> AmberWarning
                    }
                )
                Text(
                    text = if (totalDaysInMonth > 0) {
                        "Month Rate: $presentCount Present out of $totalDaysInMonth days ($formattedPct)"
                    } else if (denominator == 0) {
                        "Live ratio updates automatically as P/A dates accumulate."
                    } else {
                        "Tracked $presentCount P out of $denominator elapsed day(s) this month."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InsightLine(
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DynamicQuickPulseRow(
    todayCheckedIn: Boolean,
    pendingLeavesCount: Int,
    pendingApprovalsCount: Int,
    isHodOrAdmin: Boolean,
    adminNoticesCount: Int,
    reminderEnabled: Boolean,
    onOpenAttendance: () -> Unit,
    onOpenLeaves: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PulseActionTile(
            icon = Icons.Filled.Fingerprint,
            title = "Attendance",
            subtitle = if (todayCheckedIn) "Checked In" else "Mark Now",
            accent = if (todayCheckedIn) EmeraldSuccess else AmberWarning,
            bg = if (todayCheckedIn) EmeraldContainer else AmberContainer,
            onClick = onOpenAttendance,
            modifier = Modifier
                .weight(1f)
                .testTag("pulse_tile_attendance")
        )
        PulseActionTile(
            icon = Icons.Filled.Approval,
            title = "Leaves",
            subtitle = if (isHodOrAdmin && pendingApprovalsCount > 0) {
                "$pendingApprovalsCount Queue"
            } else {
                "$pendingLeavesCount Pending"
            },
            accent = MaterialTheme.colorScheme.primary,
            bg = BlueContainer,
            onClick = onOpenLeaves,
            modifier = Modifier
                .weight(1f)
                .testTag("pulse_tile_leaves")
        )
        PulseActionTile(
            icon = Icons.Filled.NotificationsActive,
            title = "Notices",
            subtitle = "$adminNoticesCount Msg • ${if (reminderEnabled) "10m On" else "Paused"}",
            accent = if (adminNoticesCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            bg = if (adminNoticesCount > 0) BlueContainer else SoftWhiteTint,
            onClick = onOpenNotifications,
            modifier = Modifier
                .weight(1f)
                .testTag("pulse_tile_notifications")
        )
    }
}

@Composable
private fun PulseActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    bg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, SubtleBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = bg,
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun parseMinutesOfDay(timeStr: String?): Int? {
    if (timeStr.isNullOrBlank()) return null
    val parts = timeStr.trim().split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
    return (hour * 60) + minute
}

private fun computeShiftProgress(shiftStart: String, shiftEnd: String, nowMillis: Long): Float {
    val startMin = parseMinutesOfDay(shiftStart) ?: (9 * 60 + 30)
    val endMin = parseMinutesOfDay(shiftEnd) ?: (16 * 60 + 30)
    if (endMin <= startMin) return 0f
    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    return ((nowMin - startMin).toFloat() / (endMin - startMin).toFloat()).coerceIn(0f, 1f)
}

private fun formatDutyElapsed(checkIn: String?, checkOut: String?, nowMillis: Long): String {
    val inMin = parseMinutesOfDay(checkIn) ?: return "Active"
    val endMin = parseMinutesOfDay(checkOut) ?: run {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }
    val diff = (endMin - inMin).coerceAtLeast(0)
    val hrs = diff / 60
    val mins = diff % 60
    return if (hrs > 0) "${hrs}h ${mins}m elapsed" else "${mins}m elapsed"
}

fun computeAverageCheckIn(records: List<AttendanceRecordDto>): String {
    val minutesList = records.mapNotNull { parseMinutesOfDay(it.checkInTime) }
    if (minutesList.isEmpty()) return "--:--"
    val avg = minutesList.sum() / minutesList.size
    return String.format(Locale.US, "%02d:%02d", avg / 60, avg % 60)
}

fun computeOnTimeRate(records: List<AttendanceRecordDto>): Int {
    val checkedIn = records.filter {
        !it.checkInTime.isNullOrBlank() ||
            it.status.uppercase() in setOf("PRESENT", "LATE", "ON_DUTY", "HALF_DAY")
    }
    if (checkedIn.isEmpty()) return 0
    val onTime = checkedIn.count {
        it.status.uppercase() in setOf("PRESENT", "ON_DUTY")
    }
    return ((onTime.toFloat() / checkedIn.size.toFloat()) * 100f).toInt()
}

fun computeLongestPresentStreak(marks: List<String>): Int {
    var best = 0
    var current = 0
    for (mark in marks) {
        if (mark == "P") {
            current++
            if (current > best) best = current
        } else if (mark == "A") {
            current = 0
        }
    }
    return best
}

fun computeTotalDutyHoursLabel(records: List<AttendanceRecordDto>): String {
    var totalMinutes = 0
    for (rec in records) {
        val inMin = parseMinutesOfDay(rec.checkInTime) ?: continue
        val outMin = parseMinutesOfDay(rec.checkOutTime) ?: continue
        if (outMin > inMin) {
            totalMinutes += (outMin - inMin)
        }
    }
    if (totalMinutes == 0) return "--"
    val hrs = totalMinutes / 60
    val mins = totalMinutes % 60
    return if (mins > 0) "${hrs}h ${mins}m" else "${hrs}h"
}

fun computeRequiredPresentDaysForTarget(
    present: Int,
    absent: Int,
    targetPercent: Int = 75
): Int {
    val total = present + absent
    if (total == 0) return 0
    val targetFraction = targetPercent.toDouble() / 100.0
    if ((present.toDouble() / total.toDouble()) >= targetFraction) return 0
    val denominator = 1.0 - targetFraction
    if (denominator <= 0.0) return 0
    val needed = ceil(((targetFraction * total) - present) / denominator).toInt()
    return needed.coerceAtLeast(1)
}
