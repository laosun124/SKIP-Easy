package com.android.skip.ui.whitelist

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.livedata.observeAsState
import com.android.skip.ui.components.CompactWhitelistControls
import androidx.compose.runtime.LaunchedEffect
import android.widget.Toast
import com.android.skip.ui.components.SupportingText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.android.skip.ui.components.FlatButton
import com.android.skip.ui.components.RowContent
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.skip.MyApp
import com.android.skip.R
import com.android.skip.ui.components.ResourceIcon
import com.android.skip.ui.components.ScaffoldPage
import com.android.skip.ui.settings.theme.SwitchThemeViewModel
import com.android.skip.ui.theme.AppTheme
import com.android.skip.ui.help.HelpActivity
import com.android.skip.ui.whitelist.list.AppListColumn
import com.android.skip.ui.whitelist.list.AppListViewModel
import com.android.skip.ui.whitelist.search.SearchTextField
import com.android.skip.ui.whitelist.search.SearchViewModel
import com.android.skip.ui.whitelist.system.ShowSystemButton
import com.android.skip.ui.whitelist.system.ShowSystemViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WhiteListActivity : AppCompatActivity() {
    private val appListViewModel by viewModels<AppListViewModel>()

    private val showSystemViewModel by viewModels<ShowSystemViewModel>()

    private val whiteListViewModel by viewModels<WhiteListViewModel>()

    private val switchThemeViewModel by viewModels<SwitchThemeViewModel>()

    private val searchViewModel by viewModels<SearchViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme(switchThemeViewModel) {
                ScaffoldPage(R.string.whitelist, { finish() }, {
                    val busy by whiteListViewModel.busy.collectAsState()
                    val wechatOptimization by whiteListViewModel.wechatOptimization.collectAsState()
                    val result by whiteListViewModel.message.collectAsState()
                    val showSystem by showSystemViewModel.isShowSystem.observeAsState(false)
                    LaunchedEffect(result) {
                        result?.let { Toast.makeText(this@WhiteListActivity, it, Toast.LENGTH_LONG).show() }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompactWhitelistControls(showSystem, busy, wechatOptimization,
                            onShowSystem = { showSystemViewModel.changeIsShowSystem(it) },
                            onRecognize = { whiteListViewModel.applyAutomatic() },
                            onWechatOptimization = { whiteListViewModel.setWechatOptimization(it) })
                        // 搜索栏
                        SearchTextField(searchViewModel)
                        // 应用列表
                        AppListColumn(appListViewModel, whiteListViewModel)
                    }
                })
            }
        }

        showSystemViewModel.isShowSystem.observe(this) {
            appListViewModel.reloadData(it, searchViewModel.getQuery())
        }

        searchViewModel.query.observe(this) {
            appListViewModel.reloadData(showSystemViewModel.getIsShowSystem(), it)
        }
    }
}