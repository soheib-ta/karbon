package com.soheib_ta.karbon.charts.piechart.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.soheib_ta.karbon.charts.piechart.data.PieEntry
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PieChartGeometryTest {
    @Test
    fun percentagesSumToOne() {
        val entries = listOf(
            PieEntry("A", 50f, Color.Red),
            PieEntry("B", 30f, Color.Blue),
            PieEntry("C", 20f, Color.Green),
        )
        val slices = calculatePieSlices(entries, -90f, clockwise = true, gapAngle = 2f)

        assertEquals(3, slices.size)
        assertTrue(abs(slices.sumOf { it.percentage.toDouble() }.toFloat() - 1f) < 0.001f)
    }

    @Test
    fun invalidAndNegativeValuesAreIgnored() {
        val entries = listOf(
            PieEntry("Valid", 10f, Color.Red),
            PieEntry("Negative", -5f, Color.Blue),
            PieEntry("NaN", Float.NaN, Color.Green),
        )
        val slices = calculatePieSlices(entries, 0f, clockwise = true, gapAngle = 2f)

        assertEquals(1, slices.size)
        assertEquals(0, slices.first().index)
        assertTrue(abs(slices.first().percentage - 1f) < 0.001f)
    }

    @Test
    fun allZeroValuesProduceNoSlices() {
        val entries = listOf(
            PieEntry("A", 0f, Color.Red),
            PieEntry("B", 0f, Color.Blue),
        )

        assertTrue(calculatePieSlices(entries, 0f, true, 2f).isEmpty())
    }

    @Test
    fun singleSliceUsesFullCircleWithoutGap() {
        val slices = calculatePieSlices(
            listOf(PieEntry("Only", 10f, Color.Red)),
            startAngle = -90f,
            clockwise = true,
            gapAngle = 12f,
        )

        assertEquals(1, slices.size)
        assertTrue(abs(slices.first().sweepAngle - 360f) < 0.001f)
    }

    @Test
    fun hitTestingRespectsDonutHole() {
        val entries = listOf(
            PieEntry("A", 50f, Color.Red),
            PieEntry("B", 50f, Color.Blue),
        )
        val slices = calculatePieSlices(entries, 0f, clockwise = true, gapAngle = 0f)
        val center = Offset(100f, 100f)

        assertNull(
            findPieSliceAt(
                offset = center,
                center = center,
                outerRadius = 80f,
                innerRadius = 40f,
                slices = slices,
                entries = entries,
            ),
        )

        val selection = findPieSliceAt(
            offset = Offset(160f, 100f),
            center = center,
            outerRadius = 80f,
            innerRadius = 40f,
            slices = slices,
            entries = entries,
        )
        assertNotNull(selection)
        assertEquals("A", selection.label)
    }
}
