package com.android.skip.ui.settings.recent

import org.junit.Assert.*
import org.junit.Test

class RecentTaskVisibilityTest {
    @Test fun toggleOnSettingsThenHomeKeepsTaskExcludedWithoutVisitingMain() {
        var hidden = false
        var saved = false
        val state = RecentTaskVisibility(false, { hidden = it }, { saved = it })
        state.reapply() // main screen created
        state.setExcluded(true) // settings screen toggle
        assertTrue(hidden) // applies immediately, before returning to main
        state.reapply() // settings paused on Home
        state.reapply() // settings stopped
        assertTrue(hidden)
        assertTrue(saved)
    }

    @Test fun reopeningAppReappliesCurrentValueToNewTask() {
        val tasks = mutableListOf(false)
        val state = RecentTaskVisibility(false, { hidden -> tasks.indices.forEach { tasks[it] = hidden } }, {})
        state.setExcluded(true)
        tasks.add(false) // launcher created another task
        state.reapply()
        assertTrue(tasks.all { it })
    }

    @Test fun disablingAndRestartingRestoresVisibleTasks() {
        var hidden = true
        var saved = true
        val state = RecentTaskVisibility(true, { hidden = it }, { saved = it })
        state.setExcluded(false)
        state.reapply()
        assertFalse(hidden)
        val restarted = RecentTaskVisibility(saved, { hidden = it }, {})
        restarted.reapply()
        assertFalse(hidden)
    }
}
