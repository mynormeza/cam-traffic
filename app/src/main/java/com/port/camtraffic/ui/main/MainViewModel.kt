package com.port.camtraffic.ui.main

import androidx.lifecycle.ViewModel
import com.port.camtraffic.repositories.LocalRepository
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val localRepository: LocalRepository
) : ViewModel() {
    val pioList = localRepository.getTrafficCameras()
}
