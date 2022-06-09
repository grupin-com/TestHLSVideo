package com.lukasdylan.hlsvideo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lukasdylan.hlsvideo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.floatingVideoPlayer.initializePlayer(
            url = "https://grupin-video-output-public.s3.ap-southeast-1.amazonaws.com/output/sample_revhls_h264_aac.m3u8",
            isAutoRepeat = true
        )
    }

    override fun onBackPressed() {
        if (!binding.floatingVideoPlayer.minimizeOnBackPressed()) {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.floatingVideoPlayer.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.floatingVideoPlayer.onPause()
    }

    override fun onDestroy() {
        binding.floatingVideoPlayer.onDestroy()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            val latestMillis = data?.getLongExtra("LATEST_MILLIS", 0L) ?: 0L
            binding.floatingVideoPlayer.seekTo(latestMillis + 1000L)
        }
    }
}