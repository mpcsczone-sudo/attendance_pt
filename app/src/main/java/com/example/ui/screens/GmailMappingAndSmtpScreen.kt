package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AdminMessageThreadDto
import com.example.data.model.StaffProfile
import com.example.ui.components.FeedbackBanner
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberContainer
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

private enum class NotificationsSectionTab {
    ADMIN_MESSAGES,
    SHIFT_AND_TOOLS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GmailMappingAndSmtpScreen(
    uiState: DashboardUiState,
    onRefresh: () -> Unit,
    onSendAdminMessage: (targetLoginId: String?, title: String, messageBody: String) -> Unit,
    onReplyToAdminMessage: (parentId: Int, messageBody: String) -> Unit,
    onInitiateGmailMapping: (String) -> Unit,
    onConfirmGmailMapping: (String) -> Unit,
    onCancelGmailMapping: () -> Unit,
    onAdminMapGmail: (loginId: String, gmailId: String) -> Unit,
    onReviewStaffRegistration: (staffId: Int, approve: Boolean) -> Unit,
    onUpdateShiftTimeMapping: (loginId: String, startTime: String, endTime: String) -> Unit,
    onToggleShiftReminder: (Boolean) -> Unit,
    onOpenServerSetup: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val staff = uiState.staff
    val isAdmin = staff?.role?.equals("ADMIN", ignoreCase = true) == true
    val isHodOrAdmin = staff?.role?.equals("HOD", ignoreCase = true) == true || isAdmin

    var selectedSection by rememberSaveable { mutableStateOf(NotificationsSectionTab.ADMIN_MESSAGES) }

    // Admin message composer state
    var msgTargetLoginId by rememberSaveable { mutableStateOf("") }
    var msgTitle by rememberSaveable { mutableStateOf("") }
    var msgBody by rememberSaveable { mutableStateOf("") }

    // Per-thread reply drafts
    val replyDrafts = remember { mutableStateMapOf<Int, String>() }

    // Shift & Gmail mapping state
    var newGmailInput by rememberSaveable(staff?.gmailId) {
        mutableStateOf(staff?.gmailId.orEmpty())
    }
    var linkOtpInput by rememberSaveable { mutableStateOf("") }
    var adminLoginIdInput by rememberSaveable { mutableStateOf("") }
    var adminGmailInput by rememberSaveable { mutableStateOf("") }
    var shiftTargetLoginId by rememberSaveable(staff?.loginId) {
        mutableStateOf(staff?.loginId.orEmpty())
    }
    var shiftStartInput by rememberSaveable(staff?.shiftStartTime) {
        mutableStateOf(staff?.shiftStartTime ?: "09:30")
    }
    var shiftEndInput by rememberSaveable(staff?.shiftEndTime) {
        mutableStateOf(staff?.shiftEndTime ?: "16:30")
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .testTag("notifications_pull_refresh")
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

            // Sub-tab selector: Admin Messages (default) | Shift & Account Tools (ADMIN ONLY)
            if (isAdmin) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SoftWhiteTint,
                        border = BorderStroke(1.dp, SubtleBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(5.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SectionPillButton(
                                title = "Admin Messages (${uiState.adminMessages.size})",
                                selected = selectedSection == NotificationsSectionTab.ADMIN_MESSAGES,
                                onClick = { selectedSection = NotificationsSectionTab.ADMIN_MESSAGES },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("notif_subtab_admin_messages")
                            )
                            SectionPillButton(
                                title = "Shift & Account Tools",
                                selected = selectedSection == NotificationsSectionTab.SHIFT_AND_TOOLS,
                                onClick = { selectedSection = NotificationsSectionTab.SHIFT_AND_TOOLS },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("notif_subtab_shift_tools")
                            )
                        }
                    }
                }
            }

            if (!isAdmin || selectedSection == NotificationsSectionTab.ADMIN_MESSAGES) {
                if (isAdmin) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            border = BorderStroke(1.dp, SubtleBorder),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Campaign,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Send Admin Message to Staff",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }

                                OutlinedTextField(
                                    value = msgTargetLoginId,
                                    onValueChange = { msgTargetLoginId = it },
                                    label = { Text("Target Staff Login ID (leave blank for All Staff)") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_msg_target_input")
                                )

                                OutlinedTextField(
                                    value = msgTitle,
                                    onValueChange = { msgTitle = it },
                                    label = { Text("Subject / Title") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_msg_title_input")
                                )

                                OutlinedTextField(
                                    value = msgBody,
                                    onValueChange = { msgBody = it },
                                    label = { Text("Message from Admin") },
                                    minLines = 3,
                                    maxLines = 5,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_msg_body_input")
                                )

                                Button(
                                    onClick = {
                                        onSendAdminMessage(
                                            msgTargetLoginId.takeIf { it.isNotBlank() },
                                            msgTitle,
                                            msgBody
                                        )
                                        msgTitle = ""
                                        msgBody = ""
                                    },
                                    enabled = !uiState.isSubmittingAction,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("send_admin_message_button")
                                ) {
                                    if (uiState.isSubmittingAction) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Send Notification to Staff")
                                    }
                                }
                            }
                        }
                    }
                }

                if (uiState.adminMessages.isEmpty()) {
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
                                    imageVector = Icons.Filled.MarkChatRead,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "No messages from Admin yet",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Notifications sent by the College Admin and your replies will appear here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.adminMessages, key = { it.id }) { thread ->
                        AdminMessageThreadCard(
                            thread = thread,
                            currentUserRole = staff?.role ?: "STAFF",
                            replyDraft = replyDrafts[thread.id].orEmpty(),
                            isSubmitting = uiState.isSubmittingAction,
                            onDraftChanged = { text -> replyDrafts[thread.id] = text },
                            onSendReply = {
                                val text = replyDrafts[thread.id].orEmpty()
                                onReplyToAdminMessage(thread.id, text)
                                replyDrafts[thread.id] = ""
                            }
                        )
                    }
                }
            } else {
                // Shift & Account Tools Sub-Tab
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, SubtleBorder),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    Icon(
                                        imageVector = Icons.Filled.NotificationsActive,
                                        contentDescription = "10-Minute Shift Reminder",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = "10-Min Pre-Shift Reminder",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Alerts 10 min before shift if not checked in (${uiState.nextReminderLabel})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = uiState.reminderEnabled,
                                    onCheckedChange = onToggleShiftReminder,
                                    modifier = Modifier.testTag("shift_reminder_switch")
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Staff-Wise Time Mapping",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            if (isHodOrAdmin) {
                                OutlinedTextField(
                                    value = shiftTargetLoginId,
                                    onValueChange = { shiftTargetLoginId = it },
                                    label = { Text("Staff Login ID") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = shiftStartInput,
                                    onValueChange = { shiftStartInput = it },
                                    label = { Text("Shift Start (HH:MM)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = shiftEndInput,
                                    onValueChange = { shiftEndInput = it },
                                    label = { Text("Shift End (HH:MM)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Button(
                                onClick = {
                                    val targetId = if (isHodOrAdmin) shiftTargetLoginId else staff?.loginId.orEmpty()
                                    onUpdateShiftTimeMapping(targetId, shiftStartInput, shiftEndInput)
                                },
                                enabled = !uiState.isSubmittingAction,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_shift_time_mapping_button")
                            ) {
                                Text("Save Shift Time Mapping")
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, SubtleBorder),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Image(
                                painter = painterResource(id = R.drawable.img_gmail_sync_art),
                                contentDescription = stringResource(R.string.cd_gmail_sync_art),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Login ID ↔ Gmail Mapping",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = "Link your college staff Login ID to a verified Gmail ID for OTP login and email notifications.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (staff != null) {
                                    StatusPill(label = if (staff.gmailVerified) "VERIFIED" else "PENDING")
                                }
                            }

                            if (staff != null) {
                                Surface(
                                    color = if (staff.gmailVerified) EmeraldContainer.copy(alpha = 0.5f) else AmberContainer.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.VerifiedUser,
                                            contentDescription = null,
                                            tint = if (staff.gmailVerified) EmeraldSuccess else AmberWarning
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Active ID: ${staff.loginId}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Mapped Gmail: ${staff.gmailId}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }

                            if (uiState.pendingGmailLinkOtp == null) {
                                OutlinedTextField(
                                    value = newGmailInput,
                                    onValueChange = { newGmailInput = it },
                                    label = { Text("Gmail Address to Map / Verify") },
                                    leadingIcon = { Icon(Icons.Filled.AlternateEmail, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("map_gmail_input")
                                )

                                Button(
                                    onClick = { onInitiateGmailMapping(newGmailInput) },
                                    enabled = !uiState.isSubmittingAction,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("send_mapping_otp_button")
                                ) {
                                    Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Send Verification OTP")
                                }
                            } else {
                                Text(
                                    text = "Enter the 6-digit code sent to ${uiState.pendingGmailAddress}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.SemiBold
                                )
                                OutlinedTextField(
                                    value = linkOtpInput,
                                    onValueChange = { if (it.length <= 6) linkOtpInput = it },
                                    label = { Text("6-Digit Verification Code") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("confirm_mapping_otp_input")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            linkOtpInput = ""
                                            onCancelGmailMapping()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cancel")
                                    }
                                    Button(
                                        onClick = {
                                            onConfirmGmailMapping(linkOtpInput)
                                            linkOtpInput = ""
                                        },
                                        enabled = !uiState.isSubmittingAction,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("confirm_mapping_button")
                                    ) {
                                        Text("Confirm Mapping")
                                    }
                                }
                            }
                        }
                    }
                }

                if (isHodOrAdmin) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            border = BorderStroke(1.dp, SubtleBorder),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.HowToReg,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Pending Staff Approvals (${uiState.pendingStaffRegistrations.size})",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                if (uiState.pendingStaffRegistrations.isEmpty()) {
                                    Text(
                                        text = "No pending Gmail registrations awaiting approval.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    uiState.pendingStaffRegistrations.forEach { candidate ->
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
                                                Text(
                                                    text = "${candidate.fullName} • ${candidate.department}",
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                Text(
                                                    text = "${candidate.loginId} ↔ ${candidate.gmailId}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    OutlinedButton(
                                                        onClick = { onReviewStaffRegistration(candidate.id, false) },
                                                        enabled = !uiState.isSubmittingAction,
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonError),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Reject")
                                                    }
                                                    Button(
                                                        onClick = { onReviewStaffRegistration(candidate.id, true) },
                                                        enabled = !uiState.isSubmittingAction,
                                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .testTag("approve_staff_${candidate.id}")
                                                    ) {
                                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Approve")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            border = BorderStroke(1.dp, SubtleBorder),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "HOD / Admin Staff ID ↔ Gmail Mapper",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = adminLoginIdInput,
                                        onValueChange = { adminLoginIdInput = it },
                                        label = { Text("Staff Login ID") },
                                        leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = adminGmailInput,
                                        onValueChange = { adminGmailInput = it },
                                        label = { Text("Gmail ID") },
                                        leadingIcon = { Icon(Icons.Filled.AlternateEmail, contentDescription = null) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.3f)
                                    )
                                }
                                Button(
                                    onClick = {
                                        onAdminMapGmail(adminLoginIdInput, adminGmailInput)
                                        adminLoginIdInput = ""
                                        adminGmailInput = ""
                                    },
                                    enabled = !uiState.isSubmittingAction,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Save Staff Mapping")
                                }

                                if (uiState.staffDirectory.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Mapped Staff Directory (${uiState.staffDirectory.size})",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    uiState.staffDirectory.take(10).forEach { member ->
                                        StaffDirectoryRow(member)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMessageThreadCard(
    thread: AdminMessageThreadDto,
    currentUserRole: String,
    replyDraft: String,
    isSubmitting: Boolean,
    onDraftChanged: (String) -> Unit,
    onSendReply: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, SubtleBorder),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Admin Sender Header
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
                        color = BlueContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = "Admin",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = thread.senderName.ifBlank { "College Admin" },
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = thread.createdAt,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusPill(
                    label = if (thread.targetLoginId.isNullOrBlank()) "ALL STAFF" else "TO ${thread.targetLoginId}"
                )
            }

            if (thread.title.isNotBlank()) {
                Text(
                    text = thread.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = thread.messageBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // User Replies Thread
            if (thread.replies.isNotEmpty()) {
                HorizontalDivider(color = SubtleBorder)
                Text(
                    text = "Replies (${thread.replies.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                thread.replies.forEach { reply ->
                    val isReplyFromAdmin = reply.senderRole.equals("ADMIN", ignoreCase = true)
                    Surface(
                        color = if (isReplyFromAdmin) BlueContainer.copy(alpha = 0.45f) else SoftWhiteTint,
                        border = BorderStroke(1.dp, SubtleBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${reply.senderName} (${reply.senderLoginId})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                StatusPill(label = if (isReplyFromAdmin) "ADMIN" else "USER REPLY")
                            }
                            Text(
                                text = reply.messageBody,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = reply.createdAt,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Inline Reply Box
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = replyDraft,
                    onValueChange = onDraftChanged,
                    placeholder = {
                        Text(
                            if (currentUserRole == "ADMIN") "Reply to staff..." else "Reply to Admin..."
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reply_input_${thread.id}")
                )
                Button(
                    onClick = onSendReply,
                    enabled = !isSubmitting && replyDraft.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("reply_send_${thread.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = "Send Reply",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionPillButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StaffDirectoryRow(member: StaffProfile) {
    Surface(
        color = SoftWhiteTint,
        border = BorderStroke(1.dp, SubtleBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${member.fullName} (${member.department})",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${member.loginId} ↔ ${member.gmailId}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusPill(label = if (member.gmailVerified) "VERIFIED" else "PENDING")
        }
    }
}
