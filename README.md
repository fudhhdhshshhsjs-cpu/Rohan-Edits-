# Rohan Edits App

A private, offline-first native Android video-editing career management system built for **Rohan** (**Rohan Edits**) using Kotlin, Jetpack Compose, Material 3, Room Database, and DataStore.

## Key Features
- **Home Dashboard**: Real-time Career Overview, Financial Overview, Monthly Performance, Quick Actions, and Active Projects preview.
- **Editing Journey Calendar**: Official journey start date fixed at **06 July 2026** (`Asia/Kolkata`), manual daily statuses (`Editing Done`, `Editing Missed`, `Not Marked`), Monthly & Yearly views, and automatic consistency calculation (`Done / (Done + Missed) × 100`, excluding Not Marked and future dates).
- **Client Management**: Client profiles with 9 fields, search, filter by New/Old/Regular, sort by Name or Recent Activity, safe deletion policy preserving financial ledger history.
- **Project & Video Management**: Commissioned Client Videos and standalone Personal/Portfolio/Practice Videos, priority & overdue tracking, client feedback/rating, and multi-criteria filtering.
- **Budget & Financial Ledger**: Income and Expense ledgers in INR (`₹`) with transactional project payment linking (`Partial` / `Paid` / `Unpaid`) that prevents double-counting.
- **Editor Info & Analytics**: Live experience counter since 6 July 2026, calendar integration, interactive Income vs Expenses and Video Output charts, and client analytics.
- **Backup, Restore & Export**: Full JSON backup & validated restore plus 7 CSV exports via Android Storage Access Framework (zero broad storage permissions).
- **Theming & Accessibility**: Light Mode (default), Dark Mode, and System Default theme; `DD MMM YYYY` date format; optional local deadline notifications.

## Architecture
- **Language & UI**: Kotlin, Jetpack Compose, Material Design 3
- **Persistence**: Room SQLite (`AppDatabase`) + Jetpack DataStore (`PreferencesDataStore`)
- **State & Concurrency**: MVVM (`MainViewModel`), Kotlin Coroutines & `StateFlow`
- **Package ID**: `com.rohanedits.edittrack`
- **Minimum SDK**: Android 8.0 (API 26) · Target SDK 36

## Building APKs
- **Debug APK**:
  ```bash
  gradle :app:assembleDebug
  ```
  Output: `app/build/outputs/apk/debug/app-debug.apk`
- **Unit & Robolectric Tests**:
  ```bash
  gradle :app:testDebugUnitTest
  ```
- **Release APK**:
  1. Generate or configure your private upload keystore (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD` environment variables—never commit private keystores or secrets to Git).
  2. Run `gradle :app:assembleRelease` (or `gradle :app:bundleRelease` for Play Store AAB).
