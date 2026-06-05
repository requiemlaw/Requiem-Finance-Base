package com.example.rartyfinancebase

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private val binanceApi = Retrofit.Builder().baseUrl("https://api.binance.com/").addConverterFactory(GsonConverterFactory.create()).build().create(BinanceApi::class.java)
private val yahooApi = Retrofit.Builder().baseUrl("https://query1.finance.yahoo.com/").addConverterFactory(GsonConverterFactory.create()).build().create(YahooApi::class.java)

private val bgColor = Color(0xFF0B0E11)
private val surfaceColor = Color(0xFF161A1E)
private val goldAccent = Color(0xFFF0B90B)
private val textPrimary = Color.White
private val textSecondary = Color(0xFF848E9C)
private val profitColor = Color(0xFF0ECB81)
private val lossColor = Color(0xFFF6465D)

private data class PredefinedAsset(val symbol: String, val displayName: String, val category: String)

private val ASSET_POOL = listOf(
    // Hisseler
    PredefinedAsset("AAPL", "Apple Inc.", "Hisse"),
    PredefinedAsset("MSFT", "Microsoft Corp.", "Hisse"),
    PredefinedAsset("NVDA", "NVIDIA Corp.", "Hisse"),
    PredefinedAsset("TSLA", "Tesla Inc.", "Hisse"),
    PredefinedAsset("GOOGL", "Alphabet Inc.", "Hisse"),
    PredefinedAsset("AMZN", "Amazon.com Inc.", "Hisse"),
    PredefinedAsset("META", "Meta Platforms", "Hisse"),
    PredefinedAsset("AVGO", "Broadcom Inc.", "Hisse"),
    PredefinedAsset("BRK-B", "Berkshire Hathaway", "Hisse"),
    PredefinedAsset("LLY", "Eli Lilly and Co.", "Hisse"),
    PredefinedAsset("JPM", "JPMorgan Chase", "Hisse"),
    PredefinedAsset("V", "Visa Inc.", "Hisse"),
    PredefinedAsset("MA", "Mastercard Inc.", "Hisse"),
    PredefinedAsset("PG", "Procter & Gamble", "Hisse"),
    PredefinedAsset("JNJ", "Johnson & Johnson", "Hisse"),
    PredefinedAsset("XOM", "Exxon Mobil", "Hisse"),
    PredefinedAsset("WMT", "Walmart Inc.", "Hisse"),
    PredefinedAsset("UNH", "UnitedHealth Group", "Hisse"),
    PredefinedAsset("HD", "Home Depot", "Hisse"),
    PredefinedAsset("BAC", "Bank of America", "Hisse"),
    PredefinedAsset("PFE", "Pfizer Inc.", "Hisse"),
    PredefinedAsset("KO", "Coca-Cola Co.", "Hisse"),
    PredefinedAsset("DIS", "Walt Disney Co.", "Hisse"),
    PredefinedAsset("CSCO", "Cisco Systems", "Hisse"),
    PredefinedAsset("PEP", "PepsiCo Inc.", "Hisse"),
    PredefinedAsset("NFLX", "Netflix Inc.", "Hisse"),
    PredefinedAsset("INTC", "Intel Corp.", "Hisse"),
    PredefinedAsset("AMD", "Advanced Micro Devices", "Hisse"),
    PredefinedAsset("THYAO.IS", "Türk Hava Yolları", "Hisse"),
    PredefinedAsset("TUPRS.IS", "Tüpraş", "Hisse"),
    PredefinedAsset("KCHOL.IS", "Koç Holding", "Hisse"),
    PredefinedAsset("ISCTR.IS", "İş Bankası (C)", "Hisse"),
    PredefinedAsset("EREGL.IS", "Erdemir", "Hisse"),

    // Kripto
    PredefinedAsset("BTC", "Bitcoin", "Kripto"),
    PredefinedAsset("ETH", "Ethereum", "Kripto"),
    PredefinedAsset("BNB", "Binance Coin", "Kripto"),
    PredefinedAsset("SOL", "Solana", "Kripto"),
    PredefinedAsset("XRP", "Ripple", "Kripto"),

    // Emtia
    PredefinedAsset("GC=F", "Altın (Ons)", "Emtia"),
    PredefinedAsset("SI=F", "Gümüş", "Emtia"),
    PredefinedAsset("HG=F", "Bakır", "Emtia"),
    PredefinedAsset("PA=F", "Paladyum", "Emtia"),

    // Endeks
    PredefinedAsset("XU100.IS", "BIST 100", "Endeks"),
    PredefinedAsset("^GSPC", "S&P 500", "Endeks"),
    PredefinedAsset("^DJI", "Dow Jones 30", "Endeks"),
    PredefinedAsset("^IXIC", "NASDAQ Composite", "Endeks")
)

@Composable
fun PortfolioScreen(viewModel: PortfolioViewModel = viewModel()) {
    val portfolioList by viewModel.portfolioState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val livePrices = remember { mutableStateMapOf<String, Double>() }

    LaunchedEffect(portfolioList) {
        portfolioList.forEach { asset ->
            if (!livePrices.containsKey(asset.symbol)) {
                fetchLivePrice(asset.symbol, asset.category) { price ->
                    livePrices[asset.symbol] = price
                }
            }
        }
    }

    // ── DİNAMİK PARASAL AĞIRLIK VE RİSK HESAPLAMASI ──
    var cryptoValue = 0.0
    var safeValue = 0.0 // Emtia + Endeks
    var stockValue = 0.0

    val liveTotalValue = portfolioList.sumOf { asset ->
        val currentPrice = livePrices[asset.symbol] ?: asset.buyPrice
        val itemValue = asset.amount * currentPrice

        // Hangi varlığa ne kadar dolar bağladığını hesaplıyoruz
        when (asset.category.lowercase()) {
            "kripto" -> cryptoValue += itemValue
            "emtia", "endeks" -> safeValue += itemValue
            "hisse" -> stockValue += itemValue
        }
        itemValue
    }

    val totalCost = portfolioList.sumOf { it.amount * it.buyPrice }
    val totalPnL = liveTotalValue - totalCost
    val totalPnLPercent = if (totalCost > 0) (totalPnL / totalCost) * 100 else 0.0

    // YENİ KURALLAR: Kripto riskli, Emtia/Endeks risksiz liman.
    val (riskText, riskColor) = when {
        portfolioList.isEmpty() || liveTotalValue == 0.0 -> Pair("Belirsiz (Veri Yok)", textSecondary)
        (cryptoValue / liveTotalValue) >= 0.4 -> Pair("YÜKSEK (Kripto Ağırlıklı)", lossColor) // Paranın %40'ı veya fazlası kriptoysa
        (safeValue / liveTotalValue) >= 0.5 -> Pair("DÜŞÜK (Güvenli Liman)", profitColor) // Paranın yarısı emtia/endeksteyse
        else -> Pair("ORTA (Dengeli / Hisse Ağırlıklı)", goldAccent) // Geriye kalan senaryolar
    }

    Scaffold(
        containerColor = bgColor,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = goldAccent,
                contentColor = bgColor
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Varlık Ekle")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Varlıklar & Portföy",
                color = textPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Canlı Toplam Büyüklük", color = textSecondary, fontSize = 14.sp)
                    Text(
                        text = "$${String.format("%.2f", liveTotalValue)}",
                        color = textPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (portfolioList.isNotEmpty()) {
                        val pnlSign = if (totalPnL >= 0) "+" else ""
                        val pnlColor = if (totalPnL >= 0) profitColor else lossColor
                        Text(
                            text = "$pnlSign$${String.format("%.2f", totalPnL)} ($pnlSign${String.format("%.2f", totalPnLPercent)}%)",
                            color = pnlColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Risk Profili: ", color = textSecondary, fontSize = 14.sp)
                        Text(riskText, color = riskColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (portfolioList.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Sistemde aktif varlık bulunmuyor.", color = textSecondary)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(portfolioList) { asset ->
                        val currentPrice = livePrices[asset.symbol] ?: asset.buyPrice
                        AssetCard(
                            asset = asset,
                            currentPrice = currentPrice,
                            onDelete = {
                                viewModel.deleteAsset(asset)
                                livePrices.remove(asset.symbol)
                            }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddAssetDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { symbol, amount, price, category ->
                    viewModel.addAsset(symbol.uppercase(), amount, price, category)
                    fetchLivePrice(symbol.uppercase(), category) { livePrice ->
                        livePrices[symbol.uppercase()] = livePrice
                    }
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AssetCard(
    asset: PortfolioAsset,
    currentPrice: Double,
    onDelete: () -> Unit
) {
    val totalCost = asset.amount * asset.buyPrice
    val currentValue = asset.amount * currentPrice
    val pnl = currentValue - totalCost
    val pnlPercent = if (totalCost > 0) (pnl / totalCost) * 100 else 0.0

    val pnlColor = if (pnl >= 0) profitColor else lossColor
    val pnlSign = if (pnl >= 0) "+" else ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = asset.symbol, color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = asset.category, color = textSecondary, fontSize = 12.sp, modifier = Modifier.border(1.dp, textSecondary, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Miktar: ${asset.amount}  |  Maliyet: $${asset.buyPrice}", color = textSecondary, fontSize = 13.sp)
                Text(text = "Anlık Fiyat: $${String.format("%.2f", currentPrice)}", color = textPrimary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "$${String.format("%.2f", currentValue)} ", color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = "($pnlSign$${String.format("%.2f", pnl)} | $pnlSign${String.format("%.2f", pnlPercent)}%)", color = pnlColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Sil", tint = lossColor)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAssetDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, String) -> Unit
) {
    var symbol by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Hisse") }
    var expanded by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val categories = listOf("Hisse", "Kripto", "Emtia", "Endeks")

    val filteredPool = ASSET_POOL.filter {
        it.category.equals(category, ignoreCase = true) &&
                (it.symbol.contains(symbol, ignoreCase = true) || it.displayName.contains(symbol, ignoreCase = true))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        title = { Text("Yeni Varlık Girişi", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Kategori Sınıfı", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    categories.forEach { cat ->
                        val isSelected = category == cat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(1.dp)
                                .border(1.dp, if (isSelected) goldAccent else Color.DarkGray, RoundedCornerShape(8.dp))
                                .background(if (isSelected) goldAccent.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable {
                                    category = cat
                                    symbol = ""
                                    focusManager.clearFocus()
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cat, color = if (isSelected) goldAccent else Color.Gray, fontSize = 12.sp, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { symbol = it; expanded = true },
                        label = { Text("Sembol Seçin veya Yazın", color = Color.Gray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = goldAccent)
                    )

                    if (filteredPool.isNotEmpty() && expanded) {
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier
                                .background(surfaceColor)
                                .heightIn(max = 220.dp)
                        ) {
                            filteredPool.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(item.symbol, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text(item.displayName, color = Color.Gray, fontSize = 11.sp)
                                        }
                                    },
                                    onClick = {
                                        symbol = item.symbol
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Miktar", color = Color.Gray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = goldAccent),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Maliyet", color = Color.Gray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = goldAccent),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                    val parsedPrice = price.toDoubleOrNull() ?: 0.0
                    if (symbol.isNotBlank()) onConfirm(symbol, parsedAmount, parsedPrice, category)
                },
                colors = ButtonDefaults.buttonColors(containerColor = goldAccent)
            ) {
                Text("Sisteme Ekle", color = bgColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = Color.Gray) }
        }
    )
}

private fun fetchLivePrice(symbol: String, category: String, onResult: (Double) -> Unit) {
    if (category.equals("Kripto", ignoreCase = true)) {
        val bSymbol = if (symbol.endsWith("USDT")) symbol else "${symbol}USDT"
        binanceApi.getKlines(bSymbol, "1m", 1).enqueue(object : Callback<List<List<Any>>> {
            override fun onResponse(call: Call<List<List<Any>>>, response: Response<List<List<Any>>>) {
                try {
                    val lastClose = response.body()?.lastOrNull()?.get(4).toString().toDouble()
                    onResult(lastClose)
                } catch (e: Exception) { }
            }
            override fun onFailure(call: Call<List<List<Any>>>, t: Throwable) {}
        })
    } else {
        yahooApi.getChartData(symbol, "1m", "1d").enqueue(object : Callback<YahooFinanceResponse> {
            override fun onResponse(call: Call<YahooFinanceResponse>, response: Response<YahooFinanceResponse>) {
                try {
                    val closes = response.body()?.chart?.result?.firstOrNull()?.indicators?.quote?.firstOrNull()?.close
                    val lastClose = closes?.lastOrNull { it != null } ?: return
                    onResult(lastClose)
                } catch (e: Exception) { }
            }
            override fun onFailure(call: Call<YahooFinanceResponse>, t: Throwable) {}
        })
    }
}