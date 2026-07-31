package com.soheib_ta.karbon.charts.linechart.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import com.soheib_ta.karbon.charts.linechart.LineChartConfig
import com.soheib_ta.karbon.charts.linechart.data.LineEntry
import com.soheib_ta.karbon.charts.linechart.data.LineSeries
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LineChartGeometryTest {
    private val series = listOf(LineSeries("value", "Value", Color.Blue))

    @Test
    fun automaticRangeIncludesPositiveValues() {
        val range = computeLineValueRange(
            entries = listOf(
                LineEntry("A", mapOf("value" to 12f)),
                LineEntry("B", mapOf("value" to 38f)),
            ),
            series = series,
            requestedMin = null,
            requestedMax = null,
            includeZero = true,
            tickCount = 5,
        )

        assertTrue(range.min <= 0f)
        assertTrue(range.max >= 38f)
    }

    @Test
    fun automaticRangeSupportsNegativeValues() {
        val range = computeLineValueRange(
            entries = listOf(
                LineEntry("A", mapOf("value" to -20f)),
                LineEntry("B", mapOf("value" to 15f)),
            ),
            series = series,
            requestedMin = null,
            requestedMax = null,
            includeZero = true,
            tickCount = 5,
        )

        assertTrue(range.min <= -20f)
        assertTrue(range.max >= 15f)
    }

    @Test
    fun equalValuesProduceNonZeroRange() {
        val range = computeLineValueRange(
            entries = listOf(LineEntry("A", mapOf("value" to 8f))),
            series = series,
            requestedMin = null,
            requestedMax = null,
            includeZero = false,
            tickCount = 5,
        )

        assertTrue(range.max > range.min)
        assertTrue(8f in range.min..range.max)
    }

    @Test
    fun emptyDataProducesSafeDefaultRange() {
        val range = computeLineValueRange(
            entries = emptyList(),
            series = series,
            requestedMin = null,
            requestedMax = null,
            includeZero = true,
            tickCount = 5,
        )

        assertTrue(range.max > range.min)
    }

    @Test
    fun ticksCoverTheRange() {
        val range = LineValueRange(-10f, 30f)
        val ticks = generateLineTicks(range, 5)

        assertEquals(5, ticks.size)
        assertTrue(abs(ticks.first() - -10f) < 0.001f)
        assertTrue(abs(ticks.last() - 30f) < 0.001f)
    }

    @Test
    fun coordinateMappingAndHitTestingAreConsistent() {
        val entries = listOf(
            LineEntry("A", mapOf("value" to 0f)),
            LineEntry("B", mapOf("value" to 50f)),
            LineEntry("C", mapOf("value" to 100f)),
        )
        val dimensions = computeLineDimensions(
            density = Density(1f),
            size = Size(300f, 220f),
            entries = entries,
            config = LineChartConfig(yMin = 0f, yMax = 100f),
            valueRange = LineValueRange(0f, 100f),
            zoomScale = 1f,
        )

        assertTrue(dimensions.yForValue(100f) < dimensions.yForValue(0f))
        val selection = findNearestLinePoint(
            offset = Offset(dimensions.xForIndex(1), dimensions.yForValue(50f)),
            entries = entries,
            series = series,
            dimensions = dimensions,
            hitRadius = 10f,
        )

        assertNotNull(selection)
        assertEquals(1, selection.entryIndex)
        assertEquals(50f, selection.value)
    }
}
