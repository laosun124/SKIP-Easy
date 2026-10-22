package com.android.skip.ui.about

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import com.android.skip.R
import com.android.skip.ui.components.FlatButton
import com.android.skip.ui.components.RowContent
import com.android.skip.ui.components.ScaffoldPage
import com.android.skip.ui.help.HelpActivity
import com.android.skip.ui.settings.theme.SwitchThemeViewModel
import com.android.skip.ui.theme.AppTheme
import com.android.skip.ui.webview.WebViewActivity
import com.blankj.utilcode.util.AppUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AboutActivity : AppCompatActivity() {
    private val theme by viewModels<SwitchThemeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme(theme) {
                ScaffoldPage(R.string.about, { finish() }, {
                    Text("本应用基于 GuoXiCheng 的开源项目 SKIP 修改，是个人修改版。原项目源代码链接如下。")
                    FlatButton(content = {
                        RowContent("原项目源代码", "https://github.com/GuoXiCheng/SKIP")
                    }, onClick = {
                        startActivity(Intent(this, WebViewActivity::class.java)
                            .putExtra("address", "https://github.com/GuoXiCheng/SKIP"))
                    })
                    FlatButton(content = {
                        RowContent(R.string.about_app_version, AppUtils.getAppVersionName())
                    })
                })
            }
        }
    }
}
