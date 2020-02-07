package com.port.camtraffic.ui.main

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.CircleManager
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions
import com.port.camtraffic.R
import com.port.camtraffic.databinding.BottomSheetBinding
import com.port.camtraffic.db.entity.TrafficCamera
import com.port.camtraffic.utils.FactoryFunctions.toTrafficCamera
import com.port.camtraffic.utils.toClassObject
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class MainFragment : Fragment(), OnMapReadyCallback, PermissionsListener {

    @Inject lateinit var factory: ViewModelProvider.Factory
    private val viewModel: MainViewModel by viewModels { factory }
    private var mapView: MapView? = null
    private lateinit var mapboxMap: MapboxMap
    private lateinit var permissionManager: PermissionsManager
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private var bottomSheetBinding: BottomSheetBinding? = null

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.main_fragment, container, false)
        val bottomSheetLayout = root.findViewById<ConstraintLayout>(R.id.bottom_sheet)
        bottomSheetBinding = DataBindingUtil.bind(bottomSheetLayout)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)
        bottomSheetBehavior.addBottomSheetCallback( object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
            }
        })

        mapView = root.findViewById(R.id.map_view)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
            setPios(it)
        })
    }

    private fun setPios(pios: List<TrafficCamera>) {
       val circleOptions = ArrayList<CircleOptions>()
        pios.forEach {
            circleOptions.add(CircleOptions()
                .withData(it.toJsonObject())
                .withLatLng( LatLng(it.latitude.toDouble(), it.longitude.toDouble()))
                .withCircleRadius(8f)
                .withCircleColor("#247835")
            )
        }

        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            enableLocation(style)
            val circleManager =  CircleManager(mapView!!, mapboxMap, style)
            circleManager.addClickListener {
                val pio = it.data?.asJsonObject?.toClassObject(::toTrafficCamera)
                bottomSheetBinding?.poi = pio
                bottomSheetBinding?.executePendingBindings()
                it.circleRadius = 12f
                circleManager.update(it)
                if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
                }

            }
            circleManager.create(circleOptions)
        }
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
}
