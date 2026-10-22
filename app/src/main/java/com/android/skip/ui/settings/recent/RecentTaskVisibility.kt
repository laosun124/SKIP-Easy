package com.android.skip.ui.settings.recent

/** Shared process state; Activity recreation must never restore an old screen's value. */
internal class RecentTaskVisibility(
    initialExcluded: Boolean,
    private val applyToTasks: (Boolean) -> Unit,
    private val persist: (Boolean) -> Unit
) {
    var excluded = initialExcluded
        private set

    fun setExcluded(value: Boolean) {
        excluded = value
        reapply()
        persist(value)
    }

    fun reapply() = applyToTasks(excluded)
}
