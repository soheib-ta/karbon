package com.soheib_ta.karbon.charts.linechart.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.soheib_ta.karbon.charts.linechart.LineChartConfig
import com.soheib_ta.karbon.charts.linechart.LineChartSelection
import com.soheib_ta.karbon.charts.linechart.LineDistributionMode
import com.soheib_ta.karbon.charts.linechart.data.LineEntry
import com.soheib_ta.karbon.charts.linechart.data.LineSeries
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

private val MARGIN_TOP_DP = 16.dp
private val MARGIN_BOTTOM_DP = 40.dp

internal data class LineValueRange(
    val min: Float,
    val max: Float,
) {
    init {
        require(min.isFinite() && max.isFinite() && min < max)
    }

    val span: Float get() = max - min
}

internal data class LineChartDimensions(
    val marginTop: Float,
    val marginBottom: Float,
    val innerHeight: Float,
    val viewportWidth: Float,
    val horizontalPadding: Float,
    val pointSpacing: Float,
    val pointCount: Int,
    val valueRange: LineValueRange,
    val xLabelStep: Int,
) {
    fun xForIndex(index: Int): Float = horizontalPadding + index * pointSpacing

    fun yForValue(value: Float): Float {
        val fraction = ((value - valueRange.min) / valueRange.span).coerceIn(0f, 1f)
        return innerHeight - fraction * innerHeight
    }

    fun zeroBaselineY(): Float = yForValue(0f.coerceIn(valueRange.min, valueRange.max))

    fun totalContentWidth(): Float = when {
        pointCount <= 1 -> max(viewportWidth, horizontalPadding * 2f)
        else -> max(viewportWidth, horizontalPadding * 2f + (pointCount - 1) * pointSpacing)
    }
}

internal fun computeLineValueRange(
    entries: List<LineEntry>,
    series: List<LineSeries>,
    requestedMin: Float?,
    requestedMax: Float?,
    includeZero: Boolean,
    tickCount: Int,
): LineValueRange {
    val values = buildList {
        entries.forEach { entry ->
            series.forEach { lineSeries ->
                val value = entry.values[lineSeries.key]
                if (value != null && value.isFinite()) add(value)
            }
        }
    }

    var rawMin = requestedMin?.takeIf { it.isFinite() }
        ?: values.minOrNull()
        ?: 0f
    var rawMax = requestedMax?.takeIf { it.isFinite() }
        ?: values.maxOrNull()
        ?: 1f

    if (includeZero) {
        if (requestedMin == null) rawMin = min(rawMin, 0f)
        if (requestedMax == null) rawMax = max(rawMax, 0f)
    }

    if (rawMin >= rawMax || abs(rawMax - rawMin) < 1e-6f) {
        val center = (rawMin + rawMax) / 2f
        val padding = max(1f, abs(center) * 0.1f)
        rawMin = center - padding
        rawMax = center + padding
    }

    if (requestedMin != null && requestedMax != null) {
        return LineValueRange(rawMin, rawMax)
    }

    val safeTickCount = tickCount.coerceAtLeast(2)
    val step = niceNumber((rawMax - rawMin) / (safeTickCount - 1), round = true)
        .takeIf { it.isFinite() && it > 0f }
        ?: 1f

    val niceMin = requestedMin?.takeIf { it.isFinite() } ?: (floor(rawMin / step) * step)
    val niceMax = requestedMax?.takeIf { it.isFinite() } ?: (ceil(rawMax / step) * step)

    return if (niceMin < niceMax) {
        LineValueRange(niceMin, niceMax)
    } else {
        LineValueRange(niceMin - 1f, niceMax + 1f)
    }
}

internal fun generateLineTicks(
    range: LineValueRange,
    tickCount: Int,
): List<Float> {
    val count = tickCount.coerceAtLeast(2)
    val step = range.span / (count - 1)
    return List(count) { index -> range.min + index * step }
}

internal fun computeLineDimensions(
    density: Density,
    size: Size,
    entries: List<LineEntry>,
    config: LineChartConfig,
    valueRange: LineValueRange,
    zoomScale: Float,
): LineChartDimensions = with(density) {
    val marginTop = MARGIN_TOP_DP.toPx()
    val marginBottom = MARGIN_BOTTOM_DP.toPx()
    val innerHeight = (size.height - marginTop - marginBottom).coerceAtLeast(1f)
    val basePadding = config.innerHorizontalPadding.toPx()
    val count = entries.size
    val safeScale = zoomScale.takeIf { it.isFinite() && it > 0f } ?: 1f

    val baseSpacing = when (val mode = config.distributionMode) {
        LineDistributionMode.Even -> {
            if (count > 1) {
                ((size.width - basePadding * 2f).coerceAtLeast(0f) / (count - 1))
            } else {
                0f
            }
        }

        is LineDistributionMode.Fixed -> mode.pointSpacing.toPx()
    }

    val pointSpacing = baseSpacing * safeScale
    val plottedWidth = if (count > 1) (count - 1) * pointSpacing else 0f
    val horizontalPadding = max(basePadding, (size.width - plottedWidth) / 2f)
    val labelStep = ceil(count.toFloat() / config.maxVisibleXLabels.toFloat())
        .toInt()
        .coerceAtLeast(1)

    LineChartDimensions(
        marginTop = marginTop,
        marginBottom = marginBottom,
        innerHeight = innerHeight,
        viewportWidth = size.width,
        horizontalPadding = horizontalPadding,
        pointSpacing = pointSpacing,
        pointCount = count,
        valueRange = valueRange,
        xLabelStep = labelStep,
    )
}

internal fun findNearestLinePoint(
    offset: Offset,
    entries: List<LineEntry>,
    series: List<LineSeries>,
    dimensions: LineChartDimensions,
    hitRadius: Float,
): LineChartSelection? {
    if (hitRadius < 0f || !hitRadius.isFinite()) return null

    var best: LineChartSelection? = null
    var bestDistanceSquared = hitRadius * hitRadius

    entries.forEachIndexed { entryIndex, entry ->
        series.forEach { lineSeries ->
            val value = entry.values[lineSeries.key]
            if (value == null || !value.isFinite()) return@forEach

            val dx = offset.x - dimensions.xForIndex(entryIndex)
            val dy = offset.y - dimensions.yForValue(value)
            val distanceSquared = dx * dx + dy * dy
            if (distanceSquared <= bestDistanceSquared) {
                bestDistanceSquared = distanceSquared
                best = LineChartSelection(
                    entryIndex = entryIndex,
                    seriesKey = lineSeries.key,
                    seriesLabel = lineSeries.label,
                    xLabel = entry.label,
                    value = value,
                )
            }
        }
    }

    return best
}

private fun niceNumber(value: Float, round: Boolean): Float {
    if (!value.isFinite() || value <= 0f) return 1f

    val exponent = floor(log10(value.toDouble())).toInt()
    val fraction = value / 10.0.pow(exponent.toDouble()).toFloat()
    val niceFraction = if (round) {
        when {
            fraction < 1.5f -> 1f
            fraction < 3f -> 2f
            fraction < 7f -> 5f
            else -> 10f
        }
    } else {
        when {
            fraction <= 1f -> 1f
            fraction <= 2f -> 2f
            fraction <= 5f -> 5f
            else -> 10f
        }
    }

    return niceFraction * 10.0.pow(exponent.toDouble()).toFloat()
}
