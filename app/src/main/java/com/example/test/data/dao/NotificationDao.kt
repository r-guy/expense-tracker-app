package com.example.test.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.test.model.Notification
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object interface for Notification entities
 * Provides methods to perform database operations on notification records
 */
@Dao
interface NotificationDao {
    /**
     * Retrieves all notifications for one user ordered by date (newest first)
     */
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY date DESC")
    fun getAll(userId: Int): Flow<List<Notification>>

    /**
     * Inserts a new notification into the database
     */
    @Insert
    suspend fun insert(notification: Notification)

    /**
     * Marks a notification as read by updating its read flag
     */
    @Query("UPDATE notifications SET read = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)

    /**
     * Deletes all notifications belonging to one user
     */
    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun clearAll(userId: Int)
}
