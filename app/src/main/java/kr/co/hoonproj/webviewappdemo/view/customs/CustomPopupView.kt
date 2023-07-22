package kr.co.hoonproj.webviewappdemo.view.customs

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.webkit.WebView
import android.widget.LinearLayout
import com.google.android.material.progressindicator.LinearProgressIndicator
import kr.co.hoonproj.webviewappdemo.databinding.ViewCustomPopupBinding
import kr.co.hoonproj.webviewappdemo.utils.CustomAlertDialog
import kr.co.hoonproj.webviewappdemo.utils.KeyboardVisibility
import kr.co.hoonproj.webviewappdemo.view.listeners.OnKeyboardChangedListener

interface OnPopupChangedListener {

    fun onClosePopup(sender: CustomPopupView)
}

@SuppressLint("ViewConstructor")
class CustomPopupView @JvmOverloads constructor(
    activity: Activity,
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var mBinding: ViewCustomPopupBinding? = null
    private var popupWebView: WebView? = null

    var popupListener: OnPopupChangedListener? = null
    var popupProgressBar: LinearProgressIndicator? = null

    private var keyboardVisibility: KeyboardVisibility? = null
    private var keyboardListener: OnKeyboardChangedListener? = null
    private var tabTag: String? = null

    init {
        mBinding = ViewCustomPopupBinding.inflate(LayoutInflater.from(context), this, true)
        mBinding?.goBackButton!!.setOnClickListener {
            if (popupWebView != null) {
                if (popupWebView!!.canGoBack()) {
                    popupWebView!!.goBack()
                } else {
                    activity.runOnUiThread {
                        CustomAlertDialog(context, message = "이전 페이지가 없습니다.").show {
                            keyboardListener?.onHideKeyboard(tabTag)
                        }
                    }
                }
            }
        }
        mBinding?.cancelButton!!.setOnClickListener {
            keyboardVisibility?.detachKeyboardListener()
            keyboardListener?.onHideKeyboard(tabTag)

            removeWebView()
            popupListener?.onClosePopup(this)
        }
        popupProgressBar = mBinding?.progressBar!!
        keyboardVisibility = KeyboardVisibility(
            activity.window,
            onShowKeyboard = { keyboardHeight -> keyboardListener?.onShowKeyboard(tabTag) },
            onHideKeyboard = { keyboardListener?.onHideKeyboard(tabTag) }
        )
    }

    fun addKeyboardListener(keyboardListener: OnKeyboardChangedListener, tabTag: String?) {
        this.keyboardListener = keyboardListener
        this.tabTag = tabTag
    }

    fun getWebView(): WebView? = popupWebView

    fun addWebView(webView: WebView) {
        popupWebView = webView
        mBinding?.containerView!!.addView(webView)
    }

    private fun removeWebView() {
        mBinding?.containerView!!.removeView(popupWebView)
    }
}