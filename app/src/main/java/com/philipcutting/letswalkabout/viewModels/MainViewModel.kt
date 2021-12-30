package com.philipcutting.letswalkabout.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.philipcutting.letswalkabout.models.PathPointAndOrBearing

class MainViewModel : ViewModel() {
    companion object {
        private const val TAG = "MainViewModel"
        const val bearingDeltaSensitivityPositive = 5
        const val bearingDeltaSensitivityNegative = bearingDeltaSensitivityPositive * -1
    }



    private val _pathList = MutableLiveData<MutableList<Point?>>(emptyList<Point>().toMutableList())
    val pathList: LiveData<MutableList<Point?>> = _pathList

    var hasChangedBearing  = SingleLiveEvent<Boolean>().apply {
        value = false
    }

    var isTrackingLocationOnMap = SingleLiveEvent<Boolean>().apply {
        value = true
    }

    var addPointBecauseBearingChanged = SingleLiveEvent<Boolean>().apply {
        value = true
    }

    //Debugging variables.
    var locationCounter = MutableLiveData(0)
    var locationIterator = MutableLiveData(0)

    var lastBearing = MutableLiveData<Double>(0.0)
    var lastPointOrBearing= MutableLiveData<PathPointAndOrBearing?>(null)

    private var currentLocationOnChange = MutableLiveData<Point>()
    private var currentBearingOnChange = MutableLiveData<Double>()

    fun setBearingOnChanged(bearing: Double) {
        currentBearingOnChange.value = bearing

        val delta = bearing-(lastBearing.value ?: bearing)
        if(delta < bearingDeltaSensitivityNegative || delta > bearingDeltaSensitivityPositive) {
            addPointBecauseBearingChanged.value = true
            lastBearing.value = bearing
        }
    }

    fun setPointOnChangedLocation(point: Point){
        locationCounter.value =(locationCounter.value ?: 0) + 1
//        Log.d(TAG, "save point 0")
        if(addPointBecauseBearingChanged.value != true || !rulesForVerifyingLegitLocation(point) ) {
            return
        }

        currentLocationOnChange.value = point

        _pathList.value?.add(currentLocationOnChange.value )
        _pathList.value = _pathList.value  //Forces the liveData to update observers w/ collections.

        locationIterator.value = (locationIterator.value ?: 0) + 1
        addPointBecauseBearingChanged.value =  false
    }

    private fun rulesForVerifyingLegitLocation(point: Point?): Boolean {
        return point != null &&
            point.longitude() != 0.0 &&
            point.latitude() != 0.0
    }

    fun mapsPath(): List<Point>{
        val fastPath = pathList.value?.filterNotNull()?.map {
            Point.fromLngLat(it.longitude(),it.latitude())
        }
        return fastPath?.toList() ?: emptyList()
    }
}