package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SessionPreferences
import com.example.data.model.AttendanceRecordDto
import com.example.data.model.AttendanceSummaryDto
import com.example.data.model.HealthStatus
import com.example.data.model.LeaveRequestDto
import com.example.data.model.OtpDispatchData
import com.example.data.model.SmtpLogDto
import com.example.data.model.SmtpReceipt
import com.example.data.model.StaffProfile
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.RepoResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val staff: StaffProfile? = null,
    val baseUrl: String = SessionPreferences.DEFAULT_BASE_URL,
    val isRefreshing: Boolean = false,
    val isSubmittingAction: Boolean = false,
    val todayRecord: AttendanceRecordDto? = null,
    val summary: AttendanceSummaryDto = AttendanceSummaryDto(),
    val attendanceHistory: List<AttendanceRecordDto> = emptyList(),
    val departmentRegister: List<AttendanceRecordDto> = emptyList(),
    val myLeaves: List<LeaveRequestDto> = emptyList(),
    val pendingApprovals: List<LeaveRequestDto> = emptyList(),
    val smtpLogs: List<SmtpLogDto> = emptyList(),
    val staffDirectory: List<StaffProfile> = emptyList(),
    val pendingStaffRegistrations: List<StaffProfile> = emptyList(),
    val pendingGmailLinkOtp: OtpDispatchData? = null,
    val pendingGmailAddress: String = "",
    val lastSmtpReceipt: SmtpReceipt? = null,
    val bannerMessage: String? = null,
    val errorMessage: String? = null,
    val isBackendMissing: Boolean = false,
    val healthStatus: HealthStatus? = null,
    val isCheckingHealth: Boolean = false
)

class DashboardViewModel(
    private val repository: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.sessionFlow.collect { stored ->
                _uiState.update {
                    it.copy(
                        staff = stored.staff,
                        baseUrl = stored.baseUrl
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.cachedAttendanceFlow.collect { cached ->
                if (_uiState.value.attendanceHistory.isEmpty() && cached.isNotEmpty()) {
                    _uiState.update { it.copy(attendanceHistory = cached) }
                }
            }
        }
        viewModelScope.launch {
            repository.cachedMyLeavesFlow.collect { cached ->
                if (_uiState.value.myLeaves.isEmpty() && cached.isNotEmpty()) {
                    _uiState.update { it.copy(myLeaves = cached) }
                }
            }
        }
        viewModelScope.launch {
            repository.cachedApprovalQueueFlow.collect { cached ->
                if (_uiState.value.pendingApprovals.isEmpty() && cached.isNotEmpty()) {
                    _uiState.update { it.copy(pendingApprovals = cached) }
                }
            }
        }
        viewModelScope.launch {
            repository.cachedSmtpLogsFlow.collect { cached ->
                if (_uiState.value.smtpLogs.isEmpty() && cached.isNotEmpty()) {
                    _uiState.update { it.copy(smtpLogs = cached) }
                }
            }
        }
        viewModelScope.launch {
            repository.cachedStaffDirectoryFlow.collect { cached ->
                if (_uiState.value.staffDirectory.isEmpty() && cached.isNotEmpty()) {
                    _uiState.update { it.copy(staffDirectory = cached) }
                }
            }
        }
        refreshAll()
    }

    fun clearFeedback() {
        _uiState.update {
            it.copy(
                bannerMessage = null,
                errorMessage = null,
                isBackendMissing = false
            )
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null, isBackendMissing = false) }
            val attRes = repository.fetchAttendance()
            when (attRes) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            todayRecord = attRes.data.today,
                            summary = attRes.data.summary,
                            attendanceHistory = attRes.data.records,
                            departmentRegister = attRes.data.departmentRegister
                        )
                    }
                }
                is RepoResult.Error -> {
                    if (attRes.isUnauthorized) {
                        repository.logout()
                        return@launch
                    }
                    _uiState.update {
                        it.copy(
                            errorMessage = attRes.message,
                            isBackendMissing = attRes.isBackendMissing
                        )
                    }
                }
            }

            when (val leaveRes = repository.fetchLeaveRequests()) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            myLeaves = leaveRes.data.myRequests,
                            pendingApprovals = leaveRes.data.pendingApprovals
                        )
                    }
                }
                is RepoResult.Error -> Unit
            }

            when (val smtpRes = repository.fetchSmtpLogs()) {
                is RepoResult.Success -> {
                    _uiState.update { it.copy(smtpLogs = smtpRes.data) }
                }
                is RepoResult.Error -> Unit
            }

            val role = _uiState.value.staff?.role ?: "STAFF"
            if (role == "HOD" || role == "ADMIN") {
                when (val dirRes = repository.fetchStaffDirectory()) {
                    is RepoResult.Success -> {
                        _uiState.update { it.copy(staffDirectory = dirRes.data) }
                    }
                    is RepoResult.Error -> Unit
                }
                when (val pendingRes = repository.fetchPendingStaffApprovals()) {
                    is RepoResult.Success -> {
                        _uiState.update { it.copy(pendingStaffRegistrations = pendingRes.data) }
                    }
                    is RepoResult.Error -> Unit
                }
            }

            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun reviewStaffRegistration(staffId: Int, approve: Boolean, note: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null, bannerMessage = null) }
            when (val res = repository.reviewStaffRegistration(staffId, approve, note)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            pendingStaffRegistrations = res.data,
                            lastSmtpReceipt = res.smtpReceipt,
                            bannerMessage = res.message.ifBlank {
                                if (approve) "Staff registration approved & SMTP notification sent."
                                else "Staff registration rejected & SMTP notice sent."
                            }
                        )
                    }
                    refreshAll()
                }
                is RepoResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            errorMessage = res.message,
                            isBackendMissing = res.isBackendMissing
                        )
                    }
                }
            }
        }
    }

    fun markAttendance(action: String, remarks: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null, bannerMessage = null) }
            when (val res = repository.markAttendance(action, remarks)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            todayRecord = res.data,
                            lastSmtpReceipt = res.smtpReceipt,
                            bannerMessage = res.message.ifBlank { "Attendance recorded & SMTP email dispatched." }
                        )
                    }
                    refreshAll()
                }
                is RepoResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            errorMessage = res.message,
                            isBackendMissing = res.isBackendMissing
                        )
                    }
                }
            }
        }
    }

    fun submitLeave(leaveType: String, startDate: String, endDate: String, reason: String) {
        if (startDate.isBlank() || endDate.isBlank() || reason.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill start date, end date, and reason.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null, bannerMessage = null) }
            when (val res = repository.submitLeaveRequest(leaveType, startDate, endDate, reason)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            myLeaves = res.data.myRequests,
                            pendingApprovals = res.data.pendingApprovals,
                            lastSmtpReceipt = res.smtpReceipt,
                            bannerMessage = res.message.ifBlank { "Request submitted and HOD notified via SMTP." }
                        )
                    }
                    refreshSmtpLogsOnly()
                }
                is RepoResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            errorMessage = res.message,
                            isBackendMissing = res.isBackendMissing
                        )
                    }
                }
            }
        }
    }

    fun reviewLeave(requestId: Int, approve: Boolean, reviewNote: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null, bannerMessage = null) }
            when (val res = repository.reviewLeaveRequest(requestId, approve, reviewNote)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            myLeaves = res.data.myRequests,
                            pendingApprovals = res.data.pendingApprovals,
                            lastSmtpReceipt = res.smtpReceipt,
                            bannerMessage = res.message.ifBlank {
                                if (approve) "Approved & SMTP confirmation sent to staff Gmail."
                                else "Rejected & SMTP notice sent to staff Gmail."
                            }
                        )
                    }
                    refreshSmtpLogsOnly()
                }
                is RepoResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            errorMessage = res.message,
                            isBackendMissing = res.isBackendMissing
                        )
                    }
                }
            }
        }
    }

    fun initiateGmailMapping(newGmail: String) {
        val loginId = _uiState.value.staff?.loginId.orEmpty()
        if (loginId.isBlank() || !newGmail.contains("@")) {
            _uiState.update { it.copy(errorMessage = "Enter a valid Gmail address (e.g. faculty@gmail.com).") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null, bannerMessage = null) }
            when (val res = repository.initiateGmailLink(loginId, newGmail)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            pendingGmailLinkOtp = res.data,
                            pendingGmailAddress = newGmail.trim().lowercase(),
                            lastSmtpReceipt = res.smtpReceipt,
                            bannerMessage = res.message.ifBlank { "Verification OTP sent to $newGmail via SMTP." }
                        )
                    }
                    refreshSmtpLogsOnly()
                }
                is RepoResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            errorMessage = res.message,
                            isBackendMissing = res.isBackendMissing
                        )
                    }
                }
            }
        }
    }

    fun confirmGmailMapping(otp: String) {
        val loginId = _uiState.value.staff?.loginId.orEmpty()
        val gmail = _uiState.value.pendingGmailAddress
        if (otp.trim().length < 4) {
            _uiState.update { it.copy(errorMessage = "Enter the verification OTP sent to $gmail.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null, bannerMessage = null) }
            when (val res = repository.confirmGmailLink(loginId, gmail, otp)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            pendingGmailLinkOtp = null,
                            pendingGmailAddress = "",
                            lastSmtpReceipt = res.smtpReceipt,
                            bannerMessage = "Login ID $loginId mapped & verified with ${res.data.gmailId}."
                        )
                    }
                    refreshSmtpLogsOnly()
                }
                is RepoResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            errorMessage = res.message,
                            isBackendMissing = res.isBackendMissing
                        )
                    }
                }
            }
        }
    }

    fun cancelGmailMappingStep() {
        _uiState.update { it.copy(pendingGmailLinkOtp = null, pendingGmailAddress = "") }
    }

    fun adminUpdateStaffGmailMapping(targetLoginId: String, targetGmailId: String) {
        if (targetLoginId.isBlank() || !targetGmailId.contains("@")) {
            _uiState.update { it.copy(errorMessage = "Provide both Staff Login ID and a valid Gmail address.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null, bannerMessage = null) }
            when (val res = repository.adminMapGmail(targetLoginId, targetGmailId)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            lastSmtpReceipt = res.smtpReceipt,
                            bannerMessage = "Mapped ${res.data.loginId} ↔ ${res.data.gmailId} & sent SMTP notice."
                        )
                    }
                    refreshAll()
                }
                is RepoResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            errorMessage = res.message,
                            isBackendMissing = res.isBackendMissing
                        )
                    }
                }
            }
        }
    }

    fun sendTestSmtpAlert() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null, bannerMessage = null) }
            when (val res = repository.sendTestSmtp()) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            smtpLogs = res.data,
                            lastSmtpReceipt = res.smtpReceipt,
                            bannerMessage = res.message.ifBlank { "Test SMTP email dispatched to your mapped Gmail." }
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            errorMessage = res.message,
                            isBackendMissing = res.isBackendMissing
                        )
                    }
                }
            }
        }
    }

    fun checkServerHealth() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingHealth = true) }
            when (val res = repository.checkHealth()) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCheckingHealth = false,
                            healthStatus = res.data,
                            bannerMessage = "Connected to PHP ${res.data.phpVersion} & MySQL"
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isCheckingHealth = false,
                            healthStatus = null,
                            errorMessage = res.message
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

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    private suspend fun refreshSmtpLogsOnly() {
        when (val smtpRes = repository.fetchSmtpLogs()) {
            is RepoResult.Success -> _uiState.update { it.copy(smtpLogs = smtpRes.data) }
            is RepoResult.Error -> Unit
        }
    }

    class Factory(private val repository: AttendanceRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository) as T
        }
    }
}
