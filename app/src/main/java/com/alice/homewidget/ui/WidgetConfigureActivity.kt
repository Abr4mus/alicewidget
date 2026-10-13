package com.alice.homewidget.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alice.homewidget.R
import com.alice.homewidget.data.WidgetDataManager
import com.alice.homewidget.databinding.ActivityWidgetConfigureBinding
import com.alice.homewidget.model.Device
import com.alice.homewidget.model.Room
import com.alice.homewidget.widget.AliceBarWidgetProvider
import com.alice.homewidget.widget.AliceCompact2x1WidgetProvider
import com.alice.homewidget.widget.AliceCompactWidgetProvider
import com.alice.homewidget.widget.AliceHomeWidgetProvider
import com.google.android.material.materialswitch.MaterialSwitch

class WidgetConfigureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWidgetConfigureBinding
    private lateinit var dataManager: WidgetDataManager
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var selectedRoomFilter: String? = null // null means "ALL"

    /**
     * Large tactile pill button for choosing physical device metrics in HyperOS style.
     */
    class MetricPill(
        val context: Context,
        val metricKey: String,
        val label: String,
        initialChecked: Boolean,
        val onStateChanged: (Boolean) -> Unit
    ) {
        var isChecked: Boolean = initialChecked
            private set

        val view: TextView = TextView(context).apply {
            text = label
            textSize = 12.5f
            gravity = Gravity.CENTER
            val density = resources.displayMetrics.density
            setPadding(
                (14 * density).toInt(),
                (7 * density).toInt(),
                (14 * density).toInt(),
                (7 * density).toInt()
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (36 * density).toInt()
            ).apply {
                marginEnd = (8 * density).toInt()
                topMargin = (4 * density).toInt()
                bottomMargin = (4 * density).toInt()
            }
            setOnClickListener {
                setChecked(!isChecked, notify = true)
            }
        }

        init {
            updateVisual()
        }

        fun setChecked(checked: Boolean, notify: Boolean = false) {
            if (isChecked != checked) {
                isChecked = checked
                updateVisual()
                if (notify) onStateChanged(isChecked)
            }
        }

        private fun updateVisual() {
            val density = context.resources.displayMetrics.density
            val bg = GradientDrawable().apply {
                cornerRadius = 18f * density
                if (isChecked) {
                    setColor(0xFF00D2B4.toInt()) // HyperOS Cyan Teal
                } else {
                    setColor(0x1AFFFFFF)
                    setStroke((1f * density).toInt(), 0x30FFFFFF)
                }
            }
            view.background = bg
            view.setTextColor(if (isChecked) Color.WHITE else 0xFF8E95A3.toInt())
            view.setTypeface(null, if (isChecked) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private class DeviceConfigViewHolder(
        val device: Device,
        val cardView: View,
        val switchDevice: MaterialSwitch,
        val metricsContainer: ViewGroup,
        val pills: List<MetricPill>
    ) {
        fun getPill(key: String): MetricPill? = pills.firstOrNull { it.metricKey == key }
        val isEnabled: Boolean get() = switchDevice.isChecked
    }

    private class RoomGroup(
        val roomId: String?,
        val roomName: String,
        val containerView: View,
        val switchRoom: MaterialSwitch,
        val deviceHolders: MutableList<DeviceConfigViewHolder> = mutableListOf()
    )

    private val roomGroups = mutableListOf<RoomGroup>()
    private val allDeviceHolders = mutableListOf<DeviceConfigViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        binding = ActivityWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataManager = WidgetDataManager(this)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetId = intent?.getIntExtra("widget_id", AppWidgetManager.INVALID_APPWIDGET_ID)
                ?: AppWidgetManager.INVALID_APPWIDGET_ID
        }

        binding.tvConfigWidgetTitle.text = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            "Настройка виджета #${appWidgetId}"
        } else {
            "Настройка виджета"
        }

        try {
            val config = dataManager.getWidgetConfig(appWidgetId)

            // Set theme selection
            when (config.theme) {
                WidgetDataManager.THEME_IOS -> binding.rbConfigThemeIos.isChecked = true
                WidgetDataManager.THEME_ALICE -> binding.rbConfigThemeAlice.isChecked = true
                WidgetDataManager.THEME_YANDEX_WHITE -> binding.rbConfigThemeYandexWhite.isChecked = true
                WidgetDataManager.THEME_FLUENT_PASTEL -> binding.rbConfigThemeFluentPastel.isChecked = true
                WidgetDataManager.THEME_ANDROID -> binding.rbConfigThemeAndroid.isChecked = true
                WidgetDataManager.THEME_NOTHING -> binding.rbConfigThemeNothing.isChecked = true
                else -> binding.rbConfigThemeXiaomi.isChecked = true
            }

            // Set custom title if previously saved
            binding.etConfigTitle.setText(config.customTitle ?: "")

            renderRoomChips(config)
            renderGroupedDevicesList(config)

            binding.btnConfigSave.setOnClickListener {
                saveAndFinish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка при загрузке настроек: ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun renderRoomChips(config: WidgetDataManager.WidgetInstanceConfig) {
        binding.layoutRoomChips.removeAllViews()
        val rooms = dataManager.getAvailableRooms()

        addRoomChip("Все комнаты", null)
        for (room in rooms) {
            addRoomChip(room.name.ifBlank { "Комната" }, room.id)
        }
    }

    private fun addRoomChip(label: String?, roomId: String?) {
        val density = resources.displayMetrics.density
        val isSelected = selectedRoomFilter == roomId
        val chip = TextView(this).apply {
            text = label ?: "Комната"
            textSize = 12.5f
            gravity = Gravity.CENTER
            setPadding((14 * density).toInt(), (6 * density).toInt(), (14 * density).toInt(), (6 * density).toInt())
            val bg = GradientDrawable().apply {
                cornerRadius = 18f * density
                if (isSelected) {
                    setColor(getColor(R.color.alice_neon_purple))
                } else {
                    setColor(0x22FFFFFF)
                    setStroke((1f * density).toInt(), 0x30FFFFFF)
                }
            }
            background = bg
            setTextColor(if (isSelected) Color.WHITE else getColor(R.color.alice_text_light))
            setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (36 * density).toInt()
            ).apply {
                marginEnd = (8 * density).toInt()
                topMargin = (4 * density).toInt()
                bottomMargin = (4 * density).toInt()
            }
            setOnClickListener {
                selectedRoomFilter = roomId
                renderRoomChips(dataManager.getWidgetConfig(appWidgetId))
                filterRoomsAndDevices()
            }
        }
        binding.layoutRoomChips.addView(chip)
    }

    private fun filterRoomsAndDevices() {
        for (group in roomGroups) {
            val matches = selectedRoomFilter == null || group.roomId == selectedRoomFilter
            group.containerView.visibility = if (matches) View.VISIBLE else View.GONE
        }
    }

    private fun renderGroupedDevicesList(config: WidgetDataManager.WidgetInstanceConfig) {
        binding.layoutConfigDevicesList.removeAllViews()
        roomGroups.clear()
        allDeviceHolders.clear()

        val allDevices = dataManager.getAllRelevantDevices()
        val rooms = dataManager.getAvailableRooms()

        if (allDevices.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "Нет загруженных датчиков. Сначала авторизуйтесь в приложении и выполните синхронизацию."
                setTextColor(getColor(R.color.alice_text_muted))
                textSize = 13f
                setPadding(16, 16, 16, 16)
            }
            binding.layoutConfigDevicesList.addView(emptyTv)
            return
        }

        val hasSavedConfig = dataManager.hasWidgetConfig(appWidgetId)
        val selectedIds = config.selectedDeviceIds

        // Group devices by room
        val roomsWithDevices = mutableListOf<Pair<Room?, List<Device>>>()
        for (room in rooms) {
            val devInRoom = allDevices.filter { it.room == room.id || room.hasDevice(it.id) }
            if (devInRoom.isNotEmpty()) {
                roomsWithDevices.add(room to devInRoom)
            }
        }
        val unassigned = allDevices.filter { dev ->
            rooms.none { it.id == dev.room || it.hasDevice(dev.id) }
        }
        if (unassigned.isNotEmpty()) {
            roomsWithDevices.add(null to unassigned)
        }

        val density = resources.displayMetrics.density

        for ((room, devList) in roomsWithDevices) {
            val rName = room?.name ?: "Другие устройства"
            val rId = room?.id

            // Room Card Container (HyperOS Rounded Glass Tile)
            val roomCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_tile_xiaomi)
                setPadding((18 * density).toInt(), (16 * density).toInt(), (18 * density).toInt(), (16 * density).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (14 * density).toInt()
                }
            }

            // Room Header with Large Material Switch
            val roomHeader = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, (10 * density).toInt())
            }

            val iconTv = TextView(this).apply {
                text = "🏠"
                textSize = 18f
            }
            roomHeader.addView(iconTv)

            val roomTitleTv = TextView(this).apply {
                text = " $rName"
                setTextColor(Color.WHITE)
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = (6 * density).toInt()
                }
            }
            roomHeader.addView(roomTitleTv)

            val countTv = TextView(this).apply {
                text = "${devList.size} устр."
                setTextColor(getColor(R.color.alice_neon_light))
                textSize = 11.5f
                setBackgroundResource(R.drawable.bg_alice_chip)
                setPadding((8 * density).toInt(), (2 * density).toInt(), (8 * density).toInt(), (2 * density).toInt())
            }
            roomHeader.addView(countTv)

            // Large Room Switch
            val switchRoom = MaterialSwitch(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = (10 * density).toInt()
                }
            }
            roomHeader.addView(switchRoom)
            roomCard.addView(roomHeader)

            val groupHolders = mutableListOf<DeviceConfigViewHolder>()
            val group = RoomGroup(rId, rName, roomCard, switchRoom, groupHolders)
            roomGroups.add(group)

            // Devices in this room
            for (dev in devList) {
                val isSelected = if (hasSavedConfig) {
                    selectedIds.contains(dev.id)
                } else {
                    // Default for brand new widget
                    dev.isClimateSensor() || dev.hasTemperature() || dev.hasDoorSensor()
                }

                val metricPref = dataManager.getDeviceMetricSetting(dev.id, appWidgetId)

                // Device Container Card (HyperOS Inset Tile)
                val devCard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding((16 * density).toInt(), (12 * density).toInt(), (14 * density).toInt(), (12 * density).toInt())
                    val bgDrawable = GradientDrawable().apply {
                        cornerRadius = 14f * density
                        setColor(0x18FFFFFF)
                    }
                    background = bgDrawable
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = (8 * density).toInt()
                    }
                }

                // Device Header Row (Icon + Name + Subtitle + Large MaterialSwitch)
                val devHeader = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                // Device Icon
                val devIcon = when {
                    dev.hasDoorSensor() -> "🚪"
                    dev.hasWaterOrFuel() -> "⛽"
                    dev.hasPower() -> "⚡"
                    dev.hasTemperature() -> "🌡️"
                    dev.hasBattery() -> "🔋"
                    else -> "💡"
                }

                val devIconTv = TextView(this).apply {
                    text = devIcon
                    textSize = 16f
                }
                devHeader.addView(devIconTv)

                val devInfoLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginStart = (8 * density).toInt()
                        marginEnd = (8 * density).toInt()
                    }
                }

                val devNameTv = TextView(this).apply {
                    text = dev.name
                    setTextColor(Color.WHITE)
                    textSize = 14.5f
                    setTypeface(null, Typeface.BOLD)
                    maxLines = 1
                }
                devInfoLayout.addView(devNameTv)

                // Subtitle preview of current state
                val stateDesc = when {
                    dev.hasTemperature() && dev.getTemperature() != null -> {
                        val t = "${dev.getTemperature()}°C"
                        val h = dev.getHumidity()?.let { " • $it%" } ?: ""
                        "$t$h"
                    }
                    dev.hasDoorSensor() -> if (dev.isDoorOrWindowOpen() == true) "ОТКРЫТО!" else "Закрыто"
                    dev.hasWaterOrFuel() -> dev.getWaterOrFuelLevel()?.let { "Уровень: ${it.toInt()}%" } ?: ""
                    dev.hasPower() -> dev.getPower()?.let { "${it.toInt()} Вт" } ?: ""
                    dev.hasBattery() -> dev.getBattery()?.let { "Заряд: $it%" } ?: ""
                    else -> ""
                }

                if (stateDesc.isNotBlank()) {
                    val devStateTv = TextView(this).apply {
                        text = stateDesc
                        setTextColor(getColor(R.color.alice_text_muted))
                        textSize = 11f
                    }
                    devInfoLayout.addView(devStateTv)
                }
                devHeader.addView(devInfoLayout)

                // Large Device Switch ("Включить в виджет")
                val switchDevice = MaterialSwitch(this).apply {
                    isChecked = isSelected
                }
                devHeader.addView(switchDevice)
                devHeader.setOnClickListener {
                    switchDevice.performClick()
                }
                devCard.addView(devHeader)

                // Metrics Row (Tactile Pill Chips)
                val metricsContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding((4 * density).toInt(), (8 * density).toInt(), 0, 0)
                    alpha = if (isSelected) 1.0f else 0.35f
                }

                val pills = mutableListOf<MetricPill>()

                fun createPill(key: String, label: String, initialChecked: Boolean): MetricPill {
                    lateinit var pillRef: MetricPill
                    pillRef = MetricPill(this@WidgetConfigureActivity, key, label, initialChecked) { isCheckedNow ->
                        if (isCheckedNow) {
                            // Turning on ANY metric -> turn on device and turn on room!
                            if (!switchDevice.isChecked) {
                                switchDevice.isChecked = true
                                metricsContainer.alpha = 1.0f
                            }
                            switchRoom.isChecked = true
                        } else {
                            // If all pills turned off -> turn off device
                            val anyPillActive = pills.any { it.isChecked }
                            if (!anyPillActive) {
                                switchDevice.isChecked = false
                                metricsContainer.alpha = 0.35f
                                val anyDevActive = groupHolders.any { it.switchDevice.isChecked }
                                if (!anyDevActive) switchRoom.isChecked = false
                            }
                        }
                    }
                    return pillRef
                }

                // 1. Temperature
                if (dev.hasTemperature()) {
                    val p = createPill("temp", "🌡️ Темп.", isSelected && metricPref.showTemperature)
                    pills.add(p)
                    metricsContainer.addView(p.view)
                }

                // 2. Humidity
                if (dev.hasHumidity()) {
                    val p = createPill("hum", "💧 Влажн.", isSelected && metricPref.showHumidity)
                    pills.add(p)
                    metricsContainer.addView(p.view)
                }

                // 3. Pressure
                if (dev.hasPressure()) {
                    val p = createPill("press", "📊 Давл.", isSelected && metricPref.showPressure)
                    pills.add(p)
                    metricsContainer.addView(p.view)
                }

                // 4. Door
                if (dev.hasDoorSensor()) {
                    val p = createPill("door", "🚪 Дверь", isSelected && metricPref.showDoor)
                    pills.add(p)
                    metricsContainer.addView(p.view)
                }

                // 5. Battery
                if (dev.hasBattery()) {
                    val p = createPill("bat", "🔋 Батарея", isSelected && metricPref.showBattery)
                    pills.add(p)
                    metricsContainer.addView(p.view)
                }

                // 6. Fuel / Water Level
                if (dev.hasWaterOrFuel()) {
                    val p = createPill("fuel", "⛽ Уровень", isSelected && metricPref.showFuel)
                    pills.add(p)
                    metricsContainer.addView(p.view)
                }

                // 7. Power
                if (dev.hasPower()) {
                    val p = createPill("power", "⚡ Мощность", isSelected && metricPref.showPower)
                    pills.add(p)
                    metricsContainer.addView(p.view)
                }

                if (pills.isNotEmpty()) {
                    devCard.addView(metricsContainer)
                }

                roomCard.addView(devCard)

                val holder = DeviceConfigViewHolder(dev, devCard, switchDevice, metricsContainer, pills)
                groupHolders.add(holder)
                allDeviceHolders.add(holder)

                // When Device Switch is clicked by user:
                switchDevice.setOnClickListener {
                    val devActive = switchDevice.isChecked
                    metricsContainer.alpha = if (devActive) 1.0f else 0.35f

                    if (devActive) {
                        // Check all available physical metric pills
                        pills.forEach { it.setChecked(true) }
                        // Room MUST turn on when ANY device is enabled
                        switchRoom.isChecked = true
                    } else {
                        // Uncheck all metric pills when device is disabled
                        pills.forEach { it.setChecked(false) }
                        // If no devices enabled in room, turn off room switch
                        val anyDevInRoom = groupHolders.any { it.switchDevice.isChecked }
                        if (!anyDevInRoom) {
                            switchRoom.isChecked = false
                        }
                    }
                }
            }

            // Initial state of Room Switch: ON if ANY device in room is enabled!
            switchRoom.isChecked = groupHolders.any { it.switchDevice.isChecked }

            // When Room Switch is clicked by user:
            switchRoom.setOnClickListener {
                val roomActive = switchRoom.isChecked
                for (h in groupHolders) {
                    h.switchDevice.isChecked = roomActive
                    h.metricsContainer.alpha = if (roomActive) 1.0f else 0.35f
                    h.pills.forEach { it.setChecked(roomActive) }
                }
            }

            roomHeader.setOnClickListener {
                switchRoom.performClick()
            }

            binding.layoutConfigDevicesList.addView(roomCard)
        }
    }

    private fun saveAndFinish() {
        val theme = when (binding.rgConfigTheme.checkedRadioButtonId) {
            R.id.rbConfigThemeIos -> WidgetDataManager.THEME_IOS
            R.id.rbConfigThemeAlice -> WidgetDataManager.THEME_ALICE
            R.id.rbConfigThemeYandexWhite -> WidgetDataManager.THEME_YANDEX_WHITE
            R.id.rbConfigThemeFluentPastel -> WidgetDataManager.THEME_FLUENT_PASTEL
            R.id.rbConfigThemeAndroid -> WidgetDataManager.THEME_ANDROID
            R.id.rbConfigThemeNothing -> WidgetDataManager.THEME_NOTHING
            else -> WidgetDataManager.THEME_XIAOMI
        }

        val customTitle = binding.etConfigTitle.text.toString().trim().ifBlank { null }
        val chosenDeviceIds = mutableListOf<String>()

        for (holder in allDeviceHolders) {
            val devId = holder.device.id
            if (holder.isEnabled) {
                chosenDeviceIds.add(devId)
            }

            // Save individual metrics for this device STRICTLY ISOLATED to this widget instance!
            val newSetting = WidgetDataManager.DeviceMetricSetting(
                showTemperature = holder.getPill("temp")?.isChecked ?: true,
                showHumidity = holder.getPill("hum")?.isChecked ?: true,
                showPressure = holder.getPill("press")?.isChecked ?: false,
                showDoor = holder.getPill("door")?.isChecked ?: true,
                showLeak = true,
                showBattery = holder.getPill("bat")?.isChecked ?: true,
                showFuel = holder.getPill("fuel")?.isChecked ?: true,
                showPower = holder.getPill("power")?.isChecked ?: true
            )
            dataManager.saveDeviceMetricSetting(devId, newSetting, appWidgetId)
        }

        // Save isolated config for THIS specific widget instance
        val config = WidgetDataManager.WidgetInstanceConfig(
            appWidgetId = appWidgetId,
            theme = theme,
            selectedDeviceIds = chosenDeviceIds,
            customTitle = customTitle
        )
        dataManager.saveWidgetConfig(config)

        // Update ONLY the exact provider corresponding to this widget instance
        val appWidgetManager = AppWidgetManager.getInstance(this)
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
            val providerClass = info?.provider?.className

            when (providerClass) {
                AliceHomeWidgetProvider::class.java.name -> {
                    AliceHomeWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId, dataManager)
                }
                AliceBarWidgetProvider::class.java.name -> {
                    AliceBarWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId, dataManager)
                }
                AliceCompactWidgetProvider::class.java.name -> {
                    AliceCompactWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId, dataManager)
                }
                AliceCompact2x1WidgetProvider::class.java.name -> {
                    AliceCompact2x1WidgetProvider.updateWidget(this, appWidgetManager, appWidgetId, dataManager)
                }
                else -> {
                    // Fallback: match by registered app widget IDs
                    val p4x2 = appWidgetManager.getAppWidgetIds(ComponentName(this, AliceHomeWidgetProvider::class.java))
                    val p4x1 = appWidgetManager.getAppWidgetIds(ComponentName(this, AliceBarWidgetProvider::class.java))
                    val p2x2 = appWidgetManager.getAppWidgetIds(ComponentName(this, AliceCompactWidgetProvider::class.java))
                    val p2x1 = appWidgetManager.getAppWidgetIds(ComponentName(this, AliceCompact2x1WidgetProvider::class.java))

                    when {
                        p4x2.contains(appWidgetId) -> AliceHomeWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId, dataManager)
                        p4x1.contains(appWidgetId) -> AliceBarWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId, dataManager)
                        p2x2.contains(appWidgetId) -> AliceCompactWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId, dataManager)
                        p2x1.contains(appWidgetId) -> AliceCompact2x1WidgetProvider.updateWidget(this, appWidgetManager, appWidgetId, dataManager)
                    }
                }
            }
        }

        val resultIntent = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultIntent)
        Toast.makeText(this, "✅ Настройки виджета #${appWidgetId} сохранены!", Toast.LENGTH_SHORT).show()
        finish()
    }
}
