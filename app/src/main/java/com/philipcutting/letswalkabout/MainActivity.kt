package com.philipcutting.letswalkabout

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.philipcutting.letswalkabout.databinding.ActivityMainBinding
import com.philipcutting.letswalkabout.models.PathPointAndOrBearing
import com.philipcutting.letswalkabout.viewModels.MainViewModel
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var binding: ActivityMainBinding
    private lateinit var locationPermissionHelper: LocationPermissionHelper

    lateinit var annotationApi: AnnotationPlugin
    lateinit var polylineAnnotationManager: PolylineAnnotationManager

    private val viewModel : MainViewModel by viewModels()

    companion object {
        private const val TAG = "MainActivity"
        private const val GEOJSON_SOURCE_ID = "line"
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            Log.d(TAG, "routeProgressObserver")
        }

    }

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        viewModel.setBearingOnChanged(it)
        if(viewModel.hasChangedBearing.value == true) {
            mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
        }
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        if(it.latitude() == 0.0 ||  it.longitude() == 0.0) {
            return@OnIndicatorPositionChangedListener
        }

        viewModel.setPointOnChangedLocation(it)
        viewModel.lastPointOrBearing.value = PathPointAndOrBearing(it)
        if(viewModel.isTrackingLocationOnMap.value == true) {
            mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
            mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(it)
        }
    }

    private val onMapReady: () -> Unit = {
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .zoom(16.0)
                .build()
        )
        mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS
        ) {
            Log.i(TAG, "onMapReady: .loadStyleUri")
            initLocationComponent()
            setupGesturesListener()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        mapView = binding.mapView
        setContentView(binding.root)
        annotationApi = mapView.annotations
        polylineAnnotationManager = annotationApi.createPolylineAnnotationManager()
//        val polylineAnnotationOptions: PolylineAnnotationOptions = PolylineAnnotationOptions()
//            .withPoints(emptyList())
//            .withLineColor("#FF1122")
//            .withLineWidth(5.0)
//        polylineAnnotationManager.create(polylineAnnotationOptions)
//


        binding.fabCurrentLocation.setOnClickListener {
            fabCurrentLocationOnClick()
        }

        binding.fabBarringOption.setOnClickListener {
            fabBearingOptionOnClick()
        }

        locationPermissionHelper = LocationPermissionHelper(WeakReference(this))
        locationPermissionHelper.checkPermissions(onMapReady)

//        val testData = """{ "type": "FeatureCollection","features": [{ "type": "Feature","geometry": {"type": "LineString","coordinates": [[23.3129909, 42.66594740], [103.0, 1.0], [104.0, 0.0], [105.0, 1.0]]}}]}"""
//            """
//                                {"type":"FeatureCollection",
//                                    "features": [
//                                       { "type":"Feature",
//                                           "geometry": {
//                                                "type":"LineString",
//                                                "coordinates":
//                                                    [[23.3129909, 42.66594740],
//                                                    [23.3117390595374, 42.665942498188734],
//                                                    [23.3067223, 42.6665434]]
//                                          },
//                                           "properties": {
//                                               "prop0": "value0",
//                                               "prop1": "value1"
//                                           }
//                                       }
//                                    ]
//                                }
//                                 """.trimMargin()

//        mapView.getMapboxMap().loadStyle(
//            (
//                    style(styleUri = Style.MAPBOX_STREETS) {
//                        +geoJsonSource(GEOJSON_SOURCE_ID) {
//                            data(testData)
//                        }
//                        +lineLayer("linelayer", GEOJSON_SOURCE_ID) {
//                            lineWidth(8.0)
//                            lineColor("#0F0")
//                        }
//                    }
//                    )
//        )

        viewModel.pathList.observe(this){
//            Log.i(TAG, "pathList Observer. Path Size: ${it.size}")
//            //TODO("move this from here?")
//            val path = viewModel.mapsPath()
//            val testPath = listOf(
//                Point.fromLngLat(23.3129909, 42.66594740),
//                Point.fromLngLat(23.3129909, 42.6659474),
//                Point.fromLngLat(23.312228430009565, 42.66589287000068),
//                Point.fromLngLat(23.3117390595374, 42.665942498188734),
//                Point.fromLngLat(23.31045962093641, 42.666072249007605),
//                Point.fromLngLat(23.310123353011488, 42.66610732903864),
//                Point.fromLngLat(23.309381942956698, 42.66618467417269),
//                Point.fromLngLat(23.308231339373087, 42.66630470705846),
//                Point.fromLngLat(23.30728735837411, 42.66640318473142),
//                Point.fromLngLat(23.30696417501965, 42.66644107083942),
//                Point.fromLngLat(23.306600593736853, 42.66648369271197),
//                Point.fromLngLat(23.306576758596545, 42.66650617279971),
//                Point.fromLngLat(23.306625838599505, 42.66651449489985),
//                Point.fromLngLat(23.30665860999862, 42.666524314999585),
//                Point.fromLngLat(23.306691497203147, 42.66653416980095),
//                Point.fromLngLat(23.3067223, 42.6665434))

//            polylineAnnotationOptions.withGeometry()
//            polylineAnnotationManager.create(polylineAnnotationOptions)
        }
    }

    private fun fabBearingOptionOnClick() {
        viewModel.hasChangedBearing.value = viewModel.hasChangedBearing.value == false

        if (viewModel.hasChangedBearing.value == true) {
            //TODO change color of button when pressed
            binding.fabBarringOption.backgroundTintList =
                ColorStateList.valueOf(getColor(R.color.fab_primary))
        } else {
            binding.fabBarringOption.backgroundTintList =
                ColorStateList.valueOf(getColor(R.color.fab_alt))
        }
    }

    private fun fabCurrentLocationOnClick() {
        viewModel.isTrackingLocationOnMap.value = viewModel.isTrackingLocationOnMap.value == false

        if (viewModel.isTrackingLocationOnMap.value == true) {
            binding.fabCurrentLocation.backgroundTintList =
                ColorStateList.valueOf(getColor(R.color.fab_primary))
        } else {
            binding.fabCurrentLocation.backgroundTintList =
                ColorStateList.valueOf(getColor(R.color.fab_alt))
        }
    }

    private fun setupGesturesListener() {
        Log.i(TAG, "setupGesturesListener")
        mapView.gestures.addOnMoveListener(onMoveListener)
    }

    private fun initLocationComponent() {
        Log.i(TAG, "init Location Components")
        val locationComponentPlugin = mapView.location
        locationComponentPlugin.updateSettings {
            this.enabled = true
        }

        locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)

    }

    private fun removeListeners() {
        mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    private fun onCameraTrackingDismissed() {
        Toast.makeText(
            this,
            "onCameraTrackingDismissed",
            Toast.LENGTH_SHORT).show()
        viewModel.isTrackingLocationOnMap.value = false
        viewModel.hasChangedBearing.value = false
    }

    private val onMoveListener = object : OnMoveListener {
        // These are for moving of the map view, not the users location.

        override fun onMove(detector: MoveGestureDetector): Boolean {
//            Log.i(TAG, "onMove")
            return false
        }

        override fun onMoveBegin(detector: MoveGestureDetector) {
//            Log.i(TAG, "onMoveBegin")
            onCameraTrackingDismissed()
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {
//            Log.i(TAG, "OnMoveListener called onMoveEnd")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeListeners()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.i(TAG, "onRequestPermissionsResult")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionHelper.onRequestPermissionsResult(requestCode, permissions,grantResults)
    }
}