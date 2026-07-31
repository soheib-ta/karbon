package com.soheib_ta.karbon.charts.common

import kotlin.math.abs
import kotlin.math.round

/** Placement shared by chart legends. */
enum class ChartLegendPosition { TopLeft, TopRight, BottomLeft, BottomRight }

/** Controls when an entry animation begins. */
enum class ChartAnimationTrigger {
    OnCompose,
    OnVisible,
}

/** A reusable legend descriptor for applications that render their own legend. */
data class ChartLegendItem(
    val key: String,
    val label: String,
)

/**
 * Compact, locale-independent formatter suitable for chart axes and tooltips.
 * Consumers that need locale-aware output should pass a custom formatter.
 */
fun compactChartValue(value: Float): String {
    if (!value.isFinite()) return "—"

    val magnitude = abs(value)
    return when {
        magnitude >= 1_000_000_000f -> "${roundedOneDecimal(value / 1_000_000_000f)}B"
        magnitude >= 1_000_000f -> "${roundedOneDecimal(value / 1_000_000f)}M"
        magnitude >= 1_000f -> "${roundedOneDecimal(value / 1_000f)}k"
        value == value.toInt().toFloat() -> value.toInt().toString()
        else -> roundedOneDecimal(value).toString()
    }
}

internal fun Float.finiteOrZero(): Float = if (isFinite()) this else 0f
internal fun Float.nonNegativeFiniteOrZero(): Float = finiteOrZero().coerceAtLeast(0f)

private fun roundedOneDecimal(value: Float): Float = round(value * 10f) / 10f
