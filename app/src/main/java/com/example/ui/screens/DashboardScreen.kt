package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AttendanceRecordDto
import com.example.data.model.StaffProfile
import com.example.ui.components.AttendanceBenchmarkCard
import com.example.ui.components.AttendanceRateDonutGauge
import com.example.ui.components.DynamicQuickPulseRow
import com.example.ui.components.FeedbackBanner
import com.example.ui.components.LiveShiftStatusBanner
import com.example.ui.components.StatusBreakdownStackedBar
import com.example.ui.components.StatusPill
import com.example.ui.components.WeeklyAttendanceBarChart
import com.example.ui.components.WeeklyAttendanceBucket
import com.example.ui.components.computeAverageCheckIn
import com.example.ui.components.computeLongestPresentStreak
import com.example.ui.components.computeOnTimeRate
import com.example.ui.components.computeTotalDutyHoursLabel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.RoyalBluePrimary
import com.example.ui.theme.SoftWhiteTint
import com.example.ui.theme.SubtleBorder
import com.example.ui.viewmodel.DashboardUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class CalendarFilterMode(val label: String) {
    ALL("All"),
    PRESENT("Present (P)"),
    LATE("Late"),
    ABSENT("Absent (A)")
}

private data class MonthDayAttendanceCell(
    val dayOfMonth: Int,
    val dateIso: String,
    val mark: String, // "P", "A", or "-"
    val isToday: Boolean,
    val record: AttendanceRecordDto?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onChangeMonth: (deltaMonths: Int) -> Unit,
    onJumpToCurrentMonth: () -> Unit,
    onSelectDashboardStaff: (StaffProfile?) -> Unit,
    onRefresh: () -> Unit,
    onOpenAttendanceTab: () -> Unit,
    onOpenLeavesTab: () -> Unit,
    onOpenNotificationsTab: () -> Unit,
    onOpenServerSetup: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val staff = uiState.staff
    val isAdmin = staff?.role?.equals("ADMIN", ignoreCase = true) == true
    val isHodOrAdmin = staff?.role?.equals("HOD", ignoreCase = true) == true || isAdmin
    val selectedTargetStaff = if (isAdmin) uiState.selectedDashboardStaff else null
    val activeSubject = selectedTargetStaff ?: staff
    val viewingOtherStaff = isAdmin && selectedTargetStaff != null && selectedTargetStaff.id != staff?.id

    var staffDropdownExpanded by remember { mutableStateOf(false) }
    var filterMode by rememberSaveable { mutableStateOf(CalendarFilterMode.ALL) }

    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val currentMonthIso = remember { SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) }
    var selectedDateIso by rememberSaveable(uiState.selectedMonth) {
        mutableStateOf(
            if (todayIso.startsWith(uiState.selectedMonth)) todayIso else "${uiState.selectedMonth}-01"
        )
    }
    var modalDateIso by rememberSaveable(uiState.selectedMonth) {
        mutableStateOf<String?>(null)
    }

    val recordByDate = remember(uiState.attendanceHistory, uiState.todayRecord, uiState.selectedMonth, viewingOtherStaff) {
        val map = LinkedHashMap<String, AttendanceRecordDto>()
        uiState.attendanceHistory.forEach { rec ->
            val key = rec.attendanceDate.trim().take(10)
            if (key.isNotBlank()) {
                map[key] = rec
            }
        }
        if (!viewingOtherStaff) {
            uiState.todayRecord?.let { today ->
                val key = today.attendanceDate.trim().take(10)
                if (key.startsWith(uiState.selectedMonth)) {
                    map[key] = today
                }
            }
        }
        map
    }

    val calendarModel = remember(uiState.selectedMonth, recordByDate, todayIso) {
        buildMonthCalendarModel(
            yearMonth = uiState.selectedMonth,
            todayIso = todayIso,
            recordByDate = recordByDate
        )
    }

    val modalCell = modalDateIso?.let { iso ->
        calendarModel.cells.firstOrNull { it.dateIso == iso }
    }

    val activeSubjectTodayRecord = remember(viewingOtherStaff, uiState.todayRecord, recordByDate, todayIso) {
        if (viewingOtherStaff) {
            recordByDate[todayIso]
        } else {
            uiState.todayRecord ?: recordByDate[todayIso]
        }
    }

    val checkedInTodayStaffIds = remember(uiState.departmentRegister) {
        uiState.departmentRegister.map { it.staffId }.toSet()
    }

    val avgCheckIn = remember(uiState.attendanceHistory) {
        computeAverageCheckIn(uiState.attendanceHistory)
    }
    val onTimeRatePct = remember(uiState.attendanceHistory) {
        computeOnTimeRate(uiState.attendanceHistory)
    }
    val longestStreak = remember(calendarModel.cells) {
        computeLongestPresentStreak(calendarModel.cells.map { it.mark })
    }
    val totalDutyHoursLabel = remember(uiState.attendanceHistory) {
        computeTotalDutyHoursLabel(uiState.attendanceHistory)
    }
    val weeklyBuckets = remember(calendarModel.cells) {
        calendarModel.cells.chunked(7).mapIndexed { idx, chunk ->
            WeeklyAttendanceBucket(
                label = "W${idx + 1}",
                presentCount = chunk.count { it.mark == "P" },
                absentCount = chunk.count { it.mark == "A" }
            )
        }
    }

    val summary = uiState.summary
    val totalDaysInMonth = calendarModel.cells.size.coerceAtLeast(1)
    val effectiveRate = (calendarModel.presentCount.toDouble() / totalDaysInMonth.toDouble()) * 100.0
    val rateLabel = if (effectiveRate % 1.0 == 0.0) {
        "${effectiveRate.toInt()}%"
    } else {
        String.format(Locale.US, "%.1f%%", effectiveRate)
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_pull_refresh")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .align(Alignment.TopCenter)
                .testTag("dashboard_lazy_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FeedbackBanner(
                    bannerMessage = uiState.bannerMessage,
                    errorMessage = uiState.errorMessage,
                    isBackendMissing = uiState.isBackendMissing,
                    onOpenServerSetup = onOpenServerSetup,
                    onDismiss = onDismissFeedback
                )
            }

            if (!uiState.summarySyncError.isNullOrBlank()) {
                item {
                    Surface(
                        color = CrimsonContainer,
                        border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_sync_error_banner")
                    ) {
                        Text(
                            text = uiState.summarySyncError,
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonError,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            // Admin-only Staff Member Selector right at the top
            if (isAdmin && staff != null) {
                item {
                    val otherUsers = uiState.staffDirectory.filter { it.id != staff.id }
                    val selectedLabel = if (selectedTargetStaff == null || selectedTargetStaff.id == staff.id) {
                        "${staff.fullName} (${staff.loginId}) — My Attendance"
                    } else {
                        "${selectedTargetStaff.fullName} (${selectedTargetStaff.loginId} • ${selectedTargetStaff.department})"
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, SubtleBorder),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.People,
                                        contentDescription = "Select Staff Member",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Staff Dashboard Selector",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                Surface(
                                    color = EmeraldContainer,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    val totalStaff = uiState.staffDirectory.size.coerceAtLeast(1)
                                    Text(
                                        text = "${uiState.departmentRegister.size} / $totalStaff In Today",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldSuccess,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            ExposedDropdownMenuBox(
                                expanded = staffDropdownExpanded,
                                onExpandedChange = { staffDropdownExpanded = !staffDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select Staff Name") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = staffDropdownExpanded)
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                        .testTag("admin_dashboard_user_selector")
                                )

                                ExposedDropdownMenu(
                                    expanded = staffDropdownExpanded,
                                    onDismissRequest = { staffDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text("${staff.fullName} (${staff.loginId}) — My Attendance")
                                        },
                                        onClick = {
                                            staffDropdownExpanded = false
                                            onSelectDashboardStaff(null)
                                        }
                                    )
                                    otherUsers.forEach { member ->
                                        DropdownMenuItem(
                                            text = {
                                                Text("${member.fullName} (${member.loginId} • ${member.department})")
                                            },
                                            onClick = {
                                                staffDropdownExpanded = false
                                                onSelectDashboardStaff(member)
                                            }
                                        )
                                    }
                                }
                            }

                            if (otherUsers.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    item {
                                        FilterChip(
                                            selected = !viewingOtherStaff,
                                            onClick = { onSelectDashboardStaff(null) },
                                            label = { Text("My View") },
                                            modifier = Modifier.testTag("quick_chip_self")
                                        )
                                    }
                                    items(otherUsers, key = { it.id }) { member ->
                                        val isPresentToday = checkedInTodayStaffIds.contains(member.id)
                                        val isSelected = selectedTargetStaff?.id == member.id
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { onSelectDashboardStaff(member) },
                                            leadingIcon = if (isPresentToday) {
                                                {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(EmeraldSuccess)
                                                    )
                                                }
                                            } else null,
                                            label = {
                                                Text(
                                                    text = member.fullName,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            modifier = Modifier.testTag("quick_chip_staff_${member.id}")
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Approved staff users will appear here once loaded.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = onRefresh) {
                                        Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = "Reload Staff",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(" Reload")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Hero Active Subject Profile Card
            if (activeSubject != null) {
                item {
                    val initials = remember(activeSubject.fullName) {
                        activeSubject.fullName
                            .split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .joinToString("") { it.first().uppercaseChar().toString() }
                            .ifBlank { "PS" }
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(
                            1.dp,
                            if (viewingOtherStaff) RoyalBluePrimary.copy(alpha = 0.45f) else SubtleBorder
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            BlueContainer.copy(alpha = 0.7f),
                                            PureWhite
                                        )
                                    )
                                )
                                .padding(15.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(RoyalBluePrimary, RoyalBlueDark)
                                            )
                                        )
                                ) {
                                    Text(
                                        text = initials,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activeSubject.fullName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${activeSubject.designation} • ${activeSubject.department}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    StatusPill(label = if (viewingOtherStaff) "STAFF VIEW" else activeSubject.role)
                                    if (viewingOtherStaff) {
                                        TextButton(
                                            onClick = { onSelectDashboardStaff(null) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            modifier = Modifier.testTag("reset_to_my_dashboard")
                                        ) {
                                            Text("My View")
                                        }
                                    }
                                }
                            }

                            Surface(
                                color = PureWhite.copy(alpha = 0.92f),
                                border = BorderStroke(1.dp, SubtleBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Badge,
                                        contentDescription = "Login ID",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = activeSubject.loginId,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("↔", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Icon(
                                        imageVector = Icons.Filled.AlternateEmail,
                                        contentDescription = "Mapped Gmail",
                                        tint = if (activeSubject.gmailVerified) EmeraldSuccess else AmberWarning,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = activeSubject.gmailId,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (activeSubject.gmailVerified) {
                                        Icon(
                                            imageVector = Icons.Filled.VerifiedUser,
                                            contentDescription = "Verified Gmail",
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Month-Wise Date Attendance Calendar (Right below selector & profile for instant visibility!)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = BorderStroke(1.dp, SubtleBorder),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_month_calendar_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header + Month Switcher
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = { onChangeMonth(-1) },
                                    modifier = Modifier.testTag("dashboard_prev_month")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Previous Month"
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CalendarMonth,
                                            contentDescription = "Monthly Date Attendance",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = calendarModel.monthTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (activeSubject != null) {
                                        Text(
                                            text = "Month-Wise Date View • ${activeSubject.fullName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onChangeMonth(1) },
                                    modifier = Modifier.testTag("dashboard_next_month")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Next Month"
                                    )
                                }
                            }

                            if (uiState.selectedMonth != currentMonthIso) {
                                TextButton(
                                    onClick = onJumpToCurrentMonth,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("dashboard_jump_today")
                                ) {
                                    Text("Today")
                                }
                            }
                        }

                        AnimatedVisibility(visible = uiState.isRefreshing) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = SoftWhiteTint
                            )
                        }

                        AnimatedVisibility(
                            visible = uiState.syncStatusText.isNotBlank() && uiState.summarySyncError.isNullOrBlank(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Surface(
                                color = EmeraldContainer.copy(alpha = 0.72f),
                                border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dashboard_sync_status_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Sync,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = uiState.syncStatusText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldSuccess,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Compact P / A / Late / Rate strip right above calendar dates
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SummaryMetricTile(
                                label = "P (In)",
                                value = "${calendarModel.presentCount}",
                                accent = EmeraldSuccess,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryMetricTile(
                                label = "A (Out)",
                                value = "${calendarModel.absentCount}",
                                accent = CrimsonError,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryMetricTile(
                                label = "Late",
                                value = "${summary.lateDays}",
                                accent = AmberWarning,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryMetricTile(
                                label = "Rate",
                                value = rateLabel,
                                accent = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Interactive Status Filter Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(CalendarFilterMode.entries) { mode ->
                                FilterChip(
                                    selected = filterMode == mode,
                                    onClick = { filterMode = mode },
                                    label = { Text(mode.label) },
                                    modifier = Modifier.testTag("calendar_filter_${mode.name.lowercase()}")
                                )
                            }
                        }

                        // Legend pills
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendChip(mark = "P", label = "Checked In", bg = EmeraldContainer, fg = EmeraldSuccess)
                            LegendChip(mark = "A", label = "Absent / No In", bg = CrimsonContainer, fg = CrimsonError)
                            LegendChip(mark = "-", label = "Upcoming", bg = SoftWhiteTint, fg = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Weekday headers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { dayName ->
                                Text(
                                    text = dayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // 7-column calendar rows
                        val slots = buildList<MonthDayAttendanceCell?> {
                            repeat(calendarModel.leadingEmptyCells) { add(null) }
                            addAll(calendarModel.cells)
                            while (size % 7 != 0) {
                                add(null)
                            }
                        }
                        slots.chunked(7).forEach { weekRow ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                weekRow.forEach { cell ->
                                    if (cell == null) {
                                        Spacer(modifier = Modifier.weight(1f).height(50.dp))
                                    } else {
                                        val matchesFilter = when (filterMode) {
                                            CalendarFilterMode.ALL -> true
                                            CalendarFilterMode.PRESENT -> cell.mark == "P"
                                            CalendarFilterMode.LATE -> cell.record?.status.equals("LATE", ignoreCase = true)
                                            CalendarFilterMode.ABSENT -> cell.mark == "A"
                                        }
                                        MonthDayCellView(
                                            cell = cell,
                                            selected = cell.dateIso == selectedDateIso,
                                            dimmed = !matchesFilter,
                                            onClick = {
                                                selectedDateIso = cell.dateIso
                                                modalDateIso = cell.dateIso
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Tap any date cell to view Date, In, Out & Geo details",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Dynamic Live Shift Progress Strip
            if (activeSubject != null) {
                item {
                    LiveShiftStatusBanner(
                        subject = activeSubject,
                        todayRecord = activeSubjectTodayRecord,
                        viewingOtherStaff = viewingOtherStaff,
                        onActionClick = onOpenAttendanceTab
                    )
                }
            }

            // Advanced Dynamic Analytics Card (Donut Gauge + Weekly Trend Chart + Status Breakdown + 75% Benchmark)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = BorderStroke(1.dp, SubtleBorder),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_advanced_analytics_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Insights,
                                contentDescription = "Attendance Insights",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Dynamic Monthly Analytics (${uiState.selectedMonth})",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        AttendanceRateDonutGauge(
                            attendancePercentage = effectiveRate,
                            presentStreak = calendarModel.presentStreak,
                            longestStreak = longestStreak,
                            avgCheckInLabel = avgCheckIn,
                            totalDutyHoursLabel = totalDutyHoursLabel,
                            onTimeRatePct = onTimeRatePct,
                            presentDaysInMonth = calendarModel.presentCount,
                            totalDaysInMonth = totalDaysInMonth
                        )

                        WeeklyAttendanceBarChart(buckets = weeklyBuckets)

                        StatusBreakdownStackedBar(
                            present = summary.presentDays,
                            late = summary.lateDays,
                            onDuty = summary.onDutyDays,
                            halfDay = summary.halfDays,
                            absent = summary.absentDays
                        )

                        AttendanceBenchmarkCard(
                            presentCount = calendarModel.presentCount,
                            absentCount = calendarModel.absentCount,
                            totalDaysInMonth = totalDaysInMonth
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SummaryMetricTile(
                                label = "Present",
                                value = "${calendarModel.presentCount}",
                                accent = EmeraldSuccess,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryMetricTile(
                                label = "On-Duty",
                                value = "${summary.onDutyDays}",
                                accent = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryMetricTile(
                                label = "Half Day",
                                value = "${summary.halfDays}",
                                accent = AmberWarning,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryMetricTile(
                                label = "Month Days",
                                value = "$totalDaysInMonth",
                                accent = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Dynamic Quick Pulse Row (interactive shortcuts to Attendance, Leaves, Notifications)
            item {
                val pendingMyLeaves = uiState.myLeaves.count { it.status.equals("PENDING", ignoreCase = true) }
                val ownCheckedIn = uiState.todayRecord != null && !uiState.todayRecord.checkInTime.isNullOrBlank()
                DynamicQuickPulseRow(
                    todayCheckedIn = ownCheckedIn,
                    pendingLeavesCount = pendingMyLeaves,
                    pendingApprovalsCount = uiState.pendingApprovals.size,
                    isHodOrAdmin = isHodOrAdmin,
                    adminNoticesCount = uiState.adminMessages.size,
                    reminderEnabled = uiState.reminderEnabled,
                    onOpenAttendance = onOpenAttendanceTab,
                    onOpenLeaves = onOpenLeavesTab,
                    onOpenNotifications = onOpenNotificationsTab
                )
            }
        }

        if (modalCell != null) {
            DayAttendanceDetailModal(
                cell = modalCell,
                staffName = activeSubject?.fullName,
                onDismiss = { modalDateIso = null }
            )
        }
    }
}

@Composable
private fun MonthDayCellView(
    cell: MonthDayAttendanceCell,
    selected: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgBrush = when (cell.mark) {
        "P" -> Brush.verticalGradient(listOf(EmeraldContainer, EmeraldContainer.copy(alpha = 0.55f)))
        "A" -> Brush.verticalGradient(listOf(CrimsonContainer, CrimsonContainer.copy(alpha = 0.6f)))
        else -> Brush.verticalGradient(listOf(SoftWhiteTint, PureWhite))
    }
    val markColor = when (cell.mark) {
        "P" -> EmeraldSuccess
        "A" -> CrimsonError
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = when {
        selected -> RoyalBluePrimary
        cell.isToday -> RoyalBluePrimary.copy(alpha = 0.65f)
        else -> SubtleBorder
    }

    Column(
        modifier = modifier
            .height(52.dp)
            .alpha(if (dimmed) 0.30f else 1f)
            .clip(RoundedCornerShape(11.dp))
            .background(bgBrush)
            .border(
                width = if (selected) 2.dp else if (cell.isToday) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(11.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .testTag("calendar_day_${cell.dateIso}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "${cell.dayOfMonth}",
                style = MaterialTheme.typography.labelSmall,
                color = if (cell.isToday) RoyalBluePrimary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (cell.isToday || selected) FontWeight.Bold else FontWeight.Medium
            )
            if (cell.isToday) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(RoyalBluePrimary)
                )
            }
        }
        Text(
            text = cell.mark,
            style = MaterialTheme.typography.labelLarge,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            color = markColor
        )
    }
}

@Composable
private fun LegendChip(
    mark: String,
    label: String,
    bg: Color,
    fg: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Surface(
            color = bg,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = mark,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = fg
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SummaryMetricTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.06f)),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(accent.copy(alpha = 0.75f))
            )
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DayAttendanceDetailModal(
    cell: MonthDayAttendanceCell,
    staffName: String?,
    onDismiss: () -> Unit
) {
    val rec = cell.record
    val markBg = when (cell.mark) {
        "P" -> EmeraldContainer
        "A" -> CrimsonContainer
        else -> BlueContainer
    }
    val markFg = when (cell.mark) {
        "P" -> EmeraldSuccess
        "A" -> CrimsonError
        else -> MaterialTheme.colorScheme.primary
    }
    val geoText = when {
        !rec?.geoAddress.isNullOrBlank() -> rec?.geoAddress.orEmpty()
        rec?.latitude != null && rec.longitude != null ->
            String.format(Locale.US, "%.5f, %.5f", rec.latitude, rec.longitude)
        else -> "Not tagged"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = BorderStroke(1.dp, SubtleBorder),
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("date_detail_modal")
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = markBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = cell.mark,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = markFg
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Date: ${cell.dateIso}" + if (cell.isToday) " (Today)" else "",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        if (!staffName.isNullOrBlank()) {
                            Text(
                                text = staffName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close Day Details"
                        )
                    }
                }

                HorizontalDivider(color = SubtleBorder)

                Surface(
                    color = SoftWhiteTint,
                    border = BorderStroke(1.dp, SubtleBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModalDetailRow(
                            icon = Icons.Filled.CalendarMonth,
                            tint = MaterialTheme.colorScheme.primary,
                            label = "Attendance Status",
                            value = when {
                                rec != null -> "${cell.mark} (${rec.status})"
                                cell.mark == "A" -> "A (No Check-In Recorded)"
                                else -> "- (Upcoming Date)"
                            }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ModalDetailRow(
                                    icon = Icons.Filled.Schedule,
                                    tint = EmeraldSuccess,
                                    label = "In Time",
                                    value = rec?.checkInTime?.takeIf { it.isNotBlank() } ?: "--:--"
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ModalDetailRow(
                                    icon = Icons.Filled.Schedule,
                                    tint = RoyalBlueDark,
                                    label = "Out Time",
                                    value = rec?.checkOutTime?.takeIf { it.isNotBlank() } ?: "--:--"
                                )
                            }
                        }
                        ModalDetailRow(
                            icon = Icons.Filled.LocationOn,
                            tint = MaterialTheme.colorScheme.primary,
                            label = "Geo Location",
                            value = geoText
                        )
                        if (rec?.faceVerified == true) {
                            ModalDetailRow(
                                icon = Icons.Filled.FaceRetouchingNatural,
                                tint = EmeraldSuccess,
                                label = "Verification",
                                value = "Live Face Verified"
                            )
                        }
                        if (!rec?.remarks.isNullOrBlank()) {
                            ModalDetailRow(
                                icon = Icons.Filled.VerifiedUser,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "Remarks",
                                value = rec?.remarks.orEmpty()
                            )
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("date_detail_modal_close")
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun ModalDetailRow(
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private data class MonthCalendarModel(
    val monthTitle: String,
    val leadingEmptyCells: Int,
    val cells: List<MonthDayAttendanceCell>,
    val presentCount: Int,
    val absentCount: Int,
    val presentStreak: Int
)

private fun buildMonthCalendarModel(
    yearMonth: String,
    todayIso: String,
    recordByDate: Map<String, AttendanceRecordDto>
): MonthCalendarModel {
    val cal = Calendar.getInstance(Locale.US)
    val parsed = try {
        SimpleDateFormat("yyyy-MM", Locale.US).parse(yearMonth)
    } catch (_: Exception) {
        null
    }
    if (parsed != null) {
        cal.time = parsed
    }
    cal.set(Calendar.DAY_OF_MONTH, 1)

    val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    val leadingEmpty = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val cells = ArrayList<MonthDayAttendanceCell>(maxDays)
    var pCount = 0
    var aCount = 0

    for (day in 1..maxDays) {
        val dateIso = String.format(Locale.US, "%s-%02d", yearMonth, day)
        val rec = recordByDate[dateIso]
        val checkedIn = rec != null && (
            !rec.checkInTime.isNullOrBlank() ||
                rec.status.uppercase() in setOf("PRESENT", "LATE", "ON_DUTY", "HALF_DAY")
            )

        val mark = when {
            checkedIn -> {
                pCount++
                "P"
            }
            dateIso <= todayIso -> {
                aCount++
                "A"
            }
            else -> "-"
        }

        cells.add(
            MonthDayAttendanceCell(
                dayOfMonth = day,
                dateIso = dateIso,
                mark = mark,
                isToday = dateIso == todayIso,
                record = rec
            )
        )
    }

    var streak = 0
    val pastOrTodayCells = cells.filter { it.dateIso <= todayIso }
    for (cell in pastOrTodayCells.asReversed()) {
        if (cell.mark == "P") {
            streak++
        } else if (cell.dateIso != todayIso) {
            break
        }
    }

    return MonthCalendarModel(
        monthTitle = monthTitle,
        leadingEmptyCells = leadingEmpty.coerceAtLeast(0),
        cells = cells,
        presentCount = pCount,
        absentCount = aCount,
        presentStreak = streak
    )
}
