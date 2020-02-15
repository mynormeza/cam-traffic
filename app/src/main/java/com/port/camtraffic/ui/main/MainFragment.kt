package com.port.camtraffic.ui.main

import android.content.Context
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsResponse
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
import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCamera
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.mapbox.services.android.navigation.v5.navigation.camera.Camera
import com.mapbox.services.android.navigation.v5.navigation.camera.RouteInformation
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.port.camtraffic.MainActivity
import com.port.camtraffic.R
import com.port.camtraffic.databinding.BottomSheetBinding
import com.port.camtraffic.databinding.RouteViewBinding
import com.port.camtraffic.db.entity.TrafficCamera
import com.port.camtraffic.utils.FactoryFunctions.toTrafficCamera
import com.port.camtraffic.utils.toClassObject
import dagger.android.support.AndroidSupportInjection
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.lang.Exception
import java.lang.ref.WeakReference
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
    private var gpsEnabled = false
    private var isNavigating = false

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        routeViewBinding = RouteViewBinding.bind(toolbar.findViewById(R.id.route_view))
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

        bottomSheetBinding.startNav?.setOnClickListener {
            isNavigating = true
            routeNavigation.addProgressChangeListener(this)
            routeViewBinding.navRouteActive = true
            bottomSheetBinding.routeDetails?.visibility  = View.GONE
            bottomSheetBinding.hint?.visibility = View.GONE
            bottomSheetBinding.finish?.visibility = View.VISIBLE
        }
        bottomSheetBinding.finish?.setOnClickListener {
            routeNavigation.removeProgressChangeListener(null)
            routeViewBinding.navRouteActive = false
            bottomSheetBinding.poiDetails?.visibility = View.VISIBLE
            bottomSheetBinding.finish?.visibility = View.GONE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        bottomSheetBinding.poiDirections?.setOnClickListener {
            directionsClick()
        }
        bottomSheetBehavior.addBottomSheetCallback( object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if ( newState == BottomSheetBehavior.STATE_COLLAPSED){
                    clearAll()
                }
            }
        })

        viewModel.syncState.observe(this, Observer {
            if (it){
                Toast.makeText(activity,getString(R.string.data_sync), Toast.LENGTH_SHORT).show()
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
        Toast.makeText(activity, "You need permission", Toast.LENGTH_SHORT).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            mapboxMap.getStyle {
                enableLocation(it)
            }
        }else {
            Toast.makeText(activity, "You need permission 2", Toast.LENGTH_SHORT).show()
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

    override fun onProgressChange(location: Location?, routeProgress: RouteProgress?) {
        getRoute(Point.fromLngLat(location?.longitude!!,location.latitude))
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
                if (!isNavigating) routeNavigation.startNavigation(route)
                setRoute(it)

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
                PropertyFactory.lineColor(Color.parseColor("#e55e5e")))
            style.addLayer(routeLayer)
        }
    }

    private fun poiClick(clickedCircle: Circle) {
        gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (viewModel.preRouteMode && !gpsEnabled) {

            viewModel.originCircle?.let {circle ->
                circle.circleRadius = 8f
                circleManager.update(circle)
            }
            viewModel.originCircle = clickedCircle
            viewModel.origin = clickedCircle.data?.asJsonObject?.toClassObject(::toTrafficCamera)
            routeViewBinding.origin = viewModel.origin  ?.title
        }else {
            viewModel.destinationCircle?.let {circle ->
                circle.circleRadius = 8f
                circleManager.update(circle)
            }
            viewModel.destinationCircle = clickedCircle
            viewModel.destination = clickedCircle.data?.asJsonObject?.toClassObject(::toTrafficCamera)
            routeViewBinding.destination = viewModel.destination?.title
            routeViewBinding.executePendingBindings()
        }


        bottomSheetBinding.poi = viewModel.destination
        bottomSheetBinding.executePendingBindings()
        clickedCircle.circleRadius = 12f
        circleManager.update(clickedCircle)
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
        }
        if (viewModel.isRouteValid()) {
            val ori=  Point.fromLngLat(viewModel.origin?.longitude!!.toDouble(), viewModel.origin?.latitude!!.toDouble())
            getRoute(ori)
        }
    }

    private fun getRoute(ori: Point) {
        val des = Point.fromLngLat(viewModel.destination?.longitude!!.toDouble(), viewModel.destination?.latitude!!.toDouble())

        NavigationRoute.builder(context)
            .accessToken(getString(R.string.mapbox_access_token))
            .origin(ori)
            .destination(des)
            .build()
            .getRoute(this)
    }

    private fun directionsClick() {
        viewModel.preRouteMode = true
        gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (gpsEnabled) {
            getRoute(getCurrentLocation())
            routeLayer.setProperties(visibility(VISIBLE))
            routeViewBinding.origin = getString(R.string.current_location)
        }
        bottomSheetBinding.poiDetails?.visibility = View.GONE
        viewModel.destination?.let {
            if (gpsEnabled){
                bottomSheetBinding.routeDetails?.visibility  = View.VISIBLE
            }else {
                bottomSheetBinding.hint?.visibility = View.VISIBLE
            }
            routeViewBinding.destination = it.title
        }
        toolbar.visibility = View.VISIBLE
        routeViewBinding.executePendingBindings()
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
        isNavigating = false
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

    private fun getCurrentLocation(): Point {
        return locationComponent.lastKnownLocation?.let {
            Point.fromLngLat(it.longitude, it.latitude)
        }!!
    }

    private fun setRoute(directionsResponse: DirectionsResponse){
        val routes = directionsResponse.routes()
        if (routes.isNotEmpty()) {
            val route = routes[0]
            if (!isNavigating) {
                val distance = route.distance().toString()
                val duration = TimeUnit.SECONDS.toMinutes(route.duration()?.toLong()!!).toString()
                routeLayer.setProperties(visibility(VISIBLE))
                bottomSheetBinding.distance = getString(R.string.route_distance, duration, distance)
                bottomSheetBinding.hint?.visibility = View.GONE
                bottomSheetBinding.routeDetails?.visibility = View.VISIBLE
                bottomSheetBinding.startNav?.isEnabled = gpsEnabled
            }

            route.geometry()?.let {
                val lineString = LineString.fromPolyline(it, Constants.PRECISION_6)
                routeSource.setGeoJson(lineString)
            }
        }
    }
}
