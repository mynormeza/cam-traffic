package com.port.camtraffic.di

import com.port.camtraffic.api.WebApi
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module(includes = [ViewModelsModule::class])
object AppModule {

    @Provides @Singleton
    fun providesWebApi() = Retrofit.Builder()
        .baseUrl("https://javieraragon.carto.com/api/v2/")
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()
        .create(WebApi::class.java)

}