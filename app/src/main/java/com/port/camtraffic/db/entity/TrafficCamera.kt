package com.port.camtraffic.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "traffic_cameras")
class TrafficCamera (
    @PrimaryKey val id: String,
    val direction: String,
    val image: String,
    val region: String,
    val title: String,
    val description: String,
    val longitude: Float,
    val latitude: Float
)