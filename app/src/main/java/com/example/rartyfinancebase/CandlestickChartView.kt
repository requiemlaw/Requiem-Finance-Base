package com.example.rartyfinancebase

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CandlestickChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Candle(
        val open: Float, val high: Float, val low: Float, val close: Float, val volume: Float, val timestamp: Long
    )

    private var candles: List<Candle> = emptyList()
    private var dynamicMAs: Map<Int, List<Float?>> = emptyMap()
    private var rsiValues: List<Float?> = emptyList()
    private var macdData: IndicatorEngine.MacdResult? = null

    private var showRsi: Boolean = true
    private var showMacd: Boolean = false

    // ── Layout constants ──
    private val PADDING_LEFT   = 12f
    private val PADDING_RIGHT  = 60f
    private val PADDING_TOP    = 24f
    private val GAP            = 12f

    private var chartW = 0f
    private var mainH  = 0f
    private var volH   = 0f
    private var rsiH   = 0f
    private var macdH  = 0f

    private var mainTop  = 0f
    private var volTop   = 0f
    private var rsiTop   = 0f
    private var macdTop  = 0f

    // ── Scroll / Zoom ──
    private var candleWidth  = 14f
    private var scrollOffset = 0f
    private val MIN_CANDLE_W = 4f
    private val MAX_CANDLE_W = 40f

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            candleWidth = (candleWidth * detector.scaleFactor).coerceIn(MIN_CANDLE_W, MAX_CANDLE_W)
            invalidate()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            scrollOffset -= distanceX
            scrollOffset = scrollOffset.coerceIn(-(candles.size * candleWidth - chartW).coerceAtLeast(0f), 0f)
            invalidate()
            return true
        }
    })

    private var crosshairX = -1f
    private var crosshairCandle: Candle? = null

    // ── Paints ──
    private val paintBull   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#00D06C") }
    private val paintBear   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F23645") }
    private val paintWick   = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1.5f; style = Paint.Style.STROKE }
    private val paintGrid   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1E2327"); strokeWidth = 1f; style = Paint.Style.STROKE }
    private val paintLabel  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#848E9C"); textSize = 22f }
    private val paintCross  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; strokeWidth = 1f; style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f) }
    private val paintCrossBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2B3139") }

    // MACD ve RSI Boyaları
    private val paintRSI    = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#00E5FF"); strokeWidth = 2f; style = Paint.Style.STROKE }
    private val paintMacdLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2962FF"); strokeWidth = 2f; style = Paint.Style.STROKE }
    private val paintSignalLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF6D00"); strokeWidth = 2f; style = Paint.Style.STROKE }
    private val paintHistBull = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#00D06C"); style = Paint.Style.FILL }
    private val paintHistBear = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F23645"); style = Paint.Style.FILL }

    // Dinamik MA Renk Paleti (Sırasıyla: Sarı, Mor, Turuncu, Turkuaz, Pembe)
    private val maColors = arrayOf("#F0B90B", "#7B68EE", "#FF8C00", "#00FFFF", "#FF1493")

    fun setData(
        candles: List<Candle>,
        dynamicMAs: Map<Int, List<Float?>>,
        rsi: List<Float?>,
        macd: IndicatorEngine.MacdResult?,
        showRsi: Boolean,
        showMacd: Boolean
    ) {
        this.candles = candles
        this.dynamicMAs = dynamicMAs
        this.rsiValues = rsi
        this.macdData = macd
        this.showRsi = showRsi
        this.showMacd = showMacd

        // ── DİNAMİK YÜKSEKLİK MOTORU: Paneller Eklendikçe Ekranı Uzat! ──
        val displayMetrics = context.resources.displayMetrics
        var desiredHeightDp = 350f // Sadece Chart + Vol için baz yükseklik
        if (showRsi) desiredHeightDp += 110f // RSI varsa 110dp uzat
        if (showMacd) desiredHeightDp += 120f // MACD varsa 120dp uzat

        val desiredHeightPx = (desiredHeightDp * displayMetrics.density).toInt()

        // XML'deki sabit 340dp'yi ezip kendi istediğimiz yüksekliği veriyoruz
        if (layoutParams.height != desiredHeightPx) {
            layoutParams.height = desiredHeightPx
            requestLayout()
        }

        post {
            scrollOffset = -(candles.size * candleWidth - chartW).coerceAtLeast(0f)
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                crosshairX = event.x
                val idx = xToCandleIndex(crosshairX)
                crosshairCandle = if (idx in candles.indices) candles[idx] else null
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                crosshairX = -1f; crosshairCandle = null; invalidate()
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (candles.isEmpty()) return

        recalculateDimensions()
        val visibleRange = getVisibleRange()
        val (priceMin, priceMax) = getPriceRange(visibleRange)
        val volMax = getVolumeMax(visibleRange)

        drawGridLines(canvas, priceMin, priceMax)
        drawVolumePanel(canvas, visibleRange, volMax)
        drawCandlePanel(canvas, visibleRange, priceMin, priceMax)
        drawDynamicMAs(canvas, visibleRange, priceMin, priceMax)

        if (showRsi) drawRSIPanel(canvas, visibleRange)
        if (showMacd) drawMACDPanel(canvas, visibleRange)

        drawPanelLabels(canvas)
        if (crosshairX > 0) drawCrosshair(canvas, priceMin, priceMax)
    }

    private fun recalculateDimensions() {
        chartW = width - PADDING_LEFT - PADDING_RIGHT
        val totalH = height.toFloat() - PADDING_TOP - 8f

        // Dinamik Yükseklik Paylaşımı
        volH = totalH * 0.15f
        rsiH = if (showRsi) totalH * 0.20f else 0f
        macdH = if (showMacd) totalH * 0.20f else 0f

        // Kalan alanı ana grafiğe ver
        var usedGaps = 1
        if (showRsi) usedGaps++
        if (showMacd) usedGaps++

        mainH = totalH - volH - rsiH - macdH - (GAP * usedGaps)

        mainTop = PADDING_TOP
        volTop = mainTop + mainH + GAP

        if (showRsi) {
            rsiTop = volTop + volH + GAP
            if (showMacd) macdTop = rsiTop + rsiH + GAP
        } else if (showMacd) {
            macdTop = volTop + volH + GAP
        }
    }

    private fun getVisibleRange(): IntRange {
        val firstVisible = (-scrollOffset / candleWidth).toInt().coerceAtLeast(0)
        val count = (chartW / candleWidth).toInt() + 2
        return firstVisible..(firstVisible + count).coerceAtMost(candles.size - 1)
    }

    private fun getPriceRange(range: IntRange): Pair<Float, Float> {
        var lo = Float.MAX_VALUE; var hi = Float.MIN_VALUE
        for (i in range) {
            val c = candles[i]
            if (c.low < lo) lo = c.low
            if (c.high > hi) hi = c.high
        }
        val pad = (hi - lo) * 0.05f
        return Pair(lo - pad, hi + pad)
    }

    private fun getVolumeMax(range: IntRange): Float {
        var max = 0f
        for (i in range) if (candles[i].volume > max) max = candles[i].volume
        return max
    }

    private fun priceToY(price: Float, pMin: Float, pMax: Float): Float = mainTop + mainH - ((price - pMin) / (pMax - pMin)) * mainH
    private fun rsiToY(rsi: Float): Float = rsiTop + rsiH - (rsi / 100f) * rsiH
    private fun candleIndexToX(index: Int): Float = PADDING_LEFT + index * candleWidth + scrollOffset + candleWidth / 2f
    private fun xToCandleIndex(x: Float): Int = ((x - PADDING_LEFT - scrollOffset) / candleWidth).toInt()

    private fun drawGridLines(canvas: Canvas, pMin: Float, pMax: Float) {
        val step = (pMax - pMin) / 4f
        for (i in 0..4) {
            val price = pMin + step * i
            val y = priceToY(price, pMin, pMax)
            canvas.drawLine(PADDING_LEFT, y, width - PADDING_RIGHT, y, paintGrid)
            val label = if (price >= 1000) "%.0f".format(price) else "%.2f".format(price)
            canvas.drawText(label, width - PADDING_RIGHT + 4f, y + 6f, paintLabel)
        }
    }

    private fun drawCandlePanel(canvas: Canvas, range: IntRange, pMin: Float, pMax: Float) {
        val bodyW = (candleWidth * 0.65f).coerceAtLeast(2f)
        for (i in range) {
            val c = candles[i]
            val x = candleIndexToX(i)
            val isBull = c.close >= c.open
            val paint = if (isBull) paintBull else paintBear

            paintWick.color = paint.color
            canvas.drawLine(x, priceToY(c.high, pMin, pMax), x, priceToY(c.low, pMin, pMax), paintWick)

            val bodyTop = priceToY(max(c.open, c.close), pMin, pMax)
            val bodyBottom = priceToY(min(c.open, c.close), pMin, pMax).coerceAtLeast(bodyTop + 1f)
            canvas.drawRect(RectF(x - bodyW / 2f, bodyTop, x + bodyW / 2f, bodyBottom), paint)
        }
    }

    private fun drawDynamicMAs(canvas: Canvas, range: IntRange, pMin: Float, pMax: Float) {
        var colorIndex = 0
        val paintTemp = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 2f; style = Paint.Style.STROKE }

        for ((_, values) in dynamicMAs) {
            paintTemp.color = Color.parseColor(maColors[colorIndex % maColors.size])
            val path = Path()
            var firstPoint = true

            for (i in range) {
                if (i >= values.size) break
                values[i]?.let { v ->
                    val x = candleIndexToX(i)
                    val y = priceToY(v, pMin, pMax)
                    if (firstPoint) { path.moveTo(x, y); firstPoint = false } else path.lineTo(x, y)
                }
            }
            canvas.drawPath(path, paintTemp)
            colorIndex++
        }
    }

    private fun drawVolumePanel(canvas: Canvas, range: IntRange, volMax: Float) {
        if (volMax == 0f) return
        val bodyW = (candleWidth * 0.65f).coerceAtLeast(2f)
        for (i in range) {
            val c = candles[i]
            val x = candleIndexToX(i)
            val barH = (c.volume / volMax) * volH
            val volPaint = Paint(if (c.close >= c.open) paintBull else paintBear).apply { alpha = 120 }
            canvas.drawRect(RectF(x - bodyW / 2f, volTop + volH - barH, x + bodyW / 2f, volTop + volH), volPaint)
        }
    }

    private fun drawRSIPanel(canvas: Canvas, range: IntRange) {
        canvas.drawLine(PADDING_LEFT, rsiToY(70f), width - PADDING_RIGHT, rsiToY(70f), paintGrid)
        canvas.drawLine(PADDING_LEFT, rsiToY(30f), width - PADDING_RIGHT, rsiToY(30f), paintGrid)

        val path = Path()
        var firstPoint = true
        for (i in range) {
            if (i >= rsiValues.size) break
            rsiValues[i]?.let { rsi ->
                val x = candleIndexToX(i)
                if (firstPoint) { path.moveTo(x, rsiToY(rsi)); firstPoint = false } else path.lineTo(x, rsiToY(rsi))
            }
        }
        canvas.drawPath(path, paintRSI)
    }

    private fun drawMACDPanel(canvas: Canvas, range: IntRange) {
        val macd = macdData ?: return
        var maxVal = Float.MIN_VALUE
        var minVal = Float.MAX_VALUE

        // Min/Max Bulma
        for (i in range) {
            if (i >= macd.macdLine.size) break
            macd.macdLine[i]?.let { if (it > maxVal) maxVal = it; if (it < minVal) minVal = it }
            macd.histogram[i]?.let { if (it > maxVal) maxVal = it; if (it < minVal) minVal = it }
        }

        // Güvenlik Kilidi: Veri yetersizliği veya sıfıra bölünme hatasını engeller
        if (maxVal == Float.MIN_VALUE || maxVal == minVal) return

        val pad = (maxVal - minVal) * 0.1f
        maxVal += pad; minVal -= pad

        fun macdY(v: Float) = macdTop + macdH - ((v - minVal) / (maxVal - minVal)) * macdH
        val zeroY = macdY(0f)
        canvas.drawLine(PADDING_LEFT, zeroY, width - PADDING_RIGHT, zeroY, paintGrid)

        val pathMacd = Path(); val pathSignal = Path()
        var firstMacd = true; var firstSignal = true
        val bodyW = (candleWidth * 0.6f).coerceAtLeast(2f)

        for (i in range) {
            if (i >= macd.macdLine.size) break
            val x = candleIndexToX(i)

            // Histogram
            macd.histogram[i]?.let { h ->
                val y = macdY(h)
                val paint = if (h >= 0) paintHistBull else paintHistBear
                canvas.drawRect(RectF(x - bodyW/2f, min(zeroY, y), x + bodyW/2f, max(zeroY, y)), paint)
            }
            // Çizgiler
            macd.macdLine[i]?.let { v -> if (firstMacd) { pathMacd.moveTo(x, macdY(v)); firstMacd = false } else pathMacd.lineTo(x, macdY(v)) }
            macd.signalLine[i]?.let { v -> if (firstSignal) { pathSignal.moveTo(x, macdY(v)); firstSignal = false } else pathSignal.lineTo(x, macdY(v)) }
        }
        canvas.drawPath(pathSignal, paintSignalLine)
        canvas.drawPath(pathMacd, paintMacdLine)
    }

    private fun drawPanelLabels(canvas: Canvas) {
        var xOffset = PADDING_LEFT + 4f
        var colorIdx = 0
        for (period in dynamicMAs.keys) {
            paintLabel.color = Color.parseColor(maColors[colorIdx % maColors.size])
            val txt = "MA$period"
            canvas.drawText(txt, xOffset, mainTop + 18f, paintLabel)
            xOffset += paintLabel.measureText(txt) + 12f
            colorIdx++
        }
        paintLabel.color = Color.parseColor("#848E9C")
        if (showRsi) canvas.drawText("RSI 14", PADDING_LEFT + 4f, rsiTop + 20f, paintLabel)
        if (showMacd) canvas.drawText("MACD 12,26,9", PADDING_LEFT + 4f, macdTop + 20f, paintLabel)
    }

    private fun drawCrosshair(canvas: Canvas, pMin: Float, pMax: Float) {
        val x = crosshairX.coerceIn(PADDING_LEFT, width - PADDING_RIGHT)
        canvas.drawLine(x, PADDING_TOP, x, height.toFloat(), paintCross)
        crosshairCandle?.let { c ->
            val y = priceToY(c.close, pMin, pMax)
            canvas.drawLine(PADDING_LEFT, y, width - PADDING_RIGHT, y, paintCross)
            val tagRect = RectF(width - PADDING_RIGHT, y - 16f, width.toFloat(), y + 16f)
            canvas.drawRoundRect(tagRect, 4f, 4f, paintCrossBg)
            paintLabel.color = Color.WHITE
            canvas.drawText("%.2f".format(c.close), width - PADDING_RIGHT + 4f, y + 8f, paintLabel)
        }
    }
}