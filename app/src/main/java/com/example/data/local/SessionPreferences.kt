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
    val baseUrl: String = SessionPreferences.DEFAULT_BASE_URL
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
                role = prefs[KEY_ROLE].orEmpty().ifBlank { "STAFF" }
            )
        } else null

        StoredSession(
            token = token,
            staff = staff,
            baseUrl = normalizeBaseUrl(prefs[KEY_BASE_URL].orEmpty().ifBlank { DEFAULT_BASE_URL })
        )
    }

    suspend fun getBaseUrl(): String {
        val raw = context.dataStore.data.first()[KEY_BASE_URL].orEmpty().ifBlank { DEFAULT_BASE_URL }
        return normalizeBaseUrl(raw)
    }

    suspend fun getToken(): String {
        return context.dataStore.data.first()[KEY_TOKEN].orEmpty()
    }

    suspend fun saveAuthSession(session: AuthSession) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = session.token
            prefs[KEY_STAFF_ID] = session.staff.id
            prefs[KEY_LOGIN_ID] = session.staff.loginId
            prefs[KEY_GMAIL_ID] = session.staff.gmailId
            prefs[KEY_GMAIL_VERIFIED] = session.staff.gmailVerified
            prefs[KEY_FULL_NAME] = session.staff.fullName
            prefs[KEY_DEPARTMENT] = session.staff.department
            prefs[KEY_DESIGNATION] = session.staff.designation
            prefs[KEY_ROLE] = session.staff.role
        }
    }

    suspend fun updateMappedGmail(gmailId: String, verified: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GMAIL_ID] = gmailId
            prefs[KEY_GMAIL_VERIFIED] = verified
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
