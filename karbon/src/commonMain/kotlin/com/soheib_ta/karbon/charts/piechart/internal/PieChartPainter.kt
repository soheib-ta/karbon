package com.soheib_ta.karbon.charts.piechart.internal

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.soheib_ta.karbon.charts.piechart.PieChartConfig
import com.soheib_ta.karbon.charts.piechart.PieChartSelection
import com.soheib_ta.karbon.charts.piechart.data.PieEntry
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val TooltipStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

internal fun DrawScope.drawPieChart(
    entries: List<PieEntry>,
    slices: List<PieSliceGeometry>,
    config: PieChartConfig,
    selected: PieChartSelection?,
    animationProgress: Float,
    textMeasurer: TextMeasurer,
) {
    if (slices.isEmpty()) return

    val padding = config.outerPadding.toPx()
    val outerRadius = (min(size.width, size.height) / 2f - padding).coerceAtLeast(1f)
    val center = Offset(size.width / 2f, size.height / 2f)
    val innerRadius = outerRadius * config.innerRadiusRatio
    val progress = animationProgress.coerceIn(0f, 1f)

    slices.forEach { slice ->
        val entry = entries.getOrNull(slice.index) ?: return@forEach
        val selectedOffset = if (selected?.index == slice.index) config.selectedOffset.toPx() else 0f
        val angleRadians = slice.midAngle * PI.toFloat() / 180f
        val shiftedCenter = Offset(
            x = center.x + cos(angleRadians) * selectedOffset,
            y = center.y + sin(angleRadians) * selectedOffset,
        )
        val sweep = slice.sweepAngle * progress

        if (config.innerRadiusRatio <= 0f) {
            drawArc(
                color = entry.color,
                startAngle = slice.startAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = Offset(
                    shiftedCenter.x - outerRadius,
                    shiftedCenter.y - outerRadius,
                ),
                size = Size(outerRadius * 2f, outerRadius * 2f),
            )
        } else {
            val strokeWidth = outerRadius - innerRadius
            val arcRadius = innerRadius + strokeWidth / 2f
            drawArc(
                color = entry.color,
                startAngle = slice.startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(
                    shiftedCenter.x - arcRadius,
                    shiftedCenter.y - arcRadius,
                ),
                size = Size(arcRadius * 2f, arcRadius * 2f),
                style = Stroke(width = strokeWidth),
            )
        }
    }

    selected?.let { selection ->
        val slice = slices.firstOrNull { it.index == selection.index } ?: return@let
        drawPieTooltip(
            selection = selection,
            slice = slice,
            center = center,
            outerRadius = outerRadius,
            textMeasurer = textMeasurer,
            config = config,
        )
    }
}

private fun DrawScope.drawPieTooltip(
    selection: PieChartSelection,
    slice: PieSliceGeometry,
    center: Offset,
    outerRadius: Float,
    textMeasurer: TextMeasurer,
    config: PieChartConfig,
) {
    val angleRadians = slice.midAngle * PI.toFloat() / 180f
    val anchorRadius = outerRadius * 0.82f
    val anchor = Offset(
        x = center.x + cos(angleRadians) * anchorRadius,
        y = center.y + sin(angleRadians) * anchorRadius,
    )
    val text = "${selection.label}\n${config.valueFormatter(selection.value)} · " +
        config.percentageFormatter(selection.percentage)
    val measured = textMeasurer.measure(
        text,
        style = TooltipStyle.copy(color = config.colors.tooltipText),
    )
    val padH = 8f
    val padV = 5f
    val width = measured.size.width + padH * 2f
    val height = measured.size.height + padV * 2f
    val x = (anchor.x - width / 2f).coerceIn(4f, (size.width - width - 4f).coerceAtLeast(4f))
    val y = (anchor.y - height / 2f).coerceIn(4f, (size.height - height - 4f).coerceAtLeast(4f))

    drawRoundRect(
        color = config.colors.tooltipBackground,
        topLeft = Offset(x, y),
        size = Size(width, height),
        cornerRadius = CornerRadius(7f),
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(x + padH, y + padV),
    )
}
