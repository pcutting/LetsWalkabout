package com.philipcutting.letswalkabout.utilities

import java.math.RoundingMode
import java.text.DecimalFormat

fun Double.toScaledDouble(scale: Int = 5): String {
    val formatter = DecimalFormat("#0.${"".padEnd(scale, '0')}")
    val partiallyFormattedValue =this.toBigDecimal()
        .setScale(scale, RoundingMode.HALF_UP).toDouble()
    return "${formatter.format(partiallyFormattedValue)}"
}


//fun Pair<Double?, Double?>.safeLet(
//    onSuccess: (Double,Double) -> Pair<Double,Double>,
//    onFailed: (() -> Unit)? = null
//): (Pair<Double,Double>)?{
//
//    safeLet(this.first,this.second, onSuccess, onFailed)
//}

fun safeLet(
    a: Double?,
    b: Double?,
    onSafe: (arg1: Double, arg2: Double) -> Pair<Double,Double>,
    onFailed: (() -> Unit)? = null
){
    a?.let {
        b?.let {}
    }?.let {
        onSafe(a,b!!)
    } ?: run {
        if (onFailed != null) {
            onFailed()
        }
    }
}

fun safeLetShort(
    arg1: Double?,
    arg2:Double?,
    onSafe: (Double, Double) -> Pair<Double, Double>,
    onFailed: () -> Unit
){
    arg1?.let {
        arg2?.let {
            onSafe(arg1,arg2)
        }
    } ?: run {
        onFailed()
    }
}
