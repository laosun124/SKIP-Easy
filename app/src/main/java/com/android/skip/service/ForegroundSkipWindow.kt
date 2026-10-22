package com.android.skip.service

/** Main-thread-owned state. The clock must be monotonic (elapsedRealtime on Android). */
internal class ForegroundSkipWindow(private val now: () -> Long) {
    data class Ticket(
        val packageName: String, val windowId: Int, val visitId: Long, val generation: Long
    )

    private var packageName: String? = null
    private var enteredAt = 0L
    private var generation = 0L
    private var visitId = 0L
    private var ticket: Ticket? = null

    fun showApp(packageName: String, windowId: Int): Ticket {
        if (this.packageName != packageName) {
            this.packageName = packageName
            enteredAt = now()
            visitId++
            ticket = null
        }
        if (ticket?.packageName != packageName || ticket?.windowId != windowId) {
            ticket = Ticket(packageName, windowId, visitId, ++generation)
        }
        return ticket!!
    }

    /** A keyboard, system panel or unavailable window invalidates work, not the deadline. */
    fun suspendClicks() {
        ticket = null
    }

    /** Home, another app, screen off or the lock screen ends this foreground visit. */
    fun leaveApp() {
        suspendClicks()
        packageName = null
    }

    fun canClick(expected: Ticket): Boolean =
        ticket == expected && now() - enteredAt in 0 until DURATION_MS

    companion object {
        const val DURATION_MS = 6_000L
    }
}
