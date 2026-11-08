package com.android.skip.data.config

import com.android.skip.data.config.PersonalSplashRules.Box
import com.android.skip.data.config.PersonalSplashRules.Node
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class PersonalSplashRulesTest {
    private val screen = Box(0, 0, 1200, 2670)
    // Approximate bounds from the supplied screenshots, not a live accessibility capture.
    private val coolapkButton = Box(1020, 356, 1140, 476)
    private val walletButton = Box(1008, 2544, 1152, 2624)
    private fun select(pkg: String, vararg nodes: Node) = PersonalSplashRules.select(pkg, screen, nodes.toList())

    @Test fun screenshotButtonsMatchTheirRespectiveCorners() {
        assertEquals(0, select(PersonalSplashRules.COOLAPK, Node(coolapkButton, text = "跳过")))
        assertEquals(0, select(PersonalSplashRules.WALLET, Node(walletButton, text = "跳过")))
        assertNull(select(PersonalSplashRules.WALLET, Node(coolapkButton, text = "跳过")))
        assertNull(select(PersonalSplashRules.COOLAPK, Node(walletButton, text = "跳过")))
    }
    @Test fun smallWalletButtonHasNoMinimumFontOrTouchSize() {
        assertEquals(0, select(PersonalSplashRules.WALLET, Node(Box(1080, 2590, 1100, 2602), description = "跳过按钮")))
    }
    @Test fun formattingAndCountdownAreAcceptedButSentencesAreNot() {
        for (text in listOf("跳 过", "跳过（5秒）", "5s | 跳过", "跳过广告", "跳过\u00a0(3S)")) {
            assertEquals(text, 0, select(PersonalSplashRules.WALLET, Node(walletButton, text = text)))
        }
        for (text in listOf("立即进入", "点击跳过此步骤", "如何跳过广告", "跳过支付确认", "不可跳过")) {
            assertNull(select(PersonalSplashRules.WALLET, Node(walletButton, text = text)))
        }
    }
    @Test fun SkipIdsAreMatchedAtTokenBoundaries() {
        for (id in listOf("com.mipay.wallet:id/skip", "sdk:id/tt_splash_skip_btn", "sdk:id/splashSkipButton")) {
            assertEquals(0, select(PersonalSplashRules.WALLET, Node(walletButton, viewId = id)))
        }
        assertNull(select(PersonalSplashRules.WALLET, Node(walletButton, viewId = "sdk:id/skipper")))
    }
    @Test fun HiddenDisabledEmptyOrOutOfWindowNodesAreRejected() {
        for (n in listOf(Node(walletButton, text = "跳过", visible = false),
            Node(walletButton, text = "跳过", enabled = false),
            Node(Box(1080, 2590, 1080, 2600), text = "跳过"),
            Node(Box(1080, 2590, 1250, 2620), text = "跳过"))) {
            assertNull(select(PersonalSplashRules.WALLET, n))
        }
    }
    @Test fun LargeAdAndArticleTextCannotBecomeSkipButtons() {
        assertNull(select(PersonalSplashRules.COOLAPK, Node(screen, text = "跳过")))
        assertNull(select(PersonalSplashRules.WALLET, Node(Box(50, 2000, 800, 2400), text = "跳过")))
    }
    @Test fun PackageScopeCannotLeakToOtherApps() {
        assertNull(select("com.tencent.mm", Node(walletButton, text = "跳过")))
    }
    private fun blankCircle(bounds: Box = coolapkButton, inAd: Boolean = true) = Node(bounds,
        className = "android.view.View", clickable = true, leaf = true, insideCoolapkSplashAd = inAd)

    @Test fun UnlabelledCircleRequiresActualCoolapkSplashAdContainer() {
        assertEquals(0, select(PersonalSplashRules.COOLAPK, blankCircle()))
        assertNull(select(PersonalSplashRules.COOLAPK, blankCircle(inAd = false)))
        assertNull(select(PersonalSplashRules.WALLET, blankCircle(walletButton)))
        assertNull(select(PersonalSplashRules.COOLAPK, blankCircle().copy(clickable = false)))
        assertNull(select(PersonalSplashRules.COOLAPK, blankCircle().copy(leaf = false)))
        assertNull(select(PersonalSplashRules.COOLAPK, blankCircle().copy(viewId = "sdk:id/ad_profile_icon")))
    }
    @Test fun AmbiguousUnlabelledControlsAreNotClicked() {
        assertNull(select(PersonalSplashRules.COOLAPK, blankCircle(), blankCircle(Box(1020, 550, 1140, 670))))
        assertNull(select(PersonalSplashRules.COOLAPK, blankCircle(Box(950, 350, 1170, 420))))
    }
    @Test fun SemanticButtonTakesPriorityOverBlankCircle() {
        assertEquals(1, select(PersonalSplashRules.COOLAPK, blankCircle(), Node(coolapkButton, text = "跳过")))
    }
    @Test fun PositionScalesWithWindowAndKeepsWindowOffset() {
        val scaled = Box(850, 297, 950, 397)
        assertEquals(0, PersonalSplashRules.select(PersonalSplashRules.COOLAPK, Box(0, 0, 1000, 2225), listOf(Node(scaled, text = "跳过"))))
        assertEquals(0, PersonalSplashRules.select(PersonalSplashRules.WALLET, Box(100, 200, 1300, 2870),
            listOf(Node(Box(1108, 2744, 1252, 2824), text = "跳过"))))
    }
    @Test fun UpstreamRealCaptureSelectsTheDocumentedUnlabelledSkipNode() {
        val json = javaClass.getResourceAsStream("/coolapk-upstream-splash.json")!!.bufferedReader().use {
            JsonParser.parseReader(it).asJsonObject
        }
        val raw = json["nodes"].asJsonArray.map { it.asJsonObject }
        val byId = raw.associateBy { it["nodeId"].asInt }
        val nodes = raw.map { n ->
            val ancestorIds = mutableListOf<String>()
            var current = n
            while (true) {
                current["viewIdResourceName"]?.asString?.let { ancestorIds.add(it) }
                val parent = current["parentId"].asInt
                if (parent == -1) break
                current = byId.getValue(parent)
            }
            Node(Box(n["left"].asInt, n["top"].asInt, n["right"].asInt, n["bottom"].asInt),
                text = n["text"]?.asString, viewId = n["viewIdResourceName"]?.asString,
                className = n["className"]?.asString, clickable = n["isClickable"].asBoolean,
                leaf = n["childCount"].asInt == 0,
                insideCoolapkSplashAd = "com.coolapk.market:id/main_splash_ad" in ancestorIds &&
                    "com.coolapk.market:id/ad_container" in ancestorIds)
        }
        val selected = PersonalSplashRules.select(PersonalSplashRules.COOLAPK,
            Box(0, 0, json["screenWidth"].asInt, json["screenHeight"].asInt), nodes)
        assertNotNull(selected)
        assertEquals(66, raw[selected!!]["nodeId"].asInt)
    }
}
