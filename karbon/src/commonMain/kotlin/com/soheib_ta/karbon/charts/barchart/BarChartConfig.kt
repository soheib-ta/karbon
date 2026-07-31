package com.soheib_ta.karbon.charts.barchart

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// DistributionMode
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Controls how bar groups are sized and distributed across the X-axis.
 */
sealed interface DistributionMode {

    /**
     * Groups stretch to fill the available container width evenly.
     * At zoom scale = 1 the chart never scrolls.
     * At zoom scale > 1 the content overflows and becomes pannable.
     */
    data object Even : DistributionMode

    /**
     * Every group has exactly [groupWidth].
     * Content wider than the container is pannable regardless of zoom.
     *
     * @param groupWidth Fixed width per group at zoom scale = 1.
     */
    data class Fixed(val groupWidth: Dp = 100.dp) : DistributionMode {
        init {
            require(groupWidth.value > 0f) { "groupWidth must be positive." }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// LegendPosition
// ─────────────────────────────────────────────────────────────────────────────

enum class LegendPosition { TopLeft, TopRight, BottomLeft, BottomRight }


// ─────────────────────────────────────────────────────────────────────────────
// AnimationTrigger
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Controls when the bar grow-in animation starts.
 */
enum class AnimationTrigger {
    /** Animate immediately when the composable enters the composition. */
    OnCompose,

    /**
     * Animate only when the chart is at least [BarChartConfig.visibilityThreshold]
     * percent visible within its parent scroll container (or screen).
     *
     * Requires the chart to be inside a scrollable container for the
     * visibility check to be meaningful; otherwise it behaves like [OnCompose].
     */
    OnVisible,
}


// ─────────────────────────────────────────────────────────────────────────────
// BarChartColors — all colour tokens in one place
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Colour tokens for the chart's structural chrome (grid, axes, labels, tooltip).
 * Separate from per-series colours defined in [com.soheib_ta.karbon.charts.barchart.data.BarSeries].
 *
 * All values have sensible light-theme defaults.
 */
data class BarChartColors(
    val gridLine: Color = Color(0xFFF0F0F0),
    val axisLine: Color = Color(0xFFE5E7EB),
    val yLabel: Color = Color(0xFF9CA3AF),
    val xLabel: Color = Color(0xFF6B7280),
    val tooltipBackground: Color = Color(0xCC1E3A5F),
    val tooltipText: Color = Color(0xFFFFFFFF),
    val zoomIndicatorBackground: Color = Color(0xCC1E3A5F),
    val zoomIndicatorText: Color = Color(0xFFFFFFFF),
)


// ─────────────────────────────────────────────────────────────────────────────
// BarChartConfig
// ─────────────────────────────────────────────────────────────────────────────

/**
 * All visual and layout settings for [BarChart].
 *
 * Every field has a sensible default — only override what you need.
 *
 * @param distributionMode    How groups fill the X-axis. See [DistributionMode].
 * @param legendPosition      Where the legend appears. See [LegendPosition].
 * @param innerHorizontalPadding Gap between the chart edge and the first/last bar (dp).
 * @param outerHorizontalPadding Width of the non-scrolling Y-axis panel (dp).
 * @param groupSpacing        Gap between consecutive bar groups (dp).
 * @param barSpacing          Gap between bars inside one group (dp).
 * @param chartHeight         Height of the chart drawing area (dp).
 * @param forceDiagonalLabels
 *   - `true`  → always rotate X labels −45°.
 *   - `false` → never rotate.
 *   - `null`  → auto-detect: rotate when groupWidth < estimated label width.
 * @param yTickCount          Number of horizontal grid lines / Y-axis ticks.
 * @param zoomEnabled         When true, pinch-to-zoom and drag-to-pan are active.
 * @param showZoomIndicator   When true and zoom ≠ 1, a "2.4×" badge is shown.
 * @param animationEnabled    When true, bars animate from 0 to full height.
 * @param animationTrigger    When the animation starts. See [AnimationTrigger].
 * @param visibilityThreshold Fraction of the chart that must be on-screen before
 *                            the animation fires (only used with [AnimationTrigger.OnVisible]).
 *                            Range 0f..1f; default 0.2f (20 % visible).
 * @param animationSpec       The animation spec for the bar grow-in animation.
 * @param colors              All structural colour tokens. See [BarChartColors].
 */
data class BarChartConfig(
    val distributionMode: DistributionMode      = DistributionMode.Even,
    val legendPosition: LegendPosition          = LegendPosition.TopRight,
    val innerHorizontalPadding: Dp              = 8.dp,
    val outerHorizontalPadding: Dp              = 32.dp,
    val groupSpacing: Dp                        = 20.dp,
    val barSpacing: Dp                          = 4.dp,
    val chartHeight: Dp                         = 280.dp,
    val forceDiagonalLabels: Boolean?           = null,
    val yTickCount: Int                         = 5,
    val zoomEnabled: Boolean                    = true,
    val showZoomIndicator: Boolean              = true,
    val animationEnabled: Boolean               = true,
    val animationTrigger: AnimationTrigger      = AnimationTrigger.OnVisible,
    val visibilityThreshold: Float              = 0.2f,
    val animationSpec: AnimationSpec<Float>     = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness    = Spring.StiffnessLow,
    ),
    val colors: BarChartColors                  = BarChartColors(),
) {
    init {
        require(yTickCount >= 2) { "yTickCount must be at least 2." }
        require(innerHorizontalPadding.value >= 0f) { "innerHorizontalPadding must not be negative." }
        require(outerHorizontalPadding.value >= 0f) { "outerHorizontalPadding must not be negative." }
        require(groupSpacing.value >= 0f) { "groupSpacing must not be negative." }
        require(barSpacing.value >= 0f) { "barSpacing must not be negative." }
        require(chartHeight.value > 0f) { "chartHeight must be positive." }
        require(visibilityThreshold in 0f..1f) { "visibilityThreshold must be in 0..1." }
    }
}
