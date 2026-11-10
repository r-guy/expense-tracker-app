# Personal Finance Manager Using Kotlin and Jetpack Compose

A comprehensive Android application for managing personal finances. Track income, expenses, budgets, and financial goals with a modern, intuitive interface built using Kotlin and Jetpack Compose.

## Features

### Core Functionality
- **Multi-Account Management** - Track multiple bank accounts, wallets, and etc.
- **Smart Categorization** - Organize income and expenses with custom categories
- **Transaction Tracking** - Record income, expenses, and transfers between accounts
- **Budget Management** - Set budgets with custom periods and track spending limits
- **Financial Reports** - Visualize financial data with comprehensive reports
- **Notifications** - Stay informed about financial activities and budget alerts
- **Flexible Settings** - Customize calendar type, currency, and financial month start date

### Advanced Features
- **Recurring Budgets** - Set up repeating budgets for regular expenses (e.g., loan payments)
- **Transfer Management** - Move money between your own accounts seamlessly
- **Custom Financial Month** - Define your own financial month start date
- **Multi-Currency Support** - Manage finances in different currencies
- **Calendar Flexibility** - Choose between different calendar types

## Technology Stack

- **Platform**: Android (API 24+)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with Clean Architecture principles
- **Dependency Injection**: Hilt (Dagger)
- **Database**: Room (SQLite), one schema scoped per signed-in user
- **Async Operations**: Kotlin Coroutines & Flow
- **Data Storage**: DataStore (Preferences) for session/user id
- **Authentication**: Neon Auth (Managed Better Auth) over HTTPS, cookie sessions
- **Networking**: OkHttp (Neon Auth client + currency-rate client)
- **Build System**: Gradle with Kotlin DSL

## Project Structure

```
app/
├── data/
│   ├── api/              # Currency rates + Neon Auth REST client
│   ├── auth/             # Persistent session cookie jar
│   ├── dao/              # Room DAOs (all queries scoped by userId)
│   ├── datastore/        # DataStore implementations (session + user id)
│   └── repository/       # Repository implementations (auth, data seeding)
├── di/                   # Dependency injection modules
├── model/                # Data models and entities
├── navigation/           # Navigation graph and screen definitions
├── service/              # Background services
├── ui/
│   ├── auth/             # Authentication screens
│   ├── components/       # Reusable UI components
│   └── theme/            # Theme configuration
├── util/                 # Utility classes
└── view/                 # Feature screens
    ├── accounts/
    ├── budgeting/
    ├── categories/
    ├── dashboard/
    ├── notifications/
    ├── reports/
    ├── settings/
    └── transactions/
```

A companion backend lives in `D:/ExpenseTrackerBackend` (FastAPI, Docker) and
serves cached currency rates. Authentication is handled by Neon, not by that service.

## Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 11 or higher
- Android SDK with API 24+

### Installation
1. Clone this repository
   ```bash
   git clone https://github.com/Erfan-Farahmandnejad/Personal-Finance-Manager-Using-Kotlin-and-Jetpack-Compose.git
   ```
2. Create the environment file (see [Configuration](#configuration))
   ```bash
   cp .env.example .env
   ```
   Fill in your own values. `.env` is git-ignored and is baked into `BuildConfig`
   at build time, so rebuild after changing it.
3. Start the currency backend (optional but recommended)
   ```bash
   cd D:/ExpenseTrackerBackend
   docker compose up --build -d
   ```
4. Open the project in Android Studio
5. Sync Gradle dependencies
6. Build and run the application on your device or emulator

## Configuration

The app reads a root-level `.env` file at build time:

| Variable | Purpose |
|---|---|
| `EXCHANGE_RATE_API_KEY` | ExchangeRate-API fallback key (only used if the backend is unreachable) |
| `EXCHANGE_RATE_API_BASE` | `https://v6.exchangerate-api.com/v6` |
| `CURRENCY_BACKEND_BASE_URL` | One or more backend URLs, comma-separated, tried in order |
| `NEON_AUTH_BASE_URL` | Neon Auth endpoint used for sign-up / sign-in / sign-out |

See `.env.example` for a template. Never commit `.env`.

### Local backend URL

`CURRENCY_BACKEND_BASE_URL` accepts a list; the app uses the first reachable one:

| Scenario | Value | Setup |
|---|---|---|
| Physical device over USB | `http://127.0.0.1:8000` | `adb reverse tcp:8000 tcp:8000` |
| Physical device on Wi-Fi | `http://<PC-LAN-IP>:8000` | allow port 8000 in the firewall |
| Android emulator | `http://10.0.2.2:8000` | nothing extra |

Example:

```
CURRENCY_BACKEND_BASE_URL=http://127.0.0.1:8000,http://192.168.0.44:8000
```

### Authentication

Sign-in is handled by **Neon Auth (Managed Better Auth)**. Users, sessions and
accounts are stored in the `neon_auth` schema of a Neon Postgres branch; the app
never holds a database credential.

Provision it once from the backend folder:

```bash
neon init -y --org-id <org> --project-id <project> --branch production --services "postgres,auth"
neon login     # browser sign-in
neon deploy    # applies neon.ts and writes NEON_AUTH_* / DATABASE_URL to .env
```

Copy the resulting `NEON_AUTH_BASE_URL` into the app's `.env` and rebuild.
Sessions are cookie-based: the app stores them in a persistent cookie jar, so
users stay signed in across restarts. Signing out revokes the server session.

Never put `DATABASE_URL` in the app — it belongs on the server only.

### First-Time Setup

#### 1. Authentication
- Launch the app and create your account via the Sign Up screen
- Credentials are verified by Neon Auth; the session is remembered on the device
- Sign out from **Settings → Account → Sign Out**

#### 2. Per-account data isolation
Every account, category, transaction, budget and notification row carries a
`userId`. All queries are filtered by the signed-in user, so two people using
the same device each see only their own data.

#### 2. Dashboard Overview
- After signing in, you will land on the Dashboard
- The navigation drawer provides access to all features:
  - Dashboard
  - Accounts
  - Categories
  - Transactions
  - Reports
  - Notifications
  - Settings
  - Budgets

#### 3. Initial Configuration

##### a. Add Accounts
Navigate to **Accounts** and add your financial accounts:
- Bank accounts
- Wallets
- Other

##### b. Create Categories
Set up **Categories** for better organization:
- **Expense Categories**: Food, Transportation, Entertainment, Utilities, Healthcare, etc.
- **Income Categories**: Salary, Freelance, Investments, Gifts, etc.

##### c. Configure Settings
Go to **Settings** to customize:
- **First Day of Financial Month**: Choose when your financial month starts
- **Calendar Type**: Select your preferred calendar system
- **Currency**: Set your primary currency

#### 4. Set Up Budgets
Navigate to **Budgets** to create spending limits:
- Choose an expense category
- Set the budget amount
- Define the time period with start and end dates
- Set number of repeats for recurring budgets (useful for loan payments, subscriptions)

## Usage

### Adding Transactions

1. Navigate to **Transactions**
2. Click on "Add Transaction"
3. Fill in the required details:
   - **Type**: Choose between Income, Expense, or Transfer
   - **Category**: Select the appropriate category (not required for transfers)
   - **Account**: Choose the source account
   - **Destination Account**: For transfers, select the destination account
   - **Amount**: Enter the transaction amount
   - **Date**: Select the transaction date
   - **Note**: Add optional notes for reference

### Transaction Types

- **Income**: Money coming into your account
- **Expense**: Money spent from your account
- **Transfer**: Moving money between your own accounts only (not to external accounts). This feature is designed for internal financial management, allowing you to track money movement between your personal accounts (e.g., from checking to savings, from cash to bank account)

### Monitoring Your Finances

#### Reports
- View financial reports
- Monitor budget performance

#### Notifications
- Stay updated on budget limits

## User Flow

```
Sign Up → Sign In → Dashboard
                        ↓
        ┌───────────────┴───────────────┐
        ↓                               ↓
    Setup Phase                    Daily Usage
        ↓                               ↓
1. Add Accounts              → Add Transactions
2. Create Categories         → View Reports
3. Configure Settings        → Check Notifications
4. Set Up Budgets            → Monitor Budgets
```

## Best Practices

1. **Start with Setup**: Complete the initial configuration (accounts, categories, settings) before adding transactions
2. **Regular Updates**: Record transactions promptly for accurate tracking
3. **Review Reports**: Check your financial reports regularly to understand spending patterns
4. **Budget Monitoring**: Keep an eye on budget notifications to stay within limits
5. **Categorize Properly**: Use appropriate categories for better financial insights

## Screenshots

The application includes the following screens:

### Authentication
- **Sign Up Screen**: User registration with account creation
- **Sign In Screen**: User authentication and login

### Main Application
- **Dashboard**: Overview of financial status, recent transactions, and quick statistics
- **Accounts Management**: View, add, edit, and manage multiple financial accounts
- **Categories**: Organize and manage income and expense categories
- **Transactions**: Record and view all financial transactions (income, expense, transfer)
- **Reports**: Visual analytics and insights into spending patterns and financial trends
- **Notifications**: Alerts and reminders for budget limits
- **Settings**: Customize app preferences including calendar type, currency, and financial month start date
- **Budgets**: Create and monitor spending limits with custom periods and recurring options

<img src="https://github.com/user-attachments/assets/7fc79436-39b3-4729-b22b-7ce4c2217d76" width="300">
<img src="https://github.com/user-attachments/assets/96814c5b-672d-4abb-958e-f5dc0a0aea56" width="300">
<img src="https://github.com/user-attachments/assets/3ac9152a-9d58-4fcd-b709-e51b46354083" width="300">
<img src="https://github.com/user-attachments/assets/530c5c11-8a12-4522-93b9-15d1e35a8b9c" width="300">
<img src="https://github.com/user-attachments/assets/65ba700d-78e5-491f-9b70-e60ba4b7602b" width="300">
<img src="https://github.com/user-attachments/assets/e1d8b827-f42c-47ce-b758-20be8e7acf0b" width="300">
<img src="https://github.com/user-attachments/assets/358e597e-5347-4d00-8562-65c0551fc8a1" width="300">
<img src="https://github.com/user-attachments/assets/23043fcc-d57b-4c75-8e47-d8619067d253" width="300">
<img src="https://github.com/user-attachments/assets/1c068561-7707-409b-8626-94b8f20ab6b3" width="300">
<img src="https://github.com/user-attachments/assets/94682744-2eaf-47e4-9829-4b47c1ca81d1" width="300">
<img src="https://github.com/user-attachments/assets/ed1fbccb-4902-4f1d-bf0a-a692e1275fe9" width="300">
<img src="https://github.com/user-attachments/assets/8a41153a-1b74-44a2-b2e7-41c350660ec2" width="300">
<img src="https://github.com/user-attachments/assets/43720f2c-0490-40cf-89ef-d192253812ee" width="300">
<img src="https://github.com/user-attachments/assets/20373154-b663-439b-af6c-e8bb9fb53c27" width="300">
<img src="https://github.com/user-attachments/assets/39eae99e-5e52-48e5-aa54-b4ff03745090" width="300">
<img src="https://github.com/user-attachments/assets/5968b5bd-f4a5-4dfe-8535-091264eeeb56" width="300">
<img src="https://github.com/user-attachments/assets/9087e42e-6c80-499f-9917-91985eebb84d" width="300">
<img src="https://github.com/user-attachments/assets/25dc91f3-49de-4582-b28a-47ee84e19927" width="300">
<img src="https://github.com/user-attachments/assets/03c55518-a627-47bc-a240-aee104160a7e" width="300">
<img src="https://github.com/user-attachments/assets/95586df7-c247-442f-802e-7d6549d7acd2" width="300">
<img src="https://github.com/user-attachments/assets/218151a1-b6e4-4968-a812-1e544476bf9d" width="300">
<img src="https://github.com/user-attachments/assets/a480d0d9-8b0a-40ae-883c-6b5b806b228c" width="300">




## Key Features Implementation

### Database Schema
- **Users**: local mapping of an authenticated email to the id that owns its data
- **Accounts**: Financial account information (scoped by `userId`)
- **Categories**: Income and expense categories (scoped by `userId`)
- **Transactions**: Financial transactions with type, amount, date, and notes (scoped by `userId`)
- **Budgets**: Budget limits with start/end dates and repeat functionality (scoped by `userId`)
- **Notifications**: System notifications and alerts (scoped by `userId`)
- **Settings**: User preferences and app configuration (one row per user)

Schema version 6, with migrations from versions 1–5. Existing pre-multi-user
rows are claimed by the first account that signs in, so upgrading keeps data.

### Services
- **BudgetNotificationService**: Background service for monitoring budgets and sending alerts

### Currency Rates
Amounts are stored in USD and displayed in the user's primary currency, with a
secondary currency shown alongside (USD + INR by default). Rates come from the
FastAPI backend first and fall back to ExchangeRate-API directly if it is
unreachable; results are cached for one hour and prefetched at app start.

## Security Notes

- Passwords are never stored on the device; only the auth session cookie is.
- `DATABASE_URL` and other Neon credentials stay in the backend `.env`, never in the app.
- Debug builds allow cleartext HTTP for the local backend; release builds stay HTTPS-only.

## Contributors

* [Erfan-Farahmandnejad](https://github.com/Erfan-Farahmandnejad)
* [Shabnam-Khaqanpoor](https://github.com/Shabnam-Khaqanpoor)

