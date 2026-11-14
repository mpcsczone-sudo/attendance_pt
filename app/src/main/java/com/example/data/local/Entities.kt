package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_attendance")
data class AttendanceEntity(
    @PrimaryKey val id: Int,
    val staffId: Int,
    val loginId: String,
    val fullName: String,
    val department: String,
    val gmailId: String,
    val attendanceDate: String,
    val checkInTime: String?,
    val checkOutTime: String?,
    val status: String,
    val latitude: Double?,
    val longitude: Double?,
    val geoAddress: String?,
    val faceVerified: Boolean,
    val remarks: String,
    val checkInSource: String
)

@Entity(tableName = "cached_monthly_summary")
data class MonthlySummaryEntity(
    @PrimaryKey val month: String,
    val totalDays: Int,
    val presentDays: Int,
    val lateDays: Int,
    val onDutyDays: Int,
    val halfDays: Int,
    val absentDays: Int,
    val attendancePercentage: Double,
    val syncedAtMillis: Long
)

@Entity(tableName = "cached_leave_requests")
data class LeaveRequestEntity(
    @PrimaryKey val id: Int,
    val staffId: Int,
    val loginId: String,
    val fullName: String,
    val gmailId: String,
    val department: String,
    val leaveType: String,
    val startDate: String,
    val endDate: String,
    val reason: String,
    val status: String,
    val reviewNote: String?,
    val createdAt: String,
    val isApprovalQueue: Boolean
)

@Entity(tableName = "cached_smtp_logs")
data class SmtpLogEntity(
    @PrimaryKey val id: Int,
    val staffId: Int,
    val recipientGmail: String,
    val eventType: String,
    val subject: String,
    val bodyPreview: String,
    val smtpMessageId: String,
    val deliveryStatus: String,
    val errorMessage: String?,
    val sentAt: String
)

@Entity(tableName = "cached_staff_directory")
data class StaffDirectoryEntity(
    @PrimaryKey val id: Int,
    val loginId: String,
    val gmailId: String,
    val gmailVerified: Boolean,
    val fullName: String,
    val department: String,
    val designation: String,
    val role: String,
    val shiftStartTime: String,
    val shiftEndTime: String
)

@Entity(tableName = "cached_admin_messages")
data class AdminMessageEntity(
    @PrimaryKey val id: Int,
    val senderStaffId: Int,
    val senderName: String,
    val senderLoginId: String,
    val targetStaffId: Int?,
    val targetLoginId: String?,
    val targetName: String?,
    val title: String,
    val messageBody: String,
    val createdAt: String
)

@Entity(tableName = "cached_admin_replies")
data class AdminReplyEntity(
    @PrimaryKey val id: Int,
    val parentId: Int,
    val senderStaffId: Int,
    val senderLoginId: String,
    val senderName: String,
    val senderRole: String,
    val messageBody: String,
    val createdAt: String
)
