package com.soheib_ta.karbon.charts.barchart.internal

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.soheib_ta.karbon.charts.barchart.BarChartConfig
import com.soheib_ta.karbon.charts.barchart.DistributionMode
import com.soheib_ta.karbon.charts.barchart.data.BarEntry
import com.soheib_ta.karbon.charts.barchart.data.BarSeries
import kotlin.math.ceil

// ─────────────────────────────────────────────────────────────────────────────
// Layout constants
// ─────────────────────────────────────────────────────────────────────────────

private val MARGIN_RIGHT_DP           = 16.dp
private val MARGIN_TOP_DP             = 12.dp
private val MARGIN_BOTTOM_DP          = 48.dp
private val MARGIN_BOTTOM_DIAGONAL_DP = 72.dp

/** Estimated px-per-character for diagonal label auto-detection. */
private const val CHARS_PER_PX = 7f

/** Minimum rendered bar width in px (prevents bars from becoming invisible). */
private const val MIN_BAR_WIDTH_PX = 3f

// ─────────────────────────────────────────────────────────────────────────────
// SelectedBar
// ─────────────────────────────────────────────────────────────────────────────

/** Represents a specific bar that was tapped by the user. */
internal data class SelectedBar(val groupIndex: Int, val seriesKey: String)

// ─────────────────────────────────────────────────────────────────────────────
// ChartDimensions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pre-computed layout metrics for one draw pass.
 *
 * Immutable value type — recomputed whenever size, zoom, or config change.
 * Holding a stable reference and re-using it across multiple draw calls
 * within the same frame is safe (and encouraged via [remember]).
 */
internal data class ChartDimensions(
    val marginLeft: Float,
    val marginTop: Float,
    val marginBottom: Float,
    val marginRight: Float,

    val innerWidth: Float,
    val innerHeight: Float,

    val groupWidth: Float,
    val barWidth: Float,
    val groupSpacing: Float,
    val barSpacing: Float,

    val groupCount: Int,

    val yMax: Float,
    val diagonal: Boolean,
    val horizontalPadding: Float = 0f,
) {
    /** Maps a data value to a Y pixel offset (0 = top of plot area). */
    fun yForValue(value: Float): Float = innerHeight - (value / yMax) * innerHeight

    /** Left-edge pixel of group [index] within the inner (scrollable) area. */
    fun groupLeft(index: Int): Float = horizontalPadding + index * (groupWidth + groupSpacing)

    /**
     * Total pixel width of all scrollable bar content.
     * Used to derive `maxScrollOffset = max(0, totalContentWidth - canvasWidth)`.
     */
    fun totalContentWidth(): Float =
        horizontalPadding * 2 + groupCount * groupWidth + maxOf(0, groupCount - 1) * groupSpacing
}

// ─────────────────────────────────────────────────────────────────────────────
// computeDimensions (DrawScope convenience overload)
// ─────────────────────────────────────────────────────────────────────────────

internal fun DrawScope.computeDimensions(
    size: Size,
    entries: List<BarEntry>,
    series: List<BarSeries>,
    config: BarChartConfig,
    includeLeftMargin: Boolean = true,
    zoomScale: Float = 1f,
): ChartDimensions = computeDimensionsImpl(this, size, entries, series, config, includeLeftMargin, zoomScale)

// ─────────────────────────────────────────────────────────────────────────────
// computeDimensionsWithDensity (usable from pointerInput / outside DrawScope)
// ─────────────────────────────────────────────────────────────────────────────

internal fun computeDimensionsWithDensity(
    density: Density,
    size: Size,
    entries: List<BarEntry>,
    series: List<BarSeries>,
    config: BarChartConfig,
    includeLeftMargin: Boolean = true,
    zoomScale: Float = 1f,
): ChartDimensions = computeDimensionsImpl(density, size, entries, series, config, includeLeftMargin, zoomScale)

// ─────────────────────────────────────────────────────────────────────────────
// computeDimensionsImpl — shared implementation
//
//   zoomScale multiplies the base groupWidth:
//     scale = 1  → standard layout.
//     scale > 1  → wider groups; content may overflow → panning enabled.
//     scale < 1  → narrower groups; more visible without panning.
//
//   groupSpacing is intentionally NOT scaled so the gap between groups
//   stays visually constant regardless of zoom level.
//
//   includeLeftMargin:
//     true  → Y-axis canvas (needs marginLeft for label positioning).
//     false → Bars canvas (left edge IS the plot origin; marginLeft = 0).
// ─────────────────────────────────────────────────────────────────────────────

private fun computeDimensionsImpl(
    density: Density,
    size: Size,
    entries: List<BarEntry>,
    series: List<BarSeries>,
    config: BarChartConfig,
    includeLeftMargin: Boolean,
    zoomScale: Float,
): ChartDimensions = with(density) {
    val groupCount  = entries.size
    val seriesCount = series.size

    val marginLeft  = if (includeLeftMargin) config.outerHorizontalPadding.toPx() else 0f
    val marginRight = if (includeLeftMargin) MARGIN_RIGHT_DP.toPx() else 0f
    val marginTop   = MARGIN_TOP_DP.toPx()
    val hPad        = config.innerHorizontalPadding.toPx()

    val groupSpacingPx = config.groupSpacing.toPx()
    val barSpacingPx   = config.barSpacing.toPx()

    // innerWidth excludes the Y-axis panel and (for the bars canvas) horizontal padding
    val innerWidth = size.width - marginLeft - marginRight -
            (if (!includeLeftMargin) hPad * 2 else 0f)

    // ── Base group width (at scale = 1) ───────────────────────────────────
    val baseGroupWidth: Float = when (val mode = config.distributionMode) {
        is DistributionMode.Even ->
            if (groupCount > 0) (innerWidth - (groupCount - 1) * groupSpacingPx) / groupCount
            else innerWidth
        is DistributionMode.Fixed -> mode.groupWidth.toPx()
    }

    val groupWidth = baseGroupWidth * zoomScale

    // ── Diagonal label detection ──────────────────────────────────────────
    //   Uses zoomed groupWidth so labels flip back to horizontal when the
    //   user zooms in enough.
    val longestLabelPx = (entries.maxOfOrNull { it.label.length } ?: 0) * CHARS_PER_PX
    val diagonal = config.forceDiagonalLabels ?: (groupWidth < longestLabelPx)

    val marginBottom = if (diagonal) MARGIN_BOTTOM_DIAGONAL_DP.toPx() else MARGIN_BOTTOM_DP.toPx()
    val innerHeight  = size.height - marginTop - marginBottom

    val barWidth = maxOf(MIN_BAR_WIDTH_PX, (groupWidth - (seriesCount - 1) * barSpacingPx) / seriesCount)

    // ── Y ceiling — rounds up to the nearest 1 000 ────────────────────────
    val maxVal = computeMaxValue(entries, series)
    val yMax   = (ceil(maxVal / 1_000.0) * 1_000).toFloat().coerceAtLeast(1_000f)

    return ChartDimensions(
        marginLeft        = marginLeft,
        marginTop         = marginTop,
        marginBottom      = marginBottom,
        marginRight       = marginRight,
        innerWidth        = innerWidth,
        innerHeight       = innerHeight,
        groupWidth        = groupWidth,
        barWidth          = barWidth,
        groupSpacing      = groupSpacingPx,
        barSpacing        = barSpacingPx,
        groupCount        = groupCount,
        yMax              = yMax,
        diagonal          = diagonal,
        horizontalPadding = if (includeLeftMargin) 0f else hPad,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// computeMaxValue
//
//   Extracted so callers (e.g. BarChart) can cache the result with
//   remember(entries, series) and avoid re-scanning on every draw frame.
// ─────────────────────────────────────────────────────────────────────────────

internal fun computeMaxValue(entries: List<BarEntry>, series: List<BarSeries>): Float =
    entries.flatMap { entry -> series.map { s -> entry.values[s.key] ?: 0f } }.maxOrNull() ?: 0f

// ─────────────────────────────────────────────────────────────────────────────
// computeInitialBarsCanvasWidth
//   Returns the pixel width of the bars canvas at zoom scale = 1 in Fixed mode.
//   Used only as an initial canvas size hint.
// ─────────────────────────────────────────────────────────────────────────────

internal fun computeInitialBarsCanvasWidth(
    entries: List<BarEntry>,
    config: BarChartConfig,
    density: Density,
): Float = with(density) {
    val mode = config.distributionMode as? DistributionMode.Fixed ?: return@with 0f
    val n              = entries.size
    val groupWidthPx   = mode.groupWidth.toPx()
    val groupSpacingPx = config.groupSpacing.toPx()
    n * groupWidthPx + maxOf(0, n - 1) * groupSpacingPx +
            MARGIN_RIGHT_DP.toPx() + config.innerHorizontalPadding.toPx() * 2
}
