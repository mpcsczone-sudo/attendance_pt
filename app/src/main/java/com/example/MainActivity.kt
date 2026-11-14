package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.SessionPreferences
import com.example.data.local.StoredSession
import com.example.data.remote.NetworkClient
import com.example.data.repository.AttendanceRepository
import com.example.notifications.ShiftReminderScheduler
import com.example.ui.components.ServerSetupBottomSheet
import com.example.ui.screens.AttendanceHomeScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GmailMappingAndSmtpScreen
import com.example.ui.screens.LeaveAndRequestsScreen
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PttpStaffTheme
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.RoyalBluePrimary
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.DashboardViewModel

private enum class AppTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD(
        label = "Dashboard",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_dashboard"
    ),
    ATTENDANCE(
        label = "Attendance",
        selectedIcon = Icons.Filled.Fingerprint,
        unselectedIcon = Icons.Outlined.Fingerprint,
        testTag = "nav_attendance"
    ),
    LEAVES(
        label = "Leaves",
        selectedIcon = Icons.Filled.EventNote,
        unselectedIcon = Icons.Outlined.EventNote,
        testTag = "nav_leaves"
    ),
    NOTIFICATIONS(
        label = "Notifications",
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications,
        testTag = "nav_notifications"
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ShiftReminderScheduler.ensureChannel(applicationContext)

        val prefs = SessionPreferences(applicationContext)
        val db = NetworkClient.getDatabase(applicationContext)
        val api = NetworkClient.createApiService(prefs)
        val repository = AttendanceRepository(api = api, prefs = prefs, dao = db.attendanceDao())

        setContent {
            PttpStaffTheme {
                val session by repository.sessionFlow.collectAsStateWithLifecycle(
                    initialValue = StoredSession()
                )

                if (!session.isLoggedIn) {
                    val authViewModel: AuthViewModel = viewModel(
                        factory = AuthViewModel.Factory(repository)
                    )
                    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
                    AuthScreen(
                        uiState = authUiState,
                        viewModel = authViewModel
                    )
                } else {
                    val dashboardViewModel: DashboardViewModel = viewModel(
                        factory = DashboardViewModel.Factory(applicationContext, repository)
                    )
                    AuthenticatedShell(viewModel = dashboardViewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedShell(
    viewModel: DashboardViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by rememberSaveable { mutableStateOf(AppTab.DASHBOARD) }
    var showServerSheet by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.silentSyncInBackground()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(currentTab) {
        viewModel.silentSyncInBackground()
    }

    BackHandler(enabled = currentTab != AppTab.DASHBOARD) {
        currentTab = AppTab.DASHBOARD
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(RoyalBluePrimary, RoyalBlueDark)
                                        )
                                    )
                            ) {
                                Text(
                                    text = "PT",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = when (currentTab) {
                                        AppTab.DASHBOARD -> stringResource(R.string.college_full_name)
                                        AppTab.ATTENDANCE -> "Check-In & Out"
                                        AppTab.LEAVES -> "Leaves & Regularization"
                                        AppTab.NOTIFICATIONS -> "Notifications"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldSuccess)
                                    )
                                    Text(
                                        text = if (uiState.lastAutoSyncedAt.isNotBlank()) {
                                            "Auto-Sync Live • ${uiState.lastAutoSyncedAt}"
                                        } else {
                                            "Auto-Sync Live"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldSuccess,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    actions = {
                        Surface(
                            color = EmeraldContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.padding(end = 2.dp)
                        ) {
                            Text(
                                text = uiState.staff?.role ?: "STAFF",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier.testTag("topbar_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Sign Out"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (currentTab) {
                        AppTab.DASHBOARD -> DashboardScreen(
                            uiState = uiState,
                            onChangeMonth = { delta -> viewModel.changeMonth(delta) },
                            onJumpToCurrentMonth = { viewModel.jumpToCurrentMonth() },
                            onSelectDashboardStaff = { selected -> viewModel.selectDashboardStaff(selected) },
                            onRefresh = { viewModel.refreshAll(manualSync = true) },
                            onOpenAttendanceTab = { currentTab = AppTab.ATTENDANCE },
                            onOpenLeavesTab = { currentTab = AppTab.LEAVES },
                            onOpenNotificationsTab = { currentTab = AppTab.NOTIFICATIONS },
                            onOpenServerSetup = { showServerSheet = true },
                            onDismissFeedback = { viewModel.clearFeedback() }
                        )

                        AppTab.ATTENDANCE -> AttendanceHomeScreen(
                            uiState = uiState,
                            onCaptureGeo = { viewModel.captureGeoLocation() },
                            onMarkAttendance = { action, remarks, faceVerified, confidence ->
                                viewModel.markAttendance(action, remarks, faceVerified, confidence)
                            },
                            onRefresh = { viewModel.refreshAll(manualSync = true) },
                            onOpenServerSetup = { showServerSheet = true },
                            onDismissFeedback = { viewModel.clearFeedback() }
                        )

                        AppTab.LEAVES -> LeaveAndRequestsScreen(
                            uiState = uiState,
                            onSubmitLeave = { type, start, end, reason ->
                                viewModel.submitLeave(type, start, end, reason)
                            },
                            onReviewLeave = { id, approve, note ->
                                viewModel.reviewLeave(id, approve, note)
                            },
                            onCancelLeave = { id ->
                                viewModel.cancelLeave(id)
                            },
                            onRefresh = { viewModel.refreshAll(manualSync = true) },
                            onOpenServerSetup = { showServerSheet = true },
                            onDismissFeedback = { viewModel.clearFeedback() }
                        )

                        AppTab.NOTIFICATIONS -> GmailMappingAndSmtpScreen(
                            uiState = uiState,
                            onRefresh = { viewModel.refreshAll(manualSync = true) },
                            onSendAdminMessage = { targetLoginId, title, body ->
                                viewModel.sendAdminMessage(targetLoginId, title, body)
                            },
                            onReplyToAdminMessage = { parentId, body ->
                                viewModel.replyToAdminMessage(parentId, body)
                            },
                            onInitiateGmailMapping = { viewModel.initiateGmailMapping(it) },
                            onConfirmGmailMapping = { viewModel.confirmGmailMapping(it) },
                            onCancelGmailMapping = { viewModel.cancelGmailMappingStep() },
                            onAdminMapGmail = { loginId, gmailId ->
                                viewModel.adminUpdateStaffGmailMapping(loginId, gmailId)
                            },
                            onReviewStaffRegistration = { staffId, approve ->
                                viewModel.reviewStaffRegistration(staffId, approve)
                            },
                            onUpdateShiftTimeMapping = { loginId, startTime, endTime ->
                                viewModel.updateStaffTimeMapping(loginId, startTime, endTime)
                            },
                            onToggleShiftReminder = { enabled ->
                                viewModel.setReminderEnabled(enabled)
                            },
                            onOpenServerSetup = { showServerSheet = true },
                            onDismissFeedback = { viewModel.clearFeedback() }
                        )
                    }
                }
            }
        }
    }

    if (showServerSheet) {
        ServerSetupBottomSheet(
            currentBaseUrl = uiState.baseUrl,
            healthStatus = uiState.healthStatus,
            healthError = uiState.errorMessage,
            isChecking = uiState.isCheckingHealth,
            onCheckHealth = { viewModel.checkServerHealth() },
            onSaveBaseUrl = { viewModel.updateBaseUrl(it) },
            onDismiss = { showServerSheet = false }
        )
    }
}
