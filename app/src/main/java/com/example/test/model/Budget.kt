package com.example.test.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int?, // null = overall budget
    val amountLimit: Double,
    val startDate: String, // "YYYY-MM-DD" format
    val endDate: String, // "YYYY-MM-DD" format
    val repeat: Int = 1, // How many times to repeat this budget for future months (1 = no repeat)
    /** Owner of this row; 0 means "not claimed yet" (legacy data). */
    @ColumnInfo(defaultValue = "0") val userId: Int = 0
)
