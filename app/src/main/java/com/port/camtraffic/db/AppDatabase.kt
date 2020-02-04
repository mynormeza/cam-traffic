package com.port.camtraffic.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.port.camtraffic.db.dao.TrafficCameraDao
import com.port.camtraffic.db.entity.TrafficCamera

@Database(entities = [TrafficCamera::class], version = 2)
abstract class AppDatabase : RoomDatabase(){
    abstract fun trafficCameraDao(): TrafficCameraDao
}