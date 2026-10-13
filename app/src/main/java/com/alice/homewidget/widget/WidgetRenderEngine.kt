package com.alice.homewidget.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.alice.homewidget.R
import com.alice.homewidget.data.WidgetDataManager
import com.alice.homewidget.model.AggregatedSensorData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object WidgetRenderEngine {

    data class ThemePalette(
        val bgStart: Int,
        val bgEnd: Int,
        val borderTop: Int,
        val borderBottom: Int,
        val tileBg: Int,
        val tileBorder: Int,
        val textPrimary: Int,
        val textSecondary: Int,
        val accentComfort: Int,
        val accentWarm: Int,
        val accentCold: Int,
        val alertRed: Int,
        val okGreen: Int,
        val cornerRadiusDp: Float = 26f
    )

    fun getThemePalette(theme: String): ThemePalette = when (theme) {
        WidgetDataManager.THEME_XIAOMI -> ThemePalette(
            bgStart = 0xEE16191E.toInt(),
            bgEnd = 0xF60D0E12.toInt(),
            borderTop = 0x50FFFFFF.toInt(),
            borderBottom = 0x15FFFFFF.toInt(),
            tileBg = 0x1AFFFFFF.toInt(),
            tileBorder = 0x22FFFFFF.toInt(),
            textPrimary = 0xFFFFFFFF.toInt(),
            textSecondary = 0xFF9DA3AE.toInt(), // HyperOS Slate
            accentComfort = 0xFF00D2B4.toInt(), // HyperOS Comfort Teal
            accentWarm = 0xFFFF6900.toInt(),    // HyperOS Warm Orange
            accentCold = 0xFF38BDF8.toInt(),    // Cool Cyan
            alertRed = 0xFFFF453A.toInt(),
            okGreen = 0xFF00D2B4.toInt()
        )
        WidgetDataManager.THEME_IOS -> ThemePalette(
            bgStart = 0xEE222228.toInt(),
            bgEnd = 0xF6131318.toInt(),
            borderTop = 0x60FFFFFF.toInt(),
            borderBottom = 0x20FFFFFF.toInt(),
            tileBg = 0x22FFFFFF.toInt(),
            tileBorder = 0x2AFFFFFF.toInt(),
            textPrimary = 0xFFF5F5F7.toInt(), // SF Pro White
            textSecondary = 0xFF8E8E93.toInt(), // iOS System Gray
            accentComfort = 0xFF34C759.toInt(), // Apple Green
            accentWarm = 0xFFFF9500.toInt(),    // Apple Amber
            accentCold = 0xFF007AFF.toInt(),    // Apple Blue
            alertRed = 0xFFFF3B30.toInt(),
            okGreen = 0xFF34C759.toInt()
        )
        WidgetDataManager.THEME_ALICE -> ThemePalette(
            bgStart = 0xEE1F133D.toInt(),
            bgEnd = 0xF6100824.toInt(),
            borderTop = 0x859D74FF.toInt(), // Lilac Neon Glow
            borderBottom = 0x289D74FF.toInt(),
            tileBg = 0x258B5CF6.toInt(),
            tileBorder = 0x409D74FF.toInt(),
            textPrimary = 0xFFFFFFFF.toInt(),
            textSecondary = 0xFFC7B6ED.toInt(), // Lavender
            accentComfort = 0xFFC77DFF.toInt(), // Alice Glow
            accentWarm = 0xFFFF6B8B.toInt(),
            accentCold = 0xFF38BDF8.toInt(),
            alertRed = 0xFFFF4D6D.toInt(),
            okGreen = 0xFF50FA7B.toInt()
        )
        else -> ThemePalette( // THEME_ANDROID (Material You)
            bgStart = 0xF01B1D20.toInt(),
            bgEnd = 0xF6111215.toInt(),
            borderTop = 0x4582D9BC.toInt(), // Mint Highlight
            borderBottom = 0x1582D9BC.toInt(),
            tileBg = 0x1FFFFFFF.toInt(),
            tileBorder = 0x25FFFFFF.toInt(),
            textPrimary = 0xFFE6E1E5.toInt(),
            textSecondary = 0xFF938F99.toInt(),
            accentComfort = 0xFF82D9BC.toInt(), // Mint
            accentWarm = 0xFFF28B82.toInt(),    // Coral
            accentCold = 0xFF78D9EC.toInt(),    // Sky
            alertRed = 0xFFF28B82.toInt(),
            okGreen = 0xFF82D9BC.toInt()
        )
    }

    private fun getComfortColor(temp: Double?, palette: ThemePalette): Int {
        val t = temp ?: 22.0
        return when {
            t < 20.0 -> palette.accentCold
            t in 20.0..24.5 -> palette.accentComfort
            else -> palette.accentWarm
        }
    }

    // --- Core Canvas Primitives ---

    fun drawSquircleCard(
        canvas: Canvas,
        rect: RectF,
        cornerRadius: Float,
        bgStartColor: Int,
        bgEndColor: Int,
        borderStartColor: Int,
        borderEndColor: Int,
        borderWidth: Float = 2.5f
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                rect.left, rect.top, rect.left, rect.bottom,
                bgStartColor, bgEndColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = borderWidth
            shader = LinearGradient(
                rect.left, rect.top, rect.left, rect.bottom,
                borderStartColor, borderEndColor,
                Shader.TileMode.CLAMP
            )
        }
        val inset = borderWidth / 2f
        val strokeRect = RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset)
        canvas.drawRoundRect(strokeRect, cornerRadius - inset, cornerRadius - inset, strokePaint)
    }

    fun drawComfortGaugeRing(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        strokeWidth: Float,
        temp: Double?,
        palette: ThemePalette
    ) {
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        // 1. Arc track
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = Color.argb(40, 255, 255, 255)
        }
        canvas.drawArc(rect, 135f, 270f, false, trackPaint)

        // 2. Active arc
        val currentTemp = temp ?: 22.0
        val ratio = ((currentTemp - 15.0) / 15.0).coerceIn(0.06, 1.0).toFloat()
        val sweepAngle = 270f * ratio
        val activeColor = getComfortColor(currentTemp, palette)

        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = activeColor
        }
        canvas.drawArc(rect, 135f, sweepAngle, false, activePaint)

        // 3. Tip needle dot with ambient glow
        val endAngleRad = Math.toRadians((135f + sweepAngle).toDouble())
        val dotX = (cx + radius * cos(endAngleRad)).toFloat()
        val dotY = (cy + radius * sin(endAngleRad)).toFloat()

        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(90, Color.red(activeColor), Color.green(activeColor), Color.blue(activeColor))
        }
        canvas.drawCircle(dotX, dotY, strokeWidth * 0.95f, haloPaint)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        canvas.drawCircle(dotX, dotY, strokeWidth * 0.45f, dotPaint)
    }

    fun drawStatusDot(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int) {
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = Color.argb(75, Color.red(color), Color.green(color), Color.blue(color))
        }
        canvas.drawCircle(cx, cy, radius * 1.6f, haloPaint)

        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }
        canvas.drawCircle(cx, cy, radius, corePaint)
    }

    fun drawMetricPill(
        context: Context,
        canvas: Canvas,
        rect: RectF,
        iconRes: Int?,
        text: String,
        textColor: Int,
        bgColor: Int = Color.argb(35, 255, 255, 255),
        borderColor: Int = Color.argb(28, 255, 255, 255),
        cornerRadius: Float = rect.height() / 2f
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = bgColor
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            color = borderColor
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, strokePaint)

        var textLeft = rect.left + 10f
        if (iconRes != null) {
            val iconSize = (rect.height() * 0.48f).coerceAtLeast(10f)
            val iconTop = rect.centerY() - iconSize / 2f
            val iconLeft = rect.left + 8f
            drawVectorDrawable(context, canvas, iconRes, iconLeft, iconTop, iconSize, iconSize, tintColor = textColor)
            textLeft = iconLeft + iconSize + 6f
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = rect.height() * 0.46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val fontMetrics = textPaint.fontMetrics
        val textY = rect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(text, textLeft, textY, textPaint)
    }

    fun drawVectorDrawable(
        context: Context,
        canvas: Canvas,
        drawableResId: Int,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        tintColor: Int? = null
    ) {
        try {
            val drawable = ContextCompat.getDrawable(context, drawableResId) ?: return
            tintColor?.let { drawable.setTint(it) }
            drawable.setBounds(left.toInt(), top.toInt(), (left + width).toInt(), (top + height).toInt())
            drawable.draw(canvas)
        } catch (e: Exception) {
            // Ignore drawable load failure
        }
    }

    fun ellipsizeText(text: String, paint: Paint, maxWidth: Float): String {
        if (maxWidth <= 0) return ""
        if (paint.measureText(text) <= maxWidth) return text
        var low = 0
        var high = text.length
        var best = ""
        while (low <= high) {
            val mid = (low + high) / 2
            val candidate = text.substring(0, mid) + "…"
            if (paint.measureText(candidate) <= maxWidth) {
                best = candidate
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return if (best.isNotEmpty()) best else "…"
    }

    // --- FULL-CARD WIDGET RENDERING ENGINE ---

    /**
     * 2x1 Compact Pill:
     * Room Name (top-left) + Large crisp temperature (bottom-left) + Comfort dot +
     * Elegant Frosted Humidity Capsule (right) + reserved action button space (top-right).
     * Eliminates text clipping and overlap across all Android launchers!
     */
    fun render2x1WidgetBitmap(
        context: Context,
        widthPx: Int,
        heightPx: Int,
        data: WidgetDataManager.Compact2x1Data,
        theme: String
    ): Bitmap {
        val w = widthPx.coerceAtLeast(240)
        val h = heightPx.coerceAtLeast(100)
        val density = context.resources.displayMetrics.density
        val palette = getThemePalette(theme)

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cornerRadius = palette.cornerRadiusDp * density
        val cardRect = RectF(0f, 0f, w.toFloat(), h.toFloat())

        // 1. Squircle Glass Card with specular highlight
        drawSquircleCard(
            canvas, cardRect, cornerRadius,
            palette.bgStart, palette.bgEnd,
            palette.borderTop, palette.borderBottom,
            borderWidth = 1.8f * density
        )

        val padLeft = 16f * density
        val padTop = 11f * density
        val padRight = 14f * density
        val padBottom = 11f * density

        // Top-right area (72dp) is reserved for ⚙️ and 🔄 touch hotspots
        val actionHotspotWidth = 72f * density

        // 2. Room Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textSecondary
            textSize = (11f * density).coerceIn(10f, 15f * density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val maxTitleW = w - padLeft - actionHotspotWidth - 10f * density
        val safeTitle = ellipsizeText(data.title, titlePaint, maxTitleW)
        val titleY = padTop + titlePaint.textSize
        canvas.drawText(safeTitle, padLeft, titleY, titlePaint)

        // 3. Main Temperature
        val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textPrimary
            textSize = (h * 0.44f).coerceIn(20f * density, 34f * density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tempY = h - padBottom - 2f * density
        canvas.drawText(data.tempText, padLeft, tempY, tempPaint)

        val tempWidth = tempPaint.measureText(data.tempText)

        // 4. Comfort indicator dot next to temperature
        val rawTemp = data.tempText.replace("°", "").toDoubleOrNull()
        val comfortColor = getComfortColor(rawTemp, palette)
        drawStatusDot(canvas, padLeft + tempWidth + 10f * density, tempY - tempPaint.textSize * 0.35f, 3.5f * density, comfortColor)

        // 5. Right Pill: Humidity or Status
        if (data.humidityText != null) {
            val pillH = (h * 0.42f).coerceIn(24f * density, 32f * density)
            val pillW = (64f * density).coerceAtMost((w - padLeft - tempWidth - 24f * density))
            if (pillW > 40f * density) {
                val pillRight = w - padRight
                val pillTop = h - padBottom - pillH
                val pillRect = RectF(pillRight - pillW, pillTop, pillRight, pillTop + pillH)
                drawMetricPill(
                    context, canvas, pillRect,
                    R.drawable.ic_humidity,
                    data.humidityText,
                    palette.accentCold,
                    bgColor = palette.tileBg,
                    borderColor = palette.tileBorder
                )
            }
        }

        return bitmap
    }

    /**
     * 2x2 Square Widget:
     * Room Title (top) + Central Circular Comfort Ring Gauge with large temperature +
     * Bottom Humidity Pill.
     */
    fun render2x2WidgetBitmap(
        context: Context,
        widthPx: Int,
        heightPx: Int,
        data: WidgetDataManager.Compact2x2Data,
        theme: String
    ): Bitmap {
        val w = widthPx.coerceAtLeast(200)
        val h = heightPx.coerceAtLeast(200)
        val density = context.resources.displayMetrics.density
        val palette = getThemePalette(theme)

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cornerRadius = palette.cornerRadiusDp * density
        drawSquircleCard(
            canvas, RectF(0f, 0f, w.toFloat(), h.toFloat()), cornerRadius,
            palette.bgStart, palette.bgEnd,
            palette.borderTop, palette.borderBottom,
            borderWidth = 1.8f * density
        )

        val pad = 14f * density

        // 1. Top Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textSecondary
            textSize = 12f * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val safeTitle = ellipsizeText(data.title, titlePaint, w - pad * 2 - 70f * density)
        canvas.drawText(safeTitle, pad, pad + 12f * density, titlePaint)

        // 2. Central Comfort Ring Gauge
        val rawTemp = data.tempText.replace("°", "").toDoubleOrNull()
        val cx = w / 2f
        val cy = h * 0.49f
        val gaugeRadius = (min(w, h) * 0.26f).coerceIn(24f * density, 52f * density)
        val strokeWidth = 5.5f * density

        drawComfortGaugeRing(canvas, cx, cy, gaugeRadius, strokeWidth, rawTemp, palette)

        // 3. Big Temperature Inside Ring
        val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textPrimary
            textSize = (gaugeRadius * 0.72f).coerceIn(16f * density, 28f * density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val fontMetrics = tempPaint.fontMetrics
        val tempY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(data.tempText, cx, tempY, tempPaint)

        // 4. Bottom Humidity Pill
        if (data.humidityText != null) {
            val pillH = 22f * density
            val pillW = 62f * density
            val pillRect = RectF(cx - pillW / 2f, h - pad - pillH, cx + pillW / 2f, h - pad)
            drawMetricPill(
                context, canvas, pillRect,
                R.drawable.ic_humidity,
                data.humidityText,
                palette.accentCold,
                bgColor = palette.tileBg,
                borderColor = palette.tileBorder
            )
        }

        return bitmap
    }

    /**
     * 4x1 Horizontal Info Bar:
     * Sleek 3-slot bar displaying multiple devices across different rooms in one continuous strip.
     */
    fun render4x1WidgetBitmap(
        context: Context,
        widthPx: Int,
        heightPx: Int,
        slots: List<WidgetDataManager.BarSlotItem>,
        theme: String
    ): Bitmap {
        val w = widthPx.coerceAtLeast(320)
        val h = heightPx.coerceAtLeast(90)
        val density = context.resources.displayMetrics.density
        val palette = getThemePalette(theme)

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cornerRadius = palette.cornerRadiusDp * density
        drawSquircleCard(
            canvas, RectF(0f, 0f, w.toFloat(), h.toFloat()), cornerRadius,
            palette.bgStart, palette.bgEnd,
            palette.borderTop, palette.borderBottom,
            borderWidth = 1.8f * density
        )

        val actionHotspotWidth = 72f * density
        val padLeft = 16f * density
        val padTop = 10f * density
        val padBottom = 10f * density
        val availWidth = w - padLeft - actionHotspotWidth

        val slotCount = slots.size.coerceIn(1, 3)
        val slotW = availWidth / slotCount

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(32, 255, 255, 255)
            strokeWidth = 1f * density
        }

        for (i in 0 until slotCount) {
            val slot = slots[i]
            val slotLeft = padLeft + i * slotW
            val slotRight = slotLeft + slotW - 8f * density

            // Vertical divider between slots
            if (i > 0) {
                canvas.drawLine(slotLeft - 4f * density, padTop + 4f * density, slotLeft - 4f * density, h - padBottom - 4f * density, dividerPaint)
            }

            // Top: Room Name
            val roomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.textSecondary
                textSize = 10.5f * density
            }
            val roomName = ellipsizeText(slot.roomName, roomPaint, slotW - 12f * density)
            canvas.drawText(roomName, slotLeft, padTop + 12f * density, roomPaint)

            // Bottom: Metrics or Door status
            if (slot.isDoor) {
                val doorColor = if (slot.isDoorAlert) palette.alertRed else palette.okGreen
                val doorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = doorColor
                    textSize = 13.5f * density
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText(slot.mainText, slotLeft, h - padBottom - 2f * density, doorPaint)
            } else {
                val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette.textPrimary
                    textSize = 15f * density
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val tempY = h - padBottom - 2f * density
                canvas.drawText(slot.mainText, slotLeft, tempY, tempPaint)

                var currentX = slotLeft + tempPaint.measureText(slot.mainText) + 6f * density

                if (slot.secondaryText != null) {
                    val humPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = palette.accentCold
                        textSize = 12f * density
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    canvas.drawText(slot.secondaryText, currentX, tempY, humPaint)
                }
            }
        }

        return bitmap
    }

    /**
     * 4x2 Flagship Smart Home Hub Dashboard:
     * Header (Title + Timestamp) + Left Primary Climate Compartment with Comfort Ring +
     * Right Secondary Multi-Room Cards & Door/Battery Security Hub.
     */
    fun render4x2WidgetBitmap(
        context: Context,
        widthPx: Int,
        heightPx: Int,
        data: AggregatedSensorData,
        theme: String
    ): Bitmap {
        val w = widthPx.coerceAtLeast(360)
        val h = heightPx.coerceAtLeast(180)
        val density = context.resources.displayMetrics.density
        val palette = getThemePalette(theme)

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cornerRadius = palette.cornerRadiusDp * density
        drawSquircleCard(
            canvas, RectF(0f, 0f, w.toFloat(), h.toFloat()), cornerRadius,
            palette.bgStart, palette.bgEnd,
            palette.borderTop, palette.borderBottom,
            borderWidth = 1.8f * density
        )

        val padLeft = 16f * density
        val padTop = 12f * density
        val padRight = 14f * density
        val padBottom = 12f * density

        // 1. Header (Left: Title + Subtitle)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textPrimary
            textSize = 13.5f * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerTitle = ellipsizeText(data.primaryRoomName, titlePaint, w - padLeft - 76f * density)
        canvas.drawText(headerTitle, padLeft, padTop + 13f * density, titlePaint)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val updateTimeStr = if (data.lastUpdatedTimestamp > 0) {
            "Обновлено в " + timeFormat.format(Date(data.lastUpdatedTimestamp))
        } else {
            "Нажмите для обновления"
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textSecondary
            textSize = 9.5f * density
        }
        canvas.drawText(updateTimeStr, padLeft, padTop + 24f * density, subPaint)

        val bodyTop = padTop + 32f * density
        val bodyBottom = h - padBottom

        val hasRightContent = data.roomSensors.isNotEmpty() || data.openDoorDeviceName != null || data.isAnyDoorOpen || data.lowestBatteryLevel != null
        val leftCardW = if (hasRightContent) (w - padLeft - padRight - 10f * density) * 0.48f else (w - padLeft - padRight)

        // 2. Left Primary Compartment Card
        val leftCardRect = RectF(padLeft, bodyTop, padLeft + leftCardW, bodyBottom)
        drawSquircleCard(
            canvas, leftCardRect, 18f * density,
            palette.tileBg, palette.tileBg,
            palette.tileBorder, palette.tileBorder,
            borderWidth = 1.2f * density
        )

        val innerPad = 10f * density
        val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textPrimary
            textSize = (leftCardRect.height() * 0.35f).coerceIn(20f * density, 30f * density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tempStr = data.primaryTemperature?.let { String.format(Locale.US, "%.1f°", it) } ?: "--°"
        val tempY = leftCardRect.top + innerPad + tempPaint.textSize
        canvas.drawText(tempStr, leftCardRect.left + innerPad, tempY, tempPaint)

        // Dynamic Comfort Ring Gauge beside primary temperature
        val gaugeRadius = (leftCardRect.height() * 0.22f).coerceIn(14f * density, 22f * density)
        val gaugeCx = leftCardRect.right - innerPad - gaugeRadius
        val gaugeCy = leftCardRect.top + innerPad + gaugeRadius
        drawComfortGaugeRing(canvas, gaugeCx, gaugeCy, gaugeRadius, 4.5f * density, data.primaryTemperature, palette)

        // Bottom Pills inside Left Compartment (Humidity and Pressure)
        var pillX = leftCardRect.left + innerPad
        val pillH = 22f * density
        val pillY = leftCardRect.bottom - innerPad - pillH

        if (data.primaryHumidity != null) {
            val humStr = String.format(Locale.US, "%.0f%%", data.primaryHumidity)
            val humPillW = 54f * density
            val humRect = RectF(pillX, pillY, pillX + humPillW, pillY + pillH)
            drawMetricPill(context, canvas, humRect, R.drawable.ic_humidity, humStr, palette.accentCold, bgColor = Color.argb(40, 0, 0, 0))
            pillX += humPillW + 6f * density
        }

        if (data.primaryPressure != null && (pillX + 58f * density) <= leftCardRect.right - innerPad) {
            val pressStr = String.format(Locale.US, "%.0f", data.primaryPressure)
            val pressPillW = 56f * density
            val pressRect = RectF(pillX, pillY, pillX + pressPillW, pillY + pillH)
            drawMetricPill(context, canvas, pressRect, R.drawable.ic_pressure, pressStr, palette.textSecondary, bgColor = Color.argb(40, 0, 0, 0))
        }

        // 3. Right Area: Secondary Rooms & Security Status
        if (hasRightContent) {
            val rightLeft = leftCardRect.right + 10f * density
            val rightW = w - padRight - rightLeft

            var currentY = bodyTop

            // Up to 2 Secondary Rooms
            val roomsToDisplay = data.roomSensors.take(2)
            for (room in roomsToDisplay) {
                val roomCardH = 26f * density
                val roomCardRect = RectF(rightLeft, currentY, rightLeft + rightW, currentY + roomCardH)
                drawSquircleCard(
                    canvas, roomCardRect, 10f * density,
                    palette.tileBg, palette.tileBg,
                    palette.tileBorder, palette.tileBorder,
                    borderWidth = 1f * density
                )

                val rNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette.textSecondary
                    textSize = 10.5f * density
                }
                val rSafeName = ellipsizeText(room.roomName, rNamePaint, rightW * 0.45f)
                canvas.drawText(rSafeName, rightLeft + 8f * density, currentY + 16f * density, rNamePaint)

                val rValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette.textPrimary
                    textSize = 11.5f * density
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val rTemp = room.temperature?.let { String.format(Locale.US, "%.1f°", it) } ?: ""
                val rHum = room.humidity?.let { String.format(Locale.US, "%.0f%%", it) } ?: ""
                val rValStr = "$rTemp $rHum".trim()
                canvas.drawText(rValStr, rightLeft + rightW - 8f * density - rValPaint.measureText(rValStr), currentY + 17f * density, rValPaint)

                currentY += roomCardH + 5f * density
            }

            // Bottom Status Row (Door and Battery)
            val bottomRowH = 24f * density
            val bottomRowY = bodyBottom - bottomRowH

            var badgeX = rightLeft
            if (data.openDoorDeviceName != null || data.isAnyDoorOpen) {
                val doorColor = if (data.isAnyDoorOpen) palette.alertRed else palette.okGreen
                val doorText = if (data.isAnyDoorOpen) "🚪 ОТКРЫТО!" else "🚪 Закрыто"
                val doorW = 76f * density
                val doorRect = RectF(badgeX, bottomRowY, badgeX + doorW, bottomRowY + bottomRowH)
                drawMetricPill(context, canvas, doorRect, null, doorText, doorColor, bgColor = Color.argb(45, Color.red(doorColor), Color.green(doorColor), Color.blue(doorColor)))
                badgeX += doorW + 6f * density
            }

            if (data.lowestBatteryLevel != null && badgeX + 50f * density <= rightLeft + rightW) {
                val batColor = if (data.lowestBatteryLevel <= 20.0) palette.accentWarm else palette.okGreen
                val batStr = String.format(Locale.US, "🔋 %.0f%%", data.lowestBatteryLevel)
                val batW = 56f * density
                val batRect = RectF(badgeX, bottomRowY, badgeX + batW, bottomRowY + bottomRowH)
                drawMetricPill(context, canvas, batRect, null, batStr, batColor, bgColor = palette.tileBg)
            }
        }

        return bitmap
    }

    // --- Backward Compatibility Helper ---
    fun renderComfortGauge(temp: Double?, humidity: Double?, theme: String, sizePx: Int = 120): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val palette = getThemePalette(theme)
        val strokeWidth = sizePx * 0.085f
        val radius = (sizePx - strokeWidth * 2.5f) / 2f
        drawComfortGaugeRing(canvas, sizePx / 2f, sizePx / 2f, radius, strokeWidth, temp, palette)
        return bitmap
    }

    // Theme resource backward compatibility
    fun getThemeBackground(theme: String): Int = when (theme) {
        WidgetDataManager.THEME_XIAOMI -> R.drawable.bg_skin_xiaomi
        WidgetDataManager.THEME_IOS -> R.drawable.bg_skin_ios
        WidgetDataManager.THEME_ALICE -> R.drawable.bg_skin_alice
        else -> R.drawable.bg_skin_android
    }

    fun getTileBackground(theme: String): Int = when (theme) {
        WidgetDataManager.THEME_XIAOMI -> R.drawable.bg_tile_xiaomi
        WidgetDataManager.THEME_IOS -> R.drawable.bg_tile_ios
        WidgetDataManager.THEME_ALICE -> R.drawable.bg_tile_alice
        else -> R.drawable.bg_tile_android
    }

    fun getButtonBackground(theme: String): Int = when (theme) {
        WidgetDataManager.THEME_XIAOMI -> R.drawable.bg_btn_xiaomi
        WidgetDataManager.THEME_IOS -> R.drawable.bg_btn_ios
        WidgetDataManager.THEME_ALICE -> R.drawable.bg_btn_alice
        else -> R.drawable.bg_btn_android
    }

    fun getPillBackground(theme: String): Int = when (theme) {
        WidgetDataManager.THEME_XIAOMI -> R.drawable.bg_pill_xiaomi
        WidgetDataManager.THEME_IOS -> R.drawable.bg_tile_ios
        WidgetDataManager.THEME_ALICE -> R.drawable.bg_tile_alice
        else -> R.drawable.bg_tile_android
    }

    fun getTextPrimaryColor(theme: String): Int = getThemePalette(theme).textPrimary
    fun getTextSecondaryColor(theme: String): Int = getThemePalette(theme).textSecondary
    fun getAccentColor(theme: String): Int = getThemePalette(theme).accentComfort
    fun getHumidityColor(theme: String): Int = getThemePalette(theme).accentCold
}
