package io.github.ammarqaisar11a55.dayweave.util

/**
 * Compares release versions such as "v1.2.3" and "1.10.0". A pre-release ("1.2.0-beta.1") sorts before
 * its release. Returns a negative number, zero or a positive number, like [Comparable.compareTo].
 */
fun compareVersions(a: String, b: String): Int {
    fun parts(version: String): Pair<List<Int>, String?> {
        val clean = version.trim().removePrefix("v").removePrefix("V")
        val core = clean.substringBefore('-').substringBefore('+')
        val numbers = core.split('.').map { it.toIntOrNull() ?: 0 }
        val pre = clean.substringAfter('-', "").substringBefore('+').ifEmpty { null }
        return numbers to pre
    }
    val (left, leftPre) = parts(a)
    val (right, rightPre) = parts(b)
    for (index in 0 until maxOf(left.size, right.size)) {
        val difference = left.getOrElse(index) { 0 } - right.getOrElse(index) { 0 }
        if (difference != 0) return difference
    }
    return when {
        leftPre == rightPre -> 0
        leftPre == null -> 1
        rightPre == null -> -1
        else -> leftPre.compareTo(rightPre)
    }
}

fun isNewer(candidate: String, installed: String) = compareVersions(candidate, installed) > 0
