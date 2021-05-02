package com.asld.asld.vnc

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.Display
import android.view.View
import com.asld.asld.R
import com.asld.asld.exception.ErrorCode

class VncPresentation(context: Context, display: Display, val handler:Handler) : Presentation(context, display) {
    companion object{
        var count = 0
    }
    private val TAG = "VncPresentation"
    init {
        Log.d(TAG, "init: ")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.vnc_presentation)
        Log.d("pre", "$window")
    }
    fun getVncCanvas(): VncCanvas{
        return findViewById(R.id.vnc_canvas)
    }

    override fun onStart() {
        Log.d(TAG, "onStart: ")
        super.onStart()
    }

    override fun onStop() {
        count += 1
        if(count == 2){
            throw Error("onstop")
        }
        Log.d(TAG, "onStop: ")
        super.onStop()
    }

    override fun onDisplayRemoved() {
        Log.d(TAG, "onDisplayRemoved: ")
        val msg = Message()
        msg.obj = ErrorCode.VNC_DISPLAY_CHANGED
        msg.what = display.displayId
        handler.sendMessage(msg)
    }

    override fun onDisplayChanged() {
        super.onDisplayChanged()
        Log.d(TAG, "onDisplayChanged: ")
    }

}