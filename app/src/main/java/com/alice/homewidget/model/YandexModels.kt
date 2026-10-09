package com.alice.homewidget.model

import com.google.gson.annotations.SerializedName

data class UserInfoResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("request_id") val requestId: String? = null,
    @SerializedName("households") val households: List<Household> = emptyList(),
    @SerializedName("rooms") val rooms: List<Room> = emptyList(),
    @SerializedName("devices") val devices: List<Device> = emptyList()
)

data class Household(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String
)

data class Room(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("household_id") val householdId: String? = null,
    @SerializedName("devices") val devices: List<String> = emptyList()
)

data class Device(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("room") val room: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("properties") val properties: List<DeviceProperty> = emptyList()
) {
    fun getFloatProperty(instance: String): Double? {
        val prop = properties.firstOrNull {
            it.type == "devices.properties.float" &&
            (it.state?.instance == instance || it.parameters?.instance == instance)
        }
        return prop?.state?.value?.let {
            when (it) {
                is Number -> it.toDouble()
                is String -> it.toDoubleOrNull()
                else -> null
            }
        }
    }

    fun getEventProperty(instance: String): String? {
        val prop = properties.firstOrNull {
            it.type == "devices.properties.event" &&
            (it.state?.instance == instance || it.parameters?.instance == instance)
        }
        return prop?.state?.value?.toString()
    }

    fun getTemperature(): Double? = getFloatProperty("temperature")
    fun getHumidity(): Double? = getFloatProperty("humidity")
    fun getPressure(): Double? = getFloatProperty("pressure")
    fun getBattery(): Double? = getFloatProperty("battery_level")

    fun isDoorOrWindowOpen(): Boolean? {
        val openState = getEventProperty("open") ?: return null
        return openState == "opened" || openState == "open"
    }

    fun hasWaterLeak(): Boolean? {
        val leakState = getEventProperty("water_leak") ?: return null
        return leakState == "leak"
    }

    fun isClimateSensor(): Boolean {
        return type.contains("sensor.climate") ||
               type.contains("climate") ||
               getTemperature() != null ||
               getHumidity() != null
    }

    fun isOpenSensor(): Boolean {
        return type.contains("sensor.open") || getEventProperty("open") != null
    }

    fun isLeakSensor(): Boolean {
        return type.contains("sensor.water_leak") || getEventProperty("water_leak") != null
    }
}

data class DeviceProperty(
    @SerializedName("type") val type: String? = null,
    @SerializedName("state") val state: PropertyState? = null,
    @SerializedName("parameters") val parameters: PropertyParameters? = null
)

data class PropertyState(
    @SerializedName("instance") val instance: String? = null,
    @SerializedName("value") val value: Any? = null
)

data class PropertyParameters(
    @SerializedName("instance") val instance: String? = null,
    @SerializedName("unit") val unit: String? = null
)

data class AggregatedSensorData(
    val householdName: String = "Мой дом",
    val primaryTemperature: Double? = null,
    val primaryHumidity: Double? = null,
    val primaryPressure: Double? = null,
    val primaryRoomName: String = "Гостиная",
    val roomSensors: List<RoomClimateSummary> = emptyList(),
    val isAnyDoorOpen: Boolean = false,
    val openDoorDeviceName: String? = null,
    val isAnyLeakDetected: Boolean = false,
    val leakDeviceName: String? = null,
    val lowestBatteryLevel: Double? = null,
    val totalSensorCount: Int = 0,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

data class RoomClimateSummary(
    val roomId: String,
    val roomName: String,
    val temperature: Double?,
    val humidity: Double?,
    val pressure: Double?
)
