package com.soheib_ta.karbon.charts.linechart.data

/**
 * Represents one X-axis category.
 *
 * Missing keys and null/non-finite values create a gap in the corresponding
 * line rather than being silently rendered as zero.
 */
data class LineEntry(
    val label: String,
    val values: Map<String, Float?>,
)
