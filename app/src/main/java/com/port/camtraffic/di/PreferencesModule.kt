package com.port.camtraffic.di

import android.app.Application
import com.port.camtraffic.R
import com.port.camtraffic.preferences.CamTrafficPreferences
import com.port.camtraffic.preferences.AppPreferences
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object PreferencesModule {
    @Singleton
    @SyncPreferences
    @Provides
    fun providesSyncPreference(context: Application): CamTrafficPreferences {
        return AppPreferences(context, R.string.preferences)
    }
}