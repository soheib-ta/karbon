package com.soheib_ta.karbon.charts.piechart.data

import androidx.compose.ui.graphics.Color

/** One slice in a pie or donut chart. */
data class PieEntry(
    val label: String,
    val value: Float,
    val color: Color,
)
