package com.philipcutting.letswalkabout.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.philipcutting.letswalkabout.MainActivity
import com.philipcutting.letswalkabout.models.PathPoint
import com.philipcutting.letswalkabout.utilities.toScaledDouble

class MainViewModel : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
        const val bearingDeltaSensitivityPositive = 5
        const val bearingDeltaSensitivityNegative = bearingDeltaSensitivityPositive * -1
    }

    lateinit var annotationApi: AnnotationPlugin
    lateinit var polylineAnnotationManager: PolylineAnnotationManager

    var hasChangedBearing  = SingleLiveEvent<Boolean>().apply {
        value = false
    }

    var isTrackingLocationOnMap = SingleLiveEvent<Boolean>().apply {
        value = true
    }

    var addPointBecauseBearingChanged = SingleLiveEvent<Boolean>().apply {
        value = true
    }


    private val _pathList = MutableLiveData<MutableList<Point?>>(emptyList<Point>().toMutableList())
    val pathList: LiveData<MutableList<Point?>> = _pathList

    //Debugging variables.
    var locationCounter = MutableLiveData(0)
    var locationIterator = MutableLiveData(0)

    var lastBearing = MutableLiveData<Double>(0.0)
    var lastPoint= MutableLiveData<PathPoint?>(null)

    private var currentBearingOnChange = MutableLiveData<Double>()

    fun setBearingOnChanged(bearing: Double) {
        currentBearingOnChange.value = bearing

        val delta = bearing-(lastBearing.value ?: bearing)
        if(delta < bearingDeltaSensitivityNegative || delta > bearingDeltaSensitivityPositive) {
            addPointBecauseBearingChanged.value = true
//            Log.i(TAG, "Bearing: ${bearing.toScaledDouble(6)} " +
//                    "{Delta ${delta.toScaledDouble(3)}: Counter: " +
//                    "${locationCounter.value}, iterator: ${locationIterator.value}")
            lastBearing.value = bearing
        }
    }

    var currentLocationOnChange = MutableLiveData<Point>()

    fun setLocationOnChanged(point: Point){
        locationCounter.value =(locationCounter.value ?: 0) + 1

        if(addPointBecauseBearingChanged.value == true) {
            currentLocationOnChange.value = point

            if( currentLocationOnChange.value != null &&
                currentLocationOnChange.value?.longitude() != 0.0 &&
                currentLocationOnChange.value?.latitude() != 0.0) {
                _pathList.value?.add(currentLocationOnChange.value )
                _pathList.value = _pathList.value
            }
            locationIterator.value = (locationIterator.value ?: 0) + 1
            addPointBecauseBearingChanged.value =  false

//            Log.i(TAG, "locationChanged. Count/Iter[${locationCounter.value}" +
//                    "/${locationIterator.value}]. " +
//                    "position ${point.longitude().toScaledDouble(5)}," +
//                    "${point.latitude().toScaledDouble(5)}")

            //TODO("move this from here")
            val polylineAnnotationOptions: PolylineAnnotationOptions = PolylineAnnotationOptions()
                .withPoints(mapsPath())
                .withLineColor("#FF0000")
                .withLineWidth(12.0)

            polylineAnnotationManager.create(polylineAnnotationOptions)

            Log.i(TAG, "pathList size: ${pathList.value?.size}")
        }



    }

    fun mapsPath(): List<Point>{

        val fastPath = pathList.value?.filterNotNull()?.map {
            Point.fromLngLat(it.longitude(),it.latitude())
        }

//        Log.i(TAG, "Fastpath: ${fastPath?.size}")

//        var path = mutableListOf<Point>()
//        pathList.value?.forEach {pathPoint ->
//            val lng = pathPoint.longitude
//
//            if(pathPoint.isPoint() && pathPoint.longitude != null && pathPoint.latitude != null) {
//                path.add(Point.fromLngLat(pathPoint.longitude!!,pathPoint.latitude!!))

//                Pair(pathPoint.longitude, pathPoint.latitude).safeLet(onSafeCall(path))
//                Pair(pathPoint.longitude, pathPoint.latitude).let{
//                    path.add(Point.fromLngLat(pathPoint.longitude ?: 0.0,pathPoint.latitude ?: 0.0))
//                }
//
//            }
//        }

//        Log.d(TAG, "Points returned: [${path.size}]")

        return fastPath?.toList() ?: emptyList()
    }


    private val onSafeCallForPoint: (Double, Double, MutableList<Point>) -> Unit = { longitude, latitude, path ->
        Pair(longitude,latitude)
    }

    private val onFailCall: () -> Unit = {
        println("There was a null")
    }

}