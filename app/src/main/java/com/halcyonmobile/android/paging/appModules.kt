package com.halcyonmobile.android.paging

import com.halcyonmobile.android.core.createCoreModules
import com.halcyonmobile.android.paging.ui.main.MainViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

fun appModules(): List<Module> = createCoreModules() + module {
    viewModel { MainViewModel(get()) }
}