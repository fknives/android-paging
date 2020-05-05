package com.halcyonmobile.android.core

import com.halcyonmobile.android.core.internal.api.GitHubRepoRemoteSource
import com.halcyonmobile.android.core.internal.api.GitHubService
import com.halcyonmobile.android.core.internal.localsource.GitHubRepoLocalSource
import com.halcyonmobile.android.core.internal.repo.GitHubRepoRepository
import com.halcyonmobile.android.core.internal.usecase.GetGitHubReposPaginatedUseCase
import com.halcyonmobile.android.paging.repo.log.DebugErrorLogger
import com.squareup.moshi.Moshi
import org.koin.core.module.Module
import org.koin.dsl.module
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

fun createCoreModules(baseUrl: String = "https://api.github.com/"): List<Module> =
    listOf(
        createNetworkModule(baseUrl),
        createRepoModule(),
        createCacheModule(),
        createUseCaseModule(),
        createLoggerModule()
    )

internal fun createNetworkModule(baseUrl: String) = module {
    single(override = true) {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(get())
            .build()
    }
    single(override = true) { Moshi.Builder().build() }
    factory<Converter.Factory> { MoshiConverterFactory.create(get()) }

    factory { get<Retrofit>().create(GitHubService::class.java) }
    factory { GitHubRepoRemoteSource(get()) }
}

internal fun createCacheModule() = module {
    factory { GitHubRepoLocalSource() }
}

internal fun createRepoModule() = module {
    single(override = true) { GitHubRepoRepository(get(), get(), get<DebugErrorLogger>()) }
}

internal fun createUseCaseModule() = module {
    factory { GetGitHubReposPaginatedUseCase(get<GitHubRepoRepository>()) }
}

internal fun createLoggerModule() = module {
    single { DebugErrorLogger() }
}