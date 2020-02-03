package com.port.camtraffic.ui.main

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource

import com.port.camtraffic.R
import dagger.android.support.AndroidSupportInjection
import timber.log.Timber
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject

class MainFragment : Fragment() {

    companion object {
        const val PIO_SOURCE_ID = "pio-source"
        const val PIO_LAYER_ID = "pio-layer"
    }

    @Inject lateinit var factory: ViewModelProvider.Factory
    private val viewModel: MainViewModel by viewModels { factory }
    private var mapView: MapView? = null
    private lateinit var mapboxMap: MapboxMap


    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.main_fragment, container, false)
        mapView = root.findViewById(R.id.map_view)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync { mapboxMap ->
            this.mapboxMap = mapboxMap
            mapboxMap.setStyle(Style.MAPBOX_STREETS) {
                try {
                    val pioURI = URI("https://javieraragon.carto.com/api/v2/sql?q=SELECT%20ST_AsGeoJSON(the_geom)%20geojson%20FROM%20ios_test")
                    val pioSource =  GeoJsonSource(PIO_SOURCE_ID, pioURI)
                    it.addSource(pioSource)
                    val pioLayer = CircleLayer(PIO_LAYER_ID, PIO_SOURCE_ID)
                    pioLayer.setProperties(
                        circleRadius(3f),
                        circleColor( Color.parseColor(
                            "#247835"
                        ))
                    )
                    it.addLayer(pioLayer)
                } catch (exception: URISyntaxException) {
                    Timber.d( exception)
                }
            }
        }
        return root
    }

}
