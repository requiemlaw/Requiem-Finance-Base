# Requiem Finance Base

![Version](https://img.shields.io/badge/version-v1.0.0--stable-brightgreen)
![Platform](https://img.shields.io/badge/platform-Android-blue)

## Overview

Requiem Finance Base is a minimalist market terminal developed for efficient tracking of primary financial assets. The project focuses on providing a centralized, low-latency dashboard for monitoring specific equity, stocks, cryptocurrency, and commodity markets.

## Core Functionality
* **S&P 500 Integration**: Access a full trading universe with a search pool covering the top 500 US equities by market cap.
* **Dynamic Category Management**: Fully customizable category headers. Users can now rename and reorder entire asset blocks (e.g., MAG-7, Crypto, Forex) via a drag-and-drop interface.
* **Hierarchical Asset Editing**: Drill down into specific categories to sort individual tickers or remove assets from your dashboard.
* **Quant-Focused UI**: Optimized "Amnesia Mode" for the search engine, disabling keyboard suggestions and history for a cleaner data-entry experience.
* **Minimalist Aesthetics**: Revamped UI icons (edit/delete/drag) featuring a sharp, monochromatic design to align with professional terminal standards.
* **Data Integration:** Implements real-time data streaming from Binance and Yahoo Finance via Retrofit.
* **Automated Alerts:** Integrated push notification system triggered by predefined price levels.
* **Pre-Market Analysis:** Algorithmic calculation of US pre-market movements and percentage shifts.
* **Multi-Asset Monitoring:** Unified tracking for US technology equities (Mag-7), major cryptocurrencies, global indices, and commodities.

## Technical Specifications

* **Language & Environment:** Developed using Native Java (JDK 17+) within the Android Studio ecosystem, ensuring maximum compatibility with Android’s core system services.
* **Network Architecture:** Utilizes Retrofit 2 for type-safe HTTP networking. The implementation features asynchronous API calls to Binance and Yahoo Finance, managed via an optimized polling mechanism for near real-time data updates.
* **Data Parsing:** Integration of the GSON library for high-speed JSON serialization and deserialization, mapping complex financial data structures into lightweight Java objects.
* **UI/UX Framework:** Employs XML-based layouts with a custom "Dark Finance" theme. The UI layer is managed through an event-driven approach, using Handlers and Runnables to ensure the main thread remains non-blocking during data refresh cycles.
* **Notification Engine:** Leverages Android's `NotificationManager` and `NotificationCompat` for a robust local alerting system, capable of processing background price monitoring against user-defined thresholds.
* **Build System:** Managed with Gradle, incorporating dependency management and ProGuard rules for code optimization and security.

---

### 📱 Terminal Interface Preview

<p align="center">
  <img src="https://github.com/requiemlaw/Requiem-Finance-Base/blob/dbd401eb31558e6da7dccdb0a1ad7b2dcea64099/app/src/test/java/com/example/rartyfinancebase/RequiemTerminal1.png?raw=true" width="30%" />
  &nbsp; &nbsp; &nbsp;
  <img src="https://github.com/requiemlaw/Requiem-Finance-Base/blob/dbd401eb31558e6da7dccdb0a1ad7b2dcea64099/app/src/test/java/com/example/rartyfinancebase/RequiemTerminal2.png?raw=true" width="30%" />
</p>

### 📥 Download & Try it Out

Experience the stable terminal environment directly on your Android device.

[![Download APK](https://img.shields.io/badge/Download_APK-v1.0.0_Stable-10b981?style=for-the-badge&logo=android)](https://github.com/requiemlaw/Requiem-Finance-Base/releases/tag/v1.0.0)

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
