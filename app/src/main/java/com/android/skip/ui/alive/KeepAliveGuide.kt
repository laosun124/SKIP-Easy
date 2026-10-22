package com.android.skip.ui.alive

import java.util.Locale

enum class SettingsDestination { APP_INFO, SYSTEM, BATTERY }
data class KeepAliveStep(val title: String, val instructions: String, val destination: SettingsDestination)
data class KeepAliveGuide(val id: String, val name: String, val steps: List<KeepAliveStep>)

object KeepAliveGuides {
    private fun step(title: String, instructions: String, destination: SettingsDestination) =
        KeepAliveStep(title, instructions, destination)
    val all = listOf(
        KeepAliveGuide("xiaomi", "小米 / 红米 · HyperOS / MIUI", listOf(
            step("允许后台运行", "应用信息 → 省电策略 → 无限制；部分版本在 设置 → 电池 → SKIP 中。", SettingsDestination.APP_INFO),
            step("开启自启动", "设置 → 应用设置 → 权限 / 授权管理 → 自启动管理（后台自启动），打开 SKIP。找不到时搜索「自启动」。", SettingsDestination.SYSTEM)
        )),
        KeepAliveGuide("huawei", "华为 · EMUI / HarmonyOS（兼容 APK）", listOf(
            step("手动管理应用启动", "设置中搜索「应用启动管理」→ SKIP，关闭自动管理，打开允许自启动、允许关联启动和允许后台活动。", SettingsDestination.SYSTEM),
            step("检查电池优化", "系统设置搜索「电池优化」，如有此项，切换到所有应用，找到 SKIP 并选择不允许优化。", SettingsDestination.BATTERY)
        )),
        KeepAliveGuide("honor", "荣耀 · MagicOS / Magic UI", listOf(
            step("允许后台活动", "设置中搜索「启动管理」或「应用启动管理」→ SKIP，关闭自动管理，勾选允许后台活动；如有自启动、关联启动，也一并允许。", SettingsDestination.SYSTEM)
        )),
        KeepAliveGuide("oppo", "OPPO / 一加 · ColorOS", listOf(
            step("允许后台运行", "应用信息 → 耗电管理 / 电池用量，允许后台活动。部分版本在 设置 → 电池 → 应用耗电管理 中。", SettingsDestination.APP_INFO),
            step("开启自启动", "设置 → 应用 → 自启动，打开 SKIP。较旧版本在 手机管家 → 权限隐私 → 自启动管理。", SettingsDestination.SYSTEM)
        )),
        KeepAliveGuide("realme", "真我 · realme UI", listOf(
            step("允许后台活动", "应用信息 → 电池用量，开启允许后台活动。较新版本可在 设置 → 电池 → 省电设置 → 应用电池管理 中选择 SKIP 并允许后台活动。", SettingsDestination.APP_INFO),
            step("开启自启动", "应用信息 → 电池用量，开启允许自动启动；或在系统设置搜索「自启动」并允许 SKIP。", SettingsDestination.APP_INFO)
        )),
        KeepAliveGuide("vivo", "vivo / iQOO · OriginOS / Funtouch OS", listOf(
            step("允许后台耗电", "设置 → 电池 → 后台耗电管理 → SKIP，允许后台高耗电。旧版本为 电池 → 后台高耗电，打开 SKIP。", SettingsDestination.SYSTEM),
            step("开启自启动", "系统设置搜索「自启动」并允许 SKIP；部分版本在 i管家 → 应用管理 → 权限管理 → 自启动 中。", SettingsDestination.SYSTEM)
        )),
        KeepAliveGuide("samsung", "三星 · One UI", listOf(
            step("取消电池限制", "应用信息 → 电池，选择不受限制（如提供此选项）。", SettingsDestination.APP_INFO),
            step("加入不休眠应用", "设置 → 电池（或 设备维护 → 电池）→ 后台使用限制，将 SKIP 移出休眠 / 深度休眠列表，并加入从不自动休眠的应用。", SettingsDestination.SYSTEM)
        )),
        KeepAliveGuide("android", "Pixel / 原生 Android / 其他系统", listOf(
            step("允许后台使用", "应用信息 → 应用耗电量 / 电池，打开允许后台使用；如有不受限制选项，可选择它。不同系统的名称可能不同。", SettingsDestination.APP_INFO),
            step("检查电池优化", "在电池优化列表切换到所有应用，找到 SKIP，选择不优化。未提供此项的设备可按应用信息中的后台运行设置操作。", SettingsDestination.BATTERY)
        ))
    )

    fun defaultId(manufacturer: String): String = when (manufacturer.lowercase(Locale.ROOT)) {
        "xiaomi", "redmi", "poco" -> "xiaomi"
        "huawei" -> "huawei"
        "honor" -> "honor"
        "oppo" -> "oppo"
        // OnePlus can run OxygenOS or ColorOS: leave the choice to the user.
        "realme" -> "realme"
        "vivo", "iqoo" -> "vivo"
        "samsung" -> "samsung"
        else -> "android"
    }
    fun resolve(id: String): KeepAliveGuide = all.firstOrNull { it.id == id } ?: all.last()
}
