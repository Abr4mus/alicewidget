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
            cornerRadiusDp = 26f
        )
        WidgetDataManager.THEME_FLUENT_PASTEL -> ThemePalette(
            // Microsoft Fluent Design: gentle blush pastel-pink into dreamy azure blue gradient with frosted acrylic
            bgStart = 0xF04E2448.toInt(),       // Soft glowing pastel mauve-pink
            bgEnd = 0xF61C3660.toInt(),         // Deep dreamy azure blue
            borderTop = 0xB8FFFFFF.toInt(),     // Fluent Specular highlight
            borderBottom = 0x30FFFFFF.toInt(),
            tileBg = 0x22FFFFFF.toInt(),        // Translucent acrylic glass sub-tiles
            tileBorder = 0x48FFFFFF.toInt(),
            textPrimary = 0xFFFFFFFF.toInt(),   // Crisp Pure White
            textSecondary = 0xFFE0E7FF.toInt(), // Soft Sky Lavender
            accentComfort = 0xFF2ED573.toInt(), // Neon Seafoam
            accentWarm = 0xFFFF7597.toInt(),    // Soft Pastel Coral Pink
            accentCold = 0xFF70A1FF.toInt(),    // Dreamy Sky Azure
            alertRed = 0xFFFF4757.toInt(),
            okGreen = 0xFF2ED573.toInt(),
            isGlassmorphism = true,
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

        // Scale Markers at Start (15°) and End (30°)
        val startRad = Math.toRadians(135.0)
        val endRad = Math.toRadians(405.0)
        val scaleOffset = radius + strokeWidth * 1.5f
        val startX = (cx + scaleOffset * cos(startRad)).toFloat()
        val startY = (cy + scaleOffset * sin(startRad)).toFloat()
        val endX = (cx + scaleOffset * cos(endRad)).toFloat()
        val endY = (cy + scaleOffset * sin(endRad)).toFloat()

        val scalePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 255, 255, 255)
            textSize = (radius * 0.20f).coerceIn(7.5f * canvas.density, 10f * canvas.density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("15°", startX, startY + scalePaint.textSize * 0.3f, scalePaint)
        canvas.drawText("30°", endX, endY + scalePaint.textSize * 0.3f, scalePaint)

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
                textSize = (radius * 0.26f).coerceIn(10f, 13f * canvas.density)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("● $statusText", cx, cy + radius + statusPaint.textSize * 1.1f, statusPaint)
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
                setShadowLayer(2f * canvas.density, 0f, 1f * canvas.density, 0x80000000.toInt())
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

    // --- FULL-CARD WIDGET RENDERING IMPLEMENTATIONS ---

    // --- FULL-CARD WIDGET RENDERING IMPLEMENTATIONS ---

    /**
     * 2x1 Compact Pill:
     * Room / Device Name (top-left) + Large state/temperature (bottom-left) +
     * High-clarity Frosted Glass Pill (right) for metrics (e.g. ⛽ 65% • 🔋 12.6 В or 💧 48%).
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

        val padLeft = 14f * density
        val padTop = 10f * density
        val padRight = 12f * density
        val padBottom = 10f * density
        val actionHotspotWidth = 64f * density

        val st = data.state

        // 1. Title (Icon + Name)
        val titleText = "${st.iconEmoji} ${data.title}"
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textSecondary
            textSize = (11.5f * density).coerceIn(10f, 13f * density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x90000000.toInt())
        }
        val maxTitleW = w - padLeft - actionHotspotWidth
        val safeTitle = ellipsizeText(titleText, titlePaint, maxTitleW)
        val titleY = padTop + titlePaint.textSize
        canvas.drawText(safeTitle, padLeft, titleY, titlePaint)

        // 2. Main Text (Big bold state or temperature)
        val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (st.isAlert) palette.alertRed else palette.textPrimary
            textSize = (h * 0.40f).coerceIn(20f * density, 30f * density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            if (palette.isGlassmorphism) setShadowLayer(3.5f * density, 0f, 1.2f * density, 0x85000000.toInt())
        }
        val mainY = h - padBottom - 2f * density
        val safeMain = ellipsizeText(st.mainText, mainPaint, (w - padLeft - padRight) * 0.54f)
        canvas.drawText(safeMain, padLeft, mainY, mainPaint)
        val mainWidth = mainPaint.measureText(safeMain)

        // 3. Right Frosted Badge (Secondary metric)
        val badgeText = when {
            st.badge1 != null && st.badge2 != null -> "${st.badge1} • ${st.badge2}"
            st.badge1 != null -> st.badge1
            st.subText != null -> st.subText
            else -> null
        }

        if (badgeText != null) {
            val pillH = (h * 0.38f).coerceIn(22f * density, 28f * density)
            val pillMaxW = (w - padLeft - mainWidth - 14f * density).coerceAtLeast(50f * density)
            val pillRight = w - padRight
            val pillTop = h - padBottom - pillH
            val pillRect = RectF(pillRight - pillMaxW, pillTop, pillRight, pillTop + pillH)
            val badgeColor = if (st.isAlert) palette.alertRed else palette.accentCold
            drawGlassPill(context, canvas, pillRect, null, badgeText, badgeColor, palette)
        }

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

        // 1. Top Title (Icon + Name) - reserve 78dp on right so it NEVER touches action buttons!
        val maxTitleW = w - pad - 78f * density
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
            val availBottom = if (st.badge1 != null) (h - pad - 26f * density) else (h - pad - 6f * density)
            val cy = (availTop + availBottom) / 2f
            val gaugeRadius = min((w - pad * 2) / 2f - 4f * density, (availBottom - availTop) / 2f - 4f * density).coerceIn(24f * density, 48f * density)
            val strokeWidth = 4.5f * density
            drawComfortGaugeRing(canvas, cx, cy, gaugeRadius, strokeWidth, st.rawTemperature, palette, showStatusText = false)

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
            canvas.drawText("● $comfortText", cx, cy + gaugeRadius * 0.52f, statusPaint)

            if (st.badge1 != null) {
                val pillH = 22f * density
                val pillW = (w - pad * 2).coerceAtMost(80f * density)
                val pillRect = RectF(cx - pillW / 2f, h - pad - pillH, cx + pillW / 2f, h - pad)
                drawGlassPill(context, canvas, pillRect, null, st.badge1, palette.accentCold, palette)
            }
        } else if (st.category == "vehicle") {
            // --- VEHICLE MODE (2 Side-by-Side Square Icon Tiles + Bottom Pills) ---
            val hasBadges = st.badge1 != null || st.badge2 != null
            val bottomH = if (hasBadges) 24f * density else 0f
            val centerTileTop = pad + titleSize + 8f * density
            val centerTileBottom = h - pad - bottomH - (if (bottomH > 0) 8f * density else 0f)

            val tileGap = 8f * density
            val tileW = (w - pad * 2 - tileGap) / 2f

            // Left Tile: Security / Lock status
            val r1 = RectF(pad, centerTileTop, pad + tileW, centerTileBottom)
            drawSquircleCard(canvas, r1, 14f * density, palette, isSubTile = true)

            val secIcon = if (st.isAlert) "🔓" else "🔒"
            val secText = if (st.isAlert) "ОТКРЫТ" else "ЗАКРЫТ"
            val secColor = if (st.isAlert) palette.alertRed else palette.okGreen

            val secIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = (r1.height() * 0.26f).coerceIn(13f * density, 20f * density)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(secIcon, r1.centerX(), r1.top + r1.height() * 0.36f, secIconPaint)

            val secTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = secColor
                textSize = (r1.height() * 0.18f).coerceIn(10f * density, 13f * density)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x80000000.toInt())
            }
            canvas.drawText(secText, r1.centerX(), r1.top + r1.height() * 0.65f, secTextPaint)

            val secSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.textSecondary
                textSize = (9f * density).coerceIn(8f * density, 10f * density)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Охрана", r1.centerX(), r1.bottom - 6f * density, secSubPaint)

            // Right Tile: Engine state
            val r2 = RectF(pad + tileW + tileGap, centerTileTop, w - pad, centerTileBottom)
            drawSquircleCard(canvas, r2, 14f * density, palette, isSubTile = true)

            val isRunning = st.subText?.contains("Заведён") == true
            val engIcon = if (isRunning) "⚡" else "🛑"
            val engText = if (isRunning) "ЗАВЕДЁН" else "ЗАГЛУШЕН"
            val engColor = if (isRunning) palette.okGreen else palette.textSecondary

            canvas.drawText(engIcon, r2.centerX(), r2.top + r2.height() * 0.36f, secIconPaint)

            val engTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = engColor
                textSize = (r2.height() * 0.17f).coerceIn(9.5f * density, 12.5f * density)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x80000000.toInt())
            }
            canvas.drawText(engText, r2.centerX(), r2.top + r2.height() * 0.65f, engTextPaint)

            canvas.drawText("Двигатель", r2.centerX(), r2.bottom - 6f * density, secSubPaint)

            // Bottom Badges (Fuel & Battery)
            if (hasBadges) {
                val bottomY = h - pad - 22f * density
                val pillH = 22f * density
                val hasTwo = st.badge1 != null && st.badge2 != null
                if (hasTwo) {
                    val pW = (w - pad * 2 - 8f * density) / 2f
                    val b1Rect = RectF(pad, bottomY, pad + pW, bottomY + pillH)
                    val b2Rect = RectF(pad + pW + 8f * density, bottomY, w - pad, bottomY + pillH)
                    drawGlassPill(context, canvas, b1Rect, null, st.badge1!!, palette.accentComfort, palette)
                    drawGlassPill(context, canvas, b2Rect, null, st.badge2!!, palette.accentCold, palette)
                } else if (st.badge1 != null) {
                    val pW = (w - pad * 2).coerceAtMost(96f * density)
                    val bRect = RectF(cx - pW / 2f, bottomY, cx + pW / 2f, bottomY + pillH)
                    drawGlassPill(context, canvas, bRect, null, st.badge1, palette.accentComfort, palette)
                }
            }
        } else {
            // --- DEVICE SPECIFIC MODE (Kettle, TV, Switch, Vacuum, AC, Leak, Door) - NO ARC! ---
            val hasBadges = st.badge1 != null || st.badge2 != null
            val bottomH = if (hasBadges) 24f * density else 0f
            val centerTileTop = pad + titleSize + 8f * density
            val centerTileBottom = h - pad - bottomH - (if (bottomH > 0) 8f * density else 0f)
            val centerTileRect = RectF(pad, centerTileTop, w - pad, centerTileBottom)
            drawSquircleCard(canvas, centerTileRect, 14f * density, palette, isSubTile = true)

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

            // Bottom Badges
            if (hasBadges) {
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
        val w = widthPx.coerceAtLeast(320)
        val h = heightPx.coerceAtLeast(90)
        val density = context.resources.displayMetrics.density
        val palette = getThemePalette(theme)

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cornerRadius = palette.cornerRadiusDp * density
        drawSquircleCard(canvas, RectF(0f, 0f, w.toFloat(), h.toFloat()), cornerRadius, palette)

        val actionHotspotWidth = 64f * density
        val padLeft = 12f * density
        val padTop = 8f * density
        val padBottom = 8f * density
        val availWidth = w - padLeft - actionHotspotWidth

        val slotCount = slots.size.coerceIn(1, 2)
        val gap = 8f * density
        val slotW = (availWidth - gap * (slotCount - 1)) / slotCount

        for (i in 0 until slotCount) {
            val slot = slots[i]
            val st = slot.state
            val slotLeft = padLeft + i * (slotW + gap)
            val slotRect = RectF(slotLeft, padTop, slotLeft + slotW, h - padBottom)

            drawSquircleCard(canvas, slotRect, 14f * density, palette, isSubTile = true)

            val innerPad = 8f * density

            // Top: Icon + Room / Device Name
            val roomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.textSecondary
                textSize = (10.5f * density).coerceIn(9f * density, 12f * density)
                if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x90000000.toInt())
            }
            val titleStr = "${st.iconEmoji} ${slot.title}"
            val roomName = ellipsizeText(titleStr, roomPaint, slotW - innerPad * 2)
            canvas.drawText(roomName, slotLeft + innerPad, padTop + innerPad + roomPaint.textSize, roomPaint)

            // Bottom Metrics Row: Main text + Badge cleanly bounded!
            val contentY = slotRect.bottom - innerPad - 2f * density
            val isOff = st.mainText == "ВЫКЛ" || st.mainText == "ВЫКЛЮЧЕН" || st.mainText == "НА БАЗЕ"
            val stateColor = when {
                st.isAlert -> palette.alertRed
                isOff -> palette.textSecondary
                st.mainText == "ВКЛ" || st.mainText == "ВКЛЮЧЕН" || st.mainText == "УБОРКА" || st.mainText == "ЗАКРЫТ" -> palette.okGreen
                else -> palette.textPrimary
            }

            val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = stateColor
                textSize = (15f * density).coerceIn(13f * density, 17f * density)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                if (palette.isGlassmorphism) setShadowLayer(3.5f * density, 0f, 1.2f * density, 0x85000000.toInt())
            }

            val badgeText = st.badge1 ?: st.subText
            val maxMainW = if (badgeText != null) (slotW - innerPad * 2) * 0.52f else (slotW - innerPad * 2)
            val safeMain = ellipsizeText(st.mainText, mainPaint, maxMainW)
            canvas.drawText(safeMain, slotLeft + innerPad, contentY, mainPaint)

            if (badgeText != null) {
                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (st.isAlert) palette.alertRed else palette.accentCold
                    textSize = 11f * density
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x80000000.toInt())
                }
                val availBadgeW = slotRect.right - innerPad - (slotLeft + innerPad + mainPaint.measureText(safeMain) + 6f * density)
                if (availBadgeW > 20f * density) {
                    val safeBadge = ellipsizeText(badgeText, badgePaint, availBadgeW)
                    val badgeX = slotRect.right - innerPad - badgePaint.measureText(safeBadge)
                    canvas.drawText(safeBadge, badgeX, contentY, badgePaint)
                }
            }
        }

        return bitmap
    }

    /**
     * 4x2 Flagship Smart Home Hub Dashboard:
     * Header with isolated Room / Device title + Left Primary Device Compartment (tailored
     * for vehicle, kettle, tv, switch, climate with zero arc on non-climate!) +
     * Right Secondary Device Pods (up to 2 devices) with zero text layering!
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

        // 1. Header (Left: Primary Title + Subtitle) - Generous vertical spacing!
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textPrimary
            textSize = 14f * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            if (palette.isGlassmorphism) setShadowLayer(3f * density, 0f, 1.2f * density, 0x90000000.toInt())
        }
        val headerTitle = ellipsizeText("${primary.iconEmoji} ${data.primaryRoomName}", titlePaint, w - padLeft - 76f * density)
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

        val hasSecondaries = data.secondaryDevices.isNotEmpty()
        val leftCardW = if (hasSecondaries) (w - padLeft - padRight - 10f * density) * 0.50f else (w - padLeft - padRight)

        // 2. Left Primary Compartment Card (Device 1)
        val leftCardRect = RectF(padLeft, bodyTop, padLeft + leftCardW, bodyBottom)
        drawSquircleCard(canvas, leftCardRect, 18f * density, palette, isSubTile = true)

        val innerPad = 10f * density

        if (primary.hasComfortArc) {
            // Climate Display with Arc
            val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.textPrimary
                textSize = (leftCardRect.height() * 0.35f).coerceIn(20f * density, 30f * density)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                if (palette.isGlassmorphism) setShadowLayer(4f * density, 0f, 1.5f * density, 0x85000000.toInt())
            }
            val tempY = leftCardRect.top + innerPad + tempPaint.textSize
            canvas.drawText(primary.mainText, leftCardRect.left + innerPad, tempY, tempPaint)

            val gaugeRadius = (leftCardRect.height() * 0.22f).coerceIn(14f * density, 22f * density)
            val gaugeCx = leftCardRect.right - innerPad - gaugeRadius
            val gaugeCy = leftCardRect.top + innerPad + gaugeRadius
            drawComfortGaugeRing(canvas, gaugeCx, gaugeCy, gaugeRadius, 4.5f * density, primary.rawTemperature, palette, showStatusText = false)

            val (comfortColor, comfortText) = getComfortInfo(primary.rawTemperature, palette)
            val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = comfortColor
                textSize = 9.5f * density
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                if (palette.isGlassmorphism) setShadowLayer(2.5f * density, 0f, 1f * density, 0x80000000.toInt())
            }
            canvas.drawText(comfortText, leftCardRect.left + innerPad, tempY + statusPaint.textSize + 4f * density, statusPaint)

            var pillX = leftCardRect.left + innerPad
            val pillH = 22f * density
            val pillY = leftCardRect.bottom - innerPad - pillH

            if (primary.badge1 != null) {
                val pillW = 56f * density
                val humRect = RectF(pillX, pillY, pillX + pillW, pillY + pillH)
                drawGlassPill(context, canvas, humRect, null, primary.badge1, palette.accentCold, palette)
                pillX += pillW + 6f * density
            }
            if (primary.badge2 != null && (pillX + 54f * density) <= leftCardRect.right - innerPad) {
                val pillW = 56f * density
                val pressRect = RectF(pillX, pillY, pillX + pillW, pillY + pillH)
                drawGlassPill(context, canvas, pressRect, null, primary.badge2, palette.textSecondary, palette)
            }
        } else if (primary.category == "vehicle") {
            // Vehicle Primary Card: Large bold lock status + Engine text underneath + Fuel/Battery pills
            val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (primary.isAlert) palette.alertRed else palette.textPrimary
                textSize = (leftCardRect.height() * 0.22f).coerceIn(16f * density, 22f * density)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                if (palette.isGlassmorphism) setShadowLayer(3.5f * density, 0f, 1.2f * density, 0x85000000.toInt())
            }
            val secY = leftCardRect.top + innerPad + secPaint.textSize
            canvas.drawText(primary.mainText, leftCardRect.left + innerPad, secY, secPaint)

            val engPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                val isRunning = primary.subText?.contains("Заведён") == true
                color = if (isRunning) palette.okGreen else palette.textSecondary
                textSize = (11f * density).coerceIn(10f * density, 13f * density)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x80000000.toInt())
            }
            canvas.drawText(primary.subText ?: "", leftCardRect.left + innerPad, secY + engPaint.textSize + 6f * density, engPaint)

            var pillX = leftCardRect.left + innerPad
            val pillH = 22f * density
            val pillY = leftCardRect.bottom - innerPad - pillH

            if (primary.badge1 != null) {
                val pillW = (leftCardRect.width() * 0.44f).coerceAtMost(70f * density)
                val r1 = RectF(pillX, pillY, pillX + pillW, pillY + pillH)
                drawGlassPill(context, canvas, r1, null, primary.badge1, palette.accentComfort, palette)
                pillX += pillW + 6f * density
            }
            if (primary.badge2 != null && (pillX + 44f * density) <= leftCardRect.right - innerPad) {
                val pillW = (leftCardRect.width() * 0.44f).coerceAtMost(70f * density)
                val r2 = RectF(pillX, pillY, pillX + pillW, pillY + pillH)
                drawGlassPill(context, canvas, r2, null, primary.badge2, palette.accentCold, palette)
            }
        } else {
            // Specialized Display (Kettle, TV, Switch, Vacuum, AC, Leak, Door) - NO ARC!
            val isOff = primary.mainText == "ВЫКЛ" || primary.mainText == "ВЫКЛЮЧЕН" || primary.mainText == "НА БАЗЕ"
            val stateColor = when {
                primary.isAlert -> palette.alertRed
                isOff -> palette.textSecondary
                primary.mainText == "ВКЛ" || primary.mainText == "ВКЛЮЧЕН" || primary.mainText == "УБОРКА" -> palette.okGreen
                else -> palette.textPrimary
            }

            val statePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = stateColor
                textSize = (leftCardRect.height() * 0.26f).coerceIn(16f * density, 24f * density)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                if (palette.isGlassmorphism) setShadowLayer(3.5f * density, 0f, 1.2f * density, 0x85000000.toInt())
            }
            val stateY = leftCardRect.top + innerPad + statePaint.textSize
            val safeState = ellipsizeText(primary.mainText, statePaint, leftCardRect.width() - innerPad * 2)
            canvas.drawText(safeState, leftCardRect.left + innerPad, stateY, statePaint)

            if (primary.subText != null) {
                val subLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (primary.isAlert) palette.alertRed else palette.textSecondary
                    textSize = 10f * density
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x80000000.toInt())
                }
                val safeSub = ellipsizeText(primary.subText, subLabelPaint, leftCardRect.width() - innerPad * 2)
                canvas.drawText(safeSub, leftCardRect.left + innerPad, stateY + subLabelPaint.textSize + 5f * density, subLabelPaint)
            }

            var pillX = leftCardRect.left + innerPad
            val pillH = 22f * density
            val pillY = leftCardRect.bottom - innerPad - pillH

            if (primary.badge1 != null) {
                val pillW = (leftCardRect.width() * 0.44f).coerceAtMost(70f * density)
                val r1 = RectF(pillX, pillY, pillX + pillW, pillY + pillH)
                drawGlassPill(context, canvas, r1, null, primary.badge1, palette.accentComfort, palette)
                pillX += pillW + 6f * density
            }
            if (primary.badge2 != null && (pillX + 44f * density) <= leftCardRect.right - innerPad) {
                val pillW = (leftCardRect.width() * 0.44f).coerceAtMost(70f * density)
                val r2 = RectF(pillX, pillY, pillX + pillW, pillY + pillH)
                drawGlassPill(context, canvas, r2, null, primary.badge2, palette.accentCold, palette)
            }
        }

        // 3. Right Area: Secondary Devices
        if (hasSecondaries) {
            val rightLeft = leftCardRect.right + 10f * density
            val rightW = w - padRight - rightLeft
            val secCount = data.secondaryDevices.size.coerceAtMost(2)

            if (secCount == 1) {
                // Exactly 2 devices total! Right card is an equal full square matching the left compartment!
                val dev = data.secondaryDevices[0]
                val cardRect = RectF(rightLeft, bodyTop, rightLeft + rightW, bodyBottom)
                drawSquircleCard(canvas, cardRect, 18f * density, palette, isSubTile = true)

                val cInnerPad = 10f * density
                val dTitle = "${dev.iconEmoji} ${dev.name}"
                val dTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette.textSecondary
                    textSize = (11.5f * density).coerceIn(10f * density, 13f * density)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x90000000.toInt())
                }
                val safeDTitle = ellipsizeText(dTitle, dTitlePaint, rightW - cInnerPad * 2)
                canvas.drawText(safeDTitle, rightLeft + cInnerPad, bodyTop + cInnerPad + dTitlePaint.textSize, dTitlePaint)

                if (dev.hasComfortArc) {
                    // Climate in secondary square: Centered temperature and comfort label
                    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = palette.textPrimary
                        textSize = (cardRect.height() * 0.32f).coerceIn(18f * density, 28f * density)
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                        if (palette.isGlassmorphism) setShadowLayer(3.5f * density, 0f, 1.2f * density, 0x85000000.toInt())
                    }
                    val metrics = tempPaint.fontMetrics
                    val centerY = cardRect.centerY() - 6f * density - (metrics.ascent + metrics.descent) / 2f
                    canvas.drawText(dev.mainText, cardRect.centerX(), centerY, tempPaint)

                    val (comfortColor, comfortText) = getComfortInfo(dev.rawTemperature, palette)
                    val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = comfortColor
                        textSize = 9.5f * density
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                        if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x80000000.toInt())
                    }
                    canvas.drawText("● $comfortText", cardRect.centerX(), centerY + statusPaint.textSize + 6f * density, statusPaint)

                    if (dev.badge1 != null) {
                        val pillH = 22f * density
                        val pillW = (rightW - cInnerPad * 2).coerceAtMost(80f * density)
                        val pillRect = RectF(cardRect.centerX() - pillW / 2f, cardRect.bottom - cInnerPad - pillH, cardRect.centerX() + pillW / 2f, cardRect.bottom - cInnerPad)
                        drawGlassPill(context, canvas, pillRect, null, dev.badge1, palette.accentCold, palette)
                    }
                } else {
                    // Device in secondary square (Switch, Kettle, TV, Vacuum, AC, Vehicle) - Large centered status!
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
                    val centerY = cardRect.centerY() - (if (dev.subText != null) 6f * density else 0f) - (metrics.ascent + metrics.descent) / 2f
                    val safeState = ellipsizeText(dev.mainText, statePaint, rightW - cInnerPad * 2)
                    canvas.drawText(safeState, cardRect.centerX(), centerY, statePaint)

                    if (dev.subText != null) {
                        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = if (dev.isAlert) palette.alertRed else palette.textSecondary
                            textSize = 10.5f * density
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            textAlign = Paint.Align.CENTER
                            if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x80000000.toInt())
                        }
                        val safeSub = ellipsizeText(dev.subText, subPaint, rightW - cInnerPad * 2)
                        canvas.drawText(safeSub, cardRect.centerX(), centerY + subPaint.textSize + 8f * density, subPaint)
                    }

                    var pillX = cardRect.left + cInnerPad
                    val pillH = 22f * density
                    val pillY = cardRect.bottom - cInnerPad - pillH

                    if (dev.badge1 != null) {
                        val pillW = (rightW * 0.44f).coerceAtMost(70f * density)
                        val r1 = RectF(pillX, pillY, pillX + pillW, pillY + pillH)
                        drawGlassPill(context, canvas, r1, null, dev.badge1, palette.accentComfort, palette)
                        pillX += pillW + 6f * density
                    }
                    if (dev.badge2 != null && (pillX + 44f * density) <= cardRect.right - cInnerPad) {
                        val pillW = (rightW * 0.44f).coerceAtMost(70f * density)
                        val r2 = RectF(pillX, pillY, pillX + pillW, pillY + pillH)
                        drawGlassPill(context, canvas, r2, null, dev.badge2, palette.accentCold, palette)
                    }
                }
            } else {
                // Exactly 3 devices total: Two vertically stacked rectangular cards on right
                val secGap = 6f * density
                val totalAvailH = bodyBottom - bodyTop
                val cardH = (totalAvailH - secGap) / 2f

                for (i in 0 until 2) {
                    val dev = data.secondaryDevices[i]
                    val cardTop = bodyTop + i * (cardH + secGap)
                    val cardRect = RectF(rightLeft, cardTop, rightLeft + rightW, cardTop + cardH)

                    drawSquircleCard(canvas, cardRect, 12f * density, palette, isSubTile = true)

                    val cInnerPad = 8f * density
                    val dTitle = "${dev.iconEmoji} ${dev.name}"
                    val dTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = palette.textSecondary
                        textSize = (10f * density).coerceIn(8.5f * density, 11f * density)
                        if (palette.isGlassmorphism) setShadowLayer(2f * density, 0f, 1f * density, 0x90000000.toInt())
                    }
                    val safeDTitle = ellipsizeText(dTitle, dTitlePaint, rightW - cInnerPad * 2)
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
