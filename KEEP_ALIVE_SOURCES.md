# 保活说明来源与范围

核对日期：2026-10-22。以下内容用于安装包内的离线说明，不会在打开「功能介绍」时访问网页。菜单名称可能随地区、机型和系统版本变化，按钮打开公开的 Android 设置入口，再由用户按对应文字操作；没有假定厂商私有页面在所有设备均可跳转。

| 系统 | 官方参考 | 本版采用的操作 |
| --- | --- | --- |
| HyperOS / MIUI | [小米后台自启动](https://www.mi.com/global/support/faq/details/KA-507608/)、[小米后台电池限制](https://www.mi.com/uk/support/faq/details/KA-538010/) | 允许后台自启动，省电策略选择无限制 |
| EMUI / 兼容 APK 的 HarmonyOS | [华为无障碍服务开关自动关闭](https://consumer.huawei.com/cn/support/content/zh-cn00410039/)、[应用无法后台运行](https://consumer.huawei.com/cn/support/content/zh-cn00428704/) | 应用启动管理改为手动，允许自启动、关联启动和后台活动 |
| MagicOS / Magic UI | [荣耀应用后台重新启动](https://www.honor.com/cn/support/content/zh-cn00406916/) | 关闭自动管理，允许后台活动 |
| ColorOS | [OPPO 手机管家与自启动](https://support.oppo.com/co/answer/?aid=neu119)、[OPPO 耗电控制](https://support.oppo.com/lk/answer/?aid=neu135) | 自启动、后台活动与耗电限制；一加仅在使用 ColorOS 时选此项 |
| realme UI | [应用管理](https://www.realme.com/global/support/kw/doc/2085394)、[后台活动与自启动选项](https://www.realme.com/in/support/kw/doc/2083586)、[应用电池管理](https://www.realme.com/in/support/kw/doc/2068398) | 允许后台活动和自动启动；由官方关闭步骤反向整理开启步骤 |
| OriginOS / Funtouch OS | [vivo 后台运行帮助](https://www.vivo.com/tw/support/questionByTitle?title=%E8%83%8C%E6%99%AF%E9%81%8B%E8%A1%8C)、[vivo 社区操作路径](https://bbs.vivo.com.cn/newbbs/thread/32191985) | 后台高耗电与自启动；旧版路径作为备选 |
| One UI | [Samsung Application Management](https://developer.samsung.com/mobile/app-management.html) | 将 SKIP 移出休眠列表、加入不休眠应用 |
| Pixel / 原生 Android / 其他 | [Google Pixel 应用后台耗电](https://support.google.com/pixelphone/answer/6090599?hl=en-GB) | 应用信息中的允许后台使用，以及设备提供的电池优化选项 |

保活只能减少服务被清理，不能保证服务永久运行。没有在各厂商真机上验证跳转。华为选项不承诺不支持 Android APK 的系统兼容性。未单列的系统采用通用设置指引，用户可手动选择更符合自己设备的系统。

# 自动白名单范围

按用户要求匹配 `com.android.` 包名前缀（并非 Android 系统应用标志），以及 `AutomaticWhitelist.kt` 中的厂商前缀和常见国外应用精确包名集合。不会按英文应用名、安装商店或通用 `com.` 前缀推断国别；无法完整识别全部国外应用。扫描只在本机进行，手动触发时覆盖当前已安装应用的白名单设置，操作完成后可手动调整。


修复 5 包名前缀参考示例：[Apple Music](https://play.google.com/store/apps/details?id=com.apple.android.music)、[微软应用](https://play.google.com/store/apps/details?id=com.microsoft.office.officehubrow)、[Adobe Reader](https://play.google.com/store/apps/details?id=com.adobe.reader)、[Proton Calendar](https://play.google.com/store/apps/details?id=me.proton.android.calendar)。厂商前缀为启发式分类，不进行签名认证，不代表所有使用该前缀的包都由该厂商发布。
