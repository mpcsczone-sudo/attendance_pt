package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.SessionPreferences
import com.example.data.local.StoredSession
import com.example.data.remote.NetworkClient
import com.example.data.repository.AttendanceRepository
import com.example.ui.components.ServerSetupBottomSheet
import com.example.ui.screens.AttendanceHomeScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.GmailMappingAndSmtpScreen
import com.example.ui.screens.LeaveAndRequestsScreen
import com.example.ui.theme.PttpStaffTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.DashboardViewModel

private enum class AppTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    ATTENDANCE(
        label = "Attendance",
        selectedIcon = Icons.Filled.Fingerprint,
        unselectedIcon = Icons.Outlined.Fingerprint,
        testTag = "nav_attendance"
    ),
    LEAVES(
        label = "Leaves & OD",
        selectedIcon = Icons.Filled.EventNote,
        unselectedIcon = Icons.Outlined.EventNote,
        testTag = "nav_leaves"
    ),
    GMAIL_SMTP(
        label = "Gmail & SMTP",
        selectedIcon = Icons.Filled.AlternateEmail,
        unselectedIcon = Icons.Outlined.AlternateEmail,
        testTag = "nav_gmail_smtp"
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                        factory = DashboardViewModel.Factory(repository)
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
    var currentTab by rememberSaveable { mutableStateOf(AppTab.ATTENDANCE) }
    var showServerSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = currentTab != AppTab.ATTENDANCE) {
        currentTab = AppTab.ATTENDANCE
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentTab) {
                                AppTab.ATTENDANCE -> stringResource(R.string.college_full_name)
                                AppTab.LEAVES -> "Leaves & Regularization"
                                AppTab.GMAIL_SMTP -> "Gmail Mapping & SMTP"
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    actions = {
                        IconButton(
                            onClick = { showServerSheet = true },
                            modifier = Modifier.testTag("topbar_server_setup_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Dns,
                                contentDescription = "PHP & MySQL Server Settings"
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
                                label = { Text(tab.label) },
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
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (currentTab) {
                        AppTab.ATTENDANCE -> AttendanceHomeScreen(
                            uiState = uiState,
                            onMarkAttendance = { action, remarks ->
                                viewModel.markAttendance(action, remarks)
                            },
                            onRefresh = { viewModel.refreshAll() },
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
                            onOpenServerSetup = { showServerSheet = true },
                            onDismissFeedback = { viewModel.clearFeedback() }
                        )

                        AppTab.GMAIL_SMTP -> GmailMappingAndSmtpScreen(
                            uiState = uiState,
                            onInitiateGmailMapping = { viewModel.initiateGmailMapping(it) },
                            onConfirmGmailMapping = { viewModel.confirmGmailMapping(it) },
                            onCancelGmailMapping = { viewModel.cancelGmailMappingStep() },
                            onAdminMapGmail = { loginId, gmailId ->
                                viewModel.adminUpdateStaffGmailMapping(loginId, gmailId)
                            },
                            onReviewStaffRegistration = { staffId, approve ->
                                viewModel.reviewStaffRegistration(staffId, approve)
                            },
                            onSendTestSmtp = { viewModel.sendTestSmtpAlert() },
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
