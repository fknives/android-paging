package com.halcyonmobile.android

import androidx.lifecycle.LiveData
import com.halcyonmobile.android.ViewModelDelegateInterface.ViewState

/**
 * The viewModel class should implement this interface such that it can rely on these overridden properties for keeping track of
 * [state] - current state of the screen, which can be one of the values from [ViewState]
 * [data] - current data
 * [fetch] - action for fetching the data
 * [retry] - retry action for the cases when the fetch action fails
 */
interface ViewModelDelegateInterface<T> {

    val state: LiveData<ViewState>
    val data: LiveData<List<T>>

    fun fetch()
    fun retry()

    enum class ViewState {
        LOADING,
        REFRESHING,
        LOADED,
        LOADING_MORE,
        ERROR,
        ERROR_LOADING_MORE,
        EMPTY
    }
}