package com.asld.asld.vnc

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import com.asld.asld.R

class VncPresentation(context: Context, display: Display) : Presentation(context, display) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.vnc_canvas)
    }
    fun getVncCanvas(): VncCanvas{
        return findViewById(R.id.vnc_canvas)
    }
}