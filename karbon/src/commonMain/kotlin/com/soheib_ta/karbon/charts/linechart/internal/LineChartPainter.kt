package com.soheib_ta.karbon.charts.linechart.internal

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.soheib_ta.karbon.charts.linechart.LineChartColors
import com.soheib_ta.karbon.charts.linechart.LineChartConfig
import com.soheib_ta.karbon.charts.linechart.LineChartSelection
import com.soheib_ta.karbon.charts.linechart.data.LineAreaFill
import com.soheib_ta.karbon.charts.linechart.data.LineCurveType
import com.soheib_ta.karbon.charts.linechart.data.LineEntry
import com.soheib_ta.karbon.charts.linechart.data.LineSeries
import kotlin.math.abs

private val AxisLabelStyle = TextStyle(fontSize = 10.sp)
private val TooltipStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
private val ZoomLabelStyle = TextStyle(fontSize = 11.sp)

internal fun DrawScope.drawLineYAxisPanel(
    dimensions: LineChartDimensions,
    ticks: List<Float>,
    textMeasurer: TextMeasurer,
    config: LineChartConfig,
) {
    if (!config.showYAxis) return

    val panelRight = size.width
    val labelStyle = AxisLabelStyle.copy(color = config.colors.yLabel)

    ticks.forEach { value ->
        val y = dimensions.marginTop + dimensions.yForValue(value)
        val measured = textMeasurer.measure(config.valueFormatter(value), style = labelStyle)
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                x = panelRight - measured.size.width - 10f,
                y = y - measured.size.height / 2f,
            ),
        )
        drawLine(
            color = config.colors.axisLine,
            start = Offset(panelRight - 4f, y),
            end = Offset(panelRight, y),
            strokeWidth = 1f,
        )
    }

    drawLine(
        color = config.colors.axisLine,
        start = Offset(panelRight, dimensions.marginTop),
        end = Offset(panelRight, dimensions.marginTop + dimensions.innerHeight),
        strokeWidth = 1f,
    )
}

internal fun DrawScope.drawLineChartPanel(
    entries: List<LineEntry>,
    series: List<LineSeries>,
    dimensions: LineChartDimensions,
    ticks: List<Float>,
    textMeasurer: TextMeasurer,
    config: LineChartConfig,
    animationProgress: Float,
    selected: LineChartSelection?,
) {
    val contentWidth = dimensions.totalContentWidth()
    val xLabelStyle = AxisLabelStyle.copy(color = config.colors.xLabel)
    val progress = animationProgress.coerceIn(0f, 1f)

    translate(top = dimensions.marginTop) {
        if (config.showHorizontalGrid) {
            ticks.forEach { value ->
                val y = dimensions.yForValue(value)
                drawLine(
                    color = config.colors.gridLine,
                    start = Offset(0f, y),
                    end = Offset(contentWidth, y),
                    strokeWidth = 1f,
                )
            }
        }

        if (config.showVerticalGrid) {
            entries.indices.forEach { index ->
                if (index % dimensions.xLabelStep == 0 || index == entries.lastIndex) {
                    val x = dimensions.xForIndex(index)
                    drawLine(
                        color = config.colors.gridLine,
                        start = Offset(x, 0f),
                        end = Offset(x, dimensions.innerHeight),
                        strokeWidth = 1f,
                    )
                }
            }
        }

        if (config.showXAxis) {
            drawLine(
                color = config.colors.axisLine,
                start = Offset(0f, dimensions.innerHeight),
                end = Offset(contentWidth, dimensions.innerHeight),
                strokeWidth = 1f,
            )
        }

        series.forEach { lineSeries ->
            val segments = buildAnimatedSegments(
                entries = entries,
                series = lineSeries,
                dimensions = dimensions,
                progress = progress,
            )

            segments.forEach segmentLoop@ { points ->
                if (points.isEmpty()) return@segmentLoop

                if (lineSeries.areaFill !is LineAreaFill.None) {
                    drawArea(
                        points = points,
                        baselineY = dimensions.zeroBaselineY(),
                        curveType = lineSeries.curveType,
                        areaFill = lineSeries.areaFill,
                        fallbackColor = lineSeries.color,
                    )
                }

                if (points.size == 1) {
                    // A single point has no line segment, so render one marker
                    // even when regular point markers are disabled.
                    drawSeriesPoint(points.first(), lineSeries, selected = false, forceVisible = true)
                } else {
                    val path = buildLinePath(points, lineSeries.curveType)
                    drawPath(
                        path = path,
                        color = lineSeries.color,
                        style = Stroke(
                            width = lineSeries.lineWidth.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }

                if (lineSeries.showPoints) {
                    points.forEach { point ->
                        val isSelected = selected?.entryIndex == point.entryIndex &&
                            selected.seriesKey == lineSeries.key
                        drawSeriesPoint(point, lineSeries, isSelected)
                    }
                }
            }
        }

        entries.forEachIndexed { index, entry ->
            if (index % dimensions.xLabelStep != 0 && index != entries.lastIndex) return@forEachIndexed
            val label = config.xLabelFormatter(entry.label)
            val measured = textMeasurer.measure(label, style = xLabelStyle)
            val centerX = dimensions.xForIndex(index)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x = centerX - measured.size.width / 2f,
                    y = dimensions.innerHeight + 10f,
                ),
            )
        }

        if (selected != null) {
            drawLineTooltip(
                selected = selected,
                entries = entries,
                series = series,
                dimensions = dimensions,
                contentWidth = contentWidth,
                textMeasurer = textMeasurer,
                config = config,
            )
        }
    }
}

internal fun DrawScope.drawLineZoomIndicator(
    scale: Float,
    textMeasurer: TextMeasurer,
    colors: LineChartColors,
) {
    if (!scale.isFinite() || abs(scale - 1f) < 0.05f) return

    val displayScale = ((scale * 10f).toInt() / 10f)
    val measured = textMeasurer.measure(
        "$displayScale×",
        style = ZoomLabelStyle.copy(color = colors.zoomIndicatorText),
    )
    val padH = 6f
    val padV = 4f
    val width = measured.size.width + padH * 2f
    val height = measured.size.height + padV * 2f
    val x = size.width - width - 8f
    val y = 8f

    drawRoundRect(
        color = colors.zoomIndicatorBackground,
        topLeft = Offset(x, y),
        size = Size(width, height),
        cornerRadius = CornerRadius(height / 2f),
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(x + padH, y + padV),
    )
}

private data class RenderedLinePoint(
    val entryIndex: Int,
    val offset: Offset,
)

private fun buildAnimatedSegments(
    entries: List<LineEntry>,
    series: LineSeries,
    dimensions: LineChartDimensions,
    progress: Float,
): List<List<RenderedLinePoint>> {
    val segments = mutableListOf<MutableList<RenderedLinePoint>>()
    var current = mutableListOf<RenderedLinePoint>()
    val baseline = dimensions.zeroBaselineY()

    entries.forEachIndexed { index, entry ->
        val value = entry.values[series.key]
        if (value == null || !value.isFinite()) {
            if (current.isNotEmpty()) segments += current
            current = mutableListOf()
        } else {
            val targetY = dimensions.yForValue(value)
            val animatedY = baseline + (targetY - baseline) * progress
            current += RenderedLinePoint(
                entryIndex = index,
                offset = Offset(dimensions.xForIndex(index), animatedY),
            )
        }
    }

    if (current.isNotEmpty()) segments += current
    return segments
}

private fun buildLinePath(
    points: List<RenderedLinePoint>,
    curveType: LineCurveType,
): Path = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points.first().offset.x, points.first().offset.y)

    points.zipWithNext().forEach { (from, to) ->
        when (curveType) {
            LineCurveType.Straight -> lineTo(to.offset.x, to.offset.y)
            LineCurveType.Smooth -> {
                // Horizontal cubic handles remain inside the rectangle between
                // the two points, preventing overshoot outside the data bounds.
                val midX = (from.offset.x + to.offset.x) / 2f
                cubicTo(
                    midX,
                    from.offset.y,
                    midX,
                    to.offset.y,
                    to.offset.x,
                    to.offset.y,
                )
            }
        }
    }
}

private fun DrawScope.drawArea(
    points: List<RenderedLinePoint>,
    baselineY: Float,
    curveType: LineCurveType,
    areaFill: LineAreaFill,
    fallbackColor: Color,
) {
    if (points.isEmpty()) return

    val path = Path().apply {
        moveTo(points.first().offset.x, baselineY)
        lineTo(points.first().offset.x, points.first().offset.y)
        points.zipWithNext().forEach { (from, to) ->
            when (curveType) {
                LineCurveType.Straight -> lineTo(to.offset.x, to.offset.y)
                LineCurveType.Smooth -> {
                    val midX = (from.offset.x + to.offset.x) / 2f
                    cubicTo(
                        midX,
                        from.offset.y,
                        midX,
                        to.offset.y,
                        to.offset.x,
                        to.offset.y,
                    )
                }
            }
        }
        lineTo(points.last().offset.x, baselineY)
        close()
    }

    when (areaFill) {
        LineAreaFill.None -> Unit
        is LineAreaFill.Solid -> drawPath(
            path = path,
            color = (areaFill.color ?: fallbackColor).copy(alpha = areaFill.alpha),
        )

        is LineAreaFill.VerticalGradient -> drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = areaFill.colors.map { it.copy(alpha = it.alpha * areaFill.alpha) },
                startY = 0f,
                endY = baselineY,
            ),
        )
    }
}

private fun DrawScope.drawSeriesPoint(
    point: RenderedLinePoint,
    series: LineSeries,
    selected: Boolean,
    forceVisible: Boolean = false,
) {
    val configuredRadius = series.pointRadius.toPx()
    val radius = if (forceVisible) configuredRadius.coerceAtLeast(2f) else configuredRadius
    if (radius <= 0f) return

    if (selected) {
        drawCircle(
            color = Color.White,
            radius = radius + 3f,
            center = point.offset,
        )
        drawCircle(
            color = series.color.copy(alpha = 0.25f),
            radius = radius + 7f,
            center = point.offset,
        )
    }

    drawCircle(
        color = series.color,
        radius = if (selected) radius + 1f else radius,
        center = point.offset,
    )
}

private fun DrawScope.drawLineTooltip(
    selected: LineChartSelection,
    entries: List<LineEntry>,
    series: List<LineSeries>,
    dimensions: LineChartDimensions,
    contentWidth: Float,
    textMeasurer: TextMeasurer,
    config: LineChartConfig,
) {
    val entry = entries.getOrNull(selected.entryIndex) ?: return
    val lineSeries = series.firstOrNull { it.key == selected.seriesKey } ?: return
    val value = entry.values[lineSeries.key] ?: return
    if (!value.isFinite()) return

    val point = Offset(
        x = dimensions.xForIndex(selected.entryIndex),
        y = dimensions.yForValue(value),
    )
    val text = "${lineSeries.label} · ${config.xLabelFormatter(entry.label)}\n${config.valueFormatter(value)}"
    val measured = textMeasurer.measure(
        text,
        style = TooltipStyle.copy(color = config.colors.tooltipText),
    )
    val padH = 8f
    val padV = 5f
    val badgeWidth = measured.size.width + padH * 2f
    val badgeHeight = measured.size.height + padV * 2f
    val maxX = (contentWidth - badgeWidth - 4f).coerceAtLeast(4f)
    val badgeX = (point.x - badgeWidth / 2f).coerceIn(4f, maxX)
    val aboveY = point.y - badgeHeight - 10f
    val badgeY = if (aboveY >= 0f) aboveY else (point.y + 10f)

    drawRoundRect(
        color = config.colors.tooltipBackground,
        topLeft = Offset(badgeX, badgeY),
        size = Size(badgeWidth, badgeHeight),
        cornerRadius = CornerRadius(7f),
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(badgeX + padH, badgeY + padV),
    )
}
