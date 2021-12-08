package com.philipcutting.letswalkabout.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.philipcutting.letswalkabout.utilities.SingleLiveEvent
import com.philipcutting.letswalkabout.models.PathPoint

class MainViewModel : ViewModel() {

    companion object {
        const val bearingDeltaSensitivityPositive = 5.0
        const val bearingDeltaSensitivityNegative = bearingDeltaSensitivityPositive * -1
    }

    var hasChangedBearing  = SingleLiveEvent<Boolean>().apply {
        value = false
    }

    var isTrackingLocationOnMap = SingleLiveEvent<Boolean>().apply {
        value = true
    }

    var addPointBecauseBearingChanged = SingleLiveEvent<Boolean>().apply {
        value = true
    }

    var pathList = MutableLiveData<MutableList<PathPoint>>()

    var locationCounter = MutableLiveData(0)
    var locationIterator = MutableLiveData(0)

    var lastBearing = MutableLiveData<Double>(0.0)
    var lastPoint= MutableLiveData<PathPoint?>(null)


}