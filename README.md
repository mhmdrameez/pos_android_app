# ⚡ QuickBill POS — Android App

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack_Compose-Material3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Room](https://img.shields.io/badge/Room-2.7.1-FF6F00?style=for-the-badge&logo=sqlite&logoColor=white)
![MinSDK](https://img.shields.io/badge/Min_SDK-24_(Android_7)-success?style=for-the-badge)

> A fast, **offline-first** Point-of-Sale app for Android — mirroring the browser-based QuickBill POS with Bluetooth thermal printing, smart price-bucket suggestions, and a full offline SQLite database. Built entirely with **Kotlin + Jetpack Compose**.

---

## ✨ Features

### 🧾 Quick Sale Screen
- Large **numeric keypad** (7-8-9 / 4-5-6 / 1-2-3 / 0-00-. layout matching web version)
- **`×` multiply key** — type `50x3` → shows ₹50 × 3 = ₹150, adds 3 qty
- Real-time **amount display** with rupee prefix
- **Add Item** button with optional label dialog (skip to add unlabelled)
- **Swipe-to-delete** cart rows + **+/− quantity stepper**
- Live **Grand Total** + item count footer
- **Print** + **Bill** buttons at the bottom

### 💡 Smart Suggestions
- **Price-bucket algorithm**: shows products within ±30% of entered price
- **Weighted ranking**: 40% price proximity + 40% usage frequency + 20% recency
- **Auto-learns** from every sale (creates/increments product entries automatically)
- **25 pre-seeded** common Indian retail items (Tea ₹10, Milk ₹28, Bread ₹40…)
- Toggle On/Off from the top bar chip
- Horizontally scrollable animated chip row

### 💳 Checkout (Bottom Sheet)
- **Cash** — tendered amount, quick-fill buttons (nearest ₹10/₹50/₹100), live change calc
- **UPI** — records payment method
- **Card** — records payment method
- Sale + items saved to Room DB on confirm

### 🖨️ Bluetooth Thermal Printing (ESC/POS)
- Any paired Bluetooth ESC/POS printer (58mm or 80mm)
- Formatted receipt: shop header, item table, GST split, total, payment, change, footer
- Powered by [DantSu/ESCPOS-ThermalPrinter-Android](https://github.com/DantSu/ESCPOS-ThermalPrinter-Android)
- Saved printer MAC → auto-reconnects on each print
- **Test Print** from Printer Settings tab

### 📊 History
- Sales log (last 100, newest first)
- **Today's revenue card** + today's sale count
- Expandable rows — tap to see item breakdown + change given
- Delete individual sales

### ⚙️ Settings (DataStore)
- Shop name, address, phone — printed on every receipt
- UPI ID
- Tax % (0 = no tax; auto-calculates GST split on receipts)
- Receipt footer message
- All persisted via DataStore Preferences

---

## 🏗️ Architecture

```
com.example.quickbillposs/
│
├── MainActivity.kt              # Compose host + bottom NavHost
├── SuggestionEngine.kt          # Price-bucket + frequency scoring
├── PrinterHelper.kt             # Bluetooth ESC/POS printing
│
├── data/
│   ├── model/
│   │   ├── Sale.kt              # @Entity — completed sales
│   │   ├── SaleItem.kt          # @Entity — line items per sale (FK → Sale)
│   │   └── Product.kt           # @Entity — product catalog + CartItem data class
│   ├── dao/
│   │   ├── SaleDao.kt           # Insert, query, delete, daily totals (Flow)
│   │   └── ProductDao.kt        # Price-range queries, frequency upsert
│   ├── AppDatabase.kt           # Room singleton + 25 seed products on first run
│   └── PreferencesManager.kt    # DataStore — shop settings + printer MAC
│
├── viewmodel/
│   ├── SalesViewModel.kt        # Cart state, keypad logic, suggestions, checkout, print
│   └── HistoryViewModel.kt      # Sale history + daily revenue (Flow)
│
└── ui/
    ├── theme/
    │   ├── Color.kt             # Navy #0F172A + Electric Blue #2563EB
    │   ├── Type.kt              # Material3 typography scale
    │   └── Theme.kt             # Dark/Light ColorScheme (system-aware)
    ├── components/
    │   ├── NumericKeypad.kt     # 4-col grid, Add Item spans 2 rows
    │   ├── CartItemRow.kt       # Swipe-to-delete + +/− stepper
    │   └── SuggestionChipRow.kt # Animated horizontal scrollable chips
    ├── screens/
    │   ├── QuickSaleScreen.kt   # Main 2-panel POS (keypad left, cart right)
    │   ├── CheckoutScreen.kt    # Modal bottom sheet (Cash/UPI/Card)
    │   ├── HistoryScreen.kt     # Sales history + daily revenue summary
    │   ├── PrinterSettingsScreen.kt  # Paired BT device picker + test print
    │   └── SettingsScreen.kt    # Shop details + receipt config
    └── navigation/
        └── Screen.kt            # Route sealed class + bottom nav definitions
```

---

## 🔧 Tech Stack

| Layer | Library | Version |
|---|---|---|
| Language | Kotlin | 2.3.21 |
| UI | Jetpack Compose + Material3 | BOM 2024.12.01 |
| Navigation | Navigation Compose | 2.8.9 |
| Database | Room + SQLite | 2.7.1 |
| Annotation Processor | KSP | 2.3.11 |
| State | ViewModel + StateFlow | 2.8.7 |
| Preferences | DataStore Preferences | 1.1.7 |
| Coroutines | kotlinx.coroutines | 1.9.0 |
| Background | WorkManager | 2.10.1 |
| Thermal Printing | DantSu ESCPOS (JitPack) | 3.3.0 |
| Build Tool | AGP | 9.4.0 |
| Min SDK | Android 7.0 Nougat | API 24 |
| Target SDK | | API 37 |

---

## 🚀 Setup & Build

### Prerequisites
- **Android Studio** Meerkat or later (bundled JBR / JDK 17+)
- Android device or emulator — API 24+
- Bluetooth thermal printer (optional)

### Build from CLI (Windows)

```bat
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
gradlew.bat assembleDebug
gradlew.bat installDebug
```

### Build from Android Studio
1. Open project → wait for Gradle sync
2. Run on device/emulator (`Shift+F10`)

> **First launch** auto-seeds 25 common products so suggestions work immediately — no manual setup needed.

---

## 🖨️ Printer Setup

1. **Pair your printer** via Android Settings → Bluetooth
2. Open app → **Printer** tab (bottom nav)
3. Select your device from the list → tap to save
4. Tap **Test Print** to verify

Supported: any 58mm or 80mm Bluetooth ESC/POS printer (Xprinter, GOOJPRT, Munbyn, etc.)

---

## 📱 Permissions

| Permission | Reason |
|---|---|
| `BLUETOOTH` / `BLUETOOTH_ADMIN` | Legacy BT (API < 31) |
| `BLUETOOTH_CONNECT` / `BLUETOOTH_SCAN` | BT device access (API 31+) |
| `INTERNET` | Optional future cloud sync |
| `VIBRATE` | Haptic feedback on keypad taps |

---

## 🗄️ Database Schema

```
sales           → id, timestamp, total, paymentMethod, itemCount,
                  amountTendered, changeGiven, receiptText

sale_items      → id, saleId (FK→sales CASCADE), amount, quantity,
                  label, lineTotal

products        → id, name, price, category, frequency,
                  lastUsed, isActive
```

---

## 💡 Suggestion Scoring Formula

```
score = (priceProximity × 0.40) + (usageFrequency × 0.40) + (recency × 0.20)

bucket = products where price ∈ [ input × 0.70 , input × 1.30 ]
```

- Bucket miss → top 8 most-used products shown instead
- Every sale: `learnFromSale(price, label)` creates/increments auto-learned products

---

## 🛣️ Roadmap

- [ ] CSV export of daily / weekly sales
- [ ] UPI QR code generation at checkout
- [ ] Coupon / discount codes
- [ ] Supabase cloud backup
- [ ] WorkManager daily email digest
- [ ] Barcode scanner support
- [ ] Multi-cashier / user support

---

## 📁 Key Files

| File | What it does |
|---|---|
| `MainActivity.kt` | App entry point, Compose host, bottom nav |
| `QuickSaleScreen.kt` | Main POS screen (2-panel layout) |
| `SalesViewModel.kt` | All cart + keypad + print state (StateFlow) |
| `SuggestionEngine.kt` | Smart price-bucket algorithm |
| `PrinterHelper.kt` | Bluetooth ESC/POS receipt builder |
| `AppDatabase.kt` | Room DB singleton + seed data |
| `PreferencesManager.kt` | DataStore settings persistence |
| `libs.versions.toml` | All dependency versions |

---

## 📄 License

MIT — free to use, modify, and distribute.

---

<p align="center">Built with ❤️ for Indian small businesses</p>
