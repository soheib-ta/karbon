package com.soheib_ta.karbon.charts.piechart.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soheib_ta.karbon.charts.common.ChartLegendPosition
import com.soheib_ta.karbon.charts.piechart.PieChartConfig
import com.soheib_ta.karbon.charts.piechart.data.PieEntry

@Composable
internal fun PieLegend(
    entries: List<PieEntry>,
    percentages: Map<Int, Float>,
    position: ChartLegendPosition,
    config: PieChartConfig,
    modifier: Modifier = Modifier,
) {
    val rightAligned = position == ChartLegendPosition.TopRight ||
        position == ChartLegendPosition.BottomRight

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentWidth(if (rightAligned) Alignment.End else Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        entries.withIndex().toList().chunked(config.legendColumns).forEach { rowEntries ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rowEntries.forEach { indexedEntry ->
                    val index = indexedEntry.index
                    val entry = indexedEntry.value
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(entry.color),
                        )
                        Spacer(Modifier.width(6.dp))
                        val suffix = if (config.showPercentInLegend) {
                            " (${config.percentageFormatter(percentages[index] ?: 0f)})"
                        } else {
                            " (${config.valueFormatter(entry.value.takeIf { it.isFinite() && it > 0f } ?: 0f)})"
                        }
                        Text(
                            text = entry.label + suffix,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
