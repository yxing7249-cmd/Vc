package com.nexus.win12desktop.mouse

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.nexus.win12desktop.NexusApplication
import com.nexus.win12desktop.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Virtual Mouse Manager for Android TV.
 * Controls a visual mouse pointer using D-pad / remote keys.
 * This operates ONLY within the app's own UI hierarchy.
 * For system-wide control, AccessibilityService is required (separate).
 */
class VirtualMouseManager(
    private val context: Context,
    private val rootView: ViewGroup
) {

    interface MouseListener {
        fun onLeftClick(x: Float, y: Float)
        fun onRightClick(x: Float, y: Float)
        fun onMouseMove(x: Float, y: Float)
    }

    var listener: MouseListener? = null

    private var pointerView: ImageView? = null
    private var clickRippleView: View? = null

    private var posX = 0f
    private var posY = 0f

    private var screenWidth = 0
    private var screenHeight = 0

    private var isMoving = false
    private var currentDirection = 0
    private val moveHandler = Handler(Looper.getMainLooper())
    private var moveRunnable: Runnable? = null

    var baseSpeed = 12f
    var acceleration = 1.15f
    private var currentSpeed = baseSpeed
    private val maxSpeed = 48f
    private val tickIntervalMs = 16L

    private var okPressStartTime = 0L
    private val longPressThresholdMs = 500L
    private var isOkPressed = false
    private val okLongPressHandler = Handler(Looper.getMainLooper())
    private var okLongPressRunnable: Runnable? = null

    var isEnabled = true
        set(value) {
            field = value
            pointerView?.visibility = if (value) View.VISIBLE else View.GONE
        }

    var backKeyAsRightClick = true

    init {
        loadSettings()
        setupPointer()
        updateScreenSize()
        posX = screenWidth / 2f
        posY = screenHeight / 2f
        updatePointerPosition()
    }

    private fun loadSettings() {
        val prefs = NexusApplication.getPrefs(context)
        isEnabled = prefs.getBoolean("mouse_enabled", true)
        baseSpeed = prefs.getFloat("mouse_speed", 12f)
        backKeyAsRightClick = prefs.getBoolean("back_as_right_click", true)
        currentSpeed = baseSpeed
    }

    fun saveSettings() {
        val prefs = NexusApplication.getPrefs(context)
        prefs.edit()
            .putBoolean("mouse_enabled", isEnabled)
            .putFloat("mouse_speed", baseSpeed)
            .putBoolean("back_as_right_click", backKeyAsRightClick)
            .apply()
    }

    private fun setupPointer() {
        pointerView = ImageView(context).apply {
            setImageResource(R.drawable.ic_mouse_pointer)
            layoutParams = FrameLayout.LayoutParams(dpToPx(32), dpToPx(32))
            elevation = 100f
            isClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        clickRippleView = View(context).apply {
            background = ContextCompat.getDrawable(context, R.drawable.click_ripple)
            layoutParams = FrameLayout.LayoutParams(dpToPx(48), dpToPx(48))
            visibility = View.GONE
            elevation = 99f
            isClickable = false
            isFocusable = false
        }

        rootView.addView(clickRippleView)
        rootView.addView(pointerView)
    }

    fun updateScreenSize() {
        rootView.post {
            screenWidth = rootView.width
            screenHeight = rootView.height
            if (screenWidth <= 0 || screenHeight <= 0) {
                val metrics = context.resources.displayMetrics
                screenWidth = metrics.widthPixels
                screenHeight = metrics.heightPixels
            }
            posX = posX.coerceIn(0f, screenWidth.toFloat())
            posY = posY.coerceIn(0f, screenHeight.toFloat())
            updatePointerPosition()
        }
    }

    private fun updatePointerPosition() {
        pointerView?.let { pv ->
            val halfW = pv.width / 2f
            val halfH = pv.height / 2f
            pv.x = posX - halfW
            pv.y = posY - halfH
            listener?.onMouseMove(posX, posY)
        }
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!isEnabled) return false

        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (event.repeatCount == 0) {
                    startMoving(keyCode)
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_BUTTON_A -> {
                if (event.repeatCount == 0) {
                    onOkPressed()
                }
                true
            }
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_BUTTON_B -> {
                if (backKeyAsRightClick) {
                    if (event.repeatCount == 0) {
                        performRightClick()
                    }
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }

    fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (!isEnabled) return false

        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                stopMoving()
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_BUTTON_A -> {
                onOkReleased()
                true
            }
            else -> false
        }
    }

    private fun startMoving(direction: Int) {
        if (isMoving && currentDirection == direction) return
        stopMoving()
        isMoving = true
        currentDirection = direction
        currentSpeed = baseSpeed

        moveRunnable = object : Runnable {
            override fun run() {
                if (!isMoving) return
                moveByDirection(currentDirection)
                currentSpeed = min(currentSpeed * acceleration, maxSpeed)
                moveHandler.postDelayed(this, tickIntervalMs)
            }
        }
        moveByDirection(direction)
        moveHandler.postDelayed(moveRunnable!!, tickIntervalMs)
    }

    private fun stopMoving() {
        isMoving = false
        currentDirection = 0
        currentSpeed = baseSpeed
        moveRunnable?.let { moveHandler.removeCallbacks(it) }
        moveRunnable = null
    }

    private fun moveByDirection(direction: Int) {
        val delta = currentSpeed
        when (direction) {
            KeyEvent.KEYCODE_DPAD_UP -> posY -= delta
            KeyEvent.KEYCODE_DPAD_DOWN -> posY += delta
            KeyEvent.KEYCODE_DPAD_LEFT -> posX -= delta
            KeyEvent.KEYCODE_DPAD_RIGHT -> posX += delta
        }
        posX = posX.coerceIn(0f, screenWidth.toFloat())
        posY = posY.coerceIn(0f, screenHeight.toFloat())
        updatePointerPosition()
    }

    private fun onOkPressed() {
        isOkPressed = true
        okPressStartTime = System.currentTimeMillis()
        okLongPressRunnable = Runnable {
            if (isOkPressed) {
                performRightClick()
                isOkPressed = false
            }
        }
        okLongPressHandler.postDelayed(okLongPressRunnable!!, longPressThresholdMs)
    }

    private fun onOkReleased() {
        okLongPressRunnable?.let { okLongPressHandler.removeCallbacks(it) }
        if (isOkPressed) {
            val duration = System.currentTimeMillis() - okPressStartTime
            if (duration < longPressThresholdMs) {
                performLeftClick()
            }
        }
        isOkPressed = false
    }

    private fun performLeftClick() {
        showClickFeedback()
        dispatchClickToViewUnderPointer(isRightClick = false)
        listener?.onLeftClick(posX, posY)
    }

    private fun performRightClick() {
        showClickFeedback()
        dispatchClickToViewUnderPointer(isRightClick = true)
        listener?.onRightClick(posX, posY)
    }

    private fun showClickFeedback() {
        clickRippleView?.let { ripple ->
            ripple.x = posX - ripple.width / 2f
            ripple.y = posY - ripple.height / 2f
            ripple.visibility = View.VISIBLE
            ripple.alpha = 1f
            ripple.scaleX = 0.5f
            ripple.scaleY = 0.5f
            ripple.animate()
                .scaleX(1.5f)
                .scaleY(1.5f)
                .alpha(0f)
                .setDuration(300)
                .withEndAction { ripple.visibility = View.GONE }
                .start()
        }
        pointerView?.animate()
            ?.scaleX(0.8f)?.scaleY(0.8f)
            ?.setDuration(80)
            ?.withEndAction {
                pointerView?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(80)?.start()
            }?.start()
    }

    private fun dispatchClickToViewUnderPointer(isRightClick: Boolean) {
        val target = findViewAt(rootView, posX.toInt(), posY.toInt())
        target?.let { view ->
            if (isRightClick) {
                view.performLongClick()
            } else {
                view.performClick()
            }
        }
    }

    private fun findViewAt(parent: ViewGroup, x: Int, y: Int): View? {
        for (i in parent.childCount - 1 downTo 0) {
            val child = parent.getChildAt(i)
            if (child === pointerView || child === clickRippleView) continue
            if (child.visibility != View.VISIBLE) continue

            val location = IntArray(2)
            child.getLocationOnScreen(location)
            val rootLoc = IntArray(2)
            rootView.getLocationOnScreen(rootLoc)

            val left = location[0] - rootLoc[0]
            val top = location[1] - rootLoc[1]
            val right = left + child.width
            val bottom = top + child.height

            if (x in left until right && y in top until bottom) {
                if (child is ViewGroup) {
                    val found = findViewAt(child, x, y)
                    if (found != null) return found
                }
                if (child.isClickable || child.isLongClickable || child.hasOnClickListeners()) {
                    return child
                }
            }
        }
        return null
    }

    fun setPosition(x: Float, y: Float) {
        posX = x.coerceIn(0f, screenWidth.toFloat())
        posY = y.coerceIn(0f, screenHeight.toFloat())
        updatePointerPosition()
    }

    fun getPosition(): PointF = PointF(posX, posY)

    fun destroy() {
        stopMoving()
        okLongPressRunnable?.let { okLongPressHandler.removeCallbacks(it) }
        pointerView?.let { rootView.removeView(it) }
        clickRippleView?.let { rootView.removeView(it) }
        pointerView = null
        clickRippleView = null
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
