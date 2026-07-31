package com.soheib_ta.karbon.charts.barchart.internal

import androidx.compose.ui.graphics.Color
import com.soheib_ta.karbon.charts.barchart.data.BarEntry
import com.soheib_ta.karbon.charts.barchart.data.BarSeries
import kotlin.test.Test
import kotlin.test.assertEquals

class BarChartGeometryTest {
    @Test
    fun maxValueIgnoresNegativeAndInvalidValues() {
        val entries = listOf(
            BarEntry("A", mapOf("value" to -10f)),
            BarEntry("B", mapOf("value" to Float.NaN)),
            BarEntry("C", mapOf("value" to 25f)),
        )
        val series = listOf(BarSeries("value", "Value", listOf(Color.Blue)))

        assertEquals(25f, computeMaxValue(entries, series))
    }

    @Test
    fun emptySeriesReturnsZero() {
        assertEquals(0f, computeMaxValue(listOf(BarEntry("A", emptyMap())), emptyList()))
    }
}
