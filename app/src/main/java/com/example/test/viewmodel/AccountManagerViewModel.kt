package com.example.test.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.test.data.UserSession
import com.example.test.data.dao.AccountDao
import com.example.test.util.CurrencyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountManagerViewModel @Inject constructor(
    private val accountDao: AccountDao,
    private val userSession: UserSession,
    private val currencyFormatter: CurrencyFormatter
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val accounts = userSession.userId
        .flatMapLatest { userId -> accountDao.getAllAccounts(userId) }
        .asLiveData()

    fun deleteAccount(id: Int) = viewModelScope.launch {
        accountDao.deleteById(id, userSession.requireUserId())
    }

    fun formatWithDualCurrency(amount: Double, currencyCode: String): String {
        return currencyFormatter.formatWithDualCurrency(amount, currencyCode)
    }
}
