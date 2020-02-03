package com.port.camtraffic.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.port.camtraffic.AppViewModelFactory
import com.port.camtraffic.ui.main.MainViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelsModule {
    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    abstract fun bindMainViewModel(mainViewModel: MainViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(appViewModelFactory: AppViewModelFactory): ViewModelProvider.Factory
}