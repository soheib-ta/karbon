package com.soheib_ta.karbon.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.soheib_ta.karbon.charts.barchart.BarChart
import com.soheib_ta.karbon.charts.barchart.data.BarEntry
import com.soheib_ta.karbon.charts.barchart.data.BarSeries

@Composable
fun App() {
    MaterialTheme {
        Scaffold { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                Text(
                    text = "Karbon Charts Demo",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(16.dp)
                )

                val entries = listOf(
                    BarEntry(label = "Jan", values = mapOf("visits" to 4200f, "revenue" to 3100f)),
                    BarEntry(label = "Feb", values = mapOf("visits" to 3800f, "revenue" to 2900f)),
                    BarEntry(label = "Mar", values = mapOf("visits" to 5000f, "revenue" to 4100f)),
                    BarEntry(label = "Apr", values = mapOf("visits" to 4500f, "revenue" to 3500f)),
                )

                val series = listOf(
                    BarSeries(
                        key = "visits",
                        label = "Visits",
                        colors = listOf(Color(0xFF4285F4))
                    ),
                    BarSeries(
                        key = "revenue",
                        label = "Revenue",
                        colors = listOf(Color(0xFF34A853))
                    )
                )

                BarChart(
                    entries = entries,
                    series = series,
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    title = { Text("Monthly Stats") }
                )
            }
        }
    }
}
