package com.android.skip.service

/** Main-thread state for one foreground visit. Callers supply a fresh match and deadline check. */
internal class SkipAttemptBudget(private val now: () -> Long) {
    private var originalTarget: String? = null
    private var lastAttempt: Long? = null
    private var count = 0
    var isFinished = false
        private set

    fun clear() {
        originalTarget = null
        lastAttempt = null
        count = 0
        isFinished = false
    }

    fun stop() {
        // App startup may change Activity before there has even been a skip target.
        if (originalTarget != null) isFinished = true
    }

    /** A disappearing/moved/replaced button ends retries; we never click remembered coordinates. */
    fun take(freshTarget: String?, eligible: Boolean): Int? {
        if (!eligible || isFinished) return null
        if (originalTarget != null && freshTarget != originalTarget) {
            stop()
            return null
        }
        // Before the first match, keep waiting for the ad to expose its button.
        if (freshTarget == null) return null
        val time = now()
        if (lastAttempt?.let { time - it < RETRY_INTERVAL_MS } == true) return null
        originalTarget = freshTarget
        lastAttempt = time
        return ++count
    }

    companion object {
        const val RETRY_INTERVAL_MS = 600L
    }
}
