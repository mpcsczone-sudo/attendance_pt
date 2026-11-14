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

    suspend fun clearAll() {
        clearAttendance()
        clearLeaveRequests()
        clearSmtpLogs()
        clearStaffDirectory()
    }
}

@Database(
    entities = [
        AttendanceEntity::class,
        LeaveRequestEntity::class,
        SmtpLogEntity::class,
        StaffDirectoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
}
