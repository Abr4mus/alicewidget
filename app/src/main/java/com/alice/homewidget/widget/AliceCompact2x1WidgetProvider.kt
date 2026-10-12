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

class AliceCompact2x1WidgetProvider : AppWidgetProvider() {

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
                val views = RemoteViews(context.packageName, R.layout.widget_alice_compact_2x1)
                val skin = dataManager.widgetTheme
                val data = dataManager.get2x1Data()

                // Apply dynamic skin
                views.setInt(R.id.rootLayoutCompact2x1, "setBackgroundResource", WidgetSkinHelper.getWidgetBackground(skin))
                views.setInt(R.id.layoutCompact2x1Humidity, "setBackgroundResource", WidgetSkinHelper.getTileBackground(skin))
                views.setInt(R.id.btnCompact2x1Refresh, "setBackgroundResource", WidgetSkinHelper.getBtnBackground(skin))

                views.setTextColor(R.id.tvCompact2x1RoomName, WidgetSkinHelper.getTextColorSecondary(skin))
                views.setTextColor(R.id.tvCompact2x1Temp, WidgetSkinHelper.getTextColorPrimary(skin))
                views.setTextColor(R.id.tvCompact2x1Humidity, WidgetSkinHelper.getAccentHumidityColor(skin))

                // Set room & temperature
                views.setTextViewText(R.id.tvCompact2x1RoomName, data.title)
                views.setTextViewText(R.id.tvCompact2x1Temp, data.tempText)

                // Humidity metric pill
                if (dataManager.showHumidity && data.humidityText != null) {
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
