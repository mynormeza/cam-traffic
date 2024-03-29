package com.port.camtraffic.api

import com.port.camtraffic.db.entity.TrafficCamera
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface WebApi {
    @GET("sql")
    fun loadPoi(@Query("q") q: String): Single<ApiResponse<TrafficCamera>>
}