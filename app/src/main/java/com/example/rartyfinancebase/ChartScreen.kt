package com.example.rartyfinancebase

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// ── Renk sabitleri ────────────────────────────────────────────────────────
private val BG        = Color(0xFF0B0E11)
private val SURFACE   = Color(0xFF161A1E)
private val GOLD      = Color(0xFFF0B90B)
private val GRAY      = Color(0xFF848E9C)
private val WHITE     = Color.White

// ── Sembol listesi ────────────────────────────────────────────────────────
private data class ChartSymbol(
    val display: String,
    val apiSymbol: String,
    val isCrypto: Boolean = false
)

private val SYMBOLS = listOf(
    ChartSymbol("BTC",   "BTC",    isCrypto = true),
    ChartSymbol("ETH",   "ETH",    isCrypto = true),
    ChartSymbol("AAPL",  "AAPL"),
    ChartSymbol("MSFT",  "MSFT"),
    ChartSymbol("NVDA",  "NVDA"),
    ChartSymbol("TSLA",  "TSLA"),
    ChartSymbol("GOOGL", "GOOGL"),
    ChartSymbol("AMZN",  "AMZN"),
    ChartSymbol("GC=F",  "GC=F"),
    ChartSymbol("BİST",  "XU100.IS"),
    ChartSymbol("S&P",   "^GSPC"),
    ChartSymbol("USD/TRY","TRY=X"),
)

private data class IntervalOption(
    val label: String,
    val interval: String,
    val range: String
)

private val INTERVALS = listOf(
    IntervalOption("15D", "15m",  "2d"),
    IntervalOption("1S",  "1h",   "5d"),
    IntervalOption("4S",  "4h",   "14d"),
    IntervalOption("1G",  "1d",   "1mo"),
    IntervalOption("1H",  "1wk",  "6mo"),
    IntervalOption("1AY", "1mo",  "1y"),
)

// ─────────────────────────────────────────────────────────────────────────
@Composable
fun ChartScreen() {
    val context = LocalContext.current

    var selectedSymbol   by remember { mutableStateOf(SYMBOLS[0]) }
    var selectedInterval by remember { mutableStateOf(INTERVALS[0]) }

    // CandlestickChartView referansı — AndroidView içinden dışa çıkarıyoruz
    var chartViewRef by remember { mutableStateOf<CandlestickChartView?>(null) }

    // Veri yükleme tetikleyici
    val loadTrigger by remember(selectedSymbol, selectedInterval) { mutableStateOf(Unit) }

    // API çağrısı
    LaunchedEffect(selectedSymbol, selectedInterval) {
        chartViewRef?.let { chartView ->
            loadChartData(
                context     = context,
                chartView   = chartView,
                symbol      = selectedSymbol,
                intervalOpt = selectedInterval
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
    ) {
        // ── Başlık ──────────────────────────────────────────────────────
        Text(
            text       = "CHART",
            color      = WHITE,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 12.dp)
        )

        // ── Sembol seçici ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SYMBOLS.forEach { sym ->
                val isSelected = sym == selectedSymbol
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .height(30.dp)
                        .widthIn(min = 52.dp)
                        .background(
                            if (isSelected) SURFACE else Color(0xFF0F1217),
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) GOLD else Color.Transparent,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { selectedSymbol = sym }
                        .padding(horizontal = 10.dp)
                ) {
                    Text(
                        text     = sym.display,
                        color    = if (isSelected) GOLD else GRAY,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Zaman aralığı seçici ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            INTERVALS.forEach { opt ->
                val isSelected = opt == selectedInterval
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .height(28.dp)
                        .width(48.dp)
                        .background(
                            if (isSelected) SURFACE else Color(0xFF0F1217),
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) GOLD else Color.Transparent,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { selectedInterval = opt }
                ) {
                    Text(
                        text     = opt.label,
                        color    = if (isSelected) GOLD else GRAY,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── CandlestickChartView (AndroidView köprüsü) ───────────────────
        AndroidView(
            factory = { ctx ->
                CandlestickChartView(ctx).also { view ->
                    chartViewRef = view
                    // İlk yükleme — factory içinde direkt çağırıyoruz
                    loadChartData(ctx, view, selectedSymbol, selectedInterval)
                }
            },
            update = { view ->
                // Sembol veya interval değişince update bloğu tetiklenir
                chartViewRef = view
                loadChartData(context, view, selectedSymbol, selectedInterval)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(BG)
        )
    }
}

// ── Veri yükleme fonksiyonu ───────────────────────────────────────────────
private fun loadChartData(
    context: android.content.Context,
    chartView: CandlestickChartView,
    symbol: ChartSymbol,
    intervalOpt: IntervalOption
) {
    if (symbol.isCrypto) {
        loadBinance(chartView, symbol.apiSymbol, intervalOpt.interval)
    } else {
        loadYahoo(chartView, symbol.apiSymbol, intervalOpt.interval, intervalOpt.range)
    }
}

private fun loadBinance(
    chartView: CandlestickChartView,
    symbol: String,
    interval: String
) {
    val api = Retrofit.Builder()
        .baseUrl("https://api.binance.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(BinanceApi::class.java)

    val bSymbol = if (symbol.endsWith("USDT")) symbol else "${symbol}USDT"

    api.getKlines(bSymbol, interval, 150).enqueue(object : Callback<List<List<Any>>> {
        override fun onResponse(call: Call<List<List<Any>>>, response: Response<List<List<Any>>>) {
            val body = response.body() ?: return
            val candles = body.mapNotNull { kline ->
                try {
                    CandlestickChartView.Candle(
                        open      = kline[1].toString().toFloat(),
                        high      = kline[2].toString().toFloat(),
                        low       = kline[3].toString().toFloat(),
                        close     = kline[4].toString().toFloat(),
                        volume    = kline[5].toString().toFloat(),
                        timestamp = kline[0].toString().toDouble().toLong() / 1000
                    )
                } catch (e: Exception) { null }
            }
            renderToChart(chartView, candles)
        }
        override fun onFailure(call: Call<List<List<Any>>>, t: Throwable) {}
    })
}

private fun loadYahoo(
    chartView: CandlestickChartView,
    symbol: String,
    interval: String,
    range: String
) {
    val api = Retrofit.Builder()
        .baseUrl("https://query1.finance.yahoo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(YahooApi::class.java)

    api.getChartData(symbol, interval, range).enqueue(object : Callback<YahooFinanceResponse> {
        override fun onResponse(call: Call<YahooFinanceResponse>, response: Response<YahooFinanceResponse>) {
            val result = response.body()?.chart?.result?.firstOrNull() ?: return
            val quote  = result.indicators?.quote?.firstOrNull() ?: return
            val closes = quote.close ?: return
            val opens  = quote.open  ?: return
            val highs  = quote.high
            val lows   = quote.low
            val vols   = quote.volume
            val times  = result.timestamp

            val candles = mutableListOf<CandlestickChartView.Candle>()
            for (i in closes.indices) {
                val c = closes[i] ?: continue
                val o = opens[i]  ?: continue
                candles.add(
                    CandlestickChartView.Candle(
                        open      = o.toFloat(),
                        high      = highs?.getOrNull(i)?.toFloat() ?: o.toFloat(),
                        low       = lows?.getOrNull(i)?.toFloat()  ?: o.toFloat(),
                        close     = c.toFloat(),
                        volume    = vols?.getOrNull(i)?.toFloat()  ?: 0f,
                        timestamp = times?.getOrNull(i) ?: 0L
                    )
                )
            }
            renderToChart(chartView, candles)
        }
        override fun onFailure(call: Call<YahooFinanceResponse>, t: Throwable) {}
    })
}

private fun renderToChart(chartView: CandlestickChartView, candles: List<CandlestickChartView.Candle>) {
    val (ma20, ma50, rsi) = IndicatorEngine.computeAll(candles)
    chartView.post {
        chartView.setData(candles, ma20, ma50, rsi)
    }
}
