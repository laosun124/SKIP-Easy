package com.android.skip

import android.annotation.SuppressLint
import android.app.Application
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.android.skip.util.DataStoreUtils
import com.android.skip.ui.settings.recent.RecentTaskVisibility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.Utils
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application(), Configuration.Provider {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        context = this
        deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

        DataStoreUtils.init(this)

        Utils.init(this)
        // Cancel the periodic task retained when upgrading from older versions.
        androidx.work.WorkManager.getInstance(this).cancelUniqueWork(getString(R.string.worker_sync))
        val recentKey = getString(R.string.store_exclude_from_recent)
        recentTaskVisibility = RecentTaskVisibility(
            DataStoreUtils.getSyncData(recentKey, false),
            applyToTasks = { excluded ->
                val manager = getSystemService(ActivityManager::class.java)
                manager.appTasks.forEach { task ->
                    runCatching { task.setExcludeFromRecents(excluded) }.onFailure { LogUtils.e(it) }
                }
            },
            persist = { excluded ->
                applicationScope.launch { DataStoreUtils.putData(recentKey, excluded) }
            }
        )
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, state: Bundle?) = recentTaskVisibility.reapply()
            override fun onActivityStarted(activity: Activity) = recentTaskVisibility.reapply()
            override fun onActivityResumed(activity: Activity) = recentTaskVisibility.reapply()
            override fun onActivityPaused(activity: Activity) = recentTaskVisibility.reapply()
            override fun onActivityStopped(activity: Activity) = recentTaskVisibility.reapply()
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
        LogUtils.getConfig()
            .setLogSwitch(false)                    // 是否输出日志开关
            .setConsoleSwitch(true)                // 是否在控制台输出日志开关
            .setGlobalTag("SKIP_APP")              // 全局标签
            .setLog2FileSwitch(true)               // 是否写入日志文件开关
            .setDir("${this.cacheDir}/logs")       // 日志文件目录
            .setFilePrefix("log")                  // 日志文件前缀
            .setBorderSwitch(true)                 // 日志边框开关
            .setConsoleFilter(LogUtils.V)          // 控制台过滤器
            .setFileFilter(LogUtils.V)             // 文件过滤器
            .setStackDeep(1)                       // 栈深度
            .setSaveDays(7)                        // 日志保留天数
    }

    companion object {
        internal lateinit var recentTaskVisibility: RecentTaskVisibility
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context

        lateinit var deviceName: String
    }

    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
