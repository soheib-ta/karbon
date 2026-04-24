package com.soheib_ta.karbon.charts.barchart.data

import androidx.compose.runtime.*

// ─────────────────────────────────────────────────────────────────────────────
// BarChartZoomState
//
//   Owns two pieces of mutable Compose state:
//     • scale        — current zoom multiplier applied to groupWidth.
//     • scrollOffset — how many px the bars canvas is panned to the right.
//
//   Both are Compose State objects, so any Canvas that reads them will
//   automatically recompose (redraw) when either value changes.
//
//   PUBLIC so callers can hoist the state above BarChart to share it with
//   other UI (e.g. a reset button, a zoom label outside the chart).
//
//   Internal mutation functions (applyZoom, applyPan, clampScroll) are
//   INTERNAL so only BarChart drives them.
// ─────────────────────────────────────────────────────────────────────────────

class BarChartZoomState(
    initialScale: Float = 1f,
    val minScale: Float = 0.5f,
    val maxScale: Float = 5f,
) {
    // ── Public readable state ─────────────────────────────────────────────

    /** Current zoom multiplier. 1f = no zoom. Driven by pinch gestures. */
    var scale: Float by mutableStateOf(initialScale.coerceIn(minScale, maxScale))
        internal set

    /** Current horizontal scroll offset in px. 0 = fully left. Driven by drag gestures. */
    var scrollOffset: Float by mutableStateOf(0f)
        internal set

    // ── Public API ────────────────────────────────────────────────────────

    /** Snaps back to scale = 1 and scrollOffset = 0. Safe to call from any coroutine or click handler. */
    fun reset() {
        scale = 1f.coerceIn(minScale, maxScale)
        scrollOffset = 0f
    }

    // ── Internal mutation (called only from BarChart) ─────────────────────

    /** Multiplies the current scale by [delta] and clamps to [minScale]..[maxScale]. */
    internal fun applyZoom(delta: Float) {
        scale = (scale * delta).coerceIn(minScale, maxScale)
    }

    /**
     * Adjusts scrollOffset by [deltaX] pixels.
     *
     * @param deltaX    Raw horizontal pan delta (negative = pan right).
     * @param maxOffset Maximum allowed offset (contentWidth - canvasWidth).
     */
    internal fun applyPan(deltaX: Float, maxOffset: Float) {
        scrollOffset = (scrollOffset - deltaX).coerceIn(0f, maxOf(0f, maxOffset))
    }

    /** Re-clamps scrollOffset after a zoom-out may have shrunk the content. */
    internal fun clampScroll(maxOffset: Float) {
        scrollOffset = scrollOffset.coerceIn(0f, maxOf(0f, maxOffset))
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// rememberBarChartZoomState
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Creates and remembers a [BarChartZoomState].
 *
 * **Basic (unhoisted) usage:**
 * ```kotlin
 * BarChart(entries = ..., series = ...)  // zoomState created internally
 * ```
 *
 * **Hoisted usage:**
 * ```kotlin
 * val zoomState = rememberBarChartZoomState()
 * BarChart(entries = ..., series = ..., zoomState = zoomState)
 * Button(onClick = { zoomState.reset() }) { Text("Reset zoom") }
 * Text("Zoom: ${"%.1f".format(zoomState.scale)}×")
 * ```
 *
 * @param initialScale Starting zoom level (1f = no zoom).
 * @param minScale     Minimum allowed scale. Default 0.5f.
 * @param maxScale     Maximum allowed scale. Default 5f.
 */
@Composable
fun rememberBarChartZoomState(
    initialScale: Float = 1f,
    minScale: Float = 0.5f,
    maxScale: Float = 5f,
): BarChartZoomState = remember(minScale, maxScale) {
    BarChartZoomState(
        initialScale = initialScale,
        minScale = minScale,
        maxScale = maxScale,
    )
}
