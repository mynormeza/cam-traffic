package com.port.camtraffic.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.port.camtraffic.repositories.LocalRepository
import com.port.camtraffic.utils.SyncManager
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val localRepository: LocalRepository,
    private val syncManager: SyncManager
) : ViewModel() {
    val poiList = localRepository.getTrafficCameras()
    val syncState: LiveData<Boolean> = syncManager.syncState
}
