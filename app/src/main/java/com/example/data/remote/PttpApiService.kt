package com.example.data.remote

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
import com.example.data.model.LoginRequest
import com.example.data.model.MapGmailRequest
import com.example.data.model.OtpDispatchData
import com.example.data.model.RegisterStaffRequest
import com.example.data.model.SmtpLogDto
import com.example.data.model.SmtpTestRequest
import com.example.data.model.StaffProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface PttpApiService {

    @GET("health.php")
    suspend fun checkHealth(): Response<ApiResponse<HealthStatus>>

    @POST("auth_login.php")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthSession>>

    @POST("auth_register.php")
    suspend fun registerStaff(@Body request: RegisterStaffRequest): Response<ApiResponse<StaffProfile>>

    @POST("auth_gmail_otp.php")
    suspend fun requestGmailOtp(@Body request: GmailOtpActionRequest): Response<ApiResponse<OtpDispatchData>>

    @POST("auth_gmail_otp.php")
    suspend fun verifyGmailOtp(@Body request: GmailOtpActionRequest): Response<ApiResponse<AuthSession>>

    @POST("map_gmail.php")
    suspend fun mapGmail(@Body request: MapGmailRequest): Response<ApiResponse<StaffProfile>>

    @POST("map_gmail.php")
    suspend fun initiateGmailLink(@Body request: MapGmailRequest): Response<ApiResponse<OtpDispatchData>>

    @GET("admin_staff_approval.php")
    suspend fun getPendingStaffApprovals(): Response<ApiResponse<List<StaffProfile>>>

    @POST("admin_staff_approval.php")
    suspend fun reviewStaffRegistration(@Body request: AdminStaffApprovalRequest): Response<ApiResponse<List<StaffProfile>>>

    @POST("attendance_mark.php")
    suspend fun markAttendance(@Body request: AttendanceMarkRequest): Response<ApiResponse<AttendanceRecordDto>>

    @GET("attendance_list.php")
    suspend fun getAttendance(
        @Query("month") month: String? = null
    ): Response<ApiResponse<AttendanceListData>>

    @GET("leave_manage.php")
    suspend fun getLeaveRequests(): Response<ApiResponse<LeaveListData>>

    @POST("leave_manage.php")
    suspend fun submitOrReviewLeave(@Body request: LeaveActionRequest): Response<ApiResponse<LeaveListData>>

    @GET("staff_directory.php")
    suspend fun getStaffDirectory(): Response<ApiResponse<List<StaffProfile>>>

    @GET("smtp_logs.php")
    suspend fun getSmtpLogs(): Response<ApiResponse<List<SmtpLogDto>>>

    @POST("smtp_logs.php")
    suspend fun sendTestSmtp(@Body request: SmtpTestRequest): Response<ApiResponse<List<SmtpLogDto>>>
}
