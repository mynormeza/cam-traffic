package com.port.camtraffic.extensions

import com.google.gson.JsonObject

fun <T> JsonObject.toClassObject(factory: (JsonObject) -> T): T {
    return factory(this)
}
