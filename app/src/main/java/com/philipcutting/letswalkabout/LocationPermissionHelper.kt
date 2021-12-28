package com.philipcutting.letswalkabout

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import java.lang.ref.WeakReference

class LocationPermissionHelper(val activity: WeakReference<Activity>) {
    companion object {
        private const val TAG = "LocationPermissionHelper"
    }

    private lateinit var permissionsManager: PermissionsManager

    fun checkPermissions(onMapReady: () ->  Unit) {
        if(PermissionsManager.areLocationPermissionsGranted(activity.get())) {
            Log.i(TAG, "checkPermissions: Has Permission")
            onMapReady()
        } else {
            Log.i(TAG, "checkPermissions: No Permission")
            permissionsManager = PermissionsManager(object : PermissionsListener {
                override fun onExplanationNeeded(p0: MutableList<String>?) {
                    Toast.makeText(
                        activity.get(),
                        "You need to accept location permissions to use this app.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onPermissionResult(granted: Boolean) {
                    if (granted) {
                        Log.i(TAG, "checkPermissions: onPermissionResult: Granted")
                        run {onMapReady}
                    } else {
                        activity.get()?.finish()
                    }
                }
            })
            permissionsManager.requestLocationPermissions(activity.get())
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}