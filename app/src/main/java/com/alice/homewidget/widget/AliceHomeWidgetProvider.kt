package com.alice.homewidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import com.alice.homewidget.R
import com.alice.homewidget.api.YandexApiClient
import com.alice.homewidget.data.WidgetDataManager
import com.alice.homewidget.ui.MainActivity
import com.alice.homewidget.ui.WidgetConfigureActivity
import com.alice.homewidget.worker.SensorSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AliceHomeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val dataManager = WidgetDataManager(context)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, dataManager)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val dataManager = WidgetDataManager(context)
        updateWidget(context, appWidgetManager, appWidgetId, dataManager)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val dataManager = WidgetDataManager(context)
        for (appWidgetId in appWidgetIds) {
            dataManager.deleteWidgetConfig(appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_REFRESH_WIDGET) {
            val dataManager = WidgetDataManager(context)
            val token = dataManager.oauthToken
            if (token.isNotBlank()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val result = YandexApiClient().fetchUserInfo(token)
                    result.onSuccess { data ->
                        dataManager.saveUserInfo(data)
                        launch(Dispatchers.Main) {
                            // Notify and refresh all active widgets across all sizes
                            SensorSyncWorker.notifyWidgetsToUpdate(context)
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.alice.homewidget.ACTION_REFRESH_WIDGET"

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            dataManager: WidgetDataManager
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_alice_sensors_4x2)
                val config = dataManager.getWidgetConfig(appWidgetId, "4x2")
                val aggregated = dataManager.get4x2SensorData(appWidgetId)

                // Compute high-DPI widget pixel dimensions
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val density = context.resources.displayMetrics.density
                val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).let { if (it > 0) it else 340 }
                val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).let { if (it > 0) it else 150 }

                val widthPx = (minWidthDp * density).toInt().coerceIn(360, 1440)
                val heightPx = (minHeightDp * density).toInt().coerceIn(160, 900)

                // Render full card via Custom Canvas Graphics Engine
                val bitmap = WidgetRenderEngine.render4x2WidgetBitmap(
                    context, widthPx, heightPx, aggregated, config.theme
                )
                views.setImageViewBitmap(R.id.ivWidgetCanvas, bitmap)

                // 1. Click Hotspot on Widget Body: Instant Refresh
                val refreshIntent = Intent(context, AliceHomeWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH_WIDGET
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context, appWidgetId, refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btnWidgetOpenApp, refreshPendingIntent)

                // 2. Click Hotspot: Configure THIS specific widget instance
                val configIntent = Intent(context, WidgetConfigureActivity::class.java).apply {
                    action = "com.alice.homewidget.CONFIGURE_WIDGET_$appWidgetId"
                    this.data = Uri.parse("alice://widget/$appWidgetId")
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val configPendingIntent = PendingIntent.getActivity(
                    context, appWidgetId, configIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btnWidgetConfigure, configPendingIntent)

                // Dynamic contrast for gear button in light themes
                val palette = WidgetRenderEngine.getThemePalette(config.theme)
                val isLightTheme = palette.isLight
                val iconColor = if (isLightTheme) 0xFF22252A.toInt() else 0xE6FFFFFF.toInt()
                val rippleBg = if (isLightTheme) R.drawable.bg_btn_ripple_light else R.drawable.bg_btn_ripple
                views.setInt(R.id.ivWidgetConfigure, "setColorFilter", iconColor)
                views.setInt(R.id.btnWidgetConfigure, "setBackgroundResource", rippleBg)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun clearWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
                val layoutId = when (info?.provider?.className) {
                    AliceBarWidgetProvider::class.java.name -> R.layout.widget_alice_bar_4x1
                    AliceCompactWidgetProvider::class.java.name -> R.layout.widget_alice_compact_2x2
                    AliceCompact2x1WidgetProvider::class.java.name -> R.layout.widget_alice_compact_2x1
                    else -> R.layout.widget_alice_sensors_4x2
                }

                val views = RemoteViews(context.packageName, layoutId)
                val density = context.resources.displayMetrics.density
                val bitmap = WidgetRenderEngine.renderDisabledWidgetBitmap(context, (340 * density).toInt(), (150 * density).toInt())
                views.setImageViewBitmap(R.id.ivWidgetCanvas, bitmap)

                val configIntent = Intent(context, WidgetConfigureActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pi = PendingIntent.getActivity(
                    context, appWidgetId, configIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btnWidgetOpenApp, pi)
                views.setOnClickPendingIntent(R.id.btnWidgetConfigure, pi)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
