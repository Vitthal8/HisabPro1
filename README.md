# HisabPro — GST Billing & Khata App for Indian SMBs

<p align="center">
  <img src="docs/screenshot_sales.png" width="200" alt="Sales Screen"/>
  <img src="docs/screenshot_khata.png" width="200" alt="Party Khata"/>
  <img src="docs/screenshot_reports.png" width="200" alt="Reports"/>
</p>

A **free, offline-first Android app** for Indian small businesses to manage:
- 📋 GST Tax Invoices, Non-GST Bills & Proforma Quotations
- 🧾 Party Khata Ledger (Customer & Supplier credit tracking)
- 📦 Item / Stock management with low-stock alerts
- 💰 Cashbook — daily income & expense tracking
- 📊 Reports — Daybook, GSTR-1, GSTR-3B, P&L, Party Aging, Stock Valuation

All data stays **on device** — no login, no cloud dependency.

---

## Features

| Module | Highlights |
|--------|-----------|
| **Sales** | Tax Invoice (CGST/SGST/IGST), Non-GST Bill, Proforma; PDF share via WhatsApp |
| **Parties** | Customer & Supplier ledger, Khata entries, balance aging, PDF statement |
| **Items** | SKU/barcode, HSN codes, GST rate per item, stock history |
| **Cashbook** | Income/Expense with category & payment-mode filters, CSV export |
| **Reports** | GSTR-1, GSTR-3B, Profit & Loss, Party Aging, Stock Valuation |
| **UPI** | QR code on invoice, one-tap UPI payment link to customer |

---

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Storage**: SharedPreferences (JSON) — Room migration planned
- **PDF**: Android Canvas / PdfDocument API
- **QR Code**: ZXing
- **Architecture**: MVVM + Repository pattern
- **Min SDK**: 24 (Android 7.0) | **Target SDK**: 36

---

## Getting Started

```bash
git clone https://github.com/Vitthal8/HisabPro1.git
cd HisabPro1
./gradlew assembleDebug
```

Open in **Android Studio Meerkat (2024.3)** or later and run on any Android 7+ device or emulator.

---

## Project Structure

```
app/src/main/kotlin/com/hisabpro/app/
├── data/
│   ├── model/          # Invoice, Party, Item, Transaction, BusinessProfile
│   └── repository/     # InvoiceRepository, PartyRepository, ItemRepository …
├── ui/
│   ├── sales/          # CreateInvoiceSheet, SalesScreen, InvoiceViewModel
│   ├── party/          # PartiesListScreen, PartyKhataScreen, PartyViewModel
│   ├── items/          # ItemsScreen, AddEditItemSheet, StockAdjustSheet
│   ├── purchases/      # CreatePurchaseSheet, PurchasesScreen
│   ├── reports/        # GSTR-1, GSTR-3B, P&L, Daybook, Aging, Stock sheets
│   └── theme/          # Color, Theme
└── util/
    ├── InvoicePdfGenerator.kt
    ├── CashbookPdfGenerator.kt
    ├── PartyStatementPdfGenerator.kt
    └── UpiPaymentHelper.kt
```

---

## Roadmap

- [ ] Room DB migration (replace SharedPreferences JSON)
- [ ] Cloud backup (Google Drive / local file export)
- [ ] Barcode scanner for item lookup
- [ ] Multi-business profile support
- [ ] Recurring invoice / subscription billing
- [ ] WhatsApp payment reminder automation

---

## License

MIT — free to use, modify and distribute.

---

> Built by [Vitthal](https://github.com/Vitthal8) · Navi Mumbai, Maharashtra 🇮🇳
> Designed for Indian kirana stores, traders, and service businesses.
