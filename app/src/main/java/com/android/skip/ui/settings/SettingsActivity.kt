package com.android.skip.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import com.android.skip.MyApp
import com.android.skip.R
import com.android.skip.data.config.ConfigViewModel
import com.android.skip.ui.components.ResourceIcon
import com.android.skip.ui.components.ScaffoldPage
import com.android.skip.ui.components.notification.NotificationDialog
import com.android.skip.ui.components.notification.NotificationDialogViewModel
import com.android.skip.ui.settings.log.SkipLogActivity
import com.android.skip.ui.settings.log.SkipLogButton
import com.android.skip.ui.settings.log.SkipLogViewModel
import com.android.skip.ui.settings.recent.RecentButton
import com.android.skip.ui.settings.recent.RecentViewModel
import com.android.skip.ui.settings.theme.SwitchThemeButton
import com.android.skip.ui.settings.theme.SwitchThemeViewModel
import com.android.skip.ui.settings.tip.TipButton
import com.android.skip.ui.settings.tip.TipViewModel
import com.android.skip.ui.theme.AppTheme
import com.android.skip.ui.help.HelpActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private val tipViewModel by viewModels<TipViewModel>()



    private val notificationDialogViewModel by viewModels<NotificationDialogViewModel>()

    private val recentViewModel by viewModels<RecentViewModel>()

    private val switchThemeViewModel by viewModels<SwitchThemeViewModel>()


    private val skipLogViewModel by viewModels<SkipLogViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme(switchThemeViewModel) {
                ScaffoldPage(R.string.settings, { finish() }, {
                    RecentButton(recentViewModel)
                    TipButton(tipViewModel)
                    SkipLogButton(skipLogViewModel) {
                        startActivity(Intent(this, SkipLogActivity::class.java))
                    }
                    SwitchThemeButton(switchThemeViewModel)
                    NotificationDialog(notificationDialogViewModel) {
                        notificationDialogViewModel.changeDialogState(false)
                        tipViewModel.changeEnable(false)
                    }
                })
            }
        }

        tipViewModel.enable.observe(this) {
            if (it && !NotificationManagerCompat.from(MyApp.context).areNotificationsEnabled()) {
                notificationDialogViewModel.changeDialogState(true)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        if (!NotificationManagerCompat.from(MyApp.context).areNotificationsEnabled()) {
            tipViewModel.changeEnable(false)
        }
    }
}