package org.kman.clearview.util

import java.util.*
import kotlin.math.abs

fun formatFixedPoint1(d: Double): String {
    return String.format(Locale.US, "%.1f", d)
}

fun formatFixedPoint1Percent(d: Double): String {
    return String.format(Locale.US, "%.1f%%", d)
}

fun formatFixedPoint2(d: Double): String {
    return String.format(Locale.US, "%.1f", d)
}

fun formatFixedPoint2Percent(d: Double): String {
    return String.format(Locale.US, "%.1f%%", d)
}

fun formatHumanDataSize(bytes: Long): String {
    if (bytes == 0L) {
        return "0"
    }
    if (bytes < 1024) {
        return String.format(Locale.US, "%d", bytes)
    }
    //val units = listOf("KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")
    val units = listOf("K", "M", "G", "T", "P", "E", "Z", "Y")
    var u = -1
    var b = bytes.toDouble()
    while (abs(b) >= 1024 && u < units.size - 1) {
        b /= 1024
        ++u
    }
    return String.format(
        Locale.US,
        if (b >= 100) {
            "%.1f %s"
        } else {
            "%.2f %s"
        }, b, units[u]
    )
}

fun formatHumanDataSizePerSecond(bytes: Long): String {
    if (bytes == 0L) {
        return "0"
    }
    return formatHumanDataSize(bytes) + "/s"
}

fun formatFractional(value: Float): String {
    if (value == 0.0f) {
        return "0"
    }
    if (value > 1000) {
        return value.toLong().toString()
    }

    return String.format(
        Locale.US, if (value > 100) {
            "%.1f"
        } else {
            "%.2f"
        }, value
    )
}

/* ----- */
class FormatFixedPoint1 : (Double) -> String {
    override operator fun invoke(value: Double): String = formatFixedPoint1(value)
}

class FormatFixedPoint1Percent : (Double) -> String {
    override operator fun invoke(value: Double): String = formatFixedPoint1Percent(value)
}

class FormatFixedPoint2 : (Double) -> String {
    override operator fun invoke(value: Double): String = formatFixedPoint2(value)
}

class FormatFixedPoint2Percent : (Double) -> String {
    override operator fun invoke(value: Double): String = formatFixedPoint2Percent(value)
}

class FormatHumanDataSize : (Double) -> String {
    override operator fun invoke(value: Double): String = formatHumanDataSize(value.toLong())
}

class FormatHumanDataSizePerSecond : (Double) -> String {
    override operator fun invoke(value: Double): String =
        formatHumanDataSizePerSecond(value.toLong())
}


