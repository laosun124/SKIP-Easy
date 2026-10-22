package com.android.skip.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.ui.platform.LocalContext
import com.android.skip.ui.help.HelpActivity
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.skip.R


@Composable
fun ScaffoldPage(
    barTitle: Int,
    onBackClick: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
    menuItems: (@Composable (ColumnScope.() -> Unit))? = null
) {
    val context = LocalContext.current
    val helpTopic = when (barTitle) {
        R.string.whitelist -> "whitelist"
        R.string.alive -> "alive"
        R.string.settings -> "settings"
        R.string.about -> "about"
        R.string.settings_skip_log_page -> "settings"
        else -> null
    }
    Scaffold(topBar = {
        Row(
            modifier = Modifier
                .padding(
                    15.dp, 15.dp, 15.dp, 0.dp
                )
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(Modifier.weight(1f)) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    ResourceIcon(iconResource = R.drawable.arrow_back)
                }
                Text(
                    text = stringResource(id = barTitle),
                    fontSize = 24.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }

            Row {
                if (menuItems != null) { Column { menuItems() } }
                if (helpTopic != null) {
                    IconButton(onClick = { HelpActivity.open(context, helpTopic) }) {
                        Icon(painterResource(R.drawable.help), contentDescription = "功能介绍")
                    }
                }
            }

        }
    }, containerColor = MaterialTheme.colorScheme.background,
        content = { contentPadding ->
            Column(
                Modifier
                    .padding(contentPadding)
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content(contentPadding)
            }
        })
}