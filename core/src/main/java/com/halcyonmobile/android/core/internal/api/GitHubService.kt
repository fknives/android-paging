package com.halcyonmobile.android.core.internal.api

import retrofit2.http.GET
import retrofit2.http.Query

internal interface GitHubService {

    @GET("https://api.github.com/users/JakeWharton/repos")
    suspend fun getReposPaginated(@Query("page") page: Int, @Query("per_page") perPage: Int): List<GitHubRepoResponse>
}