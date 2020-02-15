package com.port.camtraffic.repositories

import com.port.camtraffic.db.AppDatabase
import com.port.camtraffic.db.entity.TrafficCamera
import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Action
import io.reactivex.functions.BiConsumer
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalRepository @Inject constructor (private val appDatabase: AppDatabase){
    fun getTrafficCameras() = appDatabase.trafficCameraDao().getList()
}