package kr.co.hoonproj.webviewappdemo.utils

import android.content.Context
import android.graphics.Color
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import kr.co.hoonproj.webviewappdemo.R

class CustomAlertDialog(
    private val context: Context,
    private val title: String? = null,
    private val message: String? = null,
    private val positiveButtonText: String = "확인",
    private val negativeButtonText: String = "취소"
) {

    private lateinit var positiveListener: () -> Unit
    private lateinit var negativeListener: () -> Unit

    private val alertDialog1: AlertDialog.Builder by lazy {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(positiveButtonText) { _, _ ->
                if (::positiveListener.isInitialized) { positiveListener() }
            }
    }
    private val alertDialog2: AlertDialog.Builder by lazy {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(positiveButtonText) { _, _ ->
                if (::positiveListener.isInitialized) { positiveListener() }
            }
            .setNegativeButton(negativeButtonText) { _, _ ->
                if (::negativeListener.isInitialized) { negativeListener() }
            }
    }

    fun show(onClickNegative: (() -> Unit)? = null, onClickPositive: (() -> Unit) = {}) {
        onClickNegative?.let { this.negativeListener = it }
        this.positiveListener = onClickPositive

        val alertDialog = if (onClickNegative == null) { alertDialog1 } else { alertDialog2 }
        alertDialog.show().apply {
            findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.apply {
                textSize = 15.0F
                typeface = ResourcesCompat.getFont(context, R.font.nanum_gothic_bold)
                setTextColor(Color.BLACK)
            }
            findViewById<TextView>(android.R.id.message)?.apply {
                textSize = 15.0F
                typeface = ResourcesCompat.getFont(context, R.font.nanum_gothic)
                setTextColor(Color.parseColor("#FF303030"))
            }
            findViewById<TextView>(android.R.id.button1)?.apply {
                textSize = 13.0F
                typeface = ResourcesCompat.getFont(context, R.font.nanum_gothic_bold)
                setTextColor(context.getColor(R.color.teal_700))
            }
            findViewById<TextView>(android.R.id.button2)?.apply {
                textSize = 13.0F
                typeface = ResourcesCompat.getFont(context, R.font.nanum_gothic_bold)
                setTextColor(context.getColor(R.color.teal_700))
            }
        }
    }
}