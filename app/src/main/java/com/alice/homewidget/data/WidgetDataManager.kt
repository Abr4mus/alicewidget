package com.alice.homewidget.data

import android.content.Context
import android.content.SharedPreferences
import com.alice.homewidget.model.AggregatedSensorData
import com.alice.homewidget.model.Device
import com.alice.homewidget.model.Room
import com.alice.homewidget.model.RoomClimateSummary
import com.alice.homewidget.model.UserInfoResponse
import com.google.gson.Gson
import java.util.Locale

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

        // Theme / Skin Keys
        const val THEME_ALICE = "ALICE"
        const val THEME_IOS = "IOS"
        const val THEME_ANDROID = "ANDROID"
        private const val KEY_WIDGET_THEME = "widget_theme"

        // Per-widget configuration keys
        private const val KEY_W4X2_DEVICE_IDS = "w4x2_device_ids"
        private const val KEY_W4X1_DEVICE_IDS = "w4x1_device_ids"
        private const val KEY_W2X2_ROOM_ID = "w2x2_room_id"
        private const val KEY_W2X2_DEVICE_ID = "w2x2_device_id"
        private const val KEY_W2X1_ROOM_ID = "w2x1_room_id"
        private const val KEY_W2X1_DEVICE_ID = "w2x1_device_id"
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
        get() = prefs.getBoolean(KEY_SHOW_PRESSURE, false)
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

    // 🎨 Theme / Skin
    var widgetTheme: String
        get() = prefs.getString(KEY_WIDGET_THEME, THEME_ALICE) ?: THEME_ALICE
        set(value) = prefs.edit().putString(KEY_WIDGET_THEME, value).apply()

    // 4×2 Device Selection
    var widget4x2DeviceIds: Set<String>
        get() = prefs.getStringSet(KEY_W4X2_DEVICE_IDS, null) ?: enabledDeviceIds
        set(value) = prefs.edit().putStringSet(KEY_W4X2_DEVICE_IDS, value).apply()

    // 4×1 Device Selection
    var widget4x1DeviceIds: Set<String>
        get() = prefs.getStringSet(KEY_W4X1_DEVICE_IDS, null) ?: enabledDeviceIds
        set(value) = prefs.edit().putStringSet(KEY_W4X1_DEVICE_IDS, value).apply()

    // 2×2 Room & Device Selection
    var widget2x2RoomId: String?
        get() = prefs.getString(KEY_W2X2_ROOM_ID, selectedRoomId)
        set(value) = prefs.edit().putString(KEY_W2X2_ROOM_ID, value).apply()

    var widget2x2DeviceId: String?
        get() = prefs.getString(KEY_W2X2_DEVICE_ID, null)
        set(value) = prefs.edit().putString(KEY_W2X2_DEVICE_ID, value).apply()

    // 2×1 Room & Device Selection
    var widget2x1RoomId: String?
        get() = prefs.getString(KEY_W2X1_ROOM_ID, selectedRoomId)
        set(value) = prefs.edit().putString(KEY_W2X1_ROOM_ID, value).apply()

    var widget2x1DeviceId: String?
        get() = prefs.getString(KEY_W2X1_DEVICE_ID, null)
        set(value) = prefs.edit().putString(KEY_W2X1_DEVICE_ID, value).apply()

    // Individual Device Metric Settings
    data class DeviceMetricSetting(
        val showTemperature: Boolean = true,
        val showHumidity: Boolean = true,
        val showPressure: Boolean = false,
        val showDoor: Boolean = true,
        val showBattery: Boolean = true
    )

    fun getDeviceMetricSetting(deviceId: String): DeviceMetricSetting {
        val temp = prefs.getBoolean("dev_${deviceId}_temp", true)
        val hum = prefs.getBoolean("dev_${deviceId}_hum", showHumidity)
        val press = prefs.getBoolean("dev_${deviceId}_press", showPressure)
        val door = prefs.getBoolean("dev_${deviceId}_door", showDoor)
        val bat = prefs.getBoolean("dev_${deviceId}_bat", showBattery)
        return DeviceMetricSetting(temp, hum, press, door, bat)
    }

    fun saveDeviceMetricSetting(deviceId: String, setting: DeviceMetricSetting) {
        prefs.edit()
            .putBoolean("dev_${deviceId}_temp", setting.showTemperature)
            .putBoolean("dev_${deviceId}_hum", setting.showHumidity)
            .putBoolean("dev_${deviceId}_press", setting.showPressure)
            .putBoolean("dev_${deviceId}_door", setting.showDoor)
            .putBoolean("dev_${deviceId}_bat", setting.showBattery)
            .apply()
    }

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

    fun getAllRelevantDevices(): List<Device> {
        val allDevices = getCachedUserInfo()?.devices ?: return emptyList()
        return allDevices.filter {
            it.isClimateSensor() || it.isOpenSensor() || it.isLeakSensor() ||
            it.getTemperature() != null || it.getHumidity() != null
        }
    }

    fun getDevicesForRoom(roomId: String?): List<Device> {
        val allDevices = getAllRelevantDevices()
        if (roomId.isNullOrBlank() || roomId == "ALL") {
            return allDevices
        }
        val room = getAvailableRooms().firstOrNull { it.id == roomId }
        return allDevices.filter { dev ->
            dev.room == roomId || room?.devices?.contains(dev.id) == true
        }
    }

    fun getAvailableDevices(): List<Device> {
        return getDevicesForRoom(selectedRoomId)
    }

    // --- 4x2 Widget Data ---
    fun get4x2SensorData(): AggregatedSensorData {
        val cached = getCachedUserInfo() ?: return AggregatedSensorData(
            householdName = "Климат",
            primaryTemperature = 22.5,
            primaryHumidity = if (showHumidity) 48.0 else null,
            primaryPressure = if (showPressure) 752.0 else null,
            primaryRoomName = "Гостиная",
            totalSensorCount = 0,
            lastUpdatedTimestamp = lastSyncTimestamp
        )

        val householdName = cached.households.firstOrNull()?.name ?: "Датчики"
        val allDevices = cached.devices
        val w4x2Ids = widget4x2DeviceIds

        val targetDevices = if (w4x2Ids.isNotEmpty()) {
            allDevices.filter { w4x2Ids.contains(it.id) }
        } else if (enabledDeviceIds.isNotEmpty()) {
            allDevices.filter { enabledDeviceIds.contains(it.id) }
        } else {
            allDevices
        }

        val rooms = cached.rooms

        // Find primary device / room
        val primaryDev = targetDevices.firstOrNull { it.isClimateSensor() || it.getTemperature() != null }
            ?: targetDevices.firstOrNull()
        val primaryRoom = rooms.firstOrNull { it.id == primaryDev?.room || it.devices.contains(primaryDev?.id) }
            ?: rooms.firstOrNull()

        val primarySetting = primaryDev?.let { getDeviceMetricSetting(it.id) } ?: DeviceMetricSetting()
        val primaryTemp = if (primarySetting.showTemperature) primaryDev?.getTemperature() ?: targetDevices.firstNotNullOfOrNull { it.getTemperature() } else null
        val primaryHum = if (primarySetting.showHumidity && showHumidity) primaryDev?.getHumidity() ?: targetDevices.firstNotNullOfOrNull { it.getHumidity() } else null
        val primaryPress = if (primarySetting.showPressure && showPressure) primaryDev?.getPressure() ?: targetDevices.firstNotNullOfOrNull { it.getPressure() } else null

        val primaryRoomName = primaryRoom?.name ?: primaryDev?.name ?: "Дом"

        // Secondary device/room summaries (devices across different rooms)
        val otherDevs = targetDevices.filter { it.id != primaryDev?.id }
        val roomSummaries = mutableListOf<RoomClimateSummary>()

        for (dev in otherDevs) {
            val devSetting = getDeviceMetricSetting(dev.id)
            val t = if (devSetting.showTemperature) dev.getTemperature() else null
            val h = if (devSetting.showHumidity && showHumidity) dev.getHumidity() else null
            val p = if (devSetting.showPressure && showPressure) dev.getPressure() else null

            if (t != null || h != null || p != null) {
                val rName = rooms.firstOrNull { it.id == dev.room || it.devices.contains(dev.id) }?.name ?: dev.name
                roomSummaries.add(
                    RoomClimateSummary(
                        roomId = dev.room ?: dev.id,
                        roomName = rName,
                        temperature = t,
                        humidity = h,
                        pressure = p
                    )
                )
                if (roomSummaries.size >= 2) break
            }
        }

        // Door status
        var anyDoorOpen = false
        var openDoorName: String? = null
        if (showDoor) {
            for (dev in targetDevices) {
                val devSetting = getDeviceMetricSetting(dev.id)
                if (devSetting.showDoor && dev.isDoorOrWindowOpen() == true) {
                    anyDoorOpen = true
                    openDoorName = dev.name
                    break
                }
            }
        }

        // Lowest battery
        val lowestBattery = if (showBattery) {
            targetDevices.filter { getDeviceMetricSetting(it.id).showBattery }
                .mapNotNull { it.getBattery() }.minOrNull()
        } else null

        return AggregatedSensorData(
            householdName = householdName,
            primaryTemperature = primaryTemp,
            primaryHumidity = primaryHum,
            primaryPressure = primaryPress,
            primaryRoomName = primaryRoomName,
            roomSensors = roomSummaries,
            isAnyDoorOpen = anyDoorOpen,
            openDoorDeviceName = openDoorName,
            isAnyLeakDetected = false,
            lowestBatteryLevel = lowestBattery,
            totalSensorCount = targetDevices.size,
            lastUpdatedTimestamp = lastSyncTimestamp
        )
    }

    // --- 4x1 Bar Widget Data (Multi-device across rooms) ---
    data class BarSlotItem(
        val roomName: String,
        val mainText: String,
        val secondaryText: String? = null,
        val isDoor: Boolean = false,
        val isDoorAlert: Boolean = false
    )

    fun get4x1BarSlots(): List<BarSlotItem> {
        val cached = getCachedUserInfo()
        if (cached == null) {
            return listOf(
                BarSlotItem("Гостиная", "22.5°", if (showHumidity) "48%" else null),
                BarSlotItem("Спальня", "21.0°", null),
                BarSlotItem("", "🚪 ОК", isDoor = true)
            )
        }

        val allDevices = cached.devices
        val w4x1Ids = widget4x1DeviceIds
        val targetDevices = if (w4x1Ids.isNotEmpty()) {
            allDevices.filter { w4x1Ids.contains(it.id) }
        } else if (enabledDeviceIds.isNotEmpty()) {
            allDevices.filter { enabledDeviceIds.contains(it.id) }
        } else {
            allDevices.filter { it.isClimateSensor() || it.isOpenSensor() }
        }

        val rooms = cached.rooms
        val result = mutableListOf<BarSlotItem>()

        for (dev in targetDevices) {
            val devSetting = getDeviceMetricSetting(dev.id)
            val rName = rooms.firstOrNull { it.id == dev.room || it.devices.contains(dev.id) }?.name ?: dev.name

            if (dev.isOpenSensor() && devSetting.showDoor && showDoor) {
                val isOpen = dev.isDoorOrWindowOpen() == true
                result.add(BarSlotItem(rName, if (isOpen) "🚪 ОТКРЫТО" else "🚪 ОК", isDoor = true, isDoorAlert = isOpen))
            } else {
                val t = if (devSetting.showTemperature) dev.getTemperature() else null
                val h = if (devSetting.showHumidity && showHumidity) dev.getHumidity() else null

                if (t != null || h != null) {
                    val tStr = t?.let { String.format(Locale.US, "%.1f°", it) } ?: ""
                    val hStr = h?.let { String.format(Locale.US, "%.0f%%", it) } ?: ""
                    result.add(BarSlotItem(rName, tStr, hStr.ifEmpty { null }))
                }
            }
            if (result.size >= 3) break
        }

        if (result.isEmpty()) {
            val agg = getAggregatedSensorData()
            result.add(BarSlotItem(agg.primaryRoomName, agg.primaryTemperature?.let { String.format(Locale.US, "%.1f°", it) } ?: "--°", agg.primaryHumidity?.let { String.format(Locale.US, "%.0f%%", it) }))
            if (showDoor) {
                result.add(BarSlotItem("", if (agg.isAnyDoorOpen) "🚪 ОТКРЫТО" else "🚪 ОК", isDoor = true, isDoorAlert = agg.isAnyDoorOpen))
            }
        }

        return result
    }

    // --- 2x1 Compact Data ---
    data class Compact2x1Data(
        val title: String,
        val tempText: String,
        val humidityText: String?
    )

    fun get2x1Data(): Compact2x1Data {
        val cached = getCachedUserInfo() ?: return Compact2x1Data(
            title = "Гостиная",
            tempText = "22.5°",
            humidityText = if (showHumidity) "48%" else null
        )

        val devId = widget2x1DeviceId
        val roomId = widget2x1RoomId
        val allDevices = cached.devices
        val rooms = cached.rooms

        val dev = if (!devId.isNullOrBlank()) {
            allDevices.firstOrNull { it.id == devId }
        } else if (!roomId.isNullOrBlank() && roomId != "ALL") {
            allDevices.firstOrNull { (it.room == roomId || rooms.firstOrNull { r -> r.id == roomId }?.devices?.contains(it.id) == true) && (it.getTemperature() != null || it.isClimateSensor()) }
        } else {
            allDevices.firstOrNull { it.getTemperature() != null || it.isClimateSensor() }
        }

        val title = if (dev != null) {
            rooms.firstOrNull { it.id == dev.room || it.devices.contains(dev.id) }?.name ?: dev.name
        } else {
            "Климат"
        }

        val setting = dev?.let { getDeviceMetricSetting(it.id) } ?: DeviceMetricSetting()
        val temp = if (setting.showTemperature) dev?.getTemperature() else null
        val hum = if (setting.showHumidity && showHumidity) dev?.getHumidity() else null

        val tempStr = temp?.let { String.format(Locale.US, "%.1f°", it) }
            ?: (getAggregatedSensorData().primaryTemperature?.let { String.format(Locale.US, "%.1f°", it) } ?: "--°")

        val humStr = hum?.let { String.format(Locale.US, "%.0f%%", it) }
            ?: (if (showHumidity) getAggregatedSensorData().primaryHumidity?.let { String.format(Locale.US, "%.0f%%", it) } else null)

        return Compact2x1Data(title, tempStr, humStr)
    }

    // --- 2x2 Compact Data ---
    data class Compact2x2Data(
        val title: String,
        val tempText: String,
        val humidityText: String?
    )

    fun get2x2Data(): Compact2x2Data {
        val cached = getCachedUserInfo() ?: return Compact2x2Data(
            title = "Климат",
            tempText = "22.5°",
            humidityText = if (showHumidity) "48%" else null
        )

        val devId = widget2x2DeviceId
        val roomId = widget2x2RoomId
        val allDevices = cached.devices
        val rooms = cached.rooms

        val dev = if (!devId.isNullOrBlank()) {
            allDevices.firstOrNull { it.id == devId }
        } else if (!roomId.isNullOrBlank() && roomId != "ALL") {
            allDevices.firstOrNull { (it.room == roomId || rooms.firstOrNull { r -> r.id == roomId }?.devices?.contains(it.id) == true) && (it.getTemperature() != null || it.isClimateSensor()) }
        } else {
            allDevices.firstOrNull { it.getTemperature() != null || it.isClimateSensor() }
        }

        val title = if (dev != null) {
            rooms.firstOrNull { it.id == dev.room || it.devices.contains(dev.id) }?.name ?: dev.name
        } else {
            "Климат"
        }

        val setting = dev?.let { getDeviceMetricSetting(it.id) } ?: DeviceMetricSetting()
        val temp = if (setting.showTemperature) dev?.getTemperature() else null
        val hum = if (setting.showHumidity && showHumidity) dev?.getHumidity() else null

        val tempStr = temp?.let { String.format(Locale.US, "%.1f°", it) }
            ?: (getAggregatedSensorData().primaryTemperature?.let { String.format(Locale.US, "%.1f°", it) } ?: "--°")

        val humStr = hum?.let { String.format(Locale.US, "%.0f%%", it) }
            ?: (if (showHumidity) getAggregatedSensorData().primaryHumidity?.let { String.format(Locale.US, "%.0f%%", it) } else null)

        return Compact2x2Data(title, tempStr, humStr)
    }

    // Backward-compatible general aggregator
    fun getAggregatedSensorData(): AggregatedSensorData {
        return get4x2SensorData()
    }
}
