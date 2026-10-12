package com.alice.homewidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.alice.homewidget.R
import com.alice.homewidget.data.WidgetDataManager
import com.alice.homewidget.ui.MainActivity

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
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_alice_bar_4x1)
                val skin = dataManager.widgetTheme
                val slots = dataManager.get4x1BarSlots()

                // Apply dynamic skin
                views.setInt(R.id.rootLayoutBar, "setBackgroundResource", WidgetSkinHelper.getWidgetBackground(skin))
                views.setInt(R.id.btnBarRefresh, "setBackgroundResource", WidgetSkinHelper.getBtnBackground(skin))

                val colorPrimary = WidgetSkinHelper.getTextColorPrimary(skin)
                val colorSecondary = WidgetSkinHelper.getTextColorSecondary(skin)
                val colorHumidity = WidgetSkinHelper.getAccentHumidityColor(skin)

                views.setTextColor(R.id.tvBarHousehold, colorSecondary)
                views.setTextColor(R.id.tvBarTemp, colorPrimary)
                views.setTextColor(R.id.tvBarHumidity, colorHumidity)

                views.setTextColor(R.id.tvBarSlot2Room, colorSecondary)
                views.setTextColor(R.id.tvBarSlot2Value, colorPrimary)

                // Populate Slot 1
                if (slots.isNotEmpty()) {
                    val s1 = slots[0]
                    views.setTextViewText(R.id.tvBarHousehold, s1.roomName)
                    views.setTextViewText(R.id.tvBarTemp, s1.mainText)
                    if (s1.secondaryText != null) {
                        views.setViewVisibility(R.id.tvBarHumidity, View.VISIBLE)
                        views.setTextViewText(R.id.tvBarHumidity, s1.secondaryText)
                    } else {
                        views.setViewVisibility(R.id.tvBarHumidity, View.GONE)
                    }
                }

                // Populate Slot 2 (if available)
                if (slots.size > 1) {
                    val s2 = slots[1]
                    views.setViewVisibility(R.id.layoutBarSlot2, View.VISIBLE)
                    views.setTextViewText(R.id.tvBarSlot2Room, s2.roomName)
                    val s2Text = if (s2.secondaryText != null) "${s2.mainText} ${s2.secondaryText}" else s2.mainText
                    views.setTextViewText(R.id.tvBarSlot2Value, s2Text)
                } else {
                    views.setViewVisibility(R.id.layoutBarSlot2, View.GONE)
                }

                // Populate Slot 3 (if available)
                if (slots.size > 2) {
                    val s3 = slots[2]
                    views.setViewVisibility(R.id.layoutBarSlot3, View.VISIBLE)
                    if (s3.roomName.isNotBlank()) {
                        views.setViewVisibility(R.id.tvBarSlot3Room, View.VISIBLE)
                        views.setTextViewText(R.id.tvBarSlot3Room, s3.roomName)
                    } else {
                        views.setViewVisibility(R.id.tvBarSlot3Room, View.GONE)
                    }
                    val s3Text = if (s3.secondaryText != null) "${s3.mainText} ${s3.secondaryText}" else s3.mainText
                    views.setTextViewText(R.id.tvBarDoor, s3Text)
                    if (s3.isDoorAlert) {
                        views.setTextColor(R.id.tvBarDoor, ContextCompat.getColor(context, R.color.sensor_warn_red))
                    } else if (s3.isDoor) {
                        views.setTextColor(R.id.tvBarDoor, ContextCompat.getColor(context, R.color.sensor_ok_green))
                    } else {
                        views.setTextColor(R.id.tvBarDoor, colorPrimary)
                    }
                } else if (slots.size <= 2 && dataManager.showDoor) {
                    val agg = dataManager.getAggregatedSensorData()
                    views.setViewVisibility(R.id.layoutBarSlot3, View.VISIBLE)
                    views.setViewVisibility(R.id.tvBarSlot3Room, View.GONE)
                    if (agg.isAnyDoorOpen) {
                        views.setTextViewText(R.id.tvBarDoor, "🚪 ОТКРЫТО")
                        views.setTextColor(R.id.tvBarDoor, ContextCompat.getColor(context, R.color.sensor_warn_red))
                    } else {
                        views.setTextViewText(R.id.tvBarDoor, "🚪 ОК")
                        views.setTextColor(R.id.tvBarDoor, ContextCompat.getColor(context, R.color.sensor_ok_green))
                    }
                } else {
                    views.setViewVisibility(R.id.layoutBarSlot3, View.GONE)
                }

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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
