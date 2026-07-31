package com.soheib_ta.karbon.samples

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.soheib_ta.karbon.charts.barchart.BarChart
import com.soheib_ta.karbon.charts.barchart.BarChartConfig
import com.soheib_ta.karbon.charts.barchart.DistributionMode
import com.soheib_ta.karbon.charts.barchart.data.BarEntry
import com.soheib_ta.karbon.charts.barchart.data.BarSeries
import com.soheib_ta.karbon.charts.common.ChartLegendPosition
import com.soheib_ta.karbon.charts.linechart.LineChart
import com.soheib_ta.karbon.charts.linechart.LineChartConfig
import com.soheib_ta.karbon.charts.linechart.data.LineAreaFill
import com.soheib_ta.karbon.charts.linechart.data.LineEntry
import com.soheib_ta.karbon.charts.linechart.data.LineSeries
import com.soheib_ta.karbon.charts.piechart.PieChart
import com.soheib_ta.karbon.charts.piechart.PieChartConfig
import com.soheib_ta.karbon.charts.piechart.data.PieEntry

private val Primary = Color(0xFF01668B)
private val Secondary = Color(0xFF396666)
private val Tertiary = Color(0xFF006C57)
private val Error = Color(0xFFAC3434)

@Composable
fun BarChartSample(modifier: Modifier = Modifier) {
    BarChart(
        entries = listOf(
            BarEntry("Week 1", mapOf("billed" to 820_000f, "paid" to 650_000f)),
            BarEntry("Week 2", mapOf("billed" to 710_000f, "paid" to 600_000f)),
            BarEntry("Week 3", mapOf("billed" to 930_000f, "paid" to 850_000f)),
            BarEntry("Week 4", mapOf("billed" to 860_000f, "paid" to 750_000f)),
        ),
        series = listOf(
            BarSeries("billed", "Billed", listOf(Primary.copy(alpha = 0.55f))),
            BarSeries("paid", "Paid", listOf(Tertiary)),
        ),
        config = BarChartConfig(distributionMode = DistributionMode.Even),
        title = { Text("Billing & Collections", style = MaterialTheme.typography.titleMedium) },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun ClinicalActivityLineChartSample(modifier: Modifier = Modifier) {
    val entries = listOf(
        LineEntry("Jul 1", mapOf("appointments" to 32f, "consultations" to 22f, "operations" to 8f)),
        LineEntry("Jul 8", mapOf("appointments" to 44f, "consultations" to 28f, "operations" to 12f)),
        LineEntry("Jul 15", mapOf("appointments" to 39f, "consultations" to 31f, "operations" to 10f)),
        LineEntry("Jul 22", mapOf("appointments" to 55f, "consultations" to 36f, "operations" to 15f)),
        LineEntry("Jul 29", mapOf("appointments" to 61f, "consultations" to 42f, "operations" to 18f)),
    )

    LineChart(
        entries = entries,
        series = listOf(
            LineSeries(
                key = "appointments",
                label = "Appointments",
                color = Primary,
                areaFill = LineAreaFill.VerticalGradient(
                    listOf(Primary.copy(alpha = 0.22f), Primary.copy(alpha = 0f)),
                ),
            ),
            LineSeries("consultations", "Consultations", Secondary),
            LineSeries("operations", "Operations", Tertiary),
        ),
        config = LineChartConfig(legendPosition = ChartLegendPosition.TopRight),
        title = { Text("Clinical Activity Trend", style = MaterialTheme.typography.titleMedium) },
        description = {
            Text(
                "Appointments, consultations and patient operations",
                style = MaterialTheme.typography.bodySmall,
            )
        },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun AppointmentOutcomesDonutSample(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        PieChart(
            entries = listOf(
                PieEntry("Completed", 412f, Primary),
                PieEntry("Confirmed", 156f, Secondary),
                PieEntry("Pending", 82f, Tertiary),
                PieEntry("Cancelled", 34f, Error),
            ),
            config = PieChartConfig(
                innerRadiusRatio = 0.64f,
                legendPosition = ChartLegendPosition.BottomLeft,
                legendColumns = 2,
            ),
            title = { Text("Appointment Outcomes", style = MaterialTheme.typography.titleMedium) },
            centerValue = "684",
            centerLabel = "TOTAL",
        )
    }
}
