package com.soheib_ta.karbon.charts.barchart.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Defines one data series: how to look it up in [BarEntry.values],
 * what to display in the legend, and which colour(s) to paint its bars.
 *
 * @param key    Must match a key in [BarEntry.values].
 * @param label  Human-readable name shown in the legend.
 * @param colors One colour for a solid bar; two or more for a vertical gradient.
 * @param radius Corner radius applied to the top of each bar.
 *
 * Example:
 * ```
 * BarSeries(key = "revenue", label = "Revenue", colors = listOf(Color(0xFF1E3A5F)))
 * ```
 */
data class BarSeries(
    val key: String,
    val label: String,
    val colors: List<Color>,
    val radius: Dp = 4.dp,
) {
    init {
        require(key.isNotBlank()) { "BarSeries key must not be blank." }
        require(colors.isNotEmpty()) { "BarSeries '$key' must have at least one colour." }
        require(radius.value >= 0f) { "BarSeries radius must not be negative." }
    }

    /** True when this series should be drawn with a vertical gradient. */
    val isGradient: Boolean get() = colors.size >= 2
}
