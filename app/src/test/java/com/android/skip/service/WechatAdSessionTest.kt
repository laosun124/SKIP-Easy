package com.android.skip.service

import org.junit.Assert.*
import org.junit.Test

class WechatAdSessionTest {
    private var time = 100_000L
    private val session = WechatAdSession { time }

    @Test fun startsWhenAdAppearsAfterWechatHasBeenOpenForMinutes() {
        assertNull(session.observe(false))
        time += 300_000
        assertTrue(session.canClick(session.observe(true)!!))
    }
    @Test fun repeatedMatchesCannotExtendTheSixSecondDeadline() {
        val id = session.observe(true)!!
        time += 5999
        assertEquals(id, session.observe(true))
        assertTrue(session.canClick(id))
        time++
        assertEquals(id, session.observe(true))
        assertFalse(session.canClick(id))
    }
    @Test fun transientMissingNodesPauseClicksWithoutRestartingTime() {
        val id = session.observe(true)!!
        time += 5000;session.observe(false)
        assertFalse(session.canClick(id))
        time += 300
        assertEquals(id, session.observe(true))
        time += 700
        assertFalse(session.canClick(id))
    }
    @Test fun confirmedAdDisappearanceAllowsTheNextAdItsOwnWindow() {
        val first = session.observe(true)!!
        session.observe(false)
        time += 1000;session.observe(false)
        val second = session.observe(true)!!
        assertNotEquals(first, second)
        assertFalse(session.canClick(first))
        assertTrue(session.canClick(second))
    }
    @Test fun leavingMiniProgramInvalidatesPendingClicks() {
        val id = session.observe(true)!!
        session.leaveMiniProgram()
        assertFalse(session.canClick(id))
        assertNotEquals(id, session.observe(true))
    }
    @Test fun retryBudgetStopsAtDeadlineAndCanRestartOnlyForNextAd() {
        val budget = SkipAttemptBudget { time }
        var id = session.observe(true)!!
        assertEquals(1, budget.take("skip", session.canClick(id)))
        time += 600;assertEquals(2, budget.take("skip", session.canClick(id)))
        time += 5400;assertNull(budget.take("skip", session.canClick(id)))
        session.observe(false);time += 1000;session.observe(false)
        id = session.observe(true)!!;budget.clear()
        assertEquals(1, budget.take("skip", session.canClick(id)))
    }
    @Test fun disabledOptimizationRejectsAnOtherwiseValidPendingClick() {
        val id = session.observe(true)!!
        assertTrue(session.canClick(id, optimizationEnabled = true))
        assertFalse(session.canClick(id, optimizationEnabled = false))
    }
    @Test fun togglingOptimizationCannotExtendAnExistingAdDeadline() {
        val id = session.observe(true)!!
        time += 5000
        assertFalse(session.canClick(id, optimizationEnabled = false))
        time += 1000
        assertFalse(session.canClick(id, optimizationEnabled = true))
    }

}
