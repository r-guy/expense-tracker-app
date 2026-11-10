package com.example.test.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.test.data.UserSession
import com.example.test.data.dao.CategoryDao
import com.example.test.model.CategoryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryManagerViewModel @Inject constructor(
    private val categoryDao: CategoryDao,
    private val userSession: UserSession
) : ViewModel() {
    private val _currentType = MutableLiveData(CategoryType.EXPENSE)
    val currentType: LiveData<CategoryType> = _currentType

    @OptIn(ExperimentalCoroutinesApi::class)
    val categories = currentType.switchMap { type ->
        userSession.userId
            .flatMapLatest { userId -> categoryDao.getCategoriesByType(userId, type.toString()) }
            .asLiveData()
    }

    fun deleteCategory(id: Int) = viewModelScope.launch {
        categoryDao.deleteById(id, userSession.requireUserId())
    }

    fun switchType(type: CategoryType) {
        _currentType.value = type
    }
}
