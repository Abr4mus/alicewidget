package com.alice.homewidget.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alice.homewidget.AliceApplication
import com.alice.homewidget.api.YandexApiClient
import com.alice.homewidget.data.WidgetDataManager
import com.alice.homewidget.databinding.ActivityMainBinding
import com.alice.homewidget.worker.SensorSyncWorker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dataManager: WidgetDataManager
    private val apiClient = YandexApiClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataManager = WidgetDataManager(this)

        initViews()
        renderCachedStatus()
    }

    private fun initViews() {
        binding.etOauthToken.setText(dataManager.oauthToken)

        // Paste from clipboard
        binding.btnPasteToken.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val item = clipboard.primaryClip?.getItemAt(0)
            val text = item?.text?.toString()?.trim()
            if (!text.isNullOrBlank()) {
                binding.etOauthToken.setText(text)
                Toast.makeText(this, "Токен вставлен из буфера", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Буфер обмена пуст", Toast.LENGTH_SHORT).show()
            }
        }

        // Help dialog or browser link
        binding.btnTokenHelp.setOnClickListener {
            val url = "https://oauth.yandex.ru/client/new"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        // Test & Sync Button
        binding.btnTestSync.setOnClickListener {
            val token = binding.etOauthToken.text.toString().trim()
            if (token.isBlank()) {
                Toast.makeText(this, "Пожалуйста, введите токен Яндекса", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dataManager.oauthToken = token
            performSync(token)
        }

        // Open official Yandex app
        binding.btnOpenYandexApp.setOnClickListener {
            val launchIntent = packageManager.getLaunchIntentForPackage("com.yandex.iot")
                ?: packageManager.getLaunchIntentForPackage("ru.yandex.searchplugin")
            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.yandex.iot"))
                try {
                    startActivity(marketIntent)
                } catch (e: Exception) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.yandex.iot")))
                }
            }
        }
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

                renderCachedStatus()
                Toast.makeText(this@MainActivity, "Виджет успешно обновлен!", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                binding.tvStatusResult.text = "❌ " + (error.localizedMessage ?: "Ошибка связи")
                binding.cardStatus.visibility = View.VISIBLE
            }
        }
    }

    private fun renderCachedStatus() {
        val aggregated = dataManager.getAggregatedSensorData()
        if (dataManager.lastSyncTimestamp > 0) {
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
            val dateStr = sdf.format(Date(dataManager.lastSyncTimestamp))
            binding.cardStatus.visibility = View.VISIBLE
            binding.tvStatusResult.text = "✅ Данные актуальны на: $dateStr\n" +
                    "Дом: ${aggregated.householdName}\n" +
                    "Найдено датчиков: ${aggregated.totalSensorCount}\n" +
                    "Температура (${aggregated.primaryRoomName}): ${aggregated.primaryTemperature?.let { "$it°C" } ?: "--"}\n" +
                    "Влажность: ${aggregated.primaryHumidity?.let { "$it%" } ?: "--"}\n" +
                    "Датчик дверей: ${if (aggregated.isAnyDoorOpen) "ОТКРЫТО" else "Все закрыто"}"
        } else {
            binding.cardStatus.visibility = View.GONE
        }
    }
}
