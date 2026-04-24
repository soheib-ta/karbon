package com.soheib_ta.karbon.charts.barchart.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soheib_ta.karbon.charts.barchart.LegendPosition
import com.soheib_ta.karbon.charts.barchart.data.BarSeries

/**
 * Horizontal row of coloured swatches + labels.
 *
 * [position] controls alignment inside a [fillMaxWidth] container:
 *   - TopLeft / BottomLeft  → start-aligned
 *   - TopRight / BottomRight → end-aligned
 */
@Composable
internal fun Legend(
    series: List<BarSeries>,
    position: LegendPosition,
    modifier: Modifier = Modifier,
) {
    val isRight = position == LegendPosition.TopRight || position == LegendPosition.BottomRight

    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentWidth(if (isRight) Alignment.End else Alignment.Start),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        series.forEach { s -> LegendItem(s) }
    }
}

@Composable
private fun LegendItem(series: BarSeries) {
    val swatchShape = RoundedCornerShape(2.dp)

    Row(verticalAlignment = Alignment.CenterVertically) {
        // Colour swatch
        val swatchModifier = Modifier
            .size(width = 12.dp, height = 12.dp)
            .clip(swatchShape)

        if (series.isGradient) {
            Box(modifier = swatchModifier.background(Brush.verticalGradient(series.colors)))
        } else {
            Box(modifier = swatchModifier.background(series.colors[0]))
        }

        Spacer(Modifier.width(6.dp))

        Text(
            text     = series.label,
            style    = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
