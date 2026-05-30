package com.example.rartyfinancebase

import androidx.compose.ui.platform.ComposeView

object ComposeBridge {
    @JvmStatic
    fun setScreen(view: ComposeView, tabIndex: Int) {
        view.setContent {
            // Ana Compose ekranımızı çağırıyoruz
            RequiemQuantumApp(tabIndex = tabIndex)
        }
    }
}