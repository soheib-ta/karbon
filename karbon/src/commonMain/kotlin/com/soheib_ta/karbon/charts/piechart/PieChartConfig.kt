package com.soheib_ta.karbon.charts.piechart

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soheib_ta.karbon.charts.common.ChartAnimationTrigger
import com.soheib_ta.karbon.charts.common.ChartLegendPosition
import com.soheib_ta.karbon.charts.common.compactChartValue
import kotlin.math.round

data class PieChartColors(
    val tooltipBackground: Color = Color(0xE62D3435),
    val tooltipText: Color = Color.White,
    val centerValue: Color = Color(0xFF2D3435),
    val centerLabel: Color = Color(0xFF596062),
    val emptyStateText: Color = Color(0xFF6B7280),
)

/** Configuration shared by full pie and donut modes. */
data class PieChartConfig(
    val legendPosition: ChartLegendPosition = ChartLegendPosition.BottomLeft,
    val chartSize: Dp = 220.dp,
    val innerRadiusRatio: Float = 0.58f,
    val startAngle: Float = -90f,
    val clockwise: Boolean = true,
    val gapAngle: Float = 2f,
    val outerPadding: Dp = 10.dp,
    val selectedOffset: Dp = 7.dp,
    val showPercentInLegend: Boolean = false,
    val legendColumns: Int = 2,
    val animationEnabled: Boolean = true,
    val animationTrigger: ChartAnimationTrigger = ChartAnimationTrigger.OnVisible,
    val visibilityThreshold: Float = 0.2f,
    val animationSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow,
    ),
    val valueFormatter: (Float) -> String = ::compactChartValue,
    val percentageFormatter: (Float) -> String = ::defaultPiePercentageFormatter,
    val emptyStateLabel: String = "No data available",
    val colors: PieChartColors = PieChartColors(),
) {
    init {
        require(innerRadiusRatio >= 0f && innerRadiusRatio < 1f) { "innerRadiusRatio must be in 0..<1." }
        require(gapAngle >= 0f && gapAngle.isFinite()) { "gapAngle must be finite and non-negative." }
        require(selectedOffset.value >= 0f) { "selectedOffset must not be negative." }
        require(legendColumns >= 1) { "legendColumns must be at least 1." }
        require(visibilityThreshold in 0f..1f) { "visibilityThreshold must be in 0..1." }
    }
}

/** Information emitted when a pie slice is selected. */
data class PieChartSelection(
    val index: Int,
    val label: String,
    val value: Float,
    val percentage: Float,
)

fun defaultPiePercentageFormatter(fraction: Float): String {
    if (!fraction.isFinite()) return "—"
    val percentage = round(fraction * 1_000f) / 10f
    return if (percentage == percentage.toInt().toFloat()) {
        "${percentage.toInt()}%"
    } else {
        "$percentage%"
    }
}
