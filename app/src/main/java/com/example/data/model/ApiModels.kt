package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val success: Boolean = false,
    val message: String = "",
    val data: T? = null,
    @Json(name = "smtp_dispatched") val smtpDispatched: Boolean = false,
    @Json(name = "smtp_receipt") val smtpReceipt: SmtpReceipt? = null
)

@JsonClass(generateAdapter = true)
data class SmtpReceipt(
    @Json(name = "recipient_gmail") val recipientGmail: String = "",
    @Json(name = "message_id") val messageId: String = "",
    val status: String = "",
    val subject: String = "",
    @Json(name = "sent_at") val sentAt: String = ""
)

@JsonClass(generateAdapter = true)
data class HealthStatus(
    val status: String = "unknown",
    @Json(name = "php_version") val phpVersion: String = "",
    @Json(name = "mysql_connected") val mysqlConnected: Boolean = false,
    @Json(name = "smtp_configured") val smtpConfigured: Boolean = false,
    @Json(name = "college_domain") val collegeDomain: String = "https://pttpshuklacollege.com/",
    @Json(name = "server_time") val serverTime: String = ""
)

@JsonClass(generateAdapter = true)
data class StaffProfile(
    val id: Int = 0,
    @Json(name = "login_id") val loginId: String = "",
    @Json(name = "gmail_id") val gmailId: String = "",
    @Json(name = "gmail_verified") val gmailVerified: Boolean = false,
    @Json(name = "full_name") val fullName: String = "",
    val department: String = "",
    val designation: String = "",
    val role: String = "STAFF",
    @Json(name = "approval_status") val approvalStatus: String = "APPROVED",
    @Json(name = "shift_start_time") val shiftStartTime: String = "09:30",
    @Json(name = "shift_end_time") val shiftEndTime: String = "16:30"
)

@JsonClass(generateAdapter = true)
data class AdminStaffApprovalRequest(
    val action: String, // "approve" | "reject"
    @Json(name = "staff_id") val staffId: Int,
    val note: String = ""
)

@JsonClass(generateAdapter = true)
data class StaffTimeMappingRequest(
    @Json(name = "login_id") val loginId: String,
    @Json(name = "shift_start_time") val shiftStartTime: String,
    @Json(name = "shift_end_time") val shiftEndTime: String
)

@JsonClass(generateAdapter = true)
data class AuthSession(
    val token: String = "",
    @Json(name = "expires_at") val expiresAt: String = "",
    val staff: StaffProfile = StaffProfile()
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val identifier: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterStaffRequest(
    @Json(name = "login_id") val loginId: String,
    @Json(name = "gmail_id") val gmailId: String,
    @Json(name = "full_name") val fullName: String,
    val department: String,
    val designation: String,
    val password: String,
    val role: String = "STAFF"
)

@JsonClass(generateAdapter = true)
data class GmailOtpActionRequest(
    val action: String, // "request_otp" or "verify_otp"
    val identifier: String,
    val otp: String? = null,
    val purpose: String = "LOGIN"
)

@JsonClass(generateAdapter = true)
data class OtpDispatchData(
    @Json(name = "masked_gmail") val maskedGmail: String = "",
    @Json(name = "login_id") val loginId: String = "",
    @Json(name = "expires_in_seconds") val expiresInSeconds: Int = 600
)

@JsonClass(generateAdapter = true)
data class MapGmailRequest(
    val action: String, // "initiate_link" | "confirm_link" | "admin_map"
    @Json(name = "login_id") val loginId: String,
    @Json(name = "gmail_id") val gmailId: String,
    val otp: String? = null
)

@JsonClass(generateAdapter = true)
data class AttendanceMarkRequest(
    val action: String, // "CHECK_IN", "CHECK_OUT", "ON_DUTY"
    val remarks: String = "",
    @Json(name = "device_info") val deviceInfo: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    @Json(name = "geo_accuracy_m") val geoAccuracyM: Float? = null,
    @Json(name = "geo_address") val geoAddress: String? = null,
    @Json(name = "face_verified") val faceVerified: Boolean = false,
    @Json(name = "face_confidence") val faceConfidence: Float? = null
)

@JsonClass(generateAdapter = true)
data class AttendanceRecordDto(
    val id: Int = 0,
    @Json(name = "staff_id") val staffId: Int = 0,
    @Json(name = "login_id") val loginId: String = "",
    @Json(name = "full_name") val fullName: String = "",
    val department: String = "",
    @Json(name = "gmail_id") val gmailId: String = "",
    @Json(name = "attendance_date") val attendanceDate: String = "",
    @Json(name = "check_in_time") val checkInTime: String? = null,
    @Json(name = "check_out_time") val checkOutTime: String? = null,
    val status: String = "PRESENT",
    val latitude: Double? = null,
    val longitude: Double? = null,
    @Json(name = "geo_address") val geoAddress: String? = null,
    @Json(name = "face_verified") val faceVerified: Boolean = false,
    val remarks: String = "",
    @Json(name = "check_in_source") val checkInSource: String = "FACE_GEO_APP"
)

@JsonClass(generateAdapter = true)
data class AttendanceSummaryDto(
    val month: String = "",
    @Json(name = "total_days") val totalDays: Int = 0,
    @Json(name = "present_days") val presentDays: Int = 0,
    @Json(name = "late_days") val lateDays: Int = 0,
    @Json(name = "on_duty_days") val onDutyDays: Int = 0,
    @Json(name = "half_days") val halfDays: Int = 0,
    @Json(name = "absent_days") val absentDays: Int = 0,
    @Json(name = "attendance_percentage") val attendancePercentage: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class AttendanceListData(
    val today: AttendanceRecordDto? = null,
    val staff: StaffProfile? = null,
    @Json(name = "target_staff") val targetStaff: StaffProfile? = null,
    val summary: AttendanceSummaryDto = AttendanceSummaryDto(),
    val records: List<AttendanceRecordDto> = emptyList(),
    @Json(name = "department_register") val departmentRegister: List<AttendanceRecordDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class LeaveActionRequest(
    val action: String, // "submit" | "approve" | "reject"
    @Json(name = "leave_type") val leaveType: String? = null,
    @Json(name = "start_date") val startDate: String? = null,
    @Json(name = "end_date") val endDate: String? = null,
    val reason: String? = null,
    @Json(name = "request_id") val requestId: Int? = null,
    @Json(name = "review_note") val reviewNote: String? = null
)

@JsonClass(generateAdapter = true)
data class LeaveRequestDto(
    val id: Int = 0,
    @Json(name = "staff_id") val staffId: Int = 0,
    @Json(name = "login_id") val loginId: String = "",
    @Json(name = "full_name") val fullName: String = "",
    @Json(name = "gmail_id") val gmailId: String = "",
    val department: String = "",
    @Json(name = "leave_type") val leaveType: String = "",
    @Json(name = "start_date") val startDate: String = "",
    @Json(name = "end_date") val endDate: String = "",
    val reason: String = "",
    val status: String = "PENDING",
    @Json(name = "review_note") val reviewNote: String? = null,
    @Json(name = "created_at") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class LeaveListData(
    @Json(name = "my_requests") val myRequests: List<LeaveRequestDto> = emptyList(),
    @Json(name = "pending_approvals") val pendingApprovals: List<LeaveRequestDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SmtpLogDto(
    val id: Int = 0,
    @Json(name = "staff_id") val staffId: Int = 0,
    @Json(name = "recipient_gmail") val recipientGmail: String = "",
    @Json(name = "event_type") val eventType: String = "",
    val subject: String = "",
    @Json(name = "body_preview") val bodyPreview: String = "",
    @Json(name = "smtp_message_id") val smtpMessageId: String = "",
    @Json(name = "delivery_status") val deliveryStatus: String = "",
    @Json(name = "error_message") val errorMessage: String? = null,
    @Json(name = "sent_at") val sentAt: String = ""
)

@JsonClass(generateAdapter = true)
data class SmtpTestRequest(
    val action: String = "send_test"
)

@JsonClass(generateAdapter = true)
data class AdminMessageReplyDto(
    val id: Int = 0,
    @Json(name = "parent_id") val parentId: Int = 0,
    @Json(name = "sender_staff_id") val senderStaffId: Int = 0,
    @Json(name = "sender_login_id") val senderLoginId: String = "",
    @Json(name = "sender_name") val senderName: String = "",
    @Json(name = "sender_role") val senderRole: String = "STAFF",
    @Json(name = "message_body") val messageBody: String = "",
    @Json(name = "created_at") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class AdminMessageThreadDto(
    val id: Int = 0,
    @Json(name = "sender_staff_id") val senderStaffId: Int = 0,
    @Json(name = "sender_name") val senderName: String = "",
    @Json(name = "sender_login_id") val senderLoginId: String = "",
    @Json(name = "target_staff_id") val targetStaffId: Int? = null,
    @Json(name = "target_login_id") val targetLoginId: String? = null,
    @Json(name = "target_name") val targetName: String? = null,
    val title: String = "",
    @Json(name = "message_body") val messageBody: String = "",
    @Json(name = "created_at") val createdAt: String = "",
    val replies: List<AdminMessageReplyDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SendAdminMessageRequest(
    val action: String = "send_admin_message",
    @Json(name = "target_login_id") val targetLoginId: String? = null,
    val title: String,
    @Json(name = "message_body") val messageBody: String
)

@JsonClass(generateAdapter = true)
data class ReplyAdminMessageRequest(
    val action: String = "reply",
    @Json(name = "parent_id") val parentId: Int,
    @Json(name = "message_body") val messageBody: String
)
