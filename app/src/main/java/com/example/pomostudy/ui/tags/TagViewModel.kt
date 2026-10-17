package com.example.pomostudy.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pomostudy.data.local.entity.TagEntity
import com.example.pomostudy.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 标签管理 ViewModel。 */
class TagViewModel(private val container: AppContainer) : ViewModel() {

    val tags: StateFlow<List<TagEntity>> = container.tagRepository.tags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(name: String, color: Long) = viewModelScope.launch {
        container.tagRepository.add(name, color)
    }

    fun update(tag: TagEntity) = viewModelScope.launch {
        container.tagRepository.update(tag)
    }

    /** 删除标签（关联记录与待办的标签字段由数据库外键自动置空）。 */
    fun delete(id: Long) = viewModelScope.launch {
        container.tagRepository.delete(id)
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { TagViewModel(container) }
        }
    }
}
