package com.alice.homewidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.alice.homewidget.R
import com.alice.homewidget.data.WidgetDataManager
import com.alice.homewidget.ui.MainActivity
import java.util.Locale

class AliceBarWidgetProvider : AppWidgetProvider() {

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
            val views = RemoteViews(context.packageName, R.layout.widget_alice_bar_4x1)
            val aggregated = dataManager.getAggregatedSensorData()

            views.setTextViewText(R.id.tvBarHousehold, aggregated.householdName)

            val tempStr = aggregated.primaryTemperature?.let { String.format(Locale.US, "%.1f°C", it) } ?: "--°"
            views.setTextViewText(R.id.tvBarTemp, tempStr)

            val humStr = aggregated.primaryHumidity?.let { String.format(Locale.US, "%.0f%%", it) } ?: "--%"
            views.setTextViewText(R.id.tvBarHumidity, humStr)

            val doorStr = if (aggregated.isAnyDoorOpen) "🚪 ОТКРЫТО" else "🚪 ОК"
            views.setTextViewText(R.id.tvBarDoor, doorStr)

            // Click to open main app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.rootLayoutBar, pendingIntent)

            // Click refresh
            val refreshIntent = Intent(context, AliceHomeWidgetProvider::class.java).apply {
                action = AliceHomeWidgetProvider.ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnBarRefresh, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
