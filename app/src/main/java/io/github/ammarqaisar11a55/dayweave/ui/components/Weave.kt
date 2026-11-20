package io.github.ammarqaisar11a55.dayweave.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.ammarqaisar11a55.dayweave.ui.theme.Accent
import io.github.ammarqaisar11a55.dayweave.ui.theme.Dw

/** The Overview's progress visual: habits are the warp, tasks the weft. See docs/DESIGN.md. */
data class Thread(val title: String, val done: Boolean, val ghost: Boolean = false)

/** Fits items onto the loom. Spare positions become faint threads, so the cloth keeps its shape. */
fun layOut(items: List<Thread>, minimum: Int, maximum: Int): List<Thread> {
    if (items.size > maximum) {
        val filled = Math.round(items.count { it.done }.toDouble() / items.size * maximum).toInt()
        return List(maximum) { index -> Thread("${items.size} items", index < filled) }
    }
    val size = maxOf(minimum, items.size)
    val spare = size - items.size
    val before = spare / 2
    val ghost = Thread("", done = false, ghost = true)
    return List(before) { ghost } + items + List(spare - before) { ghost }
}

private const val PITCH = 18f
private const val WIDTH = 11f
private const val GAP = 1.5f

@Composable
fun Weave(tasks: List<Thread>, habits: List<Thread>, background: Color, label: String, modifier: Modifier = Modifier) {
    val colors = Dw.colors
    val warp = layOut(habits, 8, 14)
    val weft = layOut(tasks, 5, 8)
    fun tone(thread: Thread, done: Color) = when {
        thread.ghost -> colors.threadGhost
        thread.done -> done
        else -> colors.threadIdle
    }
    // Colours animate, so completing something fills its thread rather than switching it.
    val warpColors = warp.map { animateColorAsState(tone(it, colors.threadWarp), tween(300), label = "warp").value }
    val weftColors = weft.map { animateColorAsState(tone(it, Accent), tween(300), label = "weft").value }

    Canvas(
        modifier
            .aspectRatio(warp.size.toFloat() / weft.size, matchHeightConstraintsFirst = true)
            .semantics { contentDescription = label },
    ) {
        val unit = size.width / (warp.size * PITCH)
        val pitch = PITCH * unit
        val width = WIDTH * unit
        val gap = GAP * unit
        fun center(index: Int) = (index + 0.5f) * pitch
        warp.indices.forEach { column ->
            drawLine(warpColors[column], Offset(center(column), width / 2), Offset(center(column), size.height - width / 2), width, StrokeCap.Round)
        }
        weft.indices.forEach { row ->
            drawLine(weftColors[row], Offset(width / 2, center(row)), Offset(size.width - width / 2, center(row)), width, StrokeCap.Round)
        }
        // Each crossing lifts one thread over the other, with a sliver of background either side.
        weft.indices.forEach { row ->
            warp.indices.forEach { column ->
                val x = center(column) - width / 2
                val y = center(row) - width / 2
                if ((row + column) % 2 == 1) {
                    drawRect(background, Offset(x - gap, y), Size(width + 2 * gap, width))
                    drawRect(warpColors[column], Offset(x, y - unit), Size(width, width + 2 * unit))
                } else {
                    drawRect(background, Offset(x, y - gap), Size(width, width + 2 * gap))
                    drawRect(weftColors[row], Offset(x - unit, y), Size(width + 2 * unit, width))
                }
            }
        }
    }
}
