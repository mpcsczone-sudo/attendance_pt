package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.SmtpLogDto
import com.example.data.model.StaffProfile
import com.example.ui.components.FeedbackBanner
import com.example.ui.components.SmtpReceiptCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.DashboardUiState

@Composable
fun GmailMappingAndSmtpScreen(
    uiState: DashboardUiState,
    onInitiateGmailMapping: (String) -> Unit,
    onConfirmGmailMapping: (String) -> Unit,
    onCancelGmailMapping: () -> Unit,
    onAdminMapGmail: (loginId: String, gmailId: String) -> Unit,
    onReviewStaffRegistration: (staffId: Int, approve: Boolean) -> Unit,
    onSendTestSmtp: () -> Unit,
    onOpenServerSetup: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val staff = uiState.staff
    val isHodOrAdmin = staff?.role == "HOD" || staff?.role == "ADMIN"

    var newGmailInput by rememberSaveable(staff?.gmailId) {
        mutableStateOf(staff?.gmailId.orEmpty())
    }
    var linkOtpInput by rememberSaveable { mutableStateOf("") }

    var adminLoginIdInput by rememberSaveable { mutableStateOf("") }
    var adminGmailInput by rememberSaveable { mutableStateOf("") }

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

            // Visual Header Card for Login ID <-> Gmail Mapping
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                                .height(150.dp)
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
                                    text = "Link your college staff Login ID to a verified Gmail ID for passwordless OTP login and instant SMTP notifications.",
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
                                if (uiState.isSubmittingAction) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Send SMTP Verification OTP")
                                }
                            }
                        } else {
                            Text(
                                text = "Enter the 6-digit SMTP code sent to ${uiState.pendingGmailAddress}",
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

            // Admin / HOD Staff Registration Approvals + Mapper
            if (isHodOrAdmin) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                                        color = MaterialTheme.colorScheme.surfaceVariant,
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
                                                    Text("Approve & Email")
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                            Text(
                                text = "Directly bind or update any staff member's College Login ID to their Gmail notification address.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                Text("Save Staff Mapping & Send SMTP Notice")
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

            // Real-Time SMTP Notification Ledger Header + Test Dispatch
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Real-Time SMTP Dispatch Log",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Audited PHPMailer/SMTP deliveries from MySQL",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onSendTestSmtp,
                        enabled = !uiState.isSubmittingAction,
                        modifier = Modifier.testTag("send_test_smtp_button")
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test SMTP")
                    }
                }
            }

            if (uiState.smtpLogs.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MarkEmailRead,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = "No SMTP email receipts logged yet",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Check-ins, check-outs, OTP requests, and leave decisions automatically send real-time SMTP emails and appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.smtpLogs, key = { it.id }) { log ->
                    SmtpLogRow(log = log)
                }
            }
        }
    }
}

@Composable
private fun StaffDirectoryRow(member: StaffProfile) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
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

@Composable
private fun SmtpLogRow(log: SmtpLogDto) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(label = log.eventType)
                StatusPill(label = log.deliveryStatus)
            }
            Text(
                text = log.subject,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "To: ${log.recipientGmail}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (log.bodyPreview.isNotBlank()) {
                Text(
                    text = log.bodyPreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = log.smtpMessageId.ifBlank { "SMTP Queue" },
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = log.sentAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!log.errorMessage.isNullOrBlank()) {
                Text(
                    text = "Error: ${log.errorMessage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
