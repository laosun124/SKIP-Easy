package com.android.skip.data.config

import com.android.skip.data.config.PersonalSplashRules.Box
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** WeChat must never use the generic text-only skip rule, even when not whitelisted. */
internal object WechatAdRules {
    const val PACKAGE = "com.tencent.mm"
    private val miniActivity = Regex("com\\.tencent\\.mm\\.plugin\\.appbrand\\.ui\\.AppBrandUI\\d*")
    fun isMiniProgramActivity(name: String?) = name != null &&
        (miniActivity.matches(name) || name == "com.tencent.mm.plugin.appbrand.launching.AppBrandLaunchProxyUI")

    data class Node(val bounds: Box, val text: String? = null, val description: String? = null,
                    val parent: Int = -1, val visible: Boolean = true, val enabled: Boolean = true)
    private fun normalized(text: String?) = text.orEmpty().filterNot {
        it.isWhitespace() || Character.isSpaceChar(it) || it in "()（）|\u200b"
    }.lowercase()
    private val skip = Regex("^跳过(?:\\d{1,2}[秒s]?)?$")
    private val countdown = Regex("^(?:剩余)?\\d{1,2}[秒s]$")
    private fun labels(n: Node) = listOf(normalized(n.text), normalized(n.description))

    fun select(activity: String?, frame: Box, nodes: List<Node>): Int? {
        if (!isMiniProgramActivity(activity) || frame.width <= 0 || frame.height <= 0) return null
        val shortSide = min(frame.width, frame.height).toDouble()
        // Use the current window's short side and height: tall, wide and offset windows share no pixel coordinates.
        val band = min(frame.height * .35, shortSide * .42)
        fun inHeader(n: Node, leftOnly: Boolean): Boolean {
            val b = n.bounds
            return n.visible && b.width > 0 && b.height > 0 &&
                b.left >= frame.left && b.top >= frame.top && b.right <= frame.right && b.bottom <= frame.bottom &&
                b.centerY - frame.top <= band && b.height <= band &&
                (!leftOnly || b.centerX - frame.left <= frame.width * .5)
        }
        fun sameRow(a: Box, b: Box) = abs(a.centerY - b.centerY) <=
            max(max(a.height, b.height) * .7, shortSide * .035)
        fun compactCommonParent(a: Int, b: Int): Boolean {
            val ancestors = mutableSetOf<Int>()
            var at = nodes[a].parent
            repeat(5) {
                if (at !in nodes.indices) return@repeat
                ancestors.add(at)
                at = nodes[at].parent
            }
            at = nodes[b].parent
            repeat(5) {
                if (at !in nodes.indices) return@repeat
                val n = nodes[at]
                if (at in ancestors && inHeader(n, true) && n.bounds.height <= band &&
                    n.bounds.width <= frame.width * .75) return true
                at = n.parent
            }
            return false
        }
        val header = nodes.indices.filter { inHeader(nodes[it], true) }
        val adLabels = header.filter { "广告" in labels(nodes[it]) }
        val matches = header.filter { index ->
            val n = nodes[index]
            val b = n.bounds
            if (b.width > frame.width * .25 || !labels(n).any { skip.matches(it) }) return@filter false
            adLabels.any { ad ->
                val a = nodes[ad].bounds
                if (a.right > b.left || b.left - a.right > shortSide * .25 || !sameRow(a, b)) return@any false
                val nearbyTimer = nodes.any { timer ->
                    inHeader(timer, false) && labels(timer).any { countdown.matches(it) } &&
                        timer.bounds.left >= b.right && timer.bounds.left - b.right <= shortSide * .35 &&
                        sameRow(b, timer.bounds)
                }
                compactCommonParent(index, ad) || nearbyTimer || labels(n).any { it.any(Char::isDigit) }
            }
        }
        return matches.singleOrNull()
    }
}
