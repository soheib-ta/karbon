package com.soheib_ta.karbon.charts.barchart

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.soheib_ta.karbon.charts.barchart.data.BarChartZoomState
import com.soheib_ta.karbon.charts.barchart.data.BarEntry
import com.soheib_ta.karbon.charts.barchart.data.BarSeries
import com.soheib_ta.karbon.charts.barchart.data.rememberBarChartZoomState
import com.soheib_ta.karbon.charts.barchart.internal.Legend
import com.soheib_ta.karbon.charts.barchart.internal.SelectedBar
import com.soheib_ta.karbon.charts.barchart.internal.computeDimensions
import com.soheib_ta.karbon.charts.barchart.internal.computeDimensionsWithDensity
import com.soheib_ta.karbon.charts.barchart.internal.drawBarsPanel
import com.soheib_ta.karbon.charts.barchart.internal.drawYAxisPanel
import com.soheib_ta.karbon.charts.barchart.internal.drawZoomIndicator
import com.soheib_ta.karbon.charts.barchart.internal.findTappedBar
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// BarChart — public entry-point composable
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A grouped bar chart with pinch-to-zoom and drag-to-pan, built entirely in
 * `commonMain` for Compose Multiplatform (Android, iOS, Desktop, Wasm/JS).
 *
 * ## Layout
 * ```
 * ┌──────────────────────────────────────────────────────────┐
 * │  Title                          Legend (top-right)       │
 * │  Description                                             │
 * ├─────────────┬────────────────────────────────────────────┤
 * │  Y-axis     │  Bars + grid                               │
 * │  (fixed,    │  ← pinch to zoom · drag to pan →           │
 * │  never      │                                            │
 * │  scrolls)   │  [2.4×]  ← zoom indicator (top-right)      │
 * ├─────────────┴────────────────────────────────────────────┤
 * │  Legend (bottom-right / bottom-left)                     │
 * └──────────────────────────────────────────────────────────┘
 * ```
 *
 * ## Animation
 * By default ([AnimationTrigger.OnVisible]) bars animate in only when the
 * chart scrolls into view.  Change to [AnimationTrigger.OnCompose] to
 * animate immediately, or set `animationEnabled = false` to skip it.
 *
 * ## Zoom
 * - **Pinch** → adjusts `zoomState.scale` which scales `groupWidth`.
 * - **Drag** → adjusts `zoomState.scrollOffset`, clamped to content bounds.
 * - **Ctrl + Mouse wheel** → zoom on desktop.
 * - **Mouse wheel** → pan on desktop.
 * - Hoist [zoomState] to reset from an external button or read the scale.
 *
 * ## Minimal usage
 * ```kotlin
 * BarChart(
 *     entries = listOf(
 *         BarEntry("Surgery",      mapOf("visits" to 4200f, "revenue" to 3100f)),
 *         BarEntry("Consultation", mapOf("visits" to 2900f, "revenue" to 4500f)),
 *     ),
 *     series = listOf(
 *         BarSeries(key = "visits",  label = "Visits",  colors = listOf(Color(0xFF1E3A5F))),
 *         BarSeries(key = "revenue", label = "Revenue", colors = listOf(Color(0xFF7EC8E3))),
 *     ),
 * )
 * ```
 *
 * @param entries     One [BarEntry] per X-group, in left-to-right order.
 * @param series      One [BarSeries] per data series, in left-to-right order within a group.
 * @param config      Visual/layout config. All fields have defaults. See [BarChartConfig].
 * @param zoomState   Zoom + scroll state. Defaults to an internal [rememberBarChartZoomState].
 * @param title       Optional title slot. Pass null to hide.
 * @param description Optional subtitle slot. Pass null to hide.
 * @param modifier    Applied to the outermost [Column].
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BarChart(
    entries: List<BarEntry>,
    series: List<BarSeries>,
    config: BarChartConfig = BarChartConfig(),
    zoomState: BarChartZoomState = rememberBarChartZoomState(),
    title: (@Composable () -> Unit)? = null,
    description: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val windowInfo   = LocalWindowInfo.current

    val isTopLegend = config.legendPosition == LegendPosition.TopLeft ||
            config.legendPosition == LegendPosition.TopRight

    // ── Selected bar (tap-to-highlight) ───────────────────────────────────
    var selectedBar by remember { mutableStateOf<SelectedBar?>(null) }
    LaunchedEffect(selectedBar) {
        if (selectedBar != null) {
            delay(2_000L)
            selectedBar = null
        }
    }

    // ── Max scroll offset bridge ───────────────────────────────────────────
    //   Written from inside DrawScope; read by gesture handlers on the next event.
    var maxScrollOffset by remember { mutableStateOf(0f) }

    // ── Animation ─────────────────────────────────────────────────────────
    //
    //   OnCompose  → animationTriggered = true immediately on first composition.
    //   OnVisible  → animationTriggered = true the first time the chart's
    //                visible fraction reaches config.visibilityThreshold.
    //
    //   KEY DESIGN: for OnVisible we compute the fraction directly inside
    //   onGloballyPositioned (a layout callback) rather than via snapshotFlow.
    //   snapshotFlow only emits on *changes* after the collector starts — if
    //   the chart is already fully on-screen at app launch the flow never sees
    //   a transition and the animation never fires.
    //
    //   onGloballyPositioned fires on every layout pass including the very
    //   first one, so checking the threshold there catches the initial state
    //   regardless of whether the chart is inside a scroll container or not.
    //
    //   animationTriggered is a one-way latch: once true it stays true.
    var animationTriggered by remember { mutableStateOf(false) }

    // windowInfo.containerSize is already in physical pixels.
    val windowHeightPx = windowInfo.containerSize.height.toFloat()

    // Fired for OnCompose: set the latch immediately on first composition.
    LaunchedEffect(Unit) {
        if (config.animationEnabled && config.animationTrigger == AnimationTrigger.OnCompose) {
            animationTriggered = true
        }
    }

    val animationProgress by animateFloatAsState(
        targetValue   = if (animationTriggered || !config.animationEnabled) 1f else 0f,
        animationSpec = config.animationSpec,
        label         = "BarChartHeightAnimation",
    )

    Column(modifier = modifier) {

        // ── Header (title / description / top legend) ──────────────────────
        if (title != null || description != null || isTopLegend) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {

                if (config.legendPosition == LegendPosition.TopLeft) {
                    Legend(series, config.legendPosition, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    title?.invoke()
                    if (description != null) {
                        Spacer(Modifier.height(2.dp))
                        description()
                    }
                }

                if (config.legendPosition == LegendPosition.TopRight) {
                    Spacer(Modifier.width(12.dp))
                    Legend(series, config.legendPosition, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Chart area ─────────────────────────────────────────────────────
        //   onGloballyPositioned fires on every layout pass — including the very
        //   first one — so computing the visible fraction here catches the chart
        //   being on-screen from app launch, not only after a scroll changes it.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(config.chartHeight)
                .onGloballyPositioned { coords ->
                    if (!animationTriggered &&
                        config.animationEnabled &&
                        config.animationTrigger == AnimationTrigger.OnVisible
                    ) {
                        val h = coords.size.height.toFloat()
                        if (h > 0f) {
                            val top      = coords.positionInWindow().y
                            val visTop   = top.coerceAtLeast(0f)
                            val visBot   = (top + h).coerceAtMost(windowHeightPx)
                            val fraction = (visBot - visTop).coerceAtLeast(0f) / h
                            if (fraction >= config.visibilityThreshold) {
                                animationTriggered = true
                            }
                        }
                    }
                },
        ) {

            // ── Y-axis (fixed, never scrolls) ─────────────────────────────
            Canvas(
                modifier = Modifier
                    .width(config.outerHorizontalPadding)
                    .fillMaxHeight(),
            ) {
                val dims = computeDimensions(
                    size               = this.size,
                    entries            = entries,
                    series             = series,
                    config             = config,
                    includeLeftMargin  = true,
                    zoomScale          = zoomState.scale,
                )
                drawYAxisPanel(
                    dims        = dims,
                    textMeasurer = textMeasurer,
                    colors      = config.colors,
                    tickCount   = config.yTickCount,
                )
            }

            // ── Bars + grid (gesture-controlled, scrollable) ───────────────
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    // ── Desktop: Ctrl+Wheel = zoom, Wheel = pan ────────────
                    .pointerInput(zoomState, maxScrollOffset) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Scroll) {
                                    val delta = event.changes.first().scrollDelta
                                    if (config.zoomEnabled && event.keyboardModifiers.isCtrlPressed) {
                                        zoomState.applyZoom(if (delta.y > 0) 0.9f else 1.1f)
                                    } else {
                                        val dx = if (delta.x != 0f) -delta.x * 20f else -delta.y * 20f
                                        zoomState.applyPan(dx, maxScrollOffset)
                                    }
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                    // ── Tap = select bar ──────────────────────────────────
                    .pointerInput(zoomState, entries, series) {
                        detectTapGestures { offset ->
                            val dims = computeDimensionsWithDensity(
                                density = this,
                                size = Size(size.width.toFloat(), size.height.toFloat()),
                                entries = entries,
                                series = series,
                                config = config,
                                includeLeftMargin = false,
                                zoomScale = zoomState.scale,
                            )
                            val plotOffset = Offset(
                                x = offset.x + zoomState.scrollOffset,
                                y = offset.y - dims.marginTop,
                            )
                            selectedBar = findTappedBar(plotOffset, entries, series, dims)
                        }
                    }
                    // ── Pinch = zoom, drag = pan ──────────────────────────
                    .pointerInput(zoomState) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (config.zoomEnabled) zoomState.applyZoom(zoom)
                            zoomState.applyPan(pan.x, maxScrollOffset)
                        }
                    },
            ) {
                val dims = computeDimensions(
                    size              = this.size,
                    entries           = entries,
                    series            = series,
                    config            = config,
                    includeLeftMargin = false,
                    zoomScale         = zoomState.scale,
                )

                // Update and clamp scroll bounds
                val contentWidth = dims.totalContentWidth()
                maxScrollOffset = maxOf(0f, contentWidth - size.width)
                zoomState.clampScroll(maxScrollOffset)

                // Draw scrollable content
                clipRect {
                    translate(left = -zoomState.scrollOffset, top = 0f) {
                        drawBarsPanel(
                            entries           = entries,
                            series            = series,
                            dims              = dims,
                            textMeasurer      = textMeasurer,
                            fullWidth         = size.width + zoomState.scrollOffset,
                            colors            = config.colors,
                            tickCount         = config.yTickCount,
                            animationProgress = animationProgress,
                            selectedBar       = selectedBar,
                        )
                    }
                }

                // Zoom indicator — drawn in viewport space (outside translate)
                if (config.showZoomIndicator) {
                    drawZoomIndicator(zoomState.scale, textMeasurer, config.colors)
                }
            }

            // Symmetry spacer matching the Y-axis width
            Spacer(modifier = Modifier.width(config.outerHorizontalPadding))
        }

        // ── Bottom legend ─────────────────────────────────────────────────
        if (!isTopLegend) {
            Spacer(Modifier.height(8.dp))
            Legend(series, config.legendPosition)
        }
    }
}
