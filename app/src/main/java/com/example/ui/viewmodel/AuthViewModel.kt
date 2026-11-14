package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SessionPreferences
import com.example.data.model.HealthStatus
import com.example.data.model.OtpDispatchData
import com.example.data.model.RegisterStaffRequest
import com.example.data.model.SmtpReceipt
import com.example.data.model.StaffProfile
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.RepoResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthModeTab {
    LOGIN,
    REGISTER
}

data class AuthUiState(
    val selectedTab: AuthModeTab = AuthModeTab.LOGIN,
    val useGmailOtpInLogin: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isBackendMissing: Boolean = false,
    val infoMessage: String? = null,
    val smtpReceipt: SmtpReceipt? = null,
    val otpDispatchData: OtpDispatchData? = null,
    val pendingRegistration: StaffProfile? = null,
    val healthStatus: HealthStatus? = null,
    val isCheckingHealth: Boolean = false,
    val healthError: String? = null,
    val currentBaseUrl: String = SessionPreferences.DEFAULT_BASE_URL
)

class AuthViewModel(
    private val repository: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.sessionFlow.collect { stored ->
                _uiState.update { it.copy(currentBaseUrl = stored.baseUrl) }
            }
        }
        checkServerHealth()
    }

    fun selectTab(tab: AuthModeTab) {
        _uiState.update {
            it.copy(
                selectedTab = tab,
                errorMessage = null,
                infoMessage = null,
                isBackendMissing = false
            )
        }
    }

    fun setUseGmailOtp(enabled: Boolean) {
        _uiState.update {
            it.copy(
                useGmailOtpInLogin = enabled,
                otpDispatchData = null,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null, isBackendMissing = false) }
    }

    fun clearPendingRegistration() {
        _uiState.update { it.copy(pendingRegistration = null, selectedTab = AuthModeTab.LOGIN) }
    }

    fun checkServerHealth() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingHealth = true, healthError = null) }
            when (val res = repository.checkHealth()) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCheckingHealth = false,
                            healthStatus = res.data,
                            healthError = null
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isCheckingHealth = false,
                            healthStatus = null,
                            healthError = res.message
                        )
                    }
                }
            }
        }
    }

    fun updateBaseUrl(newUrl: String) {
        viewModelScope.launch {
            repository.saveBaseUrl(newUrl)
            checkServerHealth()
        }
    }

    fun signInWithPassword(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter your mapped Gmail ID (or Login ID) and password.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null, isBackendMissing = false) }
            when (val res = repository.login(identifier, password)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            infoMessage = "Signed in as ${res.data.staff.fullName}",
                            smtpReceipt = res.smtpReceipt
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = res.message,
                            isBackendMissing = res.isBackendMissing
                        )
                    }
                }
            }
        }
    }

    fun requestGmailOtp(identifier: String) {
        if (identifier.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter your mapped Gmail ID or College Login ID.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null, isBackendMissing = false) }
            when (val res = repository.requestGmailOtp(identifier)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            otpDispatchData = res.data,
                            smtpReceipt = res.smtpReceipt,
                            infoMessage = res.message.ifBlank { "6-digit OTP sent via SMTP to ${res.data.maskedGmail}" }
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = res.message,
                            isBackendMissing = res.isBackendMissing
                        )
                    }
                }
            }
        }
    }

    fun verifyGmailOtp(identifier: String, otp: String) {
        if (otp.trim().length < 4) {
            _uiState.update { it.copy(errorMessage = "Enter the 6-digit OTP received in your mapped Gmail.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null, isBackendMissing = false) }
            when (val res = repository.verifyGmailOtp(identifier, otp)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            otpDispatchData = null,
                            infoMessage = "Verified via Gmail OTP",
                            smtpReceipt = res.smtpReceipt
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = res.message,
                            isBackendMissing = res.isBackendMissing
                        )
                    }
                }
            }
        }
    }

    fun resetOtpStep() {
        _uiState.update { it.copy(otpDispatchData = null, errorMessage = null, infoMessage = null) }
    }

    fun registerStaff(
        loginId: String,
        gmailId: String,
        fullName: String,
        department: String,
        designation: String,
        password: String,
        role: String
    ) {
        if (loginId.isBlank() || !gmailId.contains("@") || fullName.isBlank() || department.isBlank() || password.length < 6) {
            _uiState.update {
                it.copy(errorMessage = "Complete all fields. Gmail must be valid and password must be at least 6 characters.")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null, isBackendMissing = false) }
            val req = RegisterStaffRequest(
                loginId = loginId.trim().uppercase(),
                gmailId = gmailId.trim().lowercase(),
                fullName = fullName.trim(),
                department = department.trim(),
                designation = designation.trim().ifBlank { "Assistant Professor" },
                password = password,
                role = role
            )
            when (val res = repository.registerStaff(req)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pendingRegistration = res.data,
                            smtpReceipt = res.smtpReceipt,
                            infoMessage = res.message.ifBlank {
                                "Registration submitted! Waiting for Admin approval before login is enabled."
                            }
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = res.message,
                            isBackendMissing = res.isBackendMissing
                        )
                    }
                }
            }
        }
    }

    class Factory(private val repository: AttendanceRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(repository) as T
        }
    }
}
