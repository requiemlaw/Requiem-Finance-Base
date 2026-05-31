package com.example.rartyfinancebase

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

@Composable
fun OrderBookScreen(modifier: Modifier = Modifier, viewModel: TerminalViewModel = viewModel()) {
    val orderBook by viewModel.orderBookState.collectAsState()
    val imbalance by viewModel.imbalanceState.collectAsState()
    val klines by viewModel.klinesState.collectAsState()
    val selectedTimeframe by viewModel.selectedTimeframe.collectAsState()
    val currentPrice by viewModel.currentPrice.collectAsState()

    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while(true) { currentTimeMs = System.currentTimeMillis(); delay(1000L) } }
    val interval = when(selectedTimeframe) { "1m"->60000L; "5m"->300000L; "15m"->900000L; "1h"->3600000L; "4h"->14400000L; "1d"->86400000L; else->60000L }
    val rem = max(0L, (((currentTimeMs / interval) + 1) * interval) - currentTimeMs)
    val cd = String.format("%02d:%02d", (rem / 60000), (rem / 1000) % 60)

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF0B0E11)).statusBarsPadding().padding(horizontal = 14.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("REQUIEM TERMINAL", color = Color(0xFFF0B90B), fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.background(Color(0xFF2EBD85).copy(alpha = 0.2f), RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("●", color = Color(0xFF2EBD85), fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LIVE", color = Color(0xFF2EBD85), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                Text("BTC/USDT", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text("$${String.format("%.2f", currentPrice)}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("1m", "5m", "15m", "1h", "4h", "1d").forEach { tf ->
                val isSel = selectedTimeframe == tf
                Text(tf.uppercase(), color = if (isSel) Color(0xFFF0B90B) else Color(0xFF848E9C), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (isSel) Color(0xFFF0B90B).copy(0.15f) else Color.Transparent).clickable { viewModel.selectTimeframe(tf) }.padding(8.dp, 4.dp))
            }
        }

        // MUM GRAFİĞİ
        Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
            LiveCandleChart(klines)
            Text("YENİ MUM: $cd", color = Color(0xFFF0B90B), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
        }

        ImbalanceIndicator(imbalance)

        // --- V2.0 için: MARKET DEPTH (DERİNLİK) GRAFİĞİ ---
        Spacer(modifier = Modifier.height(10.dp))
        Text("PİYASA DERİNLİĞİ (MARKET DEPTH)", color = Color(0xFF848E9C), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.fillMaxWidth().height(120.dp).padding(vertical = 8.dp)) {
            // orderBook null değilse çiz, boşsa (yükleniyorsa) gösterme
            orderBook?.let { book ->
                MarketDepthChart(bids = book.bids, asks = book.asks)
            }
        }
        // -----------------------------------------------------------------------

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("Alış (Bids)", color = Color(0xFF848E9C), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Satış (Asks)", color = Color(0xFF848E9C), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        Row(modifier = Modifier.weight(1.5f)) {
            Box(modifier = Modifier.weight(1f)) { orderBook?.bids?.take(25)?.let { OrderBookList(it, true, 100.0) } }
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) { orderBook?.asks?.take(25)?.let { OrderBookList(it, false, 100.0) } }
        }
    }
}


@Composable
fun MarketDepthChart(bids: List<OrderBookItem>, asks: List<OrderBookItem>) {
    if (bids.isEmpty() || asks.isEmpty()) return

    // Birikimli (Cumulative) hacim hesaplama
    val cumulativeBids = mutableListOf<Float>()
    var sumBids = 0f
    for (bid in bids.take(20)) {
        sumBids += (bid.quantity.toFloatOrNull() ?: 0f)
        cumulativeBids.add(sumBids)
    }

    val cumulativeAsks = mutableListOf<Float>()
    var sumAsks = 0f
    for (ask in asks.take(20)) {
        sumAsks += (ask.quantity.toFloatOrNull() ?: 0f)
        cumulativeAsks.add(sumAsks)
    }

    val maxVolume = max(cumulativeBids.maxOrNull() ?: 1f, cumulativeAsks.maxOrNull() ?: 1f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val halfWidth = width / 2f

        // --- SOL TARAF: ALIŞ (BID) DUVARI (YEŞİL) ---
        val bidPath = Path()
        bidPath.moveTo(halfWidth, height) // Ortadan başla
        val bidStepX = halfWidth / cumulativeBids.size

        cumulativeBids.forEachIndexed { index, volume ->
            // Fiyatlar sondan başa doğru (merkezden dışarı)
            val x = halfWidth - (index * bidStepX)
            val y = height - ((volume / maxVolume) * height)
            bidPath.lineTo(x, y)
        }
        bidPath.lineTo(0f, height) // Sol alt köşeye in
        bidPath.close() // Kapatıp doldur

        drawPath(
            path = bidPath,
            color = Color(0xFF008A4D).copy(alpha = 0.5f) // Mat Yeşil Şeffaf
        )

        // --- SAĞ TARAF: SATIŞ (ASK) DUVARI (KIRMIZI) ---
        val askPath = Path()
        askPath.moveTo(halfWidth, height) // Ortadan başla
        val askStepX = halfWidth / cumulativeAsks.size

        cumulativeAsks.forEachIndexed { index, volume ->
            val x = halfWidth + (index * askStepX)
            val y = height - ((volume / maxVolume) * height)
            askPath.lineTo(x, y)
        }
        askPath.lineTo(width, height) // Sağ alt köşeye in
        askPath.close() // Kapatıp doldur

        drawPath(
            path = askPath,
            color = Color(0xFFFF3B30).copy(alpha = 0.5f) // Kırmızı Şeffaf
        )
    }
}

@Composable fun LiveCandleChart(candles: List<Candle>) {
    if (candles.isEmpty()) return
    val maxP = candles.maxOfOrNull { it.high } ?: 0f; val minP = candles.minOfOrNull { it.low } ?: 1f
    val range = maxP - minP; val pad = range * 0.2f
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width / candles.size.toFloat()
        candles.forEachIndexed { i, c ->
            val color = if (c.close >= c.open) Color(0xFF2EBD85) else Color(0xFFF6465D)
            val x = i * w + (w / 2f)
            val yO = size.height - ((c.open - minP) / (range + pad) * size.height)
            val yC = size.height - ((c.close - minP) / (range + pad) * size.height)
            drawLine(color, Offset(x, size.height - ((c.high - minP) / (range + pad) * size.height)), Offset(x, size.height - ((c.low - minP) / (range + pad) * size.height)), 2f)
            drawRect(color, Offset(x - (w*0.7f) / 2f, min(yO, yC)), Size(w*0.7f, max(2f, Math.abs(yO - yC))))
        }
    }
}

@Composable fun ImbalanceIndicator(i: Double) {
    val bP by animateFloatAsState(((i + 1.0) / 2.0).coerceIn(0.02, 0.98).toFloat(), tween(300))
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Boğa Baskısı", color = Color(0xFF2EBD85), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("Ayı Baskısı", color = Color(0xFFF6465D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
            Box(modifier = Modifier.weight(bP).fillMaxHeight().background(Color(0xFF2EBD85)))
            Box(modifier = Modifier.weight(1f - bP).fillMaxHeight().background(Color(0xFFF6465D)))
        }
    }
}

@Composable fun OrderBookList(items: List<OrderBookItem>, isBid: Boolean, maxQty: Double) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items) { item ->
            val q = item.quantity.toDoubleOrNull() ?: 0.0
            Box(modifier = Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.CenterStart) {
                Box(modifier = Modifier.fillMaxWidth((q/max(maxQty, 1.0)).toFloat()).fillMaxHeight().background(if (isBid) Color(0xFF2EBD85).copy(0.15f) else Color(0xFFF6465D).copy(0.15f)).align(if (isBid) Alignment.CenterEnd else Alignment.CenterStart))
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = item.price, color = if (isBid) Color(0xFF2EBD85) else Color(0xFFF6465D), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(text = String.format("%.4f", q), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace, textAlign = TextAlign.End)
                }
            }
        }
    }
}