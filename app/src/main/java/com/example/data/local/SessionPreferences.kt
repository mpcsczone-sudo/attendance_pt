package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AuthSession
import com.example.data.model.StaffProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pttp_staff_prefs")

data class StoredSession(
    val token: String = "",
    val staff: StaffProfile? = null,
    val baseUrl: String = SessionPreferences.DEFAULT_BASE_URL,
    val lastCheckedInDate: String = "",
    val reminderEnabled: Boolean = true
) {
    val isLoggedIn: Boolean get() = token.isNotBlank() && staff != null
}

class SessionPreferences(private val context: Context) {

    val sessionFlow: Flow<StoredSession> = context.dataStore.data.map { prefs ->
        val token = prefs[KEY_TOKEN].orEmpty()
        val staffId = prefs[KEY_STAFF_ID] ?: 0
        val staff = if (token.isNotBlank() && staffId > 0) {
            StaffProfile(
                id = staffId,
                loginId = prefs[KEY_LOGIN_ID].orEmpty(),
                gmailId = prefs[KEY_GMAIL_ID].orEmpty(),
                gmailVerified = prefs[KEY_GMAIL_VERIFIED] ?: false,
                fullName = prefs[KEY_FULL_NAME].orEmpty(),
                department = prefs[KEY_DEPARTMENT].orEmpty(),
                designation = prefs[KEY_DESIGNATION].orEmpty(),
                role = prefs[KEY_ROLE].orEmpty().ifBlank { "STAFF" },
                approvalStatus = prefs[KEY_APPROVAL_STATUS].orEmpty().ifBlank { "APPROVED" },
                shiftStartTime = prefs[KEY_SHIFT_START].orEmpty().ifBlank { "09:30" },
                shiftEndTime = prefs[KEY_SHIFT_END].orEmpty().ifBlank { "16:30" }
            )
        } else null

        StoredSession(
            token = token,
            staff = staff,
            baseUrl = normalizeBaseUrl(prefs[KEY_BASE_URL].orEmpty().ifBlank { DEFAULT_BASE_URL }),
            lastCheckedInDate = prefs[KEY_LAST_CHECKIN_DATE].orEmpty(),
            reminderEnabled = prefs[KEY_REMINDER_ENABLED] ?: true
        )
    }

    suspend fun getSessionSnapshot(): StoredSession = sessionFlow.first()

    suspend fun getBaseUrl(): String {
        val raw = context.dataStore.data.first()[KEY_BASE_URL].orEmpty().ifBlank { DEFAULT_BASE_URL }
        return normalizeBaseUrl(raw)
    }

    suspend fun getToken(): String {
        return context.dataStore.data.first()[KEY_TOKEN].orEmpty()
    }

    suspend fun saveAuthSession(session: AuthSession) {
        saveStaffProfile(session.staff, session.token)
    }

    suspend fun saveStaffProfile(staff: StaffProfile, tokenOverride: String? = null) {
        context.dataStore.edit { prefs ->
            if (tokenOverride != null) {
                prefs[KEY_TOKEN] = tokenOverride
            }
            prefs[KEY_STAFF_ID] = staff.id
            prefs[KEY_LOGIN_ID] = staff.loginId
            prefs[KEY_GMAIL_ID] = staff.gmailId
            prefs[KEY_GMAIL_VERIFIED] = staff.gmailVerified
            prefs[KEY_FULL_NAME] = staff.fullName
            prefs[KEY_DEPARTMENT] = staff.department
            prefs[KEY_DESIGNATION] = staff.designation
            prefs[KEY_ROLE] = staff.role
            prefs[KEY_APPROVAL_STATUS] = staff.approvalStatus
            prefs[KEY_SHIFT_START] = staff.shiftStartTime.ifBlank { "09:30" }
            prefs[KEY_SHIFT_END] = staff.shiftEndTime.ifBlank { "16:30" }
        }
    }

    suspend fun updateMappedGmail(gmailId: String, verified: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GMAIL_ID] = gmailId
            prefs[KEY_GMAIL_VERIFIED] = verified
        }
    }

    suspend fun markCheckedInDate(dateStr: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_CHECKIN_DATE] = dateStr
        }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REMINDER_ENABLED] = enabled
        }
    }

    suspend fun saveBaseUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BASE_URL] = normalizeBaseUrl(url)
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            val currentUrl = prefs[KEY_BASE_URL]
            prefs.clear()
            if (!currentUrl.isNullOrBlank()) {
                prefs[KEY_BASE_URL] = currentUrl
            }
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://pttpshuklacollege.com/api/"

        private val KEY_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_STAFF_ID = intPreferencesKey("staff_id")
        private val KEY_LOGIN_ID = stringPreferencesKey("login_id")
        private val KEY_GMAIL_ID = stringPreferencesKey("gmail_id")
        private val KEY_GMAIL_VERIFIED = booleanPreferencesKey("gmail_verified")
        private val KEY_FULL_NAME = stringPreferencesKey("full_name")
        private val KEY_DEPARTMENT = stringPreferencesKey("department")
        private val KEY_DESIGNATION = stringPreferencesKey("designation")
        private val KEY_ROLE = stringPreferencesKey("role")
        private val KEY_APPROVAL_STATUS = stringPreferencesKey("approval_status")
        private val KEY_SHIFT_START = stringPreferencesKey("shift_start_time")
        private val KEY_SHIFT_END = stringPreferencesKey("shift_end_time")
        private val KEY_LAST_CHECKIN_DATE = stringPreferencesKey("last_checkin_date")
        private val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        private val KEY_BASE_URL = stringPreferencesKey("api_base_url")

        fun normalizeBaseUrl(input: String): String {
            var cleaned = input.trim()
            if (cleaned.isBlank()) return DEFAULT_BASE_URL
            if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
                cleaned = "https://$cleaned"
            }
            if (!cleaned.endsWith("/")) {
                cleaned = "$cleaned/"
            }
            return cleaned
        }
    }
}
