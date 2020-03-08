package com.port.camtraffic.ui.sync

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.port.camtraffic.repositories.NetworkRepository
import com.port.camtraffic.utils.SyncManager
import javax.inject.Inject


class SyncViewModel @Inject constructor(
    private val syncManager: SyncManager,
    private val networkRepository: NetworkRepository
) : ViewModel() {
    val syncState: LiveData<Boolean> = syncManager.syncState

    fun setSyncState(value: Boolean) {
        syncManager.setSyncState(value)
    }

    fun synchronize() = networkRepository.synchronize()
}
