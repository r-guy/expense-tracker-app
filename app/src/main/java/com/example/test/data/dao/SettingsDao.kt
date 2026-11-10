package com.example.test.data.dao

import androidx.room.*
import com.example.test.model.Settings
import kotlinx.coroutines.flow.Flow

/**
 * Settings are stored one row per user, using the user id as the primary key.
 */
@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = :userId")
    fun getSettings(userId: Int): Flow<Settings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: Settings)

    @Update
    suspend fun update(settings: Settings)
}
