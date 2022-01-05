package com.philipcutting.letswalkabout

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.gestures.Constants
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.core.constants.Constants.PRECISION_6
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapEvents
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.observable.eventdata.StyleDataLoadedEventData
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.delegates.listeners.OnStyleDataLoadedListener
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
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
        private const val GEOJSON_SOURCE_ID = "geojson_source_id"
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
            setupDataEventListener()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        mapView = binding.mapView
        setContentView(binding.root)
        annotationApi = mapView.annotations
        polylineAnnotationManager = annotationApi.createPolylineAnnotationManager(mapView)
        val polylineAnnotationOptions: PolylineAnnotationOptions = PolylineAnnotationOptions()
            .withPoints(emptyList())
            .withLineColor("#FF1122")
            .withLineWidth(5.0)
        polylineAnnotationManager.create(polylineAnnotationOptions)

        binding.fabCurrentLocation.setOnClickListener {
            fabCurrentLocationOnClick()
        }

        binding.fabBarringOption.setOnClickListener {
            fabBearingOptionOnClick()
        }

        locationPermissionHelper = LocationPermissionHelper(WeakReference(this))
        locationPermissionHelper.checkPermissions(onMapReady)

        viewModel.pathList.observe(this){
            if (viewModel.mapsPath().size < 5) return@observe

            mapView.getMapboxMap().getStyle { style ->
                val lineString = LineString.fromLngLats(viewModel.mapsPath())
                val feature = Feature.fromGeometry(lineString)

                // Also tried the following, but still didn't have success.
                /*val source = style.getSourceAs<GeoJsonSource>(GEOJSON_SOURCE_ID)
                if (source != null && feature.geometry() != null) {
                    source.feature(feature)
                    source.data("some data")
                }*/

                val geoJsonSourceItem = GeoJsonSource.Builder(GEOJSON_SOURCE_ID)
                    .data("path")
                    .feature(feature)
                    .build()

                if(style.styleSourceExists(GEOJSON_SOURCE_ID)) {
                    style.removeStyleSource(GEOJSON_SOURCE_ID)
                }

                if (geoJsonSourceItem.data == null) {
                    Log.e(TAG, "data is null")
                } else {
                    style.addSource(geoJsonSourceItem)
                }
            }
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

    private fun setupDataEventListener() {
        Log.i(TAG, "setupDataEventListener()")
        mapView.getMapboxMap().addOnStyleDataLoadedListener(onMapEventsStyleDataLoaded)
    }

    private val onMapEventsStyleDataLoaded = OnStyleDataLoadedListener{ eventData ->
        Log.e(TAG, "onStyleDataLoaded: $eventData")
        if (eventData == null) {
            Log.e(TAG, "data is null")
        } else {
            Log.e(TAG, "data is not null")
        }
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMove(detector: MoveGestureDetector): Boolean {

            return false
        }

        override fun onMoveBegin(detector: MoveGestureDetector) {
            onCameraTrackingDismissed()
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {
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