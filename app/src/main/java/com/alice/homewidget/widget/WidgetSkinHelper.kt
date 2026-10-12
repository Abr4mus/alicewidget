package com.alice.homewidget.widget

import com.alice.homewidget.R
import com.alice.homewidget.data.WidgetDataManager

object WidgetSkinHelper {

    fun getWidgetBackground(theme: String): Int = when (theme) {
        WidgetDataManager.THEME_IOS -> R.drawable.bg_skin_ios
        WidgetDataManager.THEME_ANDROID -> R.drawable.bg_skin_android
        else -> R.drawable.bg_skin_alice
    }

    fun getTileBackground(theme: String): Int = when (theme) {
        WidgetDataManager.THEME_IOS -> R.drawable.bg_tile_ios
        WidgetDataManager.THEME_ANDROID -> R.drawable.bg_tile_android
        else -> R.drawable.bg_tile_alice
    }

    fun getBtnBackground(theme: String): Int = when (theme) {
        WidgetDataManager.THEME_IOS -> R.drawable.bg_btn_ios
        WidgetDataManager.THEME_ANDROID -> R.drawable.bg_btn_android
        else -> R.drawable.bg_btn_alice
    }

    fun getTextColorPrimary(theme: String): Int = when (theme) {
        WidgetDataManager.THEME_IOS -> 0xFFF5F5F7.toInt()
        WidgetDataManager.THEME_ANDROID -> 0xFFF0F4F8.toInt()
        else -> 0xFFFFFFFF.toInt()
    }

    fun getTextColorSecondary(theme: String): Int = when (theme) {
        WidgetDataManager.THEME_IOS -> 0xFF8E8E93.toInt()
        WidgetDataManager.THEME_ANDROID -> 0xFF9BB0C1.toInt()
        else -> 0xFFA793D0.toInt()
    }

    fun getAccentHumidityColor(theme: String): Int = when (theme) {
        WidgetDataManager.THEME_IOS -> 0xFF64D2FF.toInt()
        WidgetDataManager.THEME_ANDROID -> 0xFF7BD5B5.toInt()
        else -> 0xFF38BDF8.toInt()
    }
}
