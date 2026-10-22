package com.android.skip.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Path
import android.graphics.Rect
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.android.skip.MyApp
import com.android.skip.R
import com.android.skip.data.config.ConfigLoadRepository
import com.android.skip.data.config.SkipTarget
import com.android.skip.ui.alive.notificationbar.NotificationBarRepository
import com.android.skip.ui.main.start.StartAccessibilityRepository
import com.android.skip.ui.settings.log.SkipLogRepository
import com.android.skip.ui.settings.tip.TipRepository
import com.android.skip.ui.whitelist.WhiteListRepository
import com.android.skip.util.AccessibilityState
import com.android.skip.util.MyToast
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ServiceUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class MyAccessibilityService : AccessibilityService() {
    private var appActivityName: String? = null
    private var isShowTip: Boolean = false
    private var scanTimes: Int = 0
    private val skipWindow = ForegroundSkipWindow(SystemClock::elapsedRealtime)
    private var lastTicket: ForegroundSkipWindow.Ticket? = null
    private var scanJob: Job? = null
    private var expiryJob: Job? = null
    private var screenReceiverRegistered = false
    private val powerManager by lazy { getSystemService(PowerManager::class.java) }
    private val keyguardManager by lazy { getSystemService(KeyguardManager::class.java) }
    private var homePackages: Set<String> = emptySet()

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) stopAutomaticClicks(leaveApp = true)
        }
    }

    private val attempts = SkipAttemptBudget(SystemClock::elapsedRealtime)
    private var tipShownForVisit = false
    private var gestureSequence = 0L
    private var pendingGesture: Long? = null
    // Keep session state and duplicate-click bookkeeping on the same thread as events.
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    @Inject
    lateinit var startAccessibilityRepository: StartAccessibilityRepository

    @Inject
    lateinit var configLoadRepository: ConfigLoadRepository

    @Inject
    lateinit var notificationBarRepository: NotificationBarRepository

    @Inject
    lateinit var tipRepository: TipRepository

    @Inject
    lateinit var skipLogRepository: SkipLogRepository

    @Inject
    lateinit var whiteListRepository: WhiteListRepository

    private val notificationBarObserver = Observer<Boolean> { enabled ->
        if (enabled) {
            ServiceUtils.startService(MyForegroundService::class.java)
        } else {
            ServiceUtils.stopService(MyForegroundService::class.java)
        }
    }

    private val tipObserver = Observer<Boolean> { isShowTip = it }


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            val rootNode = rootInActiveWindow
            val ticket = updateForegroundWindow(event)
            rootNode ?: return

            val rootNodePackageName = rootNode.packageName.toString()
            if (ticket != null && ticket.visitId != lastTicket?.visitId) {
                scanTimes = 0
                attempts.clear()
                tipShownForVisit = false
                appActivityName = null
                expiryJob?.cancel()
                expiryJob = serviceScope.launch {
                    delay(ForegroundSkipWindow.DURATION_MS)
                    scanJob?.cancel()
                }
            }
            if (ticket != lastTicket) scanJob?.cancel()
            if (ticket != null) lastTicket = ticket

            getActivityName(event)?.let {
                if (event?.packageName?.toString() == rootNodePackageName) {
                    if (appActivityName != null && appActivityName != it) attempts.stop()
                    appActivityName = it
                }
            }

            if (ticket != null && ticket.packageName == rootNodePackageName &&
                ticket.windowId == rootNode.windowId && skipWindow.canClick(ticket) &&
                !whiteListRepository.isAppInWhiteList(rootNodePackageName) &&
                scanTimes < 50 && !attempts.isFinished
            ) {
                if (scanJob?.isActive != true) startScanning(ticket)
            }


        } catch (e: Exception) {
            LogUtils.e(e)
            serviceScope.launch {
                skipLogRepository.appendExceptionLog(
                    level = "E",
                    source = "MyAccessibilityService#onAccessibilityEvent",
                    message = e.message ?: "onAccessibilityEvent exception",
                    throwable = null
                )
            }
        }
    }

    private fun startScanning(ticket: ForegroundSkipWindow.Ticket) {
        scanJob = serviceScope.launch {
            try {
                // Poll fresh nodes: an early tap may be ignored until the ad enables its button.
                while (skipWindow.canClick(ticket) && scanTimes < 50 && !attempts.isFinished) {
                    if (!canStillClick(ticket)) break
                    if (pendingGesture == null) {
                        val root = rootInActiveWindow ?: break
                        if (root.packageName?.toString() != ticket.packageName || root.windowId != ticket.windowId) break
                        scanTimes++
                        val target = configLoadRepository.getTarget(root, appActivityName)
                        ensureActive()
                        if (canStillClick(ticket)) {
                            val bounds = Rect()
                            root.getBoundsInScreen(bounds)
                            val visibleTarget = target?.takeIf {
                                bounds.contains(it.rect.centerX(), it.rect.centerY())
                            }
                            // Include the window and matched rule/node, not just the coordinates:
                            // a replacement control at the old position must not inherit retries.
                            val key = visibleTarget?.let {
                                "${ticket.packageName}/${ticket.windowId}/${it.matchKey}/" +
                                    "${it.actionNode?.viewIdResourceName}/${it.actionNode?.className}/${it.rect}"
                            }
                            val attempt = attempts.take(key, skipWindow.canClick(ticket))
                            if (attempt != null && visibleTarget != null) attemptClick(visibleTarget, ticket, attempt)
                        }
                    }
                    delay(300)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                skipLogRepository.appendExceptionLog("E", "SkipScan", e.message ?: "scan failed", e)
            }
        }
    }

    private fun attemptClick(target: SkipTarget, ticket: ForegroundSkipWindow.Ticket, attempt: Int) {
        val node = clickableNode(target)
        // Prefer the actual button when exposed by accessibility. A still-visible button
        // can fall back to a gesture on a later, freshly matched attempt.
        if (attempt == 1 && node != null && node.refresh() && node.isVisibleToUser &&
            node.isEnabled && node.isClickable && canStillClick(ticket) &&
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        ) {
            showAttemptTip(ticket)
            return
        }
        if (canStillClick(ticket)) click(this, target.rect, ticket.packageName, target.rect.toString(), ticket)
    }

    private fun clickableNode(target: SkipTarget): AccessibilityNodeInfo? {
        var node = target.actionNode ?: return null
        val targetArea = target.rect.width().toLong() * target.rect.height()
        // Text often lives inside a clickable button. Never climb into a whole ad/card.
        repeat(3) {
            if (!node.refresh() || !node.isVisibleToUser || !node.isEnabled) return null
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            val area = bounds.width().toLong() * bounds.height()
            if (node.isClickable && targetArea > 0 && bounds.contains(target.rect) &&
                area <= targetArea * 4
            ) return node
            node = node.parent ?: return null
        }
        return null
    }

    private fun showAttemptTip(ticket: ForegroundSkipWindow.Ticket) {
        // Dispatch/performAction success only means the action was accepted, not that the ad closed.
        if (isShowTip && !tipShownForVisit && skipWindow.canClick(ticket)) {
            tipShownForVisit = true
            MyToast.show(R.string.toast_skip_tip)
        }
    }

    /** System overlays interrupt clicks without making an in-app page a new launch. */
    private fun updateForegroundWindow(event: AccessibilityEvent?): ForegroundSkipWindow.Ticket? {
        if (!powerManager.isInteractive || keyguardManager.isKeyguardLocked) {
            stopAutomaticClicks(leaveApp = true)
            return null
        }

        // Some Android versions host Recents in SystemUI instead of the home launcher.
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.packageName?.toString() == "com.android.systemui" &&
            event.className?.toString()?.contains("RecentsActivity") == true
        ) {
            stopAutomaticClicks(leaveApp = true)
            return null
        }

        val windowList = windows
        val foreground = windowList.firstOrNull { it.isFocused }
            ?: windowList.firstOrNull { it.isActive }
        val foregroundPackage = foreground?.root?.packageName?.toString()
        if (foregroundPackage != null && foregroundPackage in homePackages) {
            stopAutomaticClicks(leaveApp = true)
            return null
        }
        val keyboardPackage = Settings.Secure.getString(
            contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD
        )?.substringBefore('/')
        if (foreground == null || foreground.type != AccessibilityWindowInfo.TYPE_APPLICATION ||
            foregroundPackage == null || foregroundPackage == keyboardPackage ||
            foregroundPackage in TRANSIENT_PACKAGES
        ) {
            stopAutomaticClicks()
            return null
        }
        return skipWindow.showApp(foregroundPackage, foreground.id)
    }

    private fun canStillClick(ticket: ForegroundSkipWindow.Ticket): Boolean {
        // Re-read windows: queued results must not land on another app, page or overlay.
        if (updateForegroundWindow(null) != ticket) return false
        if (whiteListRepository.isAppInWhiteList(ticket.packageName)) return false
        val currentRoot = rootInActiveWindow ?: return false
        return currentRoot.packageName?.toString() == ticket.packageName &&
            currentRoot.windowId == ticket.windowId && skipWindow.canClick(ticket)
    }

    private fun stopAutomaticClicks(leaveApp: Boolean = false) {
        scanJob?.cancel()
        pendingGesture = null
        if (leaveApp) {
            skipWindow.leaveApp()
            expiryJob?.cancel()
        } else {
            skipWindow.suspendClicks()
        }
    }

    private fun click(
        accessibilityService: AccessibilityService,
        rect: Rect,
        packageName: String,
        rectStr: String,
        ticket: ForegroundSkipWindow.Ticket
    ): Boolean {
        val path = Path()
        path.moveTo(rect.centerX().toFloat(), rect.centerY().toFloat())

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()

        // Window queries can block; check the deadline again immediately before dispatch.
        if (!skipWindow.canClick(ticket)) return false
        val gestureId = ++gestureSequence
        pendingGesture = gestureId
        val dispatched = accessibilityService.dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    super.onCompleted(gestureDescription)
                    if (pendingGesture == gestureId) {
                        pendingGesture = null
                        showAttemptTip(ticket)
                    }
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    super.onCancelled(gestureDescription)
                    if (pendingGesture == gestureId) pendingGesture = null
                    val message = "gesture cancelled: package=$packageName rect=$rectStr"
                    LogUtils.e(message)
                }
            },
            null
        )
        if (!dispatched && pendingGesture == gestureId) pendingGesture = null
        return dispatched
    }

    override fun onInterrupt() {
        stopAutomaticClicks()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        homePackages = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY
        ).map { it.activityInfo.packageName }.toSet()
        if (!screenReceiverRegistered) {
            ContextCompat.registerReceiver(
                this, screenReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            screenReceiverRegistered = true
        }
        startAccessibilityRepository.changeAccessibilityState(AccessibilityState.STARTED)

        notificationBarRepository.enable.observeForever(notificationBarObserver)
        tipRepository.enable.observeForever(tipObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAutomaticClicks(leaveApp = true)
        if (screenReceiverRegistered) {
            unregisterReceiver(screenReceiver)
            screenReceiverRegistered = false
        }
        serviceScope.cancel()
        startAccessibilityRepository.changeAccessibilityState(AccessibilityState.STOPPED)

        notificationBarRepository.enable.removeObserver(notificationBarObserver)
        tipRepository.enable.removeObserver(tipObserver)

        if (ServiceUtils.isServiceRunning(MyForegroundService::class.java)) {
            ServiceUtils.stopService(MyForegroundService::class.java)
        }
    }

    private companion object {
        val TRANSIENT_PACKAGES = setOf(
            "android", "com.android.systemui", "com.android.permissioncontroller",
            "com.google.android.permissioncontroller", "com.android.packageinstaller",
            "com.google.android.packageinstaller"
        )
    }

    private fun isSystemClass(className: String): Boolean {
        return try {
            val clazz = Class.forName(className)
            val packageName = clazz.getPackage()?.name
            packageName != null && (packageName.startsWith("android") || packageName.startsWith("androidx"))
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    private fun getActivityName(event: AccessibilityEvent?): String? {
        event ?: return null
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return null
        val className = event.className

        className ?: return null
        val classNameStr = className.toString()

        if (isSystemClass(classNameStr)) return null
        val eventPackage = event.packageName?.toString() ?: return null
        return try {
            packageManager.getActivityInfo(ComponentName(eventPackage, classNameStr), 0).name
        } catch (_: PackageManager.NameNotFoundException) {
            // Custom view / dialog classes are not Activity names for rule matching.
            null
        }
    }

}
