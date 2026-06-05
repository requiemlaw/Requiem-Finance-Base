package com.example.rartyfinancebase

/**
 * IndicatorEngine
 *
 * Requiem Finance Terminal — Quantitative indicator calculation module.
 *
 * Provides static methods for:
 * - Simple Moving Average (SMA / MA) -> Dinamik liste destekli
 * - Relative Strength Index (RSI-14, Wilder's smoothing method)
 * - MACD (Moving Average Convergence Divergence)
 */
object IndicatorEngine {

    /**
     * MACD Sonuçlarını tutan veri sınıfı
     * (MACD Line, Signal Line, Histogram)
     */
    data class MacdResult(
        val macdLine: List<Float?>,
        val signalLine: List<Float?>,
        val histogram: List<Float?>
    )

    /**
     * Simple Moving Average.
     */
    fun ma(prices: List<Float>, period: Int): List<Float?> {
        if (period <= 0 || prices.isEmpty()) return List(prices.size) { null }

        val result = ArrayList<Float?>(prices.size)
        for (i in prices.indices) {
            if (i < period - 1) {
                result.add(null)
            } else {
                var sum = 0f
                for (j in 0 until period) sum += prices[i - j]
                result.add(sum / period)
            }
        }
        return result
    }

    /**
     * EMA (Exponential Moving Average) - MACD için gerekli.
     */
    fun ema(prices: List<Float>, period: Int): List<Float?> {
        if (period <= 0 || prices.isEmpty() || prices.size < period) return List(prices.size) { null }

        val result = ArrayList<Float?>(prices.size)
        val multiplier = 2f / (period + 1f)
        var prevEma: Float? = null

        for (i in prices.indices) {
            if (i < period - 1) {
                result.add(null)
            } else if (i == period - 1) {
                // İlk EMA değeri, o periyodun SMA'sıdır
                var sum = 0f
                for (j in 0 until period) sum += prices[i - j]
                val firstEma = sum / period
                result.add(firstEma)
                prevEma = firstEma
            } else {
                // EMA = (Price - prevEMA) * multiplier + prevEMA
                val currentEma = (prices[i] - prevEma!!) * multiplier + prevEma
                result.add(currentEma)
                prevEma = currentEma
            }
        }
        return result
    }

    /**
     * Dinamik MA Listesi Hesaplayıcı
     * Kullanıcı [20, 50, 100] girdiğinde, 3 ayrı listeyi Map olarak döner.
     */
    fun computeDynamicMAs(prices: List<Float>, periods: List<Int>): Map<Int, List<Float?>> {
        val resultMap = mutableMapOf<Int, List<Float?>>()
        for (period in periods) {
            resultMap[period] = ma(prices, period)
        }
        return resultMap
    }

    /**
     * Relative Strength Index (RSI)
     */
    fun rsi(prices: List<Float>, period: Int = 14): List<Float?> {
        if (prices.size < period + 1) return List(prices.size) { null }

        val result = ArrayList<Float?>(prices.size)
        for (i in 0 until period) result.add(null)

        var avgGain = 0f
        var avgLoss = 0f
        for (i in 1..period) {
            val change = prices[i] - prices[i - 1]
            if (change > 0) avgGain += change else avgLoss += abs(change)
        }
        avgGain /= period
        avgLoss /= period

        result.add(calculateRsi(avgGain, avgLoss))

        for (i in (period + 1) until prices.size) {
            val change = prices[i] - prices[i - 1]
            val gain   = if (change > 0) change else 0f
            val loss   = if (change < 0) abs(change) else 0f

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period

            result.add(calculateRsi(avgGain, avgLoss))
        }

        return result
    }

    private fun calculateRsi(avgGain: Float, avgLoss: Float): Float {
        if (avgLoss == 0f) return 100f
        val rs = avgGain / avgLoss
        return 100f - (100f / (1f + rs))
    }

    /**
     * MACD (12, 26, 9) Hesaplayıcı
     */
    fun computeMacd(prices: List<Float>, fast: Int = 12, slow: Int = 26, signal: Int = 9): MacdResult {
        val emaFast = ema(prices, fast)
        val emaSlow = ema(prices, slow)

        val macdLine = ArrayList<Float?>(prices.size)
        val nonNullMacdForSignal = ArrayList<Float>()

        // MACD Line = EMA(fast) - EMA(slow)
        for (i in prices.indices) {
            val f = emaFast[i]
            val s = emaSlow[i]
            if (f != null && s != null) {
                val diff = f - s
                macdLine.add(diff)
                nonNullMacdForSignal.add(diff)
            } else {
                macdLine.add(null)
            }
        }

        // Signal Line = EMA(MACD Line, signal)
        val signalLineBase = ema(nonNullMacdForSignal, signal)
        val signalLine = ArrayList<Float?>(prices.size)

        // MACD Line null olan kısımları Signal Line için de null olarak eşitle
        var signalIndex = 0
        for (m in macdLine) {
            if (m == null) {
                signalLine.add(null)
            } else {
                if (signalIndex < signalLineBase.size) {
                    signalLine.add(signalLineBase[signalIndex])
                    signalIndex++
                } else {
                    signalLine.add(null)
                }
            }
        }

        // Histogram = MACD Line - Signal Line
        val histogram = ArrayList<Float?>(prices.size)
        for (i in prices.indices) {
            val m = macdLine[i]
            val s = signalLine[i]
            if (m != null && s != null) {
                histogram.add(m - s)
            } else {
                histogram.add(null)
            }
        }

        return MacdResult(macdLine, signalLine, histogram)
    }

    fun computeBollingerBands(closes: List<Float>, period: Int = 20, multiplier: Float = 2f): Triple<List<Float?>, List<Float?>, List<Float?>> {
        val upper = mutableListOf<Float?>()
        val mid = mutableListOf<Float?>()
        val lower = mutableListOf<Float?>()

        for (i in closes.indices) {
            if (i < period - 1) {
                upper.add(null); mid.add(null); lower.add(null)
            } else {
                val sub = closes.subList(i - period + 1, i + 1)
                val avg = sub.average().toFloat()
                val stdDev = Math.sqrt(sub.map { Math.pow((it - avg).toDouble(), 2.0) }.average()).toFloat()
                mid.add(avg)
                upper.add(avg + (multiplier * stdDev))
                lower.add(avg - (multiplier * stdDev))
            }
        }
        return Triple(upper, mid, lower)
    }

    // ── Utility ────────────────────────────────────────────────────────────
    private fun abs(f: Float) = if (f < 0) -f else f
}