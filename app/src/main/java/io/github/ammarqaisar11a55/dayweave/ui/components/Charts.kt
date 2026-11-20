package io.github.ammarqaisar11a55.dayweave.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import io.github.ammarqaisar11a55.dayweave.ui.theme.Tabular
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.ammarqaisar11a55.dayweave.core.HeatmapDay
import io.github.ammarqaisar11a55.dayweave.ui.theme.Dw

/** A year of habit activity, scrolled to the most recent weeks. */
@Composable
fun Heatmap(days: List<HeatmapDay>, modifier: Modifier = Modifier) {
    val colors = Dw.colors
    val weeks = days.chunked(7)
    val scroll = rememberScrollState()
    LaunchedEffect(weeks.size) { scroll.scrollTo(scroll.maxValue) }
    val total = days.sumOf { it.done }
    Box(modifier.horizontalScroll(scroll).semantics { contentDescription = "Habit activity: $total completions" }) {
        Canvas(Modifier.size((weeks.size * 14).dp, (7 * 14).dp)) {
            val cell = 11.dp.toPx()
            val step = 14.dp.toPx()
            weeks.forEachIndexed { column, week ->
                week.forEachIndexed { row, day ->
                    val topLeft = Offset(column * step, row * step)
                    drawRoundRect(colors.heat[day.level.coerceIn(0, 4)], topLeft, Size(cell, cell), CornerRadius(3.dp.toPx()))
                    if (day.missed) {
                        drawRoundRect(colors.danger.copy(alpha = 0.55f), topLeft, Size(cell, cell), CornerRadius(3.dp.toPx()), style = Stroke(1.dp.toPx()))
                    }
                }
            }
        }
    }
}

/** Plain bars from a zero baseline, with rounded tops. Values are described for screen readers. */
@Composable
fun BarChart(
    values: List<Float>,
    color: Color,
    description: String,
    modifier: Modifier = Modifier,
    start: String? = null,
    peak: String? = null,
) {
    val line = Dw.colors.line
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = description }) {
        if (peak != null) Text(peak, style = MaterialTheme.typography.labelSmall.merge(Tabular), color = Dw.colors.ink3)
        Canvas(Modifier.fillMaxWidth().height(128.dp).padding(top = 4.dp)) {
            val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
            val slot = size.width / values.size.coerceAtLeast(1)
            val bar = (slot * 0.6f).coerceAtMost(22.dp.toPx()).coerceAtLeast(1.5f)
            listOf(0f, 0.5f, 1f).forEach { fraction ->
                val y = size.height * (1 - fraction)
                drawLine(line, Offset(0f, y), Offset(size.width, y), 1f)
            }
            // Earlier weeks are quieter, so the current one is the bar the eye lands on.
            values.forEachIndexed { index, value ->
                if (value <= 0f) return@forEachIndexed
                val height = size.height * value / max
                val x = index * slot + (slot - bar) / 2
                val tone = if (index == values.lastIndex) color else color.copy(alpha = 0.42f)
                drawRoundRect(tone, Offset(x, size.height - height), Size(bar, height), CornerRadius(minOf(4.dp.toPx(), bar / 2)))
            }
        }
        if (start != null) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(start, style = MaterialTheme.typography.labelSmall, color = Dw.colors.ink3, modifier = Modifier.weight(1f))
                Text("This week", style = MaterialTheme.typography.labelSmall, color = Dw.colors.ink2)
            }
        }
    }
}
