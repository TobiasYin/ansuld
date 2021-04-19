package com.asld.asld

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.asld.asld.databinding.ActivityDiyBinding

class DiyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityDiyBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}