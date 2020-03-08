package com.port.camtraffic

import android.animation.LayoutTransition
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.port.camtraffic.extensions.setFullScreen
import com.port.camtraffic.extensions.showOnlyStatusBar
import com.port.camtraffic.extensions.toast
import com.port.camtraffic.ui.main.MainFragment
import dagger.android.AndroidInjection
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector

import javax.inject.Inject

class MainActivity : AppCompatActivity(), HasAndroidInjector, MainFragment.OnRequestLocation,
    PermissionsListener {

    @Inject lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var toolbar: Toolbar
    private lateinit var permissionManager: PermissionsManager
    private lateinit var mapboxMap: MapboxMap

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<CoordinatorLayout>(R.id.app_container).layoutTransition
            .enableTransitionType(LayoutTransition.CHANGING)
        toolbar = findViewById(R.id.toolbar)
        val navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(setOf(R.id.main_fragment, R.id.sync_fragment))
        setSupportActionBar(toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)
        visibilityNavElements(navController)
        supportActionBar?.setDisplayShowTitleEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun androidInjector() = dispatchingAndroidInjector

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus){
            if (toolbar.visibility == View.GONE) {
                setFullScreen()
            } else {
                showOnlyStatusBar()
            }
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        toast(getString(R.string.explain_permission))
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            val currentFragment = navHostFragment?.childFragmentManager?.fragments?.get(0)
            if (currentFragment is MainFragment) {
                currentFragment.updateLocationComponent(enableLocation(mapboxMap.style!!))
            }
        } else {
            toast(getString(R.string.permission))
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onEnableLocation(mapboxMap: MapboxMap): LocationComponent? {
        this.mapboxMap = mapboxMap
        return enableLocation(loadedStyle = mapboxMap.style!!)
    }

    private fun visibilityNavElements(navController: NavController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.sync_fragment -> {
                    toolbar.visibility = View.GONE
                }
                else -> {
                    toolbar.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun enableLocation(loadedStyle: Style): LocationComponent? {
        return if (PermissionsManager.areLocationPermissionsGranted(this)) {
            val locationComponent = mapboxMap.locationComponent
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(
                    this,
                    loadedStyle
                ).build()
            )

            locationComponent.isLocationComponentEnabled = true
            locationComponent.cameraMode = CameraMode.TRACKING
            locationComponent.renderMode = RenderMode.NORMAL
            locationComponent
        } else {
            permissionManager = PermissionsManager(this)
            permissionManager.requestLocationPermissions(this)
            null
        }

    }
}
