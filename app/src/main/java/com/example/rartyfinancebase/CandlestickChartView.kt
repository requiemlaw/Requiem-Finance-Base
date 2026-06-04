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
import kotlin.math.roundToInt

/**
 * CandlestickChartView
 *
 * Requiem Finance Terminal — Custom Canvas rendering engine for OHLCV data.
 * Renders candlestick bodies, wicks, volume bars, RSI panel, and MA overlays.
 *
 * Supports pinch-to-zoom and horizontal scroll via GestureDetector.
 *
 * Usage (XML):
 *   <com.example.rartyfinancebase.CandlestickChartView
 *       android:id="@+id/candlestickChart"
 *       android:layout_width="match_parent"
 *       android:layout_height="320dp" />
 *
 * Usage (Java/Kotlin):
 *   candlestickChart.setData(candles, ma20, ma50, rsiValues)
 */
class CandlestickChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // ── Data ────────────────────────────────────────────────────────────────
    data class Candle(
        val open: Float,
        val high: Float,
        val low: Float,
        val close: Float,
        val volume: Float,
        val timestamp: Long   // epoch seconds
    )

    private var candles: List<Candle> = emptyList()
    private var ma20: List<Float?> = emptyList()
    private var ma50: List<Float?> = emptyList()
    private var rsiValues: List<Float?> = emptyList()

    // ── Layout constants ────────────────────────────────────────────────────
    private val PADDING_LEFT   = 12f
    private val PADDING_RIGHT  = 60f   // space for Y-axis labels
    private val PADDING_TOP    = 24f
    private val VOLUME_RATIO   = 0.18f // volume panel takes 18% of chart height
    private val RSI_RATIO      = 0.22f // RSI panel takes 22% of chart height
    private val GAP            = 8f    // gap between panels

    // Derived at draw time
    private var chartW = 0f
    private var mainH  = 0f
    private var volH   = 0f
    private var rsiH   = 0f

    private var mainTop  = 0f
    private var volTop   = 0f
    private var rsiTop   = 0f

    // ── Scroll / Zoom ───────────────────────────────────────────────────────
    private var candleWidth  = 14f   // px per candle (body + gap)
    private var scrollOffset = 0f    // horizontal offset in px (positive = scrolled right)
    private val MIN_CANDLE_W = 4f
    private val MAX_CANDLE_W = 40f

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                candleWidth = (candleWidth * detector.scaleFactor)
                    .coerceIn(MIN_CANDLE_W, MAX_CANDLE_W)
                invalidate()
                return true
            }
        })

    private val gestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?, e2: MotionEvent,
                distanceX: Float, distanceY: Float
            ): Boolean {
                scrollOffset -= distanceX
                scrollOffset = scrollOffset.coerceIn(
                    -(candles.size * candleWidth - chartW).coerceAtLeast(0f), 0f
                )
                invalidate()
                return true
            }
        })

    // ── Crosshair ───────────────────────────────────────────────────────────
    private var crosshairX = -1f
    private var crosshairCandle: Candle? = null

    // ── Paints ──────────────────────────────────────────────────────────────
    private val paintBull   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#00D06C") }
    private val paintBear   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F23645") }
    private val paintWick   = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1.5f; style = Paint.Style.STROKE }
    private val paintMA20   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F0B90B"); strokeWidth = 1.5f; style = Paint.Style.STROKE
    }
    private val paintMA50   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7B68EE"); strokeWidth = 1.5f; style = Paint.Style.STROKE
    }
    private val paintRSI    = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00D06C"); strokeWidth = 1.5f; style = Paint.Style.STROKE
    }
    private val paintGrid   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E2327"); strokeWidth = 1f; style = Paint.Style.STROKE
    }
    private val paintLabel  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#848E9C"); textSize = 26f
    }
    private val paintCross  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF"); strokeWidth = 1f; style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
    }
    private val paintCrossLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 24f
    }
    private val paintCrossBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2B3139")
    }
    private val paintOB = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F2364540"); style = Paint.Style.FILL
    }
    private val paintOS = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00D06C30"); style = Paint.Style.FILL
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Feed chart data. Call this from your Activity/Fragment after API response.
     *
     * @param candles    OHLCV list, oldest first
     * @param ma20       20-period MA, same length as candles (nullable entries for warmup period)
     * @param ma50       50-period MA
     * @param rsi        14-period RSI
     */
    fun setData(
        candles: List<Candle>,
        ma20: List<Float?>,
        ma50: List<Float?>,
        rsi: List<Float?>
    ) {
        this.candles    = candles
        this.ma20       = ma20
        this.ma50       = ma50
        this.rsiValues  = rsi
        // Auto-scroll to latest
        post {
            scrollOffset = -(candles.size * candleWidth - chartW).coerceAtLeast(0f)
            invalidate()
        }
    }

    // ── Touch ───────────────────────────────────────────────────────────────
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
                crosshairX = -1f
                crosshairCandle = null
                invalidate()
            }
        }
        return true
    }

    // ── Draw ────────────────────────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (candles.isEmpty()) {
            drawEmptyState(canvas)
            return
        }
        recalculateDimensions()

        val visibleRange = getVisibleRange()
        val (priceMin, priceMax) = getPriceRange(visibleRange)
        val (volMax)             = getVolumeMax(visibleRange)
        val (rsiMin, rsiMax)     = Pair(0f, 100f)

        drawGridLines(canvas, priceMin, priceMax, volMax)
        drawVolumePanel(canvas, visibleRange, volMax)
        drawCandlePanel(canvas, visibleRange, priceMin, priceMax)
        drawMAOverlay(canvas, visibleRange, priceMin, priceMax)
        drawRSIPanel(canvas, visibleRange, rsiMin, rsiMax)
        drawPanelLabels(canvas)
        if (crosshairX > 0) drawCrosshair(canvas, priceMin, priceMax)
    }

    // ── Dimension helpers ───────────────────────────────────────────────────
    private fun recalculateDimensions() {
        chartW  = width  - PADDING_LEFT - PADDING_RIGHT
        val totalH = height.toFloat() - PADDING_TOP - 8f
        volH    = totalH * VOLUME_RATIO
        rsiH    = totalH * RSI_RATIO
        mainH   = totalH - volH - rsiH - GAP * 2

        mainTop = PADDING_TOP
        volTop  = mainTop + mainH + GAP
        rsiTop  = volTop  + volH  + GAP
    }

    private fun getVisibleRange(): IntRange {
        val firstVisible = (-scrollOffset / candleWidth).toInt().coerceAtLeast(0)
        val count        = (chartW / candleWidth).toInt() + 2
        val lastVisible  = (firstVisible + count).coerceAtMost(candles.size - 1)
        return firstVisible..lastVisible
    }

    private fun getPriceRange(range: IntRange): Pair<Float, Float> {
        var lo = Float.MAX_VALUE
        var hi = Float.MIN_VALUE
        for (i in range) {
            val c = candles[i]
            if (c.low  < lo) lo = c.low
            if (c.high > hi) hi = c.high
        }
        val pad = (hi - lo) * 0.06f
        return Pair(lo - pad, hi + pad)
    }

    private fun getVolumeMax(range: IntRange): Triple<Float, Unit, Unit> {
        var max = 0f
        for (i in range) if (candles[i].volume > max) max = candles[i].volume
        return Triple(max, Unit, Unit)
    }

    // ── Coordinate transforms ───────────────────────────────────────────────
    private fun priceToY(price: Float, pMin: Float, pMax: Float): Float {
        return mainTop + mainH - ((price - pMin) / (pMax - pMin)) * mainH
    }

    private fun rsiToY(rsi: Float): Float {
        return rsiTop + rsiH - (rsi / 100f) * rsiH
    }

    private fun candleIndexToX(index: Int): Float {
        return PADDING_LEFT + index * candleWidth + scrollOffset + candleWidth / 2f
    }

    private fun xToCandleIndex(x: Float): Int {
        return ((x - PADDING_LEFT - scrollOffset) / candleWidth).toInt()
    }

    // ── Draw calls ──────────────────────────────────────────────────────────
    private fun drawEmptyState(canvas: Canvas) {
        paintLabel.textAlign = Paint.Align.CENTER
        canvas.drawText("Veri yükleniyor...", width / 2f, height / 2f, paintLabel)
        paintLabel.textAlign = Paint.Align.LEFT
    }

    private fun drawGridLines(canvas: Canvas, pMin: Float, pMax: Float, volMax: Float) {
        // Horizontal price grid (4 lines)
        val step = (pMax - pMin) / 4f
        for (i in 0..4) {
            val price = pMin + step * i
            val y = priceToY(price, pMin, pMax)
            canvas.drawLine(PADDING_LEFT, y, width - PADDING_RIGHT, y, paintGrid)
            // Y-axis price label
            val label = if (price >= 1000) "%.0f".format(price)
                        else if (price >= 10) "%.2f".format(price)
                        else "%.4f".format(price)
            canvas.drawText(label, width - PADDING_RIGHT + 4f, y + 8f, paintLabel)
        }
        // RSI grid lines at 70 and 30
        val y70 = rsiToY(70f)
        val y30 = rsiToY(30f)
        canvas.drawRect(PADDING_LEFT, rsiTop, width - PADDING_RIGHT, y70, paintOB)
        canvas.drawRect(PADDING_LEFT, y30, width - PADDING_RIGHT, rsiTop + rsiH, paintOS)
        canvas.drawLine(PADDING_LEFT, y70, width - PADDING_RIGHT, y70, paintGrid)
        canvas.drawLine(PADDING_LEFT, y30, width - PADDING_RIGHT, y30, paintGrid)
        // RSI labels
        canvas.drawText("70", width - PADDING_RIGHT + 4f, y70 + 8f, paintLabel)
        canvas.drawText("30", width - PADDING_RIGHT + 4f, y30 + 8f, paintLabel)
    }

    private fun drawCandlePanel(canvas: Canvas, range: IntRange, pMin: Float, pMax: Float) {
        val bodyW = (candleWidth * 0.65f).coerceAtLeast(2f)

        for (i in range) {
            val c   = candles[i]
            val x   = candleIndexToX(i)
            val isBull = c.close >= c.open

            val bodyTop    = priceToY(max(c.open, c.close), pMin, pMax)
            val bodyBottom = priceToY(min(c.open, c.close), pMin, pMax)
            val wickTop    = priceToY(c.high, pMin, pMax)
            val wickBottom = priceToY(c.low,  pMin, pMax)

            val paint = if (isBull) paintBull else paintBear

            // Wick
            paintWick.color = paint.color
            canvas.drawLine(x, wickTop, x, wickBottom, paintWick)

            // Body
            val rect = RectF(x - bodyW / 2f, bodyTop, x + bodyW / 2f, bodyBottom.coerceAtLeast(bodyTop + 1f))
            canvas.drawRect(rect, paint)
        }
    }

    private fun drawMAOverlay(canvas: Canvas, range: IntRange, pMin: Float, pMax: Float) {
        val path20 = Path(); val path50 = Path()
        var first20 = true; var first50 = true

        for (i in range) {
            val x = candleIndexToX(i)

            if (i < ma20.size) {
                ma20[i]?.let { v ->
                    val y = priceToY(v, pMin, pMax)
                    if (first20) { path20.moveTo(x, y); first20 = false } else path20.lineTo(x, y)
                }
            }
            if (i < ma50.size) {
                ma50[i]?.let { v ->
                    val y = priceToY(v, pMin, pMax)
                    if (first50) { path50.moveTo(x, y); first50 = false } else path50.lineTo(x, y)
                }
            }
        }
        canvas.drawPath(path20, paintMA20)
        canvas.drawPath(path50, paintMA50)
    }

    private fun drawVolumePanel(canvas: Canvas, range: IntRange, volMax: Float) {
        if (volMax == 0f) return
        val bodyW = (candleWidth * 0.65f).coerceAtLeast(2f)

        for (i in range) {
            val c   = candles[i]
            val x   = candleIndexToX(i)
            val barH = (c.volume / volMax) * volH
            val paint = if (c.close >= c.open) paintBull else paintBear

            // 40% alpha clone
            val volPaint = Paint(paint).apply { alpha = 100 }
            val rect = RectF(
                x - bodyW / 2f,
                volTop + volH - barH,
                x + bodyW / 2f,
                volTop + volH
            )
            canvas.drawRect(rect, volPaint)
        }
    }

    private fun drawRSIPanel(canvas: Canvas, range: IntRange, rsiMin: Float, rsiMax: Float) {
        if (rsiValues.isEmpty()) return

        val path = Path()
        var firstPoint = true

        for (i in range) {
            if (i >= rsiValues.size) break
            rsiValues[i]?.let { rsi ->
                val x = candleIndexToX(i)
                val y = rsiToY(rsi)
                if (firstPoint) { path.moveTo(x, y); firstPoint = false } else path.lineTo(x, y)
            }
        }
        canvas.drawPath(path, paintRSI)

        // Current RSI value label
        val lastRsi = rsiValues.lastOrNull { it != null }
        lastRsi?.let {
            canvas.drawText("RSI %.1f".format(it), PADDING_LEFT + 4f, rsiTop + 22f, paintLabel)
        }
    }

    private fun drawPanelLabels(canvas: Canvas) {
        paintLabel.textSize = 22f
        canvas.drawText("VOL", PADDING_LEFT + 4f, volTop + 18f, paintLabel)
        paintLabel.textSize = 26f

        // MA Legend
        val ma20Paint = Paint(paintLabel).apply { color = paintMA20.color }
        val ma50Paint = Paint(paintLabel).apply { color = paintMA50.color }
        canvas.drawText("MA20", PADDING_LEFT + 4f, mainTop + 18f, ma20Paint)
        canvas.drawText("MA50", PADDING_LEFT + 60f, mainTop + 18f, ma50Paint)
    }

    private fun drawCrosshair(canvas: Canvas, pMin: Float, pMax: Float) {
        val x = crosshairX.coerceIn(PADDING_LEFT, width - PADDING_RIGHT)

        // Vertical line
        canvas.drawLine(x, PADDING_TOP, x, height.toFloat(), paintCross)

        crosshairCandle?.let { c ->
            // Horizontal line at close
            val y = priceToY(c.close, pMin, pMax)
            canvas.drawLine(PADDING_LEFT, y, width - PADDING_RIGHT, y, paintCross)

            // Price tag on right axis
            val label = if (c.close >= 1000) "%.2f".format(c.close) else "%.4f".format(c.close)
            val labelW = paintCrossLabel.measureText(label) + 10f
            val tagRect = RectF(width - PADDING_RIGHT, y - 14f, width.toFloat(), y + 14f)
            canvas.drawRoundRect(tagRect, 4f, 4f, paintCrossBg)
            canvas.drawText(label, width - PADDING_RIGHT + 5f, y + 8f, paintCrossLabel)

            // OHLCV tooltip at top
            val tooltipText = "O:%.4f  H:%.4f  L:%.4f  C:%.4f".format(c.open, c.high, c.low, c.close)
            val tw = paintLabel.measureText(tooltipText) + 16f
            val tx = (x - tw / 2f).coerceIn(PADDING_LEFT, width - PADDING_RIGHT - tw)
            canvas.drawRoundRect(RectF(tx, 2f, tx + tw, 30f), 4f, 4f, paintCrossBg)
            paintLabel.color = Color.WHITE
            canvas.drawText(tooltipText, tx + 8f, 22f, paintLabel)
            paintLabel.color = Color.parseColor("#848E9C")
        }
    }
}
