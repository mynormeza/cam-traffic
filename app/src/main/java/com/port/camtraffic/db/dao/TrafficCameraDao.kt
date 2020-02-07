package com.port.camtraffic.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.port.camtraffic.db.entity.TrafficCamera

@Dao
interface TrafficCameraDao {

    @Query("SELECT * FROM traffic_cameras")
    fun getList(): LiveData<List<TrafficCamera>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(trafficCameras: List<TrafficCamera>)

    @Insert
    fun insert(trafficCamera: TrafficCamera)

    @Delete
    fun delete(trafficCamera: TrafficCamera)
}