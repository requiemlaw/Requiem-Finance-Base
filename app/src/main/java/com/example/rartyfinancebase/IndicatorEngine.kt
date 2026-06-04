package com.example.rartyfinancebase

/**
 * IndicatorEngine
 *
 * Requiem Finance Terminal — Quantitative indicator calculation module.
 *
 * Provides static methods for:
 *   - Simple Moving Average  (SMA / MA)
 *   - Relative Strength Index (RSI-14, Wilder's smoothing method)
 *
 * All functions return lists of the same length as the input.
 * Values are null for the warmup period (insufficient data).
 *
 * Usage:
 *   val closes = candles.map { it.close }
 *   val ma20   = IndicatorEngine.ma(closes, 20)
 *   val ma50   = IndicatorEngine.ma(closes, 50)
 *   val rsi    = IndicatorEngine.rsi(closes, 14)
 */
object IndicatorEngine {

    /**
     * Simple Moving Average over [period] bars.
     *
     * @param prices  list of closing prices (oldest first)
     * @param period  lookback window
     * @return        list of [Float?], null for index < period - 1
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
     * Relative Strength Index using Wilder's Exponential Smoothing (SMMA).
     *
     * Standard 14-period RSI as used by Bloomberg, TradingView, and most
     * institutional platforms.
     *
     * Formula:
     *   RS  = AvgGain / AvgLoss   (Wilder smoothed)
     *   RSI = 100 - (100 / (1 + RS))
     *
     * @param prices  list of closing prices (oldest first)
     * @param period  RSI period (default: 14)
     * @return        list of [Float?], null for index < period
     */
    fun rsi(prices: List<Float>, period: Int = 14): List<Float?> {
        if (prices.size < period + 1) return List(prices.size) { null }

        val result = ArrayList<Float?>(prices.size)

        // Step 1: fill nulls for warmup
        for (i in 0 until period) result.add(null)

        // Step 2: compute initial average gain / loss over first [period] changes
        var avgGain = 0f
        var avgLoss = 0f
        for (i in 1..period) {
            val change = prices[i] - prices[i - 1]
            if (change > 0) avgGain += change else avgLoss += abs(change)
        }
        avgGain /= period
        avgLoss /= period

        // First RSI value
        result.add(calculateRsi(avgGain, avgLoss))

        // Step 3: Wilder smoothing for the rest
        for (i in (period + 1) until prices.size) {
            val change = prices[i] - prices[i - 1]
            val gain   = if (change > 0) change else 0f
            val loss   = if (change < 0) abs(change) else 0f

            // Wilder's smoothed moving average: new = (prev * (n-1) + current) / n
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period

            result.add(calculateRsi(avgGain, avgLoss))
        }

        return result
    }

    /**
     * Compute RSI value from smoothed average gain and loss.
     * Handles the edge case of zero avgLoss (RSI = 100).
     */
    private fun calculateRsi(avgGain: Float, avgLoss: Float): Float {
        if (avgLoss == 0f) return 100f
        val rs = avgGain / avgLoss
        return 100f - (100f / (1f + rs))
    }

    /**
     * Convenience: compute all indicators at once from a candle list.
     *
     * @return Triple(ma20, ma50, rsi14)
     */
    fun computeAll(candles: List<CandlestickChartView.Candle>): Triple<List<Float?>, List<Float?>, List<Float?>> {
        val closes = candles.map { it.close }
        return Triple(
            ma(closes, 20),
            ma(closes, 50),
            rsi(closes, 14)
        )
    }

    // ── Utility ────────────────────────────────────────────────────────────

    /** Absolute value helper for Float */
    private fun abs(f: Float) = if (f < 0) -f else f
}
