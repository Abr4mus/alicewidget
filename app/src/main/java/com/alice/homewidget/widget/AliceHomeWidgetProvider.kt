package com.alice.homewidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.alice.homewidget.R
import com.alice.homewidget.api.YandexApiClient
import com.alice.homewidget.data.WidgetDataManager
import com.alice.homewidget.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AliceHomeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val dataManager = WidgetDataManager(context)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, dataManager)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_REFRESH_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, AliceHomeWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            // Visual feedback: show "Синхронизация..." immediately
            for (id in allWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_alice_sensors_4x2)
                views.setTextViewText(R.id.tvWidgetUpdateTime, "Синхронизация...")
                appWidgetManager.partiallyUpdateAppWidget(id, views)
            }

            // Perform network request
            val dataManager = WidgetDataManager(context)
            val token = dataManager.oauthToken
            if (token.isNotBlank()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val result = YandexApiClient().fetchUserInfo(token)
                    result.onSuccess { data ->
                        dataManager.saveUserInfo(data)
                        // Trigger update of all widgets on Main thread
                        launch(Dispatchers.Main) {
                            for (id in allWidgetIds) {
                                updateWidget(context, appWidgetManager, id, dataManager)
                            }
                        }
                    }.onFailure {
                        launch(Dispatchers.Main) {
                            for (id in allWidgetIds) {
                                val views = RemoteViews(context.packageName, R.layout.widget_alice_sensors_4x2)
                                views.setTextViewText(R.id.tvWidgetUpdateTime, "Ошибка обновления")
                                appWidgetManager.partiallyUpdateAppWidget(id, views)
                            }
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
                val aggregated = dataManager.getAggregatedSensorData()

                // Header
                views.setTextViewText(R.id.tvHouseholdName, aggregated.householdName)

                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val timeStr = if (aggregated.lastUpdatedTimestamp > 0) {
                    "Обновлено в " + timeFormat.format(Date(aggregated.lastUpdatedTimestamp))
                } else {
                    "Требуется обновление"
                }
                views.setTextViewText(R.id.tvWidgetUpdateTime, timeStr)

                // Primary Climate (Living Room / Main)
                views.setTextViewText(R.id.tvActiveRoomLabel, aggregated.primaryRoomName)

                val tempStr = aggregated.primaryTemperature?.let { String.format(Locale.US, "%.1f°", it) } ?: "--°"
                views.setTextViewText(R.id.tvMainTemp, tempStr)

                val humStr = aggregated.primaryHumidity?.let { String.format(Locale.US, "%.0f%%", it) } ?: "--%"
                views.setTextViewText(R.id.tvMainHumidity, humStr)

                val pressStr = aggregated.primaryPressure?.let { String.format(Locale.US, "%.0f", it) } ?: "752"
                views.setTextViewText(R.id.tvMainPressure, pressStr)

                // Additional rooms summary
                val otherRooms = aggregated.roomSensors.filter { it.roomName != aggregated.primaryRoomName }
                if (otherRooms.isNotEmpty()) {
                    val r1 = otherRooms[0]
                    views.setTextViewText(R.id.tvRoom1Name, r1.roomName)
                    val r1Temp = r1.temperature?.let { String.format(Locale.US, "%.1f°", it) } ?: "--°"
                    val r1Hum = r1.humidity?.let { String.format(Locale.US, "%.0f%%", it) } ?: ""
                    views.setTextViewText(R.id.tvRoom1Value, "$r1Temp $r1Hum".trim())
                    views.setViewVisibility(R.id.tileRoom1, View.VISIBLE)
                } else {
                    views.setTextViewText(R.id.tvRoom1Name, "Спальня")
                    views.setTextViewText(R.id.tvRoom1Value, "21.8° 52%")
                    views.setViewVisibility(R.id.tileRoom1, View.VISIBLE)
                }

                if (otherRooms.size > 1) {
                    val r2 = otherRooms[1]
                    views.setTextViewText(R.id.tvRoom2Name, r2.roomName)
                    val r2Temp = r2.temperature?.let { String.format(Locale.US, "%.1f°", it) } ?: "--°"
                    val r2Hum = r2.humidity?.let { String.format(Locale.US, "%.0f%%", it) } ?: ""
                    views.setTextViewText(R.id.tvRoom2Value, "$r2Temp $r2Hum".trim())
                    views.setViewVisibility(R.id.tileRoom2, View.VISIBLE)
                } else {
                    views.setTextViewText(R.id.tvRoom2Name, "Кухня")
                    views.setTextViewText(R.id.tvRoom2Value, "22.1° 44%")
                    views.setViewVisibility(R.id.tileRoom2, View.VISIBLE)
                }

                // Door sensor
                if (aggregated.isAnyDoorOpen) {
                    views.setTextViewText(R.id.tvDoorStatus, "ОТКРЫТА!")
                    views.setTextColor(R.id.tvDoorStatus, ContextCompat.getColor(context, R.color.sensor_warn_red))
                } else {
                    views.setTextViewText(R.id.tvDoorStatus, "Закрыта")
                    views.setTextColor(R.id.tvDoorStatus, ContextCompat.getColor(context, R.color.sensor_ok_green))
                }

                // Battery
                val bat = aggregated.lowestBatteryLevel?.let { String.format(Locale.US, "%.0f%% мин.", it) } ?: "92% мин."
                views.setTextViewText(R.id.tvBatteryStatus, bat)

                // Click Intent: Open App on Header Click
                val openAppIntent = Intent(context, MainActivity::class.java)
                val openAppPendingIntent = PendingIntent.getActivity(
                    context, 0, openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.headerLayout, openAppPendingIntent)

                // Click Intent: Refresh Button
                val refreshIntent = Intent(context, AliceHomeWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH_WIDGET
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context, appWidgetId, refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btnWidgetRefresh, refreshPendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
