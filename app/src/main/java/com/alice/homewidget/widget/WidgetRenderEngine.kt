package com.alice.homewidget.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.alice.homewidget.R
import com.alice.homewidget.data.WidgetDataManager
import com.alice.homewidget.model.AggregatedSensorData
import com.alice.homewidget.model.DeviceWidgetState
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
        val isLight: Boolean = false,
        val cornerRadiusDp: Float = 26f
    )

    fun getThemePalette(theme: String): ThemePalette = when (theme) {
        WidgetDataManager.THEME_IOS -> ThemePalette(
            // Apple iOS 18 Liquid Glass: crystal-clear frosted glass with top specular refraction and chamfered rim
            bgStart = 0x501F232B.toInt(),       // High transparency dark-frosted glass (~31% opacity)
            bgEnd = 0x6612141A.toInt(),         // Gentle translucent gradient (~40% opacity)
            borderTop = 0xDBFFFFFF.toInt(),     // Brilliant specular glass edge highlight ("ребро")
            borderBottom = 0x24FFFFFF.toInt(),  // Soft bottom refraction edge
            tileBg = 0x22FFFFFF.toInt(),        // Translucent frosted glass sub-compartments
            tileBorder = 0x48FFFFFF.toInt(),    // Frosted glass pod border
            textPrimary = 0xFFFFFFFF.toInt(),   // Pure SF Pro White
            textSecondary = 0xFFD2D5DC.toInt(), // High-clarity SF Pro Silver Subtitle
            accentComfort = 0xFF30D158.toInt(), // Apple System Green
            accentWarm = 0xFFFF9F0A.toInt(),    // Apple System Amber
            accentCold = 0xFF0A84FF.toInt(),    // Apple System Blue
            alertRed = 0xFFFF453A.toInt(),      // Apple System Red
            okGreen = 0xFF30D158.toInt(),
            isGlassmorphism = true,
            isLight = false,
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
        WidgetDataManager.THEME_CYBERPUNK, WidgetDataManager.THEME_ANDROID -> ThemePalette(
            // Cyberpunk Neon: Deep OLED black with electric cyan and purple glowing contours
            bgStart = 0xF5080A10.toInt(),
            bgEnd = 0xFA040508.toInt(),
            borderTop = 0xD000F0FF.toInt(),     // Neon Cyan
            borderBottom = 0x60BD00FF.toInt(),  // Electric Purple
            tileBg = 0x2400F0FF.toInt(),
            tileBorder = 0x5500F0FF.toInt(),
            textPrimary = 0xFFFFFFFF.toInt(),
            textSecondary = 0xFF8AE8FF.toInt(), // Cyan Lavender
            accentComfort = 0xFF00F5D4.toInt(), // Cyber Mint
            accentWarm = 0xFFFF0055.toInt(),    // Neon Pink
            accentCold = 0xFF00B4D8.toInt(),    // Deep Aqua
            alertRed = 0xFFFF0055.toInt(),
            okGreen = 0xFF00F5D4.toInt(),
            isGlassmorphism = true,
            cornerRadiusDp = 26f
        )
        WidgetDataManager.THEME_NORDIC_AURORA, WidgetDataManager.THEME_NOTHING -> ThemePalette(
            // Nordic Aurora: Scandinavian midnight slate with emerald northern lights glow
            bgStart = 0xF00D1B24.toInt(),
            bgEnd = 0xF6071017.toInt(),
            borderTop = 0xBF00E5A3.toInt(),     // Aurora Borealis Mint
            borderBottom = 0x3000E5A3.toInt(),
            tileBg = 0x1E00E5A3.toInt(),
            tileBorder = 0x4800E5A3.toInt(),
            textPrimary = 0xFFFFFFFF.toInt(),
            textSecondary = 0xFFB2DFDB.toInt(), // Pale Glacier Mint
            accentComfort = 0xFF00E5A3.toInt(), // Aurora Mint
            accentWarm = 0xFFFFB340.toInt(),    // Nordic Amber
            accentCold = 0xFF64D2FF.toInt(),    // Polar Blue
            alertRed = 0xFFFF453A.toInt(),
            okGreen = 0xFF00E5A3.toInt(),
            isGlassmorphism = true,
            cornerRadiusDp = 26f
        )
        WidgetDataManager.THEME_YANDEX_WHITE -> ThemePalette(
            // Pure White Yandex: milk-white surface, dark charcoal typography, Yandex Yellow, Red & Green accents
            bgStart = 0xFCFFFFFF.toInt(),
            bgEnd = 0xF5F0F2F6.toInt(),
            borderTop = 0xA0FFFFFF.toInt(),
            borderBottom = 0x24000000.toInt(),
            tileBg = 0x0F000000.toInt(),        // Subtle soft gray pod
            tileBorder = 0x1A000000.toInt(),    // Clean thin pod stroke
            textPrimary = 0xFF19191D.toInt(),   // Deep Charcoal
            textSecondary = 0xFF767B87.toInt(), // Slate Gray
            accentComfort = 0xFF00B350.toInt(), // Yandex Fresh Mint Green
            accentWarm = 0xFFFC3F1D.toInt(),    // Yandex Red
            accentCold = 0xFF0077FF.toInt(),    // Yandex Azure Blue
            alertRed = 0xFFFC3F1D.toInt(),
            okGreen = 0xFF00B350.toInt(),
            isGlassmorphism = false,            // Clean solid porcelain look
            isLight = true,
            cornerRadiusDp = 26f
        )
        WidgetDataManager.THEME_FLUENT_PASTEL -> ThemePalette(
            // Microsoft Fluent Acrylic: Luminous dark-acrylic mauve-to-azure gradient with high-clarity white text
            bgStart = 0xF03B1E48.toInt(),       // Rich acrylic mauve/plum
            bgEnd = 0xF6162846.toInt(),         // Deep twilight azure
            borderTop = 0x90D8B4FE.toInt(),     // Soft lilac specular edge
            borderBottom = 0x3038BDF8.toInt(),  // Azure sheen edge
            tileBg = 0x2E4A2862.toInt(),        // Acrylic frosted sub-card
            tileBorder = 0x48D8B4FE.toInt(),
            textPrimary = Color.WHITE,          // Pure white
            textSecondary = 0xFFD8B4FE.toInt(), // Soft lilac / pale mauve
            accentComfort = 0xFF38BDF8.toInt(), // Vibrant azure
            accentWarm = 0xFFFF70A6.toInt(),    // Warm pastel rose
            accentCold = 0xFF818CF8.toInt(),    // Indigo cold
            alertRed = 0xFFFF4D6D.toInt(),
            okGreen = 0xFF34D399.toInt(),
            isGlassmorphism = true,
            isLight = false,
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
            t < 20.0 -> Triple(palette.accentCold, "Прохладно", "Низкая")
            t in 20.0..24.5 -> Triple(palette.accentComfort, "Комфортно", "Норма")
            else -> Triple(palette.accentWarm, "Тепло", "Высокая")
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
            // 1. Soft Ambient Drop Shadow underneath the floating glass card
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
                color = 0x2E000000.toInt()
            }
            val shadowRect = RectF(rect.left + 0.8f, rect.top + 1.8f, rect.right - 0.8f, rect.bottom)
            canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint)
        }

        // 2. Base Translucent Glass Background
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

        // 3. Apple Frosted Glass Diffusion & Specular Sheen ("Блюр" и рассеянный свет)
        if (palette.isGlassmorphism) {
            if (!isSubTile) {
                // Multi-stop diagonal specular glass sheen
                val diffusePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    shader = LinearGradient(
                        rect.left, rect.top, rect.right, rect.bottom,
                        intArrayOf(
                            0x36FFFFFF.toInt(),
                            0x14FFFFFF.toInt(),
                            0x02FFFFFF.toInt(),
                            0x0C000000.toInt()
                        ),
                        floatArrayOf(0f, 0.35f, 0.70f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, diffusePaint)

                // Top-edge light shelf (верхнее световое преломление в верхней трети стекла)
                val topLightH = rect.height() * 0.38f
                val topLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    shader = LinearGradient(
                        rect.left, rect.top, rect.left, rect.top + topLightH,
                        0x24FFFFFF.toInt(),
                        0x00FFFFFF.toInt(),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.save()
                canvas.clipRect(rect)
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, topLightPaint)
                canvas.restore()
            } else {
                // Frosted sub-compartment delicate sheen
                val subSheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    shader = LinearGradient(
                        rect.left, rect.top, rect.left, rect.bottom,
                        0x18FFFFFF.toInt(),
                        0x04FFFFFF.toInt(),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, subSheenPaint)
            }
        }

        // 4. Primary Specular Beveled Glass Edge ("Ребро" со спекулярным световым бликом)
        val strokeWidth = if (isSubTile) 1.2f else 2.2f
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            shader = LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                if (isSubTile) palette.tileBorder else palette.borderTop,
                if (isSubTile) Color.argb(16, 255, 255, 255) else palette.borderBottom,
                Shader.TileMode.CLAMP
            )
        }
        val inset = strokeWidth / 2f
        val strokeRect = RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset)
        canvas.drawRoundRect(strokeRect, cornerRadius - inset, cornerRadius - inset, strokePaint)

        // 5. Inset Chamfer Refraction ("Внутренняя грань/ребро" для глубины стеклянного среза)
        if (!isSubTile && palette.isGlassmorphism) {
            val innerStrokeW = 1.0f
            val innerInset = strokeWidth + innerStrokeW / 2f
            val innerRect = RectF(
                rect.left + innerInset,
                rect.top + innerInset,
                rect.right - innerInset,
                rect.bottom - innerInset
            )
            val innerBevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = innerStrokeW
                shader = LinearGradient(
                    innerRect.left, innerRect.top, innerRect.left, innerRect.bottom,
                    intArrayOf(0x55FFFFFF.toInt(), 0x10FFFFFF.toInt(), 0x00FFFFFF.toInt()),
                    floatArrayOf(0f, 0.35f, 0.70f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(innerRect, cornerRadius - innerInset, cornerRadius - innerInset, innerBevelPaint)
        }
    }

    fun drawComfortGaugeRing(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        strokeWidth: Float,
        temp: Double?,
        palette: ThemePalette,
        density: Float = 1.0f,
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

        // 2. Compute Temperature & Active Angle
        val currentTemp = temp ?: 22.0
        val ratio = ((currentTemp - 15.0) / 15.0).coerceIn(0.06, 1.0).toFloat()
        val sweepAngle = 270f * ratio
        val (activeColor, statusText) = getComfortInfo(currentTemp, palette)

        // 3. Stylish Scale Ticks (Risks) along the arc
        val numTicks = 9
        for (i in 0 until numTicks) {
            val tickAngleDeg = 135f + i * (270f / (numTicks - 1))
            val rad = Math.toRadians(tickAngleDeg.toDouble())
            val isMajor = (i == 0 || i == (numTicks - 1) / 2 || i == numTicks - 1)
            val tickLen = if (isMajor) strokeWidth * 0.95f else strokeWidth * 0.55f
            val innerR = radius + strokeWidth * 0.85f
            val outerR = innerR + tickLen
            val x1 = (cx + innerR * cos(rad)).toFloat()
            val y1 = (cy + innerR * sin(rad)).toFloat()
            val x2 = (cx + outerR * cos(rad)).toFloat()
            val y2 = (cy + outerR * sin(rad)).toFloat()

            val isReached = tickAngleDeg <= (135f + sweepAngle + 1f)
            val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isReached) activeColor else Color.argb(55, 255, 255, 255)
                this.strokeWidth = if (isMajor) 1.8f * density else 1.1f * density
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(x1, y1, x2, y2, tickPaint)
        }

        // Scale Markers at Start (15°) and End (30°)
        val startRad = Math.toRadians(135.0)
        val endRad = Math.toRadians(405.0)
        val scaleOffset = radius + strokeWidth * 2.2f
        val startX = (cx + scaleOffset * cos(startRad)).toFloat()
        val startY = (cy + scaleOffset * sin(startRad)).toFloat()
        val endX = (cx + scaleOffset * cos(endRad)).toFloat()
        val endY = (cy + scaleOffset * sin(endRad)).toFloat()

        val scalePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(130, 255, 255, 255)
            textSize = (8.5f * density).coerceIn(7f * density, 11f * density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("15°", startX, startY + scalePaint.textSize * 0.35f, scalePaint)
        canvas.drawText("30°", endX, endY + scalePaint.textSize * 0.35f, scalePaint)

        // 4. Active Gradient Arc
        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = activeColor
        }
        canvas.drawArc(rect, 135f, sweepAngle, false, activePaint)

        // 5. Glowing Tip Indicator Needle Dot
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

        // 6. Clear Centered Status Label (without leading dot)
        if (showStatusText) {
            val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = activeColor
                textSize = (radius * 0.26f).coerceIn(9f * density, 13f * density)
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
        cornerRadius: Float = rect.height() / 2f,
        density: Float = context.resources.displayMetrics.density
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = palette.tileBg
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        if (palette.isGlassmorphism) {
            val pillSheen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                shader = LinearGradient(
                    rect.left, rect.top, rect.left, rect.bottom,
                    0x20FFFFFF.toInt(),
                    0x03FFFFFF.toInt(),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, pillSheen)
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            if (palette.isGlassmorphism) {
                shader = LinearGradient(
                    rect.left, rect.top, rect.right, rect.bottom,
                    palette.tileBorder,
                    Color.argb(20, 255, 255, 255),
                    Shader.TileMode.CLAMP
                )
            } else {
                color = palette.tileBorder
            }
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
            if (palette.isGlassmorphism && (Color.red(textColor) + Color.green(textColor) + Color.blue(textColor) > 300)) {
                setShadowLayer(1.5f * density, 0f, 0.8f * density, 0x80000000.toInt())
            }
        }
        val fontMetrics = textPaint.fontMetrics
        val textY = rect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2f

        if (iconRes == null) {
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(text, rect.centerX(), textY, textPaint)
        } else {
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(text, textLeft, textY, textPaint)
        }
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

    fun drawVectorLock(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        isLocked: Boolean,
        isAlert: Boolean,
        palette: ThemePalette,
        density: Float = 1.0f
    ) {
        val lockColor = when {
            isAlert -> palette.alertRed
            isLocked -> palette.okGreen
            else -> palette.textPrimary
        }
        val shackleColor = if (isAlert) palette.alertRed else 0xFFCFD6E4.toInt()

        val pw = size * 0.70f
        val ph = size * 0.85f
        val bodyH = ph * 0.52f
        val bodyTop = cy - ph * 0.05f
        val bodyRect = RectF(cx - pw / 2f, bodyTop, cx + pw / 2f, bodyTop + bodyH)

        // Shackle
        val shacklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = shackleColor
            style = Paint.Style.STROKE
            strokeWidth = (size * 0.12f).coerceAtLeast(2.2f * density)
            strokeCap = Paint.Cap.ROUND
        }
        val shackleW = pw * 0.58f
        val shackleH = ph * 0.45f
        val shackleRect = RectF(cx - shackleW / 2f, bodyTop - shackleH, cx + shackleW / 2f, bodyTop + shackleH * 0.2f)

        val path = Path()
        if (isLocked) {
            path.arcTo(shackleRect, 180f, 180f, false)
            path.lineTo(cx + shackleW / 2f, bodyTop)
            path.moveTo(cx - shackleW / 2f, bodyTop)
        } else {
            val openRect = RectF(cx - shackleW / 2f, bodyTop - shackleH * 1.25f, cx + shackleW / 2f, bodyTop - shackleH * 0.05f)
            path.arcTo(openRect, 180f, 180f, false)
            path.lineTo(cx - shackleW / 2f, bodyTop)
        }
        canvas.drawPath(path, shacklePaint)

        // Body
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lockColor
            style = Paint.Style.FILL
            if (palette.isGlassmorphism) setShadowLayer(3.5f * density, 0f, 1.2f * density, 0x85000000.toInt())
        }
        canvas.drawRoundRect(bodyRect, 7f * density, 7f * density, bodyPaint)

        // Keyhole
        val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF141722.toInt()
            style = Paint.Style.FILL
        }
        val keyCy = bodyRect.centerY() - 1f * density
        canvas.drawCircle(cx, keyCy, size * 0.07f, holePaint)
        canvas.drawRect(cx - size * 0.035f, keyCy, cx + size * 0.035f, keyCy + size * 0.14f, holePaint)
    }

    fun drawVectorCar(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        isRunning: Boolean,
        palette: ThemePalette,
        density: Float = 1.0f
    ) {
        val cw = size * 1.35f
        val ch = size * 0.70f

        val carPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isRunning) palette.okGreen else 0xFF8A93A4.toInt()
            style = Paint.Style.FILL
            if (palette.isGlassmorphism && isRunning) {
                setShadowLayer(4f * density, 0f, 1f * density, 0x8000D2B4.toInt())
            }
        }

        // Headlight beam if running
        if (isRunning) {
            val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x4000D2B4.toInt()
                style = Paint.Style.FILL
            }
            val beamPath = Path().apply {
                moveTo(cx + cw * 0.48f, cy - ch * 0.05f)
                lineTo(cx + cw * 0.72f, cy - ch * 0.25f)
                lineTo(cx + cw * 0.72f, cy + ch * 0.25f)
                close()
            }
            canvas.drawPath(beamPath, beamPaint)
        }

        val left = cx - cw / 2f
        val right = cx + cw / 2f
        val top = cy - ch / 2f
        val bottom = cy + ch * 0.25f

        // Sleek modern aerodynamic car silhouette
        val bodyPath = Path().apply {
            moveTo(left + cw * 0.06f, bottom)
            quadTo(left, bottom - ch * 0.15f, left + cw * 0.02f, bottom - ch * 0.35f)
            lineTo(left + cw * 0.16f, bottom - ch * 0.52f)
            quadTo(left + cw * 0.28f, top + ch * 0.05f, left + cw * 0.45f, top)
            lineTo(left + cw * 0.65f, top + ch * 0.02f)
            quadTo(left + cw * 0.82f, bottom - ch * 0.52f, left + cw * 0.86f, bottom - ch * 0.42f)
            lineTo(right - cw * 0.02f, bottom - ch * 0.32f)
            quadTo(right, bottom - ch * 0.15f, right - cw * 0.04f, bottom)
            close()
        }
        canvas.drawPath(bodyPath, carPaint)

        // Tinted Windows
        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isRunning) 0x65FFFFFF else 0x40000000.toInt()
            style = Paint.Style.FILL
        }
        val glassPath = Path().apply {
            moveTo(left + cw * 0.32f, top + ch * 0.16f)
            lineTo(left + cw * 0.64f, top + ch * 0.10f)
            lineTo(left + cw * 0.76f, bottom - ch * 0.50f)
            lineTo(left + cw * 0.32f, bottom - ch * 0.50f)
            close()
        }
        canvas.drawPath(glassPath, glassPaint)

        // Wheels
        val wheelY = bottom + ch * 0.10f
        val wheelR = size * 0.15f
        val tirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF141722.toInt()
            style = Paint.Style.FILL
        }
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isRunning) palette.okGreen else 0xFFB0B7C6.toInt()
            style = Paint.Style.FILL
        }

        val w1X = left + cw * 0.26f
        val w2X = right - cw * 0.24f

        canvas.drawCircle(w1X, wheelY, wheelR, tirePaint)
        canvas.drawCircle(w1X, wheelY, wheelR * 0.52f, rimPaint)

        canvas.drawCircle(w2X, wheelY, wheelR, tirePaint)
        canvas.drawCircle(w2X, wheelY, wheelR * 0.52f, rimPaint)
    }

    fun drawVectorLightBulb(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        isOn: Boolean,
        palette: ThemePalette,
        density: Float = 1.0f
    ) {
        val bh = size * 0.85f
        val bw = size * 0.65f
        val bulbCenterY = cy - bh * 0.18f

        if (isOn) {
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x35FFA000.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, bulbCenterY, size * 0.55f, glowPaint)

            val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFD54F.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 2.0f * density
                strokeCap = Paint.Cap.ROUND
            }
            val rayDistInner = size * 0.44f
            val rayDistOuter = size * 0.58f
            val angles = intArrayOf(-135, -90, -45, 0, 45, 90, 135, 180)
            for (ang in angles) {
                if (ang in 60..120) continue
                val rad = Math.toRadians(ang.toDouble())
                val x1 = cx + (cos(rad) * rayDistInner).toFloat()
                val y1 = bulbCenterY + (sin(rad) * rayDistInner).toFloat()
                val x2 = cx + (cos(rad) * rayDistOuter).toFloat()
                val y2 = bulbCenterY + (sin(rad) * rayDistOuter).toFloat()
                canvas.drawLine(x1, y1, x2, y2, rayPaint)
            }
        }

        // Bulb dome
        val domePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isOn) 0xFFFFD233.toInt() else 0xFF4A5264.toInt()
            style = Paint.Style.FILL
            if (isOn && palette.isGlassmorphism) setShadowLayer(5f * density, 0f, 1f * density, 0xFFFFA000.toInt())
        }
        val domeR = bw / 2f
        canvas.drawCircle(cx, bulbCenterY, domeR, domePaint)

        val taperPath = Path().apply {
            moveTo(cx - domeR * 0.85f, bulbCenterY + domeR * 0.50f)
            lineTo(cx - bw * 0.28f, cy + bh * 0.22f)
            lineTo(cx + bw * 0.28f, cy + bh * 0.22f)
            lineTo(cx + domeR * 0.85f, bulbCenterY + domeR * 0.50f)
            close()
        }
        canvas.drawPath(taperPath, domePaint)

        // Base screw threads
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isOn) 0xFF8E95A3.toInt() else 0xFF353C4B.toInt()
            style = Paint.Style.FILL
        }
        val baseRect = RectF(cx - bw * 0.24f, cy + bh * 0.22f, cx + bw * 0.24f, cy + bh * 0.36f)
        canvas.drawRoundRect(baseRect, 3f * density, 3f * density, basePaint)
        canvas.drawCircle(cx, cy + bh * 0.38f, 2.5f * density, basePaint)
    }

    fun drawVectorLedStrip(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        isOn: Boolean,
        palette: ThemePalette,
        density: Float = 1.0f
    ) {
        val sw = size * 1.25f
        val sh = size * 0.55f

        val stripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isOn) 0xFF2A2D3A.toInt() else 0xFF1C1F28.toInt()
            style = Paint.Style.STROKE
            strokeWidth = (size * 0.16f).coerceIn(4f * density, 8f * density)
            strokeCap = Paint.Cap.ROUND
        }
        val stripPath = Path().apply {
            moveTo(cx - sw * 0.44f, cy + sh * 0.15f)
            cubicTo(cx - sw * 0.20f, cy - sh * 0.35f, cx + sw * 0.20f, cy + sh * 0.35f, cx + sw * 0.44f, cy - sh * 0.15f)
        }
        canvas.drawPath(stripPath, stripPaint)

        val colors = if (isOn) {
            intArrayOf(0xFFFF3366.toInt(), 0xFFFFD233.toInt(), 0xFF00E676.toInt(), 0xFF00D2B4.toInt(), 0xFF2979FF.toInt(), 0xFFE040FB.toInt())
        } else {
            intArrayOf(0xFF505666.toInt(), 0xFF505666.toInt(), 0xFF505666.toInt(), 0xFF505666.toInt(), 0xFF505666.toInt(), 0xFF505666.toInt())
        }

        val dotR = (size * 0.055f).coerceIn(2f * density, 3.5f * density)
        val n = colors.size
        for (i in 0 until n) {
            val t = (i + 0.5f) / n
            val p0X = cx - sw * 0.44f; val p0Y = cy + sh * 0.15f
            val p1X = cx - sw * 0.20f; val p1Y = cy - sh * 0.35f
            val p2X = cx + sw * 0.20f; val p2Y = cy + sh * 0.35f
            val p3X = cx + sw * 0.44f; val p3Y = cy - sh * 0.15f

            val u = 1f - t
            val px = u*u*u*p0X + 3*u*u*t*p1X + 3*u*t*t*p2X + t*t*t*p3X
            val py = u*u*u*p0Y + 3*u*u*t*p1Y + 3*u*t*t*p2Y + t*t*t*p3Y

            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors[i]
                style = Paint.Style.FILL
                if (isOn && palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 0.8f * density, colors[i])
            }
            canvas.drawCircle(px, py, dotR, dotPaint)
        }
    }

    fun drawVectorWaterDroplet(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        isAlert: Boolean,
        palette: ThemePalette,
        density: Float = 1.0f
    ) {
        val dropColor = if (isAlert) palette.alertRed else 0xFF00D2B4.toInt()
        val dw = size * 0.70f
        val dh = size * 0.95f

        val path = Path().apply {
            moveTo(cx, cy - dh * 0.48f)
            cubicTo(
                cx + dw * 0.55f, cy + dh * 0.05f,
                cx + dw * 0.55f, cy + dh * 0.45f,
                cx, cy + dh * 0.45f
            )
            cubicTo(
                cx - dw * 0.55f, cy + dh * 0.45f,
                cx - dw * 0.55f, cy + dh * 0.05f,
                cx, cy - dh * 0.48f
            )
            close()
        }

        val dropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dropColor
            style = Paint.Style.FILL
            if (palette.isGlassmorphism) {
                setShadowLayer(3.5f * density, 0f, 1.2f * density, 0x85000000.toInt())
            }
        }
        canvas.drawPath(path, dropPaint)

        val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = (size * 0.08f).coerceAtLeast(1.8f * density)
            strokeCap = Paint.Cap.ROUND
        }
        val glossPath = Path().apply {
            val gx = cx - dw * 0.22f
            val gy = cy + dh * 0.05f
            val gr = dw * 0.20f
            arcTo(RectF(gx - gr, gy - gr, gx + gr, gy + gr), 180f, 90f, false)
        }
        canvas.drawPath(glossPath, glossPaint)
    }

    // --- FULL-CARD WIDGET RENDERING IMPLEMENTATIONS ---

    /**
     * 2x1 Compact Pill:
     * Room / Device Name (top-left) + Large state/temperature (bottom-left) +
     * High-clarity Frosted Glass Pill (right) with zero clipping or text overflow!
     */
    fun render2x1WidgetBitmap(
        context: Context,
        widthPx: Int,
        heightPx: Int,
        data: WidgetDataManager.Compact2x1Data,
        theme: String
    ): Bitmap {
        val w = widthPx.coerceAtLeast(220)
        val h = heightPx.coerceAtLeast(90)
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
        val actionHotspotWidth = 36f * density

        val st = data.state

        // 1. Top Title (Icon + Name) - compact and non-overflowing
        val titleText = "${st.iconEmoji} ${data.title}"
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textSecondary
            textSize = (11f * density).coerceIn(9.5f * density, 12f * density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x90000000.toInt())
        }
        val maxTitleW = w - padLeft - actionHotspotWidth
        val safeTitle = ellipsizeText(titleText, titlePaint, maxTitleW)
        val titleY = padTop + titlePaint.textSize
        canvas.drawText(safeTitle, padLeft, titleY, titlePaint)

        // 2. Secondary badge calculation
        val badgeText = when {
            st.badge1 != null && st.badge2 != null -> "${st.badge1} • ${st.badge2}"
            st.badge1 != null -> st.badge1
            st.subText != null && !st.subText.startsWith("Датчик") && !st.subText.startsWith("Освещение") -> st.subText
            else -> null
        }

        var pillLeft = w - padRight
        if (badgeText != null) {
            val pillH = 22f * density
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 10f * density
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val measuredBadgeW = badgePaint.measureText(badgeText) + 16f * density
            val pillW = measuredBadgeW.coerceIn(40f * density, (w - padLeft - padRight) * 0.48f)
            pillLeft = w - padRight - pillW
            val pillTop = h - padBottom - pillH
            val pillRect = RectF(pillLeft, pillTop, w - padRight, pillTop + pillH)
            val badgeColor = if (st.isAlert) palette.alertRed else palette.accentCold
            drawGlassPill(context, canvas, pillRect, null, badgeText, badgeColor, palette, density = density)
        }

        // 3. Main Text (Big bold state or temperature) - scaled to available width!
        val maxMainW = (pillLeft - padLeft - 6f * density).coerceAtLeast(30f * density)
        val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (st.isAlert) palette.alertRed else palette.textPrimary
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            if (palette.isGlassmorphism) setShadowLayer(3.5f * density, 0f, 1.2f * density, 0x85000000.toInt())
        }

        var targetMainSize = (h * 0.38f).coerceIn(16f * density, 26f * density)
        while (targetMainSize > 12f * density) {
            mainPaint.textSize = targetMainSize
            if (mainPaint.measureText(st.mainText) <= maxMainW) break
            targetMainSize -= 1f * density
        }
        mainPaint.textSize = targetMainSize

        val mainY = h - padBottom - 2f * density
        val safeMain = ellipsizeText(st.mainText, mainPaint, maxMainW)
        canvas.drawText(safeMain, padLeft, mainY, mainPaint)

        return bitmap
    }

    /**
     * 2x2 Square Widget:
     * Custom tailored display for each device type!
     * Climate sensor gets comfort gauge arc; Vehicle, TV, Kettle, Switch, Leak get
     * large prominent state cards with secondary badges and NO temperature arc!
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

        val pad = 12f * density
        val st = data.state

        // 1. Top Title (Icon + Name) - reserve clearance on right so it NEVER touches action buttons!
        val maxTitleW = w - pad - 42f * density
        val titleText = "${st.iconEmoji} ${data.title}"
        var titleSize = 12.5f * density
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textSecondary
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x90000000.toInt())
        }
        while (titleSize > 8.5f * density) {
            titlePaint.textSize = titleSize
            if (titlePaint.measureText(titleText) <= maxTitleW) break
            titleSize -= 0.5f * density
        }
        val safeTitle = ellipsizeText(titleText, titlePaint, maxTitleW)
        canvas.drawText(safeTitle, pad, pad + titleSize, titlePaint)

        val cx = w / 2f

        if (st.hasComfortArc) {
            // --- CLIMATE SENSOR MODE (with Centered Comfort Gauge Ring) ---
            val availTop = pad + titleSize + 8f * density
            val availBottom = if (st.badge1 != null) (h - pad - 28f * density) else (h - pad - 6f * density)
            val cy = (availTop + availBottom) / 2f
            val gaugeRadius = min((w - pad * 2) / 2f - 4f * density, (availBottom - availTop) / 2f - 4f * density).coerceIn(24f * density, 48f * density)
            val strokeWidth = 4.5f * density
            drawComfortGaugeRing(canvas, cx, cy, gaugeRadius, strokeWidth, st.rawTemperature, palette, density = density, showStatusText = false)

            val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.textPrimary
                textSize = (gaugeRadius * 0.54f).coerceIn(13f * density, 21f * density)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                if (palette.isGlassmorphism) setShadowLayer(4f * density, 0f, 1.5f * density, 0x85000000.toInt())
            }
            val fontMetrics = tempPaint.fontMetrics
            val tempY = cy - 2f * density - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(st.mainText, cx, tempY, tempPaint)

            val (comfortColor, comfortText) = getComfortInfo(st.rawTemperature, palette)
            val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = comfortColor
                textSize = (gaugeRadius * 0.25f).coerceIn(8.5f * density, 11f * density)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x80000000.toInt())
            }
            canvas.drawText(comfortText, cx, cy + gaugeRadius * 0.52f, statusPaint)

            if (st.badge1 != null) {
                val pillH = 25f * density
                val pillW = (w - pad * 2).coerceAtMost(96f * density)
                val pillRect = RectF(cx - pillW / 2f, h - pad - pillH, cx + pillW / 2f, h - pad)
                drawGlassPill(context, canvas, pillRect, null, st.badge1, palette.accentCold, palette, density = density)
            }
        } else if (st.category == "vehicle") {
            // --- VEHICLE MODE: Single card with large Lock & Car vector icons, NO words! ---
            val hasBadges = st.badge1 != null || st.badge2 != null
            val bottomH = if (hasBadges) 24f * density else 0f
            val centerTileTop = pad + titleSize + 8f * density
            val centerTileBottom = h - pad - bottomH - (if (bottomH > 0) 8f * density else 0f)

            val centerTileRect = RectF(pad, centerTileTop, w - pad, centerTileBottom)
            drawSquircleCard(canvas, centerTileRect, 14f * density, palette, isSubTile = true)

            val isRunning = st.subText?.contains("Заведён") == true
            val isLocked = !st.isAlert

            val iconSize = (centerTileRect.height() * 0.44f).coerceIn(24f * density, 36f * density)
            val lockCx = centerTileRect.left + centerTileRect.width() * 0.28f
            val carCx = centerTileRect.left + centerTileRect.width() * 0.72f
            val cardCy = centerTileRect.centerY()

            drawVectorLock(canvas, lockCx, cardCy, iconSize, isLocked, st.isAlert, palette, density)
            drawVectorCar(canvas, carCx, cardCy, iconSize, isRunning, palette, density)

            // Bottom Badges (Fuel & Battery)
            if (hasBadges) {
                val bottomY = h - pad - 22f * density
                val pillH = 22f * density
                val hasTwo = st.badge1 != null && st.badge2 != null
                if (hasTwo) {
                    val pW = (w - pad * 2 - 8f * density) / 2f
                    val b1Rect = RectF(pad, bottomY, pad + pW, bottomY + pillH)
                    val b2Rect = RectF(pad + pW + 8f * density, bottomY, w - pad, bottomY + pillH)
                    drawGlassPill(context, canvas, b1Rect, null, st.badge1!!, palette.accentComfort, palette, density = density)
                    drawGlassPill(context, canvas, b2Rect, null, st.badge2!!, palette.accentCold, palette, density = density)
                } else if (st.badge1 != null) {
                    val pW = (w - pad * 2).coerceAtMost(96f * density)
                    val bRect = RectF(cx - pW / 2f, bottomY, cx + pW / 2f, bottomY + pillH)
                    drawGlassPill(context, canvas, bRect, null, st.badge1, palette.accentComfort, palette, density = density)
                }
            }
        } else if (st.category == "light") {
            // --- LIGHTING MODE: Large Vector Lightbulb (Glowing ON / Dimmed OFF) ---
            val hasBadges = st.badge1 != null || st.badge2 != null
            val bottomH = if (hasBadges) 24f * density else 0f
            val centerTileTop = pad + titleSize + 8f * density
            val centerTileBottom = h - pad - bottomH - (if (bottomH > 0) 8f * density else 0f)

            val centerTileRect = RectF(pad, centerTileTop, w - pad, centerTileBottom)
            drawSquircleCard(canvas, centerTileRect, 14f * density, palette, isSubTile = true)

            val isLightOn = st.mainText == "ВКЛ" || st.mainText == "ВКЛЮЧЕН"
            val bulbSize = (centerTileRect.height() * 0.44f).coerceIn(24f * density, 38f * density)
            drawVectorLightBulb(canvas, cx, centerTileRect.top + centerTileRect.height() * 0.38f, bulbSize, isLightOn, palette, density)

            val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isLightOn) 0xFFFFD233.toInt() else palette.textSecondary
                textSize = (13f * density).coerceIn(11f * density, 15f * density)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                if (palette.isGlassmorphism) setShadowLayer(3f * density, 0f, 1f * density, 0x80000000.toInt())
            }
            canvas.drawText(st.mainText, cx, centerTileRect.bottom - 8f * density, statusPaint)

            if (hasBadges) {
                val bottomY = h - pad - 22f * density
                val pillH = 22f * density
                val pW = (w - pad * 2).coerceAtMost(96f * density)
                val bRect = RectF(cx - pW / 2f, bottomY, cx + pW / 2f, bottomY + pillH)
                drawGlassPill(context, canvas, bRect, null, st.badge1 ?: st.subText ?: "", palette.accentCold, palette, density = density)
            }
        } else if (st.category == "led_strip") {
            // --- LED STRIP / TV BACKLIGHT MODE: Large Glowing LED Ribbon ---
            val hasBadges = st.badge1 != null || st.badge2 != null
            val bottomH = if (hasBadges) 24f * density else 0f
            val centerTileTop = pad + titleSize + 8f * density
            val centerTileBottom = h - pad - bottomH - (if (bottomH > 0) 8f * density else 0f)

            val centerTileRect = RectF(pad, centerTileTop, w - pad, centerTileBottom)
            drawSquircleCard(canvas, centerTileRect, 14f * density, palette, isSubTile = true)

            val isStripOn = st.mainText == "ВКЛ" || st.mainText == "ВКЛЮЧЕН"
            val stripSize = (centerTileRect.height() * 0.44f).coerceIn(24f * density, 38f * density)
            drawVectorLedStrip(canvas, cx, centerTileRect.top + centerTileRect.height() * 0.38f, stripSize, isStripOn, palette, density)

            val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isStripOn) 0xFF00E676.toInt() else palette.textSecondary
                textSize = (13f * density).coerceIn(11f * density, 15f * density)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                if (palette.isGlassmorphism) setShadowLayer(3f * density, 0f, 1f * density, 0x80000000.toInt())
            }
            canvas.drawText(st.mainText, cx, centerTileRect.bottom - 8f * density, statusPaint)

            if (hasBadges) {
                val bottomY = h - pad - 22f * density
                val pillH = 22f * density
                val pW = (w - pad * 2).coerceAtMost(96f * density)
                val bRect = RectF(cx - pW / 2f, bottomY, cx + pW / 2f, bottomY + pillH)
                drawGlassPill(context, canvas, bRect, null, st.badge1 ?: st.subText ?: "", palette.accentCold, palette, density = density)
            }
        } else {
            // --- DEVICE SPECIFIC MODE (Kettle, TV, Switch, Vacuum, AC, Leak, Door) - NO ARC! ---
            val hasBadges = st.badge1 != null || st.badge2 != null
            val bottomH = if (hasBadges) 24f * density else 0f
            val centerTileTop = pad + titleSize + 8f * density
            val centerTileBottom = h - pad - bottomH - (if (bottomH > 0) 8f * density else 0f)
            val centerTileRect = RectF(pad, centerTileTop, w - pad, centerTileBottom)
            drawSquircleCard(canvas, centerTileRect, 14f * density, palette, isSubTile = true)

            if (st.category == "leak" && st.mainText.startsWith("АКБ")) {
                // When only battery is selected for leak sensor, display battery prominently and NO "СУХО"!
                val batPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette.accentCold
                    textSize = (centerTileRect.height() * 0.32f).coerceIn(18f * density, 25f * density)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    if (palette.isGlassmorphism) setShadowLayer(3.5f * density, 0f, 1.2f * density, 0x85000000.toInt())
                }
                val metrics = batPaint.fontMetrics
                val bY = centerTileRect.centerY() - 4f * density - (metrics.ascent + metrics.descent) / 2f
                canvas.drawText(st.mainText, cx, bY, batPaint)

                val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette.textSecondary
                    textSize = 10f * density
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("Заряд батареи", cx, centerTileRect.bottom - 7f * density, subPaint)
            } else {
                val isOff = st.mainText == "ВЫКЛ" || st.mainText == "ВЫКЛЮЧЕН" || st.mainText == "НА БАЗЕ"
                val stateColor = when {
                    st.isAlert -> palette.alertRed
                    isOff -> palette.textSecondary
                    st.mainText == "ВКЛ" || st.mainText == "ВКЛЮЧЕН" || st.mainText == "УБОРКА" -> palette.okGreen
                    else -> palette.textPrimary
                }

                val statePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = stateColor
                    textSize = (centerTileRect.height() * 0.32f).coerceIn(16f * density, 24f * density)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    if (palette.isGlassmorphism) setShadowLayer(3.5f * density, 0f, 1.2f * density, 0x85000000.toInt())
                }
                val stateMetrics = statePaint.fontMetrics
                val stateY = centerTileRect.centerY() - (if (st.subText != null) 4f * density else 0f) - (stateMetrics.ascent + stateMetrics.descent) / 2f
                val safeState = ellipsizeText(st.mainText, statePaint, centerTileRect.width() - 12f * density)
                canvas.drawText(safeState, cx, stateY, statePaint)

                if (st.subText != null) {
                    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = if (st.isAlert) palette.alertRed else palette.textSecondary
                        textSize = 10f * density
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                        if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x80000000.toInt())
                    }
                    val safeSub = ellipsizeText(st.subText, subPaint, centerTileRect.width() - 12f * density)
                    canvas.drawText(safeSub, cx, centerTileRect.bottom - 7f * density, subPaint)
                }
            }

            // Bottom Badges
            if (hasBadges && !st.mainText.startsWith("АКБ")) {
                val bottomY = h - pad - 22f * density
                val pillH = 22f * density
                val hasTwo = st.badge1 != null && st.badge2 != null
                if (hasTwo) {
                    val pillW = (w - pad * 2 - 8f * density) / 2f
                    val r1 = RectF(pad, bottomY, pad + pillW, bottomY + pillH)
                    val r2 = RectF(pad + pillW + 8f * density, bottomY, w - pad, bottomY + pillH)
                    drawGlassPill(context, canvas, r1, null, st.badge1!!, palette.accentComfort, palette)
                    drawGlassPill(context, canvas, r2, null, st.badge2!!, palette.accentCold, palette)
                } else if (st.badge1 != null) {
                    val pillW = (w - pad * 2).coerceAtMost(96f * density)
                    val r = RectF(cx - pillW / 2f, bottomY, cx + pillW / 2f, bottomY + pillH)
                    drawGlassPill(context, canvas, r, null, st.badge1, palette.accentComfort, palette)
                }
            }
        }

        return bitmap
    }

    /**
     * 4x1 Modern Glass Ribbon:
     * Up to 2 distinct Frosted Glass Pods with ample horizontal space.
     * Labels and values never stick out or overflow!
     */
    fun render4x1WidgetBitmap(
        context: Context,
        widthPx: Int,
        heightPx: Int,
        slots: List<WidgetDataManager.BarSlotItem>,
        theme: String
    ): Bitmap {
        val w = widthPx.coerceAtLeast(300)
        val h = heightPx.coerceAtLeast(85)
        val density = context.resources.displayMetrics.density
        val palette = getThemePalette(theme)

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cornerRadius = palette.cornerRadiusDp * density
        drawSquircleCard(canvas, RectF(0f, 0f, w.toFloat(), h.toFloat()), cornerRadius, palette)

        val actionHotspotWidth = 36f * density
        val padLeft = 10f * density
        val padRight = 8f * density
        val padTop = 7f * density
        val padBottom = 7f * density
        val availWidth = w - padLeft - padRight - actionHotspotWidth

        val slotCount = slots.size.coerceIn(1, 2)
        val gap = 6f * density
        val slotW = (availWidth - gap * (slotCount - 1)) / slotCount

        for (i in 0 until slotCount) {
            val slot = slots[i]
            val st = slot.state
            val slotLeft = padLeft + i * (slotW + gap)
            val slotRect = RectF(slotLeft, padTop, slotLeft + slotW, h - padBottom)

            drawSquircleCard(canvas, slotRect, 12f * density, palette, isSubTile = true)

            val innerPad = if (slotCount == 2) 7f * density else 9f * density

            // Top: Icon + Room / Device Name
            val roomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.textSecondary
                textSize = if (slotCount == 2) 9f * density else 10.5f * density
                if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x90000000.toInt())
            }
            val titleStr = "${st.iconEmoji} ${slot.title}"
            val roomName = ellipsizeText(titleStr, roomPaint, slotW - innerPad * 2)
            canvas.drawText(roomName, slotLeft + innerPad, padTop + innerPad + roomPaint.textSize, roomPaint)

            val contentY = slotRect.bottom - innerPad - 2f * density

            if (st.category == "vehicle") {
                // Vehicle: Show Lock, Engine, Fuel, and Battery
                val isRunning = st.subText?.contains("Заведён") == true
                val lockText = if (st.isAlert) "🔓 ОТКРЫТ" else "🔒 ЗАКРЫТ"
                val lockColor = if (st.isAlert) palette.alertRed else palette.okGreen

                val lockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = lockColor
                    textSize = if (slotCount == 2) 11f * density else 13f * density
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val safeLock = ellipsizeText(lockText, lockPaint, (slotW - innerPad * 2) * 0.48f)
                canvas.drawText(safeLock, slotLeft + innerPad, contentY, lockPaint)

                // Combine fuel & battery & engine into badge
                val badges = mutableListOf<String>()
                if (st.badge1 != null) badges.add(st.badge1)
                if (st.badge2 != null) badges.add(st.badge2)
                if (isRunning) badges.add("⚡ Заведён")
                val combinedBadge = badges.joinToString(" • ")

                if (combinedBadge.isNotBlank()) {
                    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = palette.accentComfort
                        textSize = if (slotCount == 2) 8.5f * density else 9.5f * density
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    val lockWidth = lockPaint.measureText(safeLock)
                    val availW = slotRect.right - innerPad - (slotLeft + innerPad + lockWidth + 4f * density)
                    if (availW > 16f * density) {
                        val safeBadge = ellipsizeText(combinedBadge, badgePaint, availW)
                        val bX = slotRect.right - innerPad - badgePaint.measureText(safeBadge)
                        canvas.drawText(safeBadge, bX, contentY, badgePaint)
                    }
                }
            } else {
                val isOff = st.mainText == "ВЫКЛ" || st.mainText == "ВЫКЛЮЧЕН" || st.mainText == "НА БАЗЕ"
                val stateColor = when {
                    st.isAlert -> palette.alertRed
                    isOff -> palette.textSecondary
                    st.mainText == "ВКЛ" || st.mainText == "ВКЛЮЧЕН" || st.mainText == "УБОРКА" || st.mainText == "ЗАКРЫТ" || st.mainText == "НА ОХРАНЕ" -> palette.okGreen
                    else -> palette.textPrimary
                }

                val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = stateColor
                    textSize = if (slotCount == 2) 12f * density else 14f * density
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    if (palette.isGlassmorphism) setShadowLayer(3.5f * density, 0f, 1.2f * density, 0x85000000.toInt())
                }

                val badgeText = st.badge1 ?: st.subText
                val maxMainW = if (badgeText != null) (slotW - innerPad * 2) * 0.48f else (slotW - innerPad * 2)
                val safeMain = ellipsizeText(st.mainText, mainPaint, maxMainW)
                canvas.drawText(safeMain, slotLeft + innerPad, contentY, mainPaint)

                if (badgeText != null) {
                    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = if (st.isAlert) palette.alertRed else palette.accentCold
                        textSize = if (slotCount == 2) 8.5f * density else 10f * density
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x80000000.toInt())
                    }
                    val mainWidth = mainPaint.measureText(safeMain)
                    val availBadgeW = slotRect.right - innerPad - (slotLeft + innerPad + mainWidth + 4f * density)
                    if (availBadgeW > 14f * density) {
                        val safeBadge = ellipsizeText(badgeText, badgePaint, availBadgeW)
                        val badgeX = slotRect.right - innerPad - badgePaint.measureText(safeBadge)
                        canvas.drawText(safeBadge, badgeX, contentY, badgePaint)
                    }
                }
            }
        }

        return bitmap
    }

    /**
     * Symmetrical Pod Card for 4x2 Widget:
     * Used for both left and right pods in 2-device mode and left pod in 3-device mode.
     * Identical layout, identical vertical positions, zero text clipping or overlapping!
     */
    private fun draw4x2PodCard(
        context: Context,
        canvas: Canvas,
        cardRect: RectF,
        dev: DeviceWidgetState,
        palette: ThemePalette,
        density: Float,
        showTitleInside: Boolean = true
    ) {
        drawSquircleCard(canvas, cardRect, 18f * density, palette, isSubTile = true)

        val innerPad = 10f * density

        // 1. Top Title (Room or Device Name) - inside the pod if enabled
        val centerAvailTop = if (showTitleInside) {
            val titleText = "${dev.iconEmoji} ${dev.roomName ?: dev.name}"
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.textSecondary
                textSize = (11f * density).coerceIn(9.5f * density, 12f * density)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x90000000.toInt())
            }
            val safeTitle = ellipsizeText(titleText, titlePaint, cardRect.width() - innerPad * 2)
            val titleY = cardRect.top + innerPad + titlePaint.textSize
            canvas.drawText(safeTitle, cardRect.left + innerPad, titleY, titlePaint)
            titleY + 6f * density
        } else {
            cardRect.top + innerPad
        }

        // 2. Bottom Badges / Pills (badge1, badge2)
        val pillH = 22f * density
        val pillY = cardRect.bottom - innerPad - pillH
        val hasBadge1 = dev.badge1 != null
        val hasBadge2 = dev.badge2 != null

        if (hasBadge1 && hasBadge2) {
            val pillW = (cardRect.width() - innerPad * 2 - 6f * density) / 2f
            val r1 = RectF(cardRect.left + innerPad, pillY, cardRect.left + innerPad + pillW, pillY + pillH)
            val r2 = RectF(cardRect.left + innerPad + pillW + 6f * density, pillY, cardRect.right - innerPad, pillY + pillH)
            drawGlassPill(context, canvas, r1, null, dev.badge1!!, palette.accentComfort, palette, density = density)
            drawGlassPill(context, canvas, r2, null, dev.badge2!!, palette.accentCold, palette, density = density)
        } else if (hasBadge1) {
            val pillW = (cardRect.width() - innerPad * 2).coerceAtMost(86f * density)
            val r = RectF(cardRect.centerX() - pillW / 2f, pillY, cardRect.centerX() + pillW / 2f, pillY + pillH)
            drawGlassPill(context, canvas, r, null, dev.badge1!!, palette.accentCold, palette, density = density)
        }

        // 3. Center Area
        val centerAvailBottom = if (hasBadge1 || hasBadge2) (pillY - 6f * density) else (cardRect.bottom - innerPad)
        val centerCy = (centerAvailTop + centerAvailBottom) / 2f

        if (dev.hasComfortArc) {
            // Climate in Pod
            val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.textPrimary
                textSize = (cardRect.height() * 0.30f).coerceIn(18f * density, 27f * density)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                if (palette.isGlassmorphism) setShadowLayer(3.5f * density, 0f, 1.2f * density, 0x85000000.toInt())
            }
            val metrics = tempPaint.fontMetrics
            val tY = centerCy - 4f * density - (metrics.ascent + metrics.descent) / 2f
            canvas.drawText(dev.mainText, cardRect.centerX(), tY, tempPaint)

            val (comfortColor, comfortText) = getComfortInfo(dev.rawTemperature, palette)
            val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = comfortColor
                textSize = 9.5f * density
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x80000000.toInt())
            }
            canvas.drawText(comfortText, cardRect.centerX(), tY + statusPaint.textSize + 6f * density, statusPaint)
        } else if (dev.category == "leak") {
            // Leak sensor in Pod
            val dropSize = (cardRect.height() * 0.28f).coerceIn(20f * density, 30f * density)
            val dropCy = centerCy - 6f * density
            drawVectorWaterDroplet(canvas, cardRect.centerX(), dropCy, dropSize, dev.isAlert, palette, density)

            val stPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (dev.isAlert) palette.alertRed else palette.okGreen
                textSize = 12f * density
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x80000000.toInt())
            }
            val leakStatus = if (dev.isAlert) "ПРОТЕЧКА!" else if (dev.mainText.startsWith("АКБ")) dev.mainText else "СУХО"
            canvas.drawText(leakStatus, cardRect.centerX(), centerAvailBottom - 2f * density, stPaint)
        } else if (dev.category == "light") {
            // Lighting in Pod
            val isLightOn = dev.mainText == "ВКЛ" || dev.mainText == "ВКЛЮЧЕН"
            val bulbSize = (cardRect.height() * 0.34f).coerceIn(24f * density, 34f * density)
            val bulbCy = centerCy - 6f * density
            drawVectorLightBulb(canvas, cardRect.centerX(), bulbCy, bulbSize, isLightOn, palette, density)

            val stPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isLightOn) 0xFFFFD233.toInt() else palette.textSecondary
                textSize = 11.5f * density
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x80000000.toInt())
            }
            canvas.drawText(dev.mainText, cardRect.centerX(), centerAvailBottom - 2f * density, stPaint)
        } else if (dev.category == "led_strip") {
            // LED Strip in Pod
            val isStripOn = dev.mainText == "ВКЛ" || dev.mainText == "ВКЛЮЧЕН"
            val stripSize = (cardRect.height() * 0.34f).coerceIn(24f * density, 34f * density)
            val stripCy = centerCy - 6f * density
            drawVectorLedStrip(canvas, cardRect.centerX(), stripCy, stripSize, isStripOn, palette, density)

            val stPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isStripOn) 0xFF00E676.toInt() else palette.textSecondary
                textSize = 11.5f * density
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x80000000.toInt())
            }
            canvas.drawText(dev.mainText, cardRect.centerX(), centerAvailBottom - 2f * density, stPaint)
        } else if (dev.category == "vehicle") {
            // Vehicle in Pod
            val isRunning = dev.subText?.contains("Заведён") == true
            val isLocked = !dev.isAlert
            val iconSize = (cardRect.height() * 0.30f).coerceIn(20f * density, 28f * density)
            val vCy = centerCy - 6f * density
            drawVectorLock(canvas, cardRect.centerX() - iconSize * 0.65f, vCy, iconSize, isLocked, dev.isAlert, palette, density)
            drawVectorCar(canvas, cardRect.centerX() + iconSize * 0.65f, vCy, iconSize, isRunning, palette, density)

            val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (dev.isAlert) palette.alertRed else palette.okGreen
                textSize = 11f * density
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x80000000.toInt())
            }
            val secStr = if (isLocked) "ЗАКРЫТ" else "ОТКРЫТ"
            canvas.drawText(secStr, cardRect.centerX(), centerAvailBottom - 2f * density, secPaint)
        } else {
            // Kettle, TV, Switch, Vacuum, Door
            val isOff = dev.mainText == "ВЫКЛ" || dev.mainText == "ВЫКЛЮЧЕН" || dev.mainText == "НА БАЗЕ"
            val stateColor = when {
                dev.isAlert -> palette.alertRed
                isOff -> palette.textSecondary
                dev.mainText == "ВКЛ" || dev.mainText == "ВКЛЮЧЕН" || dev.mainText == "УБОРКА" || dev.mainText == "ЗАКРЫТ" -> palette.okGreen
                else -> palette.textPrimary
            }

            val statePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = stateColor
                textSize = (cardRect.height() * 0.26f).coerceIn(16f * density, 24f * density)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                if (palette.isGlassmorphism) setShadowLayer(3.5f * density, 0f, 1.2f * density, 0x85000000.toInt())
            }
            val metrics = statePaint.fontMetrics
            val sY = centerCy - (if (dev.subText != null) 6f * density else 0f) - (metrics.ascent + metrics.descent) / 2f
            val safeState = ellipsizeText(dev.mainText, statePaint, cardRect.width() - innerPad * 2)
            canvas.drawText(safeState, cardRect.centerX(), sY, statePaint)

            if (dev.subText != null) {
                val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (dev.isAlert) palette.alertRed else palette.textSecondary
                    textSize = 10f * density
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x80000000.toInt())
                }
                val safeSub = ellipsizeText(dev.subText, subPaint, cardRect.width() - innerPad * 2)
                canvas.drawText(safeSub, cardRect.centerX(), sY + subPaint.textSize + 6f * density, subPaint)
            }
        }
    }

    /**
     * 4x2 Flagship Smart Home Hub Dashboard:
     * - 1 Device mode: Left side shows state/temp, right side features large prominent power tile/vector
     * - 2 Devices mode: Symmetrical Left & Right pods with zero text overlapping, room name inside each pod
     * - 3 Devices mode: Left pod + Right stacked dual-pods
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

        val padLeft = 14f * density
        val padTop = 11f * density
        val padRight = 12f * density
        val padBottom = 11f * density

        val primary = data.primaryState
        val secCount = data.secondaryDevices.size.coerceAtMost(2)
        val hasSecondaries = data.secondaryDevices.isNotEmpty()
        val isTwoDeviceMode = secCount == 1

        if (!hasSecondaries) {
            // ================= 1 DEVICE FULL-CARD MODE =================
            // Header with Room Name & Refresh Timestamp
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.textPrimary
                textSize = 14f * density
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                if (palette.isGlassmorphism) setShadowLayer(3f * density, 0f, 1.2f * density, 0x90000000.toInt())
            }
            val headerTitleStr = "${primary.iconEmoji} ${data.primaryRoomName}"
            val headerTitle = ellipsizeText(headerTitleStr, titlePaint, w - padLeft - 76f * density)
            val titleY = padTop + 13f * density
            canvas.drawText(headerTitle, padLeft, titleY, titlePaint)

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val updateTimeStr = if (data.lastUpdatedTimestamp > 0) {
                "Обновлено в " + timeFormat.format(Date(data.lastUpdatedTimestamp))
            } else {
                "Нажмите для обновления"
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.textSecondary
                textSize = 9.5f * density
                if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x85000000.toInt())
            }
            val subY = titleY + 14f * density
            canvas.drawText(updateTimeStr, padLeft, subY, subPaint)

            val bodyTop = subY + 12f * density
            val bodyBottom = h - padBottom
            val cardRect = RectF(padLeft, bodyTop, w - padRight, bodyBottom)
            drawSquircleCard(canvas, cardRect, 18f * density, palette, isSubTile = true)

            val innerPad = 14f * density

            if (primary.category == "leak") {
                // --- LEAK SENSOR: Large status, Water Droplet, and Battery pill (NO power button!) ---
                val isLeakAlert = primary.isAlert
                val leakStatus = if (isLeakAlert) "ПРОТЕЧКА!" else if (primary.mainText.startsWith("АКБ")) primary.mainText else "СУХО"
                val stateColor = if (isLeakAlert) palette.alertRed else palette.okGreen

                val statePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = stateColor
                    textSize = (cardRect.height() * 0.32f).coerceIn(22f * density, 32f * density)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    if (palette.isGlassmorphism) setShadowLayer(3.5f * density, 0f, 1.2f * density, 0x85000000.toInt())
                }
                val sY = cardRect.centerY() + 2f * density
                canvas.drawText(leakStatus, cardRect.left + innerPad, sY, statePaint)

                val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isLeakAlert) palette.alertRed else palette.textSecondary
                    textSize = 11.5f * density
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val subStr = if (isLeakAlert) "Обнаружена вода" else if (primary.mainText.startsWith("АКБ")) "Заряд батареи" else "Протечек нет"
                canvas.drawText(subStr, cardRect.left + innerPad, sY + subP.textSize + 8f * density, subP)

                if (primary.badge1 != null && !primary.mainText.startsWith("АКБ")) {
                    val pillH = 22f * density
                    val pillY = cardRect.bottom - innerPad - pillH
                    val pRect = RectF(cardRect.left + innerPad, pillY, cardRect.left + innerPad + 76f * density, pillY + pillH)
                    drawGlassPill(context, canvas, pRect, null, primary.badge1, palette.accentCold, palette, density = density)
                }

                val dropletSize = (cardRect.height() * 0.48f).coerceIn(34f * density, 48f * density)
                val dropletCx = cardRect.right - innerPad - 45f * density
                drawVectorWaterDroplet(canvas, dropletCx, cardRect.centerY(), dropletSize, isLeakAlert, palette, density)
            } else if (primary.category == "kettle") {
                // --- KETTLE: Large temperature or status on left, power button on right ---
                val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette.textPrimary
                    textSize = (cardRect.height() * 0.35f).coerceIn(24f * density, 34f * density)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val tY = cardRect.centerY() + 2f * density
                canvas.drawText(primary.mainText, cardRect.left + innerPad, tY, tempPaint)

                val stPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette.textSecondary
                    textSize = 11.5f * density
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText(primary.subText ?: "Чайник", cardRect.left + innerPad, tY + stPaint.textSize + 8f * density, stPaint)

                val pW = (cardRect.width() * 0.32f).coerceIn(100f * density, 130f * density)
                val pH = (cardRect.height() * 0.60f).coerceIn(44f * density, 60f * density)
                val pRect = RectF(cardRect.right - innerPad - pW, cardRect.centerY() - pH / 2f, cardRect.right - innerPad, cardRect.centerY() + pH / 2f)

                val isKettleOn = primary.badge1 == "ВКЛ" || primary.subText?.contains("Кипят") == true
                val pBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isKettleOn) 0x3D00D2B4.toInt() else 0x1EFFFFFF.toInt()
                    style = Paint.Style.FILL
                }
                val pStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isKettleOn) palette.okGreen else 0x30FFFFFF.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f * density
                }
                canvas.drawRoundRect(pRect, 14f * density, 14f * density, pBgPaint)
                canvas.drawRoundRect(pRect, 14f * density, 14f * density, pStrokePaint)

                val pTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isKettleOn) palette.okGreen else palette.textSecondary
                    textSize = (pH * 0.34f).coerceIn(12f * density, 15f * density)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val pTextMetrics = pTextPaint.fontMetrics
                val pTextY = pRect.centerY() - (pTextMetrics.ascent + pTextMetrics.descent) / 2f
                canvas.drawText(if (isKettleOn) "ВКЛ" else "ВЫКЛ", pRect.centerX(), pTextY, pTextPaint)
            } else if (primary.category == "light") {
                // --- LIGHTING: Status on left, Glowing bulb on right ---
                val isLightOn = primary.mainText == "ВКЛ" || primary.mainText == "ВКЛЮЧЕН"
                val stColor = if (isLightOn) palette.okGreen else palette.textSecondary
                val statePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = stColor
                    textSize = (cardRect.height() * 0.32f).coerceIn(22f * density, 32f * density)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val sY = cardRect.centerY() + 2f * density
                canvas.drawText(primary.mainText, cardRect.left + innerPad, sY, statePaint)

                val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette.textSecondary
                    textSize = 11.5f * density
                }
                canvas.drawText(primary.subText ?: "Освещение", cardRect.left + innerPad, sY + subP.textSize + 8f * density, subP)

                val bulbSize = (cardRect.height() * 0.50f).coerceIn(36f * density, 48f * density)
                drawVectorLightBulb(canvas, cardRect.right - innerPad - 45f * density, cardRect.centerY(), bulbSize, isLightOn, palette, density)
            } else if (primary.category == "led_strip") {
                // --- LED STRIP: Status on left, Glowing ribbon on right ---
                val isStripOn = primary.mainText == "ВКЛ" || primary.mainText == "ВКЛЮЧЕН"
                val statePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isStripOn) palette.okGreen else palette.textSecondary
                    textSize = (cardRect.height() * 0.32f).coerceIn(22f * density, 32f * density)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val sY = cardRect.centerY() + 2f * density
                canvas.drawText(primary.mainText, cardRect.left + innerPad, sY, statePaint)

                val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette.textSecondary
                    textSize = 11.5f * density
                }
                canvas.drawText(primary.subText ?: "Подсветка", cardRect.left + innerPad, sY + subP.textSize + 8f * density, subP)

                val stripSize = (cardRect.height() * 0.50f).coerceIn(36f * density, 48f * density)
                drawVectorLedStrip(canvas, cardRect.right - innerPad - 45f * density, cardRect.centerY(), stripSize, isStripOn, palette, density)
            } else if (primary.category == "vehicle") {
                // --- VEHICLE: Security & Engine on left, Badges below, Lock & Car on right ---
                val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (primary.isAlert) palette.alertRed else palette.textPrimary
                    textSize = (cardRect.height() * 0.24f).coerceIn(16f * density, 22f * density)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val sY = cardRect.top + innerPad + 36f * density
                canvas.drawText(primary.mainText, cardRect.left + innerPad, sY, secPaint)

                val engPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    val isRunning = primary.subText?.contains("Заведён") == true
                    color = if (isRunning) palette.okGreen else palette.textSecondary
                    textSize = 11f * density
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText(primary.subText ?: "", cardRect.left + innerPad, sY + engPaint.textSize + 6f * density, engPaint)

                var pX = cardRect.left + innerPad
                val pillH = 22f * density
                val pillY = cardRect.bottom - innerPad - pillH
                if (primary.badge1 != null) {
                    val pW = 70f * density
                    drawGlassPill(context, canvas, RectF(pX, pillY, pX + pW, pillY + pillH), null, primary.badge1, palette.accentComfort, palette, density = density)
                    pX += pW + 6f * density
                }
                if (primary.badge2 != null) {
                    val pW = 70f * density
                    drawGlassPill(context, canvas, RectF(pX, pillY, pX + pW, pillY + pillH), null, primary.badge2, palette.accentCold, palette, density = density)
                }

                val iconSize = (cardRect.height() * 0.36f).coerceIn(24f * density, 34f * density)
                val isRunning = primary.subText?.contains("Заведён") == true
                val isLocked = !primary.isAlert
                val rightCenter = cardRect.right - innerPad - 55f * density
                drawVectorLock(canvas, rightCenter - iconSize * 0.7f, cardRect.centerY(), iconSize, isLocked, primary.isAlert, palette, density)
                drawVectorCar(canvas, rightCenter + iconSize * 0.7f, cardRect.centerY(), iconSize, isRunning, palette, density)
            } else if (primary.hasComfortArc) {
                // --- CLIMATE: Temperature & Comfort status on left, Comfort Gauge on right ---
                val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette.textPrimary
                    textSize = (cardRect.height() * 0.35f).coerceIn(20f * density, 32f * density)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val tY = cardRect.centerY() - 4f * density
                canvas.drawText(primary.mainText, cardRect.left + innerPad, tY, tempPaint)

                val (comfortColor, comfortText) = getComfortInfo(primary.rawTemperature, palette)
                val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = comfortColor
                    textSize = 10.5f * density
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText(comfortText, cardRect.left + innerPad, tY + statusPaint.textSize + 6f * density, statusPaint)

                var pX = cardRect.left + innerPad
                val pillH = 22f * density
                val pillY = cardRect.bottom - innerPad - pillH
                if (primary.badge1 != null) {
                    val pW = 60f * density
                    drawGlassPill(context, canvas, RectF(pX, pillY, pX + pW, pillY + pillH), null, primary.badge1, palette.accentCold, palette, density = density)
                    pX += pW + 6f * density
                }
                if (primary.badge2 != null) {
                    val pW = 60f * density
                    drawGlassPill(context, canvas, RectF(pX, pillY, pX + pW, pillY + pillH), null, primary.badge2, palette.textSecondary, palette, density = density)
                }

                val gaugeRadius = (cardRect.height() * 0.28f).coerceIn(20f * density, 30f * density)
                val gaugeCx = cardRect.right - innerPad - gaugeRadius - 10f * density
                drawComfortGaugeRing(canvas, gaugeCx, cardRect.centerY(), gaugeRadius, 5f * density, primary.rawTemperature, palette, density = density, showStatusText = false)
            } else {
                // --- OTHER DEVICES: TV, Switch, Vacuum, etc. ---
                val isOff = primary.mainText == "ВЫКЛ" || primary.mainText == "ВЫКЛЮЧЕН" || primary.mainText == "НА БАЗЕ"
                val stateColor = when {
                    primary.isAlert -> palette.alertRed
                    isOff -> palette.textSecondary
                    primary.mainText == "ВКЛ" || primary.mainText == "ВКЛЮЧЕН" || primary.mainText == "УБОРКА" -> palette.okGreen
                    else -> palette.textPrimary
                }
                val statePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = stateColor
                    textSize = (cardRect.height() * 0.32f).coerceIn(20f * density, 30f * density)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val sY = cardRect.centerY() + 2f * density
                canvas.drawText(primary.mainText, cardRect.left + innerPad, sY, statePaint)

                if (primary.subText != null) {
                    val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = if (primary.isAlert) palette.alertRed else palette.textSecondary
                        textSize = 11.5f * density
                    }
                    canvas.drawText(primary.subText, cardRect.left + innerPad, sY + subP.textSize + 8f * density, subP)
                }

                var pX = cardRect.left + innerPad
                val pillH = 22f * density
                val pillY = cardRect.bottom - innerPad - pillH
                if (primary.badge1 != null) {
                    val pW = 70f * density
                    drawGlassPill(context, canvas, RectF(pX, pillY, pX + pW, pillY + pillH), null, primary.badge1, palette.accentComfort, palette, density = density)
                }

                val pW = (cardRect.width() * 0.32f).coerceIn(100f * density, 130f * density)
                val pH = (cardRect.height() * 0.60f).coerceIn(44f * density, 60f * density)
                val pRect = RectF(cardRect.right - innerPad - pW, cardRect.centerY() - pH / 2f, cardRect.right - innerPad, cardRect.centerY() + pH / 2f)
                val isOn = primary.mainText == "ВКЛ" || primary.mainText == "ВКЛЮЧЕН" || primary.mainText == "УБОРКА"
                val pBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isOn) 0x3D00D2B4.toInt() else 0x1EFFFFFF.toInt()
                    style = Paint.Style.FILL
                }
                val pStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isOn) palette.okGreen else 0x30FFFFFF.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f * density
                }
                canvas.drawRoundRect(pRect, 14f * density, 14f * density, pBgPaint)
                canvas.drawRoundRect(pRect, 14f * density, 14f * density, pStrokePaint)

                val pTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isOn) palette.okGreen else palette.textSecondary
                    textSize = (pH * 0.34f).coerceIn(12f * density, 15f * density)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val pTextMetrics = pTextPaint.fontMetrics
                val pTextY = pRect.centerY() - (pTextMetrics.ascent + pTextMetrics.descent) / 2f
                canvas.drawText(if (isOn) "ВКЛ" else "ВЫКЛ", pRect.centerX(), pTextY, pTextPaint)
            }
        } else if (isTwoDeviceMode) {
            // ================= 2 DEVICES SYMMETRICAL PODS MODE =================
            // No top header / time; Room names rendered ABOVE each box with gear clearance!
            val topHeaderH = 18f * density
            val bodyTop = padTop + topHeaderH + 6f * density
            val bodyBottom = h - padBottom

            val cardW = (w - padLeft - padRight - 10f * density) * 0.50f
            val leftCardRect = RectF(padLeft, bodyTop, padLeft + cardW, bodyBottom)
            val rightCardRect = RectF(padLeft + cardW + 10f * density, bodyTop, w - padRight, bodyBottom)

            val roomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.textSecondary
                textSize = (12f * density).coerceIn(10f * density, 13f * density)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x90000000.toInt())
            }
            val headerTextY = padTop + roomPaint.textSize

            // Room title above Left Box
            val leftRoomStr = "${primary.iconEmoji} ${primary.roomName ?: data.primaryRoomName}"
            val safeLeftRoom = ellipsizeText(leftRoomStr, roomPaint, cardW)
            canvas.drawText(safeLeftRoom, padLeft, headerTextY, roomPaint)

            // Room title above Right Box (38dp clearance from gear button!)
            val secDev = data.secondaryDevices[0]
            val rightRoomStr = "${secDev.iconEmoji} ${secDev.roomName ?: secDev.name}"
            val safeRightRoom = ellipsizeText(rightRoomStr, roomPaint, cardW - 38f * density)
            canvas.drawText(safeRightRoom, rightCardRect.left, headerTextY, roomPaint)

            draw4x2PodCard(context, canvas, leftCardRect, primary, palette, density, showTitleInside = false)
            draw4x2PodCard(context, canvas, rightCardRect, secDev, palette, density, showTitleInside = false)
        } else {
            // ================= 3 DEVICES MODE (Left Pod + Right Stacked Dual Pods) =================
            // No top header / time; Room names shown inside each box
            val bodyTop = padTop + 4f * density
            val bodyBottom = h - padBottom

            val cardW = (w - padLeft - padRight - 10f * density) * 0.50f
            val leftCardRect = RectF(padLeft, bodyTop, padLeft + cardW, bodyBottom)
            val rightLeft = padLeft + cardW + 10f * density
            val rightW = w - padRight - rightLeft

            draw4x2PodCard(context, canvas, leftCardRect, primary, palette, density, showTitleInside = true)

            val secGap = 6f * density
            val totalAvailH = bodyBottom - bodyTop
            val cardH = (totalAvailH - secGap) / 2f

            for (i in 0 until 2) {
                val dev = data.secondaryDevices[i]
                val cardTop = bodyTop + i * (cardH + secGap)
                val cardRect = RectF(rightLeft, cardTop, rightLeft + rightW, cardTop + cardH)

                drawSquircleCard(canvas, cardRect, 12f * density, palette, isSubTile = true)

                val cInnerPad = 8f * density
                val dTitle = "${dev.iconEmoji} ${dev.roomName ?: dev.name}"
                val dTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette.textSecondary
                    textSize = (10f * density).coerceIn(8.5f * density, 11f * density)
                    if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x90000000.toInt())
                }
                // Top card on right leaves 38dp clearance for gear button
                val maxTitleW = if (i == 0) (rightW - cInnerPad - 38f * density) else (rightW - cInnerPad * 2)
                val safeDTitle = ellipsizeText(dTitle, dTitlePaint, maxTitleW)
                canvas.drawText(safeDTitle, rightLeft + cInnerPad, cardTop + cInnerPad + dTitlePaint.textSize, dTitlePaint)

                // Bottom row in card: Main text + badge
                val dValY = cardRect.bottom - cInnerPad
                val isOff = dev.mainText == "ВЫКЛ" || dev.mainText == "ВЫКЛЮЧЕН" || dev.mainText == "НА БАЗЕ"
                val stateColor = when {
                    dev.isAlert -> palette.alertRed
                    isOff -> palette.textSecondary
                    dev.mainText == "ВКЛ" || dev.mainText == "ВКЛЮЧЕН" || dev.mainText == "УБОРКА" || dev.mainText == "ЗАКРЫТ" -> palette.okGreen
                    else -> palette.textPrimary
                }

                val dValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = stateColor
                    textSize = (12.5f * density).coerceIn(11f * density, 14f * density)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    if (palette.isGlassmorphism) setShadowLayer(2.5f * density, 0f, 1f * density, 0x85000000.toInt())
                }

                val badgeStr = dev.badge1 ?: dev.subText
                val maxValW = if (badgeStr != null) rightW * 0.50f else (rightW - cInnerPad * 2)
                val safeDVal = ellipsizeText(dev.mainText, dValPaint, maxValW)
                canvas.drawText(safeDVal, rightLeft + cInnerPad, dValY, dValPaint)

                if (badgeStr != null) {
                    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = if (dev.isAlert) palette.alertRed else palette.accentCold
                        textSize = 10.5f * density
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x80000000.toInt())
                    }
                    val availBadgeW = cardRect.right - cInnerPad - (rightLeft + cInnerPad + dValPaint.measureText(safeDVal) + 6f * density)
                    if (availBadgeW > 16f * density) {
                        val safeBadge = ellipsizeText(badgeStr, badgePaint, availBadgeW)
                        val bX = cardRect.right - cInnerPad - badgePaint.measureText(safeBadge)
                        canvas.drawText(safeBadge, bX, dValY, badgePaint)
                    }
                }
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

    fun renderDisabledWidgetBitmap(
        context: Context,
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val w = widthPx.coerceAtLeast(200)
        val h = heightPx.coerceAtLeast(100)
        val density = context.resources.displayMetrics.density
        val palette = getThemePalette(WidgetDataManager.THEME_XIAOMI)

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cornerRadius = palette.cornerRadiusDp * density
        drawSquircleCard(canvas, RectF(0f, 0f, w.toFloat(), h.toFloat()), cornerRadius, palette)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (13f * density).coerceIn(12f, 15f * density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF9DA3AE.toInt()
            textSize = (10f * density).coerceIn(9f, 12f * density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("Виджет отключен в приложении", w / 2f, h / 2f - 4f * density, textPaint)
        canvas.drawText("Нажмите для настройки или удалите с экрана", w / 2f, h / 2f + 16f * density, subPaint)

        return bitmap
    }
}
