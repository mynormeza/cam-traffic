package com.port.camtraffic.db.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.JsonObject

@Entity(tableName = "traffic_cameras")
data class TrafficCamera (
    @PrimaryKey val id: String,
    val direction: String,
    val image: String,
    val region: String,
    val title: String,
    val description: String,
    val longitude: Float = 0f,
    val latitude: Float = 0f
) {

    @Ignore
    fun toJsonObject(): JsonObject {
        val obj = JsonObject()
        obj.addProperty("id", id)
        obj.addProperty("direction", direction)
        obj.addProperty("image", image)
        obj.addProperty("region", region)
        obj.addProperty("title", title)
        obj.addProperty("description", description)
        obj.addProperty("latitude", latitude)
        obj.addProperty("longitude", longitude)
        return obj
    }
}