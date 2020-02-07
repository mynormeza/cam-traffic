package com.port.camtraffic.utils

import androidx.lifecycle.MutableLiveData
import com.port.camtraffic.di.SyncPreferences
import com.port.camtraffic.preferences.CamTrafficPreferences
import javax.inject.Inject
import javax.inject.Singleton

const val PREF_SYNC_KEY = "pref_sync_key"

@Singleton
class SyncManager @Inject constructor(@SyncPreferences private val preferences: CamTrafficPreferences) {
    private val _syncState = MutableLiveData<Boolean>()
    val syncState
        get() = _syncState

    init {
        _syncState.value = preferences.getBoolean(PREF_SYNC_KEY)
    }

    fun setSyncState(sync: Boolean) {
        preferences.setBoolean(PREF_SYNC_KEY, sync)
        _syncState.value = sync
    }
}