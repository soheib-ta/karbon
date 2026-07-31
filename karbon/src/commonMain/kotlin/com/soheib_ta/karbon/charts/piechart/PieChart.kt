package com.soheib_ta.karbon.charts.piechart

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.soheib_ta.karbon.charts.common.ChartAnimationTrigger
import com.soheib_ta.karbon.charts.common.ChartLegendPosition
import com.soheib_ta.karbon.charts.piechart.data.PieEntry
import com.soheib_ta.karbon.charts.piechart.internal.PieLegend
import com.soheib_ta.karbon.charts.piechart.internal.calculatePieSlices
import com.soheib_ta.karbon.charts.piechart.internal.drawPieChart
import com.soheib_ta.karbon.charts.piechart.internal.findPieSliceAt
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * A selectable animated pie chart. Set [PieChartConfig.innerRadiusRatio] above
 * zero to render a donut chart and optionally provide center content.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PieChart(
    entries: List<PieEntry>,
    config: PieChartConfig = PieChartConfig(),
    title: (@Composable () -> Unit)? = null,
    description: (@Composable () -> Unit)? = null,
    centerValue: String? = null,
    centerLabel: String? = null,
    centerContent: (@Composable BoxScope.() -> Unit)? = null,
    onSliceSelected: ((PieChartSelection?) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val textMeasurer = rememberTextMeasurer()
    val latestSelectionCallback by rememberUpdatedState(onSliceSelected)

    val slices = remember(entries, config.startAngle, config.clockwise, config.gapAngle) {
        calculatePieSlices(
            entries = entries,
            startAngle = config.startAngle,
            clockwise = config.clockwise,
            gapAngle = config.gapAngle,
        )
    }
    val percentages = remember(slices) { slices.associate { it.index to it.percentage } }
    val total = remember(entries) {
        entries.sumOf { entry ->
            (entry.value.takeIf { it.isFinite() && it > 0f } ?: 0f).toDouble()
        }.toFloat()
    }

    var selectedSlice by remember { mutableStateOf<PieChartSelection?>(null) }
    LaunchedEffect(selectedSlice) {
        if (selectedSlice != null) {
            delay(2_000L)
            selectedSlice = null
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
        label = "PieChartSweepAnimation",
    )

    val topLegend = config.legendPosition == ChartLegendPosition.TopLeft ||
        config.legendPosition == ChartLegendPosition.TopRight
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Column(modifier = modifier) {
        if (title != null || description != null || (topLegend && entries.isNotEmpty())) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                if (config.legendPosition == ChartLegendPosition.TopLeft && entries.isNotEmpty()) {
                    PieLegend(entries, percentages, config.legendPosition, config, Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    title?.invoke()
                    description?.invoke()
                }

                if (config.legendPosition == ChartLegendPosition.TopRight && entries.isNotEmpty()) {
                    Spacer(Modifier.width(12.dp))
                    PieLegend(entries, percentages, config.legendPosition, config, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(config.chartSize)
                .align(Alignment.CenterHorizontally)
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
            contentAlignment = Alignment.Center,
        ) {
            if (slices.isEmpty()) {
                Text(
                    text = config.emptyStateLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = config.colors.emptyStateText,
                    textAlign = TextAlign.Center,
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(entries, slices, canvasSize, selectedSlice) {
                            detectTapGestures { tapOffset ->
                                val center = Offset(
                                    canvasSize.width / 2f,
                                    canvasSize.height / 2f,
                                )
                                val outerRadius = (
                                    min(canvasSize.width, canvasSize.height) / 2f -
                                        with(density) { config.outerPadding.toPx() }
                                    ).coerceAtLeast(1f)
                                val result = findPieSliceAt(
                                    offset = tapOffset,
                                    center = center,
                                    outerRadius = outerRadius,
                                    innerRadius = outerRadius * config.innerRadiusRatio,
                                    slices = slices,
                                    entries = entries,
                                )
                                selectedSlice = result
                                latestSelectionCallback?.invoke(result)
                            }
                        },
                ) {
                    drawPieChart(
                        entries = entries,
                        slices = slices,
                        config = config,
                        selected = selectedSlice,
                        animationProgress = animationProgress,
                        textMeasurer = textMeasurer,
                    )
                }

                if (config.innerRadiusRatio > 0f) {
                    if (centerContent != null) {
                        centerContent()
                    } else if (centerValue != null || centerLabel != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = centerValue ?: config.valueFormatter(total),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = config.colors.centerValue,
                            )
                            if (centerLabel != null) {
                                Text(
                                    text = centerLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = config.colors.centerLabel,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!topLegend && entries.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            PieLegend(entries, percentages, config.legendPosition, config)
        }
    }
}
