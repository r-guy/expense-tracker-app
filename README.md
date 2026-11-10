# Personal Finance Manager

Android app for tracking income, expenses, budgets and accounts, built with
Kotlin, Jetpack Compose and Material 3.

## Features

- Multiple accounts and custom income/expense categories
- Income, expense and transfer transactions
- Budgets with custom periods, repeats and threshold alerts
- Reports, notifications, configurable calendar and currency
- Amounts stored in USD, shown with INR alongside by default

## Tech

MVVM, Hilt, Room (SQLite), Coroutines/Flow, DataStore, OkHttp.
Authentication: Neon Auth (Managed Better Auth).
Exchange rates: FastAPI backend with ExchangeRate-API fallback.

## Architecture

```
Compose screens (view/, ui/)
      ↓  collectAsStateWithLifecycle / observeAsState
ViewModels (viewmodel/)        StateFlow / LiveData, viewModelScope
      ↓
Repositories & services        AuthRepository, UserDataSeeder,
      ↓                        CurrencyExchangeService
Room DAOs ──► SQLite           Neon Auth ──► HTTPS (cookie session)
```

- **UI** — Compose screens and reusable components, one `NavHost` for navigation.
- **ViewModels** — expose `StateFlow`/`LiveData`; all work runs in `viewModelScope`.
- **Data** — DAOs filter every query by `UserSession.userId` and stamp it on insert.
  `UserDataSeeder` claims legacy rows and seeds defaults on first sign-in.
- **Auth** — `NeonAuthApi` + `AuthCookieJar` hold the session; `AuthViewModel`
  revalidates it at startup and signs out if the server revoked it.
- **Currency** — backend first, direct API fallback, one-hour cache, prefetched at start.
- **DI** — Hilt modules provide the database, DAOs, auth client and singletons.

## Data model

Every account, category, transaction, budget and notification carries a
`userId`, and every query is filtered by the signed-in user. Sessions persist
across restarts and are revoked on sign-out.

## Setup

1. `cp .env.example .env`, then fill in:
   - `NEON_AUTH_BASE_URL`
   - `EXCHANGE_RATE_API_KEY`
   - `CURRENCY_BACKEND_BASE_URL` (comma-separated, tried in order)
2. Start the backend (optional):
   `cd D:/ExpenseTrackerBackend && docker compose up --build -d`
3. Build and install: `./gradlew assembleDebug`

For USB devices, run `adb reverse tcp:8000 tcp:8000` so the app can reach the
local backend.

## Security

`.env` files and `DATABASE_URL` never leave the server; the app stores no
passwords and no database credentials. Cleartext HTTP is allowed in debug
builds only.
