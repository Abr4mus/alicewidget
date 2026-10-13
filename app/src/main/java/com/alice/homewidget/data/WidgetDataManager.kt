package com.alice.homewidget.data

import android.content.Context
import android.content.SharedPreferences
import com.alice.homewidget.model.AggregatedSensorData
import com.alice.homewidget.model.Device
import com.alice.homewidget.model.Room
import com.alice.homewidget.model.RoomClimateSummary
import com.alice.homewidget.model.UserInfoResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

        // Theme Constants
        const val THEME_XIAOMI = "XIAOMI"
        const val THEME_IOS = "IOS"
        const val THEME_ALICE = "ALICE"
        const val THEME_ANDROID = "ANDROID"
        private const val KEY_DEFAULT_THEME = "widget_default_theme"

        // Active widget tracking
        private const val KEY_CONFIGURED_WIDGET_IDS = "configured_widget_ids_set"
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

    var defaultTheme: String
        get() = prefs.getString(KEY_DEFAULT_THEME, THEME_XIAOMI) ?: THEME_XIAOMI
        set(value) = prefs.edit().putString(KEY_DEFAULT_THEME, value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIME, value).apply()

    // Backward-compatible alias
    var widgetTheme: String
        get() = defaultTheme
        set(value) { defaultTheme = value }

    // --- Per-Widget Instance Configuration ---
    data class WidgetInstanceConfig(
        val appWidgetId: Int,
        val theme: String = THEME_XIAOMI,
        val selectedDeviceIds: List<String> = emptyList(),
        val customTitle: String? = null
    )

    fun getWidgetConfig(appWidgetId: Int, defaultType: String = "4x2"): WidgetInstanceConfig {
        val theme = prefs.getString("w_${appWidgetId}_theme", defaultTheme) ?: defaultTheme
        val jsonDevices = prefs.getString("w_${appWidgetId}_dev_ids", null)
        val title = prefs.getString("w_${appWidgetId}_title", null)

        val deviceIds: List<String> = if (!jsonDevices.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson(jsonDevices, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            // Default: pick first available climate devices if not yet configured
            emptyList()
        }

        return WidgetInstanceConfig(
            appWidgetId = appWidgetId,
            theme = theme,
            selectedDeviceIds = deviceIds,
            customTitle = title
        )
    }

    fun saveWidgetConfig(config: WidgetInstanceConfig) {
        val jsonDevices = gson.toJson(config.selectedDeviceIds)
        prefs.edit()
            .putString("w_${config.appWidgetId}_theme", config.theme)
            .putString("w_${config.appWidgetId}_dev_ids", jsonDevices)
            .putString("w_${config.appWidgetId}_title", config.customTitle)
            .apply()

        // Track widget ID in active set
        val currentSet = prefs.getStringSet(KEY_CONFIGURED_WIDGET_IDS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(config.appWidgetId.toString())
        prefs.edit().putStringSet(KEY_CONFIGURED_WIDGET_IDS, currentSet).apply()
    }

    fun getAllConfiguredWidgetIds(): List<Int> {
        val set = prefs.getStringSet(KEY_CONFIGURED_WIDGET_IDS, emptySet()) ?: emptySet()
        return set.mapNotNull { it.toIntOrNull() }
    }

    fun deleteWidgetConfig(appWidgetId: Int) {
        prefs.edit()
            .remove("w_${appWidgetId}_theme")
            .remove("w_${appWidgetId}_dev_ids")
            .remove("w_${appWidgetId}_title")
            .apply()

        val currentSet = prefs.getStringSet(KEY_CONFIGURED_WIDGET_IDS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        currentSet.remove(appWidgetId.toString())
        prefs.edit().putStringSet(KEY_CONFIGURED_WIDGET_IDS, currentSet).apply()
    }

    // --- Individual Device Metric Settings ---
    data class DeviceMetricSetting(
        val showTemperature: Boolean = true,
        val showHumidity: Boolean = true,
        val showPressure: Boolean = false,
        val showDoor: Boolean = true,
        val showLeak: Boolean = true,
        val showBattery: Boolean = true
    )

    fun getDeviceMetricSetting(deviceId: String): DeviceMetricSetting {
        val temp = prefs.getBoolean("dev_${deviceId}_temp", true)
        val hum = prefs.getBoolean("dev_${deviceId}_hum", true)
        val press = prefs.getBoolean("dev_${deviceId}_press", false)
        val door = prefs.getBoolean("dev_${deviceId}_door", true)
        val leak = prefs.getBoolean("dev_${deviceId}_leak", true)
        val bat = prefs.getBoolean("dev_${deviceId}_bat", true)
        return DeviceMetricSetting(temp, hum, press, door, leak, bat)
    }

    fun saveDeviceMetricSetting(deviceId: String, setting: DeviceMetricSetting) {
        prefs.edit()
            .putBoolean("dev_${deviceId}_temp", setting.showTemperature)
            .putBoolean("dev_${deviceId}_hum", setting.showHumidity)
            .putBoolean("dev_${deviceId}_press", setting.showPressure)
            .putBoolean("dev_${deviceId}_door", setting.showDoor)
            .putBoolean("dev_${deviceId}_leak", setting.showLeak)
            .putBoolean("dev_${deviceId}_bat", setting.showBattery)
            .apply()
    }

    // --- User Info Cache ---
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
            it.getTemperature() != null || it.getHumidity() != null || it.getBattery() != null
        }
    }

    fun getDevicesForRoom(roomId: String?): List<Device> {
        val all = getAllRelevantDevices()
        if (roomId.isNullOrBlank() || roomId == "ALL") return all
        val room = getAvailableRooms().firstOrNull { it.id == roomId }
        return all.filter { dev -> dev.room == roomId || room?.devices?.contains(dev.id) == true }
    }

    // --- 4x2 Widget Data per Widget Instance ---
    fun get4x2SensorData(appWidgetId: Int = 0): AggregatedSensorData {
        val cached = getCachedUserInfo() ?: return AggregatedSensorData(
            householdName = "Климат",
            primaryTemperature = 22.5,
            primaryHumidity = 48.0,
            primaryPressure = null,
            primaryRoomName = "Гостиная",
            totalSensorCount = 0,
            lastUpdatedTimestamp = lastSyncTimestamp
        )

        val config = if (appWidgetId > 0) getWidgetConfig(appWidgetId, "4x2") else null
        val selectedIds = config?.selectedDeviceIds ?: emptyList()
        val allDevices = cached.devices
        val rooms = cached.rooms

        // Devices strictly for THIS widget instance
        val targetDevices = if (selectedIds.isNotEmpty()) {
            allDevices.filter { selectedIds.contains(it.id) }
        } else {
            allDevices.filter { it.isClimateSensor() || it.isOpenSensor() }
        }

        val primaryDev = targetDevices.firstOrNull { it.isClimateSensor() || it.getTemperature() != null }
            ?: targetDevices.firstOrNull()
        val primaryRoom = rooms.firstOrNull { it.id == primaryDev?.room || it.devices.contains(primaryDev?.id) }
            ?: rooms.firstOrNull()

        val primarySetting = primaryDev?.let { getDeviceMetricSetting(it.id) } ?: DeviceMetricSetting()
        val primaryTemp = if (primarySetting.showTemperature) primaryDev?.getTemperature() else null
        val primaryHum = if (primarySetting.showHumidity) primaryDev?.getHumidity() else null
        val primaryPress = if (primarySetting.showPressure) primaryDev?.getPressure() else null

        val primaryRoomName = config?.customTitle?.ifBlank { null }
            ?: primaryRoom?.name ?: primaryDev?.name ?: "Климат"

        // Secondary cards (from other devices selected for this widget)
        val otherDevs = targetDevices.filter { it.id != primaryDev?.id }
        val roomSummaries = mutableListOf<RoomClimateSummary>()

        for (dev in otherDevs) {
            val devSetting = getDeviceMetricSetting(dev.id)
            val t = if (devSetting.showTemperature) dev.getTemperature() else null
            val h = if (devSetting.showHumidity) dev.getHumidity() else null
            val p = if (devSetting.showPressure) dev.getPressure() else null

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

        // Door status (only from devices selected for this widget)
        var anyDoorOpen = false
        var openDoorName: String? = null
        for (dev in targetDevices) {
            val devSetting = getDeviceMetricSetting(dev.id)
            if (devSetting.showDoor && dev.isDoorOrWindowOpen() == true) {
                anyDoorOpen = true
                openDoorName = dev.name
                break
            }
        }

        // Lowest battery (only from devices selected for this widget)
        val lowestBattery = targetDevices.filter { getDeviceMetricSetting(it.id).showBattery }
            .mapNotNull { it.getBattery() }.minOrNull()

        return AggregatedSensorData(
            householdName = cached.households.firstOrNull()?.name ?: "Датчики",
            primaryTemperature = primaryTemp,
            primaryHumidity = primaryHum,
            primaryPressure = primaryPress,
            primaryRoomName = primaryRoomName,
            roomSensors = roomSummaries,
            isAnyDoorOpen = anyDoorOpen,
            openDoorDeviceName = openDoorName,
            lowestBatteryLevel = lowestBattery,
            totalSensorCount = targetDevices.size,
            lastUpdatedTimestamp = lastSyncTimestamp
        )
    }

    // --- 4x1 Bar Widget Data per Widget Instance ---
    data class BarSlotItem(
        val roomName: String,
        val mainText: String,
        val secondaryText: String? = null,
        val isDoor: Boolean = false,
        val isDoorAlert: Boolean = false
    )

    fun get4x1BarSlots(appWidgetId: Int = 0): List<BarSlotItem> {
        val cached = getCachedUserInfo() ?: return listOf(
            BarSlotItem("Гостиная", "22.5°", "48%"),
            BarSlotItem("Спальня", "21.0°", null),
            BarSlotItem("", "🚪 ОК", isDoor = true)
        )

        val config = if (appWidgetId > 0) getWidgetConfig(appWidgetId, "4x1") else null
        val selectedIds = config?.selectedDeviceIds ?: emptyList()
        val allDevices = cached.devices
        val rooms = cached.rooms

        val targetDevices = if (selectedIds.isNotEmpty()) {
            allDevices.filter { selectedIds.contains(it.id) }
        } else {
            allDevices.filter { it.isClimateSensor() || it.isOpenSensor() }
        }

        val result = mutableListOf<BarSlotItem>()

        for (dev in targetDevices) {
            val devSetting = getDeviceMetricSetting(dev.id)
            val rName = rooms.firstOrNull { it.id == dev.room || it.devices.contains(dev.id) }?.name ?: dev.name

            if (dev.isOpenSensor() && devSetting.showDoor) {
                val isOpen = dev.isDoorOrWindowOpen() == true
                result.add(BarSlotItem(rName, if (isOpen) "🚪 ОТКРЫТО" else "🚪 ОК", isDoor = true, isDoorAlert = isOpen))
            } else {
                val t = if (devSetting.showTemperature) dev.getTemperature() else null
                val h = if (devSetting.showHumidity) dev.getHumidity() else null

                if (t != null || h != null) {
                    val tStr = t?.let { String.format(Locale.US, "%.1f°", it) } ?: ""
                    val hStr = h?.let { String.format(Locale.US, "%.0f%%", it) } ?: ""
                    result.add(BarSlotItem(rName, tStr, hStr.ifEmpty { null }))
                }
            }
            if (result.size >= 3) break
        }

        if (result.isEmpty()) {
            result.add(BarSlotItem("Климат", "22.5°", "48%"))
        }

        return result
    }

    // --- 2x1 Compact Data per Widget Instance ---
    data class Compact2x1Data(
        val title: String,
        val tempText: String,
        val humidityText: String?
    )

    fun get2x1Data(appWidgetId: Int = 0): Compact2x1Data {
        val cached = getCachedUserInfo() ?: return Compact2x1Data("Гостиная", "22.5°", "48%")

        val config = if (appWidgetId > 0) getWidgetConfig(appWidgetId, "2x1") else null
        val selectedIds = config?.selectedDeviceIds ?: emptyList()
        val allDevices = cached.devices
        val rooms = cached.rooms

        val dev = if (selectedIds.isNotEmpty()) {
            allDevices.firstOrNull { selectedIds.contains(it.id) }
        } else {
            allDevices.firstOrNull { it.getTemperature() != null || it.isClimateSensor() }
        }

        val title = config?.customTitle?.ifBlank { null }
            ?: dev?.let { d -> rooms.firstOrNull { it.id == d.room || it.devices.contains(d.id) }?.name ?: d.name }
            ?: "Климат"

        val setting = dev?.let { getDeviceMetricSetting(it.id) } ?: DeviceMetricSetting()
        val temp = if (setting.showTemperature) dev?.getTemperature() else null
        val hum = if (setting.showHumidity) dev?.getHumidity() else null

        val tempStr = temp?.let { String.format(Locale.US, "%.1f°", it) } ?: "--°"
        val humStr = hum?.let { String.format(Locale.US, "%.0f%%", it) }

        return Compact2x1Data(title, tempStr, humStr)
    }

    // --- 2x2 Compact Data per Widget Instance ---
    data class Compact2x2Data(
        val title: String,
        val tempText: String,
        val humidityText: String?
    )

    fun get2x2Data(appWidgetId: Int = 0): Compact2x2Data {
        val d = get2x1Data(appWidgetId)
        return Compact2x2Data(d.title, d.tempText, d.humidityText)
    }

    // Backward compatibility
    fun getAggregatedSensorData(): AggregatedSensorData = get4x2SensorData(0)
    fun getAvailableDevices(): List<Device> = getAllRelevantDevices()
}
