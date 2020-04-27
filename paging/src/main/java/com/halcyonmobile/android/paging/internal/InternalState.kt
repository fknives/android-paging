package com.halcyonmobile.android.paging.internal

import kotlinx.coroutines.flow.Flow

sealed class InternalState<T> {
    abstract val dataStream: Flow<List<T>>
    abstract val numberOfElements: Int

    class ShowLoadingInitial<T>(
        override val dataStream: Flow<List<T>>,
        override val numberOfElements: Int,
        val refresh: Boolean
    ) : InternalState<T>()

    class DoLoading<T> constructor(
        override val dataStream: Flow<List<T>>,
        override val numberOfElements: Int,
        val isInitial: Boolean,
        val refresh: Boolean
    ) : InternalState<T>()

    class InitialLoadingFailed<T>(
        override val dataStream: Flow<List<T>>,
        override val numberOfElements: Int,
        val refresh: Boolean,
        val cause: Throwable
    ) : InternalState<T>()

    class LoadingMoreFailed<T>(
        override val dataStream: Flow<List<T>>,
        override val numberOfElements: Int,
        val cause: Throwable
    ) : InternalState<T>()

    data class ShowLoadingMore<T>(
        override val dataStream: Flow<List<T>>,
        override val numberOfElements: Int
    ) : InternalState<T>()

    class DataAtPositionBound<T> constructor(
        override val dataStream: Flow<List<T>>,
        override val numberOfElements: Int
    ) : InternalState<T>()

    class DataLoaded<T> private constructor(
        override val dataStream: Flow<List<T>>,
        override val numberOfElements: Int,
        val isInitial: Boolean
    ) : InternalState<T>() {

        constructor(doLoading: DoLoading<T>, numberOfElements: Int, dataFlow: Flow<List<T>>) : this(
            dataFlow,
            numberOfElements,
            doLoading.isInitial
        )
    }
}