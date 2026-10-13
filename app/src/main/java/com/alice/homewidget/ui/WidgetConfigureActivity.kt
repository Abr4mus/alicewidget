package com.alice.homewidget.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
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

class WidgetConfigureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWidgetConfigureBinding
    private lateinit var dataManager: WidgetDataManager
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var selectedRoomFilter: String? = null // null means "ALL"

    private class DeviceConfigViewHolder(
        val device: Device,
        val cardView: View,
        val cbEnable: CheckBox,
        val cbTemp: CheckBox?,
        val cbHum: CheckBox?,
        val cbPress: CheckBox?,
        val cbDoor: CheckBox?,
        val cbBat: CheckBox?,
        val cbFuel: CheckBox?,
        val cbPower: CheckBox?
    )

    private class RoomGroup(
        val roomId: String?,
        val roomName: String,
        val containerView: View,
        val roomCheckBox: CheckBox,
        val deviceHolders: MutableList<DeviceConfigViewHolder> = mutableListOf()
    )

    private val roomGroups = mutableListOf<RoomGroup>()
    private val allDeviceHolders = mutableListOf<DeviceConfigViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED) // Default in case user backs out

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

        val config = dataManager.getWidgetConfig(appWidgetId)

        // Set theme selection
        when (config.theme) {
            WidgetDataManager.THEME_IOS -> binding.rbConfigThemeIos.isChecked = true
            WidgetDataManager.THEME_ALICE -> binding.rbConfigThemeAlice.isChecked = true
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
    }

    private fun renderRoomChips(config: WidgetDataManager.WidgetInstanceConfig) {
        binding.layoutRoomChips.removeAllViews()
        val rooms = dataManager.getAvailableRooms()

        addRoomChip("Все комнаты", null)
        for (room in rooms) {
            addRoomChip(room.name, room.id)
        }
    }

    private fun addRoomChip(label: String, roomId: String?) {
        val chip = Button(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = label
            textSize = 11.5f
            val isSelected = selectedRoomFilter == roomId
            if (isSelected) {
                setTextColor(getColor(R.color.white))
                setBackgroundColor(getColor(R.color.alice_neon_purple))
            } else {
                setTextColor(getColor(R.color.alice_text_light))
                setBackgroundColor(0x22FFFFFF)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (36 * resources.displayMetrics.density).toInt()
            ).apply {
                marginEnd = (8 * resources.displayMetrics.density).toInt()
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

        val selectedIds = config.selectedDeviceIds

        // Group devices by room
        val roomsWithDevices = mutableListOf<Pair<Room?, List<Device>>>()
        for (room in rooms) {
            val devInRoom = allDevices.filter { it.room == room.id || room.devices.contains(it.id) }
            if (devInRoom.isNotEmpty()) {
                roomsWithDevices.add(room to devInRoom)
            }
        }
        // Devices without a room
        val unassigned = allDevices.filter { dev ->
            rooms.none { it.id == dev.room || it.devices.contains(dev.id) }
        }
        if (unassigned.isNotEmpty()) {
            roomsWithDevices.add(null to unassigned)
        }

        for ((room, devList) in roomsWithDevices) {
            val rName = room?.name ?: "Другие устройства"
            val rId = room?.id

            // Room Card Container
            val roomCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_tile_xiaomi)
                setPadding(20, 16, 20, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 14
                }
            }

            // Room Header with Master CheckBox
            val roomHeader = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, 8)
            }

            val cbRoom = CheckBox(this).apply {
                text = "🏠 $rName"
                setTextColor(getColor(R.color.white))
                textSize = 14.5f
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            roomHeader.addView(cbRoom)

            val countTv = TextView(this).apply {
                text = "${devList.size} устр."
                setTextColor(getColor(R.color.alice_neon_light))
                textSize = 11.5f
            }
            roomHeader.addView(countTv)
            roomCard.addView(roomHeader)

            val groupHolders = mutableListOf<DeviceConfigViewHolder>()
            val group = RoomGroup(rId, rName, roomCard, cbRoom, groupHolders)
            roomGroups.add(group)

            // Child devices in this room
            for (dev in devList) {
                val isSelected = if (selectedIds.isEmpty()) {
                    dev.isClimateSensor() || dev.hasTemperature()
                } else {
                    selectedIds.contains(dev.id)
                }

                val metricPref = dataManager.getDeviceMetricSetting(dev.id)

                val devContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 10, 12, 10)
                    setBackgroundColor(0x12FFFFFF)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 6
                    }
                }

                // Device Master CheckBox
                val cbDevice = CheckBox(this).apply {
                    text = dev.name
                    setTextColor(getColor(R.color.alice_text_light))
                    textSize = 13.5f
                    isChecked = isSelected
                }
                devContainer.addView(cbDevice)

                // Sub-metrics layout (STRICTLY metrics physically supported by this device!)
                val metricsRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(28, 4, 0, 0)
                }

                var cbTemp: CheckBox? = null
                var cbHum: CheckBox? = null
                var cbPress: CheckBox? = null
                var cbDoor: CheckBox? = null
                var cbBat: CheckBox? = null
                var cbFuel: CheckBox? = null
                var cbPower: CheckBox? = null

                // 1. Temperature (strictly if device has it)
                if (dev.hasTemperature()) {
                    cbTemp = CheckBox(this).apply {
                        text = "Темп."
                        setTextColor(getColor(R.color.alice_text_muted))
                        textSize = 11.5f
                        isChecked = metricPref.showTemperature
                    }
                    metricsRow.addView(cbTemp)
                }

                // 2. Humidity (strictly if device has humidity property!)
                if (dev.hasHumidity()) {
                    cbHum = CheckBox(this).apply {
                        text = "Влажн."
                        setTextColor(getColor(R.color.alice_text_muted))
                        textSize = 11.5f
                        isChecked = metricPref.showHumidity
                    }
                    metricsRow.addView(cbHum)
                }

                // 3. Pressure (strictly if barometer exists)
                if (dev.hasPressure()) {
                    cbPress = CheckBox(this).apply {
                        text = "Давл."
                        setTextColor(getColor(R.color.alice_text_muted))
                        textSize = 11.5f
                        isChecked = metricPref.showPressure
                    }
                    metricsRow.addView(cbPress)
                }

                // 4. Door (contact / open sensor)
                if (dev.hasDoorSensor()) {
                    cbDoor = CheckBox(this).apply {
                        text = "Дверь"
                        setTextColor(getColor(R.color.alice_text_muted))
                        textSize = 11.5f
                        isChecked = metricPref.showDoor
                    }
                    metricsRow.addView(cbDoor)
                }

                // 5. Battery (if reported)
                if (dev.hasBattery()) {
                    cbBat = CheckBox(this).apply {
                        text = "Батарея"
                        setTextColor(getColor(R.color.alice_text_muted))
                        textSize = 11.5f
                        isChecked = metricPref.showBattery
                    }
                    metricsRow.addView(cbBat)
                }

                // 6. Fuel / Water Level (cars, tanks)
                if (dev.hasWaterOrFuel()) {
                    cbFuel = CheckBox(this).apply {
                        text = "Уровень"
                        setTextColor(getColor(R.color.alice_text_muted))
                        textSize = 11.5f
                        isChecked = metricPref.showFuel
                    }
                    metricsRow.addView(cbFuel)
                }

                // 7. Power (sockets, relays)
                if (dev.hasPower()) {
                    cbPower = CheckBox(this).apply {
                        text = "Мощность"
                        setTextColor(getColor(R.color.alice_text_muted))
                        textSize = 11.5f
                        isChecked = metricPref.showPower
                    }
                    metricsRow.addView(cbPower)
                }

                if (metricsRow.childCount > 0) {
                    devContainer.addView(metricsRow)
                }

                roomCard.addView(devContainer)

                val holder = DeviceConfigViewHolder(
                    dev, devContainer, cbDevice, cbTemp, cbHum, cbPress, cbDoor, cbBat, cbFuel, cbPower
                )
                groupHolders.add(holder)
                allDeviceHolders.add(holder)

                // When individual device checkbox changes, update room checkbox
                cbDevice.setOnCheckedChangeListener { _, _ ->
                    val allChecked = groupHolders.all { it.cbEnable.isChecked }
                    val noneChecked = groupHolders.none { it.cbEnable.isChecked }
                    if (allChecked) {
                        cbRoom.isChecked = true
                    } else if (noneChecked) {
                        cbRoom.isChecked = false
                    }
                }
            }

            // Sync initial state of Room CheckBox
            cbRoom.isChecked = groupHolders.isNotEmpty() && groupHolders.all { it.cbEnable.isChecked }

            // When Room CheckBox is toggled, check/uncheck all devices in this room
            cbRoom.setOnClickListener {
                val targetState = cbRoom.isChecked
                for (h in groupHolders) {
                    h.cbEnable.isChecked = targetState
                }
            }

            binding.layoutConfigDevicesList.addView(roomCard)
        }
    }

    private fun saveAndFinish() {
        val theme = when (binding.rgConfigTheme.checkedRadioButtonId) {
            R.id.rbConfigThemeIos -> WidgetDataManager.THEME_IOS
            R.id.rbConfigThemeAlice -> WidgetDataManager.THEME_ALICE
            R.id.rbConfigThemeAndroid -> WidgetDataManager.THEME_ANDROID
            R.id.rbConfigThemeNothing -> WidgetDataManager.THEME_NOTHING
            else -> WidgetDataManager.THEME_XIAOMI
        }

        val customTitle = binding.etConfigTitle.text.toString().trim().ifBlank { null }
        val chosenDeviceIds = mutableListOf<String>()

        for (holder in allDeviceHolders) {
            val devId = holder.device.id
            if (holder.cbEnable.isChecked) {
                chosenDeviceIds.add(devId)
            }

            // Save individual metrics for this device
            val currentSetting = dataManager.getDeviceMetricSetting(devId)
            val newSetting = WidgetDataManager.DeviceMetricSetting(
                showTemperature = holder.cbTemp?.isChecked ?: currentSetting.showTemperature,
                showHumidity = holder.cbHum?.isChecked ?: currentSetting.showHumidity,
                showPressure = holder.cbPress?.isChecked ?: currentSetting.showPressure,
                showDoor = holder.cbDoor?.isChecked ?: currentSetting.showDoor,
                showLeak = currentSetting.showLeak,
                showBattery = holder.cbBat?.isChecked ?: currentSetting.showBattery,
                showFuel = holder.cbFuel?.isChecked ?: currentSetting.showFuel,
                showPower = holder.cbPower?.isChecked ?: currentSetting.showPower
            )
            dataManager.saveDeviceMetricSetting(devId, newSetting)
        }

        // Save isolated config for THIS specific widget instance
        val config = WidgetDataManager.WidgetInstanceConfig(
            appWidgetId = appWidgetId,
            theme = theme,
            selectedDeviceIds = chosenDeviceIds,
            customTitle = customTitle
        )
        dataManager.saveWidgetConfig(config)

        // Update ONLY the exact provider corresponding to this widget instance (eliminating provider overwrite bug!)
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
        Toast.makeText(this, "✅ Виджет успешно настроен и обновлён!", Toast.LENGTH_SHORT).show()
        finish()
    }
}
