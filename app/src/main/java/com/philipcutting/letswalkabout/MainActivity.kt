package com.philipcutting.letswalkabout

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.philipcutting.letswalkabout.databinding.ActivityMainBinding
import com.philipcutting.letswalkabout.models.PathPoint
import com.philipcutting.letswalkabout.utilities.toScaledDouble
import com.philipcutting.letswalkabout.viewModels.MainViewModel
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var binding: ActivityMainBinding
    private lateinit var locationPermissionHelper: LocationPermissionHelper

    private val viewModel : MainViewModel by viewModels()

    companion object {
        private const val TAG = "MainActivity"
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

        viewModel.setLocationOnChanged(it)
        viewModel.lastPoint.value = PathPoint(it)
        if(viewModel.isTrackingLocationOnMap.value == true) {
            mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
            mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(it)
        }
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
        Log.i(TAG, "onCreate")

        viewModel.annotationApi = mapView.annotations
        viewModel.polylineAnnotationManager =
            viewModel.annotationApi.createPolylineAnnotationManager(mapView)

        mapView.location.addOnIndicatorPositionChangedListener {
//            Log.i(TAG , "onCreate - addOnIndicator...")
            //mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).zoom(12.0).build())
        }

        binding.fabCurrentLocation.setOnClickListener {
            fabCurrentLocationOnClick()
        }

        binding.fabBarringOption.setOnClickListener {
            fabBearingOptionOnClick()
        }

        locationPermissionHelper = LocationPermissionHelper(WeakReference(this))
        locationPermissionHelper.checkPermissions(onMapReady)


        viewModel.pathList.observe(this){
            Log.i(TAG, "pathList Observer : ${it.size}")
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
        //removeListeners()
        viewModel.isTrackingLocationOnMap.value = false
        viewModel.hasChangedBearing.value = false
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