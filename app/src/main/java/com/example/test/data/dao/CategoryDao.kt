package com.example.test.data.dao


import androidx.room.*
import com.example.test.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :userId AND type = :type ORDER BY name")
    fun getCategoriesByType(userId: Int, type: String): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name")
    fun getAllCategories(userId: Int): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Int): Category?

    @Query("SELECT * FROM categories WHERE userId = :userId AND name = :name LIMIT 1")
    suspend fun getCategoryByName(userId: Int, name: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category)

    @Query("DELETE FROM categories WHERE id = :id AND userId = :userId")
    suspend fun deleteById(id: Int, userId: Int)

    @Query("SELECT name FROM categories WHERE id = :id")
    suspend fun getCategoryNameById(id: Int): String?

    /** Used to decide whether a new user needs the default categories. */
    @Query("SELECT COUNT(*) FROM categories WHERE userId = :userId")
    suspend fun countForUser(userId: Int): Int
}
