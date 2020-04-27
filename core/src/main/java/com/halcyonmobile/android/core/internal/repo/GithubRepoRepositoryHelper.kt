package com.halcyonmobile.android.core.internal.repo

import kotlinx.coroutines.flow.Flow

/**
 * Interface that should ease the communication between useCases and the actual repositories that implement it
 * [fetch] - intended for making a call to the remote source
 * [get] - intended for fetching the data that exists locally in the cache
 */
interface GithubRepoRepositoryHelper<T> {
    suspend fun fetch(numberOfElements: Int): Flow<List<T>>
    suspend fun get(numberOfElements: Int): Flow<List<T>>
}