package com.port.camtraffic

import android.app.Application
import com.mapbox.mapboxsdk.Mapbox
import com.port.camtraffic.di.DaggerAppComponent
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import timber.log.Timber
import javax.inject.Inject

class CamTrafficApp : Application (), HasAndroidInjector {

    @Inject lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG){
            Timber.plant(Timber.DebugTree())
        }
        DaggerAppComponent.builder().application(this).build().inject(this)
        Mapbox.getInstance(applicationContext, getString(R.string.mapbox_access_token))
    }

    override fun androidInjector() = dispatchingAndroidInjector
}