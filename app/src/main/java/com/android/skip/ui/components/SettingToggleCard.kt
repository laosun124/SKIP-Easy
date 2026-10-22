package com.android.skip.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SupportingText(text: String) {
    Text(text, fontSize = 13.sp, lineHeight = 20.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun SettingToggleCard(title: String, description: String, checked: Boolean,
                      enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                SupportingText(description)
            }
            Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
        }
    }
}
