package com.soheib_ta.karbon.charts.linechart.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Hoistable zoom and horizontal pan state for [com.soheib_ta.karbon.charts.linechart.LineChart]. */
class LineChartZoomState(
    initialScale: Float = 1f,
    val minScale: Float = 0.5f,
    val maxScale: Float = 5f,
) {
    init {
        require(minScale > 0f) { "minScale must be positive." }
        require(maxScale >= minScale) { "maxScale must be greater than or equal to minScale." }
    }

    var scale: Float by mutableStateOf(initialScale.coerceIn(minScale, maxScale))
        internal set

    var scrollOffset: Float by mutableStateOf(0f)
        internal set

    fun reset() {
        scale = 1f.coerceIn(minScale, maxScale)
        scrollOffset = 0f
    }

    internal fun applyZoom(delta: Float) {
        if (!delta.isFinite() || delta <= 0f) return
        scale = (scale * delta).coerceIn(minScale, maxScale)
    }

    internal fun applyPan(deltaX: Float, maxOffset: Float) {
        if (!deltaX.isFinite()) return
        scrollOffset = (scrollOffset - deltaX).coerceIn(0f, maxOf(0f, maxOffset))
    }

    internal fun clampScroll(maxOffset: Float) {
        scrollOffset = scrollOffset.coerceIn(0f, maxOf(0f, maxOffset))
    }
}

@Composable
fun rememberLineChartZoomState(
    initialScale: Float = 1f,
    minScale: Float = 0.5f,
    maxScale: Float = 5f,
): LineChartZoomState = remember(minScale, maxScale) {
    LineChartZoomState(initialScale, minScale, maxScale)
}
