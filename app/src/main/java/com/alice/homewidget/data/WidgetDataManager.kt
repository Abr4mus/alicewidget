package com.alice.homewidget.data

import android.content.Context
import android.content.SharedPreferences
import com.alice.homewidget.model.AggregatedSensorData
import com.alice.homewidget.model.RoomClimateSummary
import com.alice.homewidget.model.UserInfoResponse
import com.google.gson.Gson

class WidgetDataManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "alice_widget_prefs"
        private const val KEY_OAUTH_TOKEN = "oauth_token"
        private const val KEY_CACHED_USER_INFO = "cached_user_info_json"
        private const val KEY_LAST_SYNC_TIME = "last_sync_timestamp"
        private const val KEY_SYNC_INTERVAL_MIN = "sync_interval_minutes"
        private const val KEY_SELECTED_ROOM_ID = "selected_room_id"
    }

    var oauthToken: String
        get() = prefs.getString(KEY_OAUTH_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OAUTH_TOKEN, value.trim()).apply()

    var syncIntervalMinutes: Int
        get() = prefs.getInt(KEY_SYNC_INTERVAL_MIN, 15)
        set(value) = prefs.edit().putInt(KEY_SYNC_INTERVAL_MIN, value).apply()

    var selectedRoomId: String?
        get() = prefs.getString(KEY_SELECTED_ROOM_ID, null)
        set(value) = prefs.edit().putString(KEY_SELECTED_ROOM_ID, value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIME, value).apply()

    fun saveUserInfo(response: UserInfoResponse) {
        val json = gson.toJson(response)
        prefs.edit()
            .putString(KEY_CACHED_USER_INFO, json)
            .putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis())
            .apply()
    }

    fun getCachedUserInfo(): UserInfoResponse? {
        val json = prefs.getString(KEY_CACHED_USER_INFO, null) ?: return null
        return try {
            gson.fromJson(json, UserInfoResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getAggregatedSensorData(): AggregatedSensorData {
        val cached = getCachedUserInfo() ?: return AggregatedSensorData(
            householdName = "Мой дом",
            primaryTemperature = 22.5,
            primaryHumidity = 48.0,
            primaryPressure = 752.0,
            primaryRoomName = "Гостиная",
            totalSensorCount = 0,
            lastUpdatedTimestamp = lastSyncTimestamp
        )

        return aggregate(cached)
    }

    private fun aggregate(info: UserInfoResponse): AggregatedSensorData {
        val householdName = info.households.firstOrNull()?.name ?: "Мой дом"
        val devices = info.devices
        val rooms = info.rooms

        var primaryTemp: Double? = null
        var primaryHum: Double? = null
        var primaryPress: Double? = null
        var primaryRoomName = "Гостиная"

        // Find primary climate readings
        // If a preferred room is selected, check it first
        val targetRoom = rooms.firstOrNull { it.id == selectedRoomId } 
            ?: rooms.firstOrNull { it.name.contains("Гостин", ignoreCase = true) }
            ?: rooms.firstOrNull()

        if (targetRoom != null) {
            primaryRoomName = targetRoom.name
            val roomDevs = devices.filter { it.room == targetRoom.id || targetRoom.devices.contains(it.id) }
            for (dev in roomDevs) {
                if (primaryTemp == null) primaryTemp = dev.getTemperature()
                if (primaryHum == null) primaryHum = dev.getHumidity()
                if (primaryPress == null) primaryPress = dev.getPressure()
            }
        }

        // Fallbacks across all devices if still null
        if (primaryTemp == null) {
            primaryTemp = devices.firstNotNullOfOrNull { it.getTemperature() }
        }
        if (primaryHum == null) {
            primaryHum = devices.firstNotNullOfOrNull { it.getHumidity() }
        }
        if (primaryPress == null) {
            primaryPress = devices.firstNotNullOfOrNull { it.getPressure() }
        }

        // Check door sensors
        var anyDoorOpen = false
        var openDoorName: String? = null
        for (dev in devices) {
            if (dev.isDoorOrWindowOpen() == true) {
                anyDoorOpen = true
                openDoorName = dev.name
                break
            }
        }

        // Check leak sensors
        var anyLeak = false
        var leakName: String? = null
        for (dev in devices) {
            if (dev.hasWaterLeak() == true) {
                anyLeak = true
                leakName = dev.name
                break
            }
        }

        // Check lowest battery
        val batteryLevels = devices.mapNotNull { it.getBattery() }
        val lowestBattery = batteryLevels.minOrNull()

        // Room summaries
        val roomSummaries = mutableListOf<RoomClimateSummary>()
        for (r in rooms) {
            val rDevs = devices.filter { it.room == r.id || r.devices.contains(it.id) }
            val t = rDevs.firstNotNullOfOrNull { it.getTemperature() }
            val h = rDevs.firstNotNullOfOrNull { it.getHumidity() }
            val p = rDevs.firstNotNullOfOrNull { it.getPressure() }

            if (t != null || h != null || p != null) {
                roomSummaries.add(
                    RoomClimateSummary(
                        roomId = r.id,
                        roomName = r.name,
                        temperature = t,
                        humidity = h,
                        pressure = p
                    )
                )
            }
        }

        val totalSensors = devices.count { it.isClimateSensor() || it.isOpenSensor() || it.isLeakSensor() }

        return AggregatedSensorData(
            householdName = householdName,
            primaryTemperature = primaryTemp,
            primaryHumidity = primaryHum,
            primaryPressure = primaryPress,
            primaryRoomName = primaryRoomName,
            roomSensors = roomSummaries,
            isAnyDoorOpen = anyDoorOpen,
            openDoorDeviceName = openDoorName,
            isAnyLeakDetected = anyLeak,
            leakDeviceName = leakName,
            lowestBatteryLevel = lowestBattery,
            totalSensorCount = totalSensors,
            lastUpdatedTimestamp = lastSyncTimestamp
        )
    }
}
