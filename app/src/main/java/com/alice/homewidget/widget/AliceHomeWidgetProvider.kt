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
                        launch(Dispatchers.Main) {
                            for (id in allWidgetIds) {
                                updateWidget(context, appWidgetManager, id, dataManager)
                            }
                        }
                    }.onFailure {
                        launch(Dispatchers.Main) {
                            for (id in allWidgetIds) {
                                val views = RemoteViews(context.packageName, R.layout.widget_alice_sensors_4x2)
                                views.setTextViewText(R.id.tvWidgetUpdateTime, "Ошибка")
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

                // Header title
                val title = if (aggregated.primaryRoomName.isNotBlank() && aggregated.primaryRoomName != "Дом") {
                    aggregated.primaryRoomName
                } else {
                    aggregated.householdName
                }
                views.setTextViewText(R.id.tvHouseholdName, title)

                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val timeStr = if (aggregated.lastUpdatedTimestamp > 0) {
                    "Обновлено в " + timeFormat.format(Date(aggregated.lastUpdatedTimestamp))
                } else {
                    "Нажмите для обновления"
                }
                views.setTextViewText(R.id.tvWidgetUpdateTime, timeStr)

                // Temperature
                val tempStr = aggregated.primaryTemperature?.let { String.format(Locale.US, "%.1f°", it) } ?: "--°"
                views.setTextViewText(R.id.tvMainTemp, tempStr)
                views.setTextViewText(R.id.tvActiveRoomLabel, "Температура")

                // Humidity
                if (dataManager.showHumidity && aggregated.primaryHumidity != null) {
                    views.setViewVisibility(R.id.tileHumidity, View.VISIBLE)
                    views.setTextViewText(R.id.tvMainHumidity, String.format(Locale.US, "%.0f%%", aggregated.primaryHumidity))
                } else {
                    views.setViewVisibility(R.id.tileHumidity, View.GONE)
                }

                // Pressure (only if explicitly enabled and available)
                if (dataManager.showPressure && aggregated.primaryPressure != null) {
                    views.setViewVisibility(R.id.tilePressure, View.VISIBLE)
                    views.setTextViewText(R.id.tvMainPressure, String.format(Locale.US, "%.0f", aggregated.primaryPressure))
                } else {
                    views.setViewVisibility(R.id.tilePressure, View.GONE)
                }

                // Secondary rooms
                val otherRooms = aggregated.roomSensors
                if (otherRooms.isNotEmpty()) {
                    views.setViewVisibility(R.id.layoutSecondaryRooms, View.VISIBLE)
                    val r1 = otherRooms[0]
                    views.setTextViewText(R.id.tvRoom1Name, r1.roomName)
                    val r1Temp = r1.temperature?.let { String.format(Locale.US, "%.1f°", it) } ?: ""
                    val r1Hum = r1.humidity?.let { String.format(Locale.US, "%.0f%%", it) } ?: ""
                    views.setTextViewText(R.id.tvRoom1Value, "$r1Temp $r1Hum".trim())
                    views.setViewVisibility(R.id.tileRoom1, View.VISIBLE)

                    if (otherRooms.size > 1) {
                        val r2 = otherRooms[1]
                        views.setTextViewText(R.id.tvRoom2Name, r2.roomName)
                        val r2Temp = r2.temperature?.let { String.format(Locale.US, "%.1f°", it) } ?: ""
                        val r2Hum = r2.humidity?.let { String.format(Locale.US, "%.0f%%", it) } ?: ""
                        views.setTextViewText(R.id.tvRoom2Value, "$r2Temp $r2Hum".trim())
                        views.setViewVisibility(R.id.tileRoom2, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.tileRoom2, View.GONE)
                    }
                } else {
                    views.setViewVisibility(R.id.layoutSecondaryRooms, View.GONE)
                }

                // Door sensor
                var hasBottomItem = false
                if (dataManager.showDoor) {
                    hasBottomItem = true
                    views.setViewVisibility(R.id.tileDoor, View.VISIBLE)
                    if (aggregated.isAnyDoorOpen) {
                        views.setTextViewText(R.id.tvDoorStatus, "ОТКРЫТО!")
                        views.setTextColor(R.id.tvDoorStatus, ContextCompat.getColor(context, R.color.sensor_warn_red))
                    } else {
                        views.setTextViewText(R.id.tvDoorStatus, "Закрыто")
                        views.setTextColor(R.id.tvDoorStatus, ContextCompat.getColor(context, R.color.sensor_ok_green))
                    }
                } else {
                    views.setViewVisibility(R.id.tileDoor, View.GONE)
                }

                // Battery sensor
                if (dataManager.showBattery && aggregated.lowestBatteryLevel != null) {
                    hasBottomItem = true
                    views.setViewVisibility(R.id.tileBattery, View.VISIBLE)
                    views.setTextViewText(R.id.tvBatteryStatus, String.format(Locale.US, "%.0f%%", aggregated.lowestBatteryLevel))
                } else {
                    views.setViewVisibility(R.id.tileBattery, View.GONE)
                }

                views.setViewVisibility(R.id.layoutBottomStatus, if (hasBottomItem) View.VISIBLE else View.GONE)

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
