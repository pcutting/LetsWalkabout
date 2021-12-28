package com.philipcutting.letswalkabout.utilities

import java.math.RoundingMode
import java.text.DecimalFormat

fun Double.toScaledDouble(scale: Int = 5): String {
    val formatter = DecimalFormat("#0.${"".padEnd(scale, '0')}")
    val partiallyFormattedValue =this.toBigDecimal()
        .setScale(scale, RoundingMode.HALF_UP).toDouble()
    return "${formatter.format(partiallyFormattedValue)}"
}
