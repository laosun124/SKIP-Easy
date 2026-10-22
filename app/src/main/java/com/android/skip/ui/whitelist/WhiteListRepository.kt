package com.android.skip.ui.whitelist

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.android.skip.MyApp
import com.android.skip.R
import com.android.skip.util.dataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhiteListRepository @Inject constructor() {
    private val prefix get() = MyApp.context.getString(R.string.whitelist_dot)
    private val mutex = Mutex()
    private val state: MutableStateFlow<Set<String>> by lazy {
        MutableStateFlow(runBlocking {
            MyApp.context.dataStore.data.first().asMap()
                .filter { it.key.name.startsWith(prefix) && it.value == true }
                .keys.map { it.name.removePrefix(prefix) }.toSet()
        })
    }
    val packages: StateFlow<Set<String>> get() = state

    suspend fun updateWhiteList(packageName: String, checked: Boolean) = mutex.withLock {
        val before = state.value
        MyApp.context.dataStore.edit { it[booleanPreferencesKey(prefix + packageName)] = checked }
        state.value = if (checked) before + packageName else before - packageName
    }

    suspend fun applyAutomatic(installed: Collection<String>): Int = mutex.withLock {
        val before = state.value
        val selected = AutomaticWhitelist.select(installed)
        // One atomic write: a failure cannot leave half of the installed apps changed.
        MyApp.context.dataStore.edit { prefs ->
            installed.forEach { prefs[booleanPreferencesKey(prefix + it)] = it in selected }
        }
        state.value = (before - installed.toSet()) + selected
        selected.size
    }

    fun isAppInWhiteList(packageName: String): Boolean = packageName in state.value
}
