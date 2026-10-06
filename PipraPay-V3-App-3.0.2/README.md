# PipraPay V3 Android Companion App 📱

Official Android SMS Gateway Companion App for **PipraPay V3** automated MFS & Bank payment verification.

---

## 🌟 Key Features

- **⚡ Instant SMS Interception:** Real-time capture of incoming payment SMS from **bKash, NAGAD, Rocket, Upay, Cellfin, IBBL, and other Bangladeshi Banks**.
- **📷 QR Code Pairing:** Fast one-tap pairing by scanning the QR code from PipraPay Admin Panel (**Devices** section).
- **📶 Dual SIM Support:** Automatic SIM Slot (SIM 1 / SIM 2) and carrier detection with individual SIM toggle controls.
- **🔄 Offline Queue & Smart Recovery:** Stores messages safely when mobile data/Wi-Fi is off and pushes them in batch via WorkManager once connectivity resumes.
- **📥 Historical SMS Import Tool:** Scan past payment SMS from the phone's inbox (24 hours, 3 days, 7 days) and push them in batch to PipraPay server.
- **🛡️ Permission & Health Check Widget:** Live dashboard status chips for SMS, Notifications, SIM State, and Battery Optimization whitelist.
- **📊 Daily & Weekly Volume Summary:** Real-time calculation of today's and this week's transaction count and total amount in BDT (৳).
- **🧪 In-App SMS Test Simulator:** Built-in test simulator with bKash/Nagad presets and random TrxID generation to verify backend matching.
- **🔊 Audio & Vibration Feedback:** Plays pleasant chime on payment match and provides haptic vibration.
- **🔋 Battery Friendly & Auto-Start:** Runs as an optimized sticky foreground service with auto-start on device reboot.

---

## 🚀 How to Install & Connect

1. Download the latest **`PipraPay-V3-Companion.apk`** from [Releases](https://github.com/samsusiyam/PipraPay-V3-App/releases).
2. Install the APK on your Android device (Android 7.0 to Android 14+ supported).
3. Open PipraPay V3 Admin Panel > **Devices** > **Add Device / View QR**.
4. Open the app, tap **"Scan QR Code"**, and point the camera at your Admin Panel screen.
5. Once connected, the dashboard will show **`● Active Gateway`** and begin real-time SMS sync!

---

## 🛠️ Tech Stack & Architecture

- **Language:** Java
- **Target SDK:** Android 14 (API 34), Min SDK: Android 6.0 (API 23)
- **Networking:** Google Volley with custom TLS Socket Factory & Exponential Backoff Retry Policy
- **Background Tasks:** Android Jetpack WorkManager (`SmsSyncWorker`) & Foreground Service (`SmsReceiverService`)
- **Local Storage:** SQLite Database (`SmsLogDatabase`) + Encrypted SharedPreferences (`PrefManager`)
- **QR Scanning:** ZXing Embedded Barcode Scanner

---

## 📄 License
Released under the [MIT License](LICENSE).