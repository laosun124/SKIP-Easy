package com.android.skip.ui.settings.recent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.skip.MyApp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RecentViewModel @Inject constructor() : ViewModel() {
    private val _excludeFromRecent = MutableLiveData(MyApp.recentTaskVisibility.excluded)
    val excludeFromRecent: LiveData<Boolean> = _excludeFromRecent

    fun changeExcludeFromRecent(excludeFromRecent: Boolean) {
        MyApp.recentTaskVisibility.setExcluded(excludeFromRecent)
        _excludeFromRecent.value = excludeFromRecent
    }
}
