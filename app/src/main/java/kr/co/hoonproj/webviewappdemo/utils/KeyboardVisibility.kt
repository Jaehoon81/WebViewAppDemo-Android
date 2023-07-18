package kr.co.hoonproj.webviewappdemo.utils

import android.graphics.Rect
import android.view.ViewTreeObserver
import android.view.Window

class KeyboardVisibility(
    private val window: Window,
    private val onShowKeyboard: ((keyboardHeight: Int) -> Unit)? = null,
    private val onHideKeyboard: (() -> Unit)? = null
) {

    private val MIN_KEYBOARD_HEIGHT_PX: Int = 150

    private val windowVisibleDisplayFrame: Rect = Rect()
    private var lastVisibleDecorViewHeight: Int = 0
    private var isKeyboardVisible: Boolean = false

    private val onGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        window.decorView.getWindowVisibleDisplayFrame(windowVisibleDisplayFrame)
        val visibleDecorViewHeight = windowVisibleDisplayFrame.height()

        if (lastVisibleDecorViewHeight != 0) {
            if (lastVisibleDecorViewHeight > visibleDecorViewHeight + MIN_KEYBOARD_HEIGHT_PX) {
                val keyboardHeight = window.decorView.height - windowVisibleDisplayFrame.bottom
                onShowKeyboard?.invoke(keyboardHeight)
                isKeyboardVisible = true

            } else if (lastVisibleDecorViewHeight + MIN_KEYBOARD_HEIGHT_PX < visibleDecorViewHeight) {
                if (isKeyboardVisible == true) {
                    isKeyboardVisible = false
                    onHideKeyboard?.invoke()
                }
            }
        }
        lastVisibleDecorViewHeight = visibleDecorViewHeight
    }

    init {
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
    }

    fun detachKeyboardListener() {
        window.decorView.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
    }
}