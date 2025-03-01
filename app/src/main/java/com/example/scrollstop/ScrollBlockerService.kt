package com.example.scrollstop

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView

class ScrollBlockerService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var scrollCounter = 0
    private val maxScrollCount = 4
    private var countdownTextView: TextView? = null
    private val TAG = "ScrollBlockerService"
    private val scrollWindowMillis = 5000L // 5 second window to detect scrolling
    private val handler = Handler(Looper.getMainLooper())
    private var lastScrollTime: Long = 0
    private var scrollsInWindow = 0
    private var lastScrollY: Int = 0
    private val minScrollDistance = 50 // minimum pixels to consider as scroll

    private val targetPackages = setOf(
        "com.android.chrome",
        "com.instagram.android",
        "com.facebook.katana",
        "com.whatsapp"
    )
    
    private var currentPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service connected")
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_SCROLLED or 
                        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Update current package name when app switches
        event.packageName?.toString()?.let { 
            currentPackage = it
            Log.d(TAG, "Current package: $currentPackage")
        }

        // Only process events for target apps
        if (!targetPackages.contains(currentPackage)) {
            return
        }

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            val scrollY = event.scrollY
            
            // Only count as scroll if there's significant vertical movement
            if (Math.abs(scrollY - lastScrollY) > minScrollDistance) {
                val currentTime = System.currentTimeMillis()
                
                // Reset counter if too much time has passed
                if (currentTime - lastScrollTime > scrollWindowMillis) {
                    scrollsInWindow = 0
                }
                
                scrollsInWindow++
                lastScrollTime = currentTime
                lastScrollY = scrollY

                Log.d(TAG, "Scroll detected in $currentPackage, count: $scrollsInWindow, scrollY: $scrollY")

                // Check for rapid scrolling
                if (scrollsInWindow >= 3) {
                    handleScrollEvent()
                }
            }
        }
    }

    private fun handleScrollEvent() {
        scrollCounter++
        Log.d(TAG, "Rapid scrolling detected. Count: $scrollCounter")
        
        if (scrollCounter >= maxScrollCount) {
            Log.d(TAG, "Maximum scroll count reached. Showing final warning.")
            showOverlay()
            handler.postDelayed({
                performGlobalAction(GLOBAL_ACTION_HOME)
            }, 2000)
            resetScrollCounter()
        } else {
            showOverlay()
        }
        
        scrollsInWindow = 0
    }

    private fun showOverlay() {
        try {
            if (windowManager == null) {
                windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                Log.d(TAG, "Window manager initialized")
            }

            if (overlayView == null) {
                val inflater = LayoutInflater.from(this)
                overlayView = inflater.inflate(R.layout.overlay_layout, null)
                Log.d(TAG, "Overlay view created")

                countdownTextView = overlayView?.findViewById(R.id.countdown_text)

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT
                )

                params.gravity = Gravity.CENTER

                try {
                    windowManager?.addView(overlayView, params)
                    Log.d(TAG, "Overlay view added to window manager")
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding overlay view: ${e.message}")
                    e.printStackTrace()
                }
            }

            // Update the countdown text
            val remainingAttempts = maxScrollCount - scrollCounter
            countdownTextView?.text = "${getString(R.string.exit_message)} ($remainingAttempts)"
            Log.d(TAG, "Updated countdown: $remainingAttempts")

            overlayView?.visibility = View.VISIBLE

            handler.postDelayed({
                overlayView?.visibility = View.GONE
                Log.d(TAG, "Overlay hidden after delay")
            }, 2000)
        } catch (e: Exception) {
            Log.e(TAG, "Error in showOverlay: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
        cleanup()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        cleanup()
    }

    private fun cleanup() {
        if (windowManager != null && overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
                Log.d(TAG, "Overlay view removed")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing view: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun resetScrollCounter() {
        scrollCounter = 0
        scrollsInWindow = 0
        lastScrollY = 0
    }
}