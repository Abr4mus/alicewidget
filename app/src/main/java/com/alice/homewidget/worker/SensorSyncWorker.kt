package com.alice.homewidget.worker

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alice.homewidget.api.YandexApiClient
import com.alice.homewidget.data.WidgetDataManager
import com.alice.homewidget.widget.AliceBarWidgetProvider
import com.alice.homewidget.widget.AliceCompact2x1WidgetProvider
import com.alice.homewidget.widget.AliceCompactWidgetProvider
import com.alice.homewidget.widget.AliceHomeWidgetProvider

class SensorSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val apiClient = YandexApiClient()
    private val dataManager = WidgetDataManager(context)

    override suspend fun doWork(): Result {
        val token = dataManager.oauthToken
        if (token.isBlank()) {
            return Result.success()
        }

        val result = apiClient.fetchUserInfo(token)
        return if (result.isSuccess) {
            val userInfo = result.getOrNull()
            if (userInfo != null) {
                dataManager.saveUserInfo(userInfo)
                notifyWidgetsToUpdate(context)
            }
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        fun notifyWidgetsToUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)

            // Update 4x2
            val mainIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, AliceHomeWidgetProvider::class.java)
            )
            if (mainIds.isNotEmpty()) {
                val intent = Intent(context, AliceHomeWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, mainIds)
                }
                context.sendBroadcast(intent)
            }

            // Update 2x2
            val compactIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, AliceCompactWidgetProvider::class.java)
            )
            if (compactIds.isNotEmpty()) {
                val intent = Intent(context, AliceCompactWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, compactIds)
                }
                context.sendBroadcast(intent)
            }

            // Update 2x1
            val compact2x1Ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, AliceCompact2x1WidgetProvider::class.java)
            )
            if (compact2x1Ids.isNotEmpty()) {
                val intent = Intent(context, AliceCompact2x1WidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, compact2x1Ids)
                }
                context.sendBroadcast(intent)
            }

            // Update 4x1
            val barIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, AliceBarWidgetProvider::class.java)
            )
            if (barIds.isNotEmpty()) {
                val intent = Intent(context, AliceBarWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, barIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
