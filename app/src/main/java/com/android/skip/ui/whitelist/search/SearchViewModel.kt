package com.android.skip.ui.whitelist.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(): ViewModel() {
    private val _query = MutableLiveData("")
    val query: LiveData<String> = _query

    fun changeQuery(query: String) {
        // Called on the UI thread; publish the latest text before list observers run.
        // Selection-only edits must not recreate the app list.
        if (_query.value != query) _query.value = query
    }

    fun getQuery(): String {
        return _query.value ?: ""
    }
}