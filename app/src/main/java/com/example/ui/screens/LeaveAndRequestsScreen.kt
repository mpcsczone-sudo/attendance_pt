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
import androidx.compose.material.icons.filled.Approval
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.LeaveRequestDto
import com.example.ui.components.FeedbackBanner
import com.example.ui.components.StatusPill
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.DashboardUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveAndRequestsScreen(
    uiState: DashboardUiState,
    onSubmitLeave: (leaveType: String, startDate: String, endDate: String, reason: String) -> Unit,
    onReviewLeave: (requestId: Int, approve: Boolean, reviewNote: String) -> Unit,
    onCancelLeave: (requestId: Int) -> Unit,
    onRefresh: () -> Unit,
    onOpenServerSetup: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var leaveType by rememberSaveable { mutableStateOf("CASUAL") }
    var typeExpanded by remember { mutableStateOf(false) }
    var startDate by rememberSaveable { mutableStateOf(todayIso) }
    var endDate by rememberSaveable { mutableStateOf(todayIso) }
    var reason by rememberSaveable { mutableStateOf("") }

    val isHodOrAdmin = uiState.staff?.role?.equals("HOD", ignoreCase = true) == true ||
        uiState.staff?.role?.equals("ADMIN", ignoreCase = true) == true
    var showApprovalsTab by rememberSaveable { mutableStateOf(false) }

    var reviewTarget by remember { mutableStateOf<Pair<LeaveRequestDto, Boolean>?>(null) }
    var reviewNoteInput by remember { mutableStateOf("") }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .testTag("leaves_pull_refresh")
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

            // Submit Leave / Regularization Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Submit Leave / Regularization",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Submits to MySQL and dispatches an instant SMTP email to your HOD and mapped Gmail.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        ExposedDropdownMenuBox(
                            expanded = typeExpanded,
                            onExpandedChange = { typeExpanded = !typeExpanded }
                        ) {
                            OutlinedTextField(
                                value = leaveType.replace('_', ' '),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Request Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = typeExpanded,
                                onDismissRequest = { typeExpanded = false }
                            ) {
                                listOf("CASUAL", "EARNED", "MEDICAL", "ON_DUTY", "REGULARIZATION").forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.replace('_', ' ')) },
                                        onClick = {
                                            leaveType = option
                                            typeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = startDate,
                                onValueChange = { startDate = it },
                                label = { Text("From (YYYY-MM-DD)") },
                                leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("leave_start_date_input")
                            )
                            OutlinedTextField(
                                value = endDate,
                                onValueChange = { endDate = it },
                                label = { Text("To (YYYY-MM-DD)") },
                                leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("leave_end_date_input")
                            )
                        }

                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text("Reason / Academic Coverage Details") },
                            leadingIcon = { Icon(Icons.Filled.NoteAlt, contentDescription = null) },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("leave_reason_input")
                        )

                        Button(
                            onClick = {
                                onSubmitLeave(leaveType, startDate, endDate, reason)
                                reason = ""
                            },
                            enabled = !uiState.isSubmittingAction,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_leave_button")
                        ) {
                            if (uiState.isSubmittingAction) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Submit & Notify via SMTP")
                            }
                        }
                    }
                }
            }

            // Switcher for My Requests vs HOD Approvals
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !showApprovalsTab,
                        onClick = { showApprovalsTab = false },
                        label = { Text("My Requests (${uiState.myLeaves.size})") },
                        modifier = Modifier.testTag("filter_my_leaves")
                    )
                    if (isHodOrAdmin) {
                        FilterChip(
                            selected = showApprovalsTab,
                            onClick = { showApprovalsTab = true },
                            label = { Text("Approvals Queue (${uiState.pendingApprovals.size})") },
                            modifier = Modifier.testTag("filter_approvals_queue")
                        )
                    }
                }
            }

            val activeList = if (showApprovalsTab && isHodOrAdmin) uiState.pendingApprovals else uiState.myLeaves

            if (activeList.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
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
                                imageVector = Icons.Filled.Approval,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = if (showApprovalsTab) "No pending staff requests" else "No leave requests filed",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Submitted requests and HOD approval decisions automatically trigger real-time SMTP emails to the mapped Gmail address.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(activeList, key = { "${it.id}_${showApprovalsTab}" }) { req ->
                    val isPending = req.status.equals("PENDING", ignoreCase = true)
                    LeaveRequestCard(
                        item = req,
                        canReview = showApprovalsTab && isHodOrAdmin && isPending,
                        canCancel = !showApprovalsTab && isPending,
                        isSubmitting = uiState.isSubmittingAction,
                        onApprove = {
                            reviewNoteInput = "Approved"
                            reviewTarget = req to true
                        },
                        onReject = {
                            reviewNoteInput = ""
                            reviewTarget = req to false
                        },
                        onCancel = { onCancelLeave(req.id) }
                    )
                }
            }
        }
    }

    reviewTarget?.let { (target, isApprove) ->
        AlertDialog(
            onDismissRequest = { reviewTarget = null },
            title = {
                Text(if (isApprove) "Approve Request" else "Reject Request")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${target.fullName} (${target.loginId}) • ${target.leaveType}\n" +
                            "SMTP decision will be sent to: ${target.gmailId}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = reviewNoteInput,
                        onValueChange = { reviewNoteInput = it },
                        label = { Text("HOD / Principal Remark") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReviewLeave(target.id, isApprove, reviewNoteInput)
                        reviewTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isApprove) EmeraldSuccess else CrimsonError
                    )
                ) {
                    Text(if (isApprove) "Approve & Email" else "Reject & Email")
                }
            },
            dismissButton = {
                TextButton(onClick = { reviewTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LeaveRequestCard(
    item: LeaveRequestDto,
    canReview: Boolean,
    canCancel: Boolean,
    isSubmitting: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                StatusPill(label = item.leaveType)
                StatusPill(label = item.status)
            }

            if (item.fullName.isNotBlank()) {
                Text(
                    text = "${item.fullName} (${item.loginId}) • ${item.gmailId}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "${item.startDate} → ${item.endDate}",
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMonoFontFamily
            )

            Text(
                text = item.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!item.reviewNote.isNullOrBlank()) {
                Text(
                    text = "Review Note: ${item.reviewNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (canReview) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonError),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reject")
                    }
                    Button(
                        onClick = onApprove,
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approve")
                    }
                }
            } else if (canCancel) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isSubmitting,
                    border = BorderStroke(1.dp, CrimsonError),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonError),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cancel_leave_${item.id}")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel Request", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cancel Leave Request")
                }
            }
        }
    }
}
