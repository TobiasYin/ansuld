package com.asld.asld.tools

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import kotlin.concurrent.thread

class ProgressBarDialog(context: Context, text:String="loadding", layoutPadding:Int=30): AlertDialog(context) {
    val layout = LinearLayout(context)
    val progressBar = ProgressBar(context)
    val textView = TextView(context)
    private val handler = Handler(Looper.getMainLooper()) {
        val updater = it.obj as ()->Unit
        updater()
        true
    }
    fun updateView(updater: ()->Unit){
        val m = Message()
        m.obj = updater
        handler.sendMessage(m)
    }


    init {
        layout.orientation = LinearLayout.HORIZONTAL
        layout.setPadding(layoutPadding, layoutPadding, layoutPadding, layoutPadding)
        layout.gravity = Gravity.CENTER
        var layoutParam = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParam.gravity = Gravity.CENTER
        layout.layoutParams = layoutParam
        progressBar.isIndeterminate = true
        progressBar.setPadding(0, 0, layoutPadding, 0)
        progressBar.layoutParams = layoutParam
        layoutParam.gravity = Gravity.CENTER
        textView.text = text
        textView.setTextColor(Color.parseColor("#000000"))
        textView.textSize = 20f
        textView.layoutParams = layoutParam
        layout.addView(progressBar)
        layout.addView(textView)
        setCancelable(false)
        setView(layout)
    }

    companion object{
        fun create(
            context: Context,
            text: String,
            then: ((ProgressBarDialog) -> Unit)? = null
        ):ProgressBarDialog{
            val dialog = ProgressBarDialog(context, text)

            dialog.show()
            dialog.window?.let {
                val layoutParams = WindowManager.LayoutParams()
                layoutParams.copyFrom(it.attributes)
                layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
                layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
                it.attributes = layoutParams
            }
            if (then != null) {
                thread {
                    then(dialog)
                    dialog.cancel()
                }
            }
            return dialog
        }
    }
}

fun create(
    context: Context,
    text: String,
    then: ((androidx.appcompat.app.AlertDialog) -> Unit)? = null
): androidx.appcompat.app.AlertDialog {
    val llPadding = 30
    val ll = LinearLayout(context)
    ll.orientation = LinearLayout.HORIZONTAL
    ll.setPadding(llPadding, llPadding, llPadding, llPadding)
    ll.gravity = Gravity.CENTER
    var llParam = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
    llParam.gravity = Gravity.CENTER
    ll.layoutParams = llParam
    val progressBar = ProgressBar(context)
    progressBar.isIndeterminate = true
    progressBar.setPadding(0, 0, llPadding, 0)
    progressBar.layoutParams = llParam
    llParam = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
    llParam.gravity = Gravity.CENTER
    val tvText = TextView(context)
    tvText.text = text
    tvText.setTextColor(Color.parseColor("#000000"))
    tvText.textSize = 20f
    tvText.layoutParams = llParam
    ll.addView(progressBar)
    ll.addView(tvText)
    val builder: androidx.appcompat.app.AlertDialog.Builder = androidx.appcompat.app.AlertDialog.Builder(context)
    builder.setCancelable(false)
    builder.setView(ll)
    val dialog: androidx.appcompat.app.AlertDialog = builder.create()
    dialog.show()
    dialog.window?.let {
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(it.attributes)
        layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
        layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
        it.attributes = layoutParams
    }
    if (then != null) {
        thread {
            then(dialog)
            dialog.cancel()
        }
    }
    return dialog
}