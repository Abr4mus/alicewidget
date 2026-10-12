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
import com.alice.homewidget.ui.WidgetConfigureActivity

class AliceCompact2x1WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val dataManager = WidgetDataManager(context)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, dataManager)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val dataManager = WidgetDataManager(context)
        for (appWidgetId in appWidgetIds) {
            dataManager.deleteWidgetConfig(appWidgetId)
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
                val views = RemoteViews(context.packageName, R.layout.widget_alice_compact_2x1)
                val config = dataManager.getWidgetConfig(appWidgetId, "2x1")
                val theme = config.theme
                val data = dataManager.get2x1Data(appWidgetId)

                // Apply engine skin backgrounds
                views.setInt(R.id.rootLayoutCompact2x1, "setBackgroundResource", WidgetRenderEngine.getThemeBackground(theme))
                views.setInt(R.id.layoutCompact2x1Humidity, "setBackgroundResource", WidgetRenderEngine.getTileBackground(theme))
                views.setInt(R.id.btnCompact2x1Refresh, "setBackgroundResource", WidgetRenderEngine.getButtonBackground(theme))
                views.setInt(R.id.btnCompact2x1Configure, "setBackgroundResource", WidgetRenderEngine.getButtonBackground(theme))

                views.setTextColor(R.id.tvCompact2x1RoomName, WidgetRenderEngine.getTextSecondaryColor(theme))
                views.setTextColor(R.id.tvCompact2x1Temp, WidgetRenderEngine.getTextPrimaryColor(theme))
                views.setTextColor(R.id.tvCompact2x1Humidity, WidgetRenderEngine.getHumidityColor(theme))

                // Set room & temperature
                views.setTextViewText(R.id.tvCompact2x1RoomName, data.title)
                views.setTextViewText(R.id.tvCompact2x1Temp, data.tempText)

                // Humidity metric pill
                if (data.humidityText != null) {
                    views.setViewVisibility(R.id.layoutCompact2x1Humidity, View.VISIBLE)
                    views.setTextViewText(R.id.tvCompact2x1Humidity, data.humidityText)
                } else {
                    views.setViewVisibility(R.id.layoutCompact2x1Humidity, View.GONE)
                }

                // Click to open main app
                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.rootLayoutCompact2x1, pendingIntent)

                // Click to configure THIS widget instance
                val configIntent = Intent(context, WidgetConfigureActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val configPendingIntent = PendingIntent.getActivity(
                    context, appWidgetId, configIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btnCompact2x1Configure, configPendingIntent)

                // Click refresh
                val refreshIntent = Intent(context, AliceHomeWidgetProvider::class.java).apply {
                    action = AliceHomeWidgetProvider.ACTION_REFRESH_WIDGET
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context, appWidgetId, refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btnCompact2x1Refresh, refreshPendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
