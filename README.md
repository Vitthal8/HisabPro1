# HisabPro — Indian Billing, GST & Khata App for Small Businesses

A production-ready **100% offline-first Android accounting app** designed for Indian shop owners, kirana stores, traders, and small businesses to manage daily billing, customer ledgers (Khata), inventory, cashbook, and GST filing reports.

---

## Key Highlights

- ⚡ **100% Offline-First Architecture**: Core billing, accounting, and Khata ledger function completely without internet connectivity.
- 🏢 **Multi-Tenant Business Isolation**: Manage multiple shops/businesses independently with strict data isolation.
- 💰 **Exact Monetary Calculation Precision**: All financial arithmetic uses `Long` paise or `BigDecimal` scale 2 with `RoundingMode.HALF_UP`. Zero float/double rounding errors.
- ⚖️ **Centralized GST Policy Enforcement**: Composition scheme and non-GST shops NEVER produce GST output headers, tax columns, or GSTR filing entries.
- ☁️ **Optional Supabase Cloud Sync**: Encrypted multi-tenant synchronization backed by PostgreSQL Row-Level Security (RLS).
- 💳 **Google Play Billing 6+ / 7+ Integration**: Transparent monetization with single entitlement management.

---

## Monetization & Plans

| Feature | Free Starter | Pro Business (₹99/mo, ₹799/yr) | Premium Enterprise (₹199/mo, ₹1499/yr) |
| :--- | :---: | :---: | :---: |
| **Parties & Items** | Unlimited | Unlimited | Unlimited |
| **Offline Billing & Khata** | Unlimited | Unlimited | Unlimited |
| **Monthly Bill Limit** | **50 bills / month** | **Unlimited** | **Unlimited** |
| **AdMob Ads** | Banner Enabled | **Ad-Free** | **Ad-Free** |
| **Business Profiles** | 1 Business | 1 Business | **Up to 5 Businesses** |
| **Custom Business Logo on Bills** | ❌ | ✅ | ✅ |
| **WhatsApp Direct Bill Sharing** | ❌ | ✅ | ✅ |
| **CSV / Excel Export** | ❌ | ✅ | ✅ |
| **GSTR-1 & GSTR-3B Tax Filing** | ❌ | ✅ | ✅ |
| **Supabase Cloud Sync & Backup** | ❌ | ❌ | ✅ |

---

## Technology Stack

- **Language**: Kotlin 2.1
- **UI Framework**: Jetpack Compose + Material 3
- **Database**: Room 2.8.5 (Version 6, non-destructive migration `MIGRATION_5_6`)
- **Background Operations**: WorkManager 2.10.0
- **Cloud Backend (Optional)**: Custom Supabase Ktor/REST Client with Auth Session handling
- **In-App Billing**: Google Play Billing Library 7.1.1 (`billing-ktx`)
- **Advertising**: Google Mobile Ads (AdMob) & User Messaging Platform (UMP)
- **PDF Generation**: Native Android `PdfDocument` Canvas API
- **Thermal POS Printing**: ESC/POS Binary Protocol (58mm & 80mm)
- **Min SDK**: 24 (Android 7.0) | **Target SDK**: 36 (Android 15)

---

## Getting Started

### Build Prerequisites
- Android Studio Meerkat (2024.3) or later
- JDK 21

### Local Compilation
```bash
git clone https://github.com/Vitthal8/HisabPro1.git
cd HisabPro1
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

---

## Project Structure

```
app/src/main/kotlin/com/hisabpro/app/
├── data/
│   ├── local/          # AppDatabase, DAOs (InvoiceDao, PartyDao...), Entities
│   ├── model/          # Invoice, Party, Item, Transaction, BusinessProfile
│   ├── repository/     # InvoiceRepository, PartyRepository, BillingRepository
│   └── sync/           # CloudSyncManager, SupabaseAuthManager, SupabaseApiClient
├── domain/
│   ├── accounting/     # GstPolicy, AccountingEngine, DayBookCalculator
│   ├── subscription/   # SubscriptionManager, Entitlements, BillingConstants
│   └── usecase/        # CreateInvoiceUseCase, RecordPaymentUseCase...
├── ui/
│   ├── sales/          # SalesScreen, CreateInvoiceSheet, InvoiceViewModel
│   ├── party/          # PartiesListScreen, PartyKhataScreen, PartyViewModel
│   ├── items/          # ItemsScreen, AddEditItemSheet, StockAdjustSheet
│   ├── purchases/      # CreatePurchaseSheet, PurchasesScreen
│   ├── reports/        # ReportsScreen, Daybook, GSTR-1, P&L sheets
│   └── theme/          # Color, Theme
└── util/
    ├── InvoicePdfGenerator.kt
    ├── ThermalSlipGenerator.kt
    ├── MonetaryUtils.kt
    └── IndianAccountingFormat.kt
```
