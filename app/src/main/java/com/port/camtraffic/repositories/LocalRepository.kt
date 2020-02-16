package com.port.camtraffic.repositories

import com.port.camtraffic.db.AppDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalRepository @Inject constructor (private val appDatabase: AppDatabase){
    fun getTrafficCameras() = appDatabase.trafficCameraDao().getList()
}