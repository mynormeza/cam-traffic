package com.port.camtraffic.api

import com.port.camtraffic.TrafficCamera
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface WebApi {
    @GET("sql")
    fun loadPio(@Query("q") q: String): Single<ApiResponse<TrafficCamera>>
}