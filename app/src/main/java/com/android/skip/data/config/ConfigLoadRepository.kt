package com.android.skip.data.config

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.android.skip.R
import com.android.skip.dataclass.ConfigLoadSchema
import com.android.skip.dataclass.LoadSkipBound
import com.android.skip.dataclass.LoadSkipId
import com.android.skip.dataclass.LoadSkipText
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.StringUtils.getString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

data class SkipTarget(
    val rect: Rect, val matchKey: String, val actionNode: AccessibilityNodeInfo? = null
)

@Singleton
class ConfigLoadRepository @Inject constructor() {
    private var configLoadSchemaMap: Map<String, ConfigLoadSchema> = mutableMapOf()

    fun loadConfig(config: Map<String, ConfigLoadSchema>) {
        configLoadSchemaMap = config
    }

    suspend fun getTarget(
        rootNode: AccessibilityNodeInfo, activityName: String?
    ): SkipTarget? {
        val targetAppPackage = rootNode.packageName.toString()
        val configured = configLoadSchemaMap[targetAppPackage]
        val defaultSkipTexts = listOf(LoadSkipText(text = getString(R.string.settings_skip_keyword)))
        // Keep ordinary-mode behavior without modifying the stored configuration.
        // An explicitly empty text list still disables the text fallback for this app.
        val targetConfig = configured?.copy(skipTexts = configured.skipTexts ?: defaultSkipTexts)
            ?: ConfigLoadSchema(targetAppPackage, defaultSkipTexts)

        return withContext(Dispatchers.Main) {
            try {
                val skipByTextTasks =
                    createSkipByTextTasks(this, rootNode, targetConfig.skipTexts, activityName)
                val skipByIdTasks =
                    createSkipByIdTasks(this, rootNode, targetConfig.skipIds, activityName)
                val skipByBoundTasks =
                    createSkipByBoundTasks(this, rootNode, targetConfig.skipBounds, activityName)
                val tasks = skipByTextTasks + skipByIdTasks + skipByBoundTasks
                awaitFirstNonNullOrComplete(this, tasks)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LogUtils.e(e)
                null
            }
        }
    }

    private suspend fun <T> awaitFirstNonNullOrComplete(
        scope: CoroutineScope, tasks: List<Deferred<T?>>
    ): T? {
        val deferredResults = mutableListOf<Deferred<T?>>()

        tasks.forEach { deferred ->
            deferredResults.add(scope.async {
                val result = deferred.await()
                if (result != null) {
                    return@async result
                }
                null
            })
        }

        deferredResults.forEach { result ->
            val nonNullResult = result.await()
            if (nonNullResult != null) {
                return nonNullResult
            }
        }

        return null
    }

    private fun createSkipByTextTasks(
        scope: CoroutineScope,
        rootNode: AccessibilityNodeInfo,
        skipTexts: List<LoadSkipText>?,
        activityName: String?
    ): List<Deferred<SkipTarget?>> {
        val deferredResults = mutableListOf<Deferred<SkipTarget?>>()
        if (skipTexts.isNullOrEmpty()) return deferredResults

        for (skipText in skipTexts) {
            if (skipText.activityName != null && skipText.activityName != activityName) continue
            deferredResults.add(scope.async {
                val targetNode =
                    rootNode.findAccessibilityNodeInfosByText(skipText.text).firstOrNull { node ->
                        node.isVisibleToUser && node.isEnabled &&
                            (skipText.length == null ||
                                (node.text != null && node.text.length <= skipText.length))
                    }

                if (targetNode != null) {
                    if (skipText.click != null) {
                        SkipTarget(skipText.click, "text:${skipText.text}")
                    } else {
                        val rect = Rect()
                        targetNode.getBoundsInScreen(rect)
                        SkipTarget(rect, "text:${skipText.text}", targetNode)
                    }
                } else {
                    null
                }
            })
        }
        return deferredResults
    }

    private fun createSkipByIdTasks(
        scope: CoroutineScope,
        rootNode: AccessibilityNodeInfo,
        skipIds: List<LoadSkipId>?,
        activityName: String?
    ): List<Deferred<SkipTarget?>> {
        val deferredResults = mutableListOf<Deferred<SkipTarget?>>()
        if (skipIds.isNullOrEmpty()) return deferredResults

        for (skipId in skipIds) {
            if (skipId.activityName != null && skipId.activityName != activityName) continue
            deferredResults.add(scope.async {
                val foundNode = rootNode.findAccessibilityNodeInfosByViewId(skipId.id).firstOrNull { it.isVisibleToUser && it.isEnabled }

                if (foundNode != null) {
                    if (skipId.click != null) {
                        SkipTarget(skipId.click, "id:${skipId.id}")
                    } else {
                        val rect = Rect()
                        foundNode.getBoundsInScreen(rect)
                        SkipTarget(rect, "id:${skipId.id}", foundNode)
                    }
                } else {
                    null
                }
            })
        }
        return deferredResults
    }

    private fun createSkipByBoundTasks(
        scope: CoroutineScope,
        rootNode: AccessibilityNodeInfo,
        skipBounds: List<LoadSkipBound>?,
        activityName: String?
    ): List<Deferred<SkipTarget?>> {
        val deferredResults = mutableListOf<Deferred<SkipTarget?>>()
        if (skipBounds.isNullOrEmpty()) return deferredResults

        for (skipBound in skipBounds) {
            if (skipBound.activityName != null && skipBound.activityName != activityName) continue

            deferredResults.add(scope.async {
                val foundRect = traverseNode(rootNode, skipBound.bound)

                foundRect?.let { SkipTarget(skipBound.click ?: it, "bound:${skipBound.bound}") }
            })
        }
        return deferredResults
    }

    private fun traverseNode(
        rootNode: AccessibilityNodeInfo, targetRect: Rect
    ): Rect? {
        val queue: MutableList<AccessibilityNodeInfo> = mutableListOf(rootNode)

        while (queue.isNotEmpty()) {
            val node = queue.removeAt(0)
            val nodeRect = Rect()
            node.getBoundsInScreen(nodeRect)
            if (node.isVisibleToUser && node.isEnabled && abs(nodeRect.left - targetRect.left) <= 1 && abs(nodeRect.top - targetRect.top) <= 1 && abs(
                    nodeRect.right - targetRect.right
                ) <= 1 && abs(nodeRect.bottom - targetRect.bottom) <= 1
            ) {
                return nodeRect
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let {
                    queue.add(it)
                }
            }
        }
        return null
    }
}