package com.vishruu.vsfileexplorer

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.RemoteViews
import java.io.File
import java.util.Locale

class StorageWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.storage_widget)

            try {
                val root = Environment.getExternalStorageDirectory()
                val total = root.totalSpace
                val free = root.freeSpace
                val used = total - free
                val percent = if (total > 0) ((used * 100) / total).toInt() else 0

                views.setTextViewText(R.id.widget_percent, "$percent%")
                views.setTextViewText(
                    R.id.widget_details,
                    "Used: ${formatSize(used)} / ${formatSize(total)}"
                )
                views.setProgressBar(R.id.widget_progress, 100, percent, false)
            } catch (e: Exception) {
                views.setTextViewText(R.id.widget_details, "Unable to read storage")
            }

            // Tap opens app
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_details, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_percent, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun formatSize(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
            val mb = kb / 1024.0
            if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
            val gb = mb / 1024.0
            return String.format(Locale.US, "%.1f GB", gb)
        }
    }
}