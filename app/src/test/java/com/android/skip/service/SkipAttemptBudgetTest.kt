package com.android.skip.service

import org.junit.Assert.*
import org.junit.Test

class SkipAttemptBudgetTest {
    private var time = 0L
    private val budget = SkipAttemptBudget { time }

    @Test fun firstVisibleTargetIsClickedImmediately() {
        assertEquals(1, budget.take("skip", true))
    }

    @Test fun sameButtonWaitsSixTenthsBeforeRetry() {
        assertEquals(1, budget.take("skip", true))
        time = 599
        assertNull(budget.take("skip", true))
        time = 600
        assertEquals(2, budget.take("skip", true))
        assertNull(budget.take("skip", true))
    }

    @Test fun retriesRemainAvailableForButtonThatOnlyRespondsAfterFiveSeconds() {
        val foreground = ForegroundSkipWindow { time }
        val ticket = foreground.showApp("app", 1)
        // Each sample represents a fresh match at the same position, every 300 ms.
        val clickTimes = mutableListOf<Long>()
        for (sample in 0..20) {
            time = sample * 300L
            if (budget.take("skip", foreground.canClick(ticket)) != null) clickTimes.add(time)
        }
        assertEquals((0..9).map { it * 600L }, clickTimes)
        assertEquals(5400L, clickTimes.last())
        assertFalse(foreground.canClick(ticket)) // exactly 6 seconds: no click
    }

    @Test fun absentButtonBeforeFirstClickDoesNotEndStartupDetection() {
        assertNull(budget.take(null, true))
        time = 3000
        assertEquals(1, budget.take("skip", true))
    }

    @Test fun disappearingButtonEndsRetriesEvenIfAnotherLaterOccupiesThatPosition() {
        budget.take("skip", true)
        time = 300
        assertNull(budget.take(null, true))
        time = 600
        assertNull(budget.take("skip", true))
        assertTrue(budget.isFinished)
    }

    @Test fun movedButtonCannotInheritTheRetry() {
        budget.take("app/window/skip/position1", true)
        time = 600
        assertNull(budget.take("app/window/skip/position2", true))
        assertTrue(budget.isFinished)
    }

    @Test fun replacementRuleOrWindowAtSamePositionCannotInheritTheRetry() {
        for (replacement in listOf("app/window/otherRule/position", "app/otherWindow/skip/position")) {
            budget.clear()
            budget.take("app/window/skip/position", true)
            time += 600
            assertNull(budget.take(replacement, true))
        }
    }

    @Test fun pageChangeBeforeFirstMatchStillAllowsFirstClick() {
        budget.stop()
        assertEquals(1, budget.take("skip", true))
    }

    @Test fun pageChangeAfterClickStopsEvenAnIdenticalLookingTarget() {
        budget.take("skip", true)
        budget.stop()
        time = 600
        assertNull(budget.take("skip", true))
    }

    @Test fun temporaryUnavailableWindowDoesNotCountAsAConfirmedDisappearance() {
        budget.take("skip", true)
        time = 300
        assertNull(budget.take(null, false))
        time = 600
        assertEquals(2, budget.take("skip", true))
    }

    @Test fun newForegroundVisitAllowsANewButtonAfterOldRetriesStopped() {
        budget.take("old", true)
        budget.stop()
        budget.clear()
        assertEquals(1, budget.take("new", true))
    }

    @Test fun switchingAppsRejectsOldPendingRetry() {
        val foreground = ForegroundSkipWindow { time }
        val old = foreground.showApp("app.a", 1)
        budget.take("skip", foreground.canClick(old))
        time = 600
        foreground.showApp("app.b", 2)
        assertNull(budget.take("skip", foreground.canClick(old)))
    }
}
