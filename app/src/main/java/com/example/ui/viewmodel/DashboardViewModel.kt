package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SessionPreferences
import com.example.data.model.AdminMessageThreadDto
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
import com.example.location.GeoLocationHelper
import com.example.location.GeoTagResult
import com.example.notifications.ShiftReminderScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DashboardUiState(
    val staff: StaffProfile? = null,
    val baseUrl: String = SessionPreferences.DEFAULT_BASE_URL,
    val selectedMonth: String = currentYearMonth(),
    val reminderEnabled: Boolean = true,
    val nextReminderLabel: String = "",
    val isRefreshing: Boolean = false,
    val isSubmittingAction: Boolean = false,
    val todayRecord: AttendanceRecordDto? = null,
    val summary: AttendanceSummaryDto = AttendanceSummaryDto(month = currentYearMonth()),
    val syncStatusText: String = "",
    val lastAutoSyncedAt: String = "",
    val summarySyncError: String? = null,
    val attendanceHistory: List<AttendanceRecordDto> = emptyList(),
    val departmentRegister: List<AttendanceRecordDto> = emptyList(),
    val myLeaves: List<LeaveRequestDto> = emptyList(),
    val pendingApprovals: List<LeaveRequestDto> = emptyList(),
    val adminMessages: List<AdminMessageThreadDto> = emptyList(),
    val smtpLogs: List<SmtpLogDto> = emptyList(),
    val staffDirectory: List<StaffProfile> = emptyList(),
    val selectedDashboardStaff: StaffProfile? = null,
    val pendingStaffRegistrations: List<StaffProfile> = emptyList(),
    val pendingGmailLinkOtp: OtpDispatchData? = null,
    val pendingGmailAddress: String = "",
    val lastSmtpReceipt: SmtpReceipt? = null,
    val capturedGeoTag: GeoTagResult? = null,
    val isCapturingGeo: Boolean = false,
    val bannerMessage: String? = null,
    val errorMessage: String? = null,
    val isBackendMissing: Boolean = false,
    val healthStatus: HealthStatus? = null,
    val isCheckingHealth: Boolean = false
)

class DashboardViewModel(
    private val appContext: Context,
    private val repository: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var attendanceFetchJob: Job? = null
    private var silentSyncJob: Job? = null

    init {
        viewModelScope.launch {
            repository.sessionFlow.collect { stored ->
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val checkedInToday = stored.lastCheckedInDate == todayStr
                val staff = stored.staff
                val nextLabel = if (staff != null && stored.reminderEnabled) {
                    ShiftReminderScheduler.computeNextTriggerFormatted(
                        shiftStartTime = staff.shiftStartTime,
                        alreadyCheckedInToday = checkedInToday
                    )
                } else {
                    "Paused"
                }

                val prevRole = _uiState.value.staff?.role
                _uiState.update {
                    it.copy(
                        staff = staff,
                        baseUrl = stored.baseUrl,
                        reminderEnabled = stored.reminderEnabled,
                        nextReminderLabel = nextLabel
                    )
                }

                if (staff != null) {
                    ShiftReminderScheduler.scheduleReminder(
                        context = appContext,
                        staffName = staff.fullName,
                        loginId = staff.loginId,
                        shiftStartTime = staff.shiftStartTime,
                        alreadyCheckedInToday = checkedInToday,
                        enabled = stored.reminderEnabled
                    )
                    val isAdminOrHod = staff.role.equals("ADMIN", ignoreCase = true) ||
                        staff.role.equals("HOD", ignoreCase = true)
                    if (isAdminOrHod && (prevRole != staff.role || _uiState.value.staffDirectory.isEmpty())) {
                        refreshAdminDirectoryOnly()
                    }
                }
            }
        }

        viewModelScope.launch {
            repository.cachedAttendanceFlow.collect { cached ->
                val selected = _uiState.value.selectedDashboardStaff
                val isViewingOwn = selected == null || selected.id == _uiState.value.staff?.id
                if (isViewingOwn && cached.isNotEmpty()) {
                    val month = _uiState.value.selectedMonth
                    val shouldPopulateHistory = _uiState.value.attendanceHistory.isEmpty()
                    val shouldFallbackSummary = _uiState.value.summary.totalDays == 0
                    if (shouldPopulateHistory || shouldFallbackSummary) {
                        val fallbackSummary = if (shouldFallbackSummary) {
                            repository.computeSummaryFromRecords(month, cached)
                        } else {
                            _uiState.value.summary
                        }
                        _uiState.update {
                            it.copy(
                                attendanceHistory = if (shouldPopulateHistory) cached else it.attendanceHistory,
                                summary = fallbackSummary
                            )
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            _uiState
                .map { it.selectedMonth }
                .distinctUntilChanged()
                .collectLatest { month ->
                    repository.observeCachedMonthlySummary(month).collect { cachedSummary ->
                        val selected = _uiState.value.selectedDashboardStaff
                        val isViewingOwn = selected == null || selected.id == _uiState.value.staff?.id
                        if (isViewingOwn && cachedSummary != null && _uiState.value.selectedMonth == month && _uiState.value.summary.totalDays == 0) {
                            _uiState.update { it.copy(summary = cachedSummary) }
                        }
                    }
                }
        }

        viewModelScope.launch {
            repository.cachedAdminMessagesFlow.collect { cached ->
                if (_uiState.value.adminMessages.isEmpty() && cached.isNotEmpty()) {
                    _uiState.update { it.copy(adminMessages = cached) }
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

        viewModelScope.launch {
            _uiState.map { it.bannerMessage }.distinctUntilChanged().collectLatest { msg ->
                if (!msg.isNullOrBlank()) {
                    delay(2_000L)
                    _uiState.update { if (it.bannerMessage == msg) it.copy(bannerMessage = null) else it }
                }
            }
        }

        viewModelScope.launch {
            _uiState.map { it.syncStatusText }.distinctUntilChanged().collectLatest { status ->
                if (status.isNotBlank() && !status.startsWith("Loading")) {
                    delay(2_000L)
                    _uiState.update { if (it.syncStatusText == status) it.copy(syncStatusText = "") else it }
                }
            }
        }

        viewModelScope.launch {
            while (true) {
                delay(30_000L)
                val state = _uiState.value
                if (state.staff != null && !state.isRefreshing && !state.isSubmittingAction) {
                    silentSyncInBackground()
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

    fun changeMonth(deltaMonths: Int) {
        val fmt = SimpleDateFormat("yyyy-MM", Locale.US)
        val cal = Calendar.getInstance()
        try {
            val parsed = fmt.parse(_uiState.value.selectedMonth)
            if (parsed != null) cal.time = parsed
        } catch (_: Exception) {
        }
        cal.add(Calendar.MONTH, deltaMonths)
        val newMonth = fmt.format(cal.time)
        _uiState.update {
            it.copy(
                selectedMonth = newMonth,
                summary = AttendanceSummaryDto(month = newMonth),
                attendanceHistory = emptyList(),
                summarySyncError = null
            )
        }
        refreshAttendanceOnly(newMonth)
    }

    fun jumpToCurrentMonth() {
        val currentMonth = currentYearMonth()
        if (_uiState.value.selectedMonth == currentMonth) {
            refreshAttendanceOnly(currentMonth)
            return
        }
        _uiState.update {
            it.copy(
                selectedMonth = currentMonth,
                summary = AttendanceSummaryDto(month = currentMonth),
                attendanceHistory = emptyList(),
                summarySyncError = null
            )
        }
        refreshAttendanceOnly(currentMonth)
    }

    fun selectDashboardStaff(staffProfile: StaffProfile?) {
        val currentMonth = _uiState.value.selectedMonth
        _uiState.update {
            it.copy(
                selectedDashboardStaff = staffProfile,
                summary = AttendanceSummaryDto(month = currentMonth),
                attendanceHistory = emptyList(),
                summarySyncError = null,
                syncStatusText = if (staffProfile != null) {
                    "Loading ${staffProfile.fullName}'s $currentMonth attendance..."
                } else {
                    "Loading my $currentMonth attendance..."
                }
            )
        }
        refreshAttendanceOnly(currentMonth)
    }

    fun captureGeoLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCapturingGeo = true) }
            val result = GeoLocationHelper.captureCurrentLocation(appContext)
            _uiState.update { it.copy(isCapturingGeo = false, capturedGeoTag = result) }
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setReminderEnabled(enabled)
        }
    }

    fun refreshAll(manualSync: Boolean = false) {
        attendanceFetchJob?.cancel()
        attendanceFetchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshing = true,
                    errorMessage = null,
                    summarySyncError = null,
                    isBackendMissing = false
                )
            }
            val month = _uiState.value.selectedMonth
            val selectedTarget = _uiState.value.selectedDashboardStaff
            val actorRole = _uiState.value.staff?.role ?: "STAFF"
            val targetStaffId = if (actorRole.equals("ADMIN", ignoreCase = true)) selectedTarget?.id else null
            val targetLoginId = if (actorRole.equals("ADMIN", ignoreCase = true) && selectedTarget != null && selectedTarget.id != _uiState.value.staff?.id) {
                selectedTarget.loginId
            } else {
                null
            }
            val isViewingOwn = targetStaffId == null || targetStaffId == _uiState.value.staff?.id
            val attRes = repository.fetchAttendance(month, targetStaffId, targetLoginId)
            var resolvedRole = actorRole
            when (attRes) {
                is RepoResult.Success -> {
                    resolvedRole = attRes.data.staff?.role ?: actorRole
                    applyAttendanceSuccess(
                        data = attRes.data,
                        month = month,
                        requestedTargetId = targetStaffId,
                        selectedTarget = selectedTarget,
                        isViewingOwn = isViewingOwn,
                        manualSync = manualSync,
                        showSyncFeedback = true
                    )
                }
                is RepoResult.Error -> {
                    if (attRes.isUnauthorized) {
                        ShiftReminderScheduler.cancelReminder(appContext)
                        repository.logout()
                        return@launch
                    }
                    _uiState.update {
                        it.copy(
                            errorMessage = attRes.message,
                            summarySyncError = attRes.message,
                            isBackendMissing = attRes.isBackendMissing
                        )
                    }
                }
            }

            when (val msgRes = repository.fetchAdminMessages()) {
                is RepoResult.Success -> {
                    _uiState.update { it.copy(adminMessages = msgRes.data) }
                }
                is RepoResult.Error -> Unit
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

            if (resolvedRole.equals("HOD", ignoreCase = true) || resolvedRole.equals("ADMIN", ignoreCase = true)) {
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

    private fun refreshAdminDirectoryOnly() {
        viewModelScope.launch {
            when (val dirRes = repository.fetchStaffDirectory()) {
                is RepoResult.Success -> _uiState.update { it.copy(staffDirectory = dirRes.data) }
                is RepoResult.Error -> Unit
            }
            when (val pendingRes = repository.fetchPendingStaffApprovals()) {
                is RepoResult.Success -> _uiState.update { it.copy(pendingStaffRegistrations = pendingRes.data) }
                is RepoResult.Error -> Unit
            }
        }
    }

    private fun refreshAttendanceOnly(month: String) {
        attendanceFetchJob?.cancel()
        attendanceFetchJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, summarySyncError = null) }
            val selectedTarget = _uiState.value.selectedDashboardStaff
            val actorRole = _uiState.value.staff?.role ?: "STAFF"
            val targetStaffId = if (actorRole.equals("ADMIN", ignoreCase = true)) selectedTarget?.id else null
            val targetLoginId = if (actorRole.equals("ADMIN", ignoreCase = true) && selectedTarget != null && selectedTarget.id != _uiState.value.staff?.id) {
                selectedTarget.loginId
            } else {
                null
            }
            val isViewingOwn = targetStaffId == null || targetStaffId == _uiState.value.staff?.id
            when (val attRes = repository.fetchAttendance(month, targetStaffId, targetLoginId)) {
                is RepoResult.Success -> {
                    applyAttendanceSuccess(
                        data = attRes.data,
                        month = month,
                        requestedTargetId = targetStaffId,
                        selectedTarget = selectedTarget,
                        isViewingOwn = isViewingOwn,
                        manualSync = false,
                        showSyncFeedback = true
                    )
                    _uiState.update { it.copy(isRefreshing = false) }
                }
                is RepoResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessage = attRes.message,
                            summarySyncError = attRes.message,
                            isBackendMissing = attRes.isBackendMissing
                        )
                    }
                }
            }
        }
    }

    fun silentSyncInBackground() {
        val stateSnapshot = _uiState.value
        if (stateSnapshot.staff == null || stateSnapshot.isRefreshing || stateSnapshot.isSubmittingAction) {
            return
        }
        if (silentSyncJob?.isActive == true || attendanceFetchJob?.isActive == true) {
            return
        }
        silentSyncJob = viewModelScope.launch {
            val month = _uiState.value.selectedMonth
            val selectedTarget = _uiState.value.selectedDashboardStaff
            val actorRole = _uiState.value.staff?.role ?: "STAFF"
            val targetStaffId = if (actorRole.equals("ADMIN", ignoreCase = true)) selectedTarget?.id else null
            val targetLoginId = if (actorRole.equals("ADMIN", ignoreCase = true) && selectedTarget != null && selectedTarget.id != _uiState.value.staff?.id) {
                selectedTarget.loginId
            } else {
                null
            }
            val isViewingOwn = targetStaffId == null || targetStaffId == _uiState.value.staff?.id
            var resolvedRole = actorRole
            when (val attRes = repository.fetchAttendance(month, targetStaffId, targetLoginId)) {
                is RepoResult.Success -> {
                    resolvedRole = attRes.data.staff?.role ?: actorRole
                    applyAttendanceSuccess(
                        data = attRes.data,
                        month = month,
                        requestedTargetId = targetStaffId,
                        selectedTarget = selectedTarget,
                        isViewingOwn = isViewingOwn,
                        manualSync = false,
                        showSyncFeedback = false
                    )
                }
                is RepoResult.Error -> {
                    if (attRes.isUnauthorized) {
                        ShiftReminderScheduler.cancelReminder(appContext)
                        repository.logout()
                        return@launch
                    }
                }
            }

            when (val msgRes = repository.fetchAdminMessages()) {
                is RepoResult.Success -> _uiState.update { it.copy(adminMessages = msgRes.data) }
                is RepoResult.Error -> Unit
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
            if (resolvedRole.equals("HOD", ignoreCase = true) || resolvedRole.equals("ADMIN", ignoreCase = true)) {
                when (val dirRes = repository.fetchStaffDirectory()) {
                    is RepoResult.Success -> _uiState.update { it.copy(staffDirectory = dirRes.data) }
                    is RepoResult.Error -> Unit
                }
                when (val pendingRes = repository.fetchPendingStaffApprovals()) {
                    is RepoResult.Success -> _uiState.update { it.copy(pendingStaffRegistrations = pendingRes.data) }
                    is RepoResult.Error -> Unit
                }
            }
        }
    }

    private fun applyAttendanceSuccess(
        data: com.example.data.model.AttendanceListData,
        month: String,
        requestedTargetId: Int?,
        selectedTarget: StaffProfile?,
        isViewingOwn: Boolean,
        manualSync: Boolean,
        showSyncFeedback: Boolean = true
    ) {
        val currentSelectedId = if (_uiState.value.staff?.role?.equals("ADMIN", ignoreCase = true) == true) {
            _uiState.value.selectedDashboardStaff?.id
        } else {
            null
        }
        if (currentSelectedId != requestedTargetId || _uiState.value.selectedMonth != month) {
            return
        }

        val serverDidNotTarget = !isViewingOwn &&
            requestedTargetId != null &&
            (data.targetStaff == null || data.targetStaff.id != requestedTargetId)

        val selectedLoginId = selectedTarget?.loginId.orEmpty()
        val scopedRecords = if (!isViewingOwn && requestedTargetId != null) {
            data.records.filter { rec ->
                rec.staffId == requestedTargetId ||
                    (selectedLoginId.isNotBlank() && rec.loginId.equals(selectedLoginId, ignoreCase = true))
            }
        } else {
            data.records
        }

        val mergedRecords = if (!isViewingOwn && requestedTargetId != null && month == currentYearMonth()) {
            val existingDates = scopedRecords.map { it.attendanceDate.trim().take(10) }.toHashSet()
            val registerFallback = data.departmentRegister.filter { reg ->
                (reg.staffId == requestedTargetId ||
                    (selectedLoginId.isNotBlank() && reg.loginId.equals(selectedLoginId, ignoreCase = true))) &&
                    reg.attendanceDate.trim().take(10).startsWith(month) &&
                    reg.attendanceDate.trim().take(10) !in existingDates
            }
            scopedRecords + registerFallback
        } else {
            scopedRecords
        }

        val effectiveSummary = if (serverDidNotTarget) {
            repository.computeSummaryFromRecords(month, mergedRecords)
        } else {
            data.summary
        }

        val syncedAt = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val total = effectiveSummary.totalDays
        val resolvedTarget = data.targetStaff?.takeIf { it.id == requestedTargetId } ?: selectedTarget
        val targetName = resolvedTarget?.fullName
        val syncMsg = if (!isViewingOwn && !targetName.isNullOrBlank()) {
            "Synced $targetName ($month) • $total record(s) at $syncedAt"
        } else {
            "Synced $month • $total record(s) at $syncedAt"
        }

        _uiState.update {
            it.copy(
                selectedDashboardStaff = if (!isViewingOwn && data.targetStaff?.id == requestedTargetId) data.targetStaff else it.selectedDashboardStaff,
                todayRecord = if (isViewingOwn) data.today else it.todayRecord,
                summary = effectiveSummary,
                attendanceHistory = mergedRecords,
                departmentRegister = data.departmentRegister,
                lastAutoSyncedAt = syncedAt,
                syncStatusText = if (showSyncFeedback) syncMsg else it.syncStatusText,
                summarySyncError = if (serverDidNotTarget) {
                    "Live server attendance_list.php has not been updated for per-user lookup yet. Re-upload attendance_list.php on college hosting for complete multi-month user history."
                } else {
                    null
                },
                bannerMessage = if (showSyncFeedback && manualSync && !serverDidNotTarget) syncMsg else it.bannerMessage
            )
        }
    }

    fun markAttendance(
        action: String,
        remarks: String,
        faceVerified: Boolean = false,
        faceConfidence: Float? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null, bannerMessage = null) }
            val geo = _uiState.value.capturedGeoTag ?: GeoLocationHelper.captureCurrentLocation(appContext)
            when (val res = repository.markAttendance(action, remarks, geo, faceVerified, faceConfidence)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            todayRecord = res.data,
                            capturedGeoTag = geo,
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

    fun updateStaffTimeMapping(loginId: String, startTime: String, endTime: String) {
        if (loginId.isBlank() || startTime.isBlank() || endTime.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Provide Login ID, Shift Start (HH:MM), and Shift End (HH:MM).") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null, bannerMessage = null) }
            when (val res = repository.updateStaffTimeMapping(loginId, startTime, endTime)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            lastSmtpReceipt = res.smtpReceipt,
                            bannerMessage = res.message.ifBlank {
                                "Shift timing updated for ${res.data.loginId} (${res.data.shiftStartTime} – ${res.data.shiftEndTime})."
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

    fun cancelLeave(requestId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null, bannerMessage = null) }
            when (val res = repository.cancelLeaveRequest(requestId)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            myLeaves = res.data.myRequests,
                            pendingApprovals = res.data.pendingApprovals,
                            lastSmtpReceipt = res.smtpReceipt,
                            bannerMessage = res.message.ifBlank { "Pending leave request cancelled." }
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

    fun sendAdminMessage(targetLoginId: String?, title: String, messageBody: String) {
        if (title.isBlank() || messageBody.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter both a title and message to send to staff.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null, bannerMessage = null) }
            when (val res = repository.sendAdminMessage(targetLoginId, title, messageBody)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            adminMessages = res.data,
                            lastSmtpReceipt = res.smtpReceipt,
                            bannerMessage = res.message.ifBlank { "Admin message sent to staff." }
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

    fun replyToAdminMessage(parentId: Int, messageBody: String) {
        if (messageBody.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Write a reply message before sending.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAction = true, errorMessage = null, bannerMessage = null) }
            when (val res = repository.replyToAdminMessage(parentId, messageBody)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAction = false,
                            adminMessages = res.data,
                            lastSmtpReceipt = res.smtpReceipt,
                            bannerMessage = res.message.ifBlank { "Reply sent." }
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
                            bannerMessage = "Connected to College Server"
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
            ShiftReminderScheduler.cancelReminder(appContext)
            repository.logout()
        }
    }

    private suspend fun refreshSmtpLogsOnly() {
        when (val smtpRes = repository.fetchSmtpLogs()) {
            is RepoResult.Success -> _uiState.update { it.copy(smtpLogs = smtpRes.data) }
            is RepoResult.Error -> Unit
        }
    }

    class Factory(
        private val appContext: Context,
        private val repository: AttendanceRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(appContext.applicationContext, repository) as T
        }
    }
}

private fun currentYearMonth(): String {
    return SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
}
