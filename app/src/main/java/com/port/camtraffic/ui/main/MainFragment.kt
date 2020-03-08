package com.port.camtraffic.ui.main

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.FrameLayout
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
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
import com.mapbox.mapboxsdk.maps.widgets.CompassView
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import com.bumptech.glide.Glide
import com.mapbox.mapboxsdk.R as RMB
import com.port.camtraffic.R
import com.port.camtraffic.databinding.BottomSheetBinding
import com.port.camtraffic.databinding.RouteViewBinding
import com.port.camtraffic.db.entity.TrafficCamera
import com.port.camtraffic.extensions.setFullScreen
import com.port.camtraffic.extensions.showOnlyStatusBar
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

class MainFragment : Fragment(), OnMapReadyCallback, Callback<DirectionsResponse>,
    ProgressChangeListener {

    companion object{
        const val ROUTE_SOURCE_ID = "route-source"
        const val ROUTE_LAYER_ID = "route-layer"
    }

    interface OnRequestLocation {
        fun onEnableLocation(mapboxMap: MapboxMap): LocationComponent?
    }

    @Inject lateinit var factory: ViewModelProvider.Factory
    private lateinit var callback: OnRequestLocation
    private val viewModel: MainViewModel by viewModels { factory }
    private var mapView: MapView? = null
    private lateinit var mapboxMap: MapboxMap
    private lateinit var locationManager: LocationManager
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var circleManager: CircleManager
    private lateinit var bottomSheetBinding: BottomSheetBinding
    private lateinit var routeViewBinding: RouteViewBinding
    private lateinit var toolbar: Toolbar
    private lateinit var routeSource: GeoJsonSource
    private lateinit var routeLayer: LineLayer
    private var locationComponent: LocationComponent? = null
    private lateinit var routeNavigation: MapboxNavigation
    private lateinit var bottomSheetLayout: FrameLayout
    //TODO: Current location is not initialize when cross sea location to a destination
    private lateinit var currentRoute: DirectionsRoute
    private var gpsEnabled = false

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        callback = context as OnRequestLocation
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
        bottomSheetLayout = root.findViewById(R.id.bottom_sheet)
        toolbar = activity?.findViewById(R.id.toolbar)!!
        bottomSheetBinding = BottomSheetBinding.bind(bottomSheetLayout)
        routeViewBinding = DataBindingUtil.bind(toolbar.findViewById(R.id.route_view))!!
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)
        (routeViewBinding.root as ViewGroup).layoutTransition
            .enableTransitionType(LayoutTransition.CHANGING)

        mapView = root.findViewById(R.id.map_view)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.visibility = View.GONE
        routeViewBinding.viewmodel = viewModel
        routeViewBinding.executePendingBindings()
        bottomSheetBinding.viewmodel = viewModel
        bottomSheetBinding.executePendingBindings()

        routeViewBinding.btnClose.setOnClickListener {
            clearAll()
            bottomSheetBinding.executePendingBindings()
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        bottomSheetBinding.startNav.setOnClickListener {
            routeNavigation.addProgressChangeListener(this)
            routeNavigation.startNavigation(currentRoute)
            viewModel.routeState = RouteState.NAVIGATING
            val blueColor = ContextCompat.getColor(this.context!!,R.color.light_blue)
            activity?.window?.apply {
                statusBarColor = blueColor
            }
            toolbar.setBackgroundColor(blueColor)
        }
        bottomSheetBinding.finish.setOnClickListener {
            clearAll()
            bottomSheetBinding.executePendingBindings()
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
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

        viewModel.syncState.observe(viewLifecycleOwner, Observer {
            if (it){
                context?.toast(getString(R.string.data_sync))
            } else {
                findNavController().navigate(R.id.sync_fragment)
            }
        })
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        setMarginToCompass()
        this.mapboxMap = mapboxMap
        viewModel.poiList.observe(this, Observer {
            setPois(it)
        })
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
            locationComponent = callback.onEnableLocation(mapboxMap)
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
            val loadImageUrl = viewModel.destination?.image
            if (!loadImageUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(loadImageUrl)
                    .placeholder(R.drawable.ic_image)
                    .into(bottomSheetBinding.poiImage)
            }
        }

        clickedCircle.circleRadius = 12f
        circleManager.update(clickedCircle)

        viewModel.origin?.let {
            viewModel.routeState = RouteState.LOADING
            val ori=  Point.fromLngLat(it.longitude.toDouble(), it.latitude.toDouble())
            getRoute(ori)
        }

        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
            viewModel.routeState = RouteState.POI_DETAILS
            bottomSheetBinding.executePendingBindings()
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
                viewModel.routeState = RouteState.LOADING
            }else {
                viewModel.routeState = RouteState.NO_GPS_PRE_ROUTE
            }
        }
        toolbar.visibility = View.VISIBLE
        activity?.showOnlyStatusBar()
    }

    private fun clearAll() {
        activity?.setFullScreen()
        toolbar.visibility = View.GONE
        if (viewModel.isNavigating()) {
            routeNavigation.stopNavigation()
            routeNavigation.removeProgressChangeListener(this)
            toolbar.setBackgroundColor(ContextCompat.getColor(context!!, R.color.color_primary))
        }
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
        return locationComponent?.lastKnownLocation?.let {
            Point.fromLngLat(it.longitude, it.latitude)
        }
    }

    private fun setRoute(route: DirectionsRoute){
        if (viewModel.isLoadingState()) {
            currentRoute = route
            val duration = with(route.duration()) {
                if (this != null)  TimeUnit.SECONDS.toMinutes(this.toLong()).toString() else ""
            }
            val distance = route.distance().toString()
            routeLayer.setProperties(visibility(VISIBLE))
            val text = getString(R.string.route_distance, duration, distance)
            viewModel.distance = HtmlCompat.fromHtml(text, FROM_HTML_MODE_LEGACY)
            viewModel.routeState = if (gpsEnabled) {
                RouteState.GPS_ROUTE
            } else {
                RouteState.NO_GPS_ROUTE
            }
        }

        route.geometry()?.let {
            val lineString = LineString.fromPolyline(it, Constants.PRECISION_6)
            routeSource.setGeoJson(lineString)
        }
    }

    private fun setMarginToCompass() {
        ViewCompat.setOnApplyWindowInsetsListener(activity?.findViewById(R.id.app_container)!!) { _, insets ->
            val menu = activity?.findViewById<CompassView>(RMB.id.compassView)
            menu?.let {
                val menuLayoutParams = menu.layoutParams as ViewGroup.MarginLayoutParams
                menuLayoutParams.setMargins(0, insets.systemWindowInsetTop, 0, 0)
                menu.layoutParams = menuLayoutParams
            }
            insets.consumeSystemWindowInsets()
        }
    }

    fun updateLocationComponent(locationComponent: LocationComponent?) {
        this.locationComponent = locationComponent
    }
}
