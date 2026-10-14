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
import com.alice.homewidget.data.WidgetDataManager
import com.alice.homewidget.ui.MainActivity
import com.alice.homewidget.ui.WidgetConfigureActivity

class AliceBarWidgetProvider : AppWidgetProvider() {

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

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            dataManager: WidgetDataManager
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_alice_bar_4x1)
                val config = dataManager.getWidgetConfig(appWidgetId, "4x1")
                val slots = dataManager.get4x1BarSlots(appWidgetId)

                // Compute high-DPI widget pixel dimensions
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val density = context.resources.displayMetrics.density
                val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).let { if (it > 0) it else 320 }
                val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).let { if (it > 0) it else 72 }

                val widthPx = (minWidthDp * density).toInt().coerceIn(320, 1440)
                val heightPx = (minHeightDp * density).toInt().coerceIn(80, 500)

                // Render full card via Custom Canvas Graphics Engine
                val bitmap = WidgetRenderEngine.render4x1WidgetBitmap(
                    context, widthPx, heightPx, slots, config.theme
                )
                views.setImageViewBitmap(R.id.ivWidgetCanvas, bitmap)

                // 1. Click Hotspot: Open Main App
                val appIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val appPendingIntent = PendingIntent.getActivity(
                    context, 0, appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btnWidgetOpenApp, appPendingIntent)

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

                // 3. Click Hotspot: Refresh
                val refreshIntent = Intent(context, AliceHomeWidgetProvider::class.java).apply {
                    action = AliceHomeWidgetProvider.ACTION_REFRESH_WIDGET
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context, appWidgetId, refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btnWidgetRefresh, refreshPendingIntent)

                // Dynamic contrast for action buttons in light themes
                val isLightTheme = config.theme == WidgetDataManager.THEME_YANDEX_WHITE
                val iconColor = if (isLightTheme) 0xFF22252A.toInt() else 0xE6FFFFFF.toInt()
                val rippleBg = if (isLightTheme) R.drawable.bg_btn_ripple_light else R.drawable.bg_btn_ripple
                views.setInt(R.id.ivWidgetConfigure, "setColorFilter", iconColor)
                views.setInt(R.id.ivWidgetRefresh, "setColorFilter", iconColor)
                views.setInt(R.id.btnWidgetConfigure, "setBackgroundResource", rippleBg)
                views.setInt(R.id.btnWidgetRefresh, "setBackgroundResource", rippleBg)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
