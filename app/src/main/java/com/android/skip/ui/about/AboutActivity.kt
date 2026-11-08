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
                    Text("SKIP 易用版基于 GuoXiCheng 的开源项目 SKIP 开发，修复使用问题并简化操作。感谢原作者及贡献者。")
                    FlatButton(content = {
                        RowContent("项目源代码", "https://github.com/laosun124/SKIP-Easy")
                    }, onClick = {
                        startActivity(Intent(this, WebViewActivity::class.java)
                            .putExtra("address", "https://github.com/laosun124/SKIP-Easy"))
                    })
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
