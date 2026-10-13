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
    @SerializedName("properties") val properties: List<DeviceProperty> = emptyList(),
    @SerializedName("capabilities") val capabilities: List<DeviceCapability> = emptyList()
) {
    fun hasFloatProperty(instance: String): Boolean {
        return properties.any {
            it.type == "devices.properties.float" &&
            (it.state?.instance == instance || it.parameters?.instance == instance)
        }
    }

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

    // Physical Sensor Values
    fun getTemperature(): Double? = getFloatProperty("temperature")
    fun getHumidity(): Double? = if (hasFloatProperty("humidity")) getFloatProperty("humidity") else null
    fun getPressure(): Double? = if (hasFloatProperty("pressure")) getFloatProperty("pressure") else null
    fun getBattery(): Double? = getFloatProperty("battery_level")
    fun getPower(): Double? = getFloatProperty("power")
    fun getVoltage(): Double? = getFloatProperty("voltage")
    fun getWaterOrFuelLevel(): Double? = getFloatProperty("water_level") ?: getFloatProperty("fuel_level")

    // Availability checks (strictly physical, never guessed by device type)
    fun hasTemperature(): Boolean = hasFloatProperty("temperature") && getTemperature() != null
    fun hasHumidity(): Boolean = hasFloatProperty("humidity") && getHumidity() != null
    fun hasPressure(): Boolean = hasFloatProperty("pressure") && getPressure() != null
    fun hasBattery(): Boolean = hasFloatProperty("battery_level") && getBattery() != null
    fun hasPower(): Boolean = hasFloatProperty("power") && getPower() != null
    fun hasWaterOrFuel(): Boolean = (hasFloatProperty("water_level") || hasFloatProperty("fuel_level")) && getWaterOrFuelLevel() != null

    // Door / Contact sensors
    fun hasDoorSensor(): Boolean = properties.any { it.state?.instance == "open" || it.parameters?.instance == "open" } || type.contains("sensor.open")
    fun isDoorOrWindowOpen(): Boolean? {
        val openState = getEventProperty("open") ?: return null
        return openState == "opened" || openState == "open"
    }

    // Water Leak sensors
    fun hasLeakSensor(): Boolean = properties.any { it.state?.instance == "water_leak" || it.parameters?.instance == "water_leak" } || type.contains("sensor.water_leak")
    fun hasWaterLeak(): Boolean? {
        val leakState = getEventProperty("water_leak") ?: return null
        return leakState == "leak"
    }

    // On/Off State (Sockets, Switches, Lights, Auto)
    fun isOnOffDevice(): Boolean = capabilities.any { it.type == "devices.capabilities.on_off" }
    fun isTurnedOn(): Boolean? {
        val cap = capabilities.firstOrNull { it.type == "devices.capabilities.on_off" }
        return (cap?.state?.value as? Boolean)
    }

    fun isClimateSensor(): Boolean {
        return (type.contains("sensor.climate") || type.contains("sensor.temp")) && (hasTemperature() || hasHumidity())
    }

    fun isOpenSensor(): Boolean = hasDoorSensor()
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

data class DeviceCapability(
    @SerializedName("type") val type: String? = null,
    @SerializedName("state") val state: CapabilityState? = null,
    @SerializedName("parameters") val parameters: Any? = null
)

data class CapabilityState(
    @SerializedName("instance") val instance: String? = null,
    @SerializedName("value") val value: Any? = null
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
    val pressure: Double?,
    val battery: Double? = null,
    val extraInfo: String? = null
)
