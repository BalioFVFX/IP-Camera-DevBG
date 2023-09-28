package com.example.ipcameraskeleton

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ipcameraskeleton.databinding.ActivityStreamBinding

@SuppressLint("MissingPermission")
class StreamActivity : AppCompatActivity() {

    private val binding: ActivityStreamBinding by lazy {
        ActivityStreamBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}