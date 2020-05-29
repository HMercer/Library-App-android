package com.sensorpic.demo.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sensorpic.demo.databinding.MainActivityBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
    }

    /// Private methods


}
