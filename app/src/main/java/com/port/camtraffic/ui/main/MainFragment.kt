package com.port.camtraffic.ui.main

import android.content.Context
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.*
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.Property.NONE
import com.mapbox.mapboxsdk.style.layers.Property.VISIBLE
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.port.camtraffic.R
import com.port.camtraffic.databinding.BottomSheetBinding
import com.port.camtraffic.databinding.RouteViewBinding
import com.port.camtraffic.db.entity.TrafficCamera
import com.port.camtraffic.extensions.toClassObject
import com.port.camtraffic.extensions.toast
import com.port.camtraffic.utils.FactoryFunctions.toTrafficCamera
import dagger.android.support.AndroidSupportInjection
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MainFragment : Fragment(), OnMapReadyCallback, PermissionsListener, Callback<DirectionsResponse>, ProgressChangeListener {

    companion object{
        const val ROUTE_SOURCE_ID = "route-source"
        const val ROUTE_LAYER_ID = "route-layer"
    }

    @Inject lateinit var factory: ViewModelProvider.Factory
    private val viewModel: MainViewModel by viewModels { factory }
    private var mapView: MapView? = null
    private lateinit var mapboxMap: MapboxMap
    private lateinit var locationManager: LocationManager
    private lateinit var permissionManager: PermissionsManager
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var circleManager: CircleManager
    private lateinit var bottomSheetBinding: BottomSheetBinding
    private lateinit var routeViewBinding: RouteViewBinding
    private lateinit var toolbar: Toolbar
    private lateinit var routeSource: GeoJsonSource
    private lateinit var routeLayer: LineLayer
    private lateinit var locationComponent: LocationComponent
    private lateinit var routeNavigation: MapboxNavigation
    private lateinit var currentRoute: DirectionsRoute
    private var gpsEnabled = false

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        locationManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        routeNavigation = MapboxNavigation(context!!, getString(R.string.mapbox_access_token))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.main_fragment, container, false)
        val bottomSheetLayout = root.findViewById<FrameLayout>(R.id.bottom_sheet)
        toolbar = activity?.findViewById(R.id.toolbar)!!
        bottomSheetBinding = BottomSheetBinding.bind(bottomSheetLayout)
        routeViewBinding = DataBindingUtil.bind(toolbar.findViewById(R.id.route_view))!!
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)


        mapView = root.findViewById(R.id.map_view)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuIcon = ContextCompat.getDrawable(context!!, R.drawable.ic_close)
        toolbar.navigationIcon = menuIcon
        toolbar.visibility = View.GONE
        routeViewBinding.viewmodel = viewModel
        routeViewBinding.executePendingBindings()
        bottomSheetBinding.viewmodel = viewModel
        bottomSheetBinding.executePendingBindings()

        bottomSheetBinding.startNav.setOnClickListener {
            routeNavigation.addProgressChangeListener(this)
            routeNavigation.startNavigation(currentRoute)
            viewModel.routeState = RouteState.NAVIGATING
        }
        bottomSheetBinding.finish.setOnClickListener {
            routeNavigation.removeProgressChangeListener(null)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            clearAll()
        }
        bottomSheetBinding.poiDirections.setOnClickListener {
            directionsClick()
        }
        bottomSheetBehavior.addBottomSheetCallback( object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED){
                    clearAll()
                }
            }
        })

        viewModel.syncState.observe(this, Observer {
            if (it){
                context?.toast(getString(R.string.data_sync))
            } else {
                findNavController().navigate(R.id.sync_fragment)
            }
        })
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        viewModel.poiList.observe(this, Observer {
            setPois(it)
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionManager.onRequestPermissionsResult(requestCode,permissions,grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        context?.toast(getString(R.string.explain_permission))
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            mapboxMap.getStyle {
                enableLocation(it)
            }
        }else {
            context?.toast(getString(R.string.permission))
            activity?.finish()
        }
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDestroy()

    }

    override fun onOptionsItemSelected(item: MenuItem)= when(item.itemId){
        android.R.id.home -> {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            clearAll()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onProgressChange(location: Location?, routeProgress: RouteProgress?) {
        location?.let {
            getRoute(Point.fromLngLat(it.longitude,it.latitude))
        }
    }

    override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
        Timber.e(t)
    }

    override fun onResponse(
        call: Call<DirectionsResponse>,
        response: Response<DirectionsResponse>
    ) {
        response.body()?.let {
            val routes = it.routes()
            if (routes.isNotEmpty()){
                val route = routes[0]
                setRoute(route)
            }
        }
    }

    private fun setPois(pois: List<TrafficCamera>) {
        val circleOptions = ArrayList<CircleOptions>()
        pois.forEach {
            circleOptions.add(CircleOptions()
                .withData(it.toJsonObject())
                .withLatLng( LatLng(it.latitude.toDouble(), it.longitude.toDouble()))
                .withCircleRadius(8f)
                .withCircleColor("#247835")
            )
        }

        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            enableLocation(style)
            circleManager =  CircleManager(mapView!!, mapboxMap, style)

            circleManager.addClickListener {
                poiClick(it)
            }
            circleManager.create(circleOptions)
            routeSource = GeoJsonSource(ROUTE_SOURCE_ID)
            style.addSource(routeSource)
            routeLayer = LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                PropertyFactory.lineDasharray( arrayOf(0.01f, 2f)),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineWidth(5f),
                PropertyFactory.lineColor(Color.parseColor("#4287f5")))
            style.addLayer(routeLayer)
        }
    }

    private fun poiClick(clickedCircle: Circle) {
        if (viewModel.needsOriginPoint()) {
            viewModel.origin = clickedCircle.data?.asJsonObject?.toClassObject(::toTrafficCamera)
            if (viewModel.isOriginValid()) {
                context?.toast(getString(R.string.origin_invalid))
                viewModel.origin = null
                return
            }
            viewModel.originCircle?.let {circle ->
                circle.circleRadius = 8f
                circleManager.update(circle)
            }
            viewModel.originCircle = clickedCircle

        }else {
            viewModel.destinationCircle?.let {circle ->
                circle.circleRadius = 8f
                circleManager.update(circle)
            }
            viewModel.destinationCircle = clickedCircle
            viewModel.destination = clickedCircle.data?.asJsonObject?.toClassObject(::toTrafficCamera)
        }

        clickedCircle.circleRadius = 12f
        circleManager.update(clickedCircle)

        viewModel.origin?.let {
            viewModel.routeState = RouteState.NO_GPS_ROUTE
            val ori=  Point.fromLngLat(it.longitude.toDouble(), it.latitude.toDouble())
            getRoute(ori)
        }

        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun getRoute(ori: Point) {
        viewModel.destination?.let {
            val des = Point.fromLngLat(it.longitude.toDouble(), it.latitude.toDouble())

            NavigationRoute.builder(context)
                .accessToken(getString(R.string.mapbox_access_token))
                .origin(ori)
                .destination(des)
                .build()
                .getRoute(this)
        }
    }

    private fun directionsClick() {
        gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        viewModel.destination?.let {
            if (gpsEnabled){
                getCurrentLocation()?.let {
                    getRoute(it)
                }
                routeLayer.setProperties(visibility(VISIBLE))
                viewModel.routeState = RouteState.GPS_PRE_ROUTE
            }else {
                viewModel.routeState = RouteState.NO_GPS_PRE_ROUTE
            }
        }
        toolbar.visibility = View.VISIBLE
    }

    private fun enableLocation(loadedStyle: Style) {
        if (PermissionsManager.areLocationPermissionsGranted(context)){
            locationComponent = mapboxMap.locationComponent
            context?.let {
                locationComponent.activateLocationComponent(LocationComponentActivationOptions.builder(it, loadedStyle).build())
            }
            locationComponent.isLocationComponentEnabled = true
            locationComponent.cameraMode = CameraMode.TRACKING
            locationComponent.renderMode = RenderMode.NORMAL
        }else {
            permissionManager = PermissionsManager(this)
            permissionManager.requestLocationPermissions(activity)
        }
    }

    private fun clearAll() {
        routeNavigation.stopNavigation()
        toolbar.visibility = View.GONE
        viewModel.originCircle?.let {
            it.circleRadius = 8f
            circleManager.update(it)
        }
        viewModel.destinationCircle?.let {
            it.circleRadius = 8f
            circleManager.update(it)
        }
        routeLayer.setProperties(visibility(NONE))
        viewModel.clearRoute()
    }

    private fun getCurrentLocation(): Point? {
        return locationComponent.lastKnownLocation?.let {
            Point.fromLngLat(it.longitude, it.latitude)
        }
    }

    private fun setRoute(route: DirectionsRoute){
        if (!viewModel.isNavigating()) {
            currentRoute = route
            val duration = with(route.duration()) {
                if (this != null)  TimeUnit.SECONDS.toMinutes(this.toLong()).toString() else ""
            }
            val distance = route.distance().toString()
            routeLayer.setProperties(visibility(VISIBLE))
            viewModel.distance = getString(R.string.route_distance, duration, distance)
        }

        route.geometry()?.let {
            val lineString = LineString.fromPolyline(it, Constants.PRECISION_6)
            routeSource.setGeoJson(lineString)
        }
    }
}
