package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.FeedbackBanner
import com.example.ui.components.ServerSetupBottomSheet
import com.example.ui.components.SmtpReceiptCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftWhiteTint
import com.example.ui.theme.SubtleBorder
import com.example.ui.viewmodel.AuthModeTab
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    uiState: AuthUiState,
    viewModel: AuthViewModel
) {
    var showServerSheet by remember { mutableStateOf(false) }

    var loginIdentifier by rememberSaveable { mutableStateOf("") }
    var loginPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var otpCode by rememberSaveable { mutableStateOf("") }

    var regLoginId by rememberSaveable { mutableStateOf("") }
    var regGmailId by rememberSaveable { mutableStateOf("") }
    var regFullName by rememberSaveable { mutableStateOf("") }
    var regDepartment by rememberSaveable { mutableStateOf("") }
    var regDesignation by rememberSaveable { mutableStateOf("Assistant Professor") }
    var regPassword by rememberSaveable { mutableStateOf("") }
    var regRole by rememberSaveable { mutableStateOf("STAFF") }
    var roleExpanded by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = PureWhite
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureWhite)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Crisp White College Header Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = BorderStroke(1.dp, SubtleBorder),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = BlueContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.School,
                                            contentDescription = "College Crest",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = stringResource(R.string.college_full_name),
                                        style = MaterialTheme.typography.titleLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Staff Attendance & Gmail Portal",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                color = if (uiState.healthStatus?.mysqlConnected == true) EmeraldContainer else SoftWhiteTint,
                                border = BorderStroke(1.dp, SubtleBorder),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { showServerSheet = true }
                                    .testTag("server_status_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (uiState.healthStatus?.mysqlConnected == true) {
                                            Icons.Filled.CloudDone
                                        } else {
                                            Icons.Filled.CloudQueue
                                        },
                                        contentDescription = "Server Status",
                                        tint = if (uiState.healthStatus?.mysqlConnected == true) {
                                            EmeraldSuccess
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (uiState.healthStatus?.mysqlConnected == true) "Online" else "Server",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (uiState.healthStatus?.mysqlConnected == true) {
                                            EmeraldSuccess
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                                }
                            }
                        }

                        Image(
                            painter = painterResource(id = R.drawable.img_campus_banner),
                            contentDescription = stringResource(R.string.cd_campus_banner),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(116.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }
                }

                // Clean 2-Tab Switcher: Gmail Mapping Login | Registration (Gmail)
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
                        AuthModeButton(
                            title = "Gmail Mapping Login",
                            selected = uiState.selectedTab == AuthModeTab.LOGIN,
                            onClick = { viewModel.selectTab(AuthModeTab.LOGIN) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_gmail_login")
                        )
                        AuthModeButton(
                            title = "Registration (Gmail)",
                            selected = uiState.selectedTab == AuthModeTab.REGISTER,
                            onClick = { viewModel.selectTab(AuthModeTab.REGISTER) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_register_staff")
                        )
                    }
                }

                FeedbackBanner(
                    bannerMessage = uiState.infoMessage,
                    errorMessage = uiState.errorMessage,
                    isBackendMissing = uiState.isBackendMissing,
                    onOpenServerSetup = { showServerSheet = true },
                    onDismiss = { viewModel.clearMessages() }
                )

                SmtpReceiptCard(receipt = uiState.smtpReceipt)

                when (uiState.selectedTab) {
                    AuthModeTab.LOGIN -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            border = BorderStroke(1.dp, SubtleBorder),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Gmail Mapping Login",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "Sign in using your mapped Gmail ID or College Login ID (enabled after Admin approval).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = loginIdentifier,
                                    onValueChange = { loginIdentifier = it },
                                    label = { Text("Mapped Gmail ID or Login ID") },
                                    leadingIcon = { Icon(Icons.Filled.AlternateEmail, contentDescription = null) },
                                    singleLine = true,
                                    enabled = uiState.otpDispatchData == null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_identifier_input")
                                )

                                if (!uiState.useGmailOtpInLogin) {
                                    OutlinedTextField(
                                        value = loginPassword,
                                        onValueChange = { loginPassword = it },
                                        label = { Text("Password") },
                                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                        trailingIcon = {
                                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                Icon(
                                                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                    contentDescription = "Toggle password visibility"
                                                )
                                            }
                                        },
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("login_password_input")
                                    )

                                    Button(
                                        onClick = { viewModel.signInWithPassword(loginIdentifier, loginPassword) },
                                        enabled = !uiState.isLoading,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("login_submit_button")
                                    ) {
                                        if (uiState.isLoading) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text("Login with Gmail Mapping")
                                        }
                                    }

                                    TextButton(
                                        onClick = { viewModel.setUseGmailOtp(true) },
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .testTag("switch_to_gmail_otp_button")
                                    ) {
                                        Icon(Icons.Filled.MarkEmailUnread, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Or Login via Gmail SMTP OTP")
                                    }
                                } else {
                                    if (uiState.otpDispatchData == null) {
                                        Button(
                                            onClick = { viewModel.requestGmailOtp(loginIdentifier) },
                                            enabled = !uiState.isLoading,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp)
                                                .testTag("request_otp_button")
                                        ) {
                                            if (uiState.isLoading) {
                                                CircularProgressIndicator(
                                                    color = Color.White,
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(Icons.Filled.MarkEmailUnread, contentDescription = null)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Send 6-Digit Gmail OTP")
                                            }
                                        }
                                    } else {
                                        Surface(
                                            color = EmeraldContainer,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "ID ${uiState.otpDispatchData.loginId} → ${uiState.otpDispatchData.maskedGmail}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                color = EmeraldSuccess,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }

                                        OutlinedTextField(
                                            value = otpCode,
                                            onValueChange = { if (it.length <= 6) otpCode = it },
                                            label = { Text("6-Digit Gmail OTP") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("otp_code_input")
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    otpCode = ""
                                                    viewModel.resetOtpStep()
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Change Gmail")
                                            }
                                            Button(
                                                onClick = { viewModel.verifyGmailOtp(loginIdentifier, otpCode) },
                                                enabled = !uiState.isLoading,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("verify_otp_button")
                                            ) {
                                                Text("Verify & Login")
                                            }
                                        }
                                    }

                                    TextButton(
                                        onClick = { viewModel.setUseGmailOtp(false) },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text("Back to Password Login")
                                    }
                                }
                            }
                        }
                    }

                    AuthModeTab.REGISTER -> {
                        val pending = uiState.pendingRegistration
                        if (pending != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = PureWhite),
                                border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        color = AmberContainer,
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.HourglassTop,
                                                contentDescription = "Pending Admin Approval",
                                                tint = AmberWarning,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    StatusPill(label = "PENDING")
                                    Text(
                                        text = "Awaiting Admin Approval",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = "${pending.fullName} (${pending.loginId})\nMapped Gmail: ${pending.gmailId}\n\n" +
                                            "Your registration is saved in MySQL. Once the College Admin approves your account, you will receive an SMTP confirmation on Gmail and can log in.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = { viewModel.clearPendingRegistration() },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("go_to_login_after_register")
                                    ) {
                                        Text("Go to Gmail Mapping Login")
                                    }
                                }
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = PureWhite),
                                border = BorderStroke(1.dp, SubtleBorder),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "New Staff Registration via Gmail",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = "Register your College Login ID with your Gmail ID. After Admin approval, you can sign in.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    OutlinedTextField(
                                        value = regGmailId,
                                        onValueChange = { regGmailId = it },
                                        label = { Text("Gmail ID (for Mapping & SMTP Alerts)") },
                                        leadingIcon = { Icon(Icons.Filled.AlternateEmail, contentDescription = null) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("reg_gmail_input")
                                    )

                                    OutlinedTextField(
                                        value = regLoginId,
                                        onValueChange = { regLoginId = it },
                                        label = { Text("College Login ID (e.g. PTTP-FAC-104)") },
                                        leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("reg_login_id_input")
                                    )

                                    OutlinedTextField(
                                        value = regFullName,
                                        onValueChange = { regFullName = it },
                                        label = { Text("Full Name") },
                                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = regDepartment,
                                            onValueChange = { regDepartment = it },
                                            label = { Text("Department") },
                                            leadingIcon = { Icon(Icons.Filled.Business, contentDescription = null) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = regDesignation,
                                            onValueChange = { regDesignation = it },
                                            label = { Text("Designation") },
                                            leadingIcon = { Icon(Icons.Filled.WorkOutline, contentDescription = null) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    ExposedDropdownMenuBox(
                                        expanded = roleExpanded,
                                        onExpandedChange = { roleExpanded = !roleExpanded }
                                    ) {
                                        OutlinedTextField(
                                            value = regRole,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Staff Role") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                                            modifier = Modifier
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                                .fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = roleExpanded,
                                            onDismissRequest = { roleExpanded = false }
                                        ) {
                                            listOf("STAFF", "HOD").forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option) },
                                                    onClick = {
                                                        regRole = option
                                                        roleExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = regPassword,
                                        onValueChange = { regPassword = it },
                                        label = { Text("Create Password (min 6 chars)") },
                                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                        visualTransformation = PasswordVisualTransformation(),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            viewModel.registerStaff(
                                                loginId = regLoginId,
                                                gmailId = regGmailId,
                                                fullName = regFullName,
                                                department = regDepartment,
                                                designation = regDesignation,
                                                password = regPassword,
                                                role = regRole
                                            )
                                        },
                                        enabled = !uiState.isLoading,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("register_submit_button")
                                    ) {
                                        if (uiState.isLoading) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text("Register & Request Admin Approval")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = { showServerSheet = true },
                        modifier = Modifier.testTag("configure_php_server_button")
                    ) {
                        Text("PHP & MySQL Server: ${uiState.currentBaseUrl}")
                    }
                }
            }
        }
    }

    if (showServerSheet) {
        ServerSetupBottomSheet(
            currentBaseUrl = uiState.currentBaseUrl,
            healthStatus = uiState.healthStatus,
            healthError = uiState.healthError,
            isChecking = uiState.isCheckingHealth,
            onCheckHealth = { viewModel.checkServerHealth() },
            onSaveBaseUrl = { viewModel.updateBaseUrl(it) },
            onDismiss = { showServerSheet = false }
        )
    }
}

@Composable
private fun AuthModeButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(44.dp)
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
