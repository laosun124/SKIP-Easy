package com.android.skip.data.config

import com.android.skip.data.config.PersonalSplashRules.Box
import com.android.skip.data.config.WechatAdRules.Node
import org.junit.Assert.*
import org.junit.Test

class WechatAdRulesTest {
    private val mini = "com.tencent.mm.plugin.appbrand.ui.AppBrandUI01"
    private val frame = Box(0, 0, 1200, 2670)
    // Layout approximates the supplied screenshot; actual device nodes are not available.
    private fun toolbar(f: Box = frame): List<Node> {
        val unit = minOf(f.width, f.height).toDouble()
        fun box(l: Double, t: Double, r: Double, b: Double) = Box(
            f.left + (l * unit).toInt(), f.top + (t * unit).toInt(),
            f.left + (r * unit).toInt(), f.top + (b * unit).toInt())
        return listOf(Node(f), Node(box(.03, .13, .45, .23), parent = 0),
            Node(box(.06, .15, .16, .21), "广告", parent = 1),
            Node(box(.23, .15, .32, .21), "跳过", parent = 1),
            Node(box(.37, .15, .44, .21), "5秒", parent = 0))
    }
    private fun select(nodes: List<Node>, activity: String? = mini, f: Box = frame) =
        WechatAdRules.select(activity, f, nodes)

    @Test fun suppliedHeaderSelectsOnlySkipAndNeverAdDropdown() {
        assertEquals(3, select(toolbar()))
    }
    @Test fun portraitLandscapeTabletAndOffsetWindowsUseTheirOwnGeometry() {
        for ((w, h) in listOf(720 to 1280, 1080 to 1920, 1080 to 2400, 1200 to 2670,
            1440 to 3200, 1920 to 1080, 2560 to 1600, 1600 to 2560, 2208 to 1840, 320 to 480)) {
            val f = Box(300, 200, 300 + w, 200 + h)
            assertEquals("$w x $h", 3, select(toolbar(f), f = f))
        }
    }
    @Test fun normalWechatPagesAreRejectedEvenIfTheyContainAnAdLikeChatMessage() {
        for (activity in listOf(null, "com.tencent.mm.ui.LauncherUI", "com.tencent.mm.ui.chatting.ChattingUI",
            "com.tencent.mm.plugin.sns.ui.SnsTimeLineUI", "com.tencent.mm.plugin.webview.ui.tools.WebViewUI")) {
            assertNull(select(toolbar(), activity))
        }
    }
    @Test fun onlyKnownMiniProgramActivityFamilyIsAccepted() {
        for (activity in listOf("com.tencent.mm.plugin.appbrand.ui.AppBrandUI", mini,
            "com.tencent.mm.plugin.appbrand.ui.AppBrandUI4", "com.tencent.mm.plugin.appbrand.launching.AppBrandLaunchProxyUI")) {
            assertEquals(3, select(toolbar(), activity))
        }
        assertNull(select(toolbar(), "com.tencent.mm.plugin.appbrand.ui.AppBrandUIFakeView"))
    }
    @Test fun standaloneSkipWordOrMergedCapsuleCannotTrigger() {
        val nodes = toolbar().toMutableList()
        nodes[2] = nodes[2].copy(text = "通知")
        assertNull(select(nodes))
        nodes[2] = nodes[2].copy(text = "广告")
        nodes[3] = nodes[3].copy(text = "广告 跳过 5秒")
        assertNull(select(nodes))
    }
    @Test fun missingTimerStillWorksWhenControlsShareACompactToolbar() {
        assertEquals(3, select(toolbar().dropLast(1)))
    }
    @Test fun nearbyTimerCanConfirmHeaderWhenNoCompactParentIsExposed() {
        val nodes = toolbar().toMutableList()
        nodes[2] = nodes[2].copy(parent = 0)
        nodes[3] = nodes[3].copy(parent = 0)
        assertEquals(3, select(nodes))
        assertNull(select(nodes.dropLast(1)))
    }
    @Test fun unrelatedAdTextElsewhereOnThePageCannotConfirmSkip() {
        val nodes = toolbar().toMutableList()
        nodes[2] = nodes[2].copy(bounds = Box(60, 1200, 200, 1250))
        assertNull(select(nodes))
    }
    @Test fun rightOrBottomSkipDoesNotBelongToTheTopLeftRule() {
        for (b in listOf(Box(950, 180, 1080, 250), Box(280, 2200, 400, 2280))) {
            val nodes = toolbar().toMutableList();nodes[3] = nodes[3].copy(bounds = b)
            assertNull(select(nodes))
        }
    }
    @Test fun descriptiveOrCountdownTextCanIdentifyTheActualControl() {
        for (n in listOf(toolbar()[3].copy(text = null, description = "跳过"),
            toolbar()[3].copy(text = "跳过 (5秒)"))) {
            val nodes = toolbar().toMutableList();nodes[3] = n
            assertEquals(3, select(nodes))
        }
    }
    @Test fun invisibleAndAmbiguousTargetsAreRejected() {
        val nodes = toolbar().toMutableList();nodes[3] = nodes[3].copy(visible = false)
        assertNull(select(nodes))
        assertNull(select(toolbar() + toolbar()[3]))
    }
    @Test fun disabledButtonStillIdentifiesTheAdWithoutRenewingItsSession() {
        val nodes = toolbar().toMutableList();nodes[3] = nodes[3].copy(enabled = false)
        assertEquals(3, select(nodes)) // Adapter must not dispatch until enabled.
    }
}
