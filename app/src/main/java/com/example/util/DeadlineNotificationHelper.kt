package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.entity.ProjectEntity
import com.example.data.model.ProjectStatus
import java.time.LocalDate

object DeadlineNotificationHelper {
    private const val CHANNEL_ID = "rohan_edits_deadlines"
    private const val CHANNEL_NAME = "Project Deadlines"
    private const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders for upcoming and overdue editing project deadlines"
        }
        manager.createNotificationChannel(channel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun notifyUpcomingDeadlines(
        context: Context,
        projects: List<ProjectEntity>,
        today: LocalDate
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannel(context)

        val todayEpoch = today.toEpochDay()
        val soonEpoch = today.plusDays(2).toEpochDay()

        val activeDue = projects.filter { project ->
            val status = ProjectStatus.fromName(project.projectStatus)
            val deadline = project.deadlineEpochDay
            status != ProjectStatus.COMPLETED &&
                status != ProjectStatus.CANCELLED &&
                deadline != null &&
                deadline <= soonEpoch
        }

        if (activeDue.isEmpty()) return

        val overdueCount = activeDue.count { (it.deadlineEpochDay ?: Long.MAX_VALUE) < todayEpoch }
        val upcomingCount = activeDue.size - overdueCount
        val title = when {
            overdueCount > 0 && upcomingCount > 0 -> "$overdueCount overdue, $upcomingCount due soon"
            overdueCount > 0 -> "$overdueCount project deadline(s) overdue"
            else -> "$upcomingCount project deadline(s) due soon"
        }
        val previewTitles = activeDue.take(3).joinToString(", ") { it.title }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(previewTitles)
            .setStyle(NotificationCompat.BigTextStyle().bigText(previewTitles))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
