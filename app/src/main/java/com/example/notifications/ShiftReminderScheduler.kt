package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ShiftReminderScheduler {

    const val CHANNEL_ID = "shift_reminder_channel"
    private const val REQUEST_CODE = 4410

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Shift Check-In Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts staff 10 minutes before mapped shift start if attendance is not yet marked."
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * Schedules an alarm 10 minutes before [shiftStartTime] (HH:mm).
     * If today's check-in is already marked or today's trigger has already passed, schedules for tomorrow.
     */
    fun scheduleReminder(
        context: Context,
        staffName: String,
        loginId: String,
        shiftStartTime: String,
        alreadyCheckedInToday: Boolean,
        enabled: Boolean = true
    ) {
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ShiftReminderReceiver::class.java).apply {
            putExtra(ShiftReminderReceiver.EXTRA_STAFF_NAME, staffName)
            putExtra(ShiftReminderReceiver.EXTRA_LOGIN_ID, loginId)
            putExtra(ShiftReminderReceiver.EXTRA_SHIFT_START, shiftStartTime)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val triggerMillis = computeNextTriggerMillis(
            shiftStartTime = shiftStartTime,
            forceTomorrow = alreadyCheckedInToday
        )

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            pendingIntent
        )
    }

    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ShiftReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    fun computeNextTriggerFormatted(shiftStartTime: String, alreadyCheckedInToday: Boolean): String {
        val millis = computeNextTriggerMillis(shiftStartTime, alreadyCheckedInToday)
        return SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(millis))
    }

    fun computeNextTriggerMillis(shiftStartTime: String, forceTomorrow: Boolean): Long {
        val parts = shiftStartTime.trim().split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 30

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -10) // 10 minutes before mapped shift start
        }

        if (forceTomorrow || target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }
}
