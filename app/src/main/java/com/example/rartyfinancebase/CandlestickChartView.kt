package com.example.rartyfinancebase

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

class CandlestickChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Candle(
        val open: Float, val high: Float, val low: Float, val close: Float, val volume: Float, val timestamp: Long
    )

    private var candles: List<Candle> = emptyList()
    private var dynamicMAs: Map<Int, List<Float?>>? = null
    private var rsi: List<Float?>? = null
    private var macdData: IndicatorEngine.MacdResult? = null
    private var bbands: Triple<List<Float?>, List<Float?>, List<Float?>>? = null

    private var isRsiEnabled = true
    private var isMacdEnabled = false
    private var isMaEnabled = true
    private var isBollingerEnabled = false
    private var isVolumeEnabled = true

    private var isFirstDraw = true

    private val PADDING_LEFT = 12f
    private val PADDING_RIGHT = 70f
    private val PADDING_TOP = 24f
    private val GAP = 15f

    private var chartW = 0f
    private var mainH = 0f
    private var volH = 0f
    private var rsiH = 0f
    private var macdH = 0f

    private var mainTop = 0f
    private var volTop = 0f
    private var rsiTop = 0f
    private var macdTop = 0f

    private var candleWidth = 14f
    private var scrollOffset = 0f
    private var crosshairX = -1f

    private val paintBull = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#00D06C") }
    private val paintBear = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F23645") }
    private val paintWick = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1.5f; style = Paint.Style.STROKE }
    private val paintGrid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1E2327"); strokeWidth = 1f; style = Paint.Style.STROKE }
    private val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#848E9C"); textSize = 22f }

    private val paintBollingerFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F0B90B"); style = Paint.Style.FILL; alpha = 20 }
    private val paintBollingerLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F0B90B"); strokeWidth = 1.5f; style = Paint.Style.STROKE; alpha = 180 }

    private val paintRSI = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#00E5FF"); strokeWidth = 2f; style = Paint.Style.STROKE }
    private val paintMacdLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2962FF"); strokeWidth = 2f; style = Paint.Style.STROKE }
    private val paintSignalLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF6D00"); strokeWidth = 2f; style = Paint.Style.STROKE }
    private val paintHistBull = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#00D06C"); style = Paint.Style.FILL }
    private val paintHistBear = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F23645"); style = Paint.Style.FILL }

    private val maColors = arrayOf("#F0B90B", "#7B68EE", "#FF8C00", "#00FFFF", "#FF1493")

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(d: ScaleGestureDetector): Boolean {
            candleWidth = (candleWidth * d.scaleFactor).coerceIn(4f, 50f)
            invalidate(); return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            scrollOffset -= dx
            val maxScroll = -(candles.size * candleWidth - chartW).coerceAtLeast(0f)
            scrollOffset = scrollOffset.coerceIn(maxScroll, 0f)
            invalidate(); return true
        }
    })

    fun setData(
        candles: List<Candle>,
        mas: Map<Int, List<Float?>>?,
        rsi: List<Float?>?,
        macd: IndicatorEngine.MacdResult?,
        isRsi: Boolean, isMacd: Boolean, isMa: Boolean, isBol: Boolean,
        bbands: Triple<List<Float?>, List<Float?>, List<Float?>>?,
        isVol: Boolean = true
    ) {
        this.candles = candles
        this.dynamicMAs = mas
        this.rsi = rsi
        this.macdData = macd
        this.bbands = bbands
        this.isRsiEnabled = isRsi
        this.isMacdEnabled = isMacd
        this.isMaEnabled = isMa
        this.isBollingerEnabled = isBol
        this.isVolumeEnabled = isVol


        this.isFirstDraw = true

        post {
            val h = (if(isRsi) 460f else 350f) + (if(isMacd) 120f else 0f)
            layoutParams.height = (h * context.resources.displayMetrics.density).toInt()
            requestLayout()
            invalidate()
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(e)
        gestureDetector.onTouchEvent(e)
        if (e.action == MotionEvent.ACTION_MOVE || e.action == MotionEvent.ACTION_DOWN) {
            crosshairX = e.x; invalidate()
        } else {
            crosshairX = -1f; invalidate()
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (candles.isEmpty()) return

        recalculateDimensions()


        if (isFirstDraw) {
            val maxScroll = -(candles.size * candleWidth - chartW).coerceAtLeast(0f)
            scrollOffset = maxScroll
            isFirstDraw = false
        }

        val range = getVisibleRange()
        val (pMin, pMax) = getPriceRange(range)

        drawGrid(canvas, pMin, pMax)
        drawCandles(canvas, range, pMin, pMax)

        if (isVolumeEnabled) drawVolume(canvas, range)
        if (isBollingerEnabled) drawBollinger(canvas, range, pMin, pMax)
        if (isMaEnabled) drawMAs(canvas, range, pMin, pMax)
        if (isRsiEnabled) drawRSI(canvas, range)
        if (isMacdEnabled) drawMACD(canvas, range)

        drawPanelLabels(canvas)
    }

    private fun recalculateDimensions() {
        chartW = width - PADDING_LEFT - PADDING_RIGHT
        val h = height.toFloat() - PADDING_TOP
        volH = if (isVolumeEnabled) h * 0.12f else 0f
        rsiH = if (isRsiEnabled) h * 0.18f else 0f
        macdH = if (isMacdEnabled) h * 0.18f else 0f
        mainH = h - volH - rsiH - macdH - (GAP * 3)
        mainTop = PADDING_TOP
        volTop = mainTop + mainH + GAP
        rsiTop = volTop + volH + GAP
        macdTop = rsiTop + rsiH + GAP
    }

    private fun getVisibleRange(): IntRange {
        val start = (-scrollOffset / candleWidth).toInt().coerceAtLeast(0)
        val end = (start + (chartW / candleWidth).toInt() + 2).coerceAtMost(candles.size - 1)
        return start..end
    }

    private fun getPriceRange(range: IntRange): Pair<Float, Float> {
        val visible = candles.slice(range)
        val min = visible.minOf { it.low }
        val max = visible.maxOf { it.high }
        val pad = (max - min) * 0.1f
        return Pair(min - pad, max + pad)
    }

    private fun priceToY(p: Float, min: Float, max: Float) = mainTop + mainH - ((p - min) / (max - min)) * mainH
    private fun indexToX(i: Int) = PADDING_LEFT + i * candleWidth + scrollOffset + candleWidth / 2f

    private fun drawGrid(canvas: Canvas, min: Float, max: Float) {
        for (i in 0..4) {
            val p = min + (max - min) * i / 4f
            val y = priceToY(p, min, max)
            canvas.drawLine(PADDING_LEFT, y, width - PADDING_RIGHT, y, paintGrid)
            canvas.drawText("%.2f".format(p), width - PADDING_RIGHT + 5, y + 8, paintLabel)
        }
    }

    private fun drawPanelLabels(canvas: Canvas) {
        var xOffset = PADDING_LEFT + 4f
        var colorIdx = 0

        if (isMaEnabled) {
            dynamicMAs?.keys?.forEach { period ->
                paintLabel.color = Color.parseColor(maColors[colorIdx % maColors.size])
                val txt = "MA$period"
                canvas.drawText(txt, xOffset, mainTop + 18f, paintLabel)
                xOffset += paintLabel.measureText(txt) + 12f
                colorIdx++
            }
        }

        paintLabel.color = Color.parseColor("#848E9C")
        if (isBollingerEnabled) {
            canvas.drawText("BB (20,2)", xOffset, mainTop + 18f, paintLabel)
        }

        if (isRsiEnabled) canvas.drawText("RSI 14", PADDING_LEFT + 4f, rsiTop + 20f, paintLabel)
        if (isMacdEnabled) canvas.drawText("MACD 12,26,9", PADDING_LEFT + 4f, macdTop + 20f, paintLabel)
    }

    private fun drawCandles(canvas: Canvas, range: IntRange, min: Float, max: Float) {
        val w = candleWidth * 0.8f
        for (i in range) {
            val c = candles[i]; val x = indexToX(i)
            val paint = if (c.close >= c.open) paintBull else paintBear
            paintWick.color = paint.color
            canvas.drawLine(x, priceToY(c.high, min, max), x, priceToY(c.low, min, max), paintWick)
            canvas.drawRect(x - w/2, priceToY(kotlin.math.max(c.open, c.close), min, max), x + w/2, priceToY(kotlin.math.min(c.open, c.close), min, max), paint)
        }
    }

    private fun drawVolume(canvas: Canvas, range: IntRange) {
        if (volH == 0f) return
        val visibleCandles = candles.slice(range)
        val volMax = visibleCandles.maxOfOrNull { it.volume } ?: return
        if (volMax == 0f) return

        val w = candleWidth * 0.8f
        for (i in range) {
            val c = candles[i]; val x = indexToX(i)
            val paint = Paint(if (c.close >= c.open) paintBull else paintBear).apply { alpha = 120 }
            val barH = (c.volume / volMax) * volH
            canvas.drawRect(x - w/2, volTop + volH - barH, x + w/2, volTop + volH, paint)
        }
    }

    private fun drawMAs(canvas: Canvas, range: IntRange, min: Float, max: Float) {
        var colorIndex = 0
        dynamicMAs?.forEach { (_, values) ->
            val path = Path(); var first = true
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(maColors[colorIndex % maColors.size]); strokeWidth = 2f; style = Paint.Style.STROKE }
            for (i in range) {
                if (i < values.size && values[i] != null) {
                    val x = indexToX(i); val y = priceToY(values[i]!!, min, max)
                    if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
                }
            }
            canvas.drawPath(path, p)
            colorIndex++
        }
    }

    private fun drawBollinger(canvas: Canvas, range: IntRange, min: Float, max: Float) {
        val (u, _, l) = bbands ?: return
        val pU = Path(); val pL = Path(); val pFill = Path()
        var first = true
        val validIndices = mutableListOf<Int>()

        for (i in range) {
            if (i < u.size && u[i] != null && l[i] != null) {
                validIndices.add(i)
                val x = indexToX(i)
                val yU = priceToY(u[i]!!, min, max)
                val yL = priceToY(l[i]!!, min, max)

                if (first) {
                    pU.moveTo(x, yU)
                    pL.moveTo(x, yL)
                    first = false
                } else {
                    pU.lineTo(x, yU)
                    pL.lineTo(x, yL)
                }
            }
        }

        if (validIndices.isNotEmpty()) {
            first = true
            for (i in validIndices) {
                val x = indexToX(i)
                if (first) { pFill.moveTo(x, priceToY(u[i]!!, min, max)); first = false }
                else pFill.lineTo(x, priceToY(u[i]!!, min, max))
            }
            for (i in validIndices.reversed()) {
                pFill.lineTo(indexToX(i), priceToY(l[i]!!, min, max))
            }
            pFill.close()

            canvas.drawPath(pFill, paintBollingerFill)
            canvas.drawPath(pU, paintBollingerLine)
            canvas.drawPath(pL, paintBollingerLine)
        }
    }

    private fun drawRSI(canvas: Canvas, range: IntRange) {
        val r = rsi ?: return
        val path = Path(); var first = true
        canvas.drawLine(PADDING_LEFT, rsiTop + rsiH * 0.3f, width - PADDING_RIGHT, rsiTop + rsiH * 0.3f, paintGrid)
        canvas.drawLine(PADDING_LEFT, rsiTop + rsiH * 0.7f, width - PADDING_RIGHT, rsiTop + rsiH * 0.7f, paintGrid)
        for (i in range) {
            if (i < r.size && r[i] != null) {
                val x = indexToX(i); val y = rsiTop + rsiH - (r[i]!! / 100f) * rsiH
                if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
            }
        }
        canvas.drawPath(path, paintRSI)
    }

    private fun drawMACD(canvas: Canvas, range: IntRange) {
        val macd = macdData ?: return
        var maxVal = Float.MIN_VALUE
        var minVal = Float.MAX_VALUE

        for (i in range) {
            if (i >= macd.macdLine.size) break
            macd.macdLine[i]?.let { if (it > maxVal) maxVal = it; if (it < minVal) minVal = it }
            macd.histogram[i]?.let { if (it > maxVal) maxVal = it; if (it < minVal) minVal = it }
        }

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
            val x = indexToX(i)

            macd.histogram[i]?.let { h ->
                val y = macdY(h)
                val paint = if (h >= 0) paintHistBull else paintHistBear
                canvas.drawRect(RectF(x - bodyW/2f, kotlin.math.min(zeroY, y), x + bodyW/2f, kotlin.math.max(zeroY, y)), paint)
            }
            macd.macdLine[i]?.let { v -> if (firstMacd) { pathMacd.moveTo(x, macdY(v)); firstMacd = false } else pathMacd.lineTo(x, macdY(v)) }
            macd.signalLine[i]?.let { v -> if (firstSignal) { pathSignal.moveTo(x, macdY(v)); firstSignal = false } else pathSignal.lineTo(x, macdY(v)) }
        }
        canvas.drawPath(pathSignal, paintSignalLine)
        canvas.drawPath(pathMacd, paintMacdLine)
    }
}