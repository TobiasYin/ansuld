package com.asld.asld

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import com.asld.asld.databinding.ActivityDiyBinding
const val TAG = "DIY_ACTIVITY"
class DiyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityDiyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.leftButton.setOnClickListener {
            binding.terminalView.cursor.move(-1, 0)
        }


        binding.rightButton.setOnClickListener {
            binding.terminalView.cursor.move(1, 0)
        }


        binding.upButton.setOnClickListener {
            binding.terminalView.cursor.move(0, -1)
        }

        binding.downButton.setOnClickListener {
            binding.terminalView.cursor.move(0, 1)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Log.d(TAG, "onTouchEvent: $event")
        return super.onTouchEvent(event)
    }
}