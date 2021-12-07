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
import com.mapbox.maps.plugin.annotation.Annotation
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.AnnotationType
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.philipcutting.letswalkabout.databinding.ActivityMainBinding
import com.philipcutting.letswalkabout.models.PathPoint
import com.philipcutting.letswalkabout.viewModels.MainViewModel
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var binding: ActivityMainBinding
    private lateinit var locationPermissionHelper: LocationPermissionHelper

    private lateinit var annotationApi:AnnotationPlugin
    private lateinit var polylineAnnotationManager:PolylineAnnotationManager

    private val viewModel : MainViewModel by viewModels()

    companion object {
        private const val TAG = "MainActivity"
    }

    private fun mapsPath(): List<Point>{

        var path = mutableListOf<Point>()
        viewModel.pathList.forEach {
            if(it.isPoint()) {
                //Initially we will list every point.
                //  Soon to filter out for change of directions to make lines.
                path.add(Point.fromLngLat(it.longitude ?: 0.0,it.latitude ?: 0.0))
            }
        }

        Log.d(TAG, "Points returned: [${path.size}]")

        return path.toList()
    }

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {

        val delta = it-viewModel.lastBearing
        if(delta > viewModel.bearingDeltaSensitivityPositive || delta < viewModel.bearingDeltaSensitivityNegative) {
            viewModel.pathList.add(PathPoint(it))
            viewModel.addPointBecauseBearingChanged = true
            Log.i(TAG, "Bearing: $it {Delta ${delta} : Sens.: ${viewModel.bearingDeltaSensitivityPositive} * Counter: ${viewModel.locationCounter}, iterator: ${viewModel.locationIterator}")
            viewModel.lastBearing = it
        }

        if(viewModel.changeBearing) {
            mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
        }
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        viewModel.locationCounter ++

        if(viewModel.addPointBecauseBearingChanged) {
            viewModel.pathList.add(PathPoint(it))
            viewModel.locationIterator ++
            viewModel.addPointBecauseBearingChanged =  false

            //TODO("move this from here")
            val polylineAnnotationOptions: PolylineAnnotationOptions = PolylineAnnotationOptions()
                .withPoints(mapsPath())
                .withLineColor("#FF0000")
                .withLineWidth(12.0)

            polylineAnnotationManager.create(polylineAnnotationOptions)
        }

//        Log.i(TAG, "Location Counter: $locationCounter, Location iterator: $locationIterator")

        viewModel.lastPoint = PathPoint(it)
//        Log.i(TAG, "onIndicatorPositionChangedListener. position ${it.longitude()},${it.latitude()}")
        if(viewModel.trackingLocationOnMap) {
            mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())

            //TODO verify if it should be .pixelForCoordinates(it) to center the walk.
            mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(it)
        }
    }

    private val onMoveListener = object : OnMoveListener {
        // These are for moving of the map, not the users location.

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

    private val onMapReady: () -> Unit = {
//        Log.i(TAG, "onMapReady() entered. (passed function call.)")
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .zoom(9.0)
                .build()
        )
        mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS
        ) {
            Log.i(TAG, "onMapReady: .loadStyleUri")
            initLocationComponent()
            setupGesturesListener()
        }
//        Log.i(TAG, "onMapReady() finished")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        mapView = binding.mapView
        setContentView(binding.root)
        Log.i(TAG, "onCreate")

        annotationApi = mapView.annotations
        polylineAnnotationManager =
            annotationApi.createPolylineAnnotationManager(mapView)

        mapView.location.addOnIndicatorPositionChangedListener {
//            Log.i(TAG , "onCreate - addOnIndicator...")
            //mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).zoom(12.0).build())
        }

        binding.fabCurrentLocation.setOnClickListener {
            viewModel.trackingLocationOnMap = !viewModel.trackingLocationOnMap

            if(viewModel.trackingLocationOnMap){
                binding.fabCurrentLocation.backgroundTintList =
                ColorStateList.valueOf(getColor(R.color.fab_primary))
            } else {
                binding.fabCurrentLocation.backgroundTintList =
                    ColorStateList.valueOf(getColor(R.color.fab_alt))
            }
        }

        binding.fabBarringOption.setOnClickListener {
            viewModel.changeBearing = !viewModel.changeBearing

            if(viewModel.changeBearing){
                //TODO change color of button when pressed
                binding.fabBarringOption.backgroundTintList =
                    ColorStateList.valueOf(getColor(R.color.fab_primary))
            } else {
                binding.fabBarringOption.backgroundTintList =
                    ColorStateList.valueOf(getColor(R.color.fab_alt))
            }
        }

        locationPermissionHelper = LocationPermissionHelper(WeakReference(this))
        locationPermissionHelper.checkPermissions(onMapReady)
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
        //removeListeners()
        viewModel.trackingLocationOnMap = false
        viewModel.changeBearing = false
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