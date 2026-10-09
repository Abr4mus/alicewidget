package com.alice.homewidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.alice.homewidget.R
import com.alice.homewidget.data.WidgetDataManager
import com.alice.homewidget.ui.MainActivity
import java.util.Locale

class AliceCompactWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val dataManager = WidgetDataManager(context)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, dataManager)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            dataManager: WidgetDataManager
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_alice_compact_2x2)
                val aggregated = dataManager.getAggregatedSensorData()

                val title = if (aggregated.primaryRoomName.isNotBlank() && aggregated.primaryRoomName != "Дом") {
                    aggregated.primaryRoomName
                } else {
                    "Климат"
                }
                views.setTextViewText(R.id.tvCompactRoomName, title)

                val tempStr = aggregated.primaryTemperature?.let { String.format(Locale.US, "%.1f°", it) } ?: "--°"
                views.setTextViewText(R.id.tvCompactTemp, tempStr)

                if (dataManager.showHumidity && aggregated.primaryHumidity != null) {
                    views.setViewVisibility(R.id.layoutCompactHumidity, View.VISIBLE)
                    views.setTextViewText(R.id.tvCompactHumidity, String.format(Locale.US, "%.0f%%", aggregated.primaryHumidity))
                } else {
                    views.setViewVisibility(R.id.layoutCompactHumidity, View.GONE)
                }

                // Click to open main app
                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.rootLayoutCompact, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
