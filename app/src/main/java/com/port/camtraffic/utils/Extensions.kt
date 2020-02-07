package com.port.camtraffic.utils

import com.google.gson.JsonObject
import com.port.camtraffic.db.entity.TrafficCamera

fun <T> JsonObject.toClassObject(factory: (JsonObject) -> T): T {
    return factory(this)
}

object FactoryFunctions {
    fun toTrafficCamera(json: JsonObject) = TrafficCamera(
        json.get("id").asString,
        json.get("direction").asString,
        json.get("image").asString,
        json.get("region").asString,
        json.get("title").asString,
        json.get("description").asString
    )
}