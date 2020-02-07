package com.port.camtraffic.preferences

import android.app.Application
import android.content.Context

class AppPreferences constructor(context: Application, stringId: Int) : CamTrafficPreferences {

    private val sharedPreferences = context.getSharedPreferences(context.getString(stringId),
        Context.MODE_PRIVATE)

    override fun setString(key: String, value: String) {
        with(sharedPreferences.edit()) {
            putString(key, value)
            apply()
        }
    }

    override fun getString(key: String) = sharedPreferences.getString(key,"")!!

    override fun setBoolean(key: String, value: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean(key, value)
            apply()
        }
    }

    override fun getBoolean(key: String) = sharedPreferences.getBoolean(key,false)
}