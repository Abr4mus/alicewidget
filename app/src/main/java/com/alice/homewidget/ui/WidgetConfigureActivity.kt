package com.alice.homewidget.ui

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alice.homewidget.R
import com.alice.homewidget.data.WidgetDataManager
import com.alice.homewidget.databinding.ActivityWidgetConfigureBinding
import com.alice.homewidget.model.Device
import com.alice.homewidget.widget.AliceBarWidgetProvider
import com.alice.homewidget.widget.AliceCompact2x1WidgetProvider
import com.alice.homewidget.widget.AliceCompactWidgetProvider
import com.alice.homewidget.widget.AliceHomeWidgetProvider

class WidgetConfigureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWidgetConfigureBinding
    private lateinit var dataManager: WidgetDataManager
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    private class DeviceConfigViewHolder(
        val device: Device,
        val cbEnable: CheckBox,
        val cbTemp: CheckBox?,
        val cbHum: CheckBox?,
        val cbPress: CheckBox?,
        val cbDoor: CheckBox?,
        val cbBat: CheckBox?
    )

    private val viewHolders = mutableListOf<DeviceConfigViewHolder>()

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
            else -> binding.rbConfigThemeXiaomi.isChecked = true
        }

        renderDevicesList(config)

        binding.btnConfigSave.setOnClickListener {
            saveAndFinish()
        }
    }

    private fun renderDevicesList(config: WidgetDataManager.WidgetInstanceConfig) {
        binding.layoutConfigDevicesList.removeAllViews()
        viewHolders.clear()

        val allDevices = dataManager.getAllRelevantDevices()
        val rooms = dataManager.getAvailableRooms()

        if (allDevices.isEmpty()) {
            val emptyTv = CheckBox(this).apply {
                text = "Нет загруженных датчиков. Сначала авторизуйтесь в приложении."
                isEnabled = false
                setTextColor(getColor(R.color.alice_text_muted))
            }
            binding.layoutConfigDevicesList.addView(emptyTv)
            return
        }

        val selectedIds = config.selectedDeviceIds

        for (dev in allDevices) {
            val rName = rooms.firstOrNull { it.id == dev.room }?.name ?: ""
            val title = if (rName.isNotBlank()) "${dev.name} ($rName)" else dev.name

            val isSelected = selectedIds.isEmpty() || selectedIds.contains(dev.id)
            val metricPref = dataManager.getDeviceMetricSetting(dev.id)

            // Card Container
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_tile_xiaomi)
                setPadding(24, 20, 24, 20)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 12
                }
            }

            // Master Checkbox: Include device in THIS widget
            val cbDevice = CheckBox(this).apply {
                text = title
                setTextColor(getColor(R.color.white))
                textSize = 13.5f
                isChecked = isSelected
            }
            card.addView(cbDevice)

            // Sub-metrics layout (ONLY metrics that this device actually has!)
            val metricsRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(32, 6, 0, 0)
            }

            var cbTemp: CheckBox? = null
            var cbHum: CheckBox? = null
            var cbPress: CheckBox? = null
            var cbDoor: CheckBox? = null
            var cbBat: CheckBox? = null

            // 1. Temperature
            if (dev.getTemperature() != null || dev.isClimateSensor()) {
                cbTemp = CheckBox(this).apply {
                    text = "Темп."
                    setTextColor(getColor(R.color.alice_text_light))
                    textSize = 11.5f
                    isChecked = metricPref.showTemperature
                }
                metricsRow.addView(cbTemp)
            }

            // 2. Humidity
            if (dev.getHumidity() != null || dev.isClimateSensor()) {
                cbHum = CheckBox(this).apply {
                    text = "Влажн."
                    setTextColor(getColor(R.color.alice_text_light))
                    textSize = 11.5f
                    isChecked = metricPref.showHumidity
                }
                metricsRow.addView(cbHum)
            }

            // 3. Pressure (Only if device has barometer!)
            if (dev.getPressure() != null) {
                cbPress = CheckBox(this).apply {
                    text = "Давл."
                    setTextColor(getColor(R.color.alice_text_light))
                    textSize = 11.5f
                    isChecked = metricPref.showPressure
                }
                metricsRow.addView(cbPress)
            }

            // 4. Door / Window opening (Only if device has open sensor!)
            if (dev.isOpenSensor() || dev.isDoorOrWindowOpen() != null) {
                cbDoor = CheckBox(this).apply {
                    text = "Дверь"
                    setTextColor(getColor(R.color.alice_text_light))
                    textSize = 11.5f
                    isChecked = metricPref.showDoor
                }
                metricsRow.addView(cbDoor)
            }

            // 5. Battery level (Only if reported)
            if (dev.getBattery() != null) {
                cbBat = CheckBox(this).apply {
                    text = "Батарея"
                    setTextColor(getColor(R.color.alice_text_light))
                    textSize = 11.5f
                    isChecked = metricPref.showBattery
                }
                metricsRow.addView(cbBat)
            }

            if (metricsRow.childCount > 0) {
                card.addView(metricsRow)
            }

            binding.layoutConfigDevicesList.addView(card)
            viewHolders.add(DeviceConfigViewHolder(dev, cbDevice, cbTemp, cbHum, cbPress, cbDoor, cbBat))
        }
    }

    private fun saveAndFinish() {
        val theme = when (binding.rgConfigTheme.checkedRadioButtonId) {
            R.id.rbConfigThemeIos -> WidgetDataManager.THEME_IOS
            R.id.rbConfigThemeAlice -> WidgetDataManager.THEME_ALICE
            else -> WidgetDataManager.THEME_XIAOMI
        }

        val chosenDeviceIds = mutableListOf<String>()

        for (holder in viewHolders) {
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
                showBattery = holder.cbBat?.isChecked ?: currentSetting.showBattery
            )
            dataManager.saveDeviceMetricSetting(devId, newSetting)
        }

        // Save isolated config for THIS widget instance
        val config = WidgetDataManager.WidgetInstanceConfig(
            appWidgetId = appWidgetId,
            theme = theme,
            selectedDeviceIds = chosenDeviceIds
        )
        dataManager.saveWidgetConfig(config)

        // Update the widget immediately
        val appWidgetManager = AppWidgetManager.getInstance(this)
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            // Update all potential providers for this ID
            AliceHomeWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId, dataManager)
            AliceBarWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId, dataManager)
            AliceCompactWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId, dataManager)
            AliceCompact2x1WidgetProvider.updateWidget(this, appWidgetManager, appWidgetId, dataManager)
        }

        val resultIntent = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultIntent)
        Toast.makeText(this, "✅ Виджет успешно настроен!", Toast.LENGTH_SHORT).show()
        finish()
    }
}
