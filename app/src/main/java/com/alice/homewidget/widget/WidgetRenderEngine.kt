package com.alice.homewidget.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import com.alice.homewidget.R
import com.alice.homewidget.data.WidgetDataManager

object WidgetRenderEngine {

    /**
     * Renders a high-resolution, anti-aliased Comfort Gauge Ring
     * in Xiaomi HyperOS, Apple iOS, or Yandex Alice style.
     */
    fun renderComfortGauge(temp: Double?, humidity: Double?, theme: String, sizePx: Int = 120): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val strokeWidth = sizePx * 0.085f
        val padding = strokeWidth * 1.2f
        val rect = RectF(padding, padding, sizePx - padding, sizePx - padding)

        // 1. Background Arc (Track)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = when (theme) {
                WidgetDataManager.THEME_XIAOMI -> Color.parseColor("#26FFFFFF")
                WidgetDataManager.THEME_IOS -> Color.parseColor("#1FFFFFFF")
                else -> Color.parseColor("#268B5CF6")
            }
        }
        canvas.drawArc(rect, 135f, 270f, false, trackPaint)

        // 2. Compute Level (0.0 to 1.0)
        val currentTemp = temp ?: 22.0
        // Normal indoor temperature span 15°C to 30°C
        val ratio = ((currentTemp - 15.0) / 15.0).coerceIn(0.05, 1.0).toFloat()
        val sweepAngle = 270f * ratio

        // 3. Dynamic Accent Colors based on Comfort Range
        val activeColor = when {
            currentTemp < 20.0 -> when (theme) {
                WidgetDataManager.THEME_XIAOMI -> Color.parseColor("#38BDF8") // Cool Cyan
                WidgetDataManager.THEME_IOS -> Color.parseColor("#007AFF")    // Apple Blue
                else -> Color.parseColor("#38BDF8")
            }
            currentTemp in 20.0..24.5 -> when (theme) {
                WidgetDataManager.THEME_XIAOMI -> Color.parseColor("#00D2B4") // HyperOS Comfort Teal
                WidgetDataManager.THEME_IOS -> Color.parseColor("#34C759")    // Apple Comfort Green
                else -> Color.parseColor("#C77DFF")                           // Alice Glow
            }
            else -> when (theme) {
                WidgetDataManager.THEME_XIAOMI -> Color.parseColor("#FF6900") // HyperOS Warm Orange
                WidgetDataManager.THEME_IOS -> Color.parseColor("#FF9500")    // Apple Amber
                else -> Color.parseColor("#FF6B8B")
            }
        }

        // 4. Draw Active Glowing Arc
        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = activeColor
        }
        canvas.drawArc(rect, 135f, sweepAngle, false, activePaint)

        // 5. Draw End Indicator Dot with Soft Ambient Halo
        val endAngleRad = Math.toRadians((135f + sweepAngle).toDouble())
        val radius = (sizePx - padding * 2) / 2f
        val centerX = sizePx / 2f
        val centerY = sizePx / 2f
        val dotX = (centerX + radius * Math.cos(endAngleRad)).toFloat()
        val dotY = (centerY + radius * Math.sin(endAngleRad)).toFloat()

        // Glow Halo
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(80, Color.red(activeColor), Color.green(activeColor), Color.blue(activeColor))
        }
        canvas.drawCircle(dotX, dotY, strokeWidth * 0.9f, haloPaint)

        // White Center Core
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        canvas.drawCircle(dotX, dotY, strokeWidth * 0.45f, dotPaint)

        return bitmap
    }

    fun getThemeBackground(theme: String): Int = when (theme) {
        WidgetDataManager.THEME_XIAOMI -> R.drawable.bg_skin_xiaomi
        WidgetDataManager.THEME_IOS -> R.drawable.bg_skin_ios
        else -> R.drawable.bg_skin_alice
    }

    fun getTileBackground(theme: String): Int = when (theme) {
        WidgetDataManager.THEME_XIAOMI -> R.drawable.bg_tile_xiaomi
        WidgetDataManager.THEME_IOS -> R.drawable.bg_tile_ios
        else -> R.drawable.bg_tile_alice
    }

    fun getButtonBackground(theme: String): Int = when (theme) {
        WidgetDataManager.THEME_XIAOMI -> R.drawable.bg_btn_xiaomi
        WidgetDataManager.THEME_IOS -> R.drawable.bg_btn_ios
        else -> R.drawable.bg_btn_alice
    }

    fun getPillBackground(theme: String): Int = when (theme) {
        WidgetDataManager.THEME_XIAOMI -> R.drawable.bg_pill_xiaomi
        WidgetDataManager.THEME_IOS -> R.drawable.bg_tile_ios
        else -> R.drawable.bg_tile_alice
    }

    fun getTextPrimaryColor(theme: String): Int = when (theme) {
        WidgetDataManager.THEME_XIAOMI -> 0xFFFFFFFF.toInt()
        WidgetDataManager.THEME_IOS -> 0xFFF5F5F7.toInt()
        else -> 0xFFFFFFFF.toInt()
    }

    fun getTextSecondaryColor(theme: String): Int = when (theme) {
        WidgetDataManager.THEME_XIAOMI -> 0xFF9DA3AE.toInt() // HyperOS Slate
        WidgetDataManager.THEME_IOS -> 0xFF8E8E93.toInt()    // iOS System Gray
        else -> 0xFFA793D0.toInt()                           // Alice Lavender
    }

    fun getAccentColor(theme: String): Int = when (theme) {
        WidgetDataManager.THEME_XIAOMI -> 0xFF00D2B4.toInt() // HyperOS Teal
        WidgetDataManager.THEME_IOS -> 0xFF007AFF.toInt()    // iOS Blue
        else -> 0xFF9D4EDD.toInt()                           // Alice Violet
    }

    fun getHumidityColor(theme: String): Int = when (theme) {
        WidgetDataManager.THEME_XIAOMI -> 0xFF38BDF8.toInt()
        WidgetDataManager.THEME_IOS -> 0xFF64D2FF.toInt()
        else -> 0xFF38BDF8.toInt()
    }
}
