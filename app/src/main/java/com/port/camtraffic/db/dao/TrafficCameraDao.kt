package com.port.camtraffic.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.port.camtraffic.db.entity.TrafficCamera

@Dao
interface TrafficCameraDao {

    @Query("SELECT * FROM traffic_cameras")
    fun getList(): LiveData<List<TrafficCamera>>

    @Insert
    fun insert(trafficCameras: List<TrafficCamera>)

    @Insert
    fun insert(trafficCamera: TrafficCamera)

    @Delete
    fun delete(trafficCamera: TrafficCamera)
}