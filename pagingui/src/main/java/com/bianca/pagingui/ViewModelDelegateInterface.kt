package com.bianca.pagingui

import androidx.lifecycle.LiveData

interface ViewModelDelegateInterface<T> {

    val state: LiveData<ViewState>
    val data: LiveData<List<T>>

    fun fetch(): List<T>
    fun retry(): List<T>

    enum class ViewState {
        LOADING,
        REFRESHING,
        LOADED,
        ERROR
    }
}