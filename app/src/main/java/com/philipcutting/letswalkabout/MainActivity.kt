package com.philipcutting.letswalkabout

import android.content.res.ColorStateList
import android.graphics.Color
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
import com.philipcutting.letswalkabout.models.PathPoint
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var binding: ActivityMainBinding
    private lateinit var locationPermissionHelper: LocationPermissionHelper

    private var path: MutableList<PathPoint> = mutableListOf()

    private var changeBearing = false
    private var trackingLocationOnMap = true

    private var lastBearing: Double? = null
    private var lastPoint: Pair<Double,Double>? = null


    companion object {
        private const val TAG = "MainActivity"
    }

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        Log.i(TAG, "onIndicatorBearingChangedListener, Bearing: $it")
        path.add(PathPoint(it))
        if(changeBearing) {
            mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
        }

    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        Log.i(TAG, "onIndicatorPositionChangedListener. positiong ${it.longitude()},${it.latitude()}")

        //TODO see if i need to add a sensitivity setting to this.  it doesn't need to be added too often.


        if(trackingLocationOnMap) {
            mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())

            //TODO verify if it should be .pixelForCoordinates(it) to center the walk.
            mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(it)
        }
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
            //mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).zoom(12.0).build())
        }

        binding.fabCurrentLocation.setOnClickListener {
            trackingLocationOnMap = !trackingLocationOnMap

            if(trackingLocationOnMap){
                binding.fabCurrentLocation.backgroundTintList =
                ColorStateList.valueOf(getColor(R.color.fab_primary))
            } else {
                binding.fabCurrentLocation.backgroundTintList =
                    ColorStateList.valueOf(getColor(R.color.fab_alt))
            }
        }

        binding.fabBarringOption.setOnClickListener {
            changeBearing = !changeBearing

            if(changeBearing){
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
        Toast.makeText(this, "onCameraTrackingDismissed", Toast.LENGTH_SHORT).show()
        //removeListeners()
        this.trackingLocationOnMap = false
        this.changeBearing = false
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