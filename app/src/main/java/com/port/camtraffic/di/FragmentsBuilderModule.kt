package com.port.camtraffic.di

import com.port.camtraffic.ui.main.MainFragment
import com.port.camtraffic.ui.sync.SyncFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class FragmentsBuilderModule{
    @ContributesAndroidInjector
    abstract fun contributeMainFragment(): MainFragment

    @ContributesAndroidInjector
    abstract fun contributeSyncFragment(): SyncFragment
}