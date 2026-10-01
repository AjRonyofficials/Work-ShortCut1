package com.example.service

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/**
 * Ultra High-Speed Accessibility Service for:
 * 1. Facebook Lite / Lite 96 / Lite F internal storage clear automation:
 *    - Auto-selects ALL checkboxes including "Accounts and settings" & "Clear All"
 *    - Auto-clicks "CLEAR"
 *    - Auto-confirms "OK"
 *    - Auto-closes app & settings completely
 * 2. Standard Android app zero-touch Clear Data & Auto-Close
 * 3. Instant Zero-Touch Auto Force-Close (on app long-press)
 */
class AutoCleanAccessibilityService : AccessibilityService() {

    companion object {
        const val MODE_AUTO_CLEAN = 0
        const val MODE_FORCE_CLOSE = 1

        var instance: AutoCleanAccessibilityService? = null
            private set

        var targetPackage: String? = null
            private set

        var targetAppName: String = "App"
            private set

        var currentMode: Int = MODE_AUTO_CLEAN
            private set

        var isAutomating: Boolean = false
            private set

        const val LITE_STEP_IDLE = 0
        const val LITE_STEP_SELECTING_ACCOUNTS = 1
        const val LITE_STEP_WAIT_ACCOUNTS_POPUP = 2
        const val LITE_STEP_CLICK_CLEAR = 3
        const val LITE_STEP_FINAL_CONFIRM = 4
        const val LITE_STEP_DONE = 5

        var isTargetLiteMode: Boolean = false
            private set

        var liteStep: Int = LITE_STEP_IDLE
            private set

        private var step: Int = 0
        private var lastActionTime: Long = 0L
        private var clickedClearCache: Boolean = false
        private var clickedClearData: Boolean = false
        private var handledLiteCheckboxes: Boolean = false

        fun isServiceRunning(): Boolean = instance != null

        /**
         * Clears cache & data, then immediately force-closes and returns to screen!
         */
        fun startAutoClean(
            context: Context,
            packageName: String,
            appName: String = "App",
            isLiteMode: Boolean = false
        ) {
            targetPackage = packageName
            targetAppName = appName
            currentMode = MODE_AUTO_CLEAN
            isAutomating = true
            isTargetLiteMode = isLiteMode
            step = 0
            liteStep = LITE_STEP_IDLE
            clickedClearCache = false
            clickedClearData = false
            handledLiteCheckboxes = false
            lastActionTime = System.currentTimeMillis()

            if (instance == null) {
                Toast.makeText(
                    context,
                    "Enable 'Work Shortcut' in Accessibility for 100% zero-tap auto cleaner!",
                    Toast.LENGTH_LONG
                ).show()

                try {
                    val accIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(accIntent)
                } catch (_: Exception) {
                    openAppDetailsFallback(context, packageName)
                }
                return
            }

            openAppDetailsFallback(context, packageName)
        }

        /**
         * Instantly force-stops target app and closes settings screen!
         * Triggered when long-pressing an app or clicking close.
         */
        fun startAutoForceClose(context: Context, packageName: String, appName: String = "App") {
            targetPackage = packageName
            targetAppName = appName
            currentMode = MODE_FORCE_CLOSE
            isAutomating = true
            step = 0
            lastActionTime = System.currentTimeMillis()

            // Immediate OS background kill
            try {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                am?.killBackgroundProcesses(packageName)
            } catch (_: Exception) {}

            // Shell kill attempt
            try {
                Runtime.getRuntime().exec(arrayOf("am", "force-stop", packageName))
            } catch (_: Exception) {}

            if (instance == null) {
                Toast.makeText(context, "$appName closed ✓ (Enable Accessibility for deep force-stop)", Toast.LENGTH_SHORT).show()
                openAppDetailsFallback(context, packageName)
                return
            }

            openAppDetailsFallback(context, packageName)
        }

        private fun openAppDetailsFallback(context: Context, packageName: String) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening settings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isAutomating = false
    }

    override fun onInterrupt() {
        isAutomating = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isAutomating) return
        val currentTarget = targetPackage ?: return

        // 12s safety timeout
        if (System.currentTimeMillis() - lastActionTime > 12000) {
            isAutomating = false
            targetPackage = null
            return
        }

        val rootNode = rootInActiveWindow ?: return
        val pkg = (event?.packageName?.toString() ?: "").lowercase()

        val isTargetPkg = currentTarget.isNotEmpty() && pkg.contains(currentTarget.lowercase())
        val isLiteScreen = isLiteStorageScreen(rootNode)
        val isLite = pkg.contains("lite") || pkg.contains("facebook") || pkg.contains("katana")
        val isSettings = pkg.contains("settings") || pkg.contains("packageinstaller") || pkg.contains("systemui") || pkg.isEmpty()

        // If Facebook Lite storage screen or its confirmation popup is active, handle it immediately!
        if (isLiteScreen) {
            handleLiteStorageScreenFlow(rootNode)
            return
        }

        if (isSettings || isLite || isTargetPkg || isTargetLiteMode) {
            if (currentMode == MODE_FORCE_CLOSE) {
                handleForceCloseStep(rootNode)
            } else {
                handleAutoCleanStep(rootNode)
            }
        }
    }

    /**
     * Checks if current screen is the Facebook Lite / Lite 96 / Lite F internal storage screen
     * "Clear Storage on Your Phone"
     */
    private fun isLiteStorageScreen(rootNode: AccessibilityNodeInfo): Boolean {
        if (liteStep in LITE_STEP_SELECTING_ACCOUNTS..LITE_STEP_FINAL_CONFIRM) {
            return true
        }
        val keywords = listOf(
            "facebook lite storage",
            "clear storage on your phone",
            "accounts and settings",
            "accounts and setting",
            "photo cache",
            "video cache",
            "other cache",
            "remove unnecessary app files to save space",
            "not recommended"
        )
        return findNodeByKeywords(rootNode, keywords) != null
    }

    /**
     * Instant Force Close Automation:
     * 1. In App Details, click "Force Stop"
     * 2. Click "OK" on confirmation dialog
     * 3. Press BACK/HOME immediately to close settings!
     */
    private fun handleForceCloseStep(rootNode: AccessibilityNodeInfo) {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < 200) return

        if (step == 0) {
            val forceStopBtn = findNodeByKeywords(
                rootNode,
                listOf(
                    "force stop",
                    "force close",
                    "থামিয়ে দিন",
                    "জোরপূর্বক বন্ধ করুন",
                    "বাধ্যতামূলক বন্ধ"
                ),
                resourceIds = listOf(
                    "com.android.settings:id/force_stop_button",
                    "com.android.settings:id/button2",
                    "com.android.settings:id/right_button"
                )
            )

            if (forceStopBtn != null && forceStopBtn.isEnabled) {
                clickNode(forceStopBtn)
                step = 1
                lastActionTime = now
                return
            } else {
                finishAndCloseSettings("Already closed! ✓")
                return
            }
        }

        if (step == 1) {
            val confirmBtn = findNodeByKeywords(
                rootNode,
                listOf("ok", "force stop", "ঠিক আছে", "yes", "confirm", "থামান"),
                resourceIds = listOf("android:id/button1", "com.android.settings:id/button1")
            )

            if (confirmBtn != null && confirmBtn.isEnabled) {
                clickNode(confirmBtn)
                step = 2
                lastActionTime = now
                finishAndCloseSettings("$targetAppName Force Closed! ✓")
                return
            } else {
                if (now - lastActionTime > 400) {
                    finishAndCloseSettings("$targetAppName Force Closed! ✓")
                }
            }
        }
    }

    private fun handleAutoCleanStep(rootNode: AccessibilityNodeInfo) {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < 250) return

        // SPECIAL CASE: Facebook Lite / Lite 96 / Lite F internal storage screen
        if (isLiteStorageScreen(rootNode)) {
            handleLiteStorageScreenFlow(rootNode)
            return
        }

        // Step 0: In App Details screen, find and click "Storage"
        if (step == 0) {
            val storageNode = findNodeByKeywords(
                rootNode,
                listOf(
                    "storage & cache",
                    "storage and cache",
                    "storage",
                    "internal storage",
                    "স্টোরেজ",
                    "স্টোরেজ ও ক্যাশ"
                )
            )

            if (storageNode != null) {
                val clicked = clickNode(storageNode)
                if (clicked) {
                    step = 1
                    lastActionTime = now
                    return
                }
            } else {
                try {
                    rootNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                } catch (_: Exception) {}
            }
        }

        // Step 1: In Storage screen, click "Clear cache" and then "Clear data" / "Manage space"
        if (step in 1..2) {
            if (!clickedClearCache) {
                val clearCacheNode = findNodeByKeywords(
                    rootNode,
                    listOf("clear cache", "ক্যাশ মুছুন", "ক্যাশে মুছুন", "ক্লিয়ার ক্যাশ")
                )
                if (clearCacheNode != null && clearCacheNode.isEnabled) {
                    clickNode(clearCacheNode)
                    clickedClearCache = true
                    lastActionTime = now
                }
            }

            val clearDataNode = findNodeByKeywords(
                rootNode,
                listOf(
                    "clear data",
                    "clear storage",
                    "manage space",
                    "manage storage",
                    "delete data",
                    "ডেটা মুছুন",
                    "ডাটা মুছুন",
                    "স্টোরেজ মুছুন"
                )
            )

            if (clearDataNode != null && clearDataNode.isEnabled) {
                clickNode(clearDataNode)
                clickedClearData = true
                step = 3
                lastActionTime = now
                return
            } else if (clickedClearCache) {
                step = 3
                lastActionTime = now
                return
            }
        }

        // Step 3: Handle Confirmation Dialog (Delete / OK / Confirm)
        if (step == 3) {
            val confirmNode = findNodeByKeywords(
                rootNode,
                listOf(
                    "delete",
                    "ok",
                    "clear",
                    "confirm",
                    "মুছুন",
                    "ঠিক আছে",
                    "হ্যাঁ",
                    "yes"
                ),
                resourceIds = listOf(
                    "android:id/button1",
                    "com.android.settings:id/button1"
                )
            )

            if (confirmNode != null && confirmNode.isEnabled) {
                clickNode(confirmNode)
                step = 4
                lastActionTime = now
                autoCloseCleanedSequence()
                return
            } else {
                if (now - lastActionTime > 500) {
                    step = 4
                    autoCloseCleanedSequence()
                }
            }
        }
    }

    /**
     * Dedicated Handler for Facebook Lite, Lite 96, Lite F "Clear Storage on Your Phone":
     * 1. FIRST ensures "Accounts and settings" checkbox is checked.
     * 2. When the confirmation popup appears, automatically clicks "OK".
     * 3. THEN clicks the blue "CLEAR" button to wipe all data.
     * 4. Confirms any final dialog and auto-closes the app completely!
     */
    private fun handleLiteStorageScreenFlow(rootNode: AccessibilityNodeInfo) {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < 160) return

        // 1. Check if positive dialog button (OK / Confirm) is currently visible on screen
        val okDialogBtn = findLiteOkDialogButton(rootNode)
        if (okDialogBtn != null && okDialogBtn.isEnabled) {
            clickNode(okDialogBtn)
            lastActionTime = now

            if (liteStep == LITE_STEP_FINAL_CONFIRM) {
                liteStep = LITE_STEP_DONE
                autoCloseCleanedSequence()
            } else {
                liteStep = LITE_STEP_CLICK_CLEAR
                // Wait briefly for dialog to disappear and main screen to be active
                mainHandler.postDelayed({
                    rootInActiveWindow?.let { refreshedRoot ->
                        handleLiteStorageScreenFlow(refreshedRoot)
                    }
                }, 280)
            }
            return
        }

        // 2. We are on Facebook Lite Storage screen.
        // First priority: Check "Accounts and settings" checkbox!
        val accountsRow = findAccountsAndSettingsRow(rootNode)
        if (accountsRow != null && !accountsRow.isChecked && liteStep < LITE_STEP_CLICK_CLEAR) {
            // Also ensure all cache checkboxes ("Clear All") are checked
            ensureClearAllChecked(rootNode)

            // Click Accounts and Settings to trigger the confirmation OK popup
            liteStep = LITE_STEP_WAIT_ACCOUNTS_POPUP
            lastActionTime = now
            clickNode(accountsRow.clickableTarget)

            // Check for the OK popup dialog shortly after tapping
            mainHandler.postDelayed({
                rootInActiveWindow?.let { refreshedRoot ->
                    val popupOk = findLiteOkDialogButton(refreshedRoot)
                    if (popupOk != null && popupOk.isEnabled) {
                        clickNode(popupOk)
                        liteStep = LITE_STEP_CLICK_CLEAR
                        lastActionTime = System.currentTimeMillis()
                        mainHandler.postDelayed({
                            rootInActiveWindow?.let { rootAfterOk ->
                                handleLiteStorageScreenFlow(rootAfterOk)
                            }
                        }, 280)
                    } else {
                        val refreshedRow = findAccountsAndSettingsRow(refreshedRoot)
                        if (refreshedRow?.isChecked == true || liteStep >= LITE_STEP_CLICK_CLEAR) {
                            clickClearButtonAndFinish(refreshedRoot)
                        }
                    }
                }
            }, 300)
            return
        }

        // 3. "Accounts and settings" is checked (or popup was accepted) -> Click "CLEAR"
        clickClearButtonAndFinish(rootNode)
    }

    private fun clickClearButtonAndFinish(rootNode: AccessibilityNodeInfo) {
        val now = System.currentTimeMillis()
        val clearBtn = findLiteClearButton(rootNode)

        if (clearBtn != null && clearBtn.isEnabled) {
            clickNode(clearBtn)
            liteStep = LITE_STEP_FINAL_CONFIRM
            lastActionTime = now

            mainHandler.postDelayed({
                rootInActiveWindow?.let { refreshedRoot ->
                    val finalOk = findLiteOkDialogButton(refreshedRoot)
                    if (finalOk != null && finalOk.isEnabled) {
                        clickNode(finalOk)
                    }
                }
                autoCloseCleanedSequence()
            }, 350)
        } else {
            val finalOk = findLiteOkDialogButton(rootNode)
            if (finalOk != null && finalOk.isEnabled) {
                clickNode(finalOk)
                autoCloseCleanedSequence()
            } else if (now - lastActionTime > 600) {
                autoCloseCleanedSequence()
            }
        }
    }

    data class AccountsRowInfo(
        val textNode: AccessibilityNodeInfo,
        val checkboxNode: AccessibilityNodeInfo?,
        val clickableTarget: AccessibilityNodeInfo,
        val isChecked: Boolean
    )

    private fun findAccountsAndSettingsRow(root: AccessibilityNodeInfo): AccountsRowInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = (node.text?.toString() ?: "") + " " + (node.contentDescription?.toString() ?: "")
            val lower = text.lowercase()

            if (lower.contains("accounts and setting") || lower.contains("অ্যাকাউন্ট এবং সেটিংস") || lower.contains("not recommended")) {
                var checkableNode: AccessibilityNodeInfo? = null
                var isChecked = false
                var clickableTarget: AccessibilityNodeInfo = node

                if (node.isCheckable) {
                    checkableNode = node
                    isChecked = node.isChecked
                }

                val parent = node.parent
                if (parent != null) {
                    if (parent.isClickable) clickableTarget = parent
                    if (parent.isCheckable) {
                        checkableNode = parent
                        isChecked = parent.isChecked
                    }
                    for (i in 0 until parent.childCount) {
                        val sibling = parent.getChild(i)
                        if (sibling != null && sibling.isCheckable) {
                            checkableNode = sibling
                            isChecked = sibling.isChecked
                            if (sibling.isClickable) clickableTarget = sibling
                            break
                        }
                    }
                }

                return AccountsRowInfo(
                    textNode = node,
                    checkboxNode = checkableNode,
                    clickableTarget = checkableNode ?: clickableTarget,
                    isChecked = isChecked
                )
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun ensureClearAllChecked(root: AccessibilityNodeInfo) {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = (node.text?.toString() ?: "").lowercase()
            if (text.contains("clear all") || text.contains("photo cache") ||
                text.contains("video cache") || text.contains("other cache")) {
                if (node.isCheckable && !node.isChecked) {
                    clickNode(node)
                }
                node.parent?.let { parent ->
                    for (i in 0 until parent.childCount) {
                        val child = parent.getChild(i)
                        if (child != null && child.isCheckable && !child.isChecked) {
                            clickNode(child)
                        }
                    }
                }
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
    }

    private fun findLiteOkDialogButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        val candidates = mutableListOf<AccessibilityNodeInfo>()

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = (node.text?.toString() ?: "").trim()
            val desc = (node.contentDescription?.toString() ?: "").trim()
            val lower = text.lowercase()
            val lowerDesc = desc.lowercase()
            val viewId = (node.viewIdResourceName ?: "").lowercase()

            val isCancel = lower == "cancel" || lower == "বাতিল" || lower == "না" || lower == "no"
            if (!isCancel && node.isEnabled) {
                if (lower == "ok" || lower == "okay" || lower == "confirm" ||
                    lower == "ঠিক আছে" || lower == "yes" || lower == "হ্যাঁ" ||
                    lowerDesc == "ok" || lowerDesc == "confirm") {
                    candidates.add(node)
                } else if (viewId.endsWith(":id/button1") || viewId.endsWith(":id/confirm") || viewId.endsWith(":id/ok")) {
                    candidates.add(node)
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }

        return candidates.firstOrNull { (it.text?.toString() ?: "").trim().equals("ok", ignoreCase = true) }
            ?: candidates.firstOrNull()
    }

    private fun findLiteClearButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = (node.text?.toString() ?: "").trim()
            val desc = (node.contentDescription?.toString() ?: "").trim()

            val isExactClear = text.equals("CLEAR", ignoreCase = true) ||
                    text.equals("Clear", ignoreCase = true) ||
                    text.equals("মুছুন") ||
                    desc.equals("CLEAR", ignoreCase = true)

            val isNotOtherClear = !text.contains("All", ignoreCase = true) &&
                    !text.contains("Phone", ignoreCase = true) &&
                    !text.contains("Cache", ignoreCase = true) &&
                    !text.contains("Storage", ignoreCase = true) &&
                    !text.contains("Accounts", ignoreCase = true)

            if (isExactClear && isNotOtherClear && node.isEnabled) {
                return node
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    /**
     * Automatically closes settings and terminates the app after data is cleared!
     */
    private fun autoCloseCleanedSequence() {
        val pkgToKill = targetPackage
        mainHandler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_BACK)
            mainHandler.postDelayed({
                // In App Info screen, attempt to Force Stop the app as well
                try {
                    rootInActiveWindow?.let { root ->
                        val forceStop = findNodeByKeywords(root, listOf("force stop", "force close", "থামিয়ে দিন"))
                        if (forceStop != null && forceStop.isEnabled) {
                            clickNode(forceStop)
                        }
                    }
                } catch (_: Exception) {}

                performGlobalAction(GLOBAL_ACTION_BACK)
                mainHandler.postDelayed({
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    pkgToKill?.let { pkg ->
                        try {
                            val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                            am?.killBackgroundProcesses(pkg)
                            Runtime.getRuntime().exec(arrayOf("am", "force-stop", pkg))
                        } catch (_: Exception) {}
                    }
                    Toast.makeText(applicationContext, "$targetAppName Data Cleared & Auto-Closed! ✓", Toast.LENGTH_SHORT).show()
                    isAutomating = false
                    targetPackage = null
                    step = 0
                    liteStep = LITE_STEP_IDLE
                    isTargetLiteMode = false
                }, 180)
            }, 180)
        }, 220)
    }

    private fun finishAndCloseSettings(message: String) {
        val pkgToKill = targetPackage
        mainHandler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_BACK)
            mainHandler.postDelayed({
                performGlobalAction(GLOBAL_ACTION_HOME)
                pkgToKill?.let { pkg ->
                    try {
                        val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                        am?.killBackgroundProcesses(pkg)
                        Runtime.getRuntime().exec(arrayOf("am", "force-stop", pkg))
                    } catch (_: Exception) {}
                }
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                isAutomating = false
                targetPackage = null
                step = 0
                liteStep = LITE_STEP_IDLE
                isTargetLiteMode = false
            }, 150)
        }, 150)
    }

    private fun findNodeByKeywords(
        root: AccessibilityNodeInfo,
        keywords: List<String>,
        resourceIds: List<String> = emptyList()
    ): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()

            val viewId = node.viewIdResourceName?.lowercase() ?: ""
            for (rid in resourceIds) {
                if (viewId.contains(rid.lowercase())) {
                    return node
                }
            }

            val text = (node.text?.toString() ?: "") + " " + (node.contentDescription?.toString() ?: "")
            if (text.isNotBlank()) {
                val lowerText = text.lowercase()
                for (kw in keywords) {
                    if (lowerText.contains(kw)) {
                        return node
                    }
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun clickNode(node: AccessibilityNodeInfo): Boolean {
        var curr: AccessibilityNodeInfo? = node
        while (curr != null) {
            if (curr.isClickable) {
                return curr.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            curr = curr.parent
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }
}
