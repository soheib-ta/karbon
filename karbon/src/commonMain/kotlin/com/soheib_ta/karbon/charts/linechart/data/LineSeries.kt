package com.soheib_ta.karbon.charts.linechart.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Determines how adjacent points are connected. */
enum class LineCurveType { Straight, Smooth }

/** Optional fill rendered between a line and the chart's zero baseline. */
sealed interface LineAreaFill {
    data object None : LineAreaFill

    /** Uses [color], or the series color when null. */
    data class Solid(
        val color: Color? = null,
        val alpha: Float = 0.14f,
    ) : LineAreaFill {
        init {
            require(alpha in 0f..1f) { "LineAreaFill.Solid alpha must be in 0..1." }
        }
    }

    data class VerticalGradient(
        val colors: List<Color>,
        val alpha: Float = 1f,
    ) : LineAreaFill {
        init {
            require(colors.isNotEmpty()) { "LineAreaFill.VerticalGradient requires at least one color." }
            require(alpha in 0f..1f) { "LineAreaFill.VerticalGradient alpha must be in 0..1." }
        }
    }
}

/** Visual definition for one line series. */
data class LineSeries(
    val key: String,
    val label: String,
    val color: Color,
    val lineWidth: Dp = 3.dp,
    val curveType: LineCurveType = LineCurveType.Smooth,
    val areaFill: LineAreaFill = LineAreaFill.None,
    val showPoints: Boolean = true,
    val pointRadius: Dp = 3.5f.dp,
) {
    init {
        require(key.isNotBlank()) { "LineSeries key must not be blank." }
        require(lineWidth.value > 0f) { "LineSeries lineWidth must be positive." }
        require(pointRadius.value >= 0f) { "LineSeries pointRadius must not be negative." }
    }
}
