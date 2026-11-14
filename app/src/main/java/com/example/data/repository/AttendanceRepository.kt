package com.example.data.repository

import android.os.Build
import com.example.data.local.AttendanceDao
import com.example.data.local.AttendanceEntity
import com.example.data.local.LeaveRequestEntity
import com.example.data.local.SessionPreferences
import com.example.data.local.SmtpLogEntity
import com.example.data.local.StaffDirectoryEntity
import com.example.data.local.StoredSession
import com.example.data.model.AdminStaffApprovalRequest
import com.example.data.model.ApiResponse
import com.example.data.model.AttendanceListData
import com.example.data.model.AttendanceMarkRequest
import com.example.data.model.AttendanceRecordDto
import com.example.data.model.AuthSession
import com.example.data.model.GmailOtpActionRequest
import com.example.data.model.HealthStatus
import com.example.data.model.LeaveActionRequest
import com.example.data.model.LeaveListData
import com.example.data.model.LeaveRequestDto
import com.example.data.model.LoginRequest
import com.example.data.model.MapGmailRequest
import com.example.data.model.OtpDispatchData
import com.example.data.model.RegisterStaffRequest
import com.example.data.model.SmtpLogDto
import com.example.data.model.SmtpReceipt
import com.example.data.model.SmtpTestRequest
import com.example.data.model.StaffProfile
import com.example.data.remote.NetworkClient
import com.example.data.remote.PttpApiService
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Response
import java.io.IOException

sealed class RepoResult<out T> {
    data class Success<T>(
        val data: T,
        val message: String = "",
        val smtpDispatched: Boolean = false,
        val smtpReceipt: SmtpReceipt? = null
    ) : RepoResult<T>()

    data class Error(
        val message: String,
        val isBackendMissing: Boolean = false,
        val isUnauthorized: Boolean = false
    ) : RepoResult<Nothing>()
}

class AttendanceRepository(
    private val api: PttpApiService,
    private val prefs: SessionPreferences,
    private val dao: AttendanceDao
) {
    val sessionFlow: Flow<StoredSession> = prefs.sessionFlow

    val cachedAttendanceFlow: Flow<List<AttendanceRecordDto>> = dao.observeAttendance().map { list ->
        list.map {
            AttendanceRecordDto(
                id = it.id,
                staffId = it.staffId,
                loginId = it.loginId,
                fullName = it.fullName,
                department = it.department,
                gmailId = it.gmailId,
                attendanceDate = it.attendanceDate,
                checkInTime = it.checkInTime,
                checkOutTime = it.checkOutTime,
                status = it.status,
                remarks = it.remarks,
                checkInSource = it.checkInSource
            )
        }
    }

    val cachedMyLeavesFlow: Flow<List<LeaveRequestDto>> = dao.observeLeaveRequests(false).map { list ->
        list.map { it.toDto() }
    }

    val cachedApprovalQueueFlow: Flow<List<LeaveRequestDto>> = dao.observeLeaveRequests(true).map { list ->
        list.map { it.toDto() }
    }

    val cachedSmtpLogsFlow: Flow<List<SmtpLogDto>> = dao.observeSmtpLogs().map { list ->
        list.map {
            SmtpLogDto(
                id = it.id,
                staffId = it.staffId,
                recipientGmail = it.recipientGmail,
                eventType = it.eventType,
                subject = it.subject,
                bodyPreview = it.bodyPreview,
                smtpMessageId = it.smtpMessageId,
                deliveryStatus = it.deliveryStatus,
                errorMessage = it.errorMessage,
                sentAt = it.sentAt
            )
        }
    }

    val cachedStaffDirectoryFlow: Flow<List<StaffProfile>> = dao.observeStaffDirectory().map { list ->
        list.map {
            StaffProfile(
                id = it.id,
                loginId = it.loginId,
                gmailId = it.gmailId,
                gmailVerified = it.gmailVerified,
                fullName = it.fullName,
                department = it.department,
                designation = it.designation,
                role = it.role
            )
        }
    }

    suspend fun saveBaseUrl(url: String) {
        prefs.saveBaseUrl(url)
    }

    suspend fun logout() {
        prefs.clearSession()
        dao.clearAll()
    }

    suspend fun checkHealth(): RepoResult<HealthStatus> {
        return executeCall { api.checkHealth() }
    }

    suspend fun login(identifier: String, password: String): RepoResult<AuthSession> {
        val result = executeCall {
            api.login(LoginRequest(identifier = identifier.trim(), password = password))
        }
        if (result is RepoResult.Success) {
            prefs.saveAuthSession(result.data)
        }
        return result
    }

    suspend fun registerStaff(request: RegisterStaffRequest): RepoResult<StaffProfile> {
        return executeCall { api.registerStaff(request) }
    }

    suspend fun fetchPendingStaffApprovals(): RepoResult<List<StaffProfile>> {
        return executeCall { api.getPendingStaffApprovals() }
    }

    suspend fun reviewStaffRegistration(
        staffId: Int,
        approve: Boolean,
        note: String = ""
    ): RepoResult<List<StaffProfile>> {
        return executeCall {
            api.reviewStaffRegistration(
                AdminStaffApprovalRequest(
                    action = if (approve) "approve" else "reject",
                    staffId = staffId,
                    note = note.trim()
                )
            )
        }
    }

    suspend fun requestGmailOtp(identifier: String, purpose: String = "LOGIN"): RepoResult<OtpDispatchData> {
        return executeCall {
            api.requestGmailOtp(
                GmailOtpActionRequest(
                    action = "request_otp",
                    identifier = identifier.trim(),
                    purpose = purpose
                )
            )
        }
    }

    suspend fun verifyGmailOtp(identifier: String, otp: String): RepoResult<AuthSession> {
        val result = executeCall {
            api.verifyGmailOtp(
                GmailOtpActionRequest(
                    action = "verify_otp",
                    identifier = identifier.trim(),
                    otp = otp.trim(),
                    purpose = "LOGIN"
                )
            )
        }
        if (result is RepoResult.Success) {
            prefs.saveAuthSession(result.data)
        }
        return result
    }

    suspend fun initiateGmailLink(loginId: String, gmailId: String): RepoResult<OtpDispatchData> {
        return executeCall {
            api.initiateGmailLink(
                MapGmailRequest(
                    action = "initiate_link",
                    loginId = loginId.trim(),
                    gmailId = gmailId.trim()
                )
            )
        }
    }

    suspend fun confirmGmailLink(loginId: String, gmailId: String, otp: String): RepoResult<StaffProfile> {
        val result = executeCall {
            api.mapGmail(
                MapGmailRequest(
                    action = "confirm_link",
                    loginId = loginId.trim(),
                    gmailId = gmailId.trim(),
                    otp = otp.trim()
                )
            )
        }
        if (result is RepoResult.Success) {
            prefs.updateMappedGmail(result.data.gmailId, result.data.gmailVerified)
        }
        return result
    }

    suspend fun adminMapGmail(loginId: String, gmailId: String): RepoResult<StaffProfile> {
        return executeCall {
            api.mapGmail(
                MapGmailRequest(
                    action = "admin_map",
                    loginId = loginId.trim(),
                    gmailId = gmailId.trim()
                )
            )
        }
    }

    suspend fun markAttendance(action: String, remarks: String): RepoResult<AttendanceRecordDto> {
        val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
        return executeCall {
            api.markAttendance(
                AttendanceMarkRequest(
                    action = action,
                    remarks = remarks.trim(),
                    deviceInfo = deviceInfo
                )
            )
        }
    }

    suspend fun fetchAttendance(month: String? = null): RepoResult<AttendanceListData> {
        val result = executeCall { api.getAttendance(month) }
        if (result is RepoResult.Success) {
            val entities = result.data.records.map { it.toEntity() }
            dao.clearAttendance()
            dao.upsertAttendance(entities)
        }
        return result
    }

    suspend fun fetchLeaveRequests(): RepoResult<LeaveListData> {
        val result = executeCall { api.getLeaveRequests() }
        if (result is RepoResult.Success) {
            syncLeaveCache(result.data)
        }
        return result
    }

    suspend fun submitLeaveRequest(
        leaveType: String,
        startDate: String,
        endDate: String,
        reason: String
    ): RepoResult<LeaveListData> {
        val result = executeCall {
            api.submitOrReviewLeave(
                LeaveActionRequest(
                    action = "submit",
                    leaveType = leaveType,
                    startDate = startDate.trim(),
                    endDate = endDate.trim(),
                    reason = reason.trim()
                )
            )
        }
        if (result is RepoResult.Success) {
            syncLeaveCache(result.data)
        }
        return result
    }

    suspend fun reviewLeaveRequest(
        requestId: Int,
        approve: Boolean,
        reviewNote: String
    ): RepoResult<LeaveListData> {
        val result = executeCall {
            api.submitOrReviewLeave(
                LeaveActionRequest(
                    action = if (approve) "approve" else "reject",
                    requestId = requestId,
                    reviewNote = reviewNote.trim()
                )
            )
        }
        if (result is RepoResult.Success) {
            syncLeaveCache(result.data)
        }
        return result
    }

    suspend fun fetchStaffDirectory(): RepoResult<List<StaffProfile>> {
        val result = executeCall { api.getStaffDirectory() }
        if (result is RepoResult.Success) {
            dao.clearStaffDirectory()
            dao.upsertStaffDirectory(
                result.data.map {
                    StaffDirectoryEntity(
                        id = it.id,
                        loginId = it.loginId,
                        gmailId = it.gmailId,
                        gmailVerified = it.gmailVerified,
                        fullName = it.fullName,
                        department = it.department,
                        designation = it.designation,
                        role = it.role
                    )
                }
            )
        }
        return result
    }

    suspend fun fetchSmtpLogs(): RepoResult<List<SmtpLogDto>> {
        val result = executeCall { api.getSmtpLogs() }
        if (result is RepoResult.Success) {
            syncSmtpCache(result.data)
        }
        return result
    }

    suspend fun sendTestSmtp(): RepoResult<List<SmtpLogDto>> {
        val result = executeCall { api.sendTestSmtp(SmtpTestRequest()) }
        if (result is RepoResult.Success) {
            syncSmtpCache(result.data)
        }
        return result
    }

    private suspend fun syncLeaveCache(data: LeaveListData) {
        dao.clearLeaveRequests()
        val combined = data.myRequests.map { it.toEntity(false) } +
            data.pendingApprovals.map { it.toEntity(true) }
        dao.upsertLeaveRequests(combined)
    }

    private suspend fun syncSmtpCache(items: List<SmtpLogDto>) {
        dao.clearSmtpLogs()
        dao.upsertSmtpLogs(
            items.map {
                SmtpLogEntity(
                    id = it.id,
                    staffId = it.staffId,
                    recipientGmail = it.recipientGmail,
                    eventType = it.eventType,
                    subject = it.subject,
                    bodyPreview = it.bodyPreview,
                    smtpMessageId = it.smtpMessageId,
                    deliveryStatus = it.deliveryStatus,
                    errorMessage = it.errorMessage,
                    sentAt = it.sentAt
                )
            }
        )
    }

    private suspend fun <T> executeCall(call: suspend () -> Response<ApiResponse<T>>): RepoResult<T> {
        return try {
            val response = call()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                if (body.success && body.data != null) {
                    RepoResult.Success(
                        data = body.data,
                        message = body.message,
                        smtpDispatched = body.smtpDispatched,
                        smtpReceipt = body.smtpReceipt
                    )
                } else {
                    RepoResult.Error(body.message.ifBlank { "Request could not be completed." })
                }
            } else {
                val errorText = response.errorBody()?.string().orEmpty()
                val parsedMessage = parseErrorMessage(errorText)
                val isHtmlOr404 = response.code() == 404 || errorText.contains("<html", ignoreCase = true)
                val msg = when {
                    isHtmlOr404 -> "PHP API endpoint not found (HTTP ${response.code()}). Deploy the included backend_php/ scripts to your server."
                    parsedMessage != null -> parsedMessage
                    else -> "Server returned HTTP ${response.code()} (${response.message()})."
                }
                RepoResult.Error(
                    message = msg,
                    isBackendMissing = isHtmlOr404,
                    isUnauthorized = response.code() == 401
                )
            }
        } catch (e: JsonDataException) {
            RepoResult.Error(
                message = "Unexpected response from server (expected PHP JSON API). Ensure backend_php/ is deployed cleanly.",
                isBackendMissing = true
            )
        } catch (e: IOException) {
            RepoResult.Error(
                message = "Network error reaching server: ${e.localizedMessage ?: "Check internet connection or API URL."}",
                isBackendMissing = false
            )
        } catch (e: Exception) {
            RepoResult.Error(
                message = e.localizedMessage ?: "Unexpected error communicating with college server."
            )
        }
    }

    private fun parseErrorMessage(raw: String): String? {
        if (raw.isBlank() || raw.trimStart().startsWith("<")) return null
        return try {
            val type = Types.newParameterizedType(ApiResponse::class.java, Any::class.java)
            val adapter = NetworkClient.moshi.adapter<ApiResponse<Any>>(type)
            adapter.fromJson(raw)?.message?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}

private fun AttendanceRecordDto.toEntity() = AttendanceEntity(
    id = id,
    staffId = staffId,
    loginId = loginId,
    fullName = fullName,
    department = department,
    gmailId = gmailId,
    attendanceDate = attendanceDate,
    checkInTime = checkInTime,
    checkOutTime = checkOutTime,
    status = status,
    remarks = remarks,
    checkInSource = checkInSource
)

private fun LeaveRequestDto.toEntity(isApprovalQueue: Boolean) = LeaveRequestEntity(
    id = id,
    staffId = staffId,
    loginId = loginId,
    fullName = fullName,
    gmailId = gmailId,
    department = department,
    leaveType = leaveType,
    startDate = startDate,
    endDate = endDate,
    reason = reason,
    status = status,
    reviewNote = reviewNote,
    createdAt = createdAt,
    isApprovalQueue = isApprovalQueue
)

private fun LeaveRequestEntity.toDto() = LeaveRequestDto(
    id = id,
    staffId = staffId,
    loginId = loginId,
    fullName = fullName,
    gmailId = gmailId,
    department = department,
    leaveType = leaveType,
    startDate = startDate,
    endDate = endDate,
    reason = reason,
    status = status,
    reviewNote = reviewNote,
    createdAt = createdAt
)
