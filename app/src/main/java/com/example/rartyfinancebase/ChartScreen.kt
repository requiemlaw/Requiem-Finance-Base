package com.example.rartyfinancebase

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
private val BG = Color(0xFF0B0E11)
private val SURFACE = Color(0xFF161A1E)
private val GOLD = Color(0xFFF0B90B)
private val GRAY = Color(0xFF848E9C)
private val WHITE = Color.White

private data class ChartSymbol(val display: String, val apiSymbol: String, val isCrypto: Boolean = false)
private val SYMBOLS = listOf(
    ChartSymbol("BTC", "BTC", true), ChartSymbol("ETH", "ETH", true),
    ChartSymbol("AAPL", "AAPL"), ChartSymbol("MSFT", "MSFT"),
    ChartSymbol("NVDA", "NVDA"), ChartSymbol("TSLA", "TSLA"),
    ChartSymbol("GOOGL", "GOOGL"), ChartSymbol("AMZN", "AMZN"),
    ChartSymbol("GC=F", "GC=F"), ChartSymbol("BİST", "XU100.IS"),
    ChartSymbol("S&P", "^GSPC"), ChartSymbol("USD/TRY", "TRY=X")
)

private data class IntervalOption(val label: String, val interval: String, val range: String)

// İSTEĞİN ÜZERİNE YENİ ZAMAN ARALIKLARI STRATEJİK OLARAK AYARLANDI
private val INTERVALS = listOf(
    IntervalOption("1DK",  "1m",   "1d"),
    IntervalOption("5DK",  "5m",   "1d"),
    IntervalOption("15DK", "15m",  "5d"),
    IntervalOption("1S",   "1h",   "60d"),
    IntervalOption("4S",   "4h",   "60d"),
    IntervalOption("1G",   "1d",   "2y")
)

@Composable
fun ChartScreen() {
    val context = LocalContext.current
    val flashColor = remember { androidx.compose.animation.Animatable(Color.Transparent) }
    var selectedSymbol by remember { mutableStateOf(SYMBOLS[0]) }
    var selectedInterval by remember { mutableStateOf(INTERVALS[0]) }

    // State Tanımlamaları
    var isRsiEnabled by remember { mutableStateOf(true) }
    var isMacdEnabled by remember { mutableStateOf(false) }
    var isMaEnabled by remember { mutableStateOf(true) }
    var isBollingerEnabled by remember { mutableStateOf(false) }
    var currentMaInput by remember { mutableStateOf("20, 50") }
    var activeMaPeriods by remember { mutableStateOf(listOf(20, 50)) }
    var chartViewRef by remember { mutableStateOf<CandlestickChartView?>(null) }

    // CANLI VERİ TETİKLEYİCİSİ: Her veri geldiğinde bu sayı artacak ve flaşör çakacak!
    var triggerBlink by remember { mutableStateOf(0) }

    // Flaşör Animasyon Motoru
    LaunchedEffect(triggerBlink, selectedSymbol) {
        if (triggerBlink > 0) {
            flashColor.animateTo(
                targetValue = Color(0x3300D06C), // %20 saydam kurumsal yeşil
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 100)
            )
            flashColor.animateTo(
                targetValue = Color.Transparent,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 350)
            )
        }
    }

    LaunchedEffect(selectedSymbol, selectedInterval, activeMaPeriods, isRsiEnabled, isMacdEnabled, isMaEnabled, isBollingerEnabled) {
        chartViewRef?.let { chartView ->
            loadChartData(context, chartView, selectedSymbol, selectedInterval, activeMaPeriods, isRsiEnabled, isMacdEnabled, isMaEnabled, isBollingerEnabled) {
                // Veri başarıyla yüklenip render edildiğinde canlı tetiği ateşle!
                triggerBlink++
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BG)) {

        Text(
            text = "CHART",
            color = WHITE,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(start = 20.dp, top = 24.dp, bottom = 12.dp)
                .background(flashColor.value, shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )

        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            SYMBOLS.forEach { sym ->
                val isSelected = sym == selectedSymbol
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(end = 8.dp).height(32.dp).widthIn(min = 60.dp).background(if (isSelected) SURFACE else Color(0xFF0F1217), RoundedCornerShape(4.dp)).border(width = if (isSelected) 1.dp else 0.dp, color = if (isSelected) GOLD else Color.Transparent, RoundedCornerShape(4.dp)).clickable { selectedSymbol = sym }.padding(horizontal = 12.dp)) {
                    Text(text = sym.display, color = if (isSelected) GOLD else GRAY, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                INTERVALS.forEach { opt ->
                    val isSelected = opt == selectedInterval
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(end = 6.dp).height(28.dp).width(48.dp).background(if (isSelected) SURFACE else Color(0xFF0F1217), RoundedCornerShape(4.dp)).border(width = if (isSelected) 1.dp else 0.dp, color = if (isSelected) GOLD else Color.Transparent, RoundedCornerShape(4.dp)).clickable { selectedInterval = opt }) {
                        Text(text = opt.label, color = if (isSelected) GOLD else GRAY, fontSize = 11.sp)
                    }
                }
            }
            Icon(painter = painterResource(id = android.R.drawable.ic_menu_manage), contentDescription = "Indicators", tint = GOLD, modifier = Modifier.size(26.dp).clickable {
                openIndicatorSettings(context, isRsiEnabled, isMacdEnabled, isMaEnabled, isBollingerEnabled, currentMaInput) { rsi, macd, ma, bol, maInput, maList ->
                    isRsiEnabled = rsi; isMacdEnabled = macd; isMaEnabled = ma; isBollingerEnabled = bol; currentMaInput = maInput; activeMaPeriods = maList
                }
            })
        }

        AndroidView(
            factory = { ctx -> CandlestickChartView(ctx).also { view -> chartViewRef = view; loadChartData(ctx, view, selectedSymbol, selectedInterval, activeMaPeriods, isRsiEnabled, isMacdEnabled, isMaEnabled, isBollingerEnabled) {} } },
            modifier = Modifier.fillMaxWidth().weight(1f).background(BG)
        )
    }
}

// ── Yardımcı Fonksiyonlar ──────────────────────────
private fun openIndicatorSettings(context: android.content.Context, rsi: Boolean, macd: Boolean, ma: Boolean, bol: Boolean, maText: String, onApply: (Boolean, Boolean, Boolean, Boolean, String, List<Int>) -> Unit) {
    val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(context)
    dialog.setContentView(R.layout.bottom_sheet_indicators)
    dialog.findViewById<android.widget.Switch>(R.id.switchRSI)?.isChecked = rsi
    dialog.findViewById<android.widget.Switch>(R.id.switchMACD)?.isChecked = macd
    dialog.findViewById<android.widget.Switch>(R.id.switchMA)?.isChecked = ma
    dialog.findViewById<android.widget.Switch>(R.id.switchBollinger)?.isChecked = bol
    dialog.findViewById<android.widget.EditText>(R.id.etMAPeriods)?.setText(maText)
    dialog.findViewById<android.widget.Button>(R.id.btnApplyIndicators)?.setOnClickListener {
        val maList = mutableListOf<Int>()
        dialog.findViewById<android.widget.EditText>(R.id.etMAPeriods)?.text.toString().split(",").forEach { p -> try { maList.add(p.trim().toInt()) } catch (e: Exception) {} }
        onApply(
            dialog.findViewById<android.widget.Switch>(R.id.switchRSI)!!.isChecked,
            dialog.findViewById<android.widget.Switch>(R.id.switchMACD)!!.isChecked,
            dialog.findViewById<android.widget.Switch>(R.id.switchMA)!!.isChecked,
            dialog.findViewById<android.widget.Switch>(R.id.switchBollinger)!!.isChecked,
            dialog.findViewById<android.widget.EditText>(R.id.etMAPeriods)!!.text.toString(),
            maList
        )
        dialog.dismiss()
    }
    dialog.show()
}

private fun loadChartData(context: android.content.Context, chartView: CandlestickChartView, symbol: ChartSymbol, intervalOpt: IntervalOption, activeMaPeriods: List<Int>, rsi: Boolean, macd: Boolean, ma: Boolean, bol: Boolean, onDataLoaded: () -> Unit) {
    if (symbol.isCrypto) loadBinance(chartView, symbol.apiSymbol, intervalOpt.interval, activeMaPeriods, rsi, macd, ma, bol, onDataLoaded)
    else loadYahoo(chartView, symbol.apiSymbol, intervalOpt.interval, intervalOpt.range, activeMaPeriods, rsi, macd, ma, bol, onDataLoaded)
}

private fun loadBinance(chartView: CandlestickChartView, symbol: String, interval: String, activeMaPeriods: List<Int>, rsi: Boolean, macd: Boolean, ma: Boolean, bol: Boolean, onDataLoaded: () -> Unit) {
    val api = Retrofit.Builder().baseUrl("https://api.binance.com/").addConverterFactory(GsonConverterFactory.create()).build().create(BinanceApi::class.java)
    api.getKlines(if (symbol.endsWith("USDT")) symbol else "${symbol}USDT", interval, 1000).enqueue(object : Callback<List<List<Any>>> {
        override fun onResponse(call: Call<List<List<Any>>>, response: Response<List<List<Any>>>) {
            val candles = response.body()?.mapNotNull { k -> try { CandlestickChartView.Candle(k[1].toString().toFloat(), k[2].toString().toFloat(), k[3].toString().toFloat(), k[4].toString().toFloat(), k[5].toString().toFloat(), k[0].toString().toDouble().toLong() / 1000) } catch (e: Exception) { null } } ?: return
            renderToChart(chartView, candles, activeMaPeriods, rsi, macd, ma, bol, onDataLoaded)
        }
        override fun onFailure(call: Call<List<List<Any>>>, t: Throwable) {}
    })
}

private fun loadYahoo(chartView: CandlestickChartView, symbol: String, interval: String, range: String, activeMaPeriods: List<Int>, rsi: Boolean, macd: Boolean, ma: Boolean, bol: Boolean, onDataLoaded: () -> Unit) {
    val api = Retrofit.Builder().baseUrl("https://query1.finance.yahoo.com/").addConverterFactory(GsonConverterFactory.create()).build().create(YahooApi::class.java)
    api.getChartData(symbol, interval, range).enqueue(object : Callback<YahooFinanceResponse> {
        override fun onResponse(call: Call<YahooFinanceResponse>, response: Response<YahooFinanceResponse>) {
            val result = response.body()?.chart?.result?.firstOrNull() ?: return
            val quote = result.indicators?.quote?.firstOrNull() ?: return
            val candles = mutableListOf<CandlestickChartView.Candle>()
            for (i in quote.close?.indices ?: return) {
                val c = quote.close[i] ?: continue; val o = quote.open?.get(i) ?: continue
                candles.add(CandlestickChartView.Candle(o.toFloat(), quote.high?.get(i)?.toFloat() ?: o.toFloat(), quote.low?.get(i)?.toFloat() ?: o.toFloat(), c.toFloat(), quote.volume?.get(i)?.toFloat() ?: 0f, result.timestamp?.get(i) ?: 0L))
            }
            renderToChart(chartView, candles, activeMaPeriods, rsi, macd, ma, bol, onDataLoaded)
        }
        override fun onFailure(call: Call<YahooFinanceResponse>, t: Throwable) {}
    })
}

private fun renderToChart(chartView: CandlestickChartView, candles: List<CandlestickChartView.Candle>, activeMaPeriods: List<Int>, rsi: Boolean, macd: Boolean, ma: Boolean, bol: Boolean, onDataLoaded: () -> Unit) {
    val closes = candles.map { it.close }
    val mas = if(ma) IndicatorEngine.computeDynamicMAs(closes, activeMaPeriods) else null
    val bbands = if(bol) IndicatorEngine.computeBollingerBands(closes) else null

    chartView.post {
        chartView.setData(candles, mas, IndicatorEngine.rsi(closes, 14), IndicatorEngine.computeMacd(closes), rsi, macd, ma, bol, bbands)
        // Arayüz başarıyla çizildi, veri akışı onaylandı!
        onDataLoaded()
    }
}