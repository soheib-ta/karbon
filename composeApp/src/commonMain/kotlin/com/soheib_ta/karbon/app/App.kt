package com.soheib_ta.karbon.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soheib_ta.karbon.samples.AppointmentOutcomesDonutSample
import com.soheib_ta.karbon.samples.BarChartSample
import com.soheib_ta.karbon.samples.ClinicalActivityLineChartSample

@Composable
fun App() {
    MaterialTheme {
        Scaffold { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 960.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Karbon Charts",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            text = "Interactive examples for every chart type in the library.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item {
                    ChartExample(
                        chartType = "Bar chart",
                        description = "Compare multiple series across categories.",
                    ) {
                        BarChartSample()
                    }
                }

                item {
                    ChartExample(
                        chartType = "Line chart",
                        description = "Track several series and their trends over time.",
                    ) {
                        ClinicalActivityLineChartSample()
                    }
                }

                item {
                    ChartExample(
                        chartType = "Pie / donut chart",
                        description = "Show how individual values contribute to a whole.",
                    ) {
                        AppointmentOutcomesDonutSample()
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartExample(
    chartType: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .widthIn(max = 960.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = chartType,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}
