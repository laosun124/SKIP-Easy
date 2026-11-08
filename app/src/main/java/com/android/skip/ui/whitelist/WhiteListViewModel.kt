package com.android.skip.ui.whitelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.skip.MyApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class WhiteListViewModel @Inject constructor(
    private val repository: WhiteListRepository
) : ViewModel() {
    val packages = repository.packages
    val wechatOptimization = repository.wechatOptimization
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun updateWhiteList(packageName: String, checked: Boolean) {
        if (_busy.value) return
        viewModelScope.launch {
            try {
                repository.updateWhiteList(packageName, checked)
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { _message.value = "保存失败，请重试" }
        }
    }

    fun setWechatOptimization(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.setWechatOptimization(enabled)
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { _message.value = "微信优化设置保存失败，请重试" }
        }
    }

    fun applyAutomatic() {
        if (_busy.value) return
        _busy.value = true
        _message.value = null
        viewModelScope.launch {
            try {
                val installed = withContext(Dispatchers.IO) {
                    MyApp.context.packageManager.getInstalledApplications(0).map { it.packageName }
                }
                val count = repository.applyAutomatic(installed)
                _message.value = "已完成 · 扫描 ${installed.size} 个，开启 $count 个。可继续手动调整。"
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                _message.value = "自动识别失败，白名单未更改，请重试"
            } finally { _busy.value = false }
        }
    }
}
