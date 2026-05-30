package com.example.rartyfinancebase

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun RequiemQuantumApp(tabIndex: Int) {
    val bgColor = Color(0xFF0B0E11)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.TopStart
    ) {
        when (tabIndex) {
            0 -> {
                // 1. SEKME: DERİNLİK (ORDER BOOK)
                OrderBookScreen()
            }
            1 -> {
                // 2. SEKME: PORTFÖY
                PortfolioScreen()
            }
        }
    }
}