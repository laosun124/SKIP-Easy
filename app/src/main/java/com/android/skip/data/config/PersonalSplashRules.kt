package com.android.skip.data.config

/** Additional startup rules scoped to their matching application packages. */
internal object PersonalSplashRules {
    const val COOLAPK = "com.coolapk.market"
    const val WALLET = "com.mipay.wallet"

    data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width get() = right.toLong() - left
        val height get() = bottom.toLong() - top
        val centerX get() = (left.toDouble() + right) / 2
        val centerY get() = (top.toDouble() + bottom) / 2
    }

    data class Node(
        val bounds: Box,
        val text: String? = null,
        val description: String? = null,
        val viewId: String? = null,
        val className: String? = null,
        val clickable: Boolean = false,
        val leaf: Boolean = false,
        val insideCoolapkSplashAd: Boolean = false,
        val visible: Boolean = true,
        val enabled: Boolean = true
    )

    fun supports(packageName: String) = packageName == COOLAPK || packageName == WALLET

    private val skipText = Regex("^(?:\\d{1,2}[s秒]?)?跳过(?:广告)?(?:按钮)?(?:\\d{1,2}[s秒]?)?$")
    private fun isSkipText(value: String?): Boolean {
        val normalized = value.orEmpty().filterNot {
            it.isWhitespace() || Character.isSpaceChar(it) || it in "()（）|·:：\u200b"
        }.lowercase()
        return normalized.length <= 16 && skipText.matches(normalized)
    }

    private fun isSkipId(value: String?): Boolean {
        if (value == null || ":id/" !in value) return false
        val name = value.substringAfterLast('/').replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()
        return name.split('_').any { it == "skip" } || name in setOf("splashskip", "adskip", "skipad")
    }

    private fun inCorner(packageName: String, frame: Box, node: Node): Boolean {
        val b = node.bounds
        if (!supports(packageName) || !node.visible || !node.enabled || frame.width <= 0 || frame.height <= 0 ||
            b.width <= 0 || b.height <= 0 || b.left < frame.left || b.top < frame.top ||
            b.right > frame.right || b.bottom > frame.bottom ||
            b.width > frame.width * .25 || b.height > frame.height * .10
        ) return false
        val x = (b.centerX - frame.left) / frame.width
        val y = (b.centerY - frame.top) / frame.height
        return x >= .72 && when (packageName) {
            COOLAPK -> y in .02.. .30
            WALLET -> y in .84..1.0
            else -> false
        }
    }

    /** Return an index into the fresh tree; never synthesize a click coordinate. */
    fun select(packageName: String, frame: Box, nodes: List<Node>): Int? {
        val candidates = nodes.indices.filter { inCorner(packageName, frame, nodes[it]) }
        val semantic = candidates.mapNotNull { index ->
            val n = nodes[index]
            val rank = when {
                isSkipText(n.text) -> 0
                isSkipText(n.description) -> 1
                isSkipId(n.viewId) -> 2
                else -> return@mapNotNull null
            }
            index to rank
        }.minWithOrNull(compareBy<Pair<Int, Int>> { it.second }
            .thenBy { nodes[it.first].bounds.width * nodes[it.first].bounds.height })
        if (semantic != null) return semantic.first
        if (packageName != COOLAPK) return null
        // The upstream capture exposes a blank clickable View under main_splash_ad/ad_container.
        // shortcut: require that exact container structure; unknown SDK layouts need a new capture.
        return candidates.filter { index ->
            val n = nodes[index]
            val b = n.bounds
            n.insideCoolapkSplashAd && n.clickable && n.leaf && n.className == "android.view.View" &&
                n.text.isNullOrBlank() && n.description.isNullOrBlank() && n.viewId.isNullOrBlank() &&
                (b.centerX - frame.left) / frame.width in .82.. .98 &&
                b.width.toDouble() in (frame.width * .055)..(frame.width * .16) &&
                b.width.toDouble() / b.height in .8..1.25
        }.singleOrNull()
    }
}
