package com.example.test.data

import android.util.Log
import com.example.test.data.dao.AccountDao
import com.example.test.data.dao.CategoryDao
import com.example.test.model.Account
import com.example.test.model.AccountType
import com.example.test.model.Category
import com.example.test.model.CategoryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Prepares the data a user needs the first time they sign in on this device.
 *
 * Also "claims" legacy rows (userId = 0) created before multi-user support,
 * so upgrading the app does not lose someone's existing transactions.
 */
@Singleton
class UserDataSeeder @Inject constructor(
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val database: AppDatabase
) {
    suspend fun prepareUser(userId: Int) = withContext(Dispatchers.IO) {
        if (userId <= 0) return@withContext

        claimUnownedRows(userId)

        if (categoryDao.countForUser(userId) == 0) {
            seedCategories(userId)
        }
        if (accountDao.countForUser(userId) == 0) {
            seedAccount(userId)
        }
    }

    private fun claimUnownedRows(userId: Int) {
        val db = database.openHelper.writableDatabase
        db.beginTransaction()
        try {
            listOf("accounts", "categories", "transactions", "budgets", "notifications")
                .forEach { table ->
                    db.execSQL(
                        "UPDATE $table SET userId = ? WHERE userId = 0",
                        arrayOf<Any>(userId)
                    )
                }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to claim legacy rows for user $userId", e)
        } finally {
            db.endTransaction()
        }
    }

    private suspend fun seedCategories(userId: Int) {
        defaultCategories().forEach { category ->
            categoryDao.insert(category.copy(userId = userId))
        }
    }

    private suspend fun seedAccount(userId: Int) {
        accountDao.insert(
            Account(
                name = "Cash",
                balance = 0.0,
                type = AccountType.WALLET,
                userId = userId
            )
        )
    }

    private fun defaultCategories(): List<Category> = listOf(
        Category(name = "Food & Dining", type = CategoryType.EXPENSE, color = "#FF5722"),
        Category(name = "Transportation", type = CategoryType.EXPENSE, color = "#2196F3"),
        Category(name = "Shopping", type = CategoryType.EXPENSE, color = "#9C27B0"),
        Category(name = "Bills & Utilities", type = CategoryType.EXPENSE, color = "#FF9800"),
        Category(name = "Entertainment", type = CategoryType.EXPENSE, color = "#E91E63"),
        Category(name = "Health", type = CategoryType.EXPENSE, color = "#4CAF50"),
        Category(name = "Education", type = CategoryType.EXPENSE, color = "#00BCD4"),
        Category(name = "Other", type = CategoryType.EXPENSE, color = "#607D8B"),
        Category(name = "Salary", type = CategoryType.INCOME, color = "#4CAF50"),
        Category(name = "Freelance", type = CategoryType.INCOME, color = "#8BC34A"),
        Category(name = "Investments", type = CategoryType.INCOME, color = "#009688"),
        Category(name = "Gifts", type = CategoryType.INCOME, color = "#FFC107"),
        Category(name = "Other Income", type = CategoryType.INCOME, color = "#795548")
    )

    private companion object {
        const val TAG = "UserDataSeeder"
    }
}
