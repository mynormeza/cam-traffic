package com.port.camtraffic.ui.main

import android.text.Spanned
import android.view.View
import androidx.databinding.Bindable
import androidx.databinding.library.baseAdapters.BR
import androidx.lifecycle.LiveData
import com.mapbox.mapboxsdk.plugins.annotation.Circle
import com.port.camtraffic.binding.BindableProperty
import com.port.camtraffic.binding.ObservableViewModel
import com.port.camtraffic.db.entity.TrafficCamera
import com.port.camtraffic.repositories.LocalRepository
import com.port.camtraffic.utils.SyncManager
import javax.inject.Inject
import kotlin.properties.Delegates

class MainViewModel @Inject constructor(
    private val localRepository: LocalRepository,
    private val syncManager: SyncManager
) : ObservableViewModel() {
    val poiList = localRepository.getTrafficCameras()
    val syncState: LiveData<Boolean> = syncManager.syncState
    var originCircle: Circle? = null
    var destinationCircle: Circle? = null

    @get:Bindable
    var origin: TrafficCamera? by BindableProperty(null, this, BR.origin)

    @get:Bindable
    var destination: TrafficCamera? by BindableProperty(null, this, BR.destination)

    @get:Bindable
    var routeDetailsVisibility by BindableProperty(View.GONE, this, BR.routeDetailsVisibility)

    @get:Bindable
    var poiDetailsVisibility by BindableProperty(View.VISIBLE, this, BR.poiDetailsVisibility)

    @get:Bindable
    var navigatingVisibility by BindableProperty(View.GONE, this, BR.navigatingVisibility)

    @get:Bindable
    var routeHintVisibity by BindableProperty(View.GONE, this, BR.routeHintVisibity)

    @get:Bindable
    var loadingVisibility by BindableProperty(View.GONE, this, BR.loadingVisibility)

    @get:Bindable
    var startBtnEnabled by BindableProperty(true, this, BR.startBtnEnabled)

    @get:Bindable
    var distance by BindableProperty<Spanned?>(null, this, BR.distance)

    @get:Bindable
    var originString: String? by BindableProperty(null, this, BR.originString)

    var routeState by Delegates.observable(RouteState.POI_DETAILS) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            when(newValue) {
                RouteState.LOADING -> {
                    loadingVisibility = View.VISIBLE
                    poiDetailsVisibility = View.GONE
                    routeHintVisibity = View.GONE
                }
                RouteState.NO_GPS_PRE_ROUTE -> {
                    routeHintVisibity = View.VISIBLE
                    poiDetailsVisibility = View.GONE
                    originString = ""
                }
                RouteState.NO_GPS_ROUTE -> {
                    loadingVisibility = View.GONE
                    routeDetailsVisibility = View.VISIBLE
                    startBtnEnabled = false
                    originString = origin?.title
                }
                RouteState.GPS_ROUTE -> {
                    loadingVisibility = View.GONE
                    routeDetailsVisibility = View.VISIBLE
                    startBtnEnabled = true
                    originString = null
                }
                RouteState.NAVIGATING -> {
                    routeDetailsVisibility = View.GONE
                    navigatingVisibility = View.VISIBLE
                }
                RouteState.POI_DETAILS -> {
                    poiDetailsVisibility = View.VISIBLE
                }
                RouteState.NONE -> {
                    poiDetailsVisibility = View.GONE
                    navigatingVisibility = View.GONE
                    routeDetailsVisibility = View.GONE
                    routeHintVisibity = View.GONE
                }
            }
        }
    }

    fun clearRoute() {
        routeState = RouteState.NONE
        origin = null
        destination = null
        destinationCircle = null
        originCircle = null
    }

    fun isNavigating() = routeState == RouteState.NAVIGATING

    fun isLoadingState() = routeState == RouteState.LOADING

    fun needsOriginPoint() = routeState == RouteState.NO_GPS_PRE_ROUTE

    fun isOriginValid() = origin == destination
}
