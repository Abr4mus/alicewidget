package com.alice.homewidget

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.alice.homewidget.data.WidgetDataManager
import com.alice.homewidget.worker.SensorSyncWorker
import java.util.concurrent.TimeUnit

class AliceApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        scheduleSensorSync()
    }

    fun scheduleSensorSync() {
        val dataManager = WidgetDataManager(this)
        val intervalMinutes = dataManager.syncIntervalMinutes.coerceAtLeast(15)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<SensorSyncWorker>(
            intervalMinutes.toLong(),
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_NAME_SYNC,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    }

    companion object {
        const val WORK_NAME_SYNC = "alice_sensor_periodic_sync"
    }
}
