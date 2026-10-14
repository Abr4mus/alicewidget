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
        const val THEME_YANDEX_WHITE = "YANDEX_WHITE"
        const val THEME_FLUENT_PASTEL = "FLUENT_PASTEL"
        const val THEME_CYBERPUNK = "CYBERPUNK"
        const val THEME_NORDIC_AURORA = "NORDIC_AURORA"
        // Deprecated aliases for backward compatibility with previous installs
        const val THEME_ANDROID = "ANDROID"
        const val THEME_NOTHING = "NOTHING"
        private const val KEY_DEFAULT_THEME = "widget_default_theme"

        // Active widget tracking
        private const val KEY_CONFIGURED_WIDGET_IDS = "configured_widget_ids_set"
        private const val KEY_REMOVED_WIDGET_IDS = "removed_widget_ids_set"
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

        // Un-remove if previously removed
        setWidgetRemoved(config.appWidgetId, false)
    }

    fun getAllConfiguredWidgetIds(): List<Int> {
        val set = prefs.getStringSet(KEY_CONFIGURED_WIDGET_IDS, emptySet()) ?: emptySet()
        return set.mapNotNull { it.toIntOrNull() }
    }

    fun isWidgetRemoved(appWidgetId: Int): Boolean {
        val set = prefs.getStringSet(KEY_REMOVED_WIDGET_IDS, emptySet()) ?: emptySet()
        return set.contains(appWidgetId.toString())
    }

    fun setWidgetRemoved(appWidgetId: Int, removed: Boolean) {
        val currentSet = prefs.getStringSet(KEY_REMOVED_WIDGET_IDS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        if (removed) {
            currentSet.add(appWidgetId.toString())
        } else {
            currentSet.remove(appWidgetId.toString())
        }
        prefs.edit().putStringSet(KEY_REMOVED_WIDGET_IDS, currentSet).apply()
    }

    fun hasWidgetConfig(appWidgetId: Int): Boolean {
        return prefs.contains("w_${appWidgetId}_dev_ids")
    }

    fun deleteWidgetConfig(appWidgetId: Int) {
        setWidgetRemoved(appWidgetId, true)

        val editor = prefs.edit()
            .remove("w_${appWidgetId}_theme")
            .remove("w_${appWidgetId}_dev_ids")
            .remove("w_${appWidgetId}_title")

        for (key in prefs.all.keys) {
            if (key.startsWith("w_${appWidgetId}_dev_")) {
                editor.remove(key)
            }
        }
        editor.apply()

        val currentSet = prefs.getStringSet(KEY_CONFIGURED_WIDGET_IDS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        currentSet.remove(appWidgetId.toString())
        prefs.edit().putStringSet(KEY_CONFIGURED_WIDGET_IDS, currentSet).apply()
    }

    // --- Individual Device Metric Settings (Per-Widget Isolated) ---
    data class DeviceMetricSetting(
        val showTemperature: Boolean = true,
        val showHumidity: Boolean = true,
        val showPressure: Boolean = false,
        val showDoor: Boolean = true,
        val showLeak: Boolean = true,
        val showBattery: Boolean = true,
        val showFuel: Boolean = true,
        val showPower: Boolean = true
    )

    fun getDeviceMetricSetting(deviceId: String, appWidgetId: Int = 0): DeviceMetricSetting {
        val prefix = if (appWidgetId > 0 && prefs.contains("w_${appWidgetId}_dev_${deviceId}_temp")) {
            "w_${appWidgetId}_dev_${deviceId}_"
        } else {
            "dev_${deviceId}_"
        }
        val temp = prefs.getBoolean("${prefix}temp", true)
        val hum = prefs.getBoolean("${prefix}hum", true)
        val press = prefs.getBoolean("${prefix}press", false)
        val door = prefs.getBoolean("${prefix}door", true)
        val leak = prefs.getBoolean("${prefix}leak", true)
        val bat = prefs.getBoolean("${prefix}bat", true)
        val fuel = prefs.getBoolean("${prefix}fuel", true)
        val power = prefs.getBoolean("${prefix}power", true)
        return DeviceMetricSetting(temp, hum, press, door, leak, bat, fuel, power)
    }

    fun saveDeviceMetricSetting(deviceId: String, setting: DeviceMetricSetting, appWidgetId: Int = 0) {
        val editor = prefs.edit()
        val prefix = if (appWidgetId > 0) "w_${appWidgetId}_dev_${deviceId}_" else "dev_${deviceId}_"
        editor.putBoolean("${prefix}temp", setting.showTemperature)
            .putBoolean("${prefix}hum", setting.showHumidity)
            .putBoolean("${prefix}press", setting.showPressure)
            .putBoolean("${prefix}door", setting.showDoor)
            .putBoolean("${prefix}leak", setting.showLeak)
            .putBoolean("${prefix}bat", setting.showBattery)
            .putBoolean("${prefix}fuel", setting.showFuel)
            .putBoolean("${prefix}power", setting.showPower)
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
        return getCachedUserInfo()?.devices ?: emptyList()
    }

    fun getDevicesForRoom(roomId: String?): List<Device> {
        val all = getAllRelevantDevices()
        if (roomId.isNullOrBlank() || roomId == "ALL") return all
        val room = getAvailableRooms().firstOrNull { it.id == roomId }
        return all.filter { dev -> dev.room == roomId || room?.hasDevice(dev.id) == true }
    }

    // --- 4x2 Widget Data per Widget Instance ---
    fun get4x2SensorData(appWidgetId: Int = 0): AggregatedSensorData {
        val cached = getCachedUserInfo() ?: return AggregatedSensorData(
            primaryRoomName = "Умный дом",
            primaryState = com.alice.homewidget.model.DeviceWidgetState("default", "Климат", null, "climate", "22.5°", "Комфортно", "💧 48%", null, "🌡️", false, true, 22.5),
            secondaryDevices = emptyList(),
            lastUpdatedTimestamp = lastSyncTimestamp
        )

        val config = if (appWidgetId > 0) getWidgetConfig(appWidgetId, "4x2") else null
        val selectedIds = config?.selectedDeviceIds ?: emptyList()
        val allDevices = cached.devices ?: emptyList()
        val rooms = cached.rooms ?: emptyList()

        // 4x2: Up to 3 devices maximum!
        val targetDevices = if (selectedIds.isNotEmpty()) {
            allDevices.filter { selectedIds.contains(it.id) }.take(3)
        } else {
            // Brand new widget: default to 1 primary device only!
            val single = allDevices.firstOrNull { it.isClimateSensor() || it.hasTemperature() } ?: allDevices.firstOrNull()
            if (single != null) listOf(single) else emptyList()
        }

        val primaryDev = targetDevices.firstOrNull()
            ?: allDevices.firstOrNull()
            ?: return AggregatedSensorData(
                primaryRoomName = "Умный дом",
                primaryState = com.alice.homewidget.model.DeviceWidgetState("default", "Климат", null, "climate", "22.5°", "Комфортно", "💧 48%", null, "🌡️", false, true, 22.5),
                secondaryDevices = emptyList(),
                lastUpdatedTimestamp = lastSyncTimestamp
            )

        // Primary room: NEVER guess a random room if device has no room!
        val primaryRoom = if (!primaryDev.room.isNullOrBlank()) {
            rooms.firstOrNull { it.id == primaryDev.room || it.hasDevice(primaryDev.id) }
        } else null

        val primaryTitle = config?.customTitle?.ifBlank { null }
            ?: primaryRoom?.name
            ?: primaryDev.name
            ?: "Умный дом"

        val primaryState = primaryDev.toWidgetState(primaryRoom?.name)

        // Secondary cards (Device 2 and Device 3)
        val secondaryDevs = targetDevices.filter { it.id != primaryDev.id }.take(2)
        val secondaryStates = secondaryDevs.map { dev ->
            val devRoom = if (!dev.room.isNullOrBlank()) {
                rooms.firstOrNull { it.id == dev.room || it.hasDevice(dev.id) }
            } else null
            dev.toWidgetState(devRoom?.name)
        }

        return AggregatedSensorData(
            primaryRoomName = primaryTitle,
            primaryState = primaryState,
            secondaryDevices = secondaryStates,
            totalSensorCount = targetDevices.size,
            lastUpdatedTimestamp = lastSyncTimestamp
        )
    }

    // --- 4x1 Bar Widget Data per Widget Instance ---
    data class BarSlotItem(
        val title: String,
        val state: com.alice.homewidget.model.DeviceWidgetState
    ) {
        val roomName: String get() = title
        val mainText: String get() = state.mainText
        val secondaryText: String? get() = state.badge1 ?: state.subText
        val iconType: String get() = state.category
        val isDoor: Boolean get() = state.category == "door"
        val isDoorAlert: Boolean get() = state.isAlert
    }

    fun get4x1BarSlots(appWidgetId: Int = 0): List<BarSlotItem> {
        val cached = getCachedUserInfo() ?: return listOf(
            BarSlotItem("Гостиная", com.alice.homewidget.model.DeviceWidgetState("1", "Гостиная", "Гостиная", "climate", "22.5°", "Комфортно", "💧 48%", null, "🌡️", false, true, 22.5)),
            BarSlotItem("Спальня", com.alice.homewidget.model.DeviceWidgetState("2", "Спальня", "Спальня", "climate", "21.0°", "Комфортно", "💧 52%", null, "🌡️", false, true, 21.0))
        )

        val config = if (appWidgetId > 0) getWidgetConfig(appWidgetId, "4x1") else null
        val selectedIds = config?.selectedDeviceIds ?: emptyList()
        val allDevices = cached.devices ?: emptyList()
        val rooms = cached.rooms ?: emptyList()

        // 4x1: Up to 2 devices maximum!
        val targetDevices = if (selectedIds.isNotEmpty()) {
            allDevices.filter { selectedIds.contains(it.id) }.take(2)
        } else {
            val single = allDevices.firstOrNull { it.isClimateSensor() || it.hasTemperature() } ?: allDevices.firstOrNull()
            if (single != null) listOf(single) else emptyList()
        }

        val result = mutableListOf<BarSlotItem>()
        for (dev in targetDevices) {
            val r = if (!dev.room.isNullOrBlank()) rooms.firstOrNull { it.id == dev.room || it.hasDevice(dev.id) } else null
            val devTitle = r?.name ?: dev.name
            val st = dev.toWidgetState(r?.name)
            result.add(BarSlotItem(devTitle, st))
        }

        if (result.isEmpty()) {
            result.add(BarSlotItem("Климат", com.alice.homewidget.model.DeviceWidgetState("default", "Климат", null, "climate", "22.5°", "Комфортно", "💧 48%", null, "🌡️", false, true, 22.5)))
        }

        return result
    }

    // --- 2x1 Compact Data per Widget Instance ---
    data class Compact2x1Data(
        val title: String,
        val state: com.alice.homewidget.model.DeviceWidgetState
    ) {
        val tempText: String get() = state.mainText
        val humidityText: String? get() = state.badge1 ?: state.subText
    }

    fun get2x1Data(appWidgetId: Int = 0): Compact2x1Data {
        val cached = getCachedUserInfo() ?: return Compact2x1Data(
            "Гостиная",
            com.alice.homewidget.model.DeviceWidgetState("default", "Гостиная", "Гостиная", "climate", "22.5°", "Комфортно", "💧 48%", null, "🌡️", false, true, 22.5)
        )

        val config = if (appWidgetId > 0) getWidgetConfig(appWidgetId, "2x1") else null
        val selectedIds = config?.selectedDeviceIds ?: emptyList()
        val allDevices = cached.devices ?: emptyList()
        val rooms = cached.rooms ?: emptyList()

        // 2x1: Strictly 1 device!
        val dev = if (selectedIds.isNotEmpty()) {
            allDevices.firstOrNull { selectedIds.contains(it.id) } ?: allDevices.firstOrNull()
        } else {
            allDevices.firstOrNull { it.isClimateSensor() || it.hasTemperature() } ?: allDevices.firstOrNull()
        }

        if (dev == null) {
            return Compact2x1Data("Климат", com.alice.homewidget.model.DeviceWidgetState("default", "Климат", null, "climate", "22.5°", "Комфортно", "💧 48%", null, "🌡️", false, true, 22.5))
        }

        val room = if (!dev.room.isNullOrBlank()) rooms.firstOrNull { it.id == dev.room || it.hasDevice(dev.id) } else null
        val title = config?.customTitle?.ifBlank { null } ?: room?.name ?: dev.name
        val state = dev.toWidgetState(room?.name)

        return Compact2x1Data(title, state)
    }

    // --- 2x2 Compact Data per Widget Instance ---
    data class Compact2x2Data(
        val title: String,
        val state: com.alice.homewidget.model.DeviceWidgetState
    ) {
        val tempText: String get() = state.mainText
        val humidityText: String? get() = state.badge1 ?: state.subText
    }

    fun get2x2Data(appWidgetId: Int = 0): Compact2x2Data {
        val d = get2x1Data(appWidgetId)
        return Compact2x2Data(d.title, d.state)
    }

    // Backward compatibility
    fun getAggregatedSensorData(): AggregatedSensorData = get4x2SensorData(0)
    fun getAvailableDevices(): List<Device> = getAllRelevantDevices()
}
