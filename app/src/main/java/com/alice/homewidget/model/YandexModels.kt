package com.alice.homewidget.model

import com.google.gson.annotations.SerializedName

data class UserInfoResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("request_id") val requestId: String? = null,
    @SerializedName("households") val households: List<Household>? = emptyList(),
    @SerializedName("rooms") val rooms: List<Room>? = emptyList(),
    @SerializedName("devices") val devices: List<Device>? = emptyList()
)

data class Household(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = ""
)

data class Room(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("household_id") val householdId: String? = null,
    @SerializedName("devices") val devices: List<String>? = emptyList()
) {
    fun hasDevice(devId: String?): Boolean = !devId.isNullOrBlank() && devices?.contains(devId) == true
}

data class Device(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("type") val type: String? = null,
    @SerializedName("room") val room: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("properties") val properties: List<DeviceProperty>? = emptyList(),
    @SerializedName("capabilities") val capabilities: List<DeviceCapability>? = emptyList()
) {
    fun hasFloatProperty(instance: String): Boolean {
        return properties?.any {
            it.type == "devices.properties.float" &&
            (it.state?.instance == instance || it.parameters?.instance == instance)
        } ?: false
    }

    fun getFloatProperty(instance: String): Double? {
        val prop = properties?.firstOrNull {
            it.type == "devices.properties.float" &&
            (it.state?.instance == instance || it.parameters?.instance == instance)
        } ?: return null
        return prop.state?.value?.let {
            when (it) {
                is Number -> it.toDouble()
                is String -> it.toDoubleOrNull()
                else -> null
            }
        }
    }

    fun getEventProperty(instance: String): String? {
        val prop = properties?.firstOrNull {
            it.type == "devices.properties.event" &&
            (it.state?.instance == instance || it.parameters?.instance == instance)
        } ?: return null
        return prop.state?.value?.toString()
    }

    // Physical Sensor Values
    fun getTemperature(): Double? = getFloatProperty("temperature")
    fun getHumidity(): Double? = if (hasFloatProperty("humidity")) getFloatProperty("humidity") else null
    fun getPressure(): Double? = if (hasFloatProperty("pressure")) getFloatProperty("pressure") else null
    fun getBattery(): Double? = getFloatProperty("battery_level")
    fun getPower(): Double? = getFloatProperty("power")
    fun getVoltage(): Double? = getFloatProperty("voltage")
    fun getWaterOrFuelLevel(): Double? = getFloatProperty("water_level") ?: getFloatProperty("fuel_level") ?: getFloatProperty("fuel")

    // Availability checks (strictly physical, never guessed by device type)
    fun hasTemperature(): Boolean = hasFloatProperty("temperature") && getTemperature() != null
    fun hasHumidity(): Boolean = hasFloatProperty("humidity") && getHumidity() != null
    fun hasPressure(): Boolean = hasFloatProperty("pressure") && getPressure() != null
    fun hasBattery(): Boolean = hasFloatProperty("battery_level") && getBattery() != null
    fun hasPower(): Boolean = hasFloatProperty("power") && getPower() != null
    fun hasVoltage(): Boolean = hasFloatProperty("voltage") && getVoltage() != null
    fun hasWaterOrFuel(): Boolean = (hasFloatProperty("water_level") || hasFloatProperty("fuel_level") || hasFloatProperty("fuel")) && getWaterOrFuelLevel() != null

    // Door / Contact sensors
    fun hasDoorSensor(): Boolean = (properties?.any { it.state?.instance == "open" || it.parameters?.instance == "open" } == true) || (type?.contains("sensor.open") == true)
    fun isDoorOrWindowOpen(): Boolean? {
        val openState = getEventProperty("open") ?: return null
        return openState == "opened" || openState == "open"
    }

    // Water Leak sensors
    fun hasLeakSensor(): Boolean = (properties?.any { it.state?.instance == "water_leak" || it.parameters?.instance == "water_leak" } == true) || (type?.contains("sensor.water_leak") == true)
    fun hasWaterLeak(): Boolean? {
        val leakState = getEventProperty("water_leak") ?: return null
        return leakState == "leak"
    }

    // On/Off State (Sockets, Switches, Lights, Auto)
    fun isOnOffDevice(): Boolean = capabilities?.any { it.type == "devices.capabilities.on_off" } ?: false
    fun isTurnedOn(): Boolean? {
        val cap = capabilities?.firstOrNull { it.type == "devices.capabilities.on_off" }
        return (cap?.state?.value as? Boolean)
    }

    fun isClimateSensor(): Boolean {
        return !isVehicle() && !isKettle() && !isTv() && !isAc() && !isVacuum() &&
               (type?.contains("sensor.climate") == true || type?.contains("sensor.temp") == true || (hasTemperature() && hasHumidity()))
    }

    fun isOpenSensor(): Boolean = hasDoorSensor()

    fun isVehicle(): Boolean {
        val t = type?.lowercase() ?: ""
        val n = name.lowercase()
        if (t.contains("car") || t.contains("auto") || t.contains("vehicle")) return true
        val keywords = listOf(
            "авто", "машин", "car", "starline", "pandora", "старлайн", "пандора", "сигнализац", "сигналк",
            "лада", "веста", "гранта", "нива", "bmw", "mercedes", "audi", "haval", "geely", "chery",
            "omoda", "exeed", "tank", "monjaro", "changan", "hyundai", "kia", "toyota", "vw", "volkswagen",
            "skoda", "mazda", "ford", "nissan", "lexus", "subaru", "honda", "renault", "рено", "киа",
            "хендай", "хёндай", "тойота", "шкода", "фольксваген", "бмв", "мерседес", "ауди"
        )
        if (keywords.any { n.contains(it) }) return true
        return (hasWaterOrFuel() && (hasBattery() || hasVoltage() || isOnOffDevice()))
    }

    fun isAc(): Boolean {
        val t = type?.lowercase() ?: ""
        val n = name.lowercase()
        if (t.contains("sensor")) return false
        if (t == "devices.types.ac" || t == "devices.types.thermostat" || t == "devices.types.thermostat.ac" ||
            t.endsWith(".ac") || t.contains("air_conditioner")) return true
        val keywords = listOf("кондиционер", "кондёр", "сплит", "тепловой насос")
        return keywords.any { n.contains(it) }
    }

    fun isExcludedFromWidget(): Boolean {
        val t = type?.lowercase() ?: ""
        val n = name.lowercase()
        // Hubs, Gateways, Bridges
        if (t.contains("hub") || t.contains("gateway") || t.contains("bridge")) return true
        if (n.contains("хаб") || n.contains("шлюз")) return true

        // Yandex Stations and Smart Speakers
        if (t.contains("speaker") || t.contains("yandexstation") || t.contains("smart_speaker")) return true
        if (n.contains("станция") || n.contains("колонка") || n.contains("алиса")) return true

        // Remote controls / IR Remotes
        if (t.contains("remote_control") || t.contains("remote")) return true
        if (n.contains("пульт") && !isTv() && !isVacuum() && !isAc()) return true

        return false
    }

    fun isLedStrip(): Boolean {
        val t = type?.lowercase() ?: ""
        val n = name.lowercase()
        return t.contains("strip") || t.contains("led") ||
               n.contains("лента") || n.contains("подсветк") || n.contains("led") || n.contains("strip") || n.contains("диодн")
    }

    fun isLightBulb(): Boolean {
        val t = type?.lowercase() ?: ""
        val n = name.lowercase()
        return (t.contains("light") || n.contains("свет") || n.contains("ламп") || n.contains("люстр") || n.contains("бра") || n.contains("торшер")) && !isLedStrip()
    }

    fun isVacuum(): Boolean {
        val t = type?.lowercase() ?: ""
        val n = name.lowercase()
        if (t.contains("vacuum") || t.contains("cleaner") || t.contains("robot")) return true
        val keywords = listOf("пылесос", "робот-пылесос", "робот", "уборка", "vacuum", "dreame", "roborock", "xiaomi vacuum", "irobot")
        return keywords.any { n.contains(it) }
    }

    fun isKettle(): Boolean {
        val t = type?.lowercase() ?: ""
        val n = name.lowercase()
        return t.contains("kettle") || t.contains("cooking.kettle") || n.contains("чайник") || n.contains("kettle")
    }

    fun isTv(): Boolean {
        val t = type?.lowercase() ?: ""
        val n = name.lowercase()
        return t.contains("media.tv") || t.contains("media.receiver") || t.contains("tv") || n.contains("тв") || n.contains("телевизор") || n.contains("tv")
    }

    fun isLightingOrSwitch(): Boolean {
        val t = type?.lowercase() ?: ""
        val n = name.lowercase()
        return t.contains("light") || t.contains("switch") || t.contains("socket") || t.contains("relay") ||
               (isOnOffDevice() && !isVehicle() && !isKettle() && !isTv() && !isAc() && !isVacuum() && !isClimateSensor())
    }

    fun getTargetTemperature(): Double? {
        val cap = capabilities?.firstOrNull {
            it.type == "devices.capabilities.range" &&
            (it.state?.instance == "temperature" || it.parameters?.toString()?.contains("temperature") == true)
        } ?: return null
        return when (val v = cap.state?.value) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull()
            else -> null
        }
    }

    fun getThermostatMode(): String? {
        val cap = capabilities?.firstOrNull {
            it.type == "devices.capabilities.mode" &&
            (it.state?.instance == "thermostat" || it.state?.instance == "work_speed")
        }
        val modeVal = cap?.state?.value?.toString()?.lowercase() ?: return null
        return when (modeVal) {
            "cool" -> "Охлаждение"
            "heat" -> "Обогрев"
            "auto" -> "Авто"
            "dry" -> "Осушение"
            "fan_only" -> "Вентиляция"
            "quiet" -> "Тихий"
            "turbo", "max" -> "Турбо"
            else -> modeVal.replaceFirstChar { it.uppercase() }
        }
    }

    fun getVehicleFuel(): Double? = getWaterOrFuelLevel()

    fun getVehicleBatteryOrVoltage(): String? {
        val v = getVoltage()
        if (v != null) return String.format(java.util.Locale.US, "%.1f В", v)
        val b = getBattery()
        if (b != null) return String.format(java.util.Locale.US, "%.0f%%", b)
        return null
    }

    fun isVehicleRunning(): Boolean {
        if (isTurnedOn() == true) return true
        val engCap = capabilities?.firstOrNull {
            it.state?.instance == "engine" || it.state?.instance == "ignition" ||
            it.parameters?.toString()?.contains("engine") == true
        }
        if (engCap?.state?.value == true || engCap?.state?.value?.toString()?.lowercase() == "on") return true
        val engProp = properties?.firstOrNull {
            it.state?.instance == "engine" || it.state?.instance == "ignition"
        }
        val engVal = engProp?.state?.value?.toString()?.lowercase()
        if (engVal == "running" || engVal == "on" || engVal == "started" || engVal == "заведён" || engVal == "заведен") return true
        return false
    }

    fun getVehicleSecurityState(): String {
        val sec = getEventProperty("security")
        if (sec != null) {
            val s = sec.lowercase()
            return if (s.contains("arm") || s.contains("lock") || s.contains("охран") || s.contains("закр")) "На охране" else "Снят с охраны"
        }
        val lock = getEventProperty("lock")
        if (lock != null) {
            val l = lock.lowercase()
            return if (l == "locked" || l == "lock" || l.contains("закр")) "Закрыт" else "Открыт"
        }
        val op = isDoorOrWindowOpen()
        if (op != null) {
            return if (op) "Открыт" else "Закрыт"
        }
        return "Закрыт"
    }

    fun getKettleState(): String {
        if (state == "offline" || state == "not_ready") return "Снят с подставки"
        val isOn = isTurnedOn()
        val temp = getTemperature()
        if (isOn == true) {
            if (temp != null && temp >= 98.0) return "Закипел"
            val mode = capabilities?.firstOrNull { it.type == "devices.capabilities.mode" }?.state?.value?.toString()
            if (mode == "heat" || mode == "keep_warm") return "На подогреве"
            return "Кипятится"
        } else {
            return "Не греет"
        }
    }

    fun toWidgetState(
        roomName: String?,
        metricSetting: com.alice.homewidget.data.WidgetDataManager.DeviceMetricSetting? = null
    ): DeviceWidgetState {
        return when {
            isVehicle() -> {
                val isRunning = isVehicleRunning()
                val sec = getVehicleSecurityState()
                val isSecOpen = sec == "Открыт" || sec == "Снят с охраны"
                val secMain = sec.uppercase()
                val engineText = "Двигатель: " + (if (isRunning) "Заведён" else "Заглушен")

                val showFuel = metricSetting?.showFuel ?: true
                val showBat = metricSetting?.showBattery ?: true
                val fuel = if (showFuel) getVehicleFuel()?.let { String.format(java.util.Locale.US, "%.0f%%", it) } else null
                val bat = if (showBat) getVehicleBatteryOrVoltage() else null

                DeviceWidgetState(
                    id = id,
                    name = name,
                    roomName = roomName,
                    category = "vehicle",
                    mainText = secMain,
                    subText = engineText,
                    badge1 = fuel?.let { "⛽ $it" } ?: bat?.let { "🔋 $it" },
                    badge2 = if (fuel != null) bat?.let { "🔋 $it" } else null,
                    iconEmoji = "🚗",
                    isAlert = isSecOpen,
                    hasComfortArc = false,
                    rawTemperature = null
                )
            }
            isAc() -> {
                val isOn = isTurnedOn() == true
                val targetT = getTargetTemperature()
                val mode = getThermostatMode()
                val main = if (isOn) {
                    targetT?.let { String.format(java.util.Locale.US, "%.0f°", it) } ?: "ВКЛ"
                } else {
                    "ВЫКЛ"
                }
                val sub = if (isOn) (mode ?: "Охлаждение") else "Кондиционер"
                val b1 = if (isOn) "❄️ ВКЛ" else null

                DeviceWidgetState(
                    id = id,
                    name = name,
                    roomName = roomName,
                    category = "ac",
                    mainText = main,
                    subText = sub,
                    badge1 = b1,
                    badge2 = null,
                    iconEmoji = "❄️",
                    isAlert = false,
                    hasComfortArc = false,
                    rawTemperature = null
                )
            }
            isVacuum() -> {
                val isOn = isTurnedOn() == true
                val main = if (isOn) "УБОРКА" else "НА БАЗЕ"
                val bat = getBattery()?.let { String.format(java.util.Locale.US, "%.0f%%", it) }

                DeviceWidgetState(
                    id = id,
                    name = name,
                    roomName = roomName,
                    category = "vacuum",
                    mainText = main,
                    subText = "Пылесос",
                    badge1 = bat?.let { "🔋 $it" },
                    badge2 = null,
                    iconEmoji = "🧹",
                    isAlert = false,
                    hasComfortArc = false,
                    rawTemperature = null
                )
            }
            isKettle() -> {
                val tempStr = getTemperature()?.let { String.format(java.util.Locale.US, "%.0f°C", it) } ?: "--°C"
                val kState = getKettleState()
                val onOffStr = if (isTurnedOn() == true) "ВКЛ" else "ВЫКЛ"
                DeviceWidgetState(
                    id = id,
                    name = name,
                    roomName = roomName,
                    category = "kettle",
                    mainText = tempStr,
                    subText = kState,
                    badge1 = onOffStr,
                    badge2 = null,
                    iconEmoji = "🫖",
                    isAlert = false,
                    hasComfortArc = false,
                    rawTemperature = null
                )
            }
            isTv() -> {
                val main = if (isTurnedOn() == true) "ВКЛ" else "ВЫКЛ"
                DeviceWidgetState(
                    id = id,
                    name = name,
                    roomName = roomName,
                    category = "tv",
                    mainText = main,
                    subText = "Телевизор",
                    badge1 = null,
                    badge2 = null,
                    iconEmoji = "📺",
                    isAlert = false,
                    hasComfortArc = false,
                    rawTemperature = null
                )
            }
            hasLeakSensor() -> {
                val isLeak = hasWaterLeak() == true
                val showLeakState = metricSetting?.showLeak ?: true
                val showBat = metricSetting?.showBattery ?: true
                val bat = if (showBat) getBattery()?.let { String.format(java.util.Locale.US, "%.0f%%", it) } else null
                val main = if (showLeakState) {
                    if (isLeak) "ПРОТЕЧКА!" else "СУХО"
                } else {
                    bat?.let { "АКБ $it" } ?: (if (isLeak) "ПРОТЕЧКА!" else "СУХО")
                }
                val sub = if (!showLeakState && bat != null) "Заряд батареи" else "Датчик протечки"
                val b1 = if (showLeakState) bat?.let { "🔋 $it" } else null
                DeviceWidgetState(
                    id = id,
                    name = name,
                    roomName = roomName,
                    category = "leak",
                    mainText = main,
                    subText = sub,
                    badge1 = b1,
                    badge2 = null,
                    iconEmoji = if (!showLeakState && bat != null) "🔋" else "💧",
                    isAlert = isLeak,
                    hasComfortArc = false,
                    rawTemperature = null
                )
            }
            hasDoorSensor() -> {
                val isOpen = isDoorOrWindowOpen() == true
                val main = if (isOpen) "ОТКРЫТО!" else "ЗАКРЫТО"
                val showBat = metricSetting?.showBattery ?: true
                val bat = if (showBat) getBattery()?.let { String.format(java.util.Locale.US, "%.0f%%", it) } else null
                DeviceWidgetState(
                    id = id,
                    name = name,
                    roomName = roomName,
                    category = "door",
                    mainText = main,
                    subText = "Датчик открытия",
                    badge1 = bat?.let { "🔋 $it" },
                    badge2 = null,
                    iconEmoji = "🚪",
                    isAlert = isOpen,
                    hasComfortArc = false,
                    rawTemperature = null
                )
            }
            isLightingOrSwitch() -> {
                val isLed = isLedStrip()
                val isLight = isLightBulb()
                val isSocket = type?.contains("socket") == true || name.lowercase().contains("розетк")
                val icon = when {
                    isLed -> "🌈"
                    isSocket -> "🔌"
                    else -> "💡"
                }
                val cat = when {
                    isLed -> "led_strip"
                    isLight -> "light"
                    else -> "switch"
                }
                val main = if (isTurnedOn() == true) "ВКЛ" else "ВЫКЛ"
                val showPwr = metricSetting?.showPower ?: true
                val p = if (showPwr) getPower()?.let { String.format(java.util.Locale.US, "%.0f Вт", it) } else null
                val sub = p ?: when {
                    isLed -> "Подсветка"
                    isSocket -> "Розетка"
                    else -> "Освещение"
                }
                DeviceWidgetState(
                    id = id,
                    name = name,
                    roomName = roomName,
                    category = cat,
                    mainText = main,
                    subText = sub,
                    badge1 = p?.let { "⚡ $it" },
                    badge2 = null,
                    iconEmoji = icon,
                    isAlert = false,
                    hasComfortArc = false,
                    rawTemperature = null
                )
            }
            else -> {
                val showTemp = metricSetting?.showTemperature ?: true
                val showHum = metricSetting?.showHumidity ?: true
                val showPress = metricSetting?.showPressure ?: false
                val showBat = metricSetting?.showBattery ?: true

                val t = if (showTemp) getTemperature() else null
                val h = if (showHum) getHumidity() else null
                val p = if (showPress) getPressure() else null
                val bat = if (showBat) getBattery() else null

                val tStr = when {
                    t != null -> String.format(java.util.Locale.US, "%.1f°", t)
                    h != null -> String.format(java.util.Locale.US, "💧 %.0f%%", h)
                    else -> "--°"
                }
                val humStr = if (t != null && h != null) String.format(java.util.Locale.US, "💧 %.0f%%", h) else null
                val pressStr = p?.let { String.format(java.util.Locale.US, "📊 %.0f", it) }
                val batStr = bat?.let { String.format(java.util.Locale.US, "🔋 %.0f%%", it) }

                val b1 = humStr ?: pressStr ?: batStr
                val b2 = if (b1 == humStr) (pressStr ?: batStr) else (if (b1 == pressStr) batStr else null)

                DeviceWidgetState(
                    id = id,
                    name = name,
                    roomName = roomName,
                    category = "climate",
                    mainText = tStr,
                    subText = if (t != null) "Комфортно" else "Датчик",
                    badge1 = b1,
                    badge2 = b2,
                    iconEmoji = "🌡️",
                    isAlert = false,
                    hasComfortArc = t != null,
                    rawTemperature = t
                )
            }
        }
    }
}

data class DeviceWidgetState(
    val id: String,
    val name: String,
    val roomName: String?,
    val category: String, // "vehicle", "kettle", "tv", "switch", "leak", "door", "climate", "ac", "vacuum"
    val mainText: String,
    val subText: String?,
    val badge1: String?,
    val badge2: String?,
    val iconEmoji: String,
    val isAlert: Boolean = false,
    val hasComfortArc: Boolean = false,
    val rawTemperature: Double? = null
)

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
    val primaryRoomName: String = "Умный дом",
    val primaryState: DeviceWidgetState = DeviceWidgetState("default", "Климат", null, "climate", "22.5°", "Комфортно", "💧 48%", null, "🌡️", false, true, 22.5),
    val secondaryDevices: List<DeviceWidgetState> = emptyList(),
    val totalSensorCount: Int = 1,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
) {
    val householdName: String get() = primaryRoomName
    val primaryTemperature: Double? get() = primaryState.rawTemperature
    val primaryHumidity: Double? get() = null
    val primaryPressure: Double? get() = null
    val roomSensors: List<RoomClimateSummary> get() = emptyList()
    val isAnyDoorOpen: Boolean get() = primaryState.category == "door" && primaryState.isAlert
    val openDoorDeviceName: String? get() = if (isAnyDoorOpen) primaryState.name else null
    val isAnyLeakDetected: Boolean get() = primaryState.category == "leak" && primaryState.isAlert
    val leakDeviceName: String? get() = if (isAnyLeakDetected) primaryState.name else null
    val lowestBatteryLevel: Double? get() = null
}

data class RoomClimateSummary(
    val roomId: String,
    val roomName: String,
    val temperature: Double?,
    val humidity: Double?,
    val pressure: Double?,
    val battery: Double? = null,
    val extraInfo: String? = null
)
