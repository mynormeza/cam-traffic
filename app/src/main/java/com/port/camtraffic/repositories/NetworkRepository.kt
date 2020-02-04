package com.port.camtraffic.repositories

import com.port.camtraffic.api.WebApi
import com.port.camtraffic.db.AppDatabase
import io.reactivex.Single
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val api: WebApi
) {
    fun sincronize( ): Single<Boolean>{
        val trafficCameras = api.loadPio("SELECT id, direction, href as image, region, title, view as description, ST_X(the_geom) as longitude, ST_Y(the_geom) as latitude FROM ios_test")

        return trafficCameras.flatMap {
            Single.create<Boolean> {emitter ->
                try {
                    appDatabase.trafficCameraDao().insert(it.rows)
                    emitter.onSuccess(true)
                }catch (e: Exception){
                    emitter.onError(e)
                }
            }
        }
    }
}