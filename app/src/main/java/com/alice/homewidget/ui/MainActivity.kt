package com.alice.homewidget.ui

import android.app.Dialog
import android.appwidget.AppWidgetManager
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alice.homewidget.AliceApplication
import com.alice.homewidget.R
import com.alice.homewidget.api.YandexApiClient
import com.alice.homewidget.data.WidgetDataManager
import com.alice.homewidget.databinding.ActivityMainBinding
import com.alice.homewidget.model.Device
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

    private class DeviceMetricsHolder(
        val device: Device,
        val cbTemp: CheckBox?,
        val cbHum: CheckBox?,
        val cbPress: CheckBox?,
        val cbDoor: CheckBox?,
        val cbBat: CheckBox?
    )

    private val deviceHolders = mutableListOf<DeviceMetricsHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataManager = WidgetDataManager(this)

        initViews()
        initThemeSelector()
        renderActiveWidgets()
        renderDevicesList()
        renderCachedStatus()
    }

    override fun onResume() {
        super.onResume()
        renderActiveWidgets()
        renderDevicesList()
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

        // Save device metrics
        binding.btnSaveDeviceMetrics.setOnClickListener {
            saveDeviceMetrics()
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
        when (dataManager.defaultTheme) {
            WidgetDataManager.THEME_IOS -> binding.rbThemeIos.isChecked = true
            WidgetDataManager.THEME_ALICE -> binding.rbThemeAlice.isChecked = true
            WidgetDataManager.THEME_ANDROID -> binding.rbThemeAndroid.isChecked = true
            else -> binding.rbThemeXiaomi.isChecked = true
        }

        binding.rgThemeSelector.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.rbThemeIos -> WidgetDataManager.THEME_IOS
                R.id.rbThemeAlice -> WidgetDataManager.THEME_ALICE
                R.id.rbThemeAndroid -> WidgetDataManager.THEME_ANDROID
                else -> WidgetDataManager.THEME_XIAOMI
            }
            dataManager.defaultTheme = theme
            val name = when (theme) {
                WidgetDataManager.THEME_IOS -> "iPhone / iOS"
                WidgetDataManager.THEME_ALICE -> "Яндекс / Алиса"
                WidgetDataManager.THEME_ANDROID -> "Android Material You"
                else -> "Xiaomi HyperOS"
            }
            Toast.makeText(this, "Стиль по умолчанию: $name", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderActiveWidgets() {
        binding.layoutActiveWidgetsList.removeAllViews()

        val appWidgetManager = AppWidgetManager.getInstance(this)
        val p4x2 = appWidgetManager.getAppWidgetIds(ComponentName(this, AliceHomeWidgetProvider::class.java))
        val p4x1 = appWidgetManager.getAppWidgetIds(ComponentName(this, AliceBarWidgetProvider::class.java))
        val p2x2 = appWidgetManager.getAppWidgetIds(ComponentName(this, AliceCompactWidgetProvider::class.java))
        val p2x1 = appWidgetManager.getAppWidgetIds(ComponentName(this, AliceCompact2x1WidgetProvider::class.java))

        val allPlaced = mutableListOf<Pair<Int, String>>()
        p4x2.forEach { allPlaced.add(it to "4 × 2 (Главный смарт-хаб)") }
        p4x1.forEach { allPlaced.add(it to "4 × 1 (Инфо-полоса)") }
        p2x2.forEach { allPlaced.add(it to "2 × 2 (Климат)") }
        p2x1.forEach { allPlaced.add(it to "2 × 1 (Мини-климат)") }

        if (allPlaced.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "На рабочем столе пока нет добавленных виджетов. Нажмите «+ Добавить» ниже, чтобы разместить виджет."
                setTextColor(getColor(R.color.alice_text_muted))
                textSize = 12f
                setPadding(8, 8, 8, 8)
            }
            binding.layoutActiveWidgetsList.addView(emptyTv)
            return
        }

        for ((widgetId, formatName) in allPlaced) {
            val config = dataManager.getWidgetConfig(widgetId)
            val themeName = when (config.theme) {
                WidgetDataManager.THEME_IOS -> "Стиль iOS"
                WidgetDataManager.THEME_ALICE -> "Стиль Алиса"
                WidgetDataManager.THEME_ANDROID -> "Стиль Android"
                else -> "Стиль Xiaomi"
            }

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundResource(R.drawable.bg_tile_xiaomi)
                setPadding(20, 16, 20, 16)
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 10
                }
            }

            val infoLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val titleTv = TextView(this).apply {
                text = "Виджет #${widgetId} • $formatName"
                setTextColor(getColor(R.color.white))
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val subTv = TextView(this).apply {
                val devCount = if (config.selectedDeviceIds.isEmpty()) "Все датчики" else "${config.selectedDeviceIds.size} устройств"
                text = "$themeName • $devCount"
                setTextColor(getColor(R.color.alice_text_muted))
                textSize = 11f
            }

            infoLayout.addView(titleTv)
            infoLayout.addView(subTv)
            card.addView(infoLayout)

            val configBtn = Button(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = "⚙️ Настроить"
                textSize = 11f
                setTextColor(getColor(R.color.alice_neon_light))
                setOnClickListener {
                    val intent = Intent(this@MainActivity, WidgetConfigureActivity::class.java).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    }
                    startActivity(intent)
                }
            }
            card.addView(configBtn)

            binding.layoutActiveWidgetsList.addView(card)
        }
    }

    private fun renderDevicesList() {
        binding.layoutMainDevicesList.removeAllViews()
        deviceHolders.clear()

        val allDevices = dataManager.getAllRelevantDevices()
        val rooms = dataManager.getAvailableRooms()

        if (allDevices.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "Датчики ещё не загружены. Нажмите «Синхронизировать датчики» выше."
                setTextColor(getColor(R.color.alice_text_muted))
                textSize = 12f
                setPadding(8, 8, 8, 8)
            }
            binding.layoutMainDevicesList.addView(emptyTv)
            return
        }

        for (dev in allDevices) {
            val rName = rooms.firstOrNull { it.id == dev.room }?.name ?: ""
            val title = if (rName.isNotBlank()) "${dev.name} ($rName)" else dev.name
            val metricPref = dataManager.getDeviceMetricSetting(dev.id)

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_tile_xiaomi)
                setPadding(22, 18, 22, 18)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 12
                }
            }

            val titleTv = TextView(this).apply {
                text = title
                setTextColor(getColor(R.color.white))
                textSize = 13.5f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            card.addView(titleTv)

            val metricsRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 6, 0, 0)
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

            // 3. Pressure
            if (dev.getPressure() != null) {
                cbPress = CheckBox(this).apply {
                    text = "Давл."
                    setTextColor(getColor(R.color.alice_text_light))
                    textSize = 11.5f
                    isChecked = metricPref.showPressure
                }
                metricsRow.addView(cbPress)
            }

            // 4. Door
            if (dev.isOpenSensor() || dev.isDoorOrWindowOpen() != null) {
                cbDoor = CheckBox(this).apply {
                    text = "Дверь"
                    setTextColor(getColor(R.color.alice_text_light))
                    textSize = 11.5f
                    isChecked = metricPref.showDoor
                }
                metricsRow.addView(cbDoor)
            }

            // 5. Battery
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

            binding.layoutMainDevicesList.addView(card)
            deviceHolders.add(DeviceMetricsHolder(dev, cbTemp, cbHum, cbPress, cbDoor, cbBat))
        }
    }

    private fun saveDeviceMetrics() {
        for (holder in deviceHolders) {
            val devId = holder.device.id
            val cur = dataManager.getDeviceMetricSetting(devId)
            val updated = WidgetDataManager.DeviceMetricSetting(
                showTemperature = holder.cbTemp?.isChecked ?: cur.showTemperature,
                showHumidity = holder.cbHum?.isChecked ?: cur.showHumidity,
                showPressure = holder.cbPress?.isChecked ?: cur.showPressure,
                showDoor = holder.cbDoor?.isChecked ?: cur.showDoor,
                showLeak = cur.showLeak,
                showBattery = holder.cbBat?.isChecked ?: cur.showBattery
            )
            dataManager.saveDeviceMetricSetting(devId, updated)
        }

        SensorSyncWorker.notifyWidgetsToUpdate(this)
        Toast.makeText(this, "✅ Показатели устройств сохранены и применены!", Toast.LENGTH_SHORT).show()
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

                renderActiveWidgets()
                renderDevicesList()
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
                    "Влажность: ${aggregated.primaryHumidity?.let { "$it%" } ?: "--"}"
        } else {
            binding.cardStatus.visibility = View.GONE
        }
    }
}
