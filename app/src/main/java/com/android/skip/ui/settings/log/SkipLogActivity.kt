package com.android.skip.ui.settings.log

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.skip.R
import com.android.skip.ui.components.ScaffoldPage
import com.android.skip.ui.settings.theme.SwitchThemeViewModel
import com.android.skip.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class SkipLogActivity : AppCompatActivity() {
    private val skipLogViewModel by viewModels<SkipLogViewModel>()

    private val switchThemeViewModel by viewModels<SwitchThemeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme(switchThemeViewModel) {
                ScaffoldPage(R.string.settings_skip_log_page, { finish() }, {
                    SkipLogContent(skipLogViewModel)
                }, {
                    IconButton(onClick = { skipLogViewModel.clearAllLogs() }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "清空日志")
                    }
                })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        skipLogViewModel.loadDailyLogs()
    }
}

@Composable
private fun SkipLogContent(skipLogViewModel: SkipLogViewModel) {
    val dailyLogs = skipLogViewModel.dailyLogs.observeAsState(emptyList())
    val dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (dailyLogs.value.isEmpty()) {
            Text(
                text = stringResource(id = R.string.settings_skip_log_empty),
                color = MaterialTheme.colorScheme.onBackground
            )
        } else {
            dailyLogs.value.forEach { day ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = day.date,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    day.entries.forEach { entry ->
                        val timeText = Instant.ofEpochMilli(entry.timestamp)
                            .atZone(ZoneId.systemDefault())
                            .format(dateTimeFormatter)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = timeText,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "${stringResource(id = R.string.settings_skip_log_level)}: ${entry.level}",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${stringResource(id = R.string.settings_skip_log_source)}: ${entry.source}",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${stringResource(id = R.string.settings_skip_log_message)}: ${entry.message}",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 13.sp
                            )
                            entry.stackTrace?.let {
                                Text(
                                    text = "${stringResource(id = R.string.settings_skip_log_stack_trace)}: $it",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}