package com.asld.asld.vnc

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.View
import com.asld.asld.R

class VncPresentation(context: Context, display: Display) : Presentation(context, display) {
    private val TAG = "VncPresentation"
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
        Log.d(TAG, "onStop: ")
        super.onStop()
    }
}