package com.example.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.SessionPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ShiftReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val prefs = SessionPreferences(context.applicationContext)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = prefs.getSessionSnapshot()
                val staff = snapshot.staff
                if (!snapshot.isLoggedIn || staff == null || !snapshot.reminderEnabled) {
                    return@launch
                }

                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val alreadyCheckedIn = snapshot.lastCheckedInDate == today

                if (!alreadyCheckedIn) {
                    val staffName = intent.getStringExtra(EXTRA_STAFF_NAME) ?: staff.fullName
                    val shiftStart = intent.getStringExtra(EXTRA_SHIFT_START) ?: staff.shiftStartTime
                    postNotification(context, staffName, staff.loginId, shiftStart)
                }

                // Schedule next day's reminder 10 minutes before mapped shift
                ShiftReminderScheduler.scheduleReminder(
                    context = context.applicationContext,
                    staffName = staff.fullName,
                    loginId = staff.loginId,
                    shiftStartTime = staff.shiftStartTime,
                    alreadyCheckedInToday = true,
                    enabled = snapshot.reminderEnabled
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postNotification(
        context: Context,
        staffName: String,
        loginId: String,
        shiftStart: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ShiftReminderScheduler.ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ShiftReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Check-In Reminder: 10 Min Left ($shiftStart)")
            .setContentText("Hi $staffName ($loginId), mark your face & geo-tag attendance before $shiftStart.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Hi $staffName ($loginId), your mapped shift begins at $shiftStart. " +
                        "You have not checked in yet—open PTTP Staff to complete Live Face & Geo-Tag Check-In."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val EXTRA_STAFF_NAME = "extra_staff_name"
        const val EXTRA_LOGIN_ID = "extra_login_id"
        const val EXTRA_SHIFT_START = "extra_shift_start"
        private const val NOTIFICATION_ID = 7701
    }
}
