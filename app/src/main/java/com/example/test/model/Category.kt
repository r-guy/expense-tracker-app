package com.example.test.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: CategoryType,
    val color: String = "#000000",
    /** Owner of this row; 0 means "not claimed yet" (legacy data). */
    @ColumnInfo(defaultValue = "0") val userId: Int = 0
)

enum class CategoryType {
    EXPENSE, INCOME
}
