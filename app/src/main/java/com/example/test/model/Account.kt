package com.example.test.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val balance: Double,
    val type: AccountType, // Wallet, Bank, ...
    /**
     * Owner of this row. Rows created before multi-user support have userId 0
     * and are claimed by the first account that signs in (see UserDataSeeder).
     */
    @ColumnInfo(defaultValue = "0") val userId: Int = 0
)

enum class AccountType {
    BANK, WALLET, OTHER
}
