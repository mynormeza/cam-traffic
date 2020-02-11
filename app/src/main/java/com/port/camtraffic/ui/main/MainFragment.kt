package com.port.camtraffic.ui.main

import android.content.Context
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
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
import com.mapbox.mapboxsdk.annotations.PolylineOptions
import com.mapbox.mapboxsdk.geometry.LatLng
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
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MainFragment : Fragment(), OnMapReadyCallback, PermissionsListener, Callback<DirectionsResponse> {

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
    private var bottomSheetBinding: BottomSheetBinding? = null
    private lateinit var routeViewBinding: RouteViewBinding
    private lateinit var toolbar: Toolbar
    private lateinit var routeSource: GeoJsonSource

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        locationManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val root = inflater.inflate(R.layout.main_fragment, container, false)
        val bottomSheetLayout = root.findViewById<FrameLayout>(R.id.bottom_sheet)
        toolbar = activity?.findViewById(R.id.toolbar)!!
        val menuIcon = ContextCompat.getDrawable(context!!, R.drawable.ic_close)
        toolbar.navigationIcon = menuIcon
        bottomSheetBinding = DataBindingUtil.bind(bottomSheetLayout)
        routeViewBinding = RouteViewBinding.bind(toolbar.findViewById(R.id.route_view))
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)
        bottomSheetBehavior.addBottomSheetCallback( object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if ( newState == BottomSheetBehavior.STATE_COLLAPSED){
                    toolbar.visibility = View.GONE
                    viewModel.originCircle?.let {
                        it.circleRadius = 8f
                        circleManager.update(it)
                    }
                    viewModel.destinationCircle?.let {
                        it.circleRadius = 8f
                        circleManager.update(it)
                    }

                    viewModel.destinationCircle = null
                    viewModel.originCircle = null
                }
            }
        })

        mapView = root.findViewById(R.id.map_view)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
        toolbar.visibility = View.GONE
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bottomSheetBinding?.poiDirections?.setOnClickListener {
            directionsClick()
        }

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

    override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
        Timber.e(t);
    }

    override fun onResponse(
        call: Call<DirectionsResponse>,
        response: Response<DirectionsResponse>
    ) {
        response.body()?.let {
            if (it.routes().isNotEmpty()) {
                val routes = it.routes()[0]
                val distance = routes.distance().toString()
                val duration = TimeUnit.SECONDS.toMinutes(routes.duration()?.toLong()!!).toString()
                bottomSheetBinding?.distance = getString(R.string.route_distance, duration, distance)
                bottomSheetBinding?.hint?.visibility = View.GONE
                bottomSheetBinding?.routeDetails?.visibility = View.VISIBLE
                val lineString = LineString.fromPolyline(routes.geometry()!!, Constants.PRECISION_6)
                routeSource.setGeoJson(lineString)
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
            val lineLayer = LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                PropertyFactory.lineDasharray( arrayOf(0.01f, 2f)),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineWidth(5f),
                PropertyFactory.lineColor(Color.parseColor("#e55e5e")))
            style.addLayerBelow(lineLayer, "road-label-small")
        }
    }

    private fun poiClick(clickedCircle: Circle) {
        if (!viewModel.preRouteMode) {

            viewModel.originCircle?.let {circle ->
                circle.circleRadius = 8f
                circleManager.update(circle)
            }
            viewModel.originCircle = clickedCircle
            viewModel.origin = clickedCircle.data?.asJsonObject?.toClassObject(::toTrafficCamera)
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


        bottomSheetBinding?.poi = viewModel.origin
        bottomSheetBinding?.executePendingBindings()
        clickedCircle.circleRadius = 12f
        circleManager.update(clickedCircle)
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
        }
        getRoute()
    }

    private fun getRoute() {
        if (viewModel.origin != null && viewModel.destination != null) {
            val ori = Point.fromLngLat(viewModel.origin?.longitude!!.toDouble(), viewModel.origin?.latitude!!.toDouble())
            val des = Point.fromLngLat(viewModel.destination?.longitude!!.toDouble(), viewModel.destination?.latitude!!.toDouble())

            NavigationRoute.builder(context)
                .accessToken(getString(R.string.mapbox_access_token))
                .origin(ori)
                .destination(des)
                .build()
                .getRoute(this)
        }
    }

    private fun directionsClick() {
        viewModel.preRouteMode = true
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!gpsEnabled) {
            viewModel.origin?.let {
                bottomSheetBinding?.poiDetails?.visibility = View.GONE
                bottomSheetBinding?.hint?.visibility = View.VISIBLE
                routeViewBinding.origin = it.title
                toolbar.visibility = View.VISIBLE
                routeViewBinding.executePendingBindings()
            }
        }
    }

    private fun enableLocation(loadedStyle: Style) {
        if (PermissionsManager.areLocationPermissionsGranted(context)){
            val locationComponent = mapboxMap.locationComponent
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
}
