package com.example.test.viewmodel

// Import LiveData components
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
// Import DAO interfaces for database access
import com.example.test.data.UserSession
import com.example.test.data.dao.AccountDao
import com.example.test.data.dao.BudgetDao
import com.example.test.data.dao.CategoryDao
import com.example.test.data.dao.TransactionDao
// Import currency formatting utility
import com.example.test.util.CurrencyFormatter
// Import Hilt for dependency injection
import dagger.hilt.android.lifecycle.HiltViewModel
// Import Flow components for reactive streams
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
// Import coroutine components
import kotlinx.coroutines.launch
// Import for dependency injection
import javax.inject.Inject

// View model for the dashboard screen that handles financial data aggregation and formatting
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accountDao: AccountDao,             // Data access for accounts
    private val transactionDao: TransactionDao,     // Data access for transactions
    private val categoryDao: CategoryDao,           // Data access for categories
    private val budgetDao: BudgetDao,               // Data access for budgets
    private val userSession: UserSession,           // Currently signed-in user
    internal val currencyFormatter: CurrencyFormatter // Utility for formatting currency amounts
) : ViewModel() {

    // MutableLiveData to trigger refresh of dashboard data
    private val _refreshTrigger = MutableLiveData(0)

    private val userId = userSession.userId

    // Observe all data sources, scoped to the signed-in user
    @OptIn(ExperimentalCoroutinesApi::class)
    val accounts = userId.flatMapLatest { accountDao.getAllAccounts(it) }.asLiveData()

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions = userId.flatMapLatest { transactionDao.getAllTransactions(it) }.asLiveData()

    @OptIn(ExperimentalCoroutinesApi::class)
    val categories = userId.flatMapLatest { categoryDao.getAllCategories(it) }.asLiveData()

    @OptIn(ExperimentalCoroutinesApi::class)
    val budgets = userId.flatMapLatest { budgetDao.getAllBudgets(it) }.asLiveData()

    // Calculate financial totals (income, expense, net) and update when refreshTrigger changes
    @OptIn(ExperimentalCoroutinesApi::class)
    val totals: LiveData<Triple<Double, Double, Double>> = combine(
        userId.flatMapLatest { transactionDao.getAllTransactions(it) },
        userId.flatMapLatest { accountDao.getAllAccounts(it) },
        _refreshTrigger.asFlow()
    ) { transactions, accounts, _ ->
        // Sum all income transactions
        val income = transactions.filter { it.type == "income" }.sumOf { it.amount }
        // Sum all expense transactions
        val expense = transactions.filter { it.type == "expense" }.sumOf { it.amount }
        // Calculate net balance (income minus expenses)
        val net = income - expense
        // Return the three values as a Triple
        Triple(income, expense, net)
    }.asLiveData()

    // Get most recent transactions for the dashboard display (10 newest)
    @OptIn(ExperimentalCoroutinesApi::class)
    val recentTransactions = combine(
        userId.flatMapLatest { transactionDao.getAllTransactions(it) },
        _refreshTrigger.asFlow()
    ) { transactions, _ ->
        transactions.sortedByDescending { it.date }.take(10)
    }.asLiveData()

    // Format monetary amount with both primary and secondary currencies
    // For example: "$100 (₹8,800.00)" when USD is primary and INR is secondary
    fun formatWithDualCurrency(amount: Double, currencyCode: String): String {
        return try {
            // Use the currency formatter to show both currencies
            currencyFormatter.formatWithDualCurrency(amount, currencyCode)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to simple formatting if there's an error
            CurrencyFormatter.formatAmount(amount, currencyCode)
        }
    }

    // Format amount specifically for dashboard cards with main and secondary values
    // Returns a Pair where:
    // - first = main currency amount (e.g., "$100")
    // - second = secondary currency amount (e.g., "₹8,800.00")
    fun formatForDashboard(amount: Double, currencyCode: String): Pair<String, String> {
        return try {
            // Use specialized dashboard formatter that separates the two currencies
            currencyFormatter.formatForDashboard(amount, currencyCode)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to simple formatting with empty secondary currency
            Pair(CurrencyFormatter.formatAmount(amount, currencyCode), "")
        }
    }

    // Trigger a refresh of the dashboard data by incrementing the refresh counter
    fun refreshDashboard() {
        _refreshTrigger.value = (_refreshTrigger.value ?: 0) + 1
    }

    // Force a refresh of the dashboard data using a coroutine
    fun forceRefresh() {
        viewModelScope.launch {
            refreshDashboard()
        }
    }
}
