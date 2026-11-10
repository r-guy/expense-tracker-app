package com.example.test.service

import android.util.Log
import com.example.test.data.UserSession
import com.example.test.data.dao.BudgetDao
import com.example.test.data.dao.CategoryDao
import com.example.test.data.dao.NotificationDao
import com.example.test.data.dao.SettingsDao
import com.example.test.data.dao.TransactionDao
import com.example.test.model.Notification
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for monitoring budget thresholds and generating notifications
 * Tracks spending against budgets and creates alerts at specified thresholds (50%, 80%)
 */
@Singleton
class BudgetNotificationService @Inject constructor(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao,
    private val notificationDao: NotificationDao,
    private val categoryDao: CategoryDao,
    private val settingsDao: SettingsDao,
    private val userSession: UserSession
) {
    /**
     * Data class representing a budget threshold alert
     * @property title The title of the alert, typically category name
     * @property message The detailed message with spending information
     */
    data class BudgetAlert(
        val title: String,
        val message: String
    )

    /**
     * Checks all budgets or a specific category budget against spending thresholds
     * Creates notifications when spending reaches important thresholds
     *
     * @param categoryId Optional category ID to check only that specific budget
     * @return List of generated budget alerts
     */
    suspend fun checkBudgetThresholds(categoryId: Int? = null): List<BudgetAlert> {
        val userId = userSession.requireUserId()

        // First check if notifications are enabled in settings
        val settings = settingsDao.getSettings(userId).first()
        val notificationsEnabled = settings?.notificationsEnabled ?: true

        // If notifications are disabled, return empty list
        if (!notificationsEnabled) {
            Log.d("BudgetAlert", "Notifications are disabled in settings. Skipping alerts.")
            return emptyList()
        }

        val alerts = mutableListOf<BudgetAlert>()
        val budgets = budgetDao.getAllBudgets(userId).first()
        // Filter budgets if a specific category is requested
        val filteredBudgets = if (categoryId != null) budgets.filter { it.categoryId == categoryId } else budgets

        // Process each budget to check threshold levels
        for (budget in filteredBudgets) {
            // Calculate total expenses for this budget's category within its date range
            val totalExpenses = transactionDao.getTotalExpensesForCategoryInDateRange(
                userId,
                budget.categoryId,
                budget.startDate,
                budget.endDate
            ) ?: 0.0

            // Calculate what percentage of the budget has been used
            val percentageUsed = (totalExpenses / budget.amountLimit) * 100
            val categoryName = getCategoryName(budget.categoryId)

            Log.d("BudgetAlert", "Budget for $categoryName: $totalExpenses / ${budget.amountLimit} (${percentageUsed}%)")

            // Check for 50% threshold
            if (percentageUsed >= 50 && percentageUsed < 80) {
                val remaining = budget.amountLimit - totalExpenses
                val alert = BudgetAlert(
                    title = "$categoryName",
                    message =  "Spent: $${String.format("%.2f", totalExpenses)}\n" +
                            "Remaining: $${String.format("%.2f", remaining)}"
                )
                alerts.add(alert)
                createBudgetNotification(userId, alert.title, alert.message)
                Log.d("BudgetAlert", "Created 50% alert for $categoryName")
            }
            // Check for 80% threshold
            else if (percentageUsed >= 80) {
                val remaining = budget.amountLimit - totalExpenses
                val alert = BudgetAlert(
                    title = "$categoryName",
                    message = "Spent: $${String.format("%.2f", totalExpenses)}\n" +
                            "Remaining: $${String.format("%.2f", remaining)}"
                )
                alerts.add(alert)
                createBudgetNotification(userId, alert.title, alert.message)
                Log.d("BudgetAlert", "Created 80% alert for $categoryName")
            }
        }

        Log.d("BudgetAlert", "Generated ${alerts.size} alerts")
        return alerts
    }

    /**
     * Creates and stores a notification in the database based on budget alert
     * Respects user notification preferences from settings
     */
    private suspend fun createBudgetNotification(
        userId: Int,
        title: String,
        message: String
    ) {
        // Double-check notifications are enabled before creating notification
        val settings = settingsDao.getSettings(userId).first()
        if (settings?.notificationsEnabled == false) {
            Log.d("BudgetAlert", "Notifications disabled. Skipping notification creation.")
            return
        }

        // Create and store the notification in the database
        val notification = Notification(
            title = title,
            message = message,
            date = LocalDate.now().format(DateTimeFormatter.ISO_DATE),
            userId = userId
        )
        notificationDao.insert(notification)
        Log.d("BudgetAlert", "Created notification: $title")
    }

    /**
     * Helper method to get category name from ID
     * Returns "Overall" for null category ID or the actual category name
     */
    private suspend fun getCategoryName(categoryId: Int?): String {
        return if (categoryId == null) "Overall" else categoryDao.getCategoryNameById(categoryId) ?: "Unknown Category"
    }
}
