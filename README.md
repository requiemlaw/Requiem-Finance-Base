# Requiem Finance Base

![Version](https://img.shields.io/badge/version-v2.0--release-brightgreen)
![Platform](https://img.shields.io/badge/platform-Android-blue)
![Java](https://img.shields.io/badge/Java-75.4%25-b07219?style=flat&logo=java&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-24.6%25-A97BFF?style=flat&logo=kotlin&logoColor=white)


## Overview
Requiem Finance Base has evolved into a zero-latency, institutional-grade financial terminal. Moving beyond simple asset tracking, the project now focuses on deep structural market analysis, raw order flow mechanics, and a hybrid UI architecture to deliver a centralized, high-performance dashboard for monitoring global equities, cryptocurrencies, and commodities.

## Core Functionality
* **Native Order Flow & Market Depth:** Custom `Canvas`-based rendering engine that calculates cumulative volume in real-time, displaying structural Bid/Ask walls to track institutional market positioning.
* **The Imbalance Matrix:** A real-time, dynamically animated gauge tracking the micro-structure of the market, instantly calculating the gravitational pull between Bullish and Bearish pressure.
* **Context-Aware Aesthetics:** Intelligent color-shift logic that automatically toggles asset typography and structural highlights between high-contrast daylight colors and a monolithic Deep Black (`#0B0E11`) night mode to eliminate eye fatigue.
* **Dynamic Category Management:** Fully customizable category headers. Users can rename, reorder, and restructure entire asset blocks via a real-time drag-and-drop interface.
* **S&P 500 Integration:** Access a full trading universe with a search pool covering the top 500 US equities by market cap.
* **Quant-Focused UI:** Optimized "Amnesia Mode" for the search engine, disabling keyboard suggestions and history for a cleaner, high-speed data-entry experience.
* **Automated Alerts:** Integrated push notification system triggered by predefined price targets.
* **Pre-Market Analysis:** Algorithmic calculation of US pre-market movements and percentage shifts.

## Technical Specifications
* **Language & Environment:** Hybrid development environment utilizing Native Java (JDK 17+) and Kotlin, ensuring maximum compatibility and seamless interoperability within the Android ecosystem.
* **UI/UX Architecture (Interoperability):** Optimized Android UI architecture by seamlessly bridging legacy XML layouts with modern Jetpack Compose environments using `ComposeView`. This dual-engine approach guarantees maximum rendering performance for complex, custom `Canvas` graphics without a single frame of lag.
* **Network Architecture:** Utilizes Retrofit 2 for type-safe HTTP networking. Operations run completely asynchronous via Coroutines and Handlers, ensuring the UI thread remains entirely unblocked during heavy, concurrent data pulls from Yahoo Finance and Binance APIs.
* **System UI Integration:** Complete eradication of Android's default `elevation overlay` fog and forced drop-shadows. Utilizes programmatic `WindowInsetsController` hooks to enforce strict, system-wide aesthetic compliance with the terminal's palette.
* **Data Parsing:** Integration of the GSON library for high-speed JSON serialization and deserialization, mapping complex financial data structures into lightweight objects.
* **Notification Engine:** Leverages Android's `NotificationManager` and `NotificationCompat` for a robust local alerting system, capable of processing background price monitoring against user-defined thresholds.

---

### 📱 Terminal Interface Preview

#### 🌙 Night Operations
<p align="center">
  <img src="https://github.com/requiemlaw/Requiem-Finance-Base/blob/73317729c2d949c594d6fe56c0faff36a11c39f5/app/src/test/java/com/example/rartyfinancebase/RequiemTerminalV2.0-RELEASE.png" width="30%" alt="Night Main View"/>
  &nbsp; &nbsp;
  <img src="https://github.com/requiemlaw/Requiem-Finance-Base/blob/bd7e195220516f05898d5a49cfd9b15899e52504/app/src/test/java/com/example/rartyfinancebase/RequiemTerminalV2.0-RELEASE1.png" width="30%" alt="Night Order Book & Depth"/>
  &nbsp; &nbsp;
  <img src="https://github.com/requiemlaw/Requiem-Finance-Base/blob/bd7e195220516f05898d5a49cfd9b15899e52504/app/src/test/java/com/example/rartyfinancebase/RequiemTerminalV2.0-RELEASE2.png" width="30%" alt="Night Portfolio Engine"/>
</p>

#### ☀️ Day Operations
<p align="center">
  <img src="https://github.com/requiemlaw/Requiem-Finance-Base/blob/bd7e195220516f05898d5a49cfd9b15899e52504/app/src/test/java/com/example/rartyfinancebase/RequiemTerminalV2.0-RELEASE3.png" width="30%" alt="Day Main View"/>
  &nbsp; &nbsp;
  <img src="https://github.com/requiemlaw/Requiem-Finance-Base/blob/bd7e195220516f05898d5a49cfd9b15899e52504/app/src/test/java/com/example/rartyfinancebase/RequiemTerminalV2.0-RELEASE4.png" width="30%" alt="Day Order Book & Depth"/>
  &nbsp; &nbsp;
  <img src="https://github.com/requiemlaw/Requiem-Finance-Base/blob/bd7e195220516f05898d5a49cfd9b15899e52504/app/src/test/java/com/example/rartyfinancebase/RequiemTerminalV2.0-RELEASE5.png" width="30%" alt="Day Portfolio Engine"/>
</p>

### 📥 Download & Try it Out

Experience the stable terminal environment directly on your Android device.

[![Download APK](https://img.shields.io/badge/Download_APK-v2.0.0_Release-10b981?style=for-the-badge&logo=android)](https://github.com/requiemlaw/Requiem-Finance-Base/releases/tag/v2.0.0)

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
