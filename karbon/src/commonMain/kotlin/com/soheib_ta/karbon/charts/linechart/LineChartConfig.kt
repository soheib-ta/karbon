package com.soheib_ta.karbon.charts.linechart

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soheib_ta.karbon.charts.common.ChartAnimationTrigger
import com.soheib_ta.karbon.charts.common.ChartLegendPosition
import com.soheib_ta.karbon.charts.common.compactChartValue

/** Controls horizontal point distribution at zoom scale 1. */
sealed interface LineDistributionMode {
    data object Even : LineDistributionMode
    data class Fixed(val pointSpacing: Dp = 72.dp) : LineDistributionMode
}

data class LineChartColors(
    val gridLine: Color = Color(0xFFF0F0F0),
    val axisLine: Color = Color(0xFFE5E7EB),
    val yLabel: Color = Color(0xFF9CA3AF),
    val xLabel: Color = Color(0xFF6B7280),
    val tooltipBackground: Color = Color(0xE62D3435),
    val tooltipText: Color = Color.White,
    val zoomIndicatorBackground: Color = Color(0xCC1E3A5F),
    val zoomIndicatorText: Color = Color.White,
    val emptyStateText: Color = Color(0xFF6B7280),
)

/** Configuration for [LineChart]. */
data class LineChartConfig(
    val distributionMode: LineDistributionMode = LineDistributionMode.Even,
    val legendPosition: ChartLegendPosition = ChartLegendPosition.TopRight,
    val chartHeight: Dp = 280.dp,
    val outerHorizontalPadding: Dp = 44.dp,
    val innerHorizontalPadding: Dp = 12.dp,
    val yTickCount: Int = 5,
    val yMin: Float? = null,
    val yMax: Float? = null,
    val includeZeroInRange: Boolean = true,
    val showHorizontalGrid: Boolean = true,
    val showVerticalGrid: Boolean = false,
    val showXAxis: Boolean = true,
    val showYAxis: Boolean = true,
    val maxVisibleXLabels: Int = 8,
    val zoomEnabled: Boolean = true,
    val showZoomIndicator: Boolean = true,
    val selectionRadius: Dp = 18.dp,
    val animationEnabled: Boolean = true,
    val animationTrigger: ChartAnimationTrigger = ChartAnimationTrigger.OnVisible,
    val visibilityThreshold: Float = 0.2f,
    val animationSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow,
    ),
    val valueFormatter: (Float) -> String = ::compactChartValue,
    val xLabelFormatter: (String) -> String = { it },
    val emptyStateLabel: String = "No data available",
    val colors: LineChartColors = LineChartColors(),
) {
    init {
        require(yTickCount >= 2) { "yTickCount must be at least 2." }
        require(maxVisibleXLabels >= 1) { "maxVisibleXLabels must be at least 1." }
        require(selectionRadius.value >= 0f) { "selectionRadius must not be negative." }
        require(visibilityThreshold in 0f..1f) { "visibilityThreshold must be in 0..1." }
        if (yMin != null && yMax != null) {
            require(yMin.isFinite() && yMax.isFinite() && yMin < yMax) {
                "When both are set, yMin and yMax must be finite and yMin < yMax."
            }
        }
    }
}

/** Details emitted when the user selects a rendered point. */
data class LineChartSelection(
    val entryIndex: Int,
    val seriesKey: String,
    val seriesLabel: String,
    val xLabel: String,
    val value: Float,
)
