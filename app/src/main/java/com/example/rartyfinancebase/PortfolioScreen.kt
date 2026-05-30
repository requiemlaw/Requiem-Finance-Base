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

@Composable
fun PortfolioScreen(viewModel: PortfolioViewModel = viewModel()) {
    val portfolioList by viewModel.portfolioState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val bgColor = Color(0xFF0B0E11)
    val surfaceColor = Color(0xFF161A1E)
    val goldAccent = Color(0xFFF0B90B)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF848E9C)

    val totalPortfolioValue = portfolioList.sumOf { it.amount * it.buyPrice }
    val cryptoCount = portfolioList.count { it.category.equals("Kripto", ignoreCase = true) }
    val cryptoRatio = if (portfolioList.isNotEmpty()) cryptoCount.toDouble() / portfolioList.size else 0.0

    val (riskText, riskColor) = when {
        portfolioList.isEmpty() -> Pair("Belirsiz (Veri Yok)", textSecondary)
        cryptoRatio > 0.5 -> Pair("YÜKSEK (Volatilite Uyarısı)", Color(0xFFF6465D))
        cryptoRatio > 0.2 -> Pair("ORTA (Dengeli Dağılım)", goldAccent)
        else -> Pair("DÜŞÜK (Stabil Portföy)", Color(0xFF0ECB81))
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
                    Text("Toplam Büyüklük", color = textSecondary, fontSize = 14.sp)
                    Text(
                        text = "$${String.format("%.2f", totalPortfolioValue)}",
                        color = goldAccent,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                        AssetCard(
                            asset = asset,
                            onDelete = { viewModel.deleteAsset(asset) },
                            surfaceColor = surfaceColor,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            goldAccent = goldAccent
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddAssetDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { symbol, amount, price, category ->
                    viewModel.addAsset(symbol, amount, price, category)
                    showAddDialog = false
                },
                surfaceColor = surfaceColor,
                goldAccent = goldAccent
            )
        }
    }
}

@Composable
fun AssetCard(
    asset: PortfolioAsset,
    onDelete: () -> Unit,
    surfaceColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    goldAccent: Color
) {
    val itemRiskColor = when {
        asset.category.equals("Kripto", ignoreCase = true) -> Color(0xFFF6465D)
        asset.category.equals("Hisse", ignoreCase = true) -> goldAccent
        else -> Color(0xFF0ECB81)
    }

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
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(itemRiskColor, RoundedCornerShape(50)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = asset.symbol, color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "[${asset.category}]", color = textSecondary, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Miktar: ${asset.amount}  |  Maliyet: $${asset.buyPrice}", color = textSecondary, fontSize = 14.sp)
                Text(
                    text = "Toplam: $${String.format("%.2f", asset.amount * asset.buyPrice)}",
                    color = goldAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Sil", tint = Color(0xFFF6465D))
            }
        }
    }
}

@Composable
fun AddAssetDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, String) -> Unit,
    surfaceColor: Color,
    goldAccent: Color
) {
    var symbol by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Hisse") }

    val focusManager = LocalFocusManager.current
    val categories = listOf("Hisse", "Kripto", "Emtia")

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
                                .padding(2.dp)
                                .border(1.dp, if (isSelected) goldAccent else Color.DarkGray, RoundedCornerShape(8.dp))
                                .background(if (isSelected) goldAccent.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { category = cat; focusManager.clearFocus() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cat, color = if (isSelected) goldAccent else Color.Gray, fontSize = 13.sp, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("Sembol (Örn: THYAO)", color = Color.Gray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        autoCorrect = false,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = goldAccent
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Miktar", color = Color.Gray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        autoCorrect = false,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = goldAccent
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Maliyet", color = Color.Gray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        autoCorrect = false,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = goldAccent
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                    val parsedPrice = price.toDoubleOrNull() ?: 0.0
                    if (symbol.isNotBlank()) {
                        onConfirm(symbol, parsedAmount, parsedPrice, category)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = goldAccent)
            ) {
                Text("Sisteme Ekle", color = Color(0xFF0B0E11), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal", color = Color.Gray)
            }
        }
    )
}