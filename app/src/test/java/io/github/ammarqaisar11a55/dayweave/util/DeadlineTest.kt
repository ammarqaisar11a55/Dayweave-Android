package io.github.ammarqaisar11a55.dayweave.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/** The same cases as the desktop app's src/lib/deadline.test.ts, so both describe deadlines alike. */
class DeadlineTest {
    // A Monday at 14:00 local time.
    private val now = LocalDateTime.of(2026, 11, 2, 14, 0)

    private fun open(dueDate: String?, dueTime: String? = null) = describeDeadline("todo", dueDate, dueTime, now)

    @Test fun `date-only deadlines are described in days and never in hours`() {
        assertEquals("Today", open("2026-11-02").label)
        assertEquals("Tomorrow", open("2026-11-03").label)
        assertEquals("In 3 days", open("2026-11-05").label)
        assertEquals("Due today", open("2026-11-02").detail)
        assertFalse(Regex("hour|minute").containsMatchIn(open("2026-11-02").detail))
    }

    @Test fun `date-only deadlines stay on time until the whole day has passed`() {
        val lateAtNight = LocalDateTime.of(2026, 11, 2, 23, 59)
        assertEquals(DeadlineTone.Today, describeDeadline("todo", "2026-11-02", null, lateAtNight).tone)
    }

    @Test fun `date-only deadlines report how many days overdue`() {
        assertEquals("Overdue by 1 day", open("2026-11-01").label)
        assertEquals("Overdue by 3 days", open("2026-10-30").label)
        assertEquals(DeadlineTone.Overdue, open("2026-10-30").tone)
    }

    @Test fun `timed deadlines count down in minutes, then hours`() {
        assertEquals("Due in 10 minutes", open("2026-11-02", "14:10").detail)
        assertEquals("In 10 min", open("2026-11-02", "14:10").label)
        assertEquals("Due in 2 hours", open("2026-11-02", "16:30").detail)
        assertEquals("Today, 16:30", open("2026-11-02", "16:30").label)
        assertEquals("Now", open("2026-11-02", "14:00").label)
    }

    @Test fun `timed deadlines use the day for anything after today`() {
        assertEquals("Tomorrow, 09:00", open("2026-11-03", "09:00").label)
        assertEquals("In 2 days", open("2026-11-04", "09:00").label)
        assertEquals("Due in 2 days, 09:00", open("2026-11-04", "09:00").detail)
    }

    @Test fun `timed deadlines report overdue time in the closest unit`() {
        assertEquals("Overdue by 1 minute", open("2026-11-02", "13:59").label)
        assertEquals("Overdue by 30 minutes", open("2026-11-02", "13:30").label)
        assertEquals("Overdue by 3 hours", open("2026-11-02", "11:00").label)
        assertEquals("Overdue by 3 days", open("2026-10-30", "11:00").label)
    }

    @Test fun `timed deadlines follow the time format setting`() {
        assertEquals("Today, 4:30 PM", describeDeadline("todo", "2026-11-02", "16:30", now, clock24 = false).label)
    }

    @Test fun `a completed task is never overdue`() {
        val result = describeDeadline("completed", "2020-01-01", null, now)
        assertEquals("Completed", result.label)
        assertEquals(DeadlineTone.Done, result.tone)
    }

    @Test fun `a task without a deadline shows nothing`() {
        assertEquals("", open(null).label)
        assertEquals(DeadlineTone.None, open(null).tone)
    }

    @Test fun `distant deadlines show their date`() {
        assertEquals(DeadlineTone.Later, open("2026-12-25").tone)
        assertTrue(open("2026-12-25").label.contains("25"))
    }
}
