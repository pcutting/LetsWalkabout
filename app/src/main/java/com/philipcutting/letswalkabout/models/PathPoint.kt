package com.philipcutting.letswalkabout.models

data class PathPoint(
    var latitude: Double?,
    var longitude: Double?,
    var isBearingChange: Boolean = false,
    var bearing: Double?
) {

    constructor(bearing: Double): this(null, null, true, bearing)
    constructor(longitude: Double, latitude: Double): this(longitude,latitude, false, null)

    fun isPoint():Boolean {
        return !isBearingChange
    }
}
