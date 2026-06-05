# Requiem Finance Base

![Version](https://img.shields.io/badge/version-v3.0.0--release-white)
![Platform](https://img.shields.io/badge/platform-Android-blue)
![Java](https://img.shields.io/badge/Java-56.9%25-b07219?style=flat&logo=java&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-43.1%25-A97BFF?style=flat&logo=kotlin&logoColor=white)


## Overview
Requiem Terminal v3.0 (Quantum Edition) is a clinical, zero-latency financial terminal designed for quantitative analysis and high-stakes portfolio management. Evolving from a simple tracking tool into an institutional-grade ecosystem, this release integrates deep-market intelligence, real-time PnL (Profit & Loss) tracking, and a bespoke rendering architecture to dissect the structure of the market itself. By bridging the gap between raw API data and actionable portfolio intelligence, Requiem Terminal offers a complete analytical environment for equities, cryptocurrencies, commodities, and global indices.

## Core Functionality
* **Real-Time PnL Engine:** Instant dollar and percentage-based performance monitoring. Every asset dynamically tracks its cost-basis against real-time price feeds.
* **Canvas-Based Technical Suite:** A bespoke, non-library rendering core plotting RSI, MACD, Bollinger Bands, and cumulative volume profiles with zero-latency synchronization.
* **Smart Asset Synthesis:** An intelligent, category-aware symbol acquisition engine with high-performance autocomplete filtering across S&P 500, Crypto, Commodities, and Indices.
* **Institutional Risk Profiling:** Advanced capital-weighting risk algorithm that dynamically categorizes portfolio health (Low/Medium/High) based on dollar-weighted exposure.
* **Persistent Workspace Logic:** A robust `SharedPreferences` state-binding layer that preserves all user-defined indicator configurations, volume toggles, and UI states across sessions.
* **Auto-Lock-to-Latest Charting:** Advanced `Canvas` lifecycle management that enforces a hard-snap to the most recent price action, eliminating manual scrolling friction.
* **Context-Aware Aesthetics:** Monochromatic, eye-friendly design system utilizing `#0B0E11` deep black canvas and high-contrast highlights for professional daylight/night-mode transitions.
* **Custom Order Flow:** Native `Canvas` rendering for Market Depth, mapping cumulative volume and structural bid/ask pressure without relying on third-party charting libraries.

## Technical Specifications
* **Language & Architecture:** Hybrid development environment utilizing Native Java (JDK 17+) and Kotlin. Optimized Android UI architecture bridging legacy XML layouts with modern Jetpack Compose environments using `ComposeView`.
* **Rendering Engine:** Bespoke `Canvas`-based plotting engine built from the ground up for high-frequency market data visualization, ensuring 100% frame-rate stability.
* **Network & Data Layer:** Asynchronous data synchronization via Retrofit 2 and Kotlin Coroutines, ensuring the Main UI thread remains strictly unblocked during concurrent data stream processing from Binance and Yahoo Finance APIs.
* **Persistence Layer:** Integrated state management using `SharedPreferences`, enabling persistent caching of complex indicator hierarchies and visual preferences.
* **High-Speed Serialization:** Integration of the GSON library for high-speed, type-safe JSON serialization/deserialization of complex financial data objects.
* **System Compliance:** Programmatic `WindowInsetsController` hooks for strict, system-wide aesthetic enforcement of the terminal’s visual palette, bypassing standard OS-level drop-shadows and elevation overlays.

---

### 📱 Terminal Interface Preview

#### 🌙 Night Operations
<p align="center">
  <img src="https://github.com/requiemlaw/Requiem-Finance-Base/blob/bb7235145ea5d093fdcea29e0b982caf29e2ddd9/app/src/test/java/com/example/rartyfinancebase/RequiemTerminalv3photo1.png" width="30%"/> &nbsp; &nbsp;
  <img src="https://github.com/requiemlaw/Requiem-Finance-Base/blob/bb7235145ea5d093fdcea29e0b982caf29e2ddd9/app/src/test/java/com/example/rartyfinancebase/RequiemTerminalv3photo2.png" width="30%"/> &nbsp; &nbsp;
  <img src="https://github.com/requiemlaw/Requiem-Finance-Base/blob/4ab5eabd32bf321a23c136cd026fc86098f69af3/app/src/test/java/com/example/rartyfinancebase/RequiemTerminalv3photo8.png" width="30%"/>
</p>

#### 📈 Portfolio Management & Analytics
<p align="center">
  <img src="https://github.com/requiemlaw/Requiem-Finance-Base/blob/bb7235145ea5d093fdcea29e0b982caf29e2ddd9/app/src/test/java/com/example/rartyfinancebase/RequiemTerminalv3photo5.png" width="30%"/> &nbsp; &nbsp;
  <img src="https://github.com/requiemlaw/Requiem-Finance-Base/blob/bb7235145ea5d093fdcea29e0b982caf29e2ddd9/app/src/test/java/com/example/rartyfinancebase/RequiemTerminalv3photo4.png" width="30%"/> &nbsp; &nbsp;
  <img src="https://github.com/requiemlaw/Requiem-Finance-Base/blob/bb7235145ea5d093fdcea29e0b982caf29e2ddd9/app/src/test/java/com/example/rartyfinancebase/RequiemTerminalv3photo3.png" width="30%"/>
</p>

#### ☀️ Day Operations
<p align="center">
  <img src="https://github.com/requiemlaw/Requiem-Finance-Base/blob/bb7235145ea5d093fdcea29e0b982caf29e2ddd9/app/src/test/java/com/example/rartyfinancebase/RequiemTerminalv3photo6.png" width="30%"/> &nbsp; &nbsp;
  <img src="https://github.com/requiemlaw/Requiem-Finance-Base/blob/bb7235145ea5d093fdcea29e0b982caf29e2ddd9/app/src/test/java/com/example/rartyfinancebase/RequiemTerminalv3photo7.png" width="30%"/> &nbsp; &nbsp;
  <img src="https://github.com/requiemlaw/Requiem-Finance-Base/blob/bd7e195220516f05898d5a49cfd9b15899e52504/app/src/test/java/com/example/rartyfinancebase/RequiemTerminalV2.0-RELEASE4.png" width="30%"/>
</p>

### 📥 Download & Try it Out

Experience the stable terminal environment directly on your Android device.

[![Download APK](https://img.shields.io/badge/Download_APK-v3.0.0_Release-10b981?style=for-the-badge&logo=android)](https://github.com/requiemlaw/Requiem-Finance-Base/releases/tag/v3.0.0)

---

## Legal and Usage Notice
This software and its source code are the sole intellectual property of the author. Access to this repository is granted for professional review and evaluation purposes only. Unauthorized reproduction, distribution, or commercial use is strictly prohibited.

---
COPYRIGHT AND PROTECTIVE NOTICE
--------------------------------------------------
Copyright (C) 2026 RequiemLaw. All Rights Reserved.

PROJECT: Requiem-Finance-Base

1. This software and its source code are the sole intellectual property of 
   the author. No part of this project may be reproduced, distributed, 
   or transmitted in any form or by any means, including photocopying, 
   recording, or other electronic or mechanical methods, without the 
   prior written permission of the copyright holder.

2. Access to this repository is granted for review and evaluation purposes 
   only (e.g., academic or recruitment review). Any commercial use, 
   redistribution, or unauthorized modification is strictly prohibited.

3. Failure to comply with these terms may result in legal action.

"RequiemLaw."
--------------------------------------------------
