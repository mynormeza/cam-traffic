package com.port.camtraffic.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.mapbox.mapboxsdk.plugins.annotation.Circle
import com.port.camtraffic.db.entity.TrafficCamera
import com.port.camtraffic.repositories.LocalRepository
import com.port.camtraffic.utils.SyncManager
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val localRepository: LocalRepository,
    private val syncManager: SyncManager
) : ViewModel() {
    val poiList = localRepository.getTrafficCameras()
    val syncState: LiveData<Boolean> = syncManager.syncState
    var originCircle: Circle? = null
    var destinationCircle: Circle? = null
    var origin: TrafficCamera? = null
    var destination: TrafficCamera? = null
    var preRouteMode = false
}
