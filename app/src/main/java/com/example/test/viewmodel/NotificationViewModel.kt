package com.example.test.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.test.data.UserSession
import com.example.test.data.dao.NotificationDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val dao: NotificationDao,
    private val userSession: UserSession
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val notifications = userSession.userId
        .flatMapLatest { userId -> dao.getAll(userId) }
        .asLiveData()

    fun markAsRead(id: Int) = viewModelScope.launch {
        dao.markAsRead(id)
    }

    fun clearAll() = viewModelScope.launch {
        dao.clearAll(userSession.requireUserId())
    }
}
