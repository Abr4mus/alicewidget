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
        const val THEME_NOTHING = "NOTHING"
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

    fun hasWidgetConfig(appWidgetId: Int): Boolean {
        return prefs.contains("w_${appWidgetId}_dev_ids")
    }

    fun deleteWidgetConfig(appWidgetId: Int) {
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

        val primarySetting = primaryDev?.let { getDeviceMetricSetting(it.id, appWidgetId) } ?: DeviceMetricSetting()
        val primaryTemp = if (primarySetting.showTemperature && primaryDev?.hasTemperature() == true) primaryDev.getTemperature() else null
        val primaryHum = if (primarySetting.showHumidity && primaryDev?.hasHumidity() == true) primaryDev.getHumidity() else null
        val primaryPress = if (primarySetting.showPressure && primaryDev?.hasPressure() == true) primaryDev.getPressure() else null

        val primaryRoomName = config?.customTitle?.ifBlank { null }
            ?: primaryRoom?.name ?: primaryDev?.name ?: "Климат"

        // Secondary cards (from other devices selected for this widget)
        val otherDevs = targetDevices.filter { it.id != primaryDev?.id }
        val roomSummaries = mutableListOf<RoomClimateSummary>()

        for (dev in otherDevs) {
            val devSetting = getDeviceMetricSetting(dev.id, appWidgetId)
            val t = if (devSetting.showTemperature && dev.hasTemperature()) dev.getTemperature() else null
            val h = if (devSetting.showHumidity && dev.hasHumidity()) dev.getHumidity() else null
            val p = if (devSetting.showPressure && dev.hasPressure()) dev.getPressure() else null
            val bat = if (devSetting.showBattery && dev.hasBattery()) dev.getBattery() else null
            val fuel = if (devSetting.showFuel && dev.hasWaterOrFuel()) dev.getWaterOrFuelLevel() else null

            if (t != null || h != null || p != null || bat != null || fuel != null) {
                val rName = rooms.firstOrNull { it.id == dev.room || it.devices.contains(dev.id) }?.name ?: dev.name
                val extra = if (fuel != null) "Уровень: ${String.format(Locale.US, "%.0f%%", fuel)}" else null
                roomSummaries.add(
                    RoomClimateSummary(
                        roomId = dev.room ?: dev.id,
                        roomName = rName,
                        temperature = t,
                        humidity = h,
                        pressure = p,
                        battery = bat,
                        extraInfo = extra
                    )
                )
                if (roomSummaries.size >= 2) break
            }
        }

        // Door status (only from devices selected for this widget)
        var anyDoorOpen = false
        var openDoorName: String? = null
        for (dev in targetDevices) {
            val devSetting = getDeviceMetricSetting(dev.id, appWidgetId)
            if (devSetting.showDoor && dev.isDoorOrWindowOpen() == true) {
                anyDoorOpen = true
                openDoorName = dev.name
                break
            }
        }

        // Lowest battery (only from devices selected for this widget)
        val lowestBattery = targetDevices.filter { getDeviceMetricSetting(it.id, appWidgetId).showBattery }
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
        val iconType: String = "temp", // "temp", "hum", "door", "fuel", "bat", "power"
        val isDoor: Boolean = false,
        val isDoorAlert: Boolean = false
    )

    fun get4x1BarSlots(appWidgetId: Int = 0): List<BarSlotItem> {
        val cached = getCachedUserInfo() ?: return listOf(
            BarSlotItem("Гостиная", "22.5°", "48%", iconType = "temp"),
            BarSlotItem("Спальня", "21.0°", "52%", iconType = "temp"),
            BarSlotItem("Безопасность", "Закрыто", null, iconType = "door", isDoor = true)
        )

        val config = if (appWidgetId > 0) getWidgetConfig(appWidgetId, "4x1") else null
        val selectedIds = config?.selectedDeviceIds ?: emptyList()
        val allDevices = cached.devices
        val rooms = cached.rooms

        val targetDevices = if (selectedIds.isNotEmpty()) {
            allDevices.filter { selectedIds.contains(it.id) }
        } else {
            allDevices.filter { it.isClimateSensor() || it.isOpenSensor() || it.hasTemperature() }
        }

        val result = mutableListOf<BarSlotItem>()

        for (dev in targetDevices) {
            val devSetting = getDeviceMetricSetting(dev.id, appWidgetId)
            val rName = rooms.firstOrNull { it.id == dev.room || it.devices.contains(dev.id) }?.name ?: dev.name

            if (dev.hasDoorSensor() && devSetting.showDoor) {
                val isOpen = dev.isDoorOrWindowOpen() == true
                result.add(BarSlotItem(rName, if (isOpen) "ОТКРЫТО!" else "Закрыто", iconType = "door", isDoor = true, isDoorAlert = isOpen))
            } else if (dev.hasWaterOrFuel() && devSetting.showFuel) {
                val f = dev.getWaterOrFuelLevel()?.let { String.format(Locale.US, "%.0f%%", it) } ?: ""
                result.add(BarSlotItem(rName, f, "Уровень", iconType = "fuel"))
            } else if (dev.hasPower() && devSetting.showPower) {
                val p = dev.getPower()?.let { String.format(Locale.US, "%.0f Вт", it) } ?: ""
                result.add(BarSlotItem(rName, p, null, iconType = "power"))
            } else {
                val t = if (devSetting.showTemperature && dev.hasTemperature()) dev.getTemperature() else null
                val h = if (devSetting.showHumidity && dev.hasHumidity()) dev.getHumidity() else null

                if (t != null || h != null) {
                    val tStr = t?.let { String.format(Locale.US, "%.1f°", it) } ?: ""
                    val hStr = h?.let { String.format(Locale.US, "%.0f%%", it) } ?: ""
                    result.add(BarSlotItem(rName, tStr, hStr.ifEmpty { null }, iconType = "temp"))
                } else if (dev.hasBattery() && devSetting.showBattery) {
                    val b = dev.getBattery()?.let { String.format(Locale.US, "%.0f%%", it) } ?: ""
                    result.add(BarSlotItem(rName, b, null, iconType = "bat"))
                }
            }
            if (result.size >= 3) break
        }

        if (result.isEmpty()) {
            result.add(BarSlotItem("Климат", "22.5°", "48%", iconType = "temp"))
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

        val setting = dev?.let { getDeviceMetricSetting(it.id, appWidgetId) } ?: DeviceMetricSetting()

        val tempStr: String
        val humStr: String?

        when {
            dev != null && dev.hasDoorSensor() && setting.showDoor -> {
                tempStr = if (dev.isDoorOrWindowOpen() == true) "ОТКРЫТО!" else "Закрыто"
                humStr = null
            }
            dev != null && dev.hasWaterOrFuel() && setting.showFuel -> {
                tempStr = dev.getWaterOrFuelLevel()?.let { String.format(Locale.US, "%.0f%%", it) } ?: "--%"
                humStr = "Уровень"
            }
            dev != null && dev.hasPower() && setting.showPower -> {
                tempStr = dev.getPower()?.let { String.format(Locale.US, "%.0f Вт", it) } ?: "-- Вт"
                humStr = null
            }
            dev != null && dev.hasTemperature() && setting.showTemperature -> {
                val temp = dev.getTemperature()
                tempStr = temp?.let { String.format(Locale.US, "%.1f°", it) } ?: "--°"
                val hum = if (setting.showHumidity && dev.hasHumidity()) dev.getHumidity() else null
                humStr = hum?.let { String.format(Locale.US, "%.0f%%", it) }
            }
            dev != null && dev.hasBattery() && setting.showBattery -> {
                tempStr = dev.getBattery()?.let { String.format(Locale.US, "%.0f%%", it) } ?: "--%"
                humStr = "Батарея"
            }
            else -> {
                val temp = if (setting.showTemperature && dev?.hasTemperature() == true) dev.getTemperature() else null
                val hum = if (setting.showHumidity && dev?.hasHumidity() == true) dev.getHumidity() else null
                tempStr = temp?.let { String.format(Locale.US, "%.1f°", it) } ?: "--°"
                humStr = hum?.let { String.format(Locale.US, "%.0f%%", it) }
            }
        }

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
