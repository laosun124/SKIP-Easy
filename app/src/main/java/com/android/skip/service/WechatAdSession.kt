package com.android.skip.service

/** One six-second opportunity per visible mini-program ad, independent of WeChat's launch time. */
internal class WechatAdSession(private val now: () -> Long) {
    private var active = false
    private var startedAt = 0L
    private var absentSince: Long? = null
    private var epoch = 0L

    fun observe(adVisible: Boolean): Long? {
        val time = now()
        if (!adVisible) {
            if (active) {
                if (absentSince == null) absentSince = time
                if (time - absentSince!! >= 1000) active = false
            }
            return null
        }
        // A brief missing accessibility node must not renew the deadline.
        absentSince = null
        if (!active) {
            active = true
            startedAt = time
            epoch++
        }
        return epoch
    }

    fun canClick(expected: Long, optimizationEnabled: Boolean = true) = optimizationEnabled && active && epoch == expected && absentSince == null &&
        now() - startedAt in 0 until ForegroundSkipWindow.DURATION_MS

    fun leaveMiniProgram() {
        active = false
        absentSince = null
    }
}
