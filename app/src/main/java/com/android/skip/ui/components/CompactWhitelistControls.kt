package com.android.skip.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.skip.R

@Composable
fun CompactWhitelistControls(showSystem: Boolean, busy: Boolean, wechatOptimization: Boolean,
                             onShowSystem: (Boolean) -> Unit, onRecognize: () -> Unit,
                             onWechatOptimization: (Boolean) -> Unit) {
    var help by remember { mutableStateOf<Int?>(null) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("显示系统应用", Modifier.weight(1f), fontSize = 14.sp)
            IconButton(onClick = { help = 0 }) {
                Icon(painterResource(R.drawable.info), contentDescription = "显示系统应用说明", modifier = Modifier.size(20.dp))
            }
            Switch(checked = showSystem, onCheckedChange = onShowSystem)
        }
        HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("微信小程序专属优化", Modifier.weight(1f), fontSize = 14.sp)
            IconButton(onClick = { help = 2 }) {
                Icon(painterResource(R.drawable.info), contentDescription = "微信小程序专属优化说明", modifier = Modifier.size(20.dp))
            }
            Switch(checked = wechatOptimization, onCheckedChange = onWechatOptimization)
        }
        HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("自动识别白名单", Modifier.weight(1f), fontSize = 14.sp)
            IconButton(onClick = { help = 1 }) {
                Icon(painterResource(R.drawable.info), contentDescription = "自动识别白名单说明", modifier = Modifier.size(20.dp))
            }
            TextButton(onClick = onRecognize, enabled = !busy) {
                Text(if (busy) "识别中" else "执行")
            }
        }
    }
    help?.let { topic ->
        AlertDialog(
            onDismissRequest = { help = null },
            icon = { Icon(painterResource(R.drawable.info), contentDescription = null) },
            title = { Text(when (topic) { 0 -> "显示系统应用"; 1 -> "自动识别白名单"; else -> "微信小程序专属优化" }) },
            text = { Text(when (topic) {
                0 -> "开启后，下方列表会显示系统应用。此选项只影响列表显示，不会更改白名单。"
                1 -> "点击执行一次，扫描当前已安装的全部应用，包括列表中隐藏的系统应用。\n\ncom.android. 前缀及已收录的国外应用开启白名单，其余关闭。会覆盖当前手动选择，完成后可继续调整。\n\n识别使用厂商前缀和本地名单，可能有遗漏；不会持续自动管理新安装的应用，也不会更改微信专属优化开关。"
                else -> "开启：只对确认的微信小程序广告栏尝试点击左上角「跳过」，微信在白名单中也可生效。每条广告识别后最多尝试 6 秒。\n\n关闭：微信本体和小程序都不自动点击。\n\n聊天、主界面和朋友圈始终不使用通用「跳过」规则。建议仍将微信加入白名单；这个开关单独决定是否启用小程序专属优化。\n\n开关默认开启，选择会保存在本机。不同微信版本和广告形式的实际效果仍需验证。"
            }) },
            confirmButton = { TextButton(onClick = { help = null }) { Text("知道了") } }
        )
    }
}
