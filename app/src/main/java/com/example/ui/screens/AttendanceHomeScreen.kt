package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceRecordDto
import com.example.ui.components.FeedbackBanner
import com.example.ui.components.SmtpReceiptCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftWhiteTint
import com.example.ui.theme.SubtleBorder
import com.example.ui.viewmodel.DashboardUiState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AttendanceHomeScreen(
    uiState: DashboardUiState,
    onMarkAttendance: (action: String, remarks: String) -> Unit,
    onRefresh: () -> Unit,
    onOpenServerSetup: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(formatNow("HH:mm:ss")) }
    var currentDateLabel by remember { mutableStateOf(formatNow("EEEE, dd MMM yyyy")) }
    var remarks by rememberSaveable { mutableStateOf("") }
    var showDeptRegister by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = formatNow("HH:mm:ss")
            currentDateLabel = formatNow("EEEE, dd MMM yyyy")
            delay(1000L)
        }
    }

    val staff = uiState.staff
    val today = uiState.todayRecord
    val isHodOrAdmin = staff?.role == "HOD" || staff?.role == "ADMIN"

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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

            item {
                SmtpReceiptCard(receipt = uiState.lastSmtpReceipt)
            }

            // Staff Login ID <-> Gmail Mapping Identity Card
            if (staff != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, SubtleBorder),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = staff.fullName,
                                        style = MaterialTheme.typography.titleLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${staff.designation} • ${staff.department}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusPill(label = staff.role)
                            }

                            Surface(
                                color = SoftWhiteTint,
                                border = BorderStroke(1.dp, SubtleBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Badge,
                                        contentDescription = "Login ID",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = staff.loginId,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("↔", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Icon(
                                        imageVector = Icons.Filled.AlternateEmail,
                                        contentDescription = "Mapped Gmail",
                                        tint = if (staff.gmailVerified) EmeraldSuccess else AmberWarning,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = staff.gmailId,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (staff.gmailVerified) {
                                        Icon(
                                            imageVector = Icons.Filled.VerifiedUser,
                                            contentDescription = "Verified Gmail",
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Crisp White Attendance Shift Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = BorderStroke(1.dp, SubtleBorder),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = currentDateLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentTime,
                                    style = MaterialTheme.typography.displaySmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Surface(
                                color = if (today != null) EmeraldContainer else BlueContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = when {
                                        today == null -> "NOT MARKED"
                                        today.checkOutTime != null -> "SHIFT COMPLETE"
                                        else -> today.status.replace('_', ' ')
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (today != null) EmeraldSuccess else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (today != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TimeChip(
                                    label = "CHECK IN",
                                    value = today.checkInTime ?: "--:--",
                                    modifier = Modifier.weight(1f)
                                )
                                TimeChip(
                                    label = "CHECK OUT",
                                    value = today.checkOutTime ?: "Active",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = remarks,
                            onValueChange = { remarks = it },
                            label = { Text("Optional Duty / Lecture Note") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("attendance_remarks_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val canCheckIn = today == null || today.checkInTime.isNullOrBlank()
                            val canCheckOut = today != null && !today.checkInTime.isNullOrBlank() && today.checkOutTime.isNullOrBlank()

                            Button(
                                onClick = {
                                    onMarkAttendance(
                                        if (canCheckOut) "CHECK_OUT" else "CHECK_IN",
                                        remarks
                                    )
                                    remarks = ""
                                },
                                enabled = !uiState.isSubmittingAction && (canCheckIn || canCheckOut),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (canCheckOut) CrimsonError else MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("primary_attendance_button")
                            ) {
                                if (uiState.isSubmittingAction) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (canCheckOut) Icons.Filled.Logout else Icons.Filled.Login,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when {
                                            canCheckOut -> "Check Out & Email"
                                            canCheckIn -> "Check In & Email"
                                            else -> "Checked Out"
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (canCheckIn) {
                                OutlinedButton(
                                    onClick = {
                                        onMarkAttendance("ON_DUTY", remarks.ifBlank { "Official College Duty" })
                                        remarks = ""
                                    },
                                    enabled = !uiState.isSubmittingAction,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .height(50.dp)
                                        .testTag("on_duty_button")
                                ) {
                                    Text("On-Duty")
                                }
                            }
                        }

                        Text(
                            text = "Every check-in/out logs to MySQL and dispatches a real-time SMTP receipt to your mapped Gmail.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Monthly Summary Metrics
            item {
                val summary = uiState.summary
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (summary.month.isNotBlank()) "Monthly Summary (${summary.month})" else "Monthly Summary",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(
                            onClick = onRefresh,
                            enabled = !uiState.isRefreshing,
                            modifier = Modifier.testTag("refresh_attendance_button")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (uiState.isRefreshing) "Syncing..." else "Sync")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryStatTile(
                            label = "Present",
                            value = "${summary.presentDays}",
                            accent = EmeraldSuccess,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatTile(
                            label = "Late",
                            value = "${summary.lateDays}",
                            accent = AmberWarning,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatTile(
                            label = "On-Duty",
                            value = "${summary.onDutyDays}",
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatTile(
                            label = "Rate",
                            value = "${summary.attendancePercentage.toInt()}%",
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showDeptRegister && isHodOrAdmin) "Today's Department Register" else "Recent Attendance Ledger",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (isHodOrAdmin) {
                        TextButton(onClick = { showDeptRegister = !showDeptRegister }) {
                            Text(if (showDeptRegister) "View My Logs" else "Dept Register (${uiState.departmentRegister.size})")
                        }
                    }
                }
            }

            val displayedList = if (showDeptRegister && isHodOrAdmin) {
                uiState.departmentRegister
            } else {
                uiState.attendanceHistory
            }

            if (displayedList.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SoftWhiteTint),
                        border = BorderStroke(1.dp, SubtleBorder),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EventAvailable,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No attendance entries synced yet",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Tap Check In above to record today's attendance in MySQL and receive an SMTP confirmation in your mapped Gmail.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(displayedList, key = { "${it.id}_${it.attendanceDate}_${it.staffId}" }) { record ->
                    AttendanceRecordRow(
                        record = record,
                        showStaffIdentity = showDeptRegister && isHodOrAdmin
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SoftWhiteTint,
        border = BorderStroke(1.dp, SubtleBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = JetBrainsMonoFontFamily,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SummaryStatTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, SubtleBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = JetBrainsMonoFontFamily,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AttendanceRecordRow(
    record: AttendanceRecordDto,
    showStaffIdentity: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, SubtleBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = BlueContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (record.checkOutTime != null) Icons.Filled.CheckCircle else Icons.Filled.WorkHistory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (showStaffIdentity) "${record.fullName} (${record.loginId})" else record.attendanceDate,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "In: ${record.checkInTime ?: "--"}  •  Out: ${record.checkOutTime ?: "--"}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (record.remarks.isNotBlank()) {
                    Text(
                        text = record.remarks,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            StatusPill(label = record.status)
        }
    }
}

private fun formatNow(pattern: String): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
}
