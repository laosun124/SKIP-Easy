package com.android.skip.ui.help

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.android.skip.R
import com.android.skip.ui.components.ScaffoldPage
import com.android.skip.ui.settings.theme.SwitchThemeViewModel
import com.android.skip.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HelpActivity : AppCompatActivity() {
    private val theme by viewModels<SwitchThemeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val topic = intent.getStringExtra("topic")?.takeIf { it in topics } ?: "start"
        val content = assets.open("help/$topic.txt").bufferedReader().use { it.readText() }
        setContent {
            AppTheme(theme) {
                ScaffoldPage(R.string.local_help_title, { finish() }, {
                    SelectionContainer(Modifier.verticalScroll(rememberScrollState())) {
                        Text(content)
                    }
                })
            }
        }
    }

    companion object {
        private val topics = setOf("start", "alive", "whitelist", "settings", "about")
        fun open(context: Context, topic: String) {
            context.startActivity(Intent(context, HelpActivity::class.java).putExtra("topic", topic))
        }
    }
}
