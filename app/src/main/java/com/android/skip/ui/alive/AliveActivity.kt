package com.android.skip.ui.alive

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.android.skip.ui.components.SupportingText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.android.skip.R
import com.android.skip.ui.alive.notificationbar.NotificationBarButton
import com.android.skip.ui.alive.notificationbar.NotificationBarViewModel
import com.android.skip.ui.components.FlatButton
import com.android.skip.ui.components.RowContent
import com.android.skip.ui.components.ScaffoldPage
import com.android.skip.ui.components.notification.NotificationDialog
import com.android.skip.ui.components.notification.NotificationDialogViewModel
import com.android.skip.ui.help.HelpActivity
import com.android.skip.ui.settings.theme.SwitchThemeViewModel
import com.android.skip.ui.theme.AppTheme
import com.android.skip.util.DataStoreUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AliveActivity : AppCompatActivity() {
    private val notificationBarViewModel by viewModels<NotificationBarViewModel>()
    private val notificationDialogViewModel by viewModels<NotificationDialogViewModel>()
    private val theme by viewModels<SwitchThemeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val saved = DataStoreUtils.getSyncData("KEEP_ALIVE_SYSTEM", KeepAliveGuides.defaultId(Build.MANUFACTURER))
        setContent {
            AppTheme(theme) {
                var selected by rememberSaveable { mutableStateOf(saved) }
                var expanded by remember { mutableStateOf(false) }
                val guide = KeepAliveGuides.resolve(selected)
                ScaffoldPage(R.string.alive, { finish() }, {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box {
                            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                                    SupportingText("手机操作系统")
                                    Text(guide.name, style = MaterialTheme.typography.titleSmall)
                                }
                                Text("更换 ▾", color = MaterialTheme.colorScheme.primary)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                KeepAliveGuides.all.forEach { item ->
                                    DropdownMenuItem(text = { Text(item.name) }, onClick = {
                                        selected = item.id
                                        expanded = false
                                        lifecycleScope.launch { DataStoreUtils.putData("KEEP_ALIVE_SYSTEM", item.id) }
                                    })
                                }
                            }
                        }
                        SupportingText("按顺序完成下方设置。不同系统版本的菜单名称可能略有差异。")
                        guide.steps.forEachIndexed { index, step ->
                            val target = when (step.destination) {
                                SettingsDestination.APP_INFO -> "打开 SKIP 应用信息"
                                SettingsDestination.SYSTEM -> "打开系统设置"
                                SettingsDestination.BATTERY -> "打开电池优化列表"
                            }
                            OutlinedCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${index + 1}. ${step.title}", style = MaterialTheme.typography.titleMedium)
                                    SupportingText(step.instructions)
                                    TextButton(onClick = { openSettings(step.destination) }) { Text("$target →") }
                                }
                            }
                        }
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("最近任务锁定", style = MaterialTheme.typography.titleMedium)
                                SupportingText("在最近任务中打开 SKIP 卡片菜单，选择「锁定」或「保持打开」（系统支持时）。")
                                SupportingText("看不到卡片？先暂时关闭设置中的「隐藏后台」。")
                            }
                        }
                        NotificationBarButton(notificationBarViewModel)
                        SupportingText("保活可减少服务被关闭，不会延长自动点击的 6 秒时限。")
                    }
                    NotificationDialog(notificationDialogViewModel) {
                        notificationBarViewModel.changeEnable(false)
                        notificationDialogViewModel.changeDialogState(false)
                    }
                })
            }
        }
        notificationBarViewModel.enable.observe(this) {
            if (it && !NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                notificationDialogViewModel.changeDialogState(true)
            }
        }
    }

    private fun openSettings(destination: SettingsDestination) {
        val requested = when (destination) {
            SettingsDestination.APP_INFO -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            SettingsDestination.SYSTEM -> Intent(Settings.ACTION_SETTINGS)
            SettingsDestination.BATTERY -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        }
        for (intent in listOf(requested, Intent(Settings.ACTION_SETTINGS))) {
            try {
                startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // Some vendors omit individual settings pages; fall back to Settings.
            } catch (_: SecurityException) {
                // Vendor settings may not be exported.
            }
        }
        Toast.makeText(this, "无法直接打开，请手动进入系统设置并按页面说明操作", Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            notificationBarViewModel.changeEnable(false)
        }
    }
}
