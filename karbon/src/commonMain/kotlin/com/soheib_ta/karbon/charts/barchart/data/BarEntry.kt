package com.soheib_ta.karbon.charts.barchart.data

/**
 * Represents one group on the X-axis.
 *
 * @param label  The label shown below this group (e.g. "Surgery").
 * @param values Map of seriesKey → Float value.
 *               Keys must match [BarSeries.key] for the bars to be drawn.
 *
 * Example:
 * ```
 * BarEntry(
 *     label = "Surgery",
 *     values = mapOf("visits" to 4200f, "revenue" to 3100f)
 * )
 * ```
 */
data class BarEntry(
    val label: String,
    val values: Map<String, Float>,
)
