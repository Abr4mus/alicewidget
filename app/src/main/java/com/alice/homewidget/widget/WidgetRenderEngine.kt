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
        val isGlassmorphism: Boolean = false,
        val cornerRadiusDp: Float = 26f
    )

    fun getThemePalette(theme: String): ThemePalette = when (theme) {
        WidgetDataManager.THEME_IOS -> ThemePalette(
            // Apple iOS 18 Liquid Glass: translucent multi-stop frosted glass with top specular refraction
            bgStart = 0xDE22242B.toInt(),
            bgEnd = 0xEE121318.toInt(),
            borderTop = 0x85FFFFFF.toInt(),    // Crisp specular glass edge
            borderBottom = 0x22FFFFFF.toInt(),
            tileBg = 0x28FFFFFF.toInt(),       // Frosted sub-compartments
            tileBorder = 0x38FFFFFF.toInt(),
            textPrimary = 0xFFF5F5F7.toInt(),  // SF Pro White
            textSecondary = 0xFF9E9EA6.toInt(), // SF Pro Subtitle
            accentComfort = 0xFF34C759.toInt(), // Apple Comfort Green
            accentWarm = 0xFFFF9500.toInt(),    // Apple Amber
            accentCold = 0xFF007AFF.toInt(),    // Apple System Blue
            alertRed = 0xFFFF3B30.toInt(),
            okGreen = 0xFF34C759.toInt(),
            isGlassmorphism = true,
            cornerRadiusDp = 26f
        )
        WidgetDataManager.THEME_ALICE -> ThemePalette(
            // Yandex Alice Pro Cosmic Cyberpunk: deep midnight violet with neon lilac edge
            bgStart = 0xEE1E113B.toInt(),
            bgEnd = 0xF60F0721.toInt(),
            borderTop = 0x909D74FF.toInt(),     // Neon Lilac Glow
            borderBottom = 0x309D74FF.toInt(),
            tileBg = 0x2E8B5CF6.toInt(),        // Violet tinted glass
            tileBorder = 0x459D74FF.toInt(),
            textPrimary = 0xFFFFFFFF.toInt(),
            textSecondary = 0xFFCBB9EE.toInt(), // Soft Lavender
            accentComfort = 0xFFC77DFF.toInt(), // Alice Lilac Glow
            accentWarm = 0xFFFF6B8B.toInt(),
            accentCold = 0xFF38BDF8.toInt(),
            alertRed = 0xFFFF4D6D.toInt(),
            okGreen = 0xFF50FA7B.toInt(),
            cornerRadiusDp = 26f
        )
        WidgetDataManager.THEME_NOTHING -> ThemePalette(
            // Nothing OS: Deep monochrome tech with high-contrast Bauhaus geometry
            bgStart = 0xF50D0E11.toInt(),
            bgEnd = 0xFA08080A.toInt(),
            borderTop = 0x4DFFFFFF.toInt(),
            borderBottom = 0x1AFFFFFF.toInt(),
            tileBg = 0x1CFFFFFF.toInt(),
            tileBorder = 0x2EFFFFFF.toInt(),
            textPrimary = 0xFFFFFFFF.toInt(),
            textSecondary = 0xFF8A8D93.toInt(),
            accentComfort = 0xFFFFFFFF.toInt(),
            accentWarm = 0xFFD71921.toInt(),    // Nothing Red
            accentCold = 0xFFD0D3D9.toInt(),
            alertRed = 0xFFD71921.toInt(),
            okGreen = 0xFFFFFFFF.toInt(),
            cornerRadiusDp = 22f
        )
        WidgetDataManager.THEME_ANDROID -> ThemePalette(
            // Android Material You: Tonal graphite surface with pastel accents
            bgStart = 0xF01C1E22.toInt(),
            bgEnd = 0xF6101114.toInt(),
            borderTop = 0x5082D9BC.toInt(),     // Pastel Mint Highlight
            borderBottom = 0x1882D9BC.toInt(),
            tileBg = 0x22FFFFFF.toInt(),
            tileBorder = 0x28FFFFFF.toInt(),
            textPrimary = 0xFFE6E1E5.toInt(),
            textSecondary = 0xFF9995A0.toInt(),
            accentComfort = 0xFF82D9BC.toInt(), // Mint
            accentWarm = 0xFFF28B82.toInt(),    // Coral
            accentCold = 0xFF78D9EC.toInt(),    // Sky
            alertRed = 0xFFF28B82.toInt(),
            okGreen = 0xFF82D9BC.toInt(),
            cornerRadiusDp = 26f
        )
        else -> ThemePalette( // THEME_XIAOMI (HyperOS)
            bgStart = 0xEE16181E.toInt(),
            bgEnd = 0xF60C0D11.toInt(),
            borderTop = 0x55FFFFFF.toInt(),     // Specular glass shine
            borderBottom = 0x18FFFFFF.toInt(),
            tileBg = 0x1EFFFFFF.toInt(),        // Acrylic frosted sub-cards
            tileBorder = 0x24FFFFFF.toInt(),
            textPrimary = 0xFFFFFFFF.toInt(),
            textSecondary = 0xFF9DA3AE.toInt(), // HyperOS Slate
            accentComfort = 0xFF00D2B4.toInt(), // HyperOS Teal
            accentWarm = 0xFFFF6900.toInt(),    // HyperOS Warm Orange
            accentCold = 0xFF38BDF8.toInt(),    // Cool Cyan
            alertRed = 0xFFFF453A.toInt(),
            okGreen = 0xFF00D2B4.toInt(),
            cornerRadiusDp = 26f
        )
    }

    private fun getComfortInfo(temp: Double?, palette: ThemePalette): Triple<Int, String, String> {
        val t = temp ?: 22.0
        return when {
            t < 20.0 -> Triple(palette.accentCold, "❄️ Прохладно", "Низкая")
            t in 20.0..24.5 -> Triple(palette.accentComfort, "🌿 Комфортно", "Норма")
            else -> Triple(palette.accentWarm, "🔥 Тепло", "Высокая")
        }
    }

    // --- Glassmorphic Canvas Rendering Primitives ---

    fun drawSquircleCard(
        canvas: Canvas,
        rect: RectF,
        cornerRadius: Float,
        palette: ThemePalette,
        isSubTile: Boolean = false
    ) {
        if (!isSubTile && palette.isGlassmorphism) {
            // Apple iOS 18 Multi-Stop Frosted Glass Sheen
            val diffusePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                shader = LinearGradient(
                    rect.left, rect.top, rect.right, rect.bottom,
                    intArrayOf(0x35FFFFFF.toInt(), 0x14FFFFFF.toInt(), 0x08000000.toInt()),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, diffusePaint)
        }

        // Base Tint Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                rect.left, rect.top, rect.left, rect.bottom,
                if (isSubTile) palette.tileBg else palette.bgStart,
                if (isSubTile) palette.tileBg else palette.bgEnd,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        // Specular Edge Refraction Stroke (Bright on top, fading on bottom)
        val strokeWidth = if (isSubTile) 1.2f else 2.2f
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            shader = LinearGradient(
                rect.left, rect.top, rect.left, rect.bottom,
                if (isSubTile) palette.tileBorder else palette.borderTop,
                if (isSubTile) Color.argb(15, 255, 255, 255) else palette.borderBottom,
                Shader.TileMode.CLAMP
            )
        }
        val inset = strokeWidth / 2f
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
        palette: ThemePalette,
        showStatusText: Boolean = false
    ) {
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        // 1. Background Track Arc (270°)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = Color.argb(45, 255, 255, 255)
        }
        canvas.drawArc(rect, 135f, 270f, false, trackPaint)

        // 2. Active Gradient Arc
        val currentTemp = temp ?: 22.0
        val ratio = ((currentTemp - 15.0) / 15.0).coerceIn(0.06, 1.0).toFloat()
        val sweepAngle = 270f * ratio
        val (activeColor, statusText) = getComfortInfo(currentTemp, palette)

        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = activeColor
        }
        canvas.drawArc(rect, 135f, sweepAngle, false, activePaint)

        // 3. Glowing Tip Indicator Needle Dot
        val endAngleRad = Math.toRadians((135f + sweepAngle).toDouble())
        val dotX = (cx + radius * cos(endAngleRad)).toFloat()
        val dotY = (cy + radius * sin(endAngleRad)).toFloat()

        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(100, Color.red(activeColor), Color.green(activeColor), Color.blue(activeColor))
        }
        canvas.drawCircle(dotX, dotY, strokeWidth * 1.05f, haloPaint)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        canvas.drawCircle(dotX, dotY, strokeWidth * 0.48f, dotPaint)

        // 4. Clear Interactive Status Label
        if (showStatusText) {
            val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = activeColor
                textSize = (radius * 0.28f).coerceIn(10f, 13f * canvas.density)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(statusText, cx, cy + radius + statusPaint.textSize * 1.1f, statusPaint)
        }
    }

    fun drawStatusDot(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int) {
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = Color.argb(85, Color.red(color), Color.green(color), Color.blue(color))
        }
        canvas.drawCircle(cx, cy, radius * 1.7f, haloPaint)

        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }
        canvas.drawCircle(cx, cy, radius, corePaint)
    }

    fun drawGlassPill(
        context: Context,
        canvas: Canvas,
        rect: RectF,
        iconRes: Int?,
        text: String,
        textColor: Int,
        palette: ThemePalette,
        cornerRadius: Float = rect.height() / 2f
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = palette.tileBg
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            color = palette.tileBorder
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, strokePaint)

        var textLeft = rect.left + 8f
        if (iconRes != null) {
            val iconSize = (rect.height() * 0.52f).coerceAtLeast(11f)
            val iconTop = rect.centerY() - iconSize / 2f
            val iconLeft = rect.left + 7f
            drawVectorDrawable(context, canvas, iconRes, iconLeft, iconTop, iconSize, iconSize, tintColor = textColor)
            textLeft = iconLeft + iconSize + 5f
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
            // Ignore load failure
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

    // --- FULL-CARD WIDGET RENDERING IMPLEMENTATIONS ---

    /**
     * 2x1 Compact Pill:
     * Room Name (top-left) + Large crisp temperature (bottom-left) + Comfort status text +
     * Frosted Glass Humidity Capsule (right) + reserved action buttons (top-right).
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

        drawSquircleCard(canvas, cardRect, cornerRadius, palette)

        val padLeft = 16f * density
        val padTop = 11f * density
        val padRight = 14f * density
        val padBottom = 11f * density
        val actionHotspotWidth = 68f * density

        // 1. Room Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textSecondary
            textSize = (11f * density).coerceIn(10f, 14f * density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val maxTitleW = w - padLeft - actionHotspotWidth - 10f * density
        val safeTitle = ellipsizeText(data.title, titlePaint, maxTitleW)
        val titleY = padTop + titlePaint.textSize
        canvas.drawText(safeTitle, padLeft, titleY, titlePaint)

        // 2. Main Temperature
        val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textPrimary
            textSize = (h * 0.44f).coerceIn(22f * density, 34f * density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tempY = h - padBottom - 2f * density
        canvas.drawText(data.tempText, padLeft, tempY, tempPaint)
        val tempWidth = tempPaint.measureText(data.tempText)

        // 3. Status indicator & clear text badge
        val rawTemp = data.tempText.replace("°", "").toDoubleOrNull()
        val (comfortColor, comfortText) = getComfortInfo(rawTemp, palette)
        drawStatusDot(canvas, padLeft + tempWidth + 10f * density, tempY - tempPaint.textSize * 0.35f, 3.5f * density, comfortColor)

        // 4. Right Frosted Capsule (Humidity or Status)
        if (data.humidityText != null) {
            val pillH = (h * 0.40f).coerceIn(24f * density, 32f * density)
            val pillW = (68f * density).coerceAtMost(w - padLeft - tempWidth - 24f * density)
            if (pillW > 40f * density) {
                val pillRight = w - padRight
                val pillTop = h - padBottom - pillH
                val pillRect = RectF(pillRight - pillW, pillTop, pillRight, pillTop + pillH)
                val icon = if (data.humidityText == "Уровень" || data.humidityText == "Топливо") R.drawable.ic_humidity else R.drawable.ic_humidity
                drawGlassPill(context, canvas, pillRect, icon, data.humidityText, palette.accentCold, palette)
            }
        }

        return bitmap
    }

    /**
     * 2x2 Square Widget:
     * Header with Room & Comfort Dot + Central Comfort Ring Gauge with temperature and
     * interactive clarity status ("🌿 Комфортно") + Bottom Glass Humidity Pill.
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
        drawSquircleCard(canvas, RectF(0f, 0f, w.toFloat(), h.toFloat()), cornerRadius, palette)

        val pad = 14f * density

        // 1. Top Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textSecondary
            textSize = 12f * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val safeTitle = ellipsizeText(data.title, titlePaint, w - pad * 2 - 68f * density)
        canvas.drawText(safeTitle, pad, pad + 12f * density, titlePaint)

        // 2. Central Comfort Ring Gauge
        val rawTemp = data.tempText.replace("°", "").toDoubleOrNull()
        val cx = w / 2f
        val cy = h * 0.47f
        val gaugeRadius = (min(w, h) * 0.26f).coerceIn(24f * density, 52f * density)
        val strokeWidth = 5.5f * density

        drawComfortGaugeRing(canvas, cx, cy, gaugeRadius, strokeWidth, rawTemp, palette, showStatusText = false)

        // 3. Big Temperature Inside Ring
        val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textPrimary
            textSize = (gaugeRadius * 0.68f).coerceIn(16f * density, 27f * density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val fontMetrics = tempPaint.fontMetrics
        val tempY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(data.tempText, cx, tempY, tempPaint)

        // 4. Clarifying Comfort Status under Gauge
        val (comfortColor, comfortText) = getComfortInfo(rawTemp, palette)
        val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = comfortColor
            textSize = 10f * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(comfortText, cx, cy + gaugeRadius + 12f * density, statusPaint)

        // 5. Bottom Frosted Glass Pill
        if (data.humidityText != null) {
            val pillH = 22f * density
            val pillW = 66f * density
            val pillRect = RectF(cx - pillW / 2f, h - pad - pillH, cx + pillW / 2f, h - pad)
            drawGlassPill(context, canvas, pillRect, R.drawable.ic_humidity, data.humidityText, palette.accentCold, palette)
        }

        return bitmap
    }

    /**
     * 4x1 Modern Glass Ribbon:
     * Overhauled with 3 distinct Frosted Glass Compartments, large bold numbers,
     * crisp glowing vector icons (🌡️, 💧, 🚪, 🔋, ⚡), and high visibility.
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
        drawSquircleCard(canvas, RectF(0f, 0f, w.toFloat(), h.toFloat()), cornerRadius, palette)

        val actionHotspotWidth = 68f * density
        val padLeft = 12f * density
        val padTop = 9f * density
        val padBottom = 9f * density
        val availWidth = w - padLeft - actionHotspotWidth

        val slotCount = slots.size.coerceIn(1, 3)
        val gap = 6f * density
        val slotW = (availWidth - gap * (slotCount - 1)) / slotCount

        for (i in 0 until slotCount) {
            val slot = slots[i]
            val slotLeft = padLeft + i * (slotW + gap)
            val slotRect = RectF(slotLeft, padTop, slotLeft + slotW, h - padBottom)

            // Draw Frosted Sub-Card Compartment for each slot
            drawSquircleCard(canvas, slotRect, 14f * density, palette, isSubTile = true)

            val innerPad = 7f * density

            // Top: Room / Device Name
            val roomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.textSecondary
                textSize = 9.5f * density
            }
            val roomName = ellipsizeText(slot.roomName, roomPaint, slotW - innerPad * 2)
            canvas.drawText(roomName, slotLeft + innerPad, padTop + innerPad + roomPaint.textSize, roomPaint)

            // Bottom Metrics Row with Crisp Icons & Prominent Numbers
            val contentY = slotRect.bottom - innerPad - 2f * density
            var startX = slotLeft + innerPad

            if (slot.isDoor) {
                val doorColor = if (slot.isDoorAlert) palette.alertRed else palette.okGreen
                val iconSize = 14f * density
                drawVectorDrawable(context, canvas, R.drawable.ic_door, startX, contentY - iconSize + 2f, iconSize, iconSize, tintColor = doorColor)
                startX += iconSize + 4f * density

                val doorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = doorColor
                    textSize = 12.5f * density
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText(slot.mainText, startX, contentY, doorPaint)
            } else {
                // 1. Thermometer icon
                val iconSize = 13f * density
                drawVectorDrawable(context, canvas, R.drawable.ic_thermometer, startX, contentY - iconSize + 2f, iconSize, iconSize, tintColor = palette.accentComfort)
                startX += iconSize + 3f * density

                // 2. Large Bold Temperature
                val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette.textPrimary
                    textSize = 15f * density
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText(slot.mainText, startX, contentY, tempPaint)
                startX += tempPaint.measureText(slot.mainText) + 6f * density

                // 3. Humidity or Secondary with droplet
                if (slot.secondaryText != null && (startX + 28f * density <= slotRect.right - innerPad)) {
                    val humIconSize = 11f * density
                    drawVectorDrawable(context, canvas, R.drawable.ic_humidity, startX, contentY - humIconSize + 1f, humIconSize, humIconSize, tintColor = palette.accentCold)
                    startX += humIconSize + 2f * density

                    val humPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = palette.accentCold
                        textSize = 12f * density
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    canvas.drawText(slot.secondaryText, startX, contentY, humPaint)
                }
            }
        }

        return bitmap
    }

    /**
     * 4x2 Flagship Smart Home Hub Dashboard:
     * Header + Left Primary Climate Compartment with Comfort Ring & Textual Status Badge +
     * Right Secondary Multi-Room Frosted Cards & Security/Battery Badges.
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
        drawSquircleCard(canvas, RectF(0f, 0f, w.toFloat(), h.toFloat()), cornerRadius, palette)

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

        // 2. Left Primary Compartment Card (Frosted Glass Sub-Card)
        val leftCardRect = RectF(padLeft, bodyTop, padLeft + leftCardW, bodyBottom)
        drawSquircleCard(canvas, leftCardRect, 18f * density, palette, isSubTile = true)

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
        drawComfortGaugeRing(canvas, gaugeCx, gaugeCy, gaugeRadius, 4.5f * density, data.primaryTemperature, palette, showStatusText = false)

        // Interactive Comfort Status Badge
        val (comfortColor, comfortText) = getComfortInfo(data.primaryTemperature, palette)
        val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = comfortColor
            textSize = 9.5f * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(comfortText, leftCardRect.left + innerPad, tempY + statusPaint.textSize + 4f * density, statusPaint)

        // Bottom Pills inside Left Compartment (Humidity and Pressure)
        var pillX = leftCardRect.left + innerPad
        val pillH = 22f * density
        val pillY = leftCardRect.bottom - innerPad - pillH

        if (data.primaryHumidity != null) {
            val humStr = String.format(Locale.US, "%.0f%%", data.primaryHumidity)
            val humPillW = 54f * density
            val humRect = RectF(pillX, pillY, pillX + humPillW, pillY + pillH)
            drawGlassPill(context, canvas, humRect, R.drawable.ic_humidity, humStr, palette.accentCold, palette)
            pillX += humPillW + 6f * density
        }

        if (data.primaryPressure != null && (pillX + 58f * density) <= leftCardRect.right - innerPad) {
            val pressStr = String.format(Locale.US, "%.0f", data.primaryPressure)
            val pressPillW = 56f * density
            val pressRect = RectF(pillX, pillY, pillX + pressPillW, pillY + pillH)
            drawGlassPill(context, canvas, pressRect, R.drawable.ic_pressure, pressStr, palette.textSecondary, palette)
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
                drawSquircleCard(canvas, roomCardRect, 10f * density, palette, isSubTile = true)

                val rNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette.textSecondary
                    textSize = 10f * density
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
                val rValStr = "$rTemp $rHum".trim().ifEmpty { room.extraInfo ?: "" }
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
                drawGlassPill(context, canvas, doorRect, null, doorText, doorColor, palette)
                badgeX += doorW + 6f * density
            }

            if (data.lowestBatteryLevel != null && badgeX + 50f * density <= rightLeft + rightW) {
                val batColor = if (data.lowestBatteryLevel <= 20.0) palette.accentWarm else palette.okGreen
                val batStr = String.format(Locale.US, "🔋 %.0f%%", data.lowestBatteryLevel)
                val batW = 56f * density
                val batRect = RectF(badgeX, bottomRowY, badgeX + batW, bottomRowY + bottomRowH)
                drawGlassPill(context, canvas, batRect, null, batStr, batColor, palette)
            }
        }

        return bitmap
    }

    // --- Backward Compatibility Helpers ---
    fun renderComfortGauge(temp: Double?, humidity: Double?, theme: String, sizePx: Int = 120): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val palette = getThemePalette(theme)
        val strokeWidth = sizePx * 0.085f
        val radius = (sizePx - strokeWidth * 2.5f) / 2f
        drawComfortGaugeRing(canvas, sizePx / 2f, sizePx / 2f, radius, strokeWidth, temp, palette)
        return bitmap
    }

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
