package com.soheib_ta.karbon.charts.barchart.internal

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.soheib_ta.karbon.charts.barchart.BarChartColors
import com.soheib_ta.karbon.charts.barchart.data.BarEntry
import com.soheib_ta.karbon.charts.barchart.data.BarSeries
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
// TextStyle cache
//
//   TextStyle objects are cheap, but creating them on every draw frame still
//   allocates. These are module-level vals reused across all draw calls.
//   Colour tokens are injected at call-site via .copy(color = ...) only when
//   the default differs — which is rare (custom BarChartColors usage).
// ─────────────────────────────────────────────────────────────────────────────

private val YLabelStyle      = TextStyle(fontSize = 10.sp)
private val XLabelStyle      = TextStyle(fontSize = 10.sp)
private val TooltipTextStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold)
private val ZoomLabelStyle   = TextStyle(fontSize = 11.sp)

// ─────────────────────────────────────────────────────────────────────────────
// drawYAxisPanel  (fixed, non-scrolling canvas)
// ─────────────────────────────────────────────────────────────────────────────

internal fun DrawScope.drawYAxisPanel(
    dims: ChartDimensions,
    textMeasurer: TextMeasurer,
    colors: BarChartColors,
    tickCount: Int,
) {
    val panelRight   = size.width
    val yLabelStyle  = YLabelStyle.copy(color = colors.yLabel)
    val axisColor    = colors.axisLine

    repeat(tickCount) { i ->
        val value = dims.yMax * i / (tickCount - 1)
        val y     = dims.marginTop + dims.yForValue(value)

        val measured = textMeasurer.measure(formatYLabel(value), style = yLabelStyle)

        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                x = panelRight - measured.size.width - 10f,
                y = y - measured.size.height / 2f,
            ),
        )

        // Short tick connecting to the bars canvas
        drawLine(
            color       = axisColor,
            start       = Offset(panelRight - 4f, y),
            end         = Offset(panelRight, y),
            strokeWidth = 1f,
        )
    }

    // Vertical border on the right edge of the Y-axis panel
    drawLine(
        color       = axisColor,
        start       = Offset(panelRight, dims.marginTop),
        end         = Offset(panelRight, dims.marginTop + dims.innerHeight),
        strokeWidth = 1f,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// drawBarsPanel  (scrollable canvas — caller applies translate(-scrollOffset))
//
//   Draws in LOCAL coordinate space. The caller is responsible for applying
//   translate(-scrollOffset, 0f) so that panning works correctly.
//
//   Grid lines span 0..totalContentWidth so they cover the full scrollable
//   area rather than just the visible viewport.
// ─────────────────────────────────────────────────────────────────────────────

internal fun DrawScope.drawBarsPanel(
    entries: List<BarEntry>,
    series: List<BarSeries>,
    dims: ChartDimensions,
    textMeasurer: TextMeasurer,
    fullWidth: Float,
    colors: BarChartColors,
    tickCount: Int,
    animationProgress: Float = 1f,
    selectedBar: SelectedBar? = null,
) {
    val xLabelStyle   = XLabelStyle.copy(color = colors.xLabel)
    val tooltipStyle  = TooltipTextStyle.copy(color = colors.tooltipText)
    val contentWidth  = maxOf(dims.totalContentWidth(), fullWidth)

    translate(left = 0f, top = dims.marginTop) {

        // ── 1. Horizontal grid lines ───────────────────────────────────────
        repeat(tickCount) { i ->
            val y = dims.yForValue(dims.yMax * i / (tickCount - 1))
            drawLine(
                color       = colors.gridLine,
                start       = Offset(0f, y),
                end         = Offset(contentWidth, y),
                strokeWidth = 1f,
            )
        }

        // ── 2. X-axis baseline ────────────────────────────────────────────
        drawLine(
            color       = colors.axisLine,
            start       = Offset(0f, dims.innerHeight),
            end         = Offset(contentWidth, dims.innerHeight),
            strokeWidth = 1f,
        )

        // ── 3. Bars ───────────────────────────────────────────────────────
        entries.forEachIndexed { groupIdx, entry ->
            val groupLeft = dims.groupLeft(groupIdx)

            series.forEachIndexed { seriesIdx, s ->
                val value      = entry.values[s.key] ?: 0f
                val barHeight  = (value / dims.yMax) * dims.innerHeight * animationProgress
                val barLeft    = groupLeft + seriesIdx * (dims.barWidth + dims.barSpacing)
                val barTop     = dims.innerHeight - barHeight
                val topLeft    = Offset(barLeft, barTop)
                val barSize    = Size(dims.barWidth, barHeight)
                val isSelected = selectedBar?.groupIndex == groupIdx && selectedBar.seriesKey == s.key

                val barColors = if (isSelected) s.colors.map { it.copy(alpha = 0.8f) } else s.colors
                val cornerRadius = CornerRadius(s.radius.toPx())

                // ── Draw bar ─────────────────────────────────────────────
                if (s.isGradient) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = barColors,
                            startY = topLeft.y,
                            endY   = topLeft.y + barSize.height,
                        ),
                        topLeft      = topLeft,
                        size         = barSize,
                        cornerRadius = cornerRadius,
                    )
                } else {
                    drawRoundRect(
                        color        = barColors[0],
                        topLeft      = topLeft,
                        size         = barSize,
                        cornerRadius = cornerRadius,
                    )
                }

                // ── Tooltip badge when selected ───────────────────────────
                if (isSelected) {
                    drawTooltip(
                        value     = value,
                        barLeft   = barLeft,
                        barTop    = barTop,
                        barWidth  = dims.barWidth,
                        style     = tooltipStyle,
                        bgColor   = colors.tooltipBackground,
                        measurer  = textMeasurer,
                    )
                }
            }
        }

        // ── 4. X-axis labels ──────────────────────────────────────────────
        entries.forEachIndexed { i, entry ->
            val centerX  = dims.groupLeft(i) + dims.groupWidth / 2f
            val baseY    = dims.innerHeight
            val measured = textMeasurer.measure(entry.label.uppercase(), style = xLabelStyle)

            if (dims.diagonal) {
                rotate(degrees = -45f, pivot = Offset(centerX, baseY + 6f)) {
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(centerX - measured.size.width.toFloat(), baseY + 6f),
                    )
                }
            } else {
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(centerX - measured.size.width / 2f, baseY + 10f),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// drawTooltip — value badge drawn above the selected bar
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawTooltip(
    value: Float,
    barLeft: Float,
    barTop: Float,
    barWidth: Float,
    style: TextStyle,
    bgColor: Color,
    measurer: TextMeasurer,
) {
    val label    = value.toCompactString()
    val measured = measurer.measure(label, style = style)

    val padH   = 6f
    val padV   = 2f
    val badgeW = measured.size.width.toFloat() + padH * 2f
    val badgeH = measured.size.height.toFloat() + padV * 2f

    val badgeX = barLeft + (barWidth - badgeW) / 2f
    val badgeY = barTop - badgeH - 4f

    drawRoundRect(
        color        = bgColor,
        topLeft      = Offset(badgeX, badgeY - padV),
        size         = Size(badgeW, badgeH),
        cornerRadius = CornerRadius(4f),
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(badgeX + padH, badgeY),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// drawZoomIndicator
//
//   A "2.4×" pill in the top-right corner drawn in VIEWPORT coordinates
//   (outside the translate(-scrollOffset) block) so it never scrolls away.
// ─────────────────────────────────────────────────────────────────────────────

internal fun DrawScope.drawZoomIndicator(
    scale: Float,
    textMeasurer: TextMeasurer,
    colors: BarChartColors,
) {
    if (abs(scale - 1f) < 0.05f) return

    val label    = "${(scale * 10).toInt() / 10f}×"
    val measured = textMeasurer.measure(label, style = ZoomLabelStyle.copy(color = colors.zoomIndicatorText))

    val padH   = 6f
    val padV   = 4f
    val badgeW = measured.size.width + padH * 2
    val badgeH = measured.size.height + padV * 2
    val bx     = size.width - badgeW - 8f
    val by     = 8f

    drawRoundRect(
        color        = colors.zoomIndicatorBackground,
        topLeft      = Offset(bx, by),
        size         = Size(badgeW, badgeH),
        cornerRadius = CornerRadius(badgeH / 2f),
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(bx + padH, by + padV),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// findTappedBar — hit test
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns the [SelectedBar] at [offset] (in plot space, i.e. shifted by marginTop),
 * or null if no bar is hit.
 */
internal fun findTappedBar(
    offset: Offset,
    entries: List<BarEntry>,
    series: List<BarSeries>,
    dims: ChartDimensions,
): SelectedBar? {
    entries.forEachIndexed { groupIdx, entry ->
        val groupLeft = dims.groupLeft(groupIdx)
        series.forEachIndexed { seriesIdx, s ->
            val value     = entry.values[s.key] ?: 0f
            val barHeight = (value / dims.yMax) * dims.innerHeight
            val barLeft   = groupLeft + seriesIdx * (dims.barWidth + dims.barSpacing)

            if (offset.x in barLeft..(barLeft + dims.barWidth) &&
                offset.y in (dims.innerHeight - barHeight)..dims.innerHeight
            ) {
                return SelectedBar(groupIdx, s.key)
            }
        }
    }
    return null
}

// ─────────────────────────────────────────────────────────────────────────────
// Private helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun formatYLabel(value: Float): String = when {
    value == 0f      -> "0"
    value >= 1_000f  -> "${(value / 1_000).toInt()}k"
    else             -> value.toInt().toString()
}

private fun Float.toCompactString(): String {
    val abs = abs(this.toDouble())
    return when {
        abs >= 1_000_000_000 -> "${(this / 1_000_000_000).roundToOneDecimal()}B"
        abs >= 1_000_000     -> "${(this / 1_000_000).roundToOneDecimal()}M"
        abs >= 1_000         -> "${(this / 1_000).roundToOneDecimal()}k"
        else                 -> this.toString()
    }
}

private fun Float.roundToOneDecimal(): Float = (this * 10).toInt() / 10f
