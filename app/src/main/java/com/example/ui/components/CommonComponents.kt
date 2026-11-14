package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.SessionPreferences
import com.example.data.model.HealthStatus
import com.example.data.model.SmtpReceipt
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun StatusPill(
    label: String,
    modifier: Modifier = Modifier
) {
    val normalized = label.uppercase()
    val (bg, fg) = when (normalized) {
        "PRESENT", "APPROVED", "SENT", "VERIFIED", "ONLINE" -> EmeraldContainer to EmeraldSuccess
        "LATE", "PENDING", "HALF_DAY", "QUEUED" -> AmberContainer to AmberWarning
        "ON_DUTY", "CASUAL", "EARNED", "MEDICAL", "REGULARIZATION" ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "ABSENT", "REJECTED", "FAILED", "OFFLINE" -> CrimsonContainer to CrimsonError
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = normalized.replace('_', ' '),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SmtpReceiptCard(
    receipt: SmtpReceipt?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = receipt != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        receipt?.let { item ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = EmeraldContainer.copy(alpha = 0.65f)
                ),
                border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.MarkEmailRead,
                        contentDescription = "SMTP Email Dispatched",
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Real-Time SMTP Email Dispatched",
                            style = MaterialTheme.typography.titleSmall,
                            color = EmeraldSuccess
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "To: ${item.recipientGmail}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (item.subject.isNotBlank()) {
                            Text(
                                text = item.subject,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (item.messageId.isNotBlank()) {
                            Text(
                                text = "SMTP ID: ${item.messageId}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeedbackBanner(
    bannerMessage: String?,
    errorMessage: String?,
    isBackendMissing: Boolean,
    onOpenServerSetup: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(bannerMessage) {
        if (!bannerMessage.isNullOrBlank()) {
            delay(2_000L)
            onDismiss()
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AnimatedVisibility(
            visible = !bannerMessage.isNullOrBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feedback_success_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Success",
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = bannerMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF0E4733),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) {
                        Text("OK", color = EmeraldSuccess)
                    }
                }
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isBackendMissing) AmberContainer else CrimsonContainer
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = if (isBackendMissing) Icons.Filled.WarningAmber else Icons.Filled.ErrorOutline,
                            contentDescription = "Notice",
                            tint = if (isBackendMissing) AmberWarning else CrimsonError,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (isBackendMissing) {
                            TextButton(
                                onClick = onOpenServerSetup,
                                modifier = Modifier.testTag("open_server_setup_from_error")
                            ) {
                                Text("PHP / MySQL Setup")
                            }
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSetupBottomSheet(
    currentBaseUrl: String,
    healthStatus: HealthStatus?,
    healthError: String?,
    isChecking: Boolean,
    onCheckHealth: () -> Unit,
    onSaveBaseUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var urlInput by remember(currentBaseUrl) { mutableStateOf(currentBaseUrl) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Dns,
                    contentDescription = "Server Configuration",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "College PHP & MySQL Backend",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Pt. T. P. Shukla College API Endpoint",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (healthStatus?.mysqlConnected == true) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                                contentDescription = "Connection Status",
                                tint = if (healthStatus?.mysqlConnected == true) EmeraldSuccess else AmberWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (healthStatus?.mysqlConnected == true) {
                                    "PHP & MySQL Online"
                                } else {
                                    "Awaiting Backend Connection"
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        if (isChecking) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            OutlinedButton(
                                onClick = onCheckHealth,
                                modifier = Modifier.testTag("ping_health_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Ping Server",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ping API")
                            }
                        }
                    }

                    if (healthStatus != null) {
                        HorizontalDivider()
                        Text(
                            text = "PHP: ${healthStatus.phpVersion}  •  MySQL: ${if (healthStatus.mysqlConnected) "Connected" else "Error"}  •  SMTP: ${if (healthStatus.smtpConfigured) "Ready" else "Unconfigured"}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                        if (healthStatus.serverTime.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Server Time: ${healthStatus.serverTime}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (!healthError.isNullOrBlank()) {
                        Text(
                            text = healthError,
                            style = MaterialTheme.typography.bodySmall,
                            color = AmberWarning
                        )
                    }
                }
            }

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("PHP API Base URL") },
                placeholder = { Text(SessionPreferences.DEFAULT_BASE_URL) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("api_base_url_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        urlInput = SessionPreferences.DEFAULT_BASE_URL
                        onSaveBaseUrl(SessionPreferences.DEFAULT_BASE_URL)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset Default")
                }
                Button(
                    onClick = { onSaveBaseUrl(urlInput) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_base_url_button")
                ) {
                    Text("Save & Test")
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "cPanel / Hosting Deployment Checklist",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "1. Import backend_php/schema.sql into your MySQL database in cPanel.\n" +
                            "2. Copy backend_php/config.example.php to config.php with MySQL credentials and SMTP settings (Gmail App Password or college SMTP).\n" +
                            "3. Upload backend_php/ contents to public_html/api/ on pttpshuklacollege.com.\n" +
                            "4. Verify https://pttpshuklacollege.com/api/health.php returns JSON status.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
