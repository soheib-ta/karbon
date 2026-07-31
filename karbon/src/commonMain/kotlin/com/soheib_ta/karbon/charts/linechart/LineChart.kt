package com.soheib_ta.karbon.charts.linechart

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.soheib_ta.karbon.charts.common.ChartAnimationTrigger
import com.soheib_ta.karbon.charts.common.ChartLegendPosition
import com.soheib_ta.karbon.charts.linechart.data.LineChartZoomState
import com.soheib_ta.karbon.charts.linechart.data.LineEntry
import com.soheib_ta.karbon.charts.linechart.data.LineSeries
import com.soheib_ta.karbon.charts.linechart.data.rememberLineChartZoomState
import com.soheib_ta.karbon.charts.linechart.internal.LineLegend
import com.soheib_ta.karbon.charts.linechart.internal.computeLineDimensions
import com.soheib_ta.karbon.charts.linechart.internal.computeLineValueRange
import com.soheib_ta.karbon.charts.linechart.internal.drawLineChartPanel
import com.soheib_ta.karbon.charts.linechart.internal.drawLineYAxisPanel
import com.soheib_ta.karbon.charts.linechart.internal.drawLineZoomIndicator
import com.soheib_ta.karbon.charts.linechart.internal.findNearestLinePoint
import com.soheib_ta.karbon.charts.linechart.internal.generateLineTicks
import kotlinx.coroutines.delay

/**
 * A reusable multi-series line chart for Compose Multiplatform.
 *
 * Missing values split a series into independent line segments. The chart
 * supports smooth or straight paths, optional area fills, point selection,
 * tooltips, entry animation, pinch zoom, drag pan and desktop wheel input.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LineChart(
    entries: List<LineEntry>,
    series: List<LineSeries>,
    config: LineChartConfig = LineChartConfig(),
    zoomState: LineChartZoomState = rememberLineChartZoomState(),
    title: (@Composable () -> Unit)? = null,
    description: (@Composable () -> Unit)? = null,
    onPointSelected: ((LineChartSelection?) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val windowInfo = LocalWindowInfo.current
    val latestSelectionCallback by rememberUpdatedState(onPointSelected)

    val valueRange = remember(
        entries,
        series,
        config.yMin,
        config.yMax,
        config.includeZeroInRange,
        config.yTickCount,
    ) {
        computeLineValueRange(
            entries = entries,
            series = series,
            requestedMin = config.yMin,
            requestedMax = config.yMax,
            includeZero = config.includeZeroInRange,
            tickCount = config.yTickCount,
        )
    }
    val ticks = remember(valueRange, config.yTickCount) {
        generateLineTicks(valueRange, config.yTickCount)
    }
    val hasRenderableData = remember(entries, series) {
        entries.any { entry ->
            series.any { lineSeries -> entry.values[lineSeries.key]?.isFinite() == true }
        }
    }

    var selectedPoint by remember { mutableStateOf<LineChartSelection?>(null) }
    LaunchedEffect(selectedPoint) {
        if (selectedPoint != null) {
            delay(2_000L)
            selectedPoint = null
            latestSelectionCallback?.invoke(null)
        }
    }

    var animationTriggered by remember { mutableStateOf(false) }
    val windowHeightPx = windowInfo.containerSize.height.toFloat()

    LaunchedEffect(config.animationEnabled, config.animationTrigger) {
        if (config.animationEnabled && config.animationTrigger == ChartAnimationTrigger.OnCompose) {
            animationTriggered = true
        }
    }

    val animationProgress by animateFloatAsState(
        targetValue = if (animationTriggered || !config.animationEnabled) 1f else 0f,
        animationSpec = config.animationSpec,
        label = "LineChartEntryAnimation",
    )

    val topLegend = config.legendPosition == ChartLegendPosition.TopLeft ||
        config.legendPosition == ChartLegendPosition.TopRight

    var plotSize by remember { mutableStateOf(IntSize.Zero) }
    val dimensions = remember(
        plotSize,
        entries,
        valueRange,
        zoomState.scale,
        config.distributionMode,
        config.innerHorizontalPadding,
        config.maxVisibleXLabels,
        density,
    ) {
        computeLineDimensions(
            density = density,
            size = Size(
                plotSize.width.coerceAtLeast(1).toFloat(),
                plotSize.height.coerceAtLeast(1).toFloat(),
            ),
            entries = entries,
            config = config,
            valueRange = valueRange,
            zoomScale = zoomState.scale,
        )
    }
    val maxScrollOffset = (dimensions.totalContentWidth() - plotSize.width).coerceAtLeast(0f)

    LaunchedEffect(maxScrollOffset, zoomState.scale) {
        zoomState.clampScroll(maxScrollOffset)
    }

    Column(modifier = modifier) {
        if (title != null || description != null || (topLegend && series.isNotEmpty())) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                if (config.legendPosition == ChartLegendPosition.TopLeft && series.isNotEmpty()) {
                    LineLegend(series, config.legendPosition, Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    title?.invoke()
                    description?.invoke()
                }

                if (config.legendPosition == ChartLegendPosition.TopRight && series.isNotEmpty()) {
                    Spacer(Modifier.width(12.dp))
                    LineLegend(series, config.legendPosition, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (!hasRenderableData) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(config.chartHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = config.emptyStateLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = config.colors.emptyStateText,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(config.chartHeight)
                    .onGloballyPositioned { coordinates ->
                        if (!animationTriggered &&
                            config.animationEnabled &&
                            config.animationTrigger == ChartAnimationTrigger.OnVisible
                        ) {
                            val height = coordinates.size.height.toFloat()
                            if (height > 0f) {
                                val top = coordinates.positionInWindow().y
                                val visibleTop = top.coerceAtLeast(0f)
                                val visibleBottom = (top + height).coerceAtMost(windowHeightPx)
                                val visibleFraction =
                                    (visibleBottom - visibleTop).coerceAtLeast(0f) / height
                                if (visibleFraction >= config.visibilityThreshold) {
                                    animationTriggered = true
                                }
                            }
                        }
                    },
            ) {
                Canvas(
                    modifier = Modifier
                        .width(config.outerHorizontalPadding)
                        .fillMaxHeight(),
                ) {
                    drawLineYAxisPanel(
                        dimensions = dimensions,
                        ticks = ticks,
                        textMeasurer = textMeasurer,
                        config = config,
                    )
                }

                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onSizeChanged { plotSize = it }
                        .pointerInput(zoomState, maxScrollOffset, config.zoomEnabled) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == PointerEventType.Scroll) {
                                        val delta = event.changes.first().scrollDelta
                                        if (config.zoomEnabled && event.keyboardModifiers.isCtrlPressed) {
                                            zoomState.applyZoom(if (delta.y > 0f) 0.9f else 1.1f)
                                        } else {
                                            val dx = if (delta.x != 0f) {
                                                -delta.x * 20f
                                            } else {
                                                -delta.y * 20f
                                            }
                                            zoomState.applyPan(dx, maxScrollOffset)
                                        }
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                        .pointerInput(entries, series, dimensions, zoomState.scrollOffset) {
                            detectTapGestures { tapOffset ->
                                val plotOffset = Offset(
                                    x = tapOffset.x + zoomState.scrollOffset,
                                    y = tapOffset.y - dimensions.marginTop,
                                )
                                val result = findNearestLinePoint(
                                    offset = plotOffset,
                                    entries = entries,
                                    series = series,
                                    dimensions = dimensions,
                                    hitRadius = with(density) { config.selectionRadius.toPx() },
                                )
                                selectedPoint = result
                                latestSelectionCallback?.invoke(result)
                            }
                        }
                        .pointerInput(zoomState, maxScrollOffset, config.zoomEnabled) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                if (config.zoomEnabled) zoomState.applyZoom(zoom)
                                zoomState.applyPan(pan.x, maxScrollOffset)
                            }
                        },
                ) {
                    clipRect {
                        translate(left = -zoomState.scrollOffset) {
                            drawLineChartPanel(
                                entries = entries,
                                series = series,
                                dimensions = dimensions,
                                ticks = ticks,
                                textMeasurer = textMeasurer,
                                config = config,
                                animationProgress = animationProgress,
                                selected = selectedPoint,
                            )
                        }
                    }

                    if (config.showZoomIndicator) {
                        drawLineZoomIndicator(zoomState.scale, textMeasurer, config.colors)
                    }
                }

                Spacer(Modifier.width(config.outerHorizontalPadding))
            }
        }

        if (!topLegend && series.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LineLegend(series, config.legendPosition)
        }
    }
}
