package com.example.test.data.dao

import androidx.room.*
import com.example.test.model.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE userId = :userId ORDER BY name")
    fun getAllAccounts(userId: Int): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Int): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account)

    @Delete
    suspend fun delete(account: Account)

    @Query("DELETE FROM accounts WHERE id = :id AND userId = :userId")
    suspend fun deleteById(id: Int, userId: Int)

    /** Used to decide whether a new user needs the default "Cash" account. */
    @Query("SELECT COUNT(*) FROM accounts WHERE userId = :userId")
    suspend fun countForUser(userId: Int): Int
}
