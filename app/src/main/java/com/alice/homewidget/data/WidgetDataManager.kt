package com.alice.homewidget.data

import android.content.Context
import android.content.SharedPreferences
import com.alice.homewidget.model.AggregatedSensorData
import com.alice.homewidget.model.Device
import com.alice.homewidget.model.Room
import com.alice.homewidget.model.RoomClimateSummary
import com.alice.homewidget.model.UserInfoResponse
import com.google.gson.Gson

class WidgetDataManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "alice_widget_prefs"
        private const val KEY_CLIENT_ID = "yandex_client_id"
        private const val KEY_OAUTH_TOKEN = "oauth_token"
        private const val KEY_CACHED_USER_INFO = "cached_user_info_json"
        private const val KEY_LAST_SYNC_TIME = "last_sync_timestamp"
        private const val KEY_SYNC_INTERVAL_MIN = "sync_interval_minutes"
        private const val KEY_SELECTED_ROOM_ID = "selected_room_id"
        private const val KEY_SHOW_PRESSURE = "show_pressure"
        private const val KEY_SHOW_HUMIDITY = "show_humidity"
        private const val KEY_SHOW_DOOR = "show_door"
        private const val KEY_SHOW_BATTERY = "show_battery"
        private const val KEY_ENABLED_DEVICE_IDS = "enabled_device_ids"
    }

    var clientId: String
        get() = prefs.getString(KEY_CLIENT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CLIENT_ID, value.trim()).apply()

    var oauthToken: String
        get() = prefs.getString(KEY_OAUTH_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OAUTH_TOKEN, value.trim()).apply()

    var syncIntervalMinutes: Int
        get() = prefs.getInt(KEY_SYNC_INTERVAL_MIN, 15)
        set(value) = prefs.edit().putInt(KEY_SYNC_INTERVAL_MIN, value).apply()

    var selectedRoomId: String?
        get() = prefs.getString(KEY_SELECTED_ROOM_ID, null)
        set(value) = prefs.edit().putString(KEY_SELECTED_ROOM_ID, value).apply()

    var showPressure: Boolean
        get() = prefs.getBoolean(KEY_SHOW_PRESSURE, false) // Default FALSE as requested
        set(value) = prefs.edit().putBoolean(KEY_SHOW_PRESSURE, value).apply()

    var showHumidity: Boolean
        get() = prefs.getBoolean(KEY_SHOW_HUMIDITY, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_HUMIDITY, value).apply()

    var showDoor: Boolean
        get() = prefs.getBoolean(KEY_SHOW_DOOR, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_DOOR, value).apply()

    var showBattery: Boolean
        get() = prefs.getBoolean(KEY_SHOW_BATTERY, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_BATTERY, value).apply()

    var enabledDeviceIds: Set<String>
        get() = prefs.getStringSet(KEY_ENABLED_DEVICE_IDS, null) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_ENABLED_DEVICE_IDS, value).apply()

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

    fun getAvailableRooms(): List<Room> {
        return getCachedUserInfo()?.rooms ?: emptyList()
    }

    fun getAvailableDevices(): List<Device> {
        return getCachedUserInfo()?.devices?.filter {
            it.isClimateSensor() || it.isOpenSensor() || it.isLeakSensor() || it.getTemperature() != null || it.getHumidity() != null
        } ?: emptyList()
    }

    fun getAggregatedSensorData(): AggregatedSensorData {
        val cached = getCachedUserInfo() ?: return AggregatedSensorData(
            householdName = "Климат",
            primaryTemperature = 22.5,
            primaryHumidity = if (showHumidity) 48.0 else null,
            primaryPressure = if (showPressure) 752.0 else null,
            primaryRoomName = "Гостиная",
            totalSensorCount = 0,
            lastUpdatedTimestamp = lastSyncTimestamp
        )

        return aggregate(cached)
    }

    private fun aggregate(info: UserInfoResponse): AggregatedSensorData {
        val householdName = info.households.firstOrNull()?.name ?: "Датчики"
        val allDevices = info.devices
        val enabledSet = enabledDeviceIds

        // Filter only enabled devices if user selected some, otherwise all
        val devices = if (enabledSet.isNotEmpty()) {
            allDevices.filter { enabledSet.contains(it.id) }
        } else {
            allDevices
        }

        val rooms = info.rooms

        var primaryTemp: Double? = null
        var primaryHum: Double? = null
        var primaryPress: Double? = null
        var primaryRoomName = ""

        // Find primary room
        val targetRoom = rooms.firstOrNull { it.id == selectedRoomId }
            ?: rooms.firstOrNull { r -> devices.any { it.room == r.id || r.devices.contains(it.id) } }
            ?: rooms.firstOrNull()

        if (targetRoom != null) {
            primaryRoomName = targetRoom.name
            val roomDevs = devices.filter { it.room == targetRoom.id || targetRoom.devices.contains(it.id) }
            for (dev in roomDevs) {
                if (primaryTemp == null) primaryTemp = dev.getTemperature()
                if (primaryHum == null && showHumidity) primaryHum = dev.getHumidity()
                if (primaryPress == null && showPressure) primaryPress = dev.getPressure()
            }
        }

        // Fallbacks across filtered devices
        if (primaryTemp == null) {
            primaryTemp = devices.firstNotNullOfOrNull { it.getTemperature() }
        }
        if (primaryHum == null && showHumidity) {
            primaryHum = devices.firstNotNullOfOrNull { it.getHumidity() }
        }
        if (primaryPress == null && showPressure) {
            primaryPress = devices.firstNotNullOfOrNull { it.getPressure() }
        }

        if (primaryRoomName.isBlank()) {
            primaryRoomName = targetRoom?.name ?: "Дом"
        }

        // Check door sensors (only if enabled in settings)
        var anyDoorOpen = false
        var openDoorName: String? = null
        if (showDoor) {
            for (dev in devices) {
                if (dev.isDoorOrWindowOpen() == true) {
                    anyDoorOpen = true
                    openDoorName = dev.name
                    break
                }
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

        // Check lowest battery (only if enabled)
        val lowestBattery = if (showBattery) {
            devices.mapNotNull { it.getBattery() }.minOrNull()
        } else null

        // Room summaries (excluding primary room)
        val roomSummaries = mutableListOf<RoomClimateSummary>()
        for (r in rooms) {
            if (r.id == targetRoom?.id) continue
            val rDevs = devices.filter { it.room == r.id || r.devices.contains(it.id) }
            val t = rDevs.firstNotNullOfOrNull { it.getTemperature() }
            val h = if (showHumidity) rDevs.firstNotNullOfOrNull { it.getHumidity() } else null
            val p = if (showPressure) rDevs.firstNotNullOfOrNull { it.getPressure() } else null

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
            primaryHumidity = if (showHumidity) primaryHum else null,
            primaryPressure = if (showPressure) primaryPress else null,
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
