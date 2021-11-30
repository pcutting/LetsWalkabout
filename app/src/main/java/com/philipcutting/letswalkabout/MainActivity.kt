package com.philipcutting.letswalkabout

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.philipcutting.letswalkabout.databinding.ActivityMainBinding
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var binding: ActivityMainBinding
    private lateinit var locationPermissionHelper: LocationPermissionHelper

    companion object {
        private const val TAG = "MainActivity"
    }

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        Log.i(TAG, "onIndicatorBearingChangedListener")
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        Log.i(TAG, "onIndicatorPositionChangedListener")
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())

        //TODO verify if it should be .pixelForCoordinates(it) to center the walk.
        mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(it)
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMove(detector: MoveGestureDetector): Boolean {
            Log.i(TAG, "onMove")
            return false
        }

        override fun onMoveBegin(detector: MoveGestureDetector) {
            Log.i(TAG, "onMoveBegin")
            onCameraTrackingDismissed()
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {
            Log.i(TAG, "OnMoveListener called onMoveEnd")
        }
    }

    private val onMapReady: () -> Unit = {
        Log.i(TAG, "onMapReady() entered. (passed function call.)")
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .zoom(3.0)
                .build()
        )
        mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS
        ) {
            Log.i(TAG, "onMapReady: .loadStyleUri")
            initLocationComponent()
            setupGesturesListener()
        }
        Log.i(TAG, "onMapReady() finished")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        mapView = binding.mapView
        setContentView(binding.root)
        Log.i(TAG, "onCreate")

        mapView.location.addOnIndicatorPositionChangedListener {
            Log.i(TAG , "onCreate - addOnIndicator...")
            mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).zoom(12.0).build())
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
        Toast.makeText(this, "onCameraTrackingDismissed", Toast.LENGTH_SHORT).show()
        removeListeners()
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