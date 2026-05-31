package com.example.rartyfinancebase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// --- VERİ MODELLERİ ---
data class OrderBookItem(val price: String, val quantity: String)
data class OrderBook(val bids: List<OrderBookItem>, val asks: List<OrderBookItem>)
data class Candle(val open: Float, val high: Float, val low: Float, val close: Float)
data class PortfolioAsset(val symbol: String, val amount: Double, val buyPrice: Double, val category: String)

// --- TERMINAL ANA YAPISI (Canlı WebSocket & Grafik Motoru) ---
class TerminalViewModel : ViewModel() {
    private val _orderBookState = MutableStateFlow<OrderBook?>(null)
    val orderBookState: StateFlow<OrderBook?> = _orderBookState

    private val _imbalanceState = MutableStateFlow(0.0)
    val imbalanceState: StateFlow<Double> = _imbalanceState

    private val _klinesState = MutableStateFlow<List<Candle>>(emptyList())
    val klinesState: StateFlow<List<Candle>> = _klinesState

    private val _selectedTimeframe = MutableStateFlow("15m")
    val selectedTimeframe: StateFlow<String> = _selectedTimeframe

    private val _currentPrice = MutableStateFlow(0.0)
    val currentPrice: StateFlow<Double> = _currentPrice

    // OkHttp Ağı ve WebSocket İstemcisi
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    init {

        connectBinanceWebSocket()
        fetchHistoricalKlines("15m")
    }

    private fun connectBinanceWebSocket() {
        // Canlı Emir Defteri ve Anlık Fiyat Yayınları
        val url = "wss://stream.binance.com:9443/stream?streams=btcusdt@depth20@100ms/btcusdt@ticker"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val stream = json.optString("stream")
                    val data = json.optJSONObject("data") ?: return

                    when (stream) {
                        "btcusdt@depth20@100ms" -> {
                            val bidsArray = data.optJSONArray("bids")
                            val asksArray = data.optJSONArray("asks")
                            val bids = mutableListOf<OrderBookItem>()
                            val asks = mutableListOf<OrderBookItem>()

                            for (i in 0 until (bidsArray?.length() ?: 0)) {
                                val bid = bidsArray!!.getJSONArray(i)
                                bids.add(OrderBookItem(bid.getString(0), bid.getString(1)))
                            }
                            for (i in 0 until (asksArray?.length() ?: 0)) {
                                val ask = asksArray!!.getJSONArray(i)
                                asks.add(OrderBookItem(ask.getString(0), ask.getString(1)))
                            }

                            _orderBookState.value = OrderBook(bids, asks)

                            // KANTİTATİF HESAPLAMA: Boğa vs Ayı Basıncı (Imbalance)
                            val bidVol = bids.take(15).sumOf { it.quantity.toDoubleOrNull() ?: 0.0 }
                            val askVol = asks.take(15).sumOf { it.quantity.toDoubleOrNull() ?: 0.0 }
                            val totalVol = bidVol + askVol
                            if (totalVol > 0) {
                                _imbalanceState.value = (bidVol - askVol) / totalVol
                            }
                        }
                        "btcusdt@ticker" -> {
                            _currentPrice.value = data.optString("c").toDoubleOrNull() ?: 0.0
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    private fun fetchHistoricalKlines(interval: String) {
        // Grafik için geçmiş mumları arka planda çek
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=$interval&limit=30")
                    .build()
                val response = client.newCall(request).execute()


                val responseBody = response.body()?.string()

                if (responseBody != null) {
                    val jsonArray = JSONArray(responseBody)
                    val candles = mutableListOf<Candle>()
                    for (i in 0 until jsonArray.length()) {
                        val kline = jsonArray.getJSONArray(i)
                        candles.add(
                            Candle(
                                open = kline.getString(1).toFloat(),
                                high = kline.getString(2).toFloat(),
                                low = kline.getString(3).toFloat(),
                                close = kline.getString(4).toFloat()
                            )
                        )
                    }
                    _klinesState.value = candles
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectTimeframe(tf: String) {
        _selectedTimeframe.value = tf
        fetchHistoricalKlines(tf) // Butona basılınca grafiği o dakikaya göre yeniden çek
    }

    override fun onCleared() {
        super.onCleared()
        webSocket?.close(1000, "ViewModel Kapatıldı")
    }
}

// --- PORTFÖY BEYNİ (Risk ve Varlık Ekranını Yönetir) ---
class PortfolioViewModel : ViewModel() {
    private val _portfolioState = MutableStateFlow<List<PortfolioAsset>>(emptyList())
    val portfolioState: StateFlow<List<PortfolioAsset>> = _portfolioState

    fun addAsset(symbol: String, amount: Double, price: Double, category: String) {
        val current = _portfolioState.value.toMutableList()
        current.add(PortfolioAsset(symbol, amount, price, category))
        _portfolioState.value = current
    }

    fun deleteAsset(asset: PortfolioAsset) {
        val current = _portfolioState.value.toMutableList()
        current.remove(asset)
        _portfolioState.value = current
    }
}