package com.philipcutting.letswalkabout.viewModels

import androidx.lifecycle.ViewModel
import com.philipcutting.letswalkabout.models.PathPoint

class MainViewModel : ViewModel() {
    var pathList: MutableList<PathPoint> = mutableListOf()

    var changeBearing = false
    var trackingLocationOnMap = true

    val bearingDeltaSensitivityPositive = 5.0
    val bearingDeltaSensitivityNegative = bearingDeltaSensitivityPositive * -1

    var locationCounter = 0
    var locationIterator = 0

    var lastBearing: Double = 0.0
    var lastPoint: PathPoint? = null
    var addPointBecauseBearingChanged = true

}