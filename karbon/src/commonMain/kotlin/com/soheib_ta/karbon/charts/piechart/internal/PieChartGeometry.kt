package com.soheib_ta.karbon.charts.piechart.internal

import androidx.compose.ui.geometry.Offset
import com.soheib_ta.karbon.charts.piechart.PieChartSelection
import com.soheib_ta.karbon.charts.piechart.data.PieEntry
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

internal data class PieSliceGeometry(
    val index: Int,
    val startAngle: Float,
    val sweepAngle: Float,
    val midAngle: Float,
    val value: Float,
    val percentage: Float,
)

internal fun calculatePieSlices(
    entries: List<PieEntry>,
    startAngle: Float,
    clockwise: Boolean,
    gapAngle: Float,
): List<PieSliceGeometry> {
    val sanitizedValues = entries.map { entry ->
        entry.value.takeIf { it.isFinite() && it > 0f } ?: 0f
    }
    val total = sanitizedValues.sum()
    if (!total.isFinite() || total <= 0f) return emptyList()

    val positiveCount = sanitizedValues.count { it > 0f }
    val direction = if (clockwise) 1f else -1f
    var currentAngle = startAngle

    return buildList {
        sanitizedValues.forEachIndexed { index, value ->
            if (value <= 0f) return@forEachIndexed

            val percentage = value / total
            val rawSweep = percentage * 360f
            val appliedGap = if (positiveCount <= 1) {
                0f
            } else {
                min(gapAngle, rawSweep * 0.8f)
            }
            val visibleSweep = (rawSweep - appliedGap).coerceAtLeast(0f)
            val sliceStart = currentAngle + direction * appliedGap / 2f
            val signedSweep = direction * visibleSweep

            add(
                PieSliceGeometry(
                    index = index,
                    startAngle = sliceStart,
                    sweepAngle = signedSweep,
                    midAngle = sliceStart + signedSweep / 2f,
                    value = value,
                    percentage = percentage,
                ),
            )
            currentAngle += direction * rawSweep
        }
    }
}

internal fun findPieSliceAt(
    offset: Offset,
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    slices: List<PieSliceGeometry>,
    entries: List<PieEntry>,
): PieChartSelection? {
    if (outerRadius <= 0f || innerRadius < 0f || innerRadius >= outerRadius) return null

    val dx = offset.x - center.x
    val dy = offset.y - center.y
    val distance = sqrt(dx * dx + dy * dy)
    if (distance < innerRadius || distance > outerRadius) return null

    val angle = normalizeDegrees((atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI).toFloat())
    val slice = slices.firstOrNull { isAngleInSweep(angle, it.startAngle, it.sweepAngle) }
        ?: return null
    val entry = entries.getOrNull(slice.index) ?: return null

    return PieChartSelection(
        index = slice.index,
        label = entry.label,
        value = slice.value,
        percentage = slice.percentage,
    )
}

internal fun isAngleInSweep(
    angle: Float,
    startAngle: Float,
    sweepAngle: Float,
): Boolean {
    if (!angle.isFinite() || !startAngle.isFinite() || !sweepAngle.isFinite()) return false
    val magnitude = abs(sweepAngle)
    if (magnitude <= 0f) return false
    if (magnitude >= 360f) return true

    return if (sweepAngle > 0f) {
        normalizeDegrees(angle - startAngle) <= magnitude
    } else {
        normalizeDegrees(startAngle - angle) <= magnitude
    }
}

internal fun normalizeDegrees(value: Float): Float {
    val normalized = value % 360f
    return if (normalized < 0f) normalized + 360f else normalized
}
