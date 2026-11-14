package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AttendanceRecordDto
import com.example.data.model.AttendanceSummaryDto
import com.example.data.model.StaffProfile
import com.example.ui.components.computeAverageCheckIn
import com.example.ui.components.computeLongestPresentStreak
import com.example.ui.components.computeOnTimeRate
import com.example.ui.components.computeRequiredPresentDaysForTarget
import com.example.ui.components.computeTotalDutyHoursLabel
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.PttpStaffTheme
import com.example.ui.viewmodel.DashboardUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PTTP Staff", appName)
  }

  @Test
  fun `dynamic attendance helpers compute metrics and benchmark accurately`() {
    val records = listOf(
      AttendanceRecordDto(
        id = 1,
        staffId = 2,
        attendanceDate = "2026-11-02",
        checkInTime = "09:20:00",
        checkOutTime = "16:20:00",
        status = "PRESENT"
      ),
      AttendanceRecordDto(
        id = 2,
        staffId = 2,
        attendanceDate = "2026-11-03",
        checkInTime = "09:40:00",
        checkOutTime = "16:10:00",
        status = "LATE"
      )
    )
    assertEquals("09:30", computeAverageCheckIn(records))
    assertEquals(50, computeOnTimeRate(records))
    assertEquals("13h 30m", computeTotalDutyHoursLabel(records))
    assertEquals(3, computeLongestPresentStreak(listOf("P", "P", "A", "P", "P", "P", "-")))
    assertEquals(2, computeRequiredPresentDaysForTarget(present = 1, absent = 1, targetPercent = 75))
    assertEquals(0, computeRequiredPresentDaysForTarget(present = 3, absent = 1, targetPercent = 75))
  }

  @Test
  fun `admin selecting user displays month calendar modal details and month-days rate without log list`() {
    val admin = StaffProfile(
      id = 1,
      loginId = "PTTP-ADMIN-001",
      gmailId = "admin@gmail.com",
      gmailVerified = true,
      fullName = "Principal Admin",
      department = "Administration",
      designation = "Principal",
      role = "ADMIN"
    )
    val selectedStaff = StaffProfile(
      id = 2,
      loginId = "PTTP-FAC-012",
      gmailId = "faculty@gmail.com",
      gmailVerified = true,
      fullName = "Dr. Ananya Verma",
      department = "Computer Science",
      designation = "Assistant Professor",
      role = "STAFF"
    )
    val selectedRecords = listOf(
      AttendanceRecordDto(
        id = 11,
        staffId = 2,
        loginId = "PTTP-FAC-012",
        fullName = "Dr. Ananya Verma",
        attendanceDate = "2026-11-05",
        checkInTime = "09:22:00",
        checkOutTime = "16:25:00",
        geoAddress = "Pt. T. P. Shukla College Gate",
        faceVerified = true,
        status = "PRESENT"
      )
    )
    val uiState = DashboardUiState(
      staff = admin,
      selectedMonth = "2026-11",
      staffDirectory = listOf(admin, selectedStaff),
      selectedDashboardStaff = selectedStaff,
      attendanceHistory = selectedRecords,
      summary = AttendanceSummaryDto(
        month = "2026-11",
        totalDays = 1,
        presentDays = 1,
        attendancePercentage = 100.0
      )
    )

    composeRule.setContent {
      PttpStaffTheme {
        DashboardScreen(
          uiState = uiState,
          onChangeMonth = {},
          onJumpToCurrentMonth = {},
          onSelectDashboardStaff = {},
          onRefresh = {},
          onOpenAttendanceTab = {},
          onOpenLeavesTab = {},
          onOpenNotificationsTab = {},
          onOpenServerSetup = {},
          onDismissFeedback = {}
        )
      }
    }

    composeRule.onAllNodesWithText("Dr. Ananya Verma").onFirst().assertIsDisplayed()
    composeRule.onNodeWithTag("admin_dashboard_user_selector").assertIsDisplayed()
    composeRule.onNodeWithTag("dashboard_log_search_input").assertDoesNotExist()
    composeRule.onNodeWithTag("selected_date_detail_box").assertDoesNotExist()

    composeRule.onNodeWithTag("dashboard_lazy_list")
      .performScrollToNode(hasTestTag("dashboard_month_calendar_card"))
    composeRule.onNodeWithTag("dashboard_month_calendar_card").assertIsDisplayed()
    // 1 Present out of 30 calendar days in Nov 2026 -> 3.3%
    composeRule.onAllNodesWithText("3.3%").onFirst().assertIsDisplayed()

    composeRule.onNodeWithTag("calendar_day_2026-11-05").performScrollTo().assertIsDisplayed().performClick()
    composeRule.onNodeWithTag("date_detail_modal").assertIsDisplayed()
    composeRule.onNodeWithText("09:22:00").assertIsDisplayed()
    composeRule.onNodeWithText("16:25:00").assertIsDisplayed()
    composeRule.onNodeWithText("Pt. T. P. Shukla College Gate").assertIsDisplayed()
    composeRule.onNodeWithTag("date_detail_modal_close").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("date_detail_modal").assertDoesNotExist()

    composeRule.onNodeWithTag("dashboard_lazy_list")
      .performScrollToNode(hasTestTag("dashboard_advanced_analytics_card"))
    composeRule.onNodeWithTag("dashboard_donut_gauge").performScrollTo().assertIsDisplayed()
    composeRule.onAllNodesWithText("1 / 30 d").onFirst().assertIsDisplayed()
    composeRule.onNodeWithTag("dashboard_weekly_chart").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("dashboard_benchmark_card").performScrollTo().assertIsDisplayed()
  }
}
