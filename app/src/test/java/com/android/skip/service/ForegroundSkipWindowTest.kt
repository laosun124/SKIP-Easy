package com.android.skip.service

import org.junit.Assert.*
import org.junit.Test

class ForegroundSkipWindowTest {
    private var time = 100_000L
    private val window = ForegroundSkipWindow { time }

    @Test fun coldLaunchStopsExactlyAtSixSeconds() {
        val ticket = window.showApp("app.a", 1)
        assertTrue(window.canClick(ticket))
        time += 5_999
        assertTrue(window.canClick(ticket))
        time++
        assertFalse(window.canClick(ticket))
    }

    @Test fun contentEventsDoNotExtendDeadline() {
        window.showApp("app.a", 1)
        time += 5_000
        val ticket = window.showApp("app.a", 1)
        time += 1_000
        assertFalse(window.canClick(ticket))
    }

    @Test fun inAppWindowChangeInvalidatesOldWorkWithoutRestartingTime() {
        val old = window.showApp("app.a", 1)
        time += 5_000
        val next = window.showApp("app.a", 2)
        assertEquals(old.visitId, next.visitId)
        assertFalse(window.canClick(old))
        assertTrue(window.canClick(next))
        time += 1_000
        assertFalse(window.canClick(next))
    }

    @Test fun returningFromHomeGivesFullNewWindow() {
        val old = window.showApp("app.a", 1)
        time += 10_000
        window.leaveApp()
        val next = window.showApp("app.a", 1)
        assertNotEquals(old.visitId, next.visitId)
        assertFalse(window.canClick(old))
        time += 5_999
        assertTrue(window.canClick(next))
    }

    @Test fun switchingAppsAndBackStartsNewVisitEvenWithSameWindowId() {
        val old = window.showApp("app.a", 1)
        time += 7_000
        val other = window.showApp("app.b", 2)
        assertFalse(window.canClick(old))
        assertTrue(window.canClick(other))
        val next = window.showApp("app.a", 1)
        assertTrue(window.canClick(next))
        assertFalse(window.canClick(other))
        assertFalse(window.canClick(old))
    }

    @Test fun rapidLeaveAndReturnStillInvalidatesOldResult() {
        val old = window.showApp("app.a", 1)
        window.leaveApp()
        val next = window.showApp("app.a", 1)
        assertFalse(window.canClick(old))
        assertTrue(window.canClick(next))
    }

    @Test fun temporaryPanelBlocksClicksAndDoesNotPauseClock() {
        val old = window.showApp("app.a", 1)
        time += 2_000
        window.suspendClicks()
        assertFalse(window.canClick(old))
        time += 3_000
        val next = window.showApp("app.a", 1)
        assertEquals(old.visitId, next.visitId)
        assertFalse(window.canClick(old))
        assertTrue(window.canClick(next))
        time += 1_000
        assertFalse(window.canClick(next))
    }

    @Test fun closingKeyboardOrNotificationShadeAfterExpiryDoesNotRearm() {
        window.showApp("app.a", 1)
        time += 7_000
        repeat(3) {
            window.suspendClicks()
            assertFalse(window.canClick(window.showApp("app.a", 1)))
        }
    }

    @Test fun anotherAppAfterPanelStillGetsNewVisit() {
        window.showApp("app.a", 1)
        time += 10_000
        window.suspendClicks()
        assertTrue(window.canClick(window.showApp("app.b", 2)))
    }

    @Test fun screenOffInvalidatesPendingClicksAndUnlockStartsNewVisit() {
        val old = window.showApp("app.a", 1)
        window.leaveApp()
        assertFalse(window.canClick(old))
        time += 60_000
        assertTrue(window.canClick(window.showApp("app.a", 1)))
        assertFalse(window.canClick(old))
    }

    @Test fun delayedResultCannotClickAfterDeadlineWithoutAnotherEvent() {
        val pending = window.showApp("app.a", 1)
        time += 6_100
        assertFalse(window.canClick(pending))
    }
}
