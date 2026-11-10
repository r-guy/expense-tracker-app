package com.example.test.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.data.UserSession
import com.example.test.data.dao.SettingsDao
import com.example.test.data.repository.AuthRepository
import com.example.test.model.Settings
import com.example.test.util.CurrencyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDao: SettingsDao,
    private val currencyFormatter: CurrencyFormatter,
    private val authRepository: AuthRepository,
    private val userSession: UserSession
) : ViewModel() {

    private val _firstDayOfMonth = MutableLiveData(1)
    val firstDayOfMonth: LiveData<Int> = _firstDayOfMonth

    private val _currency = MutableLiveData("USD")
    val currency: LiveData<String> = _currency

    private val _notificationsEnabled = MutableLiveData(true)
    val notificationsEnabled: LiveData<Boolean> = _notificationsEnabled

    private val _calendarType = MutableLiveData("GREGORIAN")
    val calendarType: LiveData<String> = _calendarType

    // Settings are stored one row per user, keyed by the user id.
    private var userId: Int = UserSession.NO_USER

    init {
        viewModelScope.launch {
            userId = userSession.requireUserId()
            val settings = settingsDao.getSettings(userId).first()
            if (settings == null) {
                val defaultSettings = Settings(id = userId)
                settingsDao.insert(defaultSettings)
                loadSettings(defaultSettings)
            } else {
                loadSettings(settings)
            }
        }
    }

    private fun loadSettings(settings: Settings) {
        _firstDayOfMonth.value = settings.firstDayOfMonth
        _currency.value = settings.currency
        _notificationsEnabled.value = settings.notificationsEnabled
        _calendarType.value = settings.calendarType

        // Set the primary currency in the formatter
        currencyFormatter.setPrimaryCurrency(settings.currency)
    }

    fun setFirstDayOfMonth(day: Int) {
        if (day in 1..31) {
            _firstDayOfMonth.value = day
            saveSettings()
        }
    }

    fun setCurrency(newCurrency: String) {
        _currency.value = newCurrency
        currencyFormatter.setPrimaryCurrency(newCurrency)
        saveSettings()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        saveSettings()
    }

    fun setCalendarType(type: String) {
        if (type in listOf("GREGORIAN", "PERSIAN")) {
            _calendarType.value = type
            saveSettings()
        }
    }

    fun getCurrencyFormatter(): CurrencyFormatter {
        return currencyFormatter
    }

    fun getLastExchangeRateUpdate(): String {
        return currencyFormatter.getLastUpdateTime()
    }

    /** Clears the stored session (token + user id), then reports completion. */
    fun signOut(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            authRepository.signOut()
            onDone()
        }
    }

    private fun saveSettings() {
        viewModelScope.launch {
            if (userId == UserSession.NO_USER) return@launch

            val settings = Settings(
                id = userId,
                firstDayOfMonth = _firstDayOfMonth.value ?: 1,
                currency = _currency.value ?: "USD",
                notificationsEnabled = _notificationsEnabled.value ?: true,
                calendarType = _calendarType.value ?: "GREGORIAN"
            )
            settingsDao.insert(settings)
        }
    }
}
