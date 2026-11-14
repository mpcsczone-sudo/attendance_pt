package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Query("SELECT * FROM cached_attendance ORDER BY attendanceDate DESC, id DESC")
    fun observeAttendance(): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendance(items: List<AttendanceEntity>)

    @Query("DELETE FROM cached_attendance")
    suspend fun clearAttendance()

    @Query("SELECT * FROM cached_monthly_summary WHERE month = :month LIMIT 1")
    fun observeMonthlySummary(month: String): Flow<MonthlySummaryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMonthlySummary(summary: MonthlySummaryEntity)

    @Query("DELETE FROM cached_monthly_summary")
    suspend fun clearMonthlySummary()

    @Query("SELECT * FROM cached_leave_requests WHERE isApprovalQueue = :isQueue ORDER BY id DESC")
    fun observeLeaveRequests(isQueue: Boolean): Flow<List<LeaveRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLeaveRequests(items: List<LeaveRequestEntity>)

    @Query("DELETE FROM cached_leave_requests")
    suspend fun clearLeaveRequests()

    @Query("SELECT * FROM cached_smtp_logs ORDER BY id DESC")
    fun observeSmtpLogs(): Flow<List<SmtpLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSmtpLogs(items: List<SmtpLogEntity>)

    @Query("DELETE FROM cached_smtp_logs")
    suspend fun clearSmtpLogs()

    @Query("SELECT * FROM cached_staff_directory ORDER BY loginId ASC")
    fun observeStaffDirectory(): Flow<List<StaffDirectoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStaffDirectory(items: List<StaffDirectoryEntity>)

    @Query("DELETE FROM cached_staff_directory")
    suspend fun clearStaffDirectory()

    @Query("SELECT * FROM cached_admin_messages ORDER BY id DESC")
    fun observeAdminMessages(): Flow<List<AdminMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAdminMessages(items: List<AdminMessageEntity>)

    @Query("DELETE FROM cached_admin_messages")
    suspend fun clearAdminMessages()

    @Query("SELECT * FROM cached_admin_replies ORDER BY id ASC")
    fun observeAdminReplies(): Flow<List<AdminReplyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAdminReplies(items: List<AdminReplyEntity>)

    @Query("DELETE FROM cached_admin_replies")
    suspend fun clearAdminReplies()

    suspend fun clearAll() {
        clearAttendance()
        clearMonthlySummary()
        clearLeaveRequests()
        clearSmtpLogs()
        clearStaffDirectory()
        clearAdminMessages()
        clearAdminReplies()
    }
}

@Database(
    entities = [
        AttendanceEntity::class,
        MonthlySummaryEntity::class,
        LeaveRequestEntity::class,
        SmtpLogEntity::class,
        StaffDirectoryEntity::class,
        AdminMessageEntity::class,
        AdminReplyEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
}
