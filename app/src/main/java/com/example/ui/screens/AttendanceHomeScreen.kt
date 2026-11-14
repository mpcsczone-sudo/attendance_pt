package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.components.FeedbackBanner
import com.example.ui.components.LiveFaceDetectorDialog
import com.example.ui.components.StatusPill
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHomeScreen(
    uiState: DashboardUiState,
    onCaptureGeo: () -> Unit,
    onMarkAttendance: (action: String, remarks: String, faceVerified: Boolean, faceConfidence: Float?) -> Unit,
    onRefresh: () -> Unit,
    onOpenServerSetup: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(formatNow("HH:mm:ss")) }
    var currentDateLabel by remember { mutableStateOf(formatNow("EEEE, dd MMM yyyy")) }
    var remarks by rememberSaveable { mutableStateOf("") }

    var pendingFaceAction by remember { mutableStateOf<String?>(null) }
    var showFaceDetectorDialog by remember { mutableStateOf(false) }

    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val camGranted = grants[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val locGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            hasLocationPermission

        if (locGranted) {
            onCaptureGeo()
        }
        if (camGranted && pendingFaceAction != null) {
            showFaceDetectorDialog = true
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = formatNow("HH:mm:ss")
            currentDateLabel = formatNow("EEEE, dd MMM yyyy")
            delay(1000L)
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission && uiState.capturedGeoTag == null) {
            onCaptureGeo()
        }
    }

    fun launchFaceAndGeoVerification(action: String) {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.CAMERA)
        }
        if (!hasLocationPermission) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
            needed.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        pendingFaceAction = action
        if (needed.isEmpty()) {
            onCaptureGeo()
            showFaceDetectorDialog = true
        } else {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    val staff = uiState.staff
    val today = uiState.todayRecord

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .testTag("attendance_pull_refresh")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .align(Alignment.TopCenter),
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

            // Only the Live Face & Geo-Tag Attendance Check-In / Check-Out Section
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

                        if (staff != null) {
                            Surface(
                                color = BlueContainer.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Schedule,
                                        contentDescription = "Shift Schedule",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${staff.loginId} • Shift ${staff.shiftStartTime}–${staff.shiftEndTime}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "10m: ${uiState.nextReminderLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
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
                            if (!today.geoAddress.isNullOrBlank() || today.faceVerified) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (today.faceVerified) {
                                        StatusPill(label = "FACE VERIFIED")
                                    }
                                    if (!today.geoAddress.isNullOrBlank()) {
                                        Text(
                                            text = "Geo: ${today.geoAddress}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
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
                                    if (canCheckOut) {
                                        onMarkAttendance("CHECK_OUT", remarks, false, null)
                                        remarks = ""
                                    } else {
                                        launchFaceAndGeoVerification("CHECK_IN")
                                    }
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
                                        imageVector = if (canCheckOut) Icons.Filled.Logout else Icons.Filled.FaceRetouchingNatural,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when {
                                            canCheckOut -> "Check Out & Email"
                                            canCheckIn -> "Face & Geo Check-In"
                                            else -> "Checked Out"
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (canCheckIn) {
                                OutlinedButton(
                                    onClick = { launchFaceAndGeoVerification("ON_DUTY") },
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
                            text = "Check-In opens Live Face Detector + GPS Geo-Tag, marks today P on your Dashboard calendar, and emails a receipt.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showFaceDetectorDialog && pendingFaceAction != null) {
        val actionToRun = pendingFaceAction!!
        LiveFaceDetectorDialog(
            actionLabel = if (actionToRun == "ON_DUTY") "Confirm Face & On-Duty" else "Confirm Face & Check-In",
            geoTag = uiState.capturedGeoTag,
            isCapturingGeo = uiState.isCapturingGeo,
            geoPermissionGranted = hasLocationPermission,
            onRetryGeo = onCaptureGeo,
            onConfirmCheckIn = { faceVerified, confidence ->
                showFaceDetectorDialog = false
                pendingFaceAction = null
                val note = if (actionToRun == "ON_DUTY") remarks.ifBlank { "Official College Duty" } else remarks
                onMarkAttendance(actionToRun, note, faceVerified, confidence)
                remarks = ""
            },
            onDismiss = {
                showFaceDetectorDialog = false
                pendingFaceAction = null
            }
        )
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

private fun formatNow(pattern: String): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
}
