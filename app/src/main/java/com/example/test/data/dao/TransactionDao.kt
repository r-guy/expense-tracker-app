package com.example.test.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.test.model.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object interface for Transaction entities
 * Provides methods to perform database operations on transaction records
 */
@Dao
interface TransactionDao {
    /**
     * Retrieves all transactions for one user ordered by date (newest first)
     * Returns a Flow that emits updates whenever the underlying data changes
     */
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    fun getAllTransactions(userId: Int): Flow<List<Transaction>>

    /**
     * Retrieves a specific transaction by its ID
     */
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): Transaction?

    /**
     * Calculates the total amount spent in a specific category within a date range
     */
    @Query(
        "SELECT SUM(amount) FROM transactions " +
            "WHERE userId = :userId AND type = 'expense' AND categoryId = :categoryId " +
            "AND date BETWEEN :startDate AND :endDate"
    )
    suspend fun getTotalExpensesForCategoryInDateRange(
        userId: Int,
        categoryId: Int?,
        startDate: String,
        endDate: String
    ): Double?

    /**
     * Inserts a new transaction or replaces an existing one
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    /**
     * Deletes a transaction from the database
     */
    @Delete
    suspend fun delete(transaction: Transaction)

    /**
     * Deletes a transaction by its ID
     */
    @Query("DELETE FROM transactions WHERE id = :id AND userId = :userId")
    suspend fun deleteById(id: Int, userId: Int)
}
