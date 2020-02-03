package com.port.camtraffic.di

import com.port.camtraffic.MainActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class AppActivityModule {
    @ContributesAndroidInjector(modules = [FragmentsBuilderModule::class])
    abstract fun contributeMainActivity(): MainActivity
}