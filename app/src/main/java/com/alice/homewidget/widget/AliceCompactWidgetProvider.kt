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

class AliceCompactWidgetProvider : AppWidgetProvider() {

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
                val views = RemoteViews(context.packageName, R.layout.widget_alice_compact_2x2)
                val config = dataManager.getWidgetConfig(appWidgetId, "2x2")
                val theme = config.theme
                val data = dataManager.get2x2Data(appWidgetId)

                // Apply engine skin backgrounds
                views.setInt(R.id.rootLayoutCompact, "setBackgroundResource", WidgetRenderEngine.getThemeBackground(theme))
                views.setInt(R.id.layoutCompactHumidity, "setBackgroundResource", WidgetRenderEngine.getTileBackground(theme))
                views.setInt(R.id.btnCompactConfigure, "setBackgroundResource", WidgetRenderEngine.getButtonBackground(theme))

                views.setTextColor(R.id.tvCompactRoomName, WidgetRenderEngine.getTextSecondaryColor(theme))
                views.setTextColor(R.id.tvCompactTemp, WidgetRenderEngine.getTextPrimaryColor(theme))
                views.setTextColor(R.id.tvCompactHumidity, WidgetRenderEngine.getHumidityColor(theme))

                views.setTextViewText(R.id.tvCompactRoomName, data.title)
                views.setTextViewText(R.id.tvCompactTemp, data.tempText)

                // Dynamic Comfort Gauge Ring
                try {
                    val rawTemp = data.tempText.replace("°", "").toDoubleOrNull()
                    val rawHum = data.humidityText?.replace("%", "")?.toDoubleOrNull()
                    val gaugeBitmap = WidgetRenderEngine.renderComfortGauge(rawTemp, rawHum, theme, sizePx = 130)
                    views.setImageViewBitmap(R.id.ivCompactComfortGauge, gaugeBitmap)
                    views.setViewVisibility(R.id.ivCompactComfortGauge, View.VISIBLE)
                } catch (e: Exception) {
                    views.setViewVisibility(R.id.ivCompactComfortGauge, View.GONE)
                }

                if (data.humidityText != null) {
                    views.setViewVisibility(R.id.layoutCompactHumidity, View.VISIBLE)
                    views.setTextViewText(R.id.tvCompactHumidity, data.humidityText)
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

                // Click to configure THIS widget instance
                val configIntent = Intent(context, WidgetConfigureActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val configPendingIntent = PendingIntent.getActivity(
                    context, appWidgetId, configIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btnCompactConfigure, configPendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
