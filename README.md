# Requiem Finance Base

Requiem Finance Base is a minimalist market terminal developed for efficient tracking of primary financial assets. The project focuses on providing a centralized, low-latency dashboard for monitoring specific equity, stocks, cryptocurrency, and commodity markets.

## Purpose
The primary purpose of this terminal is to bring together real-time market data from multiple sources into a single, simplified interface. It is designed for active monitoring and pre-defined price alerts, eliminating the overhead and complexity associated with traditional financial platforms.

## Core Functionality
- **Data Integration:** Implements real-time data streaming from Binance and Yahoo Finance via Retrofit.
- **Automated Alerts:** Integrated push notification system triggered by predefined price levels.
- **Pre-Market Analysis:** Algorithmic calculation of US pre-market movements and percentage shifts.
- **Multi-Asset Monitoring:** Unified tracking for US technology equities (Mag-7), major cryptocurrencies, global indices, and commodities.

## Technical Specifications

- **Language & Environment:** Developed using Native Java (JDK 17+) within the Android Studio ecosystem, ensuring maximum compatibility with Android’s core system services.
- **Network Architecture:** Utilizes Retrofit 2 for type-safe HTTP networking. The implementation features asynchronous API calls to Binance and Yahoo Finance, managed via an optimized polling mechanism for near real-time data updates.
- **Data Parsing:** Integration of the GSON library for high-speed JSON serialization and deserialization, mapping complex financial data structures into lightweight Java objects.
- **UI/UX Framework:** Employs XML-based layouts with a custom "Dark Finance" theme. The UI layer is managed through an event-driven approach, using Handlers and Runnables to ensure the main thread remains non-blocking during data refresh cycles.
- **Notification Engine:** Leverages Android's `NotificationManager` and `NotificationCompat` for a robust local alerting system, capable of processing background price monitoring against user-defined thresholds.
- **Build System:** Managed with Gradle, incorporating dependency management and ProGuard rules for code optimization and security.

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
