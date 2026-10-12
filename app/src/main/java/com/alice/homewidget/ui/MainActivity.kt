package com.alice.homewidget.ui

import android.app.Dialog
import android.appwidget.AppWidgetManager
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.alice.homewidget.AliceApplication
import com.alice.homewidget.R
import com.alice.homewidget.api.YandexApiClient
import com.alice.homewidget.data.WidgetDataManager
import com.alice.homewidget.databinding.ActivityMainBinding
import com.alice.homewidget.model.Device
import com.alice.homewidget.model.Room
import com.alice.homewidget.widget.AliceBarWidgetProvider
import com.alice.homewidget.widget.AliceCompact2x1WidgetProvider
import com.alice.homewidget.widget.AliceCompactWidgetProvider
import com.alice.homewidget.widget.AliceHomeWidgetProvider
import com.alice.homewidget.worker.SensorSyncWorker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dataManager: WidgetDataManager
    private val apiClient = YandexApiClient()
    private var availableRooms: List<Room> = emptyList()

    enum class WidgetConfigTab {
        W4X2, W4X1, W2X2, W2X1
    }

    private var currentTab: WidgetConfigTab = WidgetConfigTab.W4X2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataManager = WidgetDataManager(this)

        initViews()
        initThemeSelector()
        initTabSelector()
        renderSettings()
        renderCachedStatus()
    }

    private fun initViews() {
        binding.etClientId.setText(dataManager.clientId)
        binding.etOauthToken.setText(dataManager.oauthToken)

        // Button to create application on oauth.yandex.ru
        binding.btnCreateClientId.setOnClickListener {
            val url = "https://oauth.yandex.ru/client/new"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        // Paste ClientID
        binding.btnPasteClientId.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val item = clipboard.primaryClip?.getItemAt(0)
            val text = item?.text?.toString()?.trim()
            if (!text.isNullOrBlank()) {
                binding.etClientId.setText(text)
                dataManager.clientId = text
                Toast.makeText(this, "ClientID вставлен", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Буфер обмена пуст", Toast.LENGTH_SHORT).show()
            }
        }

        // Automatic in-app token capture via WebView
        binding.btnAuthorizeYandex.setOnClickListener {
            val clientId = binding.etClientId.text.toString().trim()
            if (clientId.isBlank()) {
                Toast.makeText(this, "Сначала введите ClientID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dataManager.clientId = clientId
            openInAppAuth(clientId)
        }

        // Paste Token
        binding.btnPasteToken.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val item = clipboard.primaryClip?.getItemAt(0)
            val text = item?.text?.toString()?.trim()
            if (!text.isNullOrBlank()) {
                binding.etOauthToken.setText(text)
                Toast.makeText(this, "Токен вставлен", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Буфер обмена пуст", Toast.LENGTH_SHORT).show()
            }
        }

        // Sync & Verify Button
        binding.btnTestSync.setOnClickListener {
            val token = binding.etOauthToken.text.toString().trim()
            if (token.isBlank()) {
                Toast.makeText(this, "Пожалуйста, получите или вставьте токен", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val clientId = binding.etClientId.text.toString().trim()
            if (clientId.isNotBlank()) {
                dataManager.clientId = clientId
            }

            dataManager.oauthToken = token
            performSync(token)
        }

        // Pin widgets directly to desktop
        binding.btnPinWidget4x2.setOnClickListener { pinWidget(AliceHomeWidgetProvider::class.java) }
        binding.btnPinWidget2x2.setOnClickListener { pinWidget(AliceCompactWidgetProvider::class.java) }
        binding.btnPinWidget2x1.setOnClickListener { pinWidget(AliceCompact2x1WidgetProvider::class.java) }
        binding.btnPinWidget4x1.setOnClickListener { pinWidget(AliceBarWidgetProvider::class.java) }

        // Save custom metric settings
        binding.btnSaveSettings.setOnClickListener {
            saveWidgetPreferences()
        }

        // Open official Yandex app
        binding.btnOpenYandexApp.setOnClickListener {
            val launchIntent = packageManager.getLaunchIntentForPackage("com.yandex.iot")
                ?: packageManager.getLaunchIntentForPackage("ru.yandex.searchplugin")
            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.yandex.iot")))
            }
        }
    }

    private fun initThemeSelector() {
        when (dataManager.widgetTheme) {
            WidgetDataManager.THEME_IOS -> binding.rbThemeIos.isChecked = true
            WidgetDataManager.THEME_ANDROID -> binding.rbThemeAndroid.isChecked = true
            else -> binding.rbThemeAlice.isChecked = true
        }

        binding.rgThemeSelector.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.rbThemeIos -> WidgetDataManager.THEME_IOS
                R.id.rbThemeAndroid -> WidgetDataManager.THEME_ANDROID
                else -> WidgetDataManager.THEME_ALICE
            }
            dataManager.widgetTheme = theme
            SensorSyncWorker.notifyWidgetsToUpdate(this)
            val name = when (theme) {
                WidgetDataManager.THEME_IOS -> "iPhone / iOS"
                WidgetDataManager.THEME_ANDROID -> "Android / Material You"
                else -> "Яндекс / Алиса"
            }
            Toast.makeText(this, "Стиль виджетов изменен на: $name", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initTabSelector() {
        val updateTabStyles = {
            binding.btnTab4x2.setTextColor(if (currentTab == WidgetConfigTab.W4X2) getColor(R.color.alice_neon_light) else Color.WHITE)
            binding.btnTab4x1.setTextColor(if (currentTab == WidgetConfigTab.W4X1) getColor(R.color.alice_neon_light) else Color.WHITE)
            binding.btnTab2x2.setTextColor(if (currentTab == WidgetConfigTab.W2X2) getColor(R.color.alice_neon_light) else Color.WHITE)
            binding.btnTab2x1.setTextColor(if (currentTab == WidgetConfigTab.W2X1) getColor(R.color.alice_neon_light) else Color.WHITE)

            when (currentTab) {
                WidgetConfigTab.W4X2 -> {
                    binding.tvTabDescription.text = "Виджет 4×2: выберите несколько устройств из разных комнат и показатели для каждого."
                    binding.layoutRoomSpinner.visibility = View.GONE
                }
                WidgetConfigTab.W4X1 -> {
                    binding.tvTabDescription.text = "Виджет 4×1 (Полоса): выберите устройства из разных комнат для компактной строки."
                    binding.layoutRoomSpinner.visibility = View.GONE
                }
                WidgetConfigTab.W2X2 -> {
                    binding.tvTabDescription.text = "Виджет 2×2 (Климат): выберите комнату или конкретный датчик."
                    binding.layoutRoomSpinner.visibility = View.VISIBLE
                }
                WidgetConfigTab.W2X1 -> {
                    binding.tvTabDescription.text = "Виджет 2×1 (Мини): узкая плитка температуры и влажности без наслоений."
                    binding.layoutRoomSpinner.visibility = View.VISIBLE
                }
            }
            renderDeviceListForCurrentTab()
        }

        binding.btnTab4x2.setOnClickListener {
            currentTab = WidgetConfigTab.W4X2
            updateTabStyles()
        }
        binding.btnTab4x1.setOnClickListener {
            currentTab = WidgetConfigTab.W4X1
            updateTabStyles()
        }
        binding.btnTab2x2.setOnClickListener {
            currentTab = WidgetConfigTab.W2X2
            updateTabStyles()
        }
        binding.btnTab2x1.setOnClickListener {
            currentTab = WidgetConfigTab.W2X1
            updateTabStyles()
        }

        updateTabStyles()
    }

    private fun renderSettings() {
        binding.cbShowHumidity.isChecked = dataManager.showHumidity
        binding.cbShowPressure.isChecked = dataManager.showPressure
        binding.cbShowDoor.isChecked = dataManager.showDoor
        binding.cbShowBattery.isChecked = dataManager.showBattery
        setupRoomsSpinner()
    }

    private fun setupRoomsSpinner() {
        availableRooms = dataManager.getAvailableRooms()
        val roomTitles = mutableListOf("Все комнаты")
        roomTitles.addAll(availableRooms.map { it.name })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roomTitles)
        binding.spRoomsList.adapter = adapter

        val curRoomId = when (currentTab) {
            WidgetConfigTab.W2X2 -> dataManager.widget2x2RoomId
            WidgetConfigTab.W2X1 -> dataManager.widget2x1RoomId
            else -> dataManager.selectedRoomId
        }

        if (!curRoomId.isNullOrBlank() && curRoomId != "ALL") {
            val index = availableRooms.indexOfFirst { it.id == curRoomId }
            if (index >= 0) {
                binding.spRoomsList.setSelection(index + 1)
            }
        } else {
            binding.spRoomsList.setSelection(0)
        }

        binding.spRoomsList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedId = if (position == 0) "ALL" else availableRooms.getOrNull(position - 1)?.id
                when (currentTab) {
                    WidgetConfigTab.W2X2 -> dataManager.widget2x2RoomId = selectedId
                    WidgetConfigTab.W2X1 -> dataManager.widget2x1RoomId = selectedId
                    else -> dataManager.selectedRoomId = selectedId
                }
                renderDeviceListForCurrentTab()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        renderDeviceListForCurrentTab()
    }

    private class DeviceItemViewHolder(
        val device: Device,
        val cbEnable: CheckBox,
        val cbTemp: CheckBox?,
        val cbHum: CheckBox?,
        val cbPress: CheckBox?,
        val cbDoor: CheckBox?,
        val cbBat: CheckBox?
    )

    private val deviceViewHolders = mutableListOf<DeviceItemViewHolder>()

    private fun renderDeviceListForCurrentTab() {
        binding.layoutDevicesList.removeAllViews()
        deviceViewHolders.clear()

        val allDevices = if (currentTab == WidgetConfigTab.W2X2 || currentTab == WidgetConfigTab.W2X1) {
            val targetRoom = if (currentTab == WidgetConfigTab.W2X2) dataManager.widget2x2RoomId else dataManager.widget2x1RoomId
            dataManager.getDevicesForRoom(targetRoom)
        } else {
            // For 4x2 and 4x1: multiple devices across all rooms!
            dataManager.getAllRelevantDevices()
        }

        if (allDevices.isEmpty()) {
            binding.tvDevicesHeader.text = "Датчики не обнаружены. Нажмите «Синхронизировать датчики»."
            return
        }

        binding.tvDevicesHeader.text = "Устройства умного дома и показатели:"

        val activeSelectedIds = when (currentTab) {
            WidgetConfigTab.W4X2 -> dataManager.widget4x2DeviceIds
            WidgetConfigTab.W4X1 -> dataManager.widget4x1DeviceIds
            WidgetConfigTab.W2X2 -> setOfNotNull(dataManager.widget2x2DeviceId)
            WidgetConfigTab.W2X1 -> setOfNotNull(dataManager.widget2x1DeviceId)
        }

        for (dev in allDevices) {
            val rName = availableRooms.firstOrNull { it.id == dev.room }?.name ?: ""
            val title = if (rName.isNotBlank()) "${dev.name} ($rName)" else dev.name

            val isDeviceSelected = activeSelectedIds.isEmpty() || activeSelectedIds.contains(dev.id)
            val metricPref = dataManager.getDeviceMetricSetting(dev.id)

            // Card container for device
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_tile_alice)
                setPadding(24, 20, 24, 20)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 12
                }
            }

            // Master Checkbox: Device Enable
            val cbDevice = CheckBox(this).apply {
                text = title
                setTextColor(getColor(R.color.alice_text_white))
                textSize = 13f
                isChecked = isDeviceSelected
            }
            card.addView(cbDevice)

            // Sub-metrics layout
            val metricsRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(32, 4, 0, 0)
            }

            var cbTemp: CheckBox? = null
            var cbHum: CheckBox? = null
            var cbPress: CheckBox? = null
            var cbDoor: CheckBox? = null
            var cbBat: CheckBox? = null

            // Temperature metric checkbox
            if (dev.getTemperature() != null || dev.isClimateSensor()) {
                cbTemp = CheckBox(this).apply {
                    text = "Темп."
                    setTextColor(getColor(R.color.alice_text_light))
                    textSize = 11f
                    isChecked = metricPref.showTemperature
                }
                metricsRow.addView(cbTemp)
            }

            // Humidity metric checkbox
            if (dev.getHumidity() != null || dev.isClimateSensor()) {
                cbHum = CheckBox(this).apply {
                    text = "Влажн."
                    setTextColor(getColor(R.color.alice_text_light))
                    textSize = 11f
                    isChecked = metricPref.showHumidity
                }
                metricsRow.addView(cbHum)
            }

            // Pressure metric checkbox
            if (dev.getPressure() != null) {
                cbPress = CheckBox(this).apply {
                    text = "Давл."
                    setTextColor(getColor(R.color.alice_text_light))
                    textSize = 11f
                    isChecked = metricPref.showPressure
                }
                metricsRow.addView(cbPress)
            }

            // Door metric checkbox
            if (dev.isOpenSensor() || dev.isDoorOrWindowOpen() != null) {
                cbDoor = CheckBox(this).apply {
                    text = "Дверь"
                    setTextColor(getColor(R.color.alice_text_light))
                    textSize = 11f
                    isChecked = metricPref.showDoor
                }
                metricsRow.addView(cbDoor)
            }

            // Battery metric checkbox
            if (dev.getBattery() != null) {
                cbBat = CheckBox(this).apply {
                    text = "Батарея"
                    setTextColor(getColor(R.color.alice_text_light))
                    textSize = 11f
                    isChecked = metricPref.showBattery
                }
                metricsRow.addView(cbBat)
            }

            if (metricsRow.childCount > 0) {
                card.addView(metricsRow)
            }

            binding.layoutDevicesList.addView(card)
            deviceViewHolders.add(DeviceItemViewHolder(dev, cbDevice, cbTemp, cbHum, cbPress, cbDoor, cbBat))
        }
    }

    private fun saveWidgetPreferences() {
        dataManager.showHumidity = binding.cbShowHumidity.isChecked
        dataManager.showPressure = binding.cbShowPressure.isChecked
        dataManager.showDoor = binding.cbShowDoor.isChecked
        dataManager.showBattery = binding.cbShowBattery.isChecked

        val selectedIds = mutableSetOf<String>()

        for (holder in deviceViewHolders) {
            val devId = holder.device.id
            if (holder.cbEnable.isChecked) {
                selectedIds.add(devId)
            }

            val curPref = dataManager.getDeviceMetricSetting(devId)
            val updatedPref = WidgetDataManager.DeviceMetricSetting(
                showTemperature = holder.cbTemp?.isChecked ?: curPref.showTemperature,
                showHumidity = holder.cbHum?.isChecked ?: curPref.showHumidity,
                showPressure = holder.cbPress?.isChecked ?: curPref.showPressure,
                showDoor = holder.cbDoor?.isChecked ?: curPref.showDoor,
                showBattery = holder.cbBat?.isChecked ?: curPref.showBattery
            )
            dataManager.saveDeviceMetricSetting(devId, updatedPref)
        }

        when (currentTab) {
            WidgetConfigTab.W4X2 -> {
                dataManager.widget4x2DeviceIds = selectedIds
            }
            WidgetConfigTab.W4X1 -> {
                dataManager.widget4x1DeviceIds = selectedIds
            }
            WidgetConfigTab.W2X2 -> {
                dataManager.widget2x2DeviceId = selectedIds.firstOrNull()
            }
            WidgetConfigTab.W2X1 -> {
                dataManager.widget2x1DeviceId = selectedIds.firstOrNull()
            }
        }

        if (selectedIds.isNotEmpty()) {
            dataManager.enabledDeviceIds = selectedIds
        }

        SensorSyncWorker.notifyWidgetsToUpdate(this)
        Toast.makeText(this, "✅ Настройки сохранены! Виджеты обновлены.", Toast.LENGTH_SHORT).show()
    }

    private fun pinWidget(providerClass: Class<*>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = getSystemService(AppWidgetManager::class.java)
            if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                val provider = ComponentName(this, providerClass)
                appWidgetManager.requestPinAppWidget(provider, null, null)
                Toast.makeText(this, "Подтвердите добавление на рабочий стол", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Ваш лаунчер не поддерживает быстрое добавление. Зажмите рабочий стол и выберите виджет.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Зажмите свободное место на рабочем столе и выберите виджет из списка.", Toast.LENGTH_LONG).show()
        }
    }

    private fun openInAppAuth(clientId: String) {
        val dialog = Dialog(this, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // Top Header in Dialog
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 24, 32, 24)
            setBackgroundColor(getColor(R.color.alice_purple_dark))
        }

        val titleTv = TextView(this).apply {
            text = "Вход в Яндекс"
            setTextColor(getColor(R.color.alice_text_white))
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeTv = TextView(this).apply {
            text = "✕ Закрыть"
            setTextColor(getColor(R.color.alice_neon_light))
            textSize = 14f
            setOnClickListener { dialog.dismiss() }
        }

        topBar.addView(titleTv)
        topBar.addView(closeTv)
        root.addView(topBar)

        val webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
        }

        val authUrl = "https://oauth.yandex.ru/authorize?response_type=token&client_id=$clientId"

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let { checkAndExtractToken(it, dialog) }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: ""
                return checkAndExtractToken(url, dialog)
            }
        }

        root.addView(webView)
        dialog.setContentView(root)
        dialog.show()

        webView.loadUrl(authUrl)
    }

    private fun checkAndExtractToken(url: String, dialog: Dialog): Boolean {
        if (url.contains("access_token=")) {
            val tokenRegex = "access_token=([^&]+)".toRegex()
            val match = tokenRegex.find(url)
            val token = match?.groupValues?.get(1)
            if (!token.isNullOrBlank()) {
                dialog.dismiss()
                dataManager.oauthToken = token
                binding.etOauthToken.setText(token)
                Toast.makeText(this@MainActivity, "✅ Токен успешно получен и сохранен!", Toast.LENGTH_SHORT).show()
                performSync(token)
                return true
            }
        }
        return false
    }

    private fun performSync(token: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnTestSync.isEnabled = false
        binding.tvStatusResult.text = "Связь с сервером Умного дома Яндекса..."

        lifecycleScope.launch {
            val result = apiClient.fetchUserInfo(token)
            binding.progressBar.visibility = View.GONE
            binding.btnTestSync.isEnabled = true

            result.onSuccess { info ->
                dataManager.saveUserInfo(info)
                (application as? AliceApplication)?.scheduleSensorSync()
                SensorSyncWorker.notifyWidgetsToUpdate(this@MainActivity)

                setupRoomsSpinner()
                renderDeviceListForCurrentTab()
                renderCachedStatus()
                Toast.makeText(this@MainActivity, "Виджеты успешно обновлены!", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                binding.tvStatusResult.text = "❌ " + (error.localizedMessage ?: "Ошибка связи")
                binding.cardStatus.visibility = View.VISIBLE
            }
        }
    }

    private fun renderCachedStatus() {
        val aggregated = dataManager.getAggregatedSensorData()
        val allDevices = dataManager.getAllRelevantDevices()

        if (dataManager.lastSyncTimestamp > 0) {
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
            val dateStr = sdf.format(Date(dataManager.lastSyncTimestamp))
            binding.cardStatus.visibility = View.VISIBLE
            binding.tvStatusResult.text = "✅ Данные получены: $dateStr\n" +
                    "Дом: ${aggregated.householdName}\n" +
                    "Комната: ${aggregated.primaryRoomName}\n" +
                    "Найдено устройств: ${allDevices.size}\n" +
                    "Температура: ${aggregated.primaryTemperature?.let { "$it°C" } ?: "--"}\n" +
                    (if (dataManager.showHumidity && aggregated.primaryHumidity != null) "Влажность: ${aggregated.primaryHumidity}%\n" else "") +
                    (if (dataManager.showPressure && aggregated.primaryPressure != null) "Давление: ${aggregated.primaryPressure} мм рт. ст.\n" else "") +
                    "Датчик дверей: ${if (aggregated.isAnyDoorOpen) "ОТКРЫТО" else "Закрыто"}"
        } else {
            binding.cardStatus.visibility = View.GONE
        }
    }
}
