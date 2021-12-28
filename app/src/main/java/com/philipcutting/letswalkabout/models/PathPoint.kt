package com.philipcutting.letswalkabout.models

import com.mapbox.geojson.Point

data class PathPoint(
    var latitude: Double?,
    var longitude: Double?,
    var isBearingChange: Boolean = false,
    var bearing: Double?
) {

    constructor(bearing: Double): this(null, null, true, bearing)
    constructor(longitude: Double, latitude: Double): this(longitude,latitude, false, null)
    constructor(point:Point): this(point.longitude(), point.latitude(),false, null)
    constructor(point:Pair<Double,Double>): this(point.first, point.second,false, null)


    fun isPoint():Boolean = !isBearingChange

    fun toPoint(): Point = Point.fromLngLat(longitude ?: 0.0,latitude ?: 0.0)
}
